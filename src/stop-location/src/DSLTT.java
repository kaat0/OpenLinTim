import com.dashoptimization.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class DSLTT{

	private FiniteDominatingSet fds;
	private CandidateEdgeSet ecs;
	private Demand demand;
	private int waitingTime;
	private XPRBprob p;
	private TravelingTime travelingTime;
	private PTN ptn_new;


	public DSLTT(FiniteDominatingSet fds, CandidateEdgeSet ecs, Demand demand, int waitingTime, TravelingTime travelingTime){
		this.fds = fds;
		this.ecs = ecs;
		this.demand = demand;
		this.waitingTime = waitingTime;
		this.travelingTime = travelingTime;
		constructXpressIPFormulation(fds,ecs,demand,waitingTime);
	}

	private void constructXpressIPFormulation(FiniteDominatingSet fds, CandidateEdgeSet ecs, Demand demand, int waitingTime){
		XPRS.init();
       	XPRB bcl = new XPRB();
		p = bcl.newProb("Traveling Time Minimization for Stop Location");

		int number_vertices = 0;
		for(Candidate candidate:fds.getCandidates()){
			if(candidate.isVertex())
				number_vertices++;
		}
		int number_candidates = fds.getCandidates().size();
		int number_candidate_edges = ecs.getCandidateEdges().size();
		int number_demand = demand.getDemand_points().size();

		XPRBvar[] y = new XPRBvar[number_candidate_edges];
		XPRBvar[] x = new XPRBvar[number_candidates];

		XPRBctr[] ctrDemand = new XPRBctr[number_demand];
        XPRBctr[] ctrBuilding = new XPRBctr[number_candidate_edges];
		XPRBctr[] ctrVertices = new XPRBctr[number_vertices];
        XPRBctr objective = p.newCtr("objective");
        objective.setType(XPRB.N);

		System.out.println("x-Variablen anlegen");
		int vertex_count = 0;
		for(Candidate candidate:fds.getCandidates()){
			x[candidate.getId()-1] = p.newVar("x_" + candidate.getId(), XPRB.BV, 0.0, 1.0);
			if(candidate.isVertex()){
				ctrVertices[vertex_count] = p.newCtr("build_vertex_" + candidate.getId());
				ctrVertices[vertex_count].setType(XPRB.E);
				ctrVertices[vertex_count].setTerm(1);
				ctrVertices[vertex_count].setTerm(x[candidate.getId()-1],1.0);
				vertex_count++;
			}
		}

		System.out.println("y-Variablen anlegen");
		for(CandidateEdge candidateEdge:ecs.getCandidateEdges())
			y[candidateEdge.getIndex()-1] = p.newVar("y_" + candidateEdge.getIndex(), XPRB.BV, 0.0, 1.0);

		System.out.println("Nebenbedingungen anlegen");
		for(DemandPoint demand_point:demand.getDemand_points()){
			ctrDemand[demand.getDemand_points().indexOf(demand_point)] = p.newCtr("cover_" + demand.getDemand_points().indexOf(demand_point));
 			ctrDemand[demand.getDemand_points().indexOf(demand_point)].setType(XPRB.G); // <=
			ctrDemand[demand.getDemand_points().indexOf(demand_point)].setTerm(1);
			for(Candidate candidate:fds.getCandidates()){
				if(fds.getDistance().calcDist(demand_point,candidate)<=fds.getRadius()+Distance.EPSILON)
					ctrDemand[demand.getDemand_points().indexOf(demand_point)].setTerm(x[candidate.getId()-1],1.0);
			}
		}
		for(Candidate candidate:fds.getCandidates()){
			objective.setTerm(x[candidate.getId()-1],waitingTime);
		}
		for(CandidateEdge candidateEdge:ecs.getCandidateEdges()){
			ctrBuilding[candidateEdge.getIndex()-1] = p.newCtr("building_"+candidateEdge.getIndex());
			ctrBuilding[candidateEdge.getIndex()-1].setType(XPRB.L);
			ctrBuilding[candidateEdge.getIndex()-1].setTerm(y[candidateEdge.getIndex()-1],-1.0);
			ctrBuilding[candidateEdge.getIndex()-1].setTerm(1);
			objective.setTerm(y[candidateEdge.getIndex()-1],travelingTime.calcTime(candidateEdge.getLength()));
			for(Candidate candidate:fds.getCandidates()){
				if(candidate == candidateEdge.getLeft_candidate() || candidate == candidateEdge.getRight_candidate())
					ctrBuilding[candidateEdge.getIndex()-1].setTerm(x[candidate.getId()-1],1.0);
				else if(candidateEdge.isOnCandidateEdge(candidate)){
					ctrBuilding[candidateEdge.getIndex()-1].setTerm(x[candidate.getId()-1],-1.0);
				}
			}
		}
		 p.setObj(objective);
		 p.getXPRSprob().setIntControl(XPRS.MAXTIME, 300);
	
	}
	
	public void solve(){
		try {
			p.exportProb(XPRB.LP, "dsltt.lp");
			p.minim("g");
			Thread.sleep(2000);
		} catch (Exception e) {}
 
		if (p.getMIPStat() == XPRB.MIP_INFEAS) {
			System.out.println("MIP UNZULAESSIG!");
			System.exit(1);
		}
		
		int number_candidates = fds.getCandidates().size();
		int number_candidate_edges = ecs.getCandidateEdges().size();
		Boolean[] y_sol = new Boolean[number_candidate_edges];
		Boolean[] x_sol = new Boolean[number_candidates];
		int built_stations = 0;
		LinkedList<Stop> stops = new LinkedList<Stop>();
		for(Candidate candidate:fds.getCandidates()){
			x_sol[candidate.getId()-1] = p.getVarByName("x_"+candidate.getId()).getSol()==1.0?true:false;
			if(x_sol[candidate.getId()-1]){
				Stop stop = candidate.toStop(built_stations+1);
				stops.add(built_stations,stop);
				built_stations++;
			}
		
		}
		int built_edges = 0;
		LinkedList<Edge> edges = new LinkedList<Edge>();
		for(CandidateEdge candidateEdge:ecs.getCandidateEdges()){
			y_sol[candidateEdge.getIndex()-1] =Math.round(p.getVarByName("y_"+candidateEdge.getIndex()).getSol())==1?true:false;
			if(y_sol[candidateEdge.getIndex()-1]){
				Edge edge = candidateEdge.toEdge(built_edges+1, stops.get(candidateEdge.getLeft_candidate().getStopIndex()-1),stops.get(candidateEdge.getRight_candidate().getStopIndex()-1));
				edges.add(built_edges, edge);
				built_edges++;
			}
		}
		this.ptn_new = new PTN(false,stops,edges);
	}

	public PTN getNewPTN(){
		return ptn_new;
	}

 }

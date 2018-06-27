import com.dashoptimization.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class DSL{

	private FiniteDominatingSet fds;
	private Demand demand;
	private XPRBprob p;
	private LinkedList<Candidate> candidates;


	public DSL(FiniteDominatingSet fds, Demand demand){
		this.fds = fds;
		this.demand = demand;
		constructXpressIPFormulation(fds,demand);
	}

	private void constructXpressIPFormulation(FiniteDominatingSet fds, Demand demand){
		XPRS.init();
       	XPRB bcl = new XPRB();
		p = bcl.newProb("Minimizing Number of Stops for Stop Location");

		int number_vertices = 0;
		for(Candidate candidate:fds.getCandidates()){
			if(candidate.isVertex())
				number_vertices++;
		}
		int number_candidates = fds.getCandidates().size();
		int number_demand = demand.getDemand_points().size();

		XPRBvar[] x = new XPRBvar[number_candidates];



		XPRBctr[] ctrDemand = new XPRBctr[number_demand];
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
			objective.setTerm(x[candidate.getId()-1],1.0);
		}
		
		p.setObj(objective);
		p.getXPRSprob().setIntControl(XPRS.MAXTIME, 300);
	}
	
	public void solve(){
		try {
            p.exportProb(XPRB.LP, "dsl.lp");
       	p.minim("g");
        Thread.sleep(2000);
        } catch (Exception e) {}
 
	if (p.getMIPStat() == XPRB.MIP_INFEAS) {
	    System.out.println("MIP UNZULAESSIG!");
	    System.exit(1);
	}
	int number_candidates = fds.getCandidates().size();
	Boolean[] x_sol = new Boolean[number_candidates];
	int built_stations = 0;
	this.candidates = new LinkedList<Candidate>();
	for(Candidate candidate:fds.getCandidates()){
		x_sol[candidate.getId()-1] = p.getVarByName("x_"+candidate.getId()).getSol()==1.0?true:false;
		if(x_sol[candidate.getId()-1]){
			this.candidates.add(candidate);
		}
	}	
		
	}

	public LinkedList<Candidate> getBuiltCandidates(){
		return this.candidates;
	}

 }

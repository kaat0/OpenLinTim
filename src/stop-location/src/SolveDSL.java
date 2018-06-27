import java.io.*;
import java.lang.*;
import java.lang.reflect.Method;
import java.util.*;

public class SolveDSL {

	public static void main(String[] args) {
		if(args.length != 1){
            throw new RuntimeException("Error: number of arguments invalid; first " +
                    "argument must be the path to the configuration file.");
        }

        try {
            File config_file = new File(args[0]);

            System.err.print("Loading Configuration... ");
            Config config = new Config(config_file);
            System.err.println("done!");
            
            System.err.print("Set variables... ");
            boolean directed=!config.getBooleanValue("ptn_is_undirected");
			boolean destruction_allowed=config.getBooleanValue("sl_destruction_allowed");
            double radius = config.getDoubleValue("sl_radius");

            Candidate.setDefault_name(config.getStringValue("sl_new_stop_default_name"));

            Stop.setHeader(config.getStringValue("stops_header"));
            Edge.setHeader(config.getStringValue("edges_header"));

            Distance distance;
            if(config.getStringValue("sl_distance").equals("euclidean_norm")){
            	distance = new EuclideanNorm();
            }else{
            	throw new IOException("Distance not defined.");
            }
            System.err.println("done!");
       
            System.err.print("Read files...");
            File existing_stop_file=new File(config.getStringValue("default_existing_stop_file"));
            File existing_edge_file=new File(config.getStringValue("default_existing_edge_file"));
            File demand_file=new File(config.getStringValue("default_demand_file"));
           
            PTN ptn=new PTN(directed);
            Demand demand = new Demand();
            
            PTNCSV.fromFile(ptn, existing_stop_file, existing_edge_file);
            DemandCSV.fromFile(demand, demand_file);
            System.err.println("done!");

	     if(destruction_allowed)
		ptn.removeDestructableStations();
             
            System.err.print("Calculate fds..."); 
            FiniteDominatingSet fds= new FiniteDominatingSet(ptn, demand, distance, radius, destruction_allowed, false);
            System.err.println("done!");
            
            System.err.print("The number of candidates is: ");
            System.err.println(fds.getNumberOfCandidates());

	     System.err.print("Setting up IP-Formulation...");
	     DSL dsl = new DSL(fds,demand);
	     System.err.println("done!");
	     
	   	 System.err.print("Solving IP-Formulation...");
	   	 long time = System.currentTimeMillis();
	     dsl.solve();
	     System.err.println("Done in " + (System.currentTimeMillis()-time) + " milliseconds!");
	

	     LinkedList<Stop> stopsToRemove = new LinkedList<Stop>();
		LinkedList<Edge> edgesToRemove = new LinkedList<Edge>();
		boolean isRemove = true;
	     for(Stop stop:ptn.getStops()){
		isRemove = true;
		for(Candidate candidate:dsl.getBuiltCandidates()){
			if(candidate.isVertex() && distance.calcDist(stop,candidate)<Distance.EPSILON){
				isRemove =false;
				break;
			}
		}
		if(isRemove){
			stopsToRemove.add(stop);
		}
	     }

	     if(dsl.getBuiltCandidates()!= null)	{
	     	for(Candidate candidate:dsl.getBuiltCandidates()){
			if(!candidate.isVertex())
            		ptn.insertCandidate(candidate);
		}
	     }
		for(Stop stop:stopsToRemove)
		for(Edge edge:ptn.getEdges())
			if(edge.getLeft_stop()==stop || edge.getRight_stop()==stop)
				edgesToRemove.add(edge);

		ptn.getStops().removeAll(stopsToRemove);
		ptn.getEdges().removeAll(edgesToRemove);
		
            System.err.print("Writing ptn to files...");
            PTNCSV.toFile(ptn, new File(config.getStringValue("default_stops_file")), new File(config.getStringValue("default_edges_file")));
            System.err.println("done!");
 
        }catch(IOException e){
        	System.err.println("An error occurred while reading a file.");
        	throw new RuntimeException(e);
        }
	}

}

import java.util.*;
import java.util.Map.Entry;

public class OD {
	PTN ptn;
	double ptn_speed;
	double waiting_time;
	double conversion_factor_length;
	private HashMap<Stop, HashMap<Stop, Double>> origins;

	//Constructor----------------------------------------------------------------
	
	public OD(PTN ptn, double ptn_speed, double waiting_time,
			double conversion_factor_length){
		this.ptn =ptn;
		this.ptn_speed=ptn_speed;
		this.waiting_time=waiting_time;
		this.conversion_factor_length=conversion_factor_length;
		origins=new HashMap<Stop, HashMap<Stop,Double>>();
		for(Stop stop:ptn.getStops()){
			origins.put(stop,new HashMap<Stop,Double>());
		}
	}
	
	//Setter---------------------------------------------------------------------
	
	public void setPassengersAt(Stop origin, Stop destination, double passengers){
		HashMap<Stop, Double> origin_map=origins.get(origin);
		origin_map.put(destination, passengers);
	}
	
	//Getter--------------------------------------------------------------------
	
	public double getPassengersAt(Stop origin, Stop destination){
		return origins.get(origin).get(destination);
	}
	
	public HashMap<Stop,HashMap<Stop,Double>> getOD(){
		return origins;
	}
	
	public double getSpeed(){
		return ptn_speed;
	}
	
	public double getConversionFactor(){
		return conversion_factor_length;
	}
	
	public double getWaitingTime(){
		return waiting_time;
	}
	
	public double getMaximumODEntry(){
		double max_passengers=0;
		double passengers;
		HashMap<Stop, Double> destinations;
		//Calculate largest entry
		for(Entry<Stop, HashMap<Stop,Double>> entry_1: origins.entrySet()){
			destinations=entry_1.getValue();
			for(Entry<Stop, Double> entry_2: destinations.entrySet()){
				passengers=entry_2.getValue();
				if(passengers>max_passengers){
					max_passengers=passengers;
				}
			}
		}
		return max_passengers;
	}
	
	//Calculate significant edges-----------------------------------------------
	
	public LinkedList<Edge> calcSignificantEdges(double ratio){
		HashMap<Edge,Double> edge_usage=new HashMap<Edge,Double>();
		LinkedList<Edge> significant_edges=new LinkedList<Edge>();
		Stop origin;
		HashMap<Stop, Double> destinations;
		double passengers;
		LinkedList<Edge> path;
		
		for(Edge edge:ptn.getEdges()){
			edge_usage.put(edge,0.);
		}

		KShortestPathsWrapper wrapper= new KShortestPathsWrapper(ptn,this);
		for(Entry<Stop, HashMap<Stop,Double>> entry_origin:origins.entrySet()){
			origin=entry_origin.getKey();
			destinations=entry_origin.getValue();
			for(Entry<LinkedList<Edge>,Double>entry_path:wrapper.getWeightedPaths(origin, destinations, 1).entrySet()){
				path=entry_path.getKey();
				passengers=entry_path.getValue();
				for(Edge edge:path){
					edge_usage.put(edge,edge_usage.get(edge)+passengers);
				}
			}
		}
		
		
		class EdgeEntryComparator implements Comparator<Entry<Edge,Double>>{
		  @Override public int compare( Entry<Edge,Double> edge_entry_1,Entry<Edge,Double> edge_entry_2){
		    return (int) Math.signum(edge_entry_2.getValue()-edge_entry_1.getValue());
		  }
		}
		
		//Compute which edges are used often enough to be considered
		LinkedList<Entry<Edge,Double>> edge_usage_list=new LinkedList<Entry<Edge,Double>>();
		
		for(Entry<Edge,Double> entry: edge_usage.entrySet()){
			edge_usage_list.add(entry);
		}
		
		Collections.sort(edge_usage_list, new EdgeEntryComparator());
		
		double quorum=ratio*ptn.getEdges().size();
		int i=0;
		for(Entry<Edge,Double> entry:edge_usage_list){
			if(i>quorum){
				break;
			}
			i++;
			significant_edges.add(entry.getKey());
		}
		
		return significant_edges;
	}
	
}

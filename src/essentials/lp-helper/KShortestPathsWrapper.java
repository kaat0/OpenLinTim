import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;

import java.util.*;
import java.util.Map.*;

public class KShortestPathsWrapper {
	//KSP-Graph
	class KSPNode{
		private Stop stop;
		private boolean is_arrival;
		
		KSPNode(Stop stop, boolean is_arrival){
			this.stop=stop;
			this.is_arrival=is_arrival;
		}
		
		Stop getStop(){
			return stop;
		}
		
		boolean isArrival(){
			return is_arrival;
		}
		
		public boolean equals(Object o){
			if(!(o instanceof KSPNode)){
				return false;
			}
			KSPNode other=(KSPNode) o;
			return stop.equals(other.getStop()) && (is_arrival==other.is_arrival);
		}
	}
	
	class KSPEdge{
		private Edge edge;
		private double weight;
		private boolean is_waiting_edge;
		
		KSPEdge(Edge edge, double weight, boolean is_waiting_edge){
			this.edge=edge;
			this.weight=weight;
			this.is_waiting_edge=is_waiting_edge;
		}
		
		Edge getEdge(){
			return edge;
		}
		
		double getWeight(){
			return weight;
		}
		
		boolean isWaitingEdge(){
			return is_waiting_edge;
		}
	}
	
	private SimpleDirectedWeightedGraph<KSPNode, KSPEdge> ptn_extended;
	private HashMap<Stop, KSPNode> ksp_arrival;
	private HashMap<Stop, KSPNode> ksp_departure;
	private HashMap<Edge, KSPEdge> ksp_edges;
	
//Constructor--------------------------------------------------------------------
	public KShortestPathsWrapper(PTN ptn, OD od) {
		ptn_extended=new SimpleDirectedWeightedGraph<KSPNode, KSPEdge>(KSPEdge.class);
		TravelingTime tt= new TravelingTime(od.getSpeed());
		
		//Construct graph
		KSPNode arrival;
		KSPNode departure;
		KSPEdge ksp_edge;
		ksp_arrival=new HashMap<Stop, KSPNode>();
		ksp_departure=new HashMap<Stop, KSPNode>();
		ksp_edges=new HashMap<Edge, KSPEdge>();
		//waiting_edges + split stops
		for(Stop stop: ptn.getStops()){
			arrival=new KSPNode(stop, true);
			ksp_arrival.put(stop, arrival);
			ptn_extended.addVertex(arrival);
			departure=new KSPNode(stop, false);
			ksp_departure.put(stop,departure);
			ptn_extended.addVertex(departure);
			ptn_extended.addEdge(arrival, departure, new KSPEdge(null, od.getWaitingTime(), true));
		}
		
		//ptn-edges
		//length is calculated using TravelingTime
		for(Edge edge: ptn.getEdges()){
			departure=ksp_departure.get(edge.getLeftStop());
			arrival=ksp_arrival.get(edge.getRightStop());
			ksp_edge=new KSPEdge(edge, tt.calcTimeInMinutes(edge.getLength()*od.getConversionFactor()), false);
			ptn_extended.addEdge(departure, arrival, ksp_edge);
			if(!ptn.isDirected()){
				departure=ksp_departure.get(edge.getRightStop());
				arrival=ksp_arrival.get(edge.getLeftStop());
				ksp_edge=new KSPEdge(edge, tt.calcTimeInMinutes(edge.getLength()*od.getConversionFactor()), false);
				ptn_extended.addEdge(departure, arrival, ksp_edge);
			}
		}
	}
	
	public LinkedList<LinkedList<Edge>> getPaths(Stop origin, HashMap<Stop, Double> destinations, int k,
			double min_passengers){
		LinkedList<LinkedList<Edge>> paths=new LinkedList<LinkedList<Edge>>();
		for(Entry<LinkedList<Edge>,Double> entry:this.getWeightedPaths(origin, destinations, k).entrySet()){
			if(entry.getValue()>=min_passengers){
				paths.add(entry.getKey());
			}
		}
		return paths;
	}
	
	public HashMap<LinkedList<Edge>,Double> getWeightedPaths(Stop origin, HashMap<Stop, Double> destinations, int k){
		if(k==1){
			return getWeightedPaths(origin, destinations);
		}
		KSPNode origin_ksp;
		KSPNode destination_ksp;
		Stop destination;
		HashMap<LinkedList<Edge>,Double> paths=new HashMap<LinkedList<Edge>,Double>();
		LinkedList<Edge> path;
		KShortestPaths<KSPNode, KSPEdge> ksp;
		List<KSPEdge> path_extended;
		List<GraphPath<KSPNode,KSPEdge>> paths_extended;
		
		origin_ksp= ksp_departure.get(origin);
		ksp=new KShortestPaths<>(ptn_extended,k);
		for(Entry<Stop, Double> entry: destinations.entrySet()){
			//there have to be passengers traveling from origin to destination 
			if(entry.getValue()>0){
				destination=entry.getKey();
				destination_ksp=ksp_arrival.get(destination);
				paths_extended=ksp.getPaths(origin_ksp, destination_ksp);
				if(paths_extended != null){
					for(GraphPath<KSPNode, KSPEdge> graph_path: paths_extended){
						path_extended=graph_path.getEdgeList();
						path=new LinkedList<Edge>();
						for(KSPEdge edge_ksp: path_extended){
							if(!edge_ksp.isWaitingEdge()){
								path.add(edge_ksp.getEdge());
							}
						}
						paths.put(path,entry.getValue());
					}
				}
			}
		}
		return paths;
	}
	
	public HashMap<LinkedList<Edge>,Double> getWeightedPaths(Stop origin, HashMap<Stop, Double> destinations){
		KSPNode origin_ksp;
		KSPNode destination_ksp;
		Stop destination;
		HashMap<LinkedList<Edge>,Double> paths=new HashMap<LinkedList<Edge>,Double>();
		LinkedList<Edge> path;
		List<KSPEdge> path_extended;
		
		origin_ksp= ksp_departure.get(origin);
		for(Entry<Stop, Double> entry: destinations.entrySet()){
			//there have to be passengers traveling from origin to destination
			if(entry.getValue()>0){
				destination=entry.getKey();
				destination_ksp=ksp_arrival.get(destination);
				path_extended=DijkstraShortestPath.findPathBetween(ptn_extended, origin_ksp, destination_ksp).getEdgeList();
				if(path_extended != null){
					path=new LinkedList<>();
					for(KSPEdge edge_ksp: path_extended){
						if(!edge_ksp.isWaitingEdge()){
							path.add(edge_ksp.getEdge());
						}
					}
					paths.put(path,entry.getValue());
				}
			}
		}
		return paths;
	}
	
}

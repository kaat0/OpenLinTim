import java.util.*;

public class MinimalSpanningTree {
	private LinkedList<Edge> edges_ptn;
	private LinkedList<Edge> edges_mst;
	private HashMap<Stop, LinkedList<Edge>> stops;
	private boolean directed;
	private int max_deg;
	
//Constructor------------------------------------------------------------------
	
	public MinimalSpanningTree(PTN ptn){
		edges_ptn=new LinkedList<Edge>();
		edges_mst=new LinkedList<Edge>();
		stops=new HashMap<Stop,LinkedList<Edge>>();
		directed=ptn.isDirected();
		for(Edge edge: ptn.getEdges()){
			edges_ptn.add(edge);
		}
		for(Stop stop:ptn.getStops()){
			//Set all stops not to be leafs
			stop.setIsLeaf(false);
			stops.put(stop, new LinkedList<Edge>());
		}
		max_deg=0;
	}
	
//Getter-----------------------------------------------------------------------
	public LinkedList<Edge> getEdgesMST(){
		return edges_mst;
	}
	
	public HashMap<Stop, LinkedList<Edge>> getMST(){
		return stops;
	}
	
	public boolean isDirected(){
		return directed;
	}
	
	public int getMaxDeg(){
		return max_deg;
	}
	
//Methods----------------------------------------------------------------------
	
	public void resetStops(){
		for(Stop stop: stops.keySet()){
			stop.setIsLeaf(false);
			stop.setIsTerminal(false);
			max_deg=0;
		}
	}


	//Find MST----------------------------------------------------------------
	
	public void findMSTKruskal(){
		if(directed)
			throw new RuntimeException("Algorithm only works for undirected graphs!");
		
		//sort edges according to weight
		Collections.sort(edges_ptn);
		
		//Initialize UnionFind:
		UnionFind connected_components=new UnionFind();
		
		//Map Stop -> Node UnionFind
		TreeMap<Integer, UFNode> uf_nodes=new TreeMap<Integer, UFNode>();
		for(Stop stop: stops.keySet()){
			uf_nodes.put(stop.getIndex(), connected_components.init());
		}
		
		Stop left_stop;
		Stop right_stop;
		UFNode x;
		UFNode y;
		for(Edge edge: edges_ptn){
			left_stop=edge.getLeftStop();
			x=uf_nodes.get(left_stop.getIndex());
			right_stop=edge.getRightStop();
			y=uf_nodes.get(right_stop.getIndex());
			x=connected_components.find(x);
			y=connected_components.find(y);
			//Check if both stops belong to the same connected component, as
			//this edge then closes a circle.
			if(!x.equals(y)){
				//Edge is added to MST
				connected_components.union(x, y);
				edges_mst.add(edge);
				stops.get(left_stop).add(edge);
				stops.get(right_stop).add(edge);
			}
		}
		
		//determine leafs and maximal node-degree
		int deg;
		for(Stop stop: stops.keySet()){
			deg=stops.get(stop).size();
			if(deg==1){
				stop.setIsLeaf(true);
			}	
			if(deg>max_deg){
				max_deg=deg;
			}
		}
	}
}

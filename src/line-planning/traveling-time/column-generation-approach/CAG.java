import com.dashoptimization.*;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;
import java.io.*;

/** class representing a change and go network 
  * corresponds to a PTN and a line pool 
  */
public class CAG {
	
	/** list of OD nodes of the change and go graph*/
	protected ArrayList<Node> odNodes;
	
	/** list of line nodes of the change and go graph*/
	protected ArrayList<ArrayList<Node>> lineNodes;
	
	/** list of line edges of the change and go graph*/
	protected ArrayList<ArrayList<Edge>> lineEdges;
	
	/** list where the original edge weights are safed*/
	protected ArrayList<ArrayList<Double>> lineEdgeWeights;
	
	/** list of change edges of the change and go graph*/
	protected ArrayList<ArrayList<Edge>> changeEdges;
	
	/** list of OD edges of the change and go graph*/
	protected ArrayList<Edge> odEdges;	
	
	/** list containing lists of the line ids of the lines containing node v
		and the position of node v in the line
		for all nodes v of the PTN */
	protected ArrayList<ArrayList<int[]>> nodeLines;
	
	/** corrsponding graph in jgrapht used for shortest paths */
	protected SimpleWeightedGraph<Node,DefaultWeightedEdge> graph;
	
	/** if the weight of the line edges is updated, the corresponding values are
	  * saved in this array 
	  * used if the relaxation type is 2 or 4 
	  */
	protected double[] dual;
	
	/** if the weight of the line edges is updated, the corresponding values are
	  * saved in this array 
	  * used if the relaxation type is 1 or 3
	  */
	protected ArrayList<ArrayList<Double>> dualAllEdges;
	
	protected PTN ptn;
	
//------------------Constructor-----------------------------------------------------	
	/** initialize the change and go graph corresponding to the ptn and the line pool 
	  * @param ptn public transportation network
	  * @param pool line pool
	  */
	public CAG(PTN ptn, Pool pool){
		this.ptn = ptn;
		double WEIGHT_CHANGE = 1;
		double WEIGHT_OD = 1;
		try {
			Config config = new Config(new File("basis/Config.cnf"));
			WEIGHT_CHANGE = config.getIntegerValue("lc_traveling_time_cg_weight_change_edge");
			WEIGHT_OD = config.getIntegerValue("lc_traveling_time_cg_weight_od_edge");
		}catch(IOException e){
			System.out.println(e.getMessage());
			System.exit(1);
		}
		graph = new SimpleWeightedGraph<Node,DefaultWeightedEdge>(DefaultWeightedEdge.class);
		
		
		this.odNodes = ptn.getNodes();
		for(int i = 0; i < odNodes.size();i++){
			this.graph.addVertex(odNodes.get(i));
		}
		this.lineNodes = new ArrayList<ArrayList<Node>>(0);
		this.lineEdges = new ArrayList<ArrayList<Edge>>(0);
		this.lineEdgeWeights = new ArrayList<ArrayList<Double>>(0);	
		this.nodeLines = new ArrayList<ArrayList<int[]>>(0);
		this.dualAllEdges = new ArrayList<ArrayList<Double>>(0);
		for(int i = 0; i < ptn.getNodes().size();i++){
			this.nodeLines.add(new ArrayList<int[]>(0));
		}

		Edge edge;
		Line line;
		int id;
		Node node;
		int pos;
		
		// initialize the node lists, line edges and the nodeLines
		for(int i = 0; i < pool.getLines().size(); i++) {
			pos = 0;
			this.lineNodes.add(new ArrayList<Node>(0));
			this.lineEdges.add(new ArrayList<Edge>(0));
			this.lineEdgeWeights.add(new ArrayList<Double>(0));
			this.dualAllEdges.add(new ArrayList<Double>(0));
			line = pool.getLines().get(i);
			node = new Node(line.getNodes().get(0),line.getId(),pos);
			this.lineNodes.get(i).add(node);
			this.graph.addVertex(node);
			this.nodeLines.get(line.getNodes().get(0).getId()-1).add(new int[]{i,0});
			for(int j = 1; j < line.getNodes().size();j++) {
				pos++;
				node = new Node(line.getNodes().get(j),line.getId(),pos);
				this.lineNodes.get(i).add(node);
				edge = new Edge(line.getEdges().get(j-1).getId(),
												this.lineNodes.get(i).get(j-1), 
												this.lineNodes.get(i).get(j),
												line.getEdges().get(j-1).getWeight(),
												pool.getLines().get(i).getId());
				this.lineEdges.get(i).add(edge);
				this.lineEdgeWeights.get(i).add(edge.getWeight());
				this.dualAllEdges.get(i).add(0.0);
				this.graph.addVertex(node);
				this.graph.addEdge(edge.getLeftNode(),edge.getRightNode());
				this.graph.setEdgeWeight(graph.getEdge(edge.getLeftNode(),edge.getRightNode()),
					edge.getWeight());
												
				this.nodeLines.get(line.getNodes().get(j).getId()-1).add(new int[]{i,j});
				
			}
			
		}
		//initialize the change and od edges 
		//uncomment part for change edges if they should be used
		this.changeEdges = new ArrayList<ArrayList<Edge>>(0);
		this.odEdges = new ArrayList<Edge>(0);
		id = -2;
		for(int i = 0; i < nodeLines.size();i++) {
			//this.changeEdges.add(new ArrayList<Edge>(0));
			for(int j = 0; j < nodeLines.get(i).size()-1; j++){
				/*for(int k = j+1; k < nodeLines.get(i).size(); k++){
					edge = new Edge(id,
						this.lineNodes.get(this.nodeLines.get(i).get(j)[0])
						.get(this.nodeLines.get(i).get(j)[1]),
						this.lineNodes.get(this.nodeLines.get(i).get(k)[0])
						.get(this.nodeLines.get(i).get(k)[1]),WEIGHT_CHANGE,MAX,MIN);
					this.changeEdges.get(i).add(edge);
					this.graph.addEdge(edge.getLeftNode(),edge.getRightNode());
					this.graph.setEdgeWeight(graph.getEdge(edge.getLeftNode(),
						edge.getRightNode()),edge.getWeight());
					id--;
				}*/
				edge = new Edge(id,odNodes.get(i),
						this.lineNodes.get(this.nodeLines.get(i).get(j)[0])
						.get(this.nodeLines.get(i).get(j)[1]),WEIGHT_OD);
				this.odEdges.add(edge);
				this.graph.addEdge(edge.getLeftNode(),edge.getRightNode());
				this.graph.setEdgeWeight(graph.getEdge(edge.getLeftNode(),
					edge.getRightNode()),edge.getWeight());
				id--;
			}
			if(nodeLines.get(i).size()>0){
				edge = new Edge(id,odNodes.get(i),
						this.lineNodes.get(this.nodeLines.get(i).
						get(nodeLines.get(i).size()-1)[0]).
						get(this.nodeLines.get(i).get(nodeLines.get(i).size()-1)[1]),
						WEIGHT_OD);
				this.odEdges.add(edge);
				this.graph.addEdge(edge.getLeftNode(),edge.getRightNode());
				this.graph.setEdgeWeight(graph.getEdge(edge.getLeftNode(),
					edge.getRightNode()),edge.getWeight());
				id--;
			}	
		} 
		
		//initialize dual with zeros
		dual = new double[pool.getLines().size()];
	}
	
//---------------Getter--------------------------------------------------

	
	public ArrayList<ArrayList<Node>> getLineNodes() {
		return this.lineNodes;
	}
	
	public ArrayList<Node> getOdNodes() {
		return this.odNodes;
	}
	
	public ArrayList<ArrayList<Edge>> getLineEdges() {
		return this.lineEdges;
	}
	
	public ArrayList<ArrayList<Edge>> getChangeEdges() {
		return this.changeEdges;
	}
	
	public ArrayList<Edge> getOdEdges() {
		return this.odEdges;
	}
	
	public SimpleWeightedGraph<Node,DefaultWeightedEdge> getGraph(){
		return this.graph;
	}
	
	public double[] getDual(){
		return this.dual;
	}
	
	public ArrayList<ArrayList<Double>> getDualAllEdges(){
		return this.dualAllEdges;
	}
	
	public PTN getPTN(){
		return this.ptn;
	}
	
//----------------ToString------------------------------------------------------
	public String toString() {
		String s="";
		s = s + "OD Nodes: \n";
		for(ListIterator<Node> iter = odNodes.listIterator(); iter.hasNext();){
			s = s + iter.next() + "\n";
		}
		s = "\n" + s + "Line Nodes:" + "\n";
		for(ListIterator<ArrayList<Node>> iter1 = lineNodes.listIterator(); iter1.hasNext();){
			for(ListIterator<Node> iter2 = iter1.next().listIterator();iter2.hasNext();) {
				s = s + iter2.next() +"\n";
			}			
		}
		s = "\n" + s + "Line Edges:" + "\n";
		for(ListIterator<ArrayList<Edge>> iter1 = lineEdges.listIterator(); iter1.hasNext();){
			for(ListIterator<Edge> iter2 = iter1.next().listIterator();iter2.hasNext();) {
				s = s + iter2.next() +"\n";
			}			
		}
		s = "\n" + s + "Change Edges:" + "\n";
		for(ListIterator<ArrayList<Edge>> iter1 = changeEdges.listIterator(); iter1.hasNext();){
			for(ListIterator<Edge> iter2 = iter1.next().listIterator();iter2.hasNext();) {
				s = s + iter2.next() +"\n";
			}			
		}
		s = "\n" + s + "OD Edges:" + "\n";
		for(ListIterator<Edge> iter = odEdges.listIterator(); iter.hasNext();){
			s = s + iter.next() +"\n";			
		}   
		return s;
	}
	
//--------------Update------------------------------------------------------------
	/** function updates the weights of the graph	
	  * the weights of alle line edges has to be changed for the pricing step
	  * add value of the dual variable of the corresponding constraint
	  * used if the constraint type is 1 or 3
	  * @param corresponding line constraints needed to get the dual values
	  */	  
	public void update(ArrayList<XPRBctr> lineConstr, int w){
		double newDual;
		DefaultWeightedEdge edge;
		for(int i = 0; i < lineEdges.size(); i++){
			newDual = lineConstr.get(i).getDual()/w;
			for (int j = 0; j < lineEdges.get(i).size(); j++){
				edge = graph.getEdge(lineEdges.get(i).get(j).getLeftNode(),
												lineEdges.get(i).get(j).getRightNode());
				this.graph.setEdgeWeight(edge, lineEdgeWeights.get(i).get(j)-newDual);
			}
			this.dual[i] = newDual; 
		}
	}
	
	/** function updates the weights of the graph	
	  * the weight of alle line edges has to be changed for the pricing step
	  * add value of the dual variable of the corresponding constraint
	  * used if the constraint type is 2 or 4
	  * @param corresponding line constraints needed to get the dual values
	  */	  
	public void updateAllEdges(ArrayList<ArrayList<XPRBctr>> lineConstr, int w){
		double newDual;
		DefaultWeightedEdge edge;
		for(int i = 0; i < lineConstr.size(); i++){
			for (int j = 0; j < lineConstr.get(i).size(); j++){
				newDual = lineConstr.get(i).get(j).getDual()/w;
				edge = graph.getEdge(lineEdges.get(i).get(j).getLeftNode(),
												lineEdges.get(i).get(j).getRightNode());
				this.graph.setEdgeWeight(edge, lineEdgeWeights.get(i).get(j) - newDual);
				
				this.dualAllEdges.get(i).set(j,newDual); 
			}
		} 
	}
	
	/** function updates the weights of the graph	
	  * changes the weights according to the values in new Dual
	  * add new value and substract old one
	  * @param newDual the lth value of newDual is added to each edge of the line edges 
	  *				   corresponding to l
	  */
	public void update(){
		DefaultWeightedEdge edge;
		for(int i = 0; i < lineNodes.size(); i++){
			for (int j = 0; j < lineEdges.get(i).size(); j++){
				edge = graph.getEdge(lineEdges.get(i).get(j).getLeftNode(),
												lineEdges.get(i).get(j).getRightNode());
				this.graph.setEdgeWeight(edge, lineEdgeWeights.get(i).get(j));
				
				
			}
		}
	}
	
}

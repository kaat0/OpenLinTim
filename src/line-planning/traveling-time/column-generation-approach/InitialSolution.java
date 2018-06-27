import com.dashoptimization.*;
import java.util.*;
import java.io.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;

/** class computing an initial solution for the column generation algorithm
  * computes cover, k shortest paths, additional input solution
  * depending on the parameters "cover", "k_shortest_paths", "add_sol"
  */

public class InitialSolution {
	
	private boolean VERBOSE;
	
	protected Pool pool;
	
	protected CAG cag;
	
	protected ArrayList<int[]> od;
	
	protected ArrayList<ArrayList<Path>> paths;
	
//------Constructor--------------------------------------------------------
	public InitialSolution(Pool pool,CAG cag, boolean VERBOSE){
		this.pool = pool;
		this.cag = cag;
		this.od = cag.getPTN().getOd();
		this.paths = new ArrayList<ArrayList<Path>>(0);
		for( int i = 0; i < od.size(); i++){
			paths.add(new ArrayList<Path>(0));
		}
		this.VERBOSE = VERBOSE;
	}
	
	
//----------makePaths---------------------------------------------------------
	
	/** computes the paths corresponding to the sol of the edge cover and add sol
	  * @return list of list of s-t-paths
	  */
	public ArrayList<ArrayList<Path>> makePaths()throws IOException{
		Config config = new Config(new File("basis/Config.cnf"));
		
		if(config.getBooleanValue("lc_traveling_time_cg_cover")){
			cover();			
		}
		
		if(config.getIntegerValue("lc_traveling_time_cg_k_shortest_paths") > 0){
			kShortestPaths(config.getIntegerValue("lc_traveling_time_cg_k_shortest_paths"));
		}
		
		if(config.getBooleanValue("lc_traveling_time_cg_add_sol_1")){
			additionalSolution(config.getStringValue("lc_traveling_time_cg_add_sol_1_name"));
		}
		if(config.getBooleanValue("lc_traveling_time_cg_add_sol_2")){
			additionalSolution(config.getStringValue("lc_traveling_time_cg_add_sol_2_name"));
		}
		if(config.getBooleanValue("lc_traveling_time_cg_add_sol_3")){
			additionalSolution(config.getStringValue("lc_traveling_time_cg_add_sol_3_name"));
		}
		return paths;
	}
	
	
	/** computes a cost minimizing edge cover*/
	protected void cover(){
		XPRS.init();
		XPRB bcl = new XPRB();

		XPRBprob cover = bcl.newProb("Cover");
		
		//initialization of covering constraints for each node
		ArrayList<XPRBctr> constraints = new ArrayList<XPRBctr>(0);
		for(int i = 0; i < pool.getPTN().getEdges().size(); i++){
			constraints.add(cover.newCtr("v_"+i));
			constraints.get(i).setType(XPRB.G);
			constraints.get(i).setTerm(1);
		}
		//initialization of variables for each line 
		ArrayList<XPRBvar> y = new ArrayList<XPRBvar>(0);
			
		//initialization of objective function
		XPRBctr objective = cover.newCtr("objective");
		objective.setType(XPRB.N);
			
		ArrayList<Edge> edgeList;
		
		for (int i = 0; i < pool.getLines().size(); i++){
			y.add(cover.newVar("y_"+i, XPRB.BV, 0.0, Double.POSITIVE_INFINITY));	
			objective.setTerm(y.get(i),pool.getLines().get(i).getCost());
			edgeList = pool.getLines().get(i).getEdges();
			for(int j = 0; j < edgeList.size(); j++){
				constraints.get(edgeList.get(j).getId()-1).setTerm(y.get(i),1);
			}
		}
		
		cover.setObj(objective);
		try{
			solve(cover);
		}catch(InterruptedException e){
			System.exit(1);
		}
			
		ArrayList<Line> lines = new ArrayList<Line>(0);
		for(int i = 0; i < pool.getLines().size(); i++){
			if(y.get(i).getSol()==1){
				lines.add(pool.getLines().get(i));
			}
		}
		shortestPathsRedCAG(lines);
		String s = "Solution: The following lines are chosen: ( ";
		for(int i = 0; i < pool.getLines().size(); i++){
			if(y.get(i).getSol()==1){
				s = s + (i+1) + " ";
			}
		}
		s = s + ")";
		if(VERBOSE)
			System.out.println(s);
	}
	
	/** solves the covering problem*/
	public void solve(XPRBprob cover) throws InterruptedException {
		try {
            cover.exportProb(XPRB.LP, "cover");
        } catch (Exception e) {}
		//let cover be a minimization problem, minimize the traveling time
        cover.minim("g");
        Thread.sleep(2000);
		if (cover.getLPStat() == XPRB.MIP_INFEAS) {
			System.out.println("UNZULAESSIG!");
			System.exit(1);
		}
	}
	

	
	/** k-shortest paths*/
	protected void kShortestPaths(int K){
		ArrayList<Node> odNodes = cag.getOdNodes();
		List<GraphPath<Node,DefaultWeightedEdge>> list;
		for( int i = 0; i < od.size(); i++){
			KShortestPaths<Node,DefaultWeightedEdge> kShortestPaths 
					= new KShortestPaths<Node,DefaultWeightedEdge>(cag.getGraph(),
												odNodes.get(od.get(i)[0]), K);
			list = kShortestPaths.getPaths(odNodes.get(od.get(i)[1]));
			for(int k = 0; k < list.size(); k++){						
				paths.get(i).add(new Path(list.get(k)));
				
			}
		}
	}
	
	/** reads an additional solution and computes corresponding shortest paths 
	  * @param filename file containing a line concept (lintim format)
	  */
	protected void additionalSolution(String filename){
		try {	
			ArrayList<Line> lines = new ArrayList<Line>(0);
			int lastId = 0;
			BufferedReader in = new BufferedReader(new FileReader(filename));
			Scanner scan = new Scanner(in);
			String line; 
			String[] values;
			while(scan.hasNext()){
				line = scan.nextLine().trim();
				if(line.indexOf("#")==0)
					continue;
				if(line.indexOf("#")>-1){
					line = line.substring(0,line.indexOf("#")-1);
				}
				if(line.contains(";")){
					values = line.split(";");
					if(Integer.parseInt(values[0].trim())!=lastId&&
											Integer.parseInt(values[3].trim())>0){
						lines.add(pool.getLines().get(Integer.parseInt(values[0].trim())-1));
						lastId = Integer.parseInt(values[0].trim());
					}
				}				
			}
			in.close();
			shortestPathsRedCAG(lines);
		} catch (IOException e) {
			System.out.println("Computation of additional solution failed.");
		}
		return;
	}
	
	/** computes the reduced CAG corresponding to a line concept 
	  * and the shortest od paths
	  */
	protected void shortestPathsRedCAG(ArrayList<Line> lines){
		//create reduced line pool and reduced CAG corresponding to the chosen lines
		Pool redPool = new Pool(pool.getPTN(),lines);
		CAG redCAG = new CAG(pool.getPTN(),redPool);
			
		ArrayList<Node> odNodes = redCAG.getOdNodes();
		DijkstraShortestPath<Node,DefaultWeightedEdge> dijkstra;
		for( int i = 0; i < od.size(); i++){
			dijkstra = new DijkstraShortestPath<Node,DefaultWeightedEdge>
					(redCAG.getGraph(),odNodes.get(od.get(i)[0]),odNodes.get(od.get(i)[1]));
			paths.get(i).add(new Path(dijkstra.getPath()));
		}
	}
}

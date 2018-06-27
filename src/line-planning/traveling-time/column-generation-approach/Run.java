import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import com.dashoptimization.*;
import java.io.*;


/** main class for the column generation algorithm solving the LP relaxation
  * of the line planning problem with minimal travelling time
  */
public class Run {
	
	public static void main(String[] args) throws IOException {
		
		long totalStartTime = System.currentTimeMillis();
		
		Config config = new Config(new File("basis/Config.cnf"));
		
		ColumnGeneration colGen = new ColumnGeneration();
		
		colGen.solveRelaxation();
		
		//solveIP?
		if(config.getBooleanValue("lc_traveling_time_cg_solve_ip")){
			colGen.solveIP();
		}
			
		//Path output file?
		if(config.getBooleanValue("lc_traveling_time_cg_print_paths")){
			colGen.writePaths(colGen.getRLPM().getPaths(),"line-planning","resulting_path_"+ config.getStringValue("lc_traveling_time_cg_constraint_type") +".giv");
		}
		
		//write detailed output file of column generation
		colGen.writeHistory();
		
		System.out.println("\nTotal time: " + (System.currentTimeMillis()-totalStartTime)+"\n");
	}
}

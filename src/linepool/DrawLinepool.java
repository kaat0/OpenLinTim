import java.io.*;

public class DrawLinepool {

	public static void main(String[] args) {
		if (args.length < 1) {
			throw new RuntimeException("Error: number of arguments invalid; first "
					+ "argument must be the path to the configuration file.");
		}

		try {
			File config_file = new File(args[0]);
			boolean draw_line_concept;
			if ( args.length >= 2 && args[1].equals("true") ){
				draw_line_concept = true;
			}
			else {
				draw_line_concept = false;
			}

			System.err.print("Loading Configuration... ");
			Config config = new Config(config_file);
			System.err.println("done!");

			System.err.print("Set variables... ");
			boolean directed = !config.getBooleanValue("ptn_is_undirected");


			Line.setDirected(directed);
			Stop.setCoordinateFactorDrawing(config.getDoubleValue("lpool_coordinate_factor"));

			System.err.println("done!");

			System.err.print("Read files...");
			File stop_file = new File(config.getStringValue("default_stops_file"));
			File edge_file = new File(config.getStringValue("default_edges_file"));
			File pool_file;
			File dot_file;
			if(draw_line_concept){
				pool_file = new File(config.getStringValue("default_lines_file"));
				dot_file = new File(config.getStringValue("default_line_graph_file"));
			}
			else {
				pool_file = new File(config.getStringValue("default_pool_file"));
				dot_file = new File(config.getStringValue("default_pool_graph_file"));
			}

			PTN ptn = new PTN(directed);
			PTNCSV.fromFile(ptn, stop_file, edge_file);

			LinePool pool = new LinePool(ptn);
			LinePoolCSV.fromFile(pool, pool_file, draw_line_concept);
			System.err.println("done!");
			
			System.err.print("Writing line-pool to DOT-file...");
			LinePoolCSV.toDOTFile(pool, dot_file);
			System.err.println("done!");

		} catch (IOException e) {
			System.err.println("An error occurred while reading a file.");
			throw new RuntimeException(e);
		}
	}

}

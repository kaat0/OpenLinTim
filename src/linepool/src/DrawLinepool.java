import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.exception.LinTimException;
import net.lintim.io.ConfigReader;
import net.lintim.util.Config;
import net.lintim.util.Logger;

import java.io.*;

public class DrawLinepool {

    private static final Logger logger = new Logger(DrawLinepool.class);

	public static void main(String[] args) {
		if (args.length < 1) {
			throw new ConfigNoFileNameGivenException();
		}

		try {
			logger.info("Begin reading configuration");
			boolean draw_line_concept;
            draw_line_concept = args.length >= 2 && args[1].equals("true");

			Config config = new ConfigReader.Builder(args[0]).build().read();

			logger.debug("Set variables... ");
			boolean directed = !config.getBooleanValue("ptn_is_undirected");


			Line.setDirected(directed);
			Stop.setCoordinateFactorDrawing(config.getDoubleValue("lpool_coordinate_factor"));

			logger.info("Finished reading configuration");

			logger.info("Begin reading input files");
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
			logger.info("Finished reading input files");

			logger.info("Begin writing output files");
			LinePoolCSV.toDOTFile(pool, dot_file);
			logger.info("Finished writing output files");

		} catch (IOException e) {
			throw new LinTimException(e.getMessage());
		}
	}

}

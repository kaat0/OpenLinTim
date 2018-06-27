import java.io.File;
import java.io.IOException;


public class CreateLinepoolSP {
	public static void main(String[] args) {
		if (args.length != 1) {
			throw new RuntimeException("Error: number of arguments invalid; first "
					+ "argument must be the path to the configuration file.");
		}

		try {
			File config_file = new File(args[0]);

			System.err.print("Loading Configuration... ");
			Config config = new Config(config_file);
			System.err.println("done!");

			System.err.print("Set variables... ");
			boolean directed = !config.getBooleanValue("ptn_is_undirected");
			double ptn_speed = config.getDoubleValue("gen_vehicle_speed");
			double waiting_time = config.getDoubleValue("ptn_stop_waiting_time");
			double conversion_factor_length = config.getDoubleValue("gen_conversion_length");
			int number_shortest_paths = config.getIntegerValue("lpool_number_shortest_paths");

			String ean_model_weight_drive = config.getStringValue("ean_model_weight_drive");
			String ean_model_weight_wait = config.getStringValue("ean_model_weight_wait");
			int minimal_wait_time = config.getIntegerValue("ean_default_minimal_waiting_time");
			int maximal_wait_time = config.getIntegerValue("ean_default_maximal_waiting_time");

			Line.setDirected(directed);
			Line.setCostsFixed(config.getDoubleValue("lpool_costs_fixed"));
			Line.setCostsLength(config.getDoubleValue("lpool_costs_length"));
			Line.setCostsEdges(config.getDoubleValue("lpool_costs_edges"));
			Line.setCostsVehicles(config.getDoubleValue("lpool_costs_vehicles"));
			Line.setPeriodLength(config.getIntegerValue("period_length"));
			Line.setMinTurnaroundTime(config.getIntegerValue("vs_turn_over_time"));
			Line.setWaitingTimeInStation(ean_model_weight_wait, minimal_wait_time, maximal_wait_time);

			LinePoolCSV.setPoolHeader(config.getStringValue("lpool_header"));
			LinePoolCSV.setPoolCostHeader(config.getStringValue("lpool_cost_header"));

			System.err.println("done!");

			System.err.print("Read files...");
			File stop_file = new File(config.getStringValue("default_stops_file"));
			File edge_file = new File(config.getStringValue("default_edges_file"));
			File load_file = new File(config.getStringValue("default_loads_file"));
			File od_file = new File(config.getStringValue("default_od_file"));

			PTN ptn = new PTN(directed);

			PTNCSV.fromFile(ptn, stop_file, edge_file, load_file, ean_model_weight_drive);

			OD od = new OD(ptn, ptn_speed, waiting_time, conversion_factor_length);

			ODCSV.fromFile(ptn, od, od_file);

			System.err.println("done!");
			
			System.err.println("Create line-pool by shortest paths...");
			LinePool pool = new LinePool(ptn);
			pool.poolFromKSP(od, number_shortest_paths);

			System.err.print("Try to finalize pool...");
			pool.finalizeSP();
			System.err.println("done!");
			
			//Output Pool
			System.err.print("\tWriting new line-pool to file...");
			LinePoolCSV.toFile(pool, 
					new File(config.getStringValue("default_pool_file")),
					new File(config.getStringValue("default_pool_cost_file")));
			System.err.println("done!");
			
			
			System.err.println("done!");


		} catch (IOException e) {
			System.err.println("An error occurred while reading a file.");
			throw new RuntimeException(e);
		}
	}
}

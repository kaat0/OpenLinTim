import java.io.*;

public class WriteCostFile {

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

			String ean_model_weight_drive = config.getStringValue("ean_model_weight_drive");
			String ean_model_weight_wait = config.getStringValue("ean_model_weight_wait");
			int minimal_wait_time = config.getIntegerValue("ean_default_minimal_waiting_time");
			int maximal_wait_time = config.getIntegerValue("ean_default_maximal_waiting_time");
			
			Line.setCostsFixed(config.getDoubleValue("lpool_costs_fixed"));
			Line.setCostsLength(config.getDoubleValue("lpool_costs_length"));
			Line.setCostsEdges(config.getDoubleValue("lpool_costs_edges"));
			Line.setDirected(directed);
			Line.setWaitingTimeInStation(ean_model_weight_wait, minimal_wait_time, maximal_wait_time);

			LinePoolCSV.setPoolCostHeader(config.getStringValue("lpool_cost_header"));

			System.err.println("done!");

			System.err.print("Read files...");
			File stop_file = new File(config.getStringValue("default_stops_file"));
			File edge_file = new File(config.getStringValue("default_edges_file"));
			File pool_file = new File(config.getStringValue("default_pool_file"));
			Line.setCostsVehicles(config.getDoubleValue("lpool_costs_vehicles"));
			Line.setPeriodLength(config.getIntegerValue("period_length"));
			Line.setMinTurnaroundTime(config.getIntegerValue("vs_turn_over_time"));
			File cost_file = new File(config.getStringValue("default_pool_cost_file"));

			PTN ptn = new PTN(directed);
			PTNCSV.fromFile(ptn, stop_file, edge_file, ean_model_weight_drive);

			LinePool pool = new LinePool(ptn);
			LinePoolCSV.fromFile(pool, pool_file);
			System.err.println("done!");

			System.err.print("Writing pool-cost...");
			LinePoolCSV.toCostFile(pool, cost_file);
			System.err.println("done!");

		} catch (IOException e) {
			System.err.println("An error occurred while reading a file.");
			throw new RuntimeException(e);
		}
	}

}

import java.io.File;
import java.io.IOException;


public class CreateOD {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new RuntimeException("Error: number of arguments invalid; first " +
					"argument must be the path to the configuration file.");
		}

		try {
			File config_file = new File(args[0]);

			System.err.print("Loading Configuration... ");
			Config config = new Config(config_file);
			System.err.println("done!");

			System.err.print("Set variables... ");
			boolean directed = !config.getBooleanValue("ptn_is_undirected");
			double radius = config.getDoubleValue("sl_radius");

			Distance distance;
			if (config.getStringValue("sl_distance").equals("euclidean_norm")) {
				distance = new EuclideanNorm();
			} else {
				throw new IOException("Distance not defined.");
			}

			double ptn_speed = config.getDoubleValue("gen_vehicle_speed");
			double conversion_factor_length = config.getDoubleValue("gen_conversion_length");
			double conversion_factor_coordinates = config.getDoubleValue("gen_conversion_coordinates");
			double waiting_time = config.getDoubleValue("ptn_stop_waiting_time");
			double od_network_acceleration = config.getDoubleValue("od_network_acceleration");
			boolean use_network_distance = config.getBooleanValue("od_use_network_distance");
			boolean remove_uncovered_demand_points = config.getBooleanValue("od_remove_uncovered_demand_points");

			ODCSV.setHeader(config.getStringValue("od_header"));

			System.err.println("done!");

			System.err.print("Read files...");
			File stop_file = new File(config.getStringValue("default_stops_file"));
			File edge_file = new File(config.getStringValue("default_edges_file"));
			File demand_file = new File(config.getStringValue("default_demand_file"));

			PTN ptn = new PTN(directed);
			Demand demand = new Demand();

			PTNCSV.fromFile(ptn, stop_file, edge_file);
			DemandCSV.fromFile(demand, demand_file);
			System.err.println("done!");

			System.err.print("Calculate od-matrix...");
			if (remove_uncovered_demand_points)
				demand.removeUncoveredDemandPoint(ptn, distance, radius);
			PassengerDistribution passenger_distribution =
					new PassengerDistribution(demand, ptn, distance, od_network_acceleration, waiting_time,
							conversion_factor_length, conversion_factor_coordinates, ptn_speed, use_network_distance);
			OD od_matrix = passenger_distribution.createOD();
			System.err.println("done!");

			System.err.print("Writing od-matrix to file...");
			ODCSV.toFile(od_matrix, ptn, new File(config.getStringValue("default_od_file")));
			System.err.println("done!");

		} catch (IOException e) {
			System.err.println("An error occurred while reading a file.");
			throw new RuntimeException(e);
		}
	}

}

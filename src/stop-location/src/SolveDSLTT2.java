import java.io.*;
import java.util.Iterator;
import java.lang.*;
import java.util.*;

public class SolveDSLTT2 {

	public static void main(String[] args) {
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
			boolean destruction_allowed = config.getBooleanValue("sl_destruction_allowed");
			double radius = config.getDoubleValue("sl_radius");
			int waiting_time = config.getIntegerValue("sl_waiting_time");
			double acceleration = config.getDoubleValue("sl_acceleration");
			double deceleration = config.getDoubleValue("sl_deceleration");
			double speed = config.getDoubleValue("gen_vehicle_speed");

			Candidate.setDefault_name(config.getStringValue("sl_new_stop_default_name"));

			Stop.setHeader(config.getStringValue("stops_header"));
			Edge.setHeader(config.getStringValue("edges_header"));

			Distance distance;
			if (config.getStringValue("sl_distance").equals("euclidean_norm")) {
				distance = new EuclideanNorm();
			} else {
				throw new IOException("Distance not defined.");
			}
			System.err.println("done!");

			TravelingTime tt = new TravelingTime(acceleration, speed, deceleration, true);

			System.err.print("Read files...");
			File existing_stop_file = new File(config.getStringValue("default_existing_stop_file"));
			File existing_edge_file = new File(config.getStringValue("default_existing_edge_file"));
			File demand_file = new File(config.getStringValue("default_demand_file"));

			PTN ptn = new PTN(directed);
			Demand demand = new Demand();

			PTNCSV.fromFile(ptn, existing_stop_file, existing_edge_file);
			DemandCSV.fromFile(demand, demand_file);
			System.err.println("done!");

			if (destruction_allowed)
				ptn.removeDestructableStations();

			System.err.print("Calculate fds...");
			FiniteDominatingSet fds = new FiniteDominatingSet(ptn, demand, distance, radius, destruction_allowed, false);
			System.err.println("done!");

			System.err.print("The number of candidates is: ");
			System.err.println(fds.getNumberOfCandidates());

			System.err.print("Calculate Candidate Edges...");
			CandidateEdgeSet ces = new CandidateEdgeSet(ptn, fds);
			System.err.println("done!");

			System.err.print("The number of candidate edges is: ");
			System.err.println(ces.getNumberOfCandidateEdges());

			System.err.print("Setting up IP-Formulation...");
			DSLTT2 dsltt = new DSLTT2(fds, ces, demand, waiting_time, tt, distance);
			System.err.println("done!");


			System.err.print("Solving IP-Formulation...");
			long time = System.currentTimeMillis();
			dsltt.solve();
			System.err.println("Done in " + (System.currentTimeMillis() - time) + " milliseconds!");

			System.err.print("Writing ptn to files...");
			PTNCSV.toFile(dsltt.getNewPTN(), new File(config.getStringValue("default_stops_file")), new File(config.getStringValue("default_edges_file")));
			System.err.println("done!");

		} catch (IOException e) {
			System.err.println("An error occurred while reading a file.");
			throw new RuntimeException(e);
		}
	}

}


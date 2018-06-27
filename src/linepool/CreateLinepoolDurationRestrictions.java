import java.io.*;
import java.lang.reflect.InvocationTargetException;

/**
 */
public class CreateLinepoolDurationRestrictions {

	public static void main(String[] args) throws IllegalAccessException, InstantiationException,
			ClassNotFoundException {
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
			double ratio = config.getDoubleValue("lpool_ratio_od");
			int max_iterations = config.getIntegerValue("lpool_max_iterations");
			int vs_turnover_time = config.getIntegerValue("vs_turn_over_time");
			int maximum_buffer_time = config.getIntegerValue("lpool_restricted_maximum_buffer_time");
			int period_length = config.getIntegerValue("period_length");
			boolean allow_lines_half_period = config.getBooleanValue("lpool_restricted_allow_half_period");
			String ean_model_weight_drive = config.getStringValue("ean_model_weight_drive");
			String ean_model_weight_wait = config.getStringValue("ean_model_weight_wait");
			int minimal_wait_time = config.getIntegerValue("ean_default_minimal_waiting_time");
			int maximal_wait_time = config.getIntegerValue("ean_default_maximal_waiting_time");
			String lc_solver_name = config.getStringValue("lc_solver");

			Stop.setCoordinateFactorConversion(config.getDoubleValue("gen_conversion_coordinates"));

			Line.setDirected(directed);
			Line.setCostsFixed(config.getDoubleValue("lpool_costs_fixed"));
			Line.setCostsLength(config.getDoubleValue("lpool_costs_length"));
			Line.setCostsEdges(config.getDoubleValue("lpool_costs_edges"));
			Line.setCostsVehicles(config.getDoubleValue("lpool_costs_vehicles"));
			Line.setMinimumEdges(config.getIntegerValue("lpool_min_edges"));
			Line.setMinimumDistance(config.getIntegerValue("lpool_min_distance_leaves"));
			Line.setRestrictLineDurationPeriodically(true);
			Line.setPeriodLength(period_length);
			Line.setMinTurnaroundTime(vs_turnover_time);
			Line.setPeriodicRestrictions(period_length-vs_turnover_time-maximum_buffer_time, period_length-vs_turnover_time);
			Line.setHalfPeriodRestrictions(period_length/2.-vs_turnover_time-maximum_buffer_time, period_length/2.-vs_turnover_time);
			Line.setAllowHalfPeriodLength(allow_lines_half_period);
			Line.setWaitingTimeInStation(ean_model_weight_wait, minimal_wait_time, maximal_wait_time);

			LinePool.setNodeDegreeRatio(config.getDoubleValue("lpool_node_degree_ratio"));
			LinePool.setMinCoverFactor(config.getIntegerValue("lpool_min_cover_factor"));
			LinePool.setMaxCoverFactor(config.getDoubleValue("lpool_max_cover_factor"));

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

			System.err.print("Iterate until line-pool is feasible for lc-line-planning...");
			MinimalSpanningTree mst = new MinimalSpanningTree(ptn);
			LinePool pool = new LinePool(ptn);
			int[] line_concept = null;
			boolean feasible = false;
			boolean improvement;
			int count = 0;

			// Main Algorithm
			while (count < max_iterations && !feasible) {
				count++;
				System.err.println("\n\tIteration: "+count);

				//Reset edge-preference
				ptn.resetEdges();

				// Set preferred edges
				// Initialization: Preference by OD
				if (count == 1) {
					try {
						for (Edge edge : od.calcSignificantEdges(ratio)) {
							edge.setPreferred(true);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				// Else: Preference by Line-Concept if not yet feasible
				else {
					line_concept = CostTransformer.transformAndSolve(ptn, pool, lc_solver_name);
					feasible = pool.preferProblematicEdges(line_concept);
					if (feasible) {
						break;
					}
				}

				// Create Line-Pool
				improvement = true;
				while (improvement && ptn.hasPreferredEdge()) {
					mst = new MinimalSpanningTree(ptn);
					mst.findMSTKruskal();
					improvement = pool.poolFromMST(mst);
				}


				// Output Pool
				pool.finalize();
				System.err.print("\tWriting new line-pool to file...");
				LinePoolCSV.toFile(pool,
						new File(config.getStringValue("default_pool_file")),
						new File(config.getStringValue("default_pool_cost_file")));
				System.err.println("done!");
			}

			//Wrap-Up

			// Output Pool
			pool.finalize();
			System.err.print("\tWriting new line-pool to file...");
			LinePoolCSV.toFile(pool,
					new File(config.getStringValue("default_pool_file")),
					new File(config.getStringValue("default_pool_cost_file")));
			System.err.println("done!");


			//Check feasibility after adding shortest paths
			line_concept = CostTransformer.transformAndSolve(ptn, pool, lc_solver_name);
			feasible = pool.preferProblematicEdges(line_concept);


			if (!feasible) {
				System.err.println("\tMaximal number of iterations has been reached. "
						+ "No feasible solution has been found.");
			}
			else{
				System.err.println("\tA feasible solution has been found!");
			}

			System.err.println("done!");

		} catch (IOException e) {
			System.err.println("An error occurred while reading a file.");
			throw new RuntimeException(e);
		} catch (NoSuchMethodException | InvocationTargetException e) {
			System.err.println("An error occurred while instantiating the solver");
			throw new RuntimeException(e);
		}
	}

}

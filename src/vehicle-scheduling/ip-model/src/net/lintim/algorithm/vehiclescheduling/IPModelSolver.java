package net.lintim.algorithm.vehiclescheduling;

import net.lintim.algorithm.Dijkstra;
import net.lintim.exception.LinTimException;
import net.lintim.exception.SolverNotSupportedException;
import net.lintim.model.*;
import net.lintim.model.impl.ArrayListGraph;
import net.lintim.model.vehiclescheduling.TripConnection;
import net.lintim.model.vehiclescheduling.TripNode;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.Pair;
import net.lintim.util.SolverType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.lintim.util.vehiclescheduling.Constants.MINUTES_PER_HOUR;
import static net.lintim.util.vehiclescheduling.Constants.SECONDS_PER_MINUTE;

/**
 * Generic class for finding a vehicle schedule using an ip solver. Should be inherited by an actual implementation.
 * Use {@link #getVehicleSchedulingIpSolver(SolverType)} to get an actual implementation.
 */
public abstract class IPModelSolver {
	static Logger logger = Logger.getLogger("net.lintim.algorithm.vehiclescheduling.IPModelSolver");

	/**
	 * Compute an optimal vehicle schedule for the given trip graph.
	 * @param tripGraph the trip graph, i.e., a graph containing all trips as nodes and the connections as edges.
	 * @param useDepot whether to use a depot
	 * @param timeLimit the timelimit for the computation. -1 corresponds to no timelimit
	 * @param logLevel the log level to use
	 * @return the found vehicle schedule.
	 */
	public abstract VehicleSchedule solveVehicleSchedulingIPModel(Graph<TripNode, TripConnection> tripGraph, boolean
			useDepot, int timeLimit, Level logLevel);

	/**
	 * Compute an optimal vehicle schedule for the given trips.
	 * @param ptn the underlying ptn
	 * @param trips the trips to cover
	 * @param config the config to read necessary values
	 * @return the found vehicle schedule
	 */
	public VehicleSchedule solveVehicleSchedulingIPModel(Graph<Stop, Link> ptn, Collection<Trip> trips,
	                                                     Config config) {
		logger.log(LogLevel.DEBUG, "Computing trip graph");
		Graph<TripNode, TripConnection> tripGraph = computeTripGraph(ptn, trips, config);
		boolean useDepot = config.getIntegerValue("vs_depot_index") != -1;
		int timeLimit = config.getIntegerValue("vs_timelimit");
		Level logLevel = config.getLogLevel("console_log_level");
		return solveVehicleSchedulingIPModel(tripGraph, useDepot, timeLimit, logLevel);
	}

	/**
	 * Get the ip model solver for the given solver type.
	 * @param solverType the solver type to use
	 * @return the ip model class
	 */
	public static IPModelSolver getVehicleSchedulingIpSolver(SolverType solverType) throws ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		String solverClassName;
		switch (solverType) {
			case GUROBI:
				logger.log(LogLevel.DEBUG, "Will use Gurobi for optimization");
				solverClassName = "net.lintim.algorithm.vehiclescheduling.IPModelGurobi";
				break;
			default:
				throw new SolverNotSupportedException(solverType.toString(), "vehicle scheduling ip model");
		}
		Class<?> solverClass = Class.forName(solverClassName);
		return (IPModelSolver) solverClass.newInstance();
	}

	private static Graph<TripNode, TripConnection> computeTripGraph(Graph<Stop, Link> ptn, Collection<Trip> trips,
	                                                                Config config) {
		// First, read all the values from the provided config
		double factorLength = config.getDoubleValue("vs_eval_cost_factor_empty_trips_length");
		double factorTime = config.getDoubleValue("vs_eval_cost_factor_empty_trips_duration") / MINUTES_PER_HOUR;
		double vehicleCost = config.getDoubleValue("vs_vehicle_costs");
		int depotIndex = config.getIntegerValue("vs_depot_index");
		double turnoverTime = config.getDoubleValue("vs_turn_over_time");
		boolean useDepot = depotIndex != -1;
		// Check whether the depot index is present in the ptn
		if (useDepot && ptn.getNode(depotIndex) == null) {
			throw new LinTimException("The ptn node with id " + depotIndex + " could not be found but should be the " +
					"depot!");
		}
		Function<Pair<Double, Double>, Double> connectionObjective = pair ->
				factorLength * pair.getFirstElement() + factorTime * pair.getSecondElement();
		HashMap<Integer, HashMap<Integer, Pair<Double, Double>>> distanceTimeMap = computeStationDistances(ptn);
		// Now we can create the trip graph
		Graph<TripNode, TripConnection> tripGraph = new ArrayListGraph<>();
		TripNode depot = new TripNode(0, null, true);
		tripGraph.addNode(depot);
		int tripIndex = 1;
		int connectionIndex = 1;
		for (Trip trip : trips) {
			TripNode node = new TripNode(tripIndex, trip, false);
			tripIndex += 1;
			tripGraph.addNode(node);
			TripConnection fromDepot = new TripConnection(connectionIndex, depot, node, vehicleCost + (useDepot ?
					connectionObjective.apply(distanceTimeMap.get(depotIndex).get(trip.getStartStopId())) : 0));
			connectionIndex += 1;
			TripConnection toDepot = new TripConnection(connectionIndex, node, depot, useDepot ?
					connectionObjective.apply(distanceTimeMap.get(trip.getEndStopId()).get(depotIndex)) : 0);
			connectionIndex += 1;
			tripGraph.addEdge(fromDepot);
			tripGraph.addEdge(toDepot);
		}
		// Now determine the compatibilities and add the respective edges
		for(TripNode origin : tripGraph.getNodes()) {
			if(origin.isDepot()) {
				continue;
			}
			HashMap<Integer, Pair<Double, Double>> originDistanceTimeMap = distanceTimeMap.get(origin.getTrip().getEndStopId());
			for(TripNode destination : tripGraph.getNodes()) {
				if(origin.equals(destination) || destination.isDepot()) {
					continue;
				}
				double timeDistanceBetweenTrips = (destination.getTrip().getStartTime() - origin.getTrip().getEndTime
						()) / SECONDS_PER_MINUTE;
				double timeToDrive = originDistanceTimeMap.get(destination.getTrip().getStartStopId()).getSecondElement();
				if(timeDistanceBetweenTrips >= timeToDrive + turnoverTime) {
					double distanceBetweenTrips = originDistanceTimeMap.get(destination.getTrip().getStartStopId())
							.getFirstElement();
					tripGraph.addEdge(new TripConnection(connectionIndex, origin, destination, connectionObjective
							.apply(new Pair<>(distanceBetweenTrips, timeDistanceBetweenTrips))));
					connectionIndex += 1;
				}
			}
		}
		return tripGraph;
	}

	static VehicleSchedule computeSchedule(Collection<TripConnection> usedConnections, boolean useDepot) {
		VehicleSchedule schedule = new VehicleSchedule();
		int vehicleId = 1;
		int tripId = 1;
		// Look for all connections starting in the depot. These are the start of all vehicle tours
		List<TripConnection> outgoingDepotConnections = usedConnections.stream().filter(tripConnection ->
				tripConnection.getLeftNode().isDepot()).collect(Collectors.toList());
		while (!outgoingDepotConnections.isEmpty()) {
			Circulation circulation = new Circulation(vehicleId);
			VehicleTour tour = new VehicleTour(vehicleId);
			circulation.addVehicle(tour);
			vehicleId += 1;
			TripConnection currentEdge = outgoingDepotConnections.get(0);
			outgoingDepotConnections.remove(currentEdge);
			while (!currentEdge.getRightNode().isDepot()) {
				Trip sourceTrip = currentEdge.getLeftNode().getTrip();
				Trip targetTrip = currentEdge.getRightNode().getTrip();
				if(!currentEdge.getLeftNode().isDepot()) {
					// Create the empty trip and add it
					Trip emptyTrip = new Trip(sourceTrip.getEndAperiodicEventId(), sourceTrip
							.getEndPeriodicEventId(), sourceTrip.getEndStopId(), sourceTrip.getEndTime(),
							targetTrip.getStartAperiodicEventId(), targetTrip.getStartPeriodicEventId(), targetTrip
							.getStartStopId(), targetTrip.getStartTime(), -1, TripType.EMPTY);
					tour.addTrip(tripId, emptyTrip);
					tripId += 1;
				}
				tour.addTrip(tripId, targetTrip);
				tripId += 1;
				TripNode lastTripNode = currentEdge.getRightNode();
				currentEdge = usedConnections.stream().filter(tripConnection -> tripConnection.getLeftNode().equals
						(lastTripNode)).findAny().orElse(null);
				if(currentEdge == null) {
					logger.log(LogLevel.ERROR, "Could not find next trip!");
					throw new LinTimException("Could not find next trip!");
				}
			}
			schedule.addCirculation(circulation);
		}
		return schedule;
	}

	private static HashMap<Integer, HashMap<Integer, Pair<Double, Double>>> computeStationDistances(Graph<Stop, Link>
			                                                                                                ptn) {
		// TODO: Should we use another approach, to model the objective function regarding empty kilometers and time
		// TODO: correctly? This will choose a longer path, that needs less time, even if it is worth in the objective
		// TODO: What we really need: The shortest path w.r.t the objective, that is not too long w.r.t the duration
		// For now, mimic the behavior of the canal-based vehicle schedules.
		// See #264 in gitlab.
		Function<Link, Double> lengthFunction = link -> (double) link.getLowerBound();
		HashMap<Integer, HashMap<Integer, Pair<Double, Double>>> returnMap = new HashMap<>();
		for(Stop origin : ptn.getNodes()) {
			returnMap.put(origin.getId(), new HashMap<>());
			Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(ptn, origin, lengthFunction);
			dijkstra.computeShortestPaths();
			for(Stop destination : ptn.getNodes()) {
				// Compute the distance
				if(dijkstra.getDistance(destination) == 0) {
					returnMap.get(origin.getId()).put(destination.getId(), new Pair<>(0., 0.));
				}
				else {
					double distance = dijkstra.getPath(destination).getEdges().stream().mapToDouble(Link::getLength).sum();
					double time = dijkstra.getDistance(destination);
					returnMap.get(origin.getId()).put(destination.getId(), new Pair<>(distance, time));
				}
			}
		}
		return returnMap;
	}

}

package net.lintim.evaluation;

import net.lintim.algorithm.Dijkstra;
import net.lintim.exception.LinTimException;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.Pair;
import net.lintim.util.Statistic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Class for evaluating a vehicle schedule
 */
public class VehicleScheduleEvaluator {
	private static Logger logger = Logger.getLogger("net.lintim.evaluation.VehicleScheduleEvaluator");
	private static final int minutesPerHour = 60;
	private static final int secondsPerMinute = 60;

	public static void evaluateVehicleSchedule(VehicleSchedule vehicleSchedule, Collection<Trip> trips,
	                                           Graph<Stop, Link> ptn, LinePool lineConcept, Statistic statistic,
	                                           Config config){
		//Read parameters from config
		logger.log(LogLevel.INFO, "Reading parameters needed for evaluation");
		int depotIndex = config.getIntegerValue("vs_depot_index");
		double vehicleSpeed = config.getDoubleValue("gen_vehicle_speed");
		//Convert cost factors to cost per minute, not hour
		double costFactorFullDuration = config.getDoubleValue("vs_eval_cost_factor_full_trips_duration") /
				minutesPerHour;
		double costFactorEmptyDuration = config.getDoubleValue("vs_eval_cost_factor_empty_trips_duration") / minutesPerHour;
		double costFactorFullLength = config.getDoubleValue("vs_eval_cost_factor_full_trips_length");
		double costFactorEmptyLength = config.getDoubleValue("vs_eval_cost_factor_empty_trips_length");
		double costPerVehicle = config.getDoubleValue("vs_vehicle_costs");
		int turnOverTime = config.getIntegerValue("vs_turn_over_time");
		logger.log(LogLevel.INFO, "Finished reading parameters");
		logger.log(LogLevel.INFO, "Begin evaluation");
		int numberOfCirculations = vehicleSchedule.getCirculations().size();
		int numberOfUsedVehicles = 0;
		double emptyTripDistanceWithoutDepot = 0;
		double emptyTripDistanceDepot = 0;
		double fullTripDistance = 0;
		double emptyTripDurationWithoutDepot = 0;
		double emptyTripDurationDepot = 0;
		double fullTripDuration = 0;
		boolean feasibility = true;
		int numberOfEmptyTripsWithoutDepot = 0;
		int numberOfEmptyTripsDepot = 0;
		double minWaitingTimeInStation = Double.POSITIVE_INFINITY;
		double maxWaitingTimeInStation = 0;
		double sumWaitingTimeInStation = 0;
		int numberOfWaitingTimesInStation = 0;
		//First, calculate the distances between the stations and for the lines
		// Store by stop ids, first the duration (in minutes), second the length (in kilometers)
		HashMap<Pair<Integer, Integer>, Pair<Double, Double>> shortestPathLengths = new HashMap<>();
		Function<Link, Double> lengthFunction = l -> (double) l.getLowerBound();
		for(Stop startStop : ptn.getNodes()){
			Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(ptn, startStop, lengthFunction);
			dijkstra.computeShortestPaths();
			for(Stop endStop : ptn.getNodes()){
				double pathLength = startStop.equals(endStop) ? 0 : dijkstra.getPath(endStop).getEdges().stream()
						.mapToDouble(Link::getLength).sum();
				shortestPathLengths.put(new Pair<>(startStop.getId(), endStop.getId()), new Pair<>(dijkstra
						.getDistance(endStop), pathLength));
			}
		}
		HashMap<Integer, Double> lineLength = new HashMap<>();
		for(Line line : lineConcept.getLines()){
			if(line.getFrequency() > 0){
				double sumOfLength = 0;
				for(Link link : line.getLinePath().getEdges()){
					sumOfLength += link.getLength();
				}
				lineLength.put(line.getId(), sumOfLength);
			}
		}
		for(Circulation circulation : vehicleSchedule.getCirculations()){
			numberOfUsedVehicles += circulation.getVehicleTourList().size();
			for(VehicleTour vehicleTour : circulation.getVehicleTourList()){
				//Store the first and last trip for special consideration later on
				List<Trip> tripList = vehicleTour.getTripList();
				Trip firstTrip = tripList.get(0);
				Trip lastTrip = tripList.get(tripList.size()-1);
				int currentTime = Integer.MIN_VALUE;
				int currentStopId = -1;
				for(Trip trip : tripList){
					if (currentTime > trip.getStartTime()) {
						logger.log(LogLevel.WARN, "Moved backwards in time, trip " + trip + " starts before the last " +
								"trip ended!");
						feasibility = false;
					}
					if (currentStopId != -1 && trip.getStartStopId() != currentStopId) {
						logger.log(LogLevel.WARN, "The last trip ended at " + currentStopId + " but the current trip " +
								trip + " starts at a different stop!");
						feasibility = false;
					}
					if (trip.getTripType() == TripType.TRIP && !trips.remove(trip)) {
						logger.log(LogLevel.WARN, "Could not find trip " + trip + " from the vehicle schedule in the" +
								" list of read trips. Please check your Trip file!");
						feasibility = false;
					}
					//First handle the case we have the first or the last trip
					if(trip.equals(firstTrip)){
						//Should we consider a depot?
						if(depotIndex != -1){
							int firstStopIdInVehicleTour = trip.getTripType() == TripType.TRIP ? trip.getStartStopId() :
									trip.getEndStopId();
							if(firstStopIdInVehicleTour != depotIndex){
								//We do not start in the depot, we need to get there
								numberOfEmptyTripsDepot += 1;
								emptyTripDistanceDepot += shortestPathLengths.get(new Pair<>(depotIndex,
										firstStopIdInVehicleTour)).getSecondElement();
								emptyTripDurationDepot += shortestPathLengths.get(new Pair<>(depotIndex,
										firstStopIdInVehicleTour)).getFirstElement();
							}
						}
						if(trip.getTripType() == TripType.EMPTY){
							//If the trip is an empty trip, we have considered everything. Otherwise we still need to
							// process the "actual" first trip from the given vehicle schedule
							continue;
						}
					}
					else if(trip.equals(lastTrip)){
						//Should we consider a depot?
						if(depotIndex != -1){
							int lastStopIdInVehicleTour = trip.getTripType() == TripType.TRIP ? trip.getEndStopId() :
									trip.getStartStopId();
							if(lastStopIdInVehicleTour != depotIndex){
								//We do not end in the depot, we need to get there
								numberOfEmptyTripsDepot += 1;
								emptyTripDistanceDepot += shortestPathLengths.get(new Pair<>(lastStopIdInVehicleTour,
										depotIndex)).getSecondElement();
								emptyTripDurationDepot += shortestPathLengths.get(new Pair<>(lastStopIdInVehicleTour,
										depotIndex)).getFirstElement();
							}
						}
						if(trip.getTripType() == TripType.EMPTY){
							//If the trip is an empty trip, we have considered everything. Otherwise we still need to
							// process the "actual" last trip from the given vehicle schedule
							continue;
						}
					}
					//Now process an ordinary trip
					boolean isEmptyTrip = trip.getTripType() == TripType.EMPTY;
					double tripDuration;
					double tripLength;
					double minTripDuration;
					if(isEmptyTrip){
						//We need to skip empty trips, if the start or stop station is -1. These are depot trips from
						// a model where no depot was considered
						if(trip.getStartStopId() == -1 || trip.getEndStopId() == -1){
							tripDuration = 0;
							tripLength = 0;
							minTripDuration = 0;
						}
						else {
							//Convert trip duration to minutes, from seconds
							tripDuration = (trip.getEndTime() - trip.getStartTime()) / (double) secondsPerMinute;
							tripLength = shortestPathLengths.get(new Pair<>(trip.getStartStopId(), trip.getEndStopId
									())).getSecondElement();
							minTripDuration = shortestPathLengths.get(new Pair<>(trip.getStartStopId(), trip
									.getEndStopId())).getFirstElement() + turnOverTime;
						}
					}
					else {
						tripDuration = (trip.getEndTime() - trip.getStartTime()) / (double) secondsPerMinute;
						minTripDuration = shortestPathLengths.get(new Pair<>(trip.getStartStopId(), trip
								.getEndStopId())).getFirstElement();
						try {
							tripLength = lineLength.get(trip.getLineId());
						} catch (NullPointerException e){
							throw new LinTimException("Used line " + trip.getLineId() + " in the vehicle schedule, " +
									"but this line has frequency 0!");
						}
					}
					if(!isEmptyTrip){
						fullTripDuration += tripDuration;
						fullTripDistance += tripLength;
					}
					else{
						//Check if the empty trip is possible, i.e., if the trip duration is enough to cover the
						// distance of the trip
						if(tripDuration < minTripDuration){
							logger.log(LogLevel.WARN, "Found a trip with insufficient time, " + trip + " has a " +
									"duration of " + tripDuration + " min, but has a minimal duration of " +
									minTripDuration + "min.");
							feasibility = false;
						}
						if(tripLength > 0){
							numberOfEmptyTripsWithoutDepot += 1;
							emptyTripDurationWithoutDepot += tripDuration;
							emptyTripDistanceWithoutDepot += tripLength;
						}
						else {
							numberOfWaitingTimesInStation += 1;
							sumWaitingTimeInStation += tripDuration;
							minWaitingTimeInStation = Math.min(minWaitingTimeInStation, tripDuration);
							maxWaitingTimeInStation = Math.max(maxWaitingTimeInStation, tripDuration);
						}
					}
					currentTime = trip.getEndTime();
					currentStopId = trip.getEndStopId();
				}
			}
		}
		double emptyDuration = emptyTripDurationDepot + emptyTripDurationWithoutDepot + sumWaitingTimeInStation;
		double emptyLength = emptyTripDistanceDepot + emptyTripDistanceWithoutDepot;
		double emptyCosts = costPerVehicle * numberOfUsedVehicles + costFactorEmptyDuration * emptyDuration +
				costFactorEmptyLength * emptyLength;
		double costs = emptyCosts + costFactorFullDuration * fullTripDuration + costFactorFullLength * fullTripDistance;
		// Are there still uncovered trips left?
		if (trips.size() > 0) {
			logger.log(LogLevel.WARN, "There were uncovered trips:");
			for (Trip trip : trips) {
				logger.log(LogLevel.WARN, trip.toString());
			}
			feasibility = false;
		}
		//Write the found values to the statistic
		statistic.put("vs_cost", costs);
		statistic.put("vs_empty_cost", emptyCosts);
		statistic.put("vs_circulations", numberOfCirculations);
		statistic.put("vs_vehicles", numberOfUsedVehicles);
		statistic.put("vs_empty_distance", emptyTripDistanceWithoutDepot);
		statistic.put("vs_empty_distance_with_depot", emptyLength);
		statistic.put("vs_empty_duration_standing", sumWaitingTimeInStation);
		statistic.put("vs_empty_duration_driving", emptyTripDurationWithoutDepot);
		statistic.put("vs_empty_duration_with_depot", emptyDuration);
		statistic.put("vs_empty_trips", numberOfEmptyTripsWithoutDepot);
		statistic.put("vs_empty_trips_depot", numberOfEmptyTripsWithoutDepot + numberOfEmptyTripsDepot);
		statistic.put("vs_minimal_waiting_time", minWaitingTimeInStation);
		statistic.put("vs_maximal_waiting_time", maxWaitingTimeInStation);
		statistic.put("vs_average_waiting_time", sumWaitingTimeInStation / numberOfWaitingTimesInStation);
		statistic.put("vs_full_distance", fullTripDistance);
		statistic.put("vs_full_duration", fullTripDuration);
		statistic.put("vs_feasible", feasibility);
		logger.log(LogLevel.INFO, "Finished evaluation");
	}

	public static void evaluateVehicleSchedule(VehicleSchedule vehicleSchedule, Collection<Trip> trips,
	                                           Graph<Stop, Link> ptn, LinePool lineConcept, Config config){
		evaluateVehicleSchedule(vehicleSchedule, trips, ptn, lineConcept, Statistic.getDefaultStatistic(), config);
	}

	public static void evaluateVehicleSchedule(VehicleSchedule vehicleSchedule, Collection<Trip> trips,
	                                           Graph<Stop, Link> ptn, LinePool lineConcept, Statistic statistic){
		evaluateVehicleSchedule(vehicleSchedule, trips, ptn, lineConcept, statistic, Config.getDefaultConfig());
	}

	public static void evaluateVehicleSchedule(VehicleSchedule vehicleSchedule, Collection<Trip> trips,
	                                           Graph<Stop, Link> ptn, LinePool lineConcept){
		evaluateVehicleSchedule(vehicleSchedule, trips, ptn, lineConcept, Statistic.getDefaultStatistic(), Config
				.getDefaultConfig());
	}

	/**
	 * Get the time in minutes that a vehicle with the given speed needs to travel a distance of given length
	 * @param length the length to convert, in kilometers
	 * @param vehicleSpeed the speed of the vehicle
	 * @return the time in minutes
	 */
	private static double getTimeInMinutes(double length, double vehicleSpeed){
		return length / vehicleSpeed * minutesPerHour;
	}
}

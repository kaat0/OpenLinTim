package net.lintim.main.vehiclescheduling;

import net.lintim.io.vehiclescheduling.IO;
import net.lintim.model.vehiclescheduling.*;

import java.io.IOException;
import java.util.*;

/**
 * Makes calculations for the vehicle scheduling problem.
 *
 */

public class VS
{
        private VS() {}  // class only contains static methods


	/** calculates the shortest distances between all pairs of stations.
 	 * Method:
 	 * the algorithm of Floyd (like in the wikipedia), where the concrete paths are not calculated
 	 * @param stops an ArrayList of the stops in the network, every stop is represented by an unique ID (Integer)
 	 * @param edges an ArrayList of the edges in the network, every edge is represented by:
 	 * 		an unique ID (Integer), the leftStopID (Integer), the rightStopID (Integer) and the length (Double)
 	 * @return distances, a 2 dimension matrix of the size stations \times stations:
 	 * 	  it contains the values of the distances between every pair of stations (double values)
	 */
	public static int[][] calculateShortestPathsFloyd(ArrayList<Integer> stops, ArrayList<Edge> edges, boolean isUndirected) throws IllegalArgumentException{
		int[][] distances = new int[stops.size()][stops.size()];

		// Algorithm of Floyd: (like in the wikipedia)
		// 1. step (first part): Initialise the distances with positive infinity or 0
		for(int i = 0; i < stops.size(); i++){
			if(i != (stops.get(i)-1)){
				throw new IllegalArgumentException("stop ID's are not continous or not starting with 1");
			}

			for(int j = 0; j < stops.size(); j++){
				if(i == j){
					distances[i][j] = 0;
				} else{
				distances[i][j] = Integer.MAX_VALUE / 2; // Don't use Integer.MAX_VALUE! Otherwise, an overflow will occur!
				}
			}
		}

		// 1. step (second part): Set the distances for the given edges to the corresponding weight
		for(int i = 0; i < edges.size(); i++){
			if(edges.get(i).getLeftStopID() > stops.size() || edges.get(i).getRightStopID() > stops.size()){
				throw new IllegalArgumentException("edge contains a stop ID, which doesn't exist");
			}

			distances[edges.get(i).getLeftStopID()-1][edges.get(i).getRightStopID()-1] = edges.get(i).getLength();
			if(isUndirected){
				distances[edges.get(i).getRightStopID()-1][edges.get(i).getLeftStopID()-1] = edges.get(i).getLength();
			}
		}

		// 2. step: Calculate stepwise the distances between all pairs of stops
		for(int k = 0; k < stops.size(); k++){
			for(int i = 0; i < stops.size(); i++){
				for(int j = 0; j < stops.size(); j++){
					if(distances[i][k] + distances[k][j] < distances[i][j]){
						distances[i][j] = distances[i][k] + distances[k][j];
					}
				}
			}
		}

		return distances;
	}

	/** calculates the compatibility matrix for the models AM, TM and NM.
 	 * Assumptions:
 	 * <ul>
 	 * <li> two trips i and j are compatible, if trip j can be driven directly after trip i by the same vehicle
 	 * <li> empty trips to connect two trips are allowed
 	 * </ul>
 	 * @param trips an ArrayList of the given trips, each trip is represented by:
 	 * 		<ul>
 	 * 		<li> ID: unique ID for the trip
 	 * 		<li> startID: the ID of the starting event
 	 * 		<li> endID: the ID of the ending event
 	 * 		<li> startStation: the ID of the first station of the trip
 	 * 		<li> endStation: the ID of the last station of a trip
 	 * 		<li> startTime: the time (in seconds), when the vehicle departs at the first station
 	 * 		<li> endTime: the time (in seconds), when the vehicle arrives at the last station
 	 *	 	</ul>
 	 * @param distances the two dimensional matrix calculated by the function {@link #calculateShortestPathsFloyd(ArrayList, ArrayList, boolean)}
 	 *
	 * @return CompatibilityMatrix, an two dimensional matrix of Integer of size trips \times trips, it holds:
 	 * 		<ul>
 	 * 		<li> CompatibilityMatrix[i][j] = 0, if the trips are compatible
 	 * 		<li> CompatibilityMatrix[i][j] = 1, if the trips are incompatible
 	 *	 	</ul>
	 */
	public static int[][] calculateCompatibilityMatrix(ArrayList<Trip> trips, int[][] distances, int minDistance){
		int[][] CompatibilityMatrix = new int[trips.size()][trips.size()];
		System.out.println("Mindistance: " + minDistance);
		for(int i = 0; i < trips.size(); i++){
			for(int j = 0; j < trips.size(); j++){
				CompatibilityMatrix[i][j] = Math.max(0, (trips.get(i).getEndTime() + (distances[trips.get(i).getEndStation()-1][trips.get(j).getStartStation()-1])
									 + (minDistance) - trips.get(j).getStartTime() - 86400)/86400 + 1);

			}
		}

		return CompatibilityMatrix;
	}


	/** calculates the canals and the transfers of the model.
 	 * Assumptions:
 	 * <ul>
 	 * <li> each station can have at most one driving-canal, one parking-canal and one maintaining-canal
 	 * <li> no station can have more than one canal of each type
 	 * <li> parking-canals and maintaining-canals exist for a station iff parking repectively maintaining is allowed in that station
 	 * </ul>
 	 * @param network, which contains:
 	 * 	<ul>
 	 * 	<li>	an ArrayList of the given trips (CTrip), each trip is represented by:
 	 * 		<ul>
 	 * 		<li> ID: unique ID for the trip (of the super-class CJourney)
 	 * 		<li> startID: the ID of the starting event
 	 * 		<li> endID: the ID of the ending event
 	 * 		<li> startStation: the ID of the first station of the trip (of the super-class CJourney)
 	 * 		<li> endStation: the ID of the last station of a trip (of the super-class CJourney)
 	 * 		<li> startTime: the time (in seconds), when the vehicle departs at the first station
 	 * 		<li> endTime: the time (in seconds), when the vehicle arrives at the last station
 	 *	 	</ul>
 	 *	 <li>	an ArrayList of the transfers (CTransfers), which is still empty, each transfer is represented by:
 	 * 		<ul>
 	 * 		<li> ID: unique ID for the trip (of the super-class CJourney)
 	 * 		<li> startEvent: the starting event (Event)
 	 * 		<li> endEvent: the ending event (Event)
 	 * 		<li> startStation: the ID of the first station of the transfer (of the super-class CJourney)
 	 * 		<li> endStation: the ID of the last station of a transfer (of the super-class CJourney)
 	 * 		<li> costs: the costs of the transfer (double)
 	 * 		<li> type: can be "TRIP", "EMPTY", "PARKING" and "MAINTAINING" (String)
 	 * 		<li> timeCycleJump: states, if the transfer contains an time cycle jump (boolean)
 	 *	 	</ul>
 	 *	 <li>	an array Canal[] of the canals of the network, which is still empty, each canal is represented by:
 	 *	 	<ul>
 	 *		<li> ID: a unique ID of the canal
 	 *		<li> stationID: the ID of the associated station
 	 *		<li> event: an ArrayList<CEvent> of the events of the canal
 	 *		<li> type: the type of the canal, that can be "DRIVING", "PARKING" and "MAINTAINING"
 	 *	 	</ul>
 	 *	 </ul>
 	 *
 	 * @param stops an ArrayList<Integer> of the stops calculated by {@link IO#readStops()}
	 * @param parkingAllowed an array boolean[] of length stops.size(), which states, in which stations a vehicle can be parked.
	 * @param maintainingAllowed an array boolean[] of length stops.size(), which states, in which stations a vehicle can be maintained.
	 * @param vehicleCosts an integer, which represents the costs of a single vehicle
	 * @param minDistance an integer, which represents the minimum distance, which has to be between two different trips, which are served one after the other by the same vehicle
	 * @param turnOverTime an integer, which represents the minimum time for a vehicle to turn over in a station
 	 *
	 * @return the canal network, which contains:
	 * 	<ul>
	 * 	<li> an Array Canal[] of canals of size 3*stops.size(), were each canal is represented by
 	 * 		<ul>
 	 * 		<li> ID: unique ID of the canal
 	 * 		<li> stationID: ID of the station
 	 * 		<li> events: an ArrayList<Event> of events, each represented by the associated CJourney, the time, the station and the type (START or END)
 	 * 		<li> type: a String, which can be DRIVING, PARKING or MAINTAINING
 	 *	 	</ul>
 	 *	 <li> an ArrayList of the newly build CTransfers
 	 *	 <li> the ArrayList of the given trips
 	 *	 <ul>
 	 *  Assumptions for the canal array:
 	 *  <ul>
 	 *  <li> ID's 0-(stops.size()-1): driving-canals
 	 *  <li> ID's stops.size()-(2*stops.size()-1): parking-canals
 	 *  <li> ID's 2*stops.size()-(3*stops.size()-1): maintaining-canals
 	 *  </ul>
	 */
	public static CanalNetwork calculateCanalsAndTransfers(CanalNetwork network,
	                                                       ArrayList<Integer> stops, boolean[] parkingAllowed,
	                                                       boolean[] maintainingAllowed, int vehicleCosts, int[][] distances,
	                                                       int turnOverTime, boolean ptnIsUndirected, int minDistance) throws IOException{
		ArrayList<CTrip> trips = network.getTrips();
		HashSet<CTrip> tripsSet = new HashSet<CTrip>(trips);
		Canal[] canals  = new Canal[3*stops.size()];
		ArrayList<CTransfer> transfers = new ArrayList<CTransfer>();
		int transferID = 1;

		ArrayList<Event> givenEvents = IO.readEvents();

		int eventID = givenEvents.get(givenEvents.size()-1).getID()+1; // this is the beginning ID for the canal events, because the given Events have the ID's before that ID
									       // (they are continous and starting with 1)


		boolean[] existingDrivingCanal = new boolean[stops.size()]; // shows, which stops have driving-canals with events
		boolean[] existingParkingCanal = new boolean[stops.size()]; // shows, which stops have a parking-canal
		boolean[] existingMaintainingCanal = new boolean[stops.size()]; // shows, which stops have a maintaining-canal

		// initialise canals
		for(int i = 0; i < stops.size(); i++){
			existingDrivingCanal[i] = false;
			existingParkingCanal[i] = false;
			existingMaintainingCanal[i] = false;
			ArrayList<CEvent> events = new ArrayList<CEvent>();
			Canal drivingCanal = new Canal(i+1, i+1, "DRIVING", events); // create a canal for each stop without events
			canals[i] = drivingCanal;
		}

		Map<Integer, Integer> usedIDs = new HashMap<Integer, Integer>();

		// create Transfers from the trips to the canals and from the canals to the trips (including the associated canal events)
		for(int i = 0; i < trips.size(); i++){
			AEvent TripStartEvent = new AEvent(trips.get(i), trips.get(i).getStartID(), trips.get(i).getStartTime(), trips.get(i).getStartStation(), Event.TYPE_START,
							   trips.get(i).getPeriodicStartID());
			AEvent TripEndEvent = new AEvent(trips.get(i), trips.get(i).getEndID(), trips.get(i).getEndTime(), trips.get(i).getEndStation(), Event.TYPE_END,
							trips.get(i).getPeriodicEndID());

			if(usedIDs.containsKey(trips.get(i).getStartID())) {
				throw new Error("ID " + trips.get(i).getStartID() + " used twice, second time via trip start id, first time: " + usedIDs.get(trips.get(i).getStartID()));
			}
			if(usedIDs.containsKey(trips.get(i).getEndID())) {
				throw new Error("ID " + trips.get(i).getEndID() + " used twice, second time via trip end id, first time: " + usedIDs.get(trips.get(i).getEndID()));
			}
			usedIDs.put(trips.get(i).getStartID(), 0);
			usedIDs.put(trips.get(i).getEndID(), 1);

			CEvent CanalStartEvent = new CEvent(trips.get(i), null, eventID, (trips.get(i).getEndTime() + turnOverTime), trips.get(i).getEndStation(), Event.TYPE_START, -1);
			CEvent CanalEndEvent = new CEvent(trips.get(i), null, (eventID + 1), trips.get(i).getStartTime(), trips.get(i).getStartStation(), Event.TYPE_END, -1);
			if(usedIDs.containsKey(eventID)) {
				throw new Error("ID " + eventID + " used twice, second time via canal start event (1)");
			}
			if(usedIDs.containsKey(eventID + 1)) {
				throw new Error("ID " + (eventID  + 1) + " used twice, second time via canal end event (1)");
			}
			usedIDs.put(eventID, 2);
			usedIDs.put(eventID + 1, 3);

			canals[trips.get(i).getStartStation()-1].addEvent(CanalEndEvent);
			canals[trips.get(i).getEndStation()-1].addEvent(CanalStartEvent);

			existingDrivingCanal[trips.get(i).getStartStation()-1] = true;
			existingDrivingCanal[trips.get(i).getEndStation()-1] = true;


			CTransfer transferFromTripToCanal = new CTransfer(transferID, TripEndEvent, CanalStartEvent, 0, "TRIP", false);
			CTransfer transferFromCanalToTrip = new CTransfer((transferID+1), CanalEndEvent, TripStartEvent, 0, "TRIP", false);

			CanalStartEvent.setAssociatedTransfer(transferFromTripToCanal);
			CanalEndEvent.setAssociatedTransfer(transferFromCanalToTrip);

			transfers.add(transferFromTripToCanal);
			transfers.add(transferFromCanalToTrip);

			transferID = transferID + 2;
			eventID = eventID + 2;
		}

		// create the empty trips between the canals
		// Assumptions: Between every pair of stations an empty trip is allowed.

		for(int i = 0; i < stops.size(); i++){
			if(!existingDrivingCanal[i]) continue;

			for(int j = 0; j < stops.size(); j++){
				if(i == j) continue;
				if(!existingDrivingCanal[j]) continue;
				ArrayList<CEvent> events = new ArrayList<CEvent>(canals[i].getEvents());
				for(CEvent event: events){
					if(!tripsSet.contains(event.getJourney())) continue;
					// calculate Leaves after an Enter in canal i
					if(event.getType() == Event.TYPE_START){
						int startTime = (event.getTime() + (minDistance)) % 86400; // distances are scaled by 100 when they were read in
						int endTime = (event.getTime() + ((minDistance) + distances[i][j])) % 86400; // distances are scaled by 100 when they were read in

						// If the transfer contains a time cycle jump ==> additional costs of a single vehicle are induced
						CTransfer transfer = new CTransfer(transferID, null, null, distances[i][j] + (startTime <= endTime? 0: vehicleCosts), "EMPTY", startTime > endTime);
						transfers.add(transfer);

						CEvent canalEndEvent = new CEvent(transfer, transfer, eventID, startTime, i+1, Event.TYPE_END, -1);
						CEvent canalStartEvent = new CEvent(transfer, transfer, eventID+1, endTime, j+1, Event.TYPE_START, -1);
						if(usedIDs.containsKey(eventID)) {
							throw new Error("ID " + eventID + " used twice, second time via canal end event (2)");
						}
						if(usedIDs.containsKey(eventID + 1)) {
							throw new Error("ID " + (eventID + 1) + " used twice, second time via canal start event (2)");
						}
						usedIDs.put(eventID, 4);
						usedIDs.put(eventID + 1, 5);

						transfer.setStartEvent(canalEndEvent);
						transfer.setEndEvent(canalStartEvent);

						canals[i].addEvent(canalEndEvent);
						canals[j].addEvent(canalStartEvent);
					}
					// calculates Enters before a Leave in canal i
					 else if(event.getType() == Event.TYPE_END){
						int endTime = (event.getTime() - (minDistance)) % 86400; // distances are scaled by 100 when they were read in
						int startTime = (event.getTime() - ((minDistance) - distances[j][i])) % 86400; // distances are scaled by 100 when they were read in

						// If the transfer contains a time cycle jump ==> additional costs of a single vehicle are induced
						CTransfer transfer = new CTransfer(transferID, null, null, distances[j][i] + (startTime <= endTime? 0: vehicleCosts), "EMPTY", startTime > endTime);
						transfers.add(transfer);

						CEvent canalStartEvent = new CEvent(transfer, transfer, eventID, endTime, i+1, Event.TYPE_START, -1);
						CEvent canalEndEvent = new CEvent(transfer, transfer, eventID+1, startTime, j+1, Event.TYPE_END, -1);
						if(usedIDs.containsKey(eventID)) {
							throw new Error("ID " + eventID + " used twice, second time via canal start event (3)");
						}
						if(usedIDs.containsKey(eventID + 1)) {
							throw new Error("ID " + (eventID + 1) + " used twice, second time via canal end event (3)");
						}
						usedIDs.put(eventID, 5);
						usedIDs.put(eventID + 1, 6);

						transfer.setStartEvent(canalEndEvent);
						transfer.setEndEvent(canalStartEvent);

						canals[i].addEvent(canalStartEvent);
						canals[j].addEvent(canalEndEvent);
					}

					transferID++;
					eventID = eventID + 2;
				}
			}
		}

		for(int i = 0; i < stops.size(); i++){
			if(parkingAllowed[i]){
				ArrayList<CEvent> events = new ArrayList<CEvent>();
				Canal parkingCanal = new Canal(i, i, "PARKING", events);
				canals[stops.size()+i] = parkingCanal;
			}
			if(maintainingAllowed[i]){
				ArrayList<CEvent> events = new ArrayList<CEvent>();
				Canal maintainingCanal = new Canal(i, i, "MAINTAINING", events);
				canals[2*stops.size()+i] = maintainingCanal;
			}
		}

		// FIXME: At this place, the events and transfers for the maintaining canals and parking canals should be created!!!

		for(int i = 0; i < canals.length; i++){
			canals[i] = sortEvents(canals[i]);
		}

		network.setTransfers(transfers);
		network.setCanals(canals);

		return network;
	}

	public static HashMap<Canal, HashMap<CEvent, CEvent>> calculateCanalMappings(Canal[] canals){
		HashMap<Canal, HashMap<CEvent, CEvent>> mappings = new HashMap<Canal, HashMap<CEvent, CEvent>>();

		for(Canal canal: canals){
			if(canal == null || canal.getEvents() == null){
				continue;
			}
			ArrayList<CEvent> events = canal.getEvents();
			HashMap<CEvent, CEvent> mapping = new HashMap<CEvent, CEvent>();

			ArrayList<CEvent> leftCanalLeaves = new ArrayList<CEvent>();
			ArrayList<CEvent> readCanalEnters = new ArrayList<CEvent>();

			// if a Leave is read, it is always mapped to a previous Enter (the first of these), if there is one in the ArrayList readCanalEnters
			// else, it is moved in the ArrayList leftCanalLeaves and will be mapped after the run through the canal events

			// First Step:
			// run through the whole time cycle and do:
			// 1.) if a Leave is read:
			//     a) if there is an Enter in the set "readCanalEnters": map the first of this Enters to the Leave
			//     b) put the Leave in the set "leftCanalLeaves"
			// 2.) if a Enter is read: put it in the set of "readCanalEnters"

			for(CEvent event: events){
				if(event.getType().equals("END") && readCanalEnters.size() <= 0){
					leftCanalLeaves.add(event);
				}
				else if(event.getType().equals("END")){
					mapping.put(readCanalEnters.get(0), event);
					readCanalEnters.remove(0);
				}
				else{
					readCanalEnters.add(event);
				}
			}

			// Second Step:
			// make the mappings between the sets "readCanalEnters" and "leftCanalLeaves", map always the first remaining events of the sets

			if(readCanalEnters.size() != leftCanalLeaves.size()){
				System.err.println("The number of the canal leaves and enters doesn't fit together");
			}

			int numberOfLeftCanalLeaves = leftCanalLeaves.size();

			for(int i = 0; i < numberOfLeftCanalLeaves; i++){
				mapping.put(readCanalEnters.get(0), leftCanalLeaves.get(0));
				readCanalEnters.remove(0);
				leftCanalLeaves.remove(0);
			}

			mappings.put(canal, mapping);
		}

		return mappings;
	}

	public static Canal[] calculateCanalsWithOccuringEvents(CanalNetwork network, ArrayList<Integer> occuringCanalEvents){
		Canal[] newCanals = network.getCanals();
		HashSet<CEvent> toDelete = new HashSet<CEvent>();

		for(int i = 0; i < newCanals.length; i++){
			if(newCanals[i] == null) continue;
			newCanals[i] = newCanals[i].clone();
		}

		for(Canal canal: newCanals){
			if(canal == null || canal.getEvents() == null){
				continue;
			}
			ArrayList<CEvent> events = canal.getEvents();
			for(CEvent event: events){
				toDelete.add(event);
			}
		}

		HashSet<Integer> occuringCanalEventsSet = new HashSet<Integer>(occuringCanalEvents);
		for(Canal canal: newCanals){
			if(canal == null || canal.getEvents() == null){
				continue;
			}
			ArrayList<CEvent> events = canal.getEvents();
			for(CEvent event: events){
				if(occuringCanalEventsSet.contains(event.getID())) {
					toDelete.remove(event);
				}
			}
		}

		for(Canal canal: newCanals){
			if(canal == null || canal.getEvents() == null){
				continue;
			}
			canal.removeAllEvents(toDelete);
		}

		return newCanals;
	}

	private static void checkTransfersMatrix(int[][] matrix) {
		for(int x = 0; x < matrix.length; ++x) {
			boolean any = false;
			for(int y = 0; y < matrix[x].length; ++y) {
				if(matrix[x][y] != 0) {
					if(any) throw new Error("Invalid column in transfersMatrix: " + x);
					any = true;
				}
			}
		}
		for(int y = 0; y < matrix[0].length; ++y) {
			boolean any = false;
			for(int x = 0; x < matrix.length; ++x) {
				if(matrix[x][y] != 0) {
					if(any) throw new Error("Invalid row in transfersMatrix: " + x);
					any = true;
				}
			}
		}
	}

	public static int[][] calculateTransfersMatrix(ArrayList<Integer> stops,
	                                               ArrayList<CTrip> trips,
	                                               HashMap<Integer, Integer> lineIDforGivenEventIDs,
	                                               HashMap<Integer, Integer> leftEventsOfTheLines,
	                                               HashMap<Integer, Integer> leftEventsOfTheOccuringTransfers,
	                                               HashMap<Integer, Integer> leftEventsOfTheMappings, Integer greatestLineID,
	                                               int[][] distances, HashMap<Integer, CTransfer> transferForGivenEventIDs, int minDistance)
	throws IOException{

		int[][] transfersMatrix = new int[trips.size()][trips.size()];
		HashMap<Integer, Integer> allMappingsFromStartsToEnds = new HashMap<Integer, Integer>();

		for(int i = 0; i <= trips.size(); i++){
			for(int j = 0; i < trips.size(); i++){
				transfersMatrix[i][j] = 0;
			}
		}

		Set<Integer> lineStartIDs = leftEventsOfTheLines.keySet();
		Set<Integer> transferStartIDs = leftEventsOfTheOccuringTransfers.keySet();
		Set<Integer> mappingsStartIDs = leftEventsOfTheMappings.keySet();

		for(Integer currentLineStartID: lineStartIDs){
			allMappingsFromStartsToEnds.put(currentLineStartID, leftEventsOfTheLines.get(currentLineStartID));
		}
		for(Integer currentTransferStartID: transferStartIDs){
			allMappingsFromStartsToEnds.put(currentTransferStartID, leftEventsOfTheOccuringTransfers.get(currentTransferStartID));
		}
		for(Integer currentMappingStartID: mappingsStartIDs){
			allMappingsFromStartsToEnds.put(currentMappingStartID, leftEventsOfTheMappings.get(currentMappingStartID));
		}

		int transfersWithMoreThanOneTimeCycleJump = 0;

		// TODO somehow reconsider 86400
		for(int i = 0; i < stops.size(); i++){
			for(int j = 0; j < stops.size(); j++){
				if(leftEventsOfTheOccuringTransfers.get(i) != null){
					if(distances[i][j] > 86400 && leftEventsOfTheOccuringTransfers.get(i) == j){
						transfersWithMoreThanOneTimeCycleJump++;
					}
				}
			}
		}


		for(Integer currentLineStartID:	lineStartIDs){
			int currentLineID = lineIDforGivenEventIDs.get(currentLineStartID);
			int currentEnd = leftEventsOfTheLines.get(currentLineStartID);

			int numberOfZS = 0;

			String typeOfDrive = "Trip";

			while(!leftEventsOfTheLines.containsKey(currentEnd)){

				if(!allMappingsFromStartsToEnds.containsKey(currentEnd)){
					System.err.println("The mappings don't contain an entry for the key " + currentEnd);
				}

				if(leftEventsOfTheOccuringTransfers.containsKey(currentEnd)){

					if(transferForGivenEventIDs.get(currentEnd).getStartEvent().getTime() > transferForGivenEventIDs.get(currentEnd).getEndEvent().getTime()){
						numberOfZS++;
					}

					if(typeOfDrive.equals("Transfer")){
						System.err.println("Two transfers in a row!!!!");
					}
					typeOfDrive = "Transfer";
				} else if(leftEventsOfTheMappings.containsKey(currentEnd)){

					if(transferForGivenEventIDs.get(currentEnd).getEndEvent().getTime()
					   > transferForGivenEventIDs.get(allMappingsFromStartsToEnds.get(currentEnd)).getStartEvent().getTime()){
						numberOfZS++;
					}

					if(typeOfDrive.equals("Mapping")){
						System.err.println("Two mappings in a row!!!!");
					}
					typeOfDrive = "Mapping";
				}


				currentEnd = allMappingsFromStartsToEnds.get(currentEnd);
			}

			typeOfDrive = "Trip";

			int followingLineID = lineIDforGivenEventIDs.get(currentEnd);


			if(numberOfZS == 0) {
				if(!(trips.get(currentLineID-1).getEndTime() + (distances[trips.get(currentLineID-1).getEndStation()-1][trips.get(followingLineID-1).getStartStation()-1]) + (minDistance)
				    <= trips.get(followingLineID-1).getStartTime())){
					System.err.print("Transfer from " + currentLineID + " at " + trips.get(currentLineID-1).getEndTime());
					System.err.print(" to " + followingLineID + " at " + trips.get(followingLineID-1).getStartTime());
					System.err.println(": distance is " + (distances[trips.get(currentLineID-1).getEndStation()-1][trips.get(followingLineID-1).getStartStation()-1]));
					System.err.println("Min Distance: " + minDistance);
				}

				transfersMatrix[currentLineID-1][followingLineID-1] = 1; // Transfer without time cycle jump
			} else{
				transfersMatrix[currentLineID-1][followingLineID-1] = -numberOfZS; // Transfer with numberOfZS time cycle jumps
			}
		}

		checkTransfersMatrix(transfersMatrix);

		return transfersMatrix;
	}

	public static Canal sortEvents(Canal canal){
		if(canal == null){
			return canal;
		} else if(canal.getEvents() == null || canal.getEvents().size() == 0){
			return canal;
		} else{
			ArrayList<CEvent> events = canal.getEvents();
			Collections.sort(events, new Comparator<CEvent>() {
				public int compare(CEvent a, CEvent b) {
					double diff = a.getTime() - b.getTime();
					if(diff < 0) return -1;
					if(diff > 0) return 1;
					if(diff == 0){
						if(a.getType() != b.getType()){
							if(a.getType() == "START"){
								return -1;
							}
							return 1;
						}
					}
					return 0;
				}
			});
			canal.setEvents(events);
		}
		return canal;
	}


}


package net.lintim.io.vehiclescheduling;

import net.lintim.algorithm.vehiclescheduling.VS;
import net.lintim.model.vehiclescheduling.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.Logger;

import java.io.*;
import java.util.*;

public class IO {
    private static Logger logger = new Logger(IO.class);

    // default names for output files which later get input files
    private static final String vehicleFlowFileName = "vehicle-scheduling/Vehicle_Flow.vs";

    // default names for output files which are only generated when "vs_verbose" is true
    private static final String canalNetworkFileName = "vehicle-scheduling/Canal_Network.vs";

    private IO() {
    }  // class only contains static methods

    // default names for output files which later get input files
    // WARNING: this file is hardcoded in the .mos files.
    // WARNING: it can not be moved to config without extra modification.
    private static final String transfersFileName = "vehicle-scheduling/Transfers.vs";

    private static int[][] distances;
    /**
     * Contains the IDs of the stops of the network. the IDs of the stops are
     * continous and starting with 1.
     */
    private static final ArrayList<Integer> stops = new ArrayList<>();
    private static final ArrayList<Edge> edges = new ArrayList<>();

    // default values for some variables - overridden by config file entries
    // =========================================================================
    // === PLEASE NOTE: THESE VALUES WILL BE IGNORED!                        ===
    // === If one of these values is not contained in the config, a null     ===
    // === pointer exception will terminate the program.                     ===
    // =========================================================================
    private static String modelName = "TRANSPORTATION_MODEL"; // This is the used model
    private static int vehicleCosts = 100; // This are the costs of the single vehicle
    private static int penCosts = 50000; // This are the costs, which occurs, when one trip isn't served
    private static int depot = 1; // This is the index of the depot station
    private static int timeUnitsPerMinute = 1; // Needed for lower bounds to seconds conversion
    private static boolean verbose = false; // This boolean states if the variable "vs_verbose" in the config is set and prints more informations if it is the case
    private static int turnOverTime = 0; // This is the time, which is needed to turn over in a station (so the minDistance has to be greater or equal as this value) --> Canal Model
    // default names for input files
    private static String eventsFileName = null;
    private static String tripsFileName = null;
    private static String stopsFileName = null;
    private static String edgesFileName = null;
    // default names for output files
    private static String vehicleSchedulesFileName = null;
    // Solver parameters
    private static int timelimit;
    private static double mipGap;
    private static int threadLimit;
    private static boolean outputSolverMessages;
    private static boolean writeLpFile;

    /**
     * gets the informations about the trips.
     * Proceeding: It reads the informations out of the file "tripsFileName"
     *
     * @return trips an ArrayList, which contains the trip, each trip is represented by
     * <ul>
     * <li> ID: unique ID for the trip
     * <li> startID: the ID of the starting event
     * <li> endID: the ID of the ending event
     * <li> startStation: the ID of the first station of the trip
     * <li> endStation: the ID of the last station of a trip
     * <li> startTime: the time (in seconds), when the vehicle departs at the first station
     * <li> endTime: the time (in seconds), when the vehicle arrives at the last station
     * 	</ul>
     */
    public static ArrayList<Trip> readTrips() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(tripsFileName));
        String line;
        ArrayList<Trip> trips = new ArrayList<>();

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            int ID = trips.size() + 1;

            String[] entries = line.split(";");
            int startID = Integer.parseInt(entries[0].trim());
            int periodicStartID = Integer.parseInt(entries[1].trim());
            int startStation = Integer.parseInt(entries[2].trim());
            int startTime = Integer.parseInt(entries[3].trim());
            int endID = Integer.parseInt(entries[4].trim());
            int periodicEndID = Integer.parseInt(entries[5].trim());
            int endStation = Integer.parseInt(entries[6].trim());
            int endTime = Integer.parseInt(entries[7].trim());
            int lineID = Integer.parseInt(entries[8].trim());

            Trip trip = new Trip(ID, startID, endID, startStation, endStation, startTime, endTime, lineID, periodicStartID, periodicEndID);
            trips.add(trip);

        }
        reader.close();

        return trips;
    }

    /**
     * gets the informations about the trips. Proceeding: It reads the
     * informations out of the file "tripsFileName"
     *
     * @return trips an ArrayList, which contains the trip, each trip is
     * represented by
     * <ul>
     * <li>ID: unique ID for the trip
     * <li>startID: the ID of the starting event
     * <li>endID: the ID of the ending event
     * <li>startStation: the ID of the first station of the trip
     * <li>endStation: the ID of the last station of a trip
     * <li>startTime: the time (in seconds), when the vehicle departs at
     * the first station
     * <li>endTime: the time (in seconds), when the vehicle arrives at
     * the last station
     * </ul>
     */
    public static ArrayList<CTrip> readCTrips() throws IOException {

        BufferedReader reader = new BufferedReader(
            new FileReader(tripsFileName));
        String line;
        ArrayList<CTrip> trips = new ArrayList<>();

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            int ID = trips.size() + 1;

            String[] entries = line.split(";");
            int startID = Integer.parseInt(entries[0].trim());
            int periodicStartID = Integer.parseInt(entries[1].trim());
            int startStation = Integer.parseInt(entries[2].trim());
            int startTime = Integer.parseInt(entries[3].trim());
            int endID = Integer.parseInt(entries[4].trim());
            int periodicEndID = Integer.parseInt(entries[5].trim());
            int endStation = Integer.parseInt(entries[6].trim());
            int endTime = Integer.parseInt(entries[7].trim());
            int lineID = Integer.parseInt(entries[8].trim());

            CTrip trip = new CTrip(ID, startID, endID, startStation,
                endStation, startTime, endTime, lineID, periodicStartID,
                periodicEndID);
            trips.add(trip);

        }
        reader.close();

        return trips;
    }

    /**
     * read the canal events contained in the calculated vehicle flow.
     *
     * @return events, an ArrayList<Integer>, which contains the id's of the
     * occuring events
     */
    public static ArrayList<Integer> readVehicleFlowCanals() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(
            vehicleFlowFileName));

        String line;
        ArrayList<Integer> events = new ArrayList<>();

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            String[] entries = line.split(";");

            if (entries.length > 4 || entries[0].trim().equals("Canal")) {
                continue;
            }
            int ID = Integer.parseInt(entries[0].trim());

            events.add(ID);

        }
        reader.close();

        return events;
    }

    /**
     * read the transfers of the calculated vehicle flow.
     *
     * @return transfers, an ArrayList<CTransfer>, which contains the
     * transfers, each transfer is represented by
     * <ul>
     * <li>ID: unique ID for the transfer
     * <li>startEvent: format ID; time; type
     * <li>endEvent: format ID; time; type
     * <li>costs: integer, which represents the costs of the transfer
     * <li>timeCycleJump: a boolean, which is true, if the transfer
     * contains a time cycle jump (a time cycle jump occurs, when a
     * vehicle pauses between two trips untill the next time cycle,
     * e.g. over night).
     * <li>type: String, which could be "TRIP", "EMPTY", "PARKING" and
     * "MAINTAINING"
     * </ul>
     */
    public static ArrayList<CTransfer> readVehicleFlowTransfers()
        throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(
            vehicleFlowFileName));
        String line;
        ArrayList<CTransfer> transfers = new ArrayList<>();
        logger.debug("Transfers read from VFFN " + vehicleFlowFileName);

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            String[] entries = line.split(";");

            if (entries.length < 10) {
                continue;
            }
            int ID = Integer.parseInt(entries[0].trim());
            int startEventID = Integer.parseInt(entries[1].trim());
            int startEventTime = Integer.parseInt(entries[2].trim());
            String startEventType = entries[3].trim();
            int endEventID = Integer.parseInt(entries[4].trim());
            int endEventTime = Integer.parseInt(entries[5].trim());
            String endEventType = entries[6].trim();
            int costs = (int) Float.parseFloat(entries[7].trim());
            boolean timeCycleJump = Boolean.parseBoolean(entries[8].trim());
            String type = entries[9].trim();

            Event startEvent = new Event(startEventID, startEventTime,
                startEventType);
            Event endEvent = new Event(endEventID, endEventTime, endEventType);

            CTransfer transfer = new CTransfer(ID, startEvent, endEvent, costs,
                type, timeCycleJump);

            transfers.add(transfer);

        }
        reader.close();

        return transfers;
    }

    /**
     * read the calculated transfers subject to the used model.
     * Models:
     * <ul>
     * <li> model = 0: Model MDM1 (Minimal Decomposition Model, type 1)
     * <li> model = 1: Model MDM2 (Minimal Decomposition Model, type 2)
     * <li> model = 2: Model AM (Assignment Model)
     * <li> model = 3: Model TM (Transportation Model)
     * <li> model = 4: Model NM (Network Flow Model)
     * </ul>
     *
     * @return transfers, an ArrayList<Transfer>, which contains the transfers, each transfer is represented by
     * <ul>
     * <li> ID: unique ID for the trip
     * <li> firstTripID: the ID of the first trip
     * <li> secondTripID: the ID of the second trip
     * <li> timeCycleJump: a boolean, which is only used for the assignment model.
     * 		    It is true, if the transfer contains a time cycle jump
     * 		    (a time cycle jump occurs, when a vehicle pauses between two trips
     * 		    untill the next time cycle, e.g. over night).
     * 	</ul>
     */
    public static ArrayList<Transfer> readTransfers() throws IOException {

        Map<String, Integer> models = new HashMap<>();
        models.put("MDM1", 0);
        models.put("MDM2", 1);
        models.put("ASSIGNMENT_MODEL", 2);
        models.put("TRANSPORTATION_MODEL", 3);
        models.put("NETWORK_FLOW_MODEL", 4);

        if (!models.containsKey(modelName)) {
            logger.error("The value of the parameter vs_model in the config file for this algorithm has to be one of " +
                "the following:");
            logger.error("MDM1, MDM2, ASSIGNMENT_MODEL, TRANSPORTATION_MODEL, NETWORK_FLOW_MODEL");
            return null;
        }
        logger.debug("Transfers read from " + transfersFileName);
        BufferedReader reader = new BufferedReader(new FileReader(transfersFileName));
        String line;
        ArrayList<Transfer> transfers = new ArrayList<>();

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            String[] entries = line.split(";");
            int firstTripID = Integer.parseInt(entries[0].trim());
            int secondTripID = Integer.parseInt(entries[1].trim());

            if (models.get(modelName) == 2) {
                boolean timeCycleJump = Boolean.parseBoolean(entries[3].trim());
                Transfer transfer = new Transfer(firstTripID, secondTripID, timeCycleJump);
                transfers.add(transfer);

            } else {
                Transfer transfer = new Transfer(firstTripID, secondTripID);
                transfers.add(transfer);
            }

        }
        reader.close();

        return transfers;
    }

    /**
     * Reads the stops for the network and save them in {@link #stops}.
     * Proceeding: reads the informations out of the File "Stops.giv"
     */
    public static void readStops() throws IOException {

        BufferedReader reader = new BufferedReader(
            new FileReader(stopsFileName));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            String[] entries = line.split(";");
            int ID = Integer.parseInt(entries[0].trim());

            stops.add(ID);

        }
        reader.close();

    }

    /**
     * read the edges of the network.
     * Proceeding: reads the informations out of the file "Edges.giv"
     */
    public static void readEdges() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(edgesFileName));
        String line;

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }


            String[] entries = line.split(";");
            int leftStopID = Integer.parseInt(entries[1].trim());
            int rightStopID = Integer.parseInt(entries[2].trim());
            int lowerBound = Integer.parseInt(entries[4].trim());

            //		double length_in_seconds = length * (3600/speed) * 0.001; // The vehicle has speed "speed" in km/h => speed * (1000/3600) m/s
            //							                   // 	=> length in seconds = length in meter * (3600/1000) * (1/speed) s/m = length in meter * (3600/speed) * (1/1000) (s/m)
            Edge edge = new Edge(leftStopID, rightStopID,
                lowerBound * 60 / timeUnitsPerMinute);

//                Edge edge = new Edge(edgeID, leftStopID, rightStopID,
//                		(int) (length_in_seconds * 100));
            // The value is scaled by 100 to prevent 0-values by the
            // casting to an integer
            edges.add(edge);

        }
        reader.close();

    }

    /**
     * read the events of the network. Proceeding: reads the informations out of
     * the file "Events-expanded.giv"
     *
     * @return events, an ArrayList<Event>, which contains the events of the
     * network, each event is represented by an unique ID (Integer), the
     * time (double), the type (String)
     */
    public static ArrayList<Event> readEvents() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(
            eventsFileName));

        String line;
        ArrayList<Event> events = new ArrayList<>();

        while ((line = reader.readLine()) != null) {

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            String[] entries = line.split(";");
            int eventID = Integer.parseInt(entries[0].trim());
            String type = entries[2].trim();
            int time = (int) (Double.parseDouble(entries[3].trim()));

            if (type.equals("\"departure\"")) {
                type = Event.TYPE_START;
            } else if (type.equals("\"arrival\"")) {
                type = Event.TYPE_END;
            } else {
                logger.error("In the file "
                    + eventsFileName
                    + " exists an event which has the type "
                    + type
                    + ", but only the types \"departure\" and \"arrival\" are allowed.");
            }

            Event event = new Event(eventID, time, type);
            events.add(event);

        }
        reader.close();

        return events;
    }

    public static void calculateMoselInput() throws IOException {
        Map<String, Integer> models = new HashMap<>();
        models.put("MDM1", 0);
        models.put("MDM2", 1);
        models.put("ASSIGNMENT_MODEL", 2);
        models.put("TRANSPORTATION_MODEL", 3);
        models.put("NETWORK_FLOW_MODEL", 4);
        models.put("CANAL_MODEL", 5);

        if (!models.containsKey(modelName)) {
            logger.error("The value of the parameter vs_model in the config file for this algorithm has to be one of " +
                "the following:");
            logger.error("MDM1, MDM2, ASSIGNMENT_MODEL, TRANSPORTATION_MODEL, NETWORK_FLOW_MODEL");
            return;
        }

        if (modelName.toUpperCase().equals("CANAL_MODEL")) {
            CanalNetwork network = calculateCanalNetwork();
            IO.calculateMoselInputVehicleFlow(network);
            IO.printCanalNetwork(network);
        } else {
            ArrayList<Trip> trips;
            trips = readTrips();
            int[][] CompMatrix = VS.calculateCompatibilityMatrix(trips,
                distances, turnOverTime);

            switch (models.get(modelName)) {
                case 0:
                    calculateMoselInputForMDM1(trips);
                    break;
                case 1:
                    calculateMoselInputForMDM2(trips);
                    break;
                case 2:
                    calculateMoselInputForAM(trips, CompMatrix);
                    break;
                case 3:
                    calculateMoselInputForTM(trips, CompMatrix);
                    break;
                case 4:
                    calculateMoselInputForNM(trips, CompMatrix);
                    break;
            }
        }


    }

    public static void calculateMoselInputVehicleFlow(CanalNetwork network) throws IOException {
        String moselVFInputFileName = "../../src/vehicle-scheduling/canal-model/data_vehicle_flow";
        File file = new File(moselVFInputFileName);

        PrintStream pstream = new PrintStream(file);


        pstream.println("NumberOfTrips: " + network.getTrips().size());
        pstream.println("NumberOfTransfers: " + network.getTransfers().size());
        pstream.println("NumberOfCanalEvents: " + network.getTotalNumberOfCanalEvents());
        pstream.println("NumberOfCanals: " + network.getCanals().length);
        pstream.println("NumberOfLineTransfers: " + (2 * network.getTrips().size()));
        pstream.println("timelimit: " + timelimit);
        pstream.println("threads: " + threadLimit);
        pstream.println("outputMessages: " + outputSolverMessages);
        pstream.println("mipGap: " + mipGap);
        pstream.println("writeLpFile: " + writeLpFile);

        pstream.println("! Costs of a single vehicle:");
        pstream.println("cv: " + vehicleCosts);

        pstream.println("! Costs of the transfers:");

        ArrayList<CTransfer> transfers = network.getTransfers();

        StringBuilder transferString = new StringBuilder("C: [ ");
        for (CTransfer cTransfer : transfers) {
            transferString.append(cTransfer.getCosts());
            transferString.append(" ");
        }
        transferString.append("]");
        pstream.println(transferString.toString());

        pstream.println("! Type of Event: 0 == train arrives at canal/start; 1 == train leaves canal/end");

        StringBuilder eventTypeString = new StringBuilder("EventType: [");
        int eventIndex = 1;
        for (Canal canal : network.getCanals()) {
            if (canal == null) continue;
            for (CEvent event : canal.getEvents()) {
                event.setMoselIndex(Integer.toString(eventIndex++));
                eventTypeString.append(event.getType().equals(Event.TYPE_START) ? " 0" : " 1");
            }
        }
        eventTypeString.append(" ]");
        pstream.println(eventTypeString);

        pstream.println("! Array of the Event Indizes, ordered by the order of the given canals and the order of the events in this canals");

        StringBuilder canalEventJavaIndizesString = new StringBuilder("CanalEventJavaIndizes: [ ");
        for (Canal canal : network.getCanals()) {
            if (canal == null) continue;
            for (CEvent event : canal.getEvents()) {
                canalEventJavaIndizesString.append(event.getID());
                canalEventJavaIndizesString.append(" ");
            }
        }
        canalEventJavaIndizesString.append(" ]");
        pstream.println(canalEventJavaIndizesString);

        pstream.println("! Index of the event occuring before this event in the same canal");

        StringBuilder previousEventString = new StringBuilder(network.getCanals().length * 32);
        previousEventString.append("PreviousEvent: [ ");

        for (Canal canal : network.getCanals()) {
            if (canal == null) continue;
            for (CEvent event : canal.getEvents()) {
                previousEventString.append(" ");
                previousEventString.append(canal.getPreviousEvent(event).getMoselIndex());
            }
        }
        previousEventString.append(" ]");
        pstream.println(previousEventString);

        pstream.println("! Transfer associated with each event");
        StringBuilder associatedTransferString = new StringBuilder("AssociatedTransfer: [ ");
        for (Canal canal : network.getCanals()) {
            if (canal == null) continue;
            for (CEvent event : canal.getEvents()) {
                associatedTransferString.append(" ");
                associatedTransferString.append(event.getJourney().getIDString());
            }
        }
        associatedTransferString.append(" ]");
        pstream.println(associatedTransferString);

        pstream.println("! Array of the indizes, which belong to the last event of the canals (length = number of the canals)");
        pstream.println("Jump: [ ");
        for (Canal canal : network.getCanals()) {
            if (canal == null || canal.getEvents().size() <= 0) {
                pstream.print(" -1");
            } else {
                pstream.print(" " + canal.getEvents().get(canal.getEvents().size() - 1).getMoselIndex());
            }
        }
        pstream.println("]");

        pstream.println("! Array of booleans, which states if a canal with the associated index exists");
        pstream.println("CanalExists: [ ");
        for (Canal canal : network.getCanals()) {
            if (canal == null || canal.getEvents().size() <= 0) {
                pstream.print(" " + false);
            } else {
                pstream.print(" " + true);
            }
        }
        pstream.println("]");

        pstream.println("! Array of booleans, which states if a transfer has a time cycle jump");
        StringBuilder TransferWithTimeCycleJumpString = new StringBuilder("TransfersWithTimeCycleJump: [");
        for (CTransfer transfer : transfers) {
            TransferWithTimeCycleJumpString.append(" ");
            TransferWithTimeCycleJumpString.append(transfer.getTimeCycleJump());
        }
        TransferWithTimeCycleJumpString.append(" ]");
        pstream.println(TransferWithTimeCycleJumpString);

        // the following data is only used for the output in the mosel-program

        Set<Integer> startIDs = new HashSet<>();
        pstream.println("! Array of the indizes of the first event of a transfer");
        StringBuilder TransferStartEventString = new StringBuilder("TransferStartEvent: [");
        for (CTransfer transfer : transfers) {
            TransferStartEventString.append(" ");
            TransferStartEventString.append(transfer.getStartEvent().getID());
            startIDs.add(transfer.getStartEvent().getID());
        }
        TransferStartEventString.append(" ]");
        pstream.println(TransferStartEventString);

        pstream.println("! Array of the indizes of the first event of a transfer");
        StringBuilder TransferEndEventString = new StringBuilder("TransferEndEvent: [");
        for (CTransfer transfer : transfers) {
            TransferEndEventString.append(" ");
            TransferEndEventString.append(transfer.getEndEvent().getID());
            if (startIDs.contains(transfer.getEndEvent().getID())) {
                throw new Error("ID " + transfer.getEndEvent().getID() + " used as both start and end");
            }
        }
        TransferEndEventString.append(" ]");
        pstream.println(TransferEndEventString);

        pstream.println("! Array of the types of the transfers");
        StringBuilder TransferTypeString = new StringBuilder("TransferType: [");
        for (CTransfer transfer : transfers) {
            TransferTypeString.append(" ");
            TransferTypeString.append(transfer.getType());
        }
        TransferTypeString.append(" ]");
        pstream.println(TransferTypeString);

        ArrayList<Event> givenEvents = IO.readEvents();
        int numberOfGivenEvents = givenEvents.size();
        pstream.println("NumberOfEvents: " + (numberOfGivenEvents + transfers.size() * 2));

        pstream.println("! Array of the times of the events");
        StringBuilder EventTimeString = new StringBuilder("TimeOfEvents: [");
        int[] timeOfTransfers = new int[numberOfGivenEvents + transfers.size() * 2];
        Arrays.fill(timeOfTransfers, -1);
        for (CTransfer transfer : transfers) {
            timeOfTransfers[transfer.getStartEvent().getID() - 1] = transfer.getStartEvent().getTime();
            timeOfTransfers[transfer.getEndEvent().getID() - 1] = transfer.getEndEvent().getTime();
        }
        for (int timeOfTransfer : timeOfTransfers) {
            EventTimeString.append(timeOfTransfer);
            EventTimeString.append(" ");
        }
        EventTimeString.append("]");
        pstream.println(EventTimeString);

    }

    /**
     * calculate the mosel-input for the model MDM1.
     * Output-File: data_mdm1 in the folder src/vehicle-scheduling/canal-model
     * format:
     * C: [ .... ], where the "...." represents the cost-matrix for the model MDM1 (line by line)
     * NumberOfTrips: trips.size()
     * <p>
     * this means: C[i][j] = 1 		if i=j=0
     * C[i][j] = 0 		if either i=0 or j=0
     * C[i][j] = 0 		if trips i and j are compatible
     * C[i][j] = -\infty 	else
     * <p>
     * remark: in this function, it is used "-1000" instead of -\infty
     *
     * @param trips an ArrayList of the trips, each trip is represented like in the function {@link #readTrips()}
     */
    public static void calculateMoselInputForMDM1(ArrayList<Trip> trips) throws FileNotFoundException {
        String moselInputFileName = "../../src/vehicle-scheduling/canal-model/data_mdm1";
        File file = new File(moselInputFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.print("C: [");

        for (int i = 0; i <= trips.size(); i++) {
            for (int j = 0; j <= trips.size(); j++) {

                if (i == 0) {
                    if (j == 0) {
                        pstream.print(" 1");
                    } else if (j < trips.size()) {
                        pstream.print(" 0");
                    } else {
                        pstream.println(" 0");
                    }
                } else if (j == 0) {
                    pstream.print(" 0");
                } else if ((trips.get(i - 1).getEndStation() == trips.get(j - 1).getStartStation()) && (trips.get(i - 1).getEndTime() + turnOverTime
                    <= trips.get(j - 1).getStartTime())) {
                    if (j < trips.size()) {
                        pstream.print(" 0");
                    } else if (i == trips.size()) {
                        pstream.print(" 0");
                    } else {
                        pstream.println(" 0");
                    }
                } else {
                    if (j < trips.size()) {
                        pstream.print(" -1000");
                    } else if (i == trips.size()) {
                        pstream.print(" -1000");
                    } else {
                        pstream.println(" -1000");
                    }

                }
            }

        }

        pstream.println(" ]");
        pstream.println();
        pstream.println("! The '-1000' is at the places, where rather should be minus infinity.");
        pstream.println();
        pstream.println("NumberOfTrips: " + trips.size());
        pstream.println("timelimit: " + timelimit);
        pstream.println("threads: " + threadLimit);
        pstream.println("outputMessages: " + outputSolverMessages);
        pstream.println("mipGap: " + mipGap);
        pstream.println("writeLpFile: " + writeLpFile);
    }

    /**
     * calculate the mosel-input for the model MDM2.
     * Output-File: data_mdm2 in the folder src/vehicle-scheduling/canal-model
     * format:
     * C: [ .... ], where the "...." represents the cost-matrix for the model MDM2 (line by line)
     * NumberOfTrips: trips.size()
     * <p>
     * this means: C[i][j] = 1 		if trips i and j are compatible
     * C[i][j] = -\infty 	else
     * <p>
     * remark: in this function, it is used "-1000" instead of -\infty
     *
     * @param trips an ArrayList of the trips, each trip is represented like in the function {@link #readTrips()}
     */
    public static void calculateMoselInputForMDM2(ArrayList<Trip> trips) throws FileNotFoundException {
        String moselInputFileName = "../../src/vehicle-scheduling/canal-model/data_mdm2";
        File file = new File(moselInputFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.print("C: [");

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if ((trips.get(i).getEndStation() == trips.get(j).getStartStation()) && (trips.get(i).getEndTime() + turnOverTime <= trips.get(j).getStartTime())) {
                    if (j < trips.size() - 1) {
                        pstream.print(" 1");
                    } else if (i == trips.size() - 1) {
                        pstream.print(" 1");
                    } else {
                        pstream.println(" 1");
                    }
                } else {
                    if (j < trips.size() - 1) {
                        pstream.print(" -1000");
                    } else if (i == trips.size() - 1) {
                        pstream.print(" -1000");
                    } else {
                        pstream.println(" -1000");
                    }

                }
            }


        }

        pstream.println(" ]");
        pstream.println();
        pstream.println("! The '-1000' is at the places, where rather should be minus infinity.");
        pstream.println();
        pstream.println("NumberOfTrips: " + trips.size());
        pstream.println("timelimit: " + timelimit);
        pstream.println("threads: " + threadLimit);
        pstream.println("outputMessages: " + outputSolverMessages);
        pstream.println("mipGap: " + mipGap);
        pstream.println("writeLpFile: " + writeLpFile);
    }

    /**
     * calculate the mosel-input for the model AM.
     * Output-File: data_am in the folder src/vehicle-scheduling/canal-model
     * format:
     * A: [ .... ], where the "...." represents the compatibility matrix (line by line)
     * U: [ .... ], where the "...." represents the matrix with the transfer costs (line by line)
     * cv: vehicleCosts
     * NumberOfTrips: trips.size()
     * <p>
     * this means: A[i][j] = 0 	if trips i and j are compatible
     * A[i][j] = 1	else
     * <p>
     * U[i][j] = costs for the transfer from trip i to trip j (pure transfer costs)
     * = costs for the trip from the end-station of trip i to the start-station of trip j
     * <p>
     * vehicleCosts = costs of a single vehicle
     * <p>
     * remarks: 1. till now, we use the real distances between to trips
     * 2. for two incompatible trips i and j, there should be added the distance from trip i to the depot and from the depot to trip j
     * 3. the costs should be reconsidered
     * FIXME!!!!!
     *
     * @param trips      an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param CompMatrix Compatibility Matrix, calculated in the function {@link VS#calculateCompatibilityMatrix}
     */
    public static void calculateMoselInputForAM(ArrayList<Trip> trips, int[][] CompMatrix) throws FileNotFoundException {
        String moselInputFileName = "../../src/vehicle-scheduling/canal-model/data_am";
        File file = new File(moselInputFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("! Compatibility-Matrix (A(i,j) = 1 <=> i \\bar{\\alpha} j; 0 else):");

        pstream.print("A: [ ");

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (j != trips.size() - 1) {
                    pstream.print(CompMatrix[i][j] + " ");
                } else {
                    pstream.print(CompMatrix[i][j]);
                }
            }
            if (i != trips.size() - 1) {
                pstream.println();
            } else {
                pstream.println(" ]");
            }
        }

        pstream.println();
        pstream.println("! Pure transfer costs (driving costs):");

        pstream.print("U: [ ");

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (j != trips.size() - 1) {
                    pstream.print(distances[trips.get(i).getEndStation() - 1][trips.get(j).getStartStation() - 1] + " ");
                } else {
                    pstream.print(distances[trips.get(i).getEndStation() - 1][trips.get(j).getStartStation() - 1]);
                }
            }
            if (i != trips.size() - 1) {
                pstream.println();
            } else {
                pstream.println(" ]");
            }
        }

        pstream.println();
        pstream.println("! Costs of a single vehicle:");
        pstream.println("cv: " + vehicleCosts);
        pstream.println();
        pstream.println("NumberOfTrips: " + trips.size());
        pstream.println("timelimit: " + timelimit);
        pstream.println("threads: " + threadLimit);
        pstream.println("outputMessages: " + outputSolverMessages);
        pstream.println("mipGap: " + mipGap);
        pstream.println("writeLpFile: " + writeLpFile);
    }

    /**
     * calculate the mosel-input for the model TM.
     * Output-File: data_tm in the folder src/vehicle-scheduling/canal-model
     * format:
     * A: [ .... ], where the "...." represents the compatibility matrix (line by line)
     * U: [ .... ], where the "...." represents the matrix with the transfer costs (line by line)
     * s: [ .... ], where the "...." represents an array with the penalty costs, if a trip isn't served (one value for each trip)
     * cv: vehicleCosts
     * NumberOfTrips: trips.size()
     * <p>
     * this means: A[i][j] = 0 	if trips i and j are compatible
     * A[i][j] = 1	else
     * <p>
     * U[i][j] = costs for the transfer from trip i to trip j (pure transfer costs)
     * = costs for the trip from the end-station of trip i to the start-station of trip j
     * <p>
     * vehicleCosts = costs of a single vehicle
     *
     * @param trips      an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param CompMatrix Compatibility Matrix, calculated in the function {@link VS#calculateCompatibilityMatrix}
     */
    public static void calculateMoselInputForTM(ArrayList<Trip> trips, int[][] CompMatrix) throws FileNotFoundException {
        String moselInputFileName = "../../src/vehicle-scheduling/canal-model/data_tm";
        File file = new File(moselInputFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("! Compatibility-Matrix (A(i,j) = 1 <=> i \\bar{\\alpha} j; 0 else):");

        pstream.print("A: [ ");

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (j != trips.size() - 1) {
                    pstream.print(CompMatrix[i][j] + " ");
                } else {
                    pstream.print(CompMatrix[i][j]);
                }
            }
            if (i != trips.size() - 1) {
                pstream.println();
            } else {
                pstream.println(" ]");
            }
        }

        pstream.println();
        pstream.println("! Pure transfer costs (driving costs):");

        pstream.print("U: [ ");

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (j != trips.size() - 1) {
                    pstream.print(distances[trips.get(i).getEndStation() - 1][trips.get(j).getStartStation() - 1] + " ");
                } else {
                    pstream.print(distances[trips.get(i).getEndStation() - 1][trips.get(j).getStartStation() - 1]);
                }
            }
            if (i != trips.size() - 1) {
                pstream.println();
            } else {
                pstream.println(" ]");
            }
        }

        pstream.println();

        pstream.println("! Penalty costs for not serving a trip:");
        pstream.print("s: [ ");
        for (int i = 0; i < trips.size(); i++) {
            pstream.print(penCosts + " ");
        }
        pstream.println("]");
        pstream.println("! Costs of a single vehicle:");
        pstream.println("cv: " + vehicleCosts);
        pstream.println();
        pstream.println("NumberOfTrips: " + trips.size());
        pstream.println("timelimit: " + timelimit);
        pstream.println("threads: " + threadLimit);
        pstream.println("outputMessages: " + outputSolverMessages);
        pstream.println("mipGap: " + mipGap);
        pstream.println("writeLpFile: " + writeLpFile);
    }

    /**
     * calculate the mosel-input for the model NM.
     * Output-File: data_tm in the folder src/vehicle-scheduling/canal-model
     * format:
     * A: [ .... ], where the "...." represents the compatibility matrix (line by line)
     * U: [ .... ], where the "...." represents the matrix with the transfer costs (line by line)
     * cv: vehicleCosts
     * NumberOfTrips: trips.size()
     * <p>
     * this means: A[i][j] = 0 	if trips i and j are compatible, i,j=trips.size() stands for the depot and is compatible with all trips
     * A[i][j] = 1	else, if i,j != trips.size()
     * A[i][j] = 0	if i=j=trips.size()
     * <p>
     * U[i][j] = costs for the transfer from trip i/the depot to trip j/the depot (pure transfer costs)
     * = costs for the trip from the end-station of trip i/the depot to the start-station of trip j/the depot
     * <p>
     * vehicleCosts = costs of a single vehicle
     *
     * @param trips      an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param CompMatrix Compatibility Matrix, calculated in the function {@link VS#calculateCompatibilityMatrix}
     */
    public static void calculateMoselInputForNM(ArrayList<Trip> trips, int[][] CompMatrix) throws FileNotFoundException {
        String moselInputFileName = "../../src/vehicle-scheduling/canal-model/data_nm";
        File file = new File(moselInputFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("! Compatibility-Matrix (A(i,j) = 1 <=> i \\bar{\\alpha} j; 0 else):");
        pstream.println("! (remark: the last row and the last column stands for the depot; this is compatible with all trips, therefore A(i,j) = 0");

        pstream.print("A: [ ");

        for (int i = 0; i <= trips.size(); i++) {
            if (i != trips.size()) {
                for (int j = 0; j <= trips.size(); j++) {
                    if (j != trips.size()) {
                        pstream.print(CompMatrix[i][j] + ", ");
                    } else {
                        pstream.print("0");
                    }
                }
            } else {
                for (int j = 0; j <= trips.size(); j++) {
                    if (j != trips.size()) {
                        pstream.print("0 ");
                    } else {
                        pstream.print("0");
                    }
                }
            }

            if (i != trips.size()) {
                pstream.println();
            } else {
                pstream.println(" ]");
            }
        }

        pstream.println();
        pstream.println("! Pure transfer costs (driving costs), also to and from the Depot (the depot has the last row/column):");

        pstream.print("U: [ ");

        for (int i = 0; i <= trips.size(); i++) {
            if (i != trips.size()) {
                for (int j = 0; j <= trips.size(); j++) {
                    if (j != trips.size()) {
                        pstream.print(distances[trips.get(i).getEndStation() - 1][trips.get(j).getStartStation() - 1] + " ");
                    } else {
                        pstream.print(distances[trips.get(i).getEndStation() - 1][depot]);
                    }
                }
            } else {
                for (int j = 0; j <= trips.size(); j++) {
                    if (j != trips.size()) {
                        pstream.print(distances[depot][trips.get(j).getStartStation() - 1] + " ");
                    } else {
                        pstream.print(vehicleCosts); // the transfer within the depot gets the costs of a single vehicle
                    }
                }
            }

            if (i != trips.size()) {
                pstream.println();
            } else {
                pstream.println(" ]");
            }
        }

        pstream.println();
        pstream.println("! Costs of a single vehicle:");
        pstream.println("cv: " + vehicleCosts);
        pstream.println();
        pstream.println("NumberOfTrips: " + trips.size());
        pstream.println("timelimit: " + timelimit);
        pstream.println("threads: " + threadLimit);
        pstream.println("outputMessages: " + outputSolverMessages);
        pstream.println("mipGap: " + mipGap);
        pstream.println("writeLpFile: " + writeLpFile);
    }

    public static void calculateVSFile(ArrayList<Trip> trips, ArrayList<Transfer> transfers) throws IOException {
        Map<String, Integer> models = new HashMap<>();
        models.put("MDM1", 0);
        models.put("MDM2", 1);
        models.put("ASSIGNMENT_MODEL", 2);
        models.put("TRANSPORTATION_MODEL", 3);
        models.put("NETWORK_FLOW_MODEL", 4);

        if (!models.containsKey(modelName)) {
            logger.error("The value of the parameter vs_model in the config file for this algorithm has to be one of " +
                "the following:");
            logger.error("MDM1, MDM2, ASSIGNMENT_MODEL, TRANSPORTATION_MODEL, NETWORK_FLOW_MODEL");
            return;
        }

        switch (models.get(modelName)) {
            case 0:
                calculateVSFileMDM1(trips, transfers);
                break;
            case 1:
                calculateVSFileMDM2(trips, transfers);
                break;
            case 2:
                calculateVSFileAM(trips, transfers);
                break;
            case 3:
                calculateVSFileTM(trips, transfers);
                break;
            case 4:
                calculateVSFileNM(trips, transfers);
                break;
        }
    }

    /**
     * calculate the output-file vehicle-scheduling/Vehicle_Schedules_MDM1.
     * Every line stands for one single vehicle and lists the assigned trips in the right order,
     * format: vehicleID; tripsID's seperated by ";"
     *
     * @param trips     an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param transfers an ArrayList of the transfers, each transfer is represented like in the function
     *                  {@link #readTransfers}
     */
    public static void calculateVSFileMDM1(ArrayList<Trip> trips, ArrayList<Transfer> transfers) throws FileNotFoundException {
        File file = new File(vehicleSchedulesFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file shows the schedules of the used vehicles, calculated by the model MDM1.");
        pstream.println("#");
        pstream.println("# In this model, the schedules are calculated separately for every vehicle. Because of that, ");
        pstream.println("# every schedule for a vehicle begins with an empty trip from the depot at the beginning of the day");
        pstream.println("# and ends with an empty trip to the depot at the end of a day.");
        pstream.println("# The depot gets as ID, periodic-ID and station-ID the value \"-1\", because it isn't really fixed.");
        pstream.println("# All empty trips from the depot begin at start-time \"0\" and all trip to the depot end at end-time \"86400\".");
        pstream.println("#");
        pstream.println("# Every line stands for one single trip performed by a vehicle.");
        pstream.print("# Format: circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; periodic-start-ID; start-station; start-time;");
        pstream.println(" end-ID; periodic-end-id; end-station; end-time; line");

        boolean[][] transfersMatrix = new boolean[trips.size() + 1][trips.size() + 1];
        for (int i = 0; i <= trips.size(); i++) {
            for (int j = 0; i <= trips.size(); i++) {
                transfersMatrix[i][j] = false;
            }
        }


        for (Transfer transfer : transfers) {
            transfersMatrix[transfer.getFirstTripID()][transfer.getSecondTripID()] = true;
        }

        boolean[] beginningTrip = new boolean[trips.size()];

        for (int i = 1; i <= trips.size(); i++) {
            beginningTrip[i - 1] = true;
            for (int j = 1; j <= trips.size(); j++) {
                if (transfersMatrix[j][i]) {
                    beginningTrip[i - 1] = false;
                    break;
                }
            }
        }

        boolean[] tripPrinted = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            tripPrinted[i] = false;
        }

        int[] nextTripInCirculation = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            nextTripInCirculation[i] = -1;
        }

        for (int i = 1; i <= trips.size(); i++) {
            for (int j = 1; j <= trips.size(); j++) {
                if (transfersMatrix[i][j]) {
                    nextTripInCirculation[i - 1] = j;
                }
            }
        }

        int circulationNumber = 0;
        int vehicleNumber = 1;
        int tripNumberOfVehicle = 1;


        for (int i = 1; i <= trips.size(); i++) {
            if (!beginningTrip[i - 1]) {
                continue;
            }

            int currentTripID = i;

            if (!tripPrinted[currentTripID - 1]) {
                circulationNumber++;
            }

            pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + "-1" + "; " + "-1"
                + "; " + "-1" + "; " + "0" + "; " + trips.get(i - 1).getStartID() + "; " + trips.get(i - 1).getPeriodicStartID()
                + "; " + trips.get(i - 1).getStartStation() + "; " + trips.get(i - 1).getStartTime() + "; " + "-1");
            tripNumberOfVehicle++;


            while (!tripPrinted[currentTripID - 1]) {
                Trip currentTrip = trips.get(currentTripID - 1);
                pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
                tripPrinted[currentTripID - 1] = true;
                tripNumberOfVehicle++;
                Trip nextTrip;
                if (nextTripInCirculation[currentTripID - 1] > 0) {
                    nextTrip = trips.get(nextTripInCirculation[currentTripID - 1] - 1);
                } else {
                    nextTrip = currentTrip;
                }
                if (!transfersMatrix[currentTripID][0]) {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                } else {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + "-1" + "; "
                        + "-1" + "; " + "-1" + "; " + "86400" + "; " + "-1");
                    vehicleNumber++;
                }
                tripNumberOfVehicle++;
                currentTripID = nextTrip.getID();
            }
        }

    }

    /**
     * calculate the output-file vehicle-scheduling/Vehicle_Schedules_MDM2.
     * Every line stands for one single vehicle and lists the assigned trips in the right order,
     * format: vehicleID; tripsID's seperated by ";"
     *
     * @param trips     an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param transfers an ArrayList of the transfers, each transfer is represented like in the function
     *                  {@link #readTransfers}
     */
    public static void calculateVSFileMDM2(ArrayList<Trip> trips, ArrayList<Transfer> transfers) throws FileNotFoundException {
        File file = new File(vehicleSchedulesFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file shows the schedules of the used vehicles, calculated by the model MDM2");
        pstream.println("#");
        pstream.println("# In this model, the schedules are calculated separately for every vehicle. Because of that, ");
        pstream.println("# every schedule for a vehicle begins with an empty trip from the depot at the beginning of the day");
        pstream.println("# and ends with an empty trip to the depot at the end of a day.");
        pstream.println("# The depot gets as ID, periodic-ID and station-ID the value \"-1\", because it isn't really fixed.");
        pstream.println("# All empty trips from the depot begin at start-time \"0\" and all trip to the depot end at end-time \"86400\".");
        pstream.println("#");
        pstream.println("# Every line stands for one single trip performed by a vehicle.");
        pstream.print("# Format: circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; periodic-start-ID; start-station; start-time;");
        pstream.println(" end-ID; periodic-end-id; end-station; end-time; line");

        boolean[][] transfersMatrix = new boolean[trips.size()][trips.size()];
        for (int i = 0; i <= trips.size(); i++) {
            for (int j = 0; i < trips.size(); i++) {
                transfersMatrix[i][j] = false;
            }
        }

        for (Transfer transfer : transfers) {
            transfersMatrix[transfer.getFirstTripID() - 1][transfer.getSecondTripID() - 1] = true;
        }

        boolean[] beginningTrip = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            beginningTrip[i] = true;
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[j][i]) {
                    beginningTrip[i] = false;
                    break;
                }
            }
        }

        boolean[] endingTrip = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            endingTrip[i] = true;
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j]) {
                    endingTrip[i] = false;
                    break;
                }
            }
        }

        boolean[] tripPrinted = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            tripPrinted[i] = false;
        }

        int[] nextTripInCirculation = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j]) {
                    nextTripInCirculation[i] = j;
                }
            }
        }

        int circulationNumber = 0;
        int vehicleNumber = 1;
        int tripNumberOfVehicle = 1;


        for (int i = 1; i <= trips.size(); i++) {
            if (!beginningTrip[i - 1]) {
                continue;
            }

            int currentTripID = i;

            if (!tripPrinted[currentTripID - 1]) {
                circulationNumber++;
            }

            pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + "-1" + "; " + "-1"
                + "; " + "-1" + "; " + "0" + "; " + trips.get(i - 1).getStartID() + "; " + trips.get(i - 1).getPeriodicStartID()
                + "; " + trips.get(i - 1).getStartStation() + "; " + trips.get(i - 1).getStartTime() + "; " + "-1");
            tripNumberOfVehicle++;


            while (!tripPrinted[currentTripID - 1]) {
                Trip currentTrip = trips.get(currentTripID - 1);
                pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
                tripPrinted[currentTripID - 1] = true;
                tripNumberOfVehicle++;
                Trip nextTrip = trips.get(nextTripInCirculation[currentTripID - 1]);
                if (!endingTrip[currentTripID - 1]) {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                } else {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + "-1" + "; "
                        + "-1" + "; " + "-1" + "; " + "86400" + "; " + "-1");
                    vehicleNumber++;
                }
                tripNumberOfVehicle++;
                currentTripID = nextTrip.getID();
            }
        }


    }

    public static void checkAssignments(ArrayList<Trip> trips, ArrayList<Transfer> transfers) {
        boolean allright = true;

        int[] numberOfLeftDepartures = new int[stops.size()];
        for (Transfer transfer : transfers) {
            numberOfLeftDepartures[trips.get(transfer.getFirstTripID() - 1).getEndStation() - 1]++;
            numberOfLeftDepartures[trips.get(transfer.getSecondTripID() - 1).getStartStation() - 1]--;
        }

        for (Trip trip : trips) {
            numberOfLeftDepartures[trip.getStartStation() - 1]++;
            numberOfLeftDepartures[trip.getEndStation() - 1]--;
        }

        for (int numberOfLeftDeparture : numberOfLeftDepartures) {
            if (numberOfLeftDeparture != 0) {
                allright = false;
                break;
            }
        }

        if (!allright) {
            logger.error("The assignment model has a station, which doesn't have the same number of entries and departures!");

            logger.error("Details: (Station, Delta Outgoing)");
            for (int i = 0; i < numberOfLeftDepartures.length; ++i) {
                if (numberOfLeftDepartures[i] != 0) {
                    logger.error((i + 1) + ": " + numberOfLeftDepartures[i]);
                }
            }
        }

        int minDistanceBetweenTwoTripsAtTheSameStation = Integer.MAX_VALUE;

        for (Transfer transfer : transfers) {
            if (trips.get(transfer.getFirstTripID() - 1).getEndStation() == trips.get(transfer.getSecondTripID() - 1).getStartStation()) {
                if (minDistanceBetweenTwoTripsAtTheSameStation > trips.get(transfer.getSecondTripID() - 1).getStartTime() - trips.get(transfer.getFirstTripID() - 1).getEndTime()) {
                    minDistanceBetweenTwoTripsAtTheSameStation = trips.get(transfer.getSecondTripID() - 1).getStartTime() - trips.get(transfer.getFirstTripID() - 1).getEndTime();
                }
            }
        }
        logger.debug("Minimal distance between two trips, where the first trip ends at the same station where the second trip began: "
            + minDistanceBetweenTwoTripsAtTheSameStation);

        int minDistanceBetweenTwoTripsDifferentStations = Integer.MAX_VALUE;

        for (Transfer transfer : transfers) {
            if (trips.get(transfer.getFirstTripID() - 1).getEndStation() != trips.get(transfer.getSecondTripID() - 1).getStartStation()) {
                if (minDistanceBetweenTwoTripsDifferentStations > trips.get(transfer.getSecondTripID() - 1).getStartTime() - trips.get(transfer.getFirstTripID() - 1).getEndTime()) {
                    minDistanceBetweenTwoTripsDifferentStations = trips.get(transfer.getSecondTripID() - 1).getStartTime() - trips.get(transfer.getFirstTripID() - 1).getEndTime();
                }
            }
        }
        logger.debug("Minimal distance between two trips, where the first trip ends at an other station than where the second trip began: "
            + minDistanceBetweenTwoTripsDifferentStations
        );

    }

    /**
     * calculate the output-file vehicle-scheduling/Vehicle_Schedules_AM.
     * Every line stands for one single vehicle and lists the assigned trips in the right order,
     * format: vehicleID; tripsID's seperated by ";"
     *
     * @param trips     an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param transfers an ArrayList of the transfers, each transfer is represented like in the function
     *                  {@link #readTransfers}
     */
    public static void calculateVSFileAM(ArrayList<Trip> trips, ArrayList<Transfer> transfers) throws IOException {

        checkAssignments(trips, transfers);

        File file = new File(vehicleSchedulesFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file shows the schedules of the used vehicles, calculated by the model AM");
        pstream.println("# Every line stands for one single trip performed by a vehicle.");

        pstream.print("# The number of needed vehicles is ");

        int numberOfVehicles = 0;

        for (Transfer transfer : transfers) {
            if (transfer.getValueTimeCycleJump()) {
                numberOfVehicles++;
            }
        }

        numberOfVehicles = numberOfVehicles + trips.size() - transfers.size();

        pstream.println(numberOfVehicles);

        pstream.print("# Format: circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; periodic-start-ID; start-station; start-time;");
        pstream.println(" end-ID; periodic-end-id; end-station; end-time; line");

        int[][] transfersMatrix = new int[trips.size()][trips.size()];
        for (int i = 0; i <= trips.size(); i++) {
            for (int j = 0; i < trips.size(); i++) {
                transfersMatrix[i][j] = 0;
            }
        }

        for (Transfer transfer : transfers) {
            if (!transfer.getValueTimeCycleJump()) {
                transfersMatrix[transfer.getFirstTripID() - 1][transfer.getSecondTripID() - 1] = 1;
            } // in this case, the value of the transfersMatrix is only true, if the transfer contains no time cycle jump
            else {
                transfersMatrix[transfer.getFirstTripID() - 1][transfer.getSecondTripID() - 1] = -1;
            }
        }

        boolean[] beginningTrip = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            beginningTrip[i] = true;
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[j][i] == 1) {
                    beginningTrip[i] = false;
                    break;
                }
            }
        }

        boolean[] tripPrinted = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            tripPrinted[i] = false;
        }

        int[] nextTripInCirculation = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j] != 0) {
                    nextTripInCirculation[i] = j;
                }
            }
        }

        int[] numberOfZsAfterTrip = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j] < 0) {
                    numberOfZsAfterTrip[i] = -transfersMatrix[i][j];
                } else if (transfersMatrix[i][j] == 1) {
                    numberOfZsAfterTrip[i] = 0;
                }
            }
        }


        int circulationNumber = 0;
        int vehicleNumber = 1;
        int tripNumberOfVehicle = 1;

        for (int i = 1; i <= trips.size(); i++) {
            if (!beginningTrip[i - 1]) {
                continue;
            }

            int currentTripID = i;

            if (!tripPrinted[currentTripID - 1]) {
                circulationNumber++;
            }

            while (!tripPrinted[currentTripID - 1]) {
                Trip currentTrip = trips.get(currentTripID - 1);
                pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
                tripPrinted[currentTripID - 1] = true;
                tripNumberOfVehicle++;
                Trip nextTrip = trips.get(nextTripInCirculation[currentTripID - 1]);
                if (numberOfZsAfterTrip[currentTripID - 1] == 0) {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                } else {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                    vehicleNumber += numberOfZsAfterTrip[currentTripID - 1];
                }
                tripNumberOfVehicle++;
                currentTripID = nextTrip.getID();
            }
        }

        for (int i = 0; i < tripPrinted.length; i++) {
            if (!tripPrinted[i]) {
                Trip currentTrip = trips.get(i);
                logger.debug("Trip " + (i + 1) + ": TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
            }
        }

    }

    /**
     * calculate the output-file vehicle-scheduling/Vehicle_Schedules_TM.
     * Every line stands for one single vehicle and lists the assigned trips in the right order,
     * format: vehicleID; tripsID's seperated by ";"
     *
     * @param trips     an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param transfers an ArrayList of the transfers, each transfer is represented like in the function
     *                  {@link #readTransfers}
     */
    public static void calculateVSFileTM(ArrayList<Trip> trips, ArrayList<Transfer> transfers) throws FileNotFoundException {
        File file = new File(vehicleSchedulesFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file shows the schedules of the used vehicles, calculated by the model TM.");
        pstream.println("#");
        pstream.println("# In this model, the schedules are calculated separately for every vehicle. Because of that, ");
        pstream.println("# every schedule for a vehicle begins with an empty trip from the depot at the beginning of the day");
        pstream.println("# and ends with an empty trip to the depot at the end of a day.");
        pstream.println("# The depot gets as ID, periodic-ID and station-ID the value \"-1\", because it isn't really fixed.");
        pstream.println("# All empty trips from the depot begin at start-time \"0\" and all trip to the depot end at end-time \"86400\".");
        pstream.println("#");
        pstream.println("# Every line stands for one single trip performed by a vehicle.");
        pstream.print("# Format: circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; periodic-start-ID; start-station; start-time;");
        pstream.println(" end-ID; periodic-end-id; end-station; end-time; line");

        boolean[][] transfersMatrix = new boolean[trips.size() + 1][trips.size() + 1];
        for (int i = 0; i <= trips.size(); i++) {
            for (int j = 0; i <= trips.size(); i++) {
                transfersMatrix[i][j] = false;
            }
        }


        for (Transfer transfer : transfers) {
            transfersMatrix[transfer.getFirstTripID() - 1][transfer.getSecondTripID() - 1] = true;
        }

        boolean[] beginningTrip = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            beginningTrip[i] = true;
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[j][i]) {
                    beginningTrip[i] = false;
                    break;
                }
            }
        }

        boolean[] tripPrinted = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            tripPrinted[i] = false;
        }

        int[] nextTripInCirculation = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            nextTripInCirculation[i] = -1;
        }

        // In this loop the depot isn't considered, because it isn't really allowed as successor.
        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j]) {
                    nextTripInCirculation[i] = j;
                }
            }
        }

        int circulationNumber = 0;
        int vehicleNumber = 1;
        int tripNumberOfVehicle = 1;


        for (int i = 1; i <= trips.size(); i++) {
            if (!beginningTrip[i - 1]) {
                continue;
            }

            int currentTripID = i;

            if (!tripPrinted[currentTripID - 1]) {
                circulationNumber++;
            }

            pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + "-1" + "; " + "-1"
                + "; " + "-1" + "; " + "0" + "; " + trips.get(i - 1).getStartID() + "; " + trips.get(i - 1).getPeriodicStartID()
                + "; " + trips.get(i - 1).getStartStation() + "; " + trips.get(i - 1).getStartTime() + "; " + "-1");
            tripNumberOfVehicle++;


            while (!tripPrinted[currentTripID - 1]) {
                Trip currentTrip = trips.get(currentTripID - 1);
                pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
                tripPrinted[currentTripID - 1] = true;
                tripNumberOfVehicle++;
                Trip nextTrip;
                if (nextTripInCirculation[currentTripID - 1] >= 0) {
                    nextTrip = trips.get(nextTripInCirculation[currentTripID - 1]);
                } else {
                    nextTrip = currentTrip;
                }
                if (!transfersMatrix[currentTripID - 1][trips.size()]) {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                } else {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + "-1" + "; "
                        + "-1" + "; " + "-1" + "; " + "86400" + "; " + "-1");
                    vehicleNumber++;
                }
                tripNumberOfVehicle++;
                currentTripID = nextTrip.getID();
            }
        }

    }

    /**
     * calculate the output-file vehicle-scheduling/Vehicle_Schedules_NM.
     * Every line stands for one single vehicle and lists the assigned trips in the right order,
     * format: vehicleID; tripsID's seperated by ";"
     *
     * @param trips     an ArrayList of the trips, each trip is represented like in the function {@link #readTrips}
     * @param transfers an ArrayList of the transfers, each transfer is represented like in the function
     *                  {@link #readTransfers}
     */
    public static void calculateVSFileNM(ArrayList<Trip> trips, ArrayList<Transfer> transfers) throws FileNotFoundException {
        File file = new File(vehicleSchedulesFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file shows the schedules of the used vehicles, calculated by the model NM.");
        pstream.println("#");
        pstream.println("# In this model, the schedules are calculated separately for every vehicle. Because of that, ");
        pstream.println("# every schedule for a vehicle begins with an empty trip from the depot at the beginning of the day");
        pstream.println("# and ends with an empty trip to the depot at the end of a day.");
        pstream.println("# The station-ID of the depot is defined by the value \"vs_depot_index\" in the config.");
        pstream.println("# The events in the depot get the value \"-1\" as ID and periodic-ID (they do not really exist).");
        pstream.println("# All empty trips from the depot begin at start-time \"0\" and all trip to the depot end at end-time \"86400\".");
        pstream.println("#");
        pstream.println("# Every line stands for one single trip performed by a vehicle.");
        pstream.print("# Format: circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; periodic-start-ID; start-station; start-time;");
        pstream.println(" end-ID; periodic-end-id; end-station; end-time; line");

        boolean[][] transfersMatrix = new boolean[trips.size() + 1][trips.size() + 1];
        for (int i = 0; i <= trips.size(); i++) {
            for (int j = 0; i <= trips.size(); i++) {
                transfersMatrix[i][j] = false;
            }
        }


        for (Transfer transfer : transfers) {
            transfersMatrix[transfer.getFirstTripID() - 1][transfer.getSecondTripID() - 1] = true;
        }

        boolean[] beginningTrip = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            beginningTrip[i] = true;
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[j][i]) {
                    beginningTrip[i] = false;
                    break;
                }
            }
        }

        boolean[] tripPrinted = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            tripPrinted[i] = false;
        }

        int[] nextTripInCirculation = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            nextTripInCirculation[i] = -1;
        }

        // In this loop the depot isn't considered, because it isn't really allowed as successor.
        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j]) {
                    nextTripInCirculation[i] = j;
                }
            }
        }

        int circulationNumber = 0;
        int vehicleNumber = 1;
        int tripNumberOfVehicle = 1;


        for (int i = 1; i <= trips.size(); i++) {
            if (!beginningTrip[i - 1]) {
                continue;
            }

            int currentTripID = i;

            if (!tripPrinted[currentTripID - 1]) {
                circulationNumber++;
            }

            pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + "-1" + "; " + "-1"
                + "; " + depot + "; " + "0" + "; " + trips.get(i - 1).getStartID() + "; " + trips.get(i - 1).getPeriodicStartID()
                + "; " + trips.get(i - 1).getStartStation() + "; " + trips.get(i - 1).getStartTime() + "; " + "-1");
            tripNumberOfVehicle++;


            while (!tripPrinted[currentTripID - 1]) {
                Trip currentTrip = trips.get(currentTripID - 1);
                pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
                tripPrinted[currentTripID - 1] = true;
                tripNumberOfVehicle++;
                Trip nextTrip;
                if (nextTripInCirculation[currentTripID - 1] >= 0) {
                    nextTrip = trips.get(nextTripInCirculation[currentTripID - 1]);
                } else {
                    nextTrip = currentTrip;
                }
                if (!transfersMatrix[currentTripID - 1][trips.size()]) {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                } else {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + "-1" + "; "
                        + "-1" + "; " + depot + "; " + "86400" + "; " + "-1");
                    vehicleNumber++;
                }
                tripNumberOfVehicle++;
                currentTripID = nextTrip.getID();
            }
        }

    }

    public static CanalNetwork calculateCanalNetwork() throws IOException {
        CanalNetwork network = new CanalNetwork();

        ArrayList<CTrip> trips;
        trips = IO.readCTrips();

        network.setTrips(trips);

        VS.calculateCanalsAndTransfers(network, stops, vehicleCosts, distances, turnOverTime);

        return network;
    }

    public static void checkAssignments(ArrayList<CTrip> trips, int[][] transfersMatrix) {

        int minDistanceBetweenTwoTripsAtTheSameStation = Integer.MAX_VALUE;

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j] == 1) {
                    if (trips.get(i).getEndStation() == trips.get(j).getStartStation()) {
                        if (minDistanceBetweenTwoTripsAtTheSameStation >
                            trips.get(j).getStartTime() - trips.get(i).getEndTime()) {
                            minDistanceBetweenTwoTripsAtTheSameStation = trips.get(j).getStartTime() - trips.get(i).getEndTime();
                        }
                    }
                }
            }
        }

        logger.debug("Minimal distance between two trips, where the first trip ends at the same station where the second trip began: "
            + minDistanceBetweenTwoTripsAtTheSameStation);

        int minDistanceBetweenTwoTripsAtDifferentStations = Integer.MAX_VALUE;

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j] == 1) {
                    if (trips.get(i).getEndStation() != trips.get(j).getStartStation()) {
                        if (minDistanceBetweenTwoTripsAtDifferentStations >
                            trips.get(j).getStartTime() - trips.get(i).getEndTime()) {
                            minDistanceBetweenTwoTripsAtDifferentStations = trips.get(j).getStartTime() - trips.get(i).getEndTime();
                        }
                    }
                }
            }
        }

        logger.debug("Minimal distance between two trips, where the first trip ends at an other station than where the second trip began: "
            + minDistanceBetweenTwoTripsAtDifferentStations);

    }

    public static void calculateVSFileCanal(Canal[] newCanals,
                                            ArrayList<CTrip> trips, ArrayList<CTransfer> occuringTransfers,
                                            HashMap<Canal, HashMap<CEvent, CEvent>> mappings)
        throws IOException {


        HashMap<Integer, Integer> eventsOfTheLines = new HashMap<>();
        HashMap<Integer, Integer> eventsOfTheOccuringTransfers = new HashMap<>();
        HashMap<Integer, Integer> eventsOfTheMappings = new HashMap<>();
        HashMap<Integer, Integer> lineIDforGivenEventIDs = new HashMap<>();

        HashMap<Integer, CTransfer> transferForGivenEventIDs = new HashMap<>();

        int greatestLineID = 0;

        for (CTransfer transfer : occuringTransfers) {
            int startID = transfer.getStartEvent().getID();
            int endID = transfer.getEndEvent().getID();

            transferForGivenEventIDs.put(startID, transfer);
            transferForGivenEventIDs.put(endID, transfer);

            eventsOfTheOccuringTransfers.put(transfer.getStartEvent().getID(), transfer.getEndEvent().getID());
        }

        for (CTrip trip : trips) {
            int startID = trip.getStartID();
            int endID = trip.getEndID();

            eventsOfTheLines.put(startID, endID);

            lineIDforGivenEventIDs.put(startID, trip.getID());
            lineIDforGivenEventIDs.put(endID, trip.getID());

            if (startID > greatestLineID) {
                greatestLineID = startID;
            }
            if (endID > greatestLineID) {
                greatestLineID = endID;
            }
        }

        for (Canal canal : newCanals) {
            if (canal == null || canal.getEvents() == null || canal.getEvents().size() <= 0) {
                continue;
            }
            HashMap<CEvent, CEvent> canalMappings = mappings.get(canal);
            if (canalMappings.size() <= 0) {
                logger.debug("No mappings!");
            }

            ArrayList<CEvent> events = canal.getEvents();

            for (CEvent event : events) {
                if (event.getType().equals("END")) {
                    continue;
                }
                if (canalMappings.get(event) == null) {
                    logger.debug("Event " + event.getID() + " doesn't have a successor!");
                    continue;
                }
                eventsOfTheMappings.put(event.getID(), canalMappings.get(event).getID());
            }
        }

        File file = new File(vehicleSchedulesFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file shows the schedules of the used vehicles, calculated by the canal model.");
        pstream.println("# Every line stands for one single trip performed by a vehicle.");
        pstream.print("# Format: circulation-ID; vehicle-ID; trip-number of this vehicle; type; start-ID; periodic-start-ID; start-station; start-time;");
        pstream.println(" end-ID; periodic-end-id; end-station; end-time; line");

        // This matrix shows, if trip j is served directly after trip i by the same vehicle. Only in this case, transfersMatrix[i][j] will be 1.
        int[][] transfersMatrix = VS.calculateTransfersMatrix(trips,
            lineIDforGivenEventIDs, eventsOfTheLines,
            eventsOfTheOccuringTransfers, eventsOfTheMappings,
            distances, transferForGivenEventIDs, turnOverTime);

        checkAssignments(trips, transfersMatrix);


        boolean[] beginningTrip = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            beginningTrip[i] = true;
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[j][i] == 1) {
                    beginningTrip[i] = false;
                    break;
                }
            }
        }

        boolean[] tripPrinted = new boolean[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            tripPrinted[i] = false;
        }

        int[] nextTripInCirculation = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j] != 0) {
                    nextTripInCirculation[i] = j;
                }
            }
        }

        int[] numberOfZsAfterTrip = new int[trips.size()];

        for (int i = 0; i < trips.size(); i++) {
            for (int j = 0; j < trips.size(); j++) {
                if (transfersMatrix[i][j] < 0) {
                    numberOfZsAfterTrip[i] = -transfersMatrix[i][j];
                } else if (transfersMatrix[i][j] == 1) {
                    numberOfZsAfterTrip[i] = 0;
                }
            }
        }

        int circulationNumber = 0;
        int vehicleNumber = 1;
        int tripNumberOfVehicle = 1;


        for (int i = 1; i <= trips.size(); i++) {
            if (!beginningTrip[i - 1]) {
                continue;
            }

            int currentTripID = i;

            if (!tripPrinted[currentTripID - 1]) {
                circulationNumber++;
            }

            while (!tripPrinted[currentTripID - 1]) {
                CTrip currentTrip = trips.get(currentTripID - 1);
                pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
                tripPrinted[currentTripID - 1] = true;
                tripNumberOfVehicle++;
                CTrip nextTrip = trips.get(nextTripInCirculation[currentTripID - 1]);
                if (numberOfZsAfterTrip[currentTripID - 1] == 0) {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                } else {
                    pstream.println(circulationNumber + "; " + vehicleNumber + "; " + tripNumberOfVehicle + "; " + "EMPTY" + "; " + currentTrip.getEndID() + "; "
                        + currentTrip.getPeriodicEndID() + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + nextTrip.getStartID() + "; "
                        + nextTrip.getPeriodicStartID() + "; " + nextTrip.getStartStation() + "; " + nextTrip.getStartTime() + "; " + "-1");
                    vehicleNumber += numberOfZsAfterTrip[currentTripID - 1];
                }
                tripNumberOfVehicle++;
                currentTripID = nextTrip.getID();
            }
        }

        for (int i = 0; i < tripPrinted.length; i++) {
            if (!tripPrinted[i]) {
                CTrip currentTrip = trips.get(i);
                logger.debug("Trip " + (i + 1) + ": TRIP" + "; " + currentTrip.getStartID() + "; " + currentTrip.getPeriodicStartID()
                    + "; " + currentTrip.getStartStation() + "; " + currentTrip.getStartTime() + "; " + currentTrip.getEndID() + "; " + currentTrip.getPeriodicEndID()
                    + "; " + currentTrip.getEndStation() + "; " + currentTrip.getEndTime() + "; " + currentTrip.getLineID());
            }
        }

    }

    public static void printCanalNetwork(CanalNetwork network) throws FileNotFoundException {
        if (!verbose) return;

        File file = new File(canalNetworkFileName);

        PrintStream pstream = new PrintStream(file);

        pstream.println("# This file contains the informations of the canal network of the model.");
        pstream.println("# The trips are the given trips which should be served, the transfers are the possible connections between them.");
        pstream.println("# The canals are the produced canals of the model. The types of the canals are \"DRIVING\", \"PARKING\" and \"MAINTAINING\".");

        // print the trips
        pstream.println("#Trips: (format = ID; startID; startStation; startTime; endID; endStation; endTime)");

        for (int i = 0; i < network.getTrips().size(); i++) {
            CTrip trip = network.getTrips().get(i);
            pstream.println(trip.getID() + "; " + trip.getStartID() + "; " + trip.getStartStation() + "; " + trip.getStartTime() + "; "
                + trip.getEndID() + "; " + trip.getEndStation() + "; " + trip.getEndTime());
        }
        pstream.println();


        // print the transfers
        pstream.println("#Transfers: (format = ID; startEvent (with format 'ID ; time; type'); endEvent (with format 'ID ; time; type'); costs; timeCycleJump; type)");
        StringBuilder transfersString = new StringBuilder();
        for (CTransfer currentTransfer : network.getTransfers()) {
            transfersString.append(currentTransfer.getID());
            transfersString.append("; ");
            final Event startEvent = currentTransfer.getStartEvent();
            transfersString.append(startEvent.getID());
            transfersString.append("; ");
            transfersString.append(startEvent.getTime());
            transfersString.append("; ");
            transfersString.append(startEvent.getType());
            transfersString.append("; ");
            final Event endEvent = currentTransfer.getEndEvent();
            transfersString.append(endEvent.getID());
            transfersString.append("; ");
            transfersString.append(endEvent.getTime());
            transfersString.append("; ");
            transfersString.append(endEvent.getType());
            transfersString.append("; ");
            transfersString.append(currentTransfer.getCosts());
            transfersString.append("; ");
            transfersString.append(currentTransfer.getTimeCycleJump());
            transfersString.append("; ");
            transfersString.append(currentTransfer.getType());
            transfersString.append("\n");
        }
        pstream.print(transfersString);
        pstream.println();

        // print the canals
        pstream.println("#Canals: (format = ID; stationID; type; events separated by \";\" with the format 'ID ; time; type')");
        StringBuilder canalString = new StringBuilder(network.getCanals().length * 65536);
        for (Canal currentCanal : network.getCanals()) {
            if (currentCanal == null) continue;
            if (currentCanal.getEvents() == null) continue;
            if (currentCanal.getEvents().isEmpty()) continue;

            canalString.append(currentCanal.getID());
            canalString.append("; ");
            canalString.append(currentCanal.getStationID());
            canalString.append("; ");
            canalString.append(currentCanal.getType());

            for (CEvent currentEvent : currentCanal.getEvents()) {
                canalString.append("; ");
                canalString.append(currentEvent.getID());
                canalString.append("; ");
                canalString.append(currentEvent.getTime());
                canalString.append("; ");
                canalString.append(currentEvent.getType());
            }

            canalString.append("\n\n");
        }
        pstream.print(canalString);
    }

    public static void initialize(Config config) throws Exception {

        modelName = config.getStringValue("vs_model").toUpperCase();
        vehicleCosts = (int) config.getDoubleValue("vs_vehicle_costs");
        boolean ptnIsUndirected = config.getBooleanValue("ptn_is_undirected");
        penCosts = config.getIntegerValue("vs_penalty_costs");
        depot = config.getIntegerValue("vs_depot_index");
        timeUnitsPerMinute = config.getIntegerValue("time_units_per_minute");
        // convert turnover time from time units to seconds
        turnOverTime = config.getIntegerValue("vs_turn_over_time") * 60 / timeUnitsPerMinute;
        stopsFileName = config.getStringValue("default_stops_file");
        edgesFileName = config.getStringValue("default_edges_file");
        tripsFileName = config.getStringValue("default_trips_file");
        eventsFileName = config.getStringValue("default_events_expanded_file");
        String stationDistancesFileName = config.getStringValue("default_vs_station_distances_file");
        vehicleSchedulesFileName = config.getStringValue("default_vehicle_schedule_file");

        timelimit = config.getIntegerValue("vs_timelimit");
        threadLimit = config.getIntegerValue("vs_threads");
        mipGap = config.getDoubleValue("vs_mip_gap");
        outputSolverMessages = config.getLogLevel("console_log_level") == LogLevel.DEBUG;
        writeLpFile = config.getBooleanValue("vs_write_lp_file");

        logger.debug("modelName= " + modelName);

        readStops();
        readEdges();
        distances = VS.calculateShortestPathsFloyd(stops, edges, ptnIsUndirected);

        // dump shortest paths
        PrintStream pstream = new PrintStream(new File(stationDistancesFileName));
        pstream.println("# from-station-id; to-station-id; distance");

        for (int i = 0; i < distances.length; i++) {
            for (int j = 0; j < distances[i].length; j++) {
                pstream.println((i + 1) + ";" + (j + 1) + ";" + distances[i][j]);
            }
        }

        pstream.close();

    }

}

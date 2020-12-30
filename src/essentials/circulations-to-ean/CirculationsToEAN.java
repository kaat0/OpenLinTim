import java.io.*;
import java.util.*;



public class CirculationsToEAN
{
	private CirculationsToEAN() {}  // class only contains static methods



	// settings read from the config file
	private static String periodicEventInputFile;
	private static String circulationFileName;
	private static String activityFileName;
	private static String distanceFileName;
	private static int turnoverTime;
	private static boolean DEBUG;
	private static boolean VERBOSE;



	public static void main(String[] args) throws Exception
	{
		// This method seems to be much more complex than needed.
		// The main problem is that the rolled-out EAN used for
		// vehicle scheduling probably is different from the
		// rolled-out EAN used for delay management (as the time
		// horizon for vehicle scheduling for example is one day,
		// but only two hours for delay management). Hence, the
		// IDs of non-periodic events in both EANs are different,
		// so we use the combination of periodic ID and non-periodic
		// time to identify non-periodic events.

		readConfig();
		NonPeriodicEANetwork Net = IO.readNonPeriodicEANetwork(false, false);
		Hashtable<String, Double> distances = readDistanceFile();

		// for each periodic event, we need a list of the
		// corresponding non-periodic events
		int count = Tools.countRelevantLines(periodicEventInputFile);
		ArrayList<LinkedList<NonPeriodicEvent>> rolledOutEvents =
			new ArrayList<>(count);
		for (int i=0; i<count; i++)
			rolledOutEvents.add(new LinkedList<>());
		for (NonPeriodicEvent e: Net.getEvents())
			rolledOutEvents.get(e.getPeriodicParentEventID()-1).add(e);

		int ID = Tools.countRelevantLines(activityFileName) + 1;
		PrintWriter outputFile =
			new PrintWriter(new BufferedWriter(new FileWriter(activityFileName, true)));
		BufferedReader reader = new BufferedReader(new FileReader(circulationFileName));
		String line;
		while ((line = reader.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			// only consider empty trips
			String[] tokens = line.split(";");
			if (! tokens[3].trim().equals("EMPTY"))
				continue;

			// only consider non-periodic empty trips, i.e., those empty
			// trips for which the non-periodic time of the end event is
			// after the non-periodic time of the start event
			int startTime = Integer.parseInt(tokens[7].trim());
			int endTime = Integer.parseInt(tokens[11].trim());
			if (startTime > endTime)
				continue;

			// find the non-periodic start event
			int pStartEvent = Integer.parseInt(tokens[5].trim());
			NonPeriodicEvent source = null;
 			for (NonPeriodicEvent e: rolledOutEvents.get(pStartEvent-1))
			{
				if (e.getTime() == startTime)
				{
					source = e;
					break;
				}
			}
			if (source == null)
				continue;

			// find the non-periodic end event
			int pEndEvent = Integer.parseInt(tokens[9].trim());
			NonPeriodicEvent target = null;
			for (NonPeriodicEvent e: rolledOutEvents.get(pEndEvent-1))
			{
				if (e.getTime() == endTime)
				{
					target = e;
					break;
				}
			}
			if (target == null)
				continue;

			int fromStation = Integer.parseInt(tokens[6].trim());
			int toStation = Integer.parseInt(tokens[10].trim());
			long d = Math.round(distances.get(fromStation + "-" + toStation));

			if (endTime - startTime < d)
			{
				  System.out.println("Neglecting circulation " + tokens[2].trim() + ", seems to take more than a day!");
				  continue;
			}

			outputFile.println(  ID + "; -1; \"fixed-turnaround\"; " + source.getID()
			                   + "; " + target.getID() + "; " + d + "; " + d + "; 0");
			ID++;
		}
		outputFile.close();
	}



	private static Hashtable<String, Double> readDistanceFile() throws IOException
	{
		int size = Tools.countRelevantLines(distanceFileName);
		Hashtable<String, Double> distances = new Hashtable<>(size);

		BufferedReader reader = new BufferedReader(new FileReader(distanceFileName));
		String line;
		while ((line = reader.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			int fromStation = Integer.parseInt(tokens[0].trim());
			int toStation = Integer.parseInt(tokens[1].trim());
			double d = Double.parseDouble(tokens[2].trim());
			distances.put(fromStation + "-" + toStation, d + turnoverTime);
		}

		return distances;
	}


	private static void readConfig() throws Exception
	{
		Config config = new Config(new File("basis/Config.cnf"));

		periodicEventInputFile = config.getStringValue("default_events_periodic_file");
		circulationFileName = config.getStringValue("default_vehicle_schedule_file");
		activityFileName = config.getStringValue("default_activities_expanded_file");
		distanceFileName = config.getStringValue("default_vs_station_distances_file");
		// convert turnover time from time units to seconds
		turnoverTime = config.getIntegerValue("vs_turn_over_time") * 60 / config.getIntegerValue("time_units_per_minute");
		DEBUG = config.getBooleanValue("DM_debug");
		VERBOSE = config.getBooleanValue("DM_verbose");

		if (DEBUG)
			VERBOSE = true;

		if (VERBOSE)
		{
			System.out.println("CirculationsToEAN: using the following configuration:");
			System.out.println("  periodic event file: " + new File(periodicEventInputFile).getAbsolutePath());
			System.out.println("  circulation file: " + new File(circulationFileName).getAbsolutePath());
			System.out.println("  non-periodic activities file: " + new File(activityFileName).getAbsolutePath());
			System.out.println("  distance file: " + new File(distanceFileName).getAbsolutePath());
		}
	}
}

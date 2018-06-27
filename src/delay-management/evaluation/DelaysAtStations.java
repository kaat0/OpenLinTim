import java.io.*;
import java.text.NumberFormat;



public class DelaysAtStations
{
	private DelaysAtStations() {}  // class only contains static methods



	// parameters from config file
	private static int earliestDelayToPlot;
	private static int latestDelayToPlot;
	private static int animationSteps;
	private static int timeUnitsPerMinute;
	private static int period;
	private static String stopFileName;
	private static String edgeFileName;
	private static String periodicEventFileName;
	private static String eventFileName;
	private static String activityFileName;
	private static String dispoFile;
	private static String stationDelayFile;
	private static String dotOutputFile;
	private static String animationOutputDir;
	private static boolean passengerDelays;
	private static boolean CHECK_CONSISTENCY;
	private static boolean DEBUG;
	private static boolean VERBOSE;

	private static double[] eventWeights;
	private static double[] connectionWeights;
	private static int[] timetable;
	private static int[] dispositionTimetable;
	private static int[] connectionStartEvents;
	private static int[] connectionEndEvents;
	private static int[] connectionDuration;
	private static int[] stationOfEvent;
	private static String[] stationNames;
	private static int[] pID;



	public static void main(String[] args) throws Exception
	{
		if (args.length != 1)
			throw new Exception("expecting 1 parameter: sum-up or generate-dot");

		if (args[0].equalsIgnoreCase("sum-up"))
			outputDelaysAtStations();
		else if (args[0].equalsIgnoreCase("generate-dot"))
			outputDotFile();
		else
			throw new Exception("expecting 1 parameter: sum-up or generate-dot");
	}



	public static void outputDelaysAtStations() throws Exception
	{
		readConfig();
		if (DEBUG && animationSteps == 1)
			System.out.println("DelaysAtStations: writing delays in stations output to " + stationDelayFile);
		else if (DEBUG && animationSteps > 1)
			System.out.println("DelaysAtStations: writing delays in stations output to " + animationOutputDir);

		readNonPeriodicEvents();
		readPeriodicEvents();
		readNonPeriodicActivities();
		readDispoTimetable();

		double[] sumOfDelays = new double[Tools.countRelevantLines(stopFileName)];

		int digits = Integer.toString(animationSteps).length();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(digits);
		nf.setMaximumIntegerDigits(digits);
		nf.setGroupingUsed(false);

		for (int i=1; i<=animationSteps; i++)
		{
			// reset sum
			for (int k=0; k<sumOfDelays.length; k++)
				sumOfDelays[k] = 0;

			// sum up delays for each station
			float step = ((latestDelayToPlot-earliestDelayToPlot)/((float) animationSteps));
			int upperBound = earliestDelayToPlot + Math.round(i * step);

			if (passengerDelays)
			{
				for (int k=0; k<timetable.length; k++)
					if (timetable[k] >= earliestDelayToPlot && timetable[k] <= upperBound)
						sumOfDelays[stationOfEvent[k]-1] += eventWeights[k] * (dispositionTimetable[k] - timetable[k]);

				for (int k=0; k<connectionStartEvents.length; k++)
				{
					if (   timetable[connectionStartEvents[k]-1] >= earliestDelayToPlot
					    && timetable[connectionStartEvents[k]-1] <= upperBound)
					{
					    if (dispositionTimetable[connectionEndEvents[k]-1] - dispositionTimetable[connectionStartEvents[k]-1] < connectionDuration[k])
					    	sumOfDelays[stationOfEvent[connectionStartEvents[k]-1]-1] += period * connectionWeights[k];
					}
				}
			}
			else // passengerDelays == false
			{
				for (int k=0; k<timetable.length; k++)
					if (timetable[k] >= earliestDelayToPlot && timetable[k] <= upperBound)
						sumOfDelays[stationOfEvent[k]-1] += dispositionTimetable[k] - timetable[k];
			}

			// output the sum of all delays at all stations
			String filename;
			if (animationSteps == 1)
				filename = stationDelayFile;
			else
				filename = animationOutputDir + "delayedstops_" + nf.format(i) + ".txt";
			if (DEBUG)
				System.out.println("DelaysAtStations: writing to output file " + filename);
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			out.println("# stop-id; delay");
			for (int k=0; k<sumOfDelays.length; k++)
				out.println((k+1) + "; " + sumOfDelays[k]);
			out.close();
		}
	}



	public static void outputDotFile() throws Exception
	{
		readConfig();

		if (DEBUG && animationSteps == 1)
			System.out.println("DelaysAtStations: writing DOT output to " + dotOutputFile);
		else if (DEBUG && animationSteps > 1)
			System.out.println("DelaysAtStations: writing DOT output to " + animationOutputDir);

		int[][] endStations = readEdgeFile(edgeFileName);
		int[][] coords = readStopFile(stopFileName);

		int digits = Integer.toString(animationSteps).length();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(digits);
		nf.setMaximumIntegerDigits(digits);
		nf.setGroupingUsed(false);

		double maxDelay = 0;
		// start loop with k=animationSteps as we need to calculate
		// the maximal delay
		for (int k=animationSteps; k>=1; k--)
		{
			double[] sumOfDelays;
			String filename;
			if (animationSteps == 1)
			{
				sumOfDelays = readDelaysAtStations(stationDelayFile);
				filename = dotOutputFile;
			}
			else
			{
				String basis = animationOutputDir + "delayedstops_" + nf.format(k);
				sumOfDelays = readDelaysAtStations(basis + ".txt");
				filename = basis + ".dot";
			}

			if (k == animationSteps)
			{
				for (int j=0; j<sumOfDelays.length; j++)
					if (sumOfDelays[j] > maxDelay)
						maxDelay = sumOfDelays[j];
				if (DEBUG)
					System.out.println("DelaysAtStations: largest delay=" + maxDelay);
			}

			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			out.println("graph G");
			out.println("{");
			out.println("  ordering=out;");
			out.println("  size=\"13,10\"");
			for (int i=0; i<sumOfDelays.length; i++)
			{
				long intensity = Math.round(255 * (1 - sumOfDelays[i]/ maxDelay));
				if (0 > intensity || 255 < intensity)
					throw new Exception("UPS... expected 0 <= intensity <= 255 in DelaysAtStations.outputDotFile()");
				String hex = Long.toHexString(intensity).toUpperCase();
				if (hex.length() == 1)
					hex = "0" + hex;
				String colorcode = "#FF" + hex + hex;
				if (DEBUG && sumOfDelays[i]>0)
					System.out.println("delay: " + sumOfDelays[i]+ ", colorcode: " + colorcode);
				out.print("  " + (i+1) + "[label=\"" + stationNames[i] + "\", style=filled, fillcolor=\"" + colorcode + "\"");
				out.print(", pos=\"" + coords[i][0] + "," + coords[i][1] + "\"");
				out.println("];");
			}
			out.println();
			for (int i=0; i<endStations.length; i++)
				out.println("  " + endStations[i][0] + " -- " + endStations[i][1] + " [color=\"#000000\"];");
			out.println("}");
			out.close();
		}
	}



	private static void readConfig() throws Exception
	{
		Config config = new Config(new File("basis/Config.cnf"));

		stopFileName = config.getStringValue("default_stops_file");
		edgeFileName = config.getStringValue("default_edges_file");
		periodicEventFileName = config.getStringValue("default_events_periodic_file");
		eventFileName = config.getStringValue("default_events_expanded_file");
		activityFileName = config.getStringValue("default_activities_expanded_file");
		dispoFile = config.getStringValue("default_disposition_timetable_file");
		stationDelayFile = config.getStringValue("default_delayed_stops_file");
		dotOutputFile = config.getStringValue("default_delay_graph_file");
		animationOutputDir = config.getStringValue("plot_delays_animation_output_dir");
		earliestDelayToPlot = config.getIntegerValue("plot_delays_min_time");
		latestDelayToPlot = config.getIntegerValue("plot_delays_max_time");
		if (config.getBooleanValue("plot_delays_enable_animation"))
			animationSteps = config.getIntegerValue("plot_delays_number_of_steps_in_animation");
		else
			animationSteps = 1;
		passengerDelays = config.getBooleanValue("plot_delays_passengers");
		timeUnitsPerMinute = config.getIntegerValue("time_units_per_minute");
		// attention: timetabling works with minutes, we use seconds!
		period = 60 * config.getIntegerValue("period_length") / timeUnitsPerMinute;
		CHECK_CONSISTENCY = config.getBooleanValue("DM_enable_consistency_checks");
		DEBUG = config.getBooleanValue("DM_debug");
		VERBOSE = config.getBooleanValue("DM_verbose");

		if (animationSteps > 1)
		{
			File directory = new File(animationOutputDir);
			if (! directory.exists())
				directory.mkdir();
			if (directory.isFile())
				throw new IOException("DelaysAtStations: " + animationOutputDir + " already exists and is a file");
			animationOutputDir = directory.getAbsolutePath() + File.separator;
		}

		if (DEBUG)
		{
			CHECK_CONSISTENCY = true;
			VERBOSE = true;
		}

		int earliestTime = config.getIntegerValue("DM_earliest_time");
		int latestTime = config.getIntegerValue("DM_latest_time");

		if (earliestTime > latestTime)
			throw new Exception("DelaysAtStations: DM_latest_time must not be smaller than DM_earliest_time");
		if (earliestDelayToPlot > latestDelayToPlot)
			throw new Exception("DelaysAtStations: plot_delays_max_time must not be smaller than plot_delays_min_time");
		if (earliestDelayToPlot < earliestTime || latestDelayToPlot > latestTime)
			throw new Exception("DelaysAtStations: [plot_delays_min_time,plot_delays_max_time] must be contained in [DM_earliest_time,DM_latest_time]");
		if (animationSteps < 1)
			throw new Exception("DelaysAtStations: plot_delays_number_of_steps_in_animation bust be at least 1");
		if (! dotOutputFile.endsWith(".dot"))
			throw new Exception("DelaysAtStations: default_delay_graph_file should end with .dot");

		if (VERBOSE)
		{
			System.out.println("DelaysAtStations: using the following configuration:");
			System.out.println("  expecting rolled out events in [" + earliestTime  + "," + latestTime + "]");
			System.out.println("  station input file: " + new File(stopFileName).getAbsolutePath());
			System.out.println("  edge input file: " + new File(edgeFileName).getAbsolutePath());
			System.out.println("  periodic events input file: " + new File(periodicEventFileName).getAbsolutePath());
			System.out.println("  non-periodic events input file: " + new File(eventFileName).getAbsolutePath());
			System.out.println("  non-periodic activities input file: " + new File(activityFileName).getAbsolutePath());
			System.out.println("  disposition timetable input file: " + new File(dispoFile).getAbsolutePath());
			System.out.println("  summing up all delays of events in [" + earliestDelayToPlot  + "," + latestDelayToPlot + "] for evaluation");
			if (passengerDelays)
				System.out.println("  plotting delays of passengers");
			else
				System.out.println("  plotting delays of trains");
			System.out.println("  using " + animationSteps + " steps for animation");
			System.out.println("  output file for summed up delays in stations: " + new File(stationDelayFile).getAbsolutePath());
			System.out.println("  DOT output file: " + new File(dotOutputFile).getAbsolutePath());
			System.out.println("  animation output directory: " + new File(animationOutputDir).getAbsolutePath());
		}
	}



	private static void readNonPeriodicEvents() throws IOException
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading non-periodic events from " + new File(eventFileName).getAbsolutePath());

		if (CHECK_CONSISTENCY && ! Tools.checkIDs(eventFileName))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + eventFileName);

		int count = Tools.countRelevantLines(eventFileName);
		pID = new int[count];
		timetable = new int[count];
		dispositionTimetable = new int[count];
		eventWeights = new double[count];
		stationOfEvent = new int[count];

		BufferedReader in = new BufferedReader(new FileReader(eventFileName));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			pID[i] = Integer.parseInt(tokens[1].trim());
			timetable[i] = Integer.parseInt(tokens[3].trim());
			eventWeights[i] = Double.parseDouble(tokens[4].trim());
			i++;
		}
	}



	private static void readPeriodicEvents() throws IOException
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading periodic events from " + new File(periodicEventFileName).getAbsolutePath());

		if (CHECK_CONSISTENCY && ! Tools.checkIDs(periodicEventFileName))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + periodicEventFileName);

		int[] stopID = new int[Tools.countRelevantLines(periodicEventFileName)];

		BufferedReader in = new BufferedReader(new FileReader(periodicEventFileName));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			stopID[i] = Integer.parseInt(tokens[2].trim());
			i++;
		}

		// create mapping eventID --> stopID
		for (i=0; i<stationOfEvent.length; i++)
			stationOfEvent[i] = stopID[pID[i]-1];
	}



	private static void readNonPeriodicActivities() throws IOException
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading non-periodic activities from " + new File(activityFileName).getAbsolutePath());

		if (CHECK_CONSISTENCY && ! Tools.checkIDs(activityFileName))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + activityFileName);

		int count = Tools.countRelevantLines(activityFileName);
		connectionStartEvents = new int[count];
		connectionEndEvents = new int[count];
		connectionDuration = new int[count];
		connectionWeights = new double[count];

		BufferedReader in = new BufferedReader(new FileReader(activityFileName));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			connectionStartEvents[i] = Integer.parseInt(tokens[3].trim());
			connectionEndEvents[i] = Integer.parseInt(tokens[4].trim());
			connectionDuration[i] = Integer.parseInt(tokens[5].trim());
			connectionWeights[i] = Double.parseDouble(tokens[6].trim());
			i++;
		}
	}



	private static void readDispoTimetable() throws IOException
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading disposition timetable from " + new File(dispoFile).getAbsolutePath());

		if (CHECK_CONSISTENCY && ! Tools.checkIDs(dispoFile))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + dispoFile);

		BufferedReader in = new BufferedReader(new FileReader(dispoFile));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			dispositionTimetable[i] = Integer.parseInt(tokens[1].trim());
			i++;
		}
	}



	private static int[][] readStopFile(String filename) throws Exception
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading stations from " + new File(filename).getAbsolutePath());
		if (CHECK_CONSISTENCY && ! Tools.checkIDs(filename))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + filename);

		int count = Tools.countRelevantLines(filename);
		stationNames = new String[count];
		int[][] coords = new int[count][2];
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			if (tokens.length < 5) // no coordinates given
				throw new Exception("no coordinates given for station " + (i+1));

			stationNames[i] = tokens[1].trim();
			coords[i][0] = Integer.parseInt(tokens[3].trim());
			coords[i][1] = Integer.parseInt(tokens[4].trim());
			i++;
		}
		return coords;
	}



	private static int[][] readEdgeFile(String filename) throws IOException
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading edges in PTN from " + new File(filename).getAbsolutePath());
		if (CHECK_CONSISTENCY && ! Tools.checkIDs(filename))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + filename);

		int[][] endStations = new int[Tools.countRelevantLines(filename)][2];
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			endStations[i][0] = Integer.parseInt(tokens[1].trim());
			endStations[i][1] = Integer.parseInt(tokens[2].trim());
			i++;
		}
		return endStations;
	}



	private static double[] readDelaysAtStations(String filename) throws IOException
	{
		if (DEBUG)
			System.out.println("DelaysAtStations: reading delays from " + new File(filename).getAbsolutePath());
		if (CHECK_CONSISTENCY && ! Tools.checkIDs(filename))
			throw new IOException("DelaysAtStations: invalid numbering of IDs or empty input file: " + filename);

		double[] delays = new double[Tools.countRelevantLines(filename)];
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line;
		int i=0;
		while ((line = in.readLine()) != null)
		{
			int position = line.indexOf('#');
			if (position != -1)
				line = line.substring(0, position);
			line = line.trim();
			if (line.isEmpty())
				continue;

			String[] tokens = line.split(";");
			delays[i] = Double.parseDouble(tokens[1].trim());
			i++;
		}
		return delays;
	}
}

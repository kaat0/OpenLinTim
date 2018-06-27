import java.io.*;


public class SolveDM
{
	private SolveDM() {}  // class only contains static methods



	// settings from config file
	private static String method;
	private static int percentage;
	private static String dispoFile;
	private static String dispoFileHeader;
	private static boolean DEBUG;
	private static boolean VERBOSE;
	private static Integer maxwait;
	private static Boolean swapHeadways;
	private static String opt_method;
	private static boolean writeBestOfAllObjectives;
	private static String bestOfAllObjectivesFile;


	public static void main(String[] args) throws Exception
	{
		readConfig();

		NonPeriodicEANetwork Net = IO.readNonPeriodicEANetwork(true, false);

		if(method.equals("DM2")){
			DM.solveDM2(Net, false);
		}
		else if(method.equals("propagate")){
			Propagator.propagate(Net, (maxwait == null ? Integer.MAX_VALUE : maxwait), (swapHeadways == null ? false : swapHeadways));
		}
		else if(method.equals("DM2-pre")){
			DM.solveDM2(Net, true);
		}
		else if(method.equals("FSFS")){
			DM.solveFSFS(Net);
		}
		else if(method.equals("FRFS")){
			DM.solveFRFS(Net);
		}
		else if(method.equals("EARLYFIX")){
			DM.solveEARLYFIX(Net);
		}
		else if(method.equals("PRIORITY")){
			DM.solvePRIORITY(Net, percentage);
		}
		else if(method.equals("PRIOREPAIR")){
			DM.solvePRIOREPAIR(Net, percentage);
		}
		else if(method.equals("DM1")){
			DM.solveDM1(Net);
		}
		else if(method.equals("PASSENGERPRIOFIX")){
			DM.solvePASSENGERPRIOFIX(Net, percentage);
		}
		else if(method.equals("PASSENGERFIX")){
			DM.solvePASSENGERFIX(Net);
		}
		else if(method.equals("FIXFSFS")){
			DM.solveFIXFSFS(Net);
		}
		else if(method.equals("FIXFRFS")){
			DM.solveFIXFRFS(Net);
		}
		else if(method.equals("best-of-all"))
		{
			// Note that FSFS is always at least as good as PRIORITY and
			// that FRFS is always as good as EARLYFIX, see Diss. Schachtebeck,
			// Lemma 4.5 and Lemma 4.6; hence, we do not have to run
			// EARLYFIX and PRIORITY.

			//For evaluation of every step
			Statistic bestOfAllObjectives = new Statistic();


			int best = 1;
			if (VERBOSE)
				System.out.println("\nrunning FSFS\n");
			double minimum = DM.solveFSFS(Net);
			bestOfAllObjectives.setDoubleValue("dm_FSFS_objective", minimum);


			if (VERBOSE)
				System.out.println("\nrunning FRFS\n");
			double FRFS = DM.solveFRFS(Net);
			bestOfAllObjectives.setDoubleValue("dm_FRFS_objective", FRFS);

			if (FRFS <= minimum)
			{
				minimum = FRFS;
				best = 2;
			}

			// use PRIOREPAIR heuristics with 11 different values
			// 0, 10, 20, ..., 100 for the importance factor
			for (int k=3; k<=13; k++)
			{
				int priority=(k-3)*10;
				if (VERBOSE)
					System.out.println("\nrunning PRIOREPAIR-" + priority + "\n");
				double PRIORITY = DM.solvePRIOREPAIR(Net, priority);
				bestOfAllObjectives.setDoubleValue("dm_PRIOREPAIR-"+priority+"_objective", PRIORITY);

				if (PRIORITY <= minimum)
				{
					minimum = PRIORITY;
					best = k;
				}
			}



			// depending on which heuristic was the best one,
			// we apply it again and use the solution as final solution

			switch (best)
			{
				case 13:
					// last solution is best solution
					// DM.solvePRIOREPAIR(Net, 100);
					break;
				case 12:
					DM.solvePRIOREPAIR(Net, 90);
					break;
				case 11:
					DM.solvePRIOREPAIR(Net, 80);
					break;
				case 10:
					DM.solvePRIOREPAIR(Net, 70);
					break;
				case 9:
					DM.solvePRIOREPAIR(Net, 60);
					break;
				case 8:
					DM.solvePRIOREPAIR(Net, 50);
					break;
				case 7:
					DM.solvePRIOREPAIR(Net, 40);
					break;
				case 6:
					DM.solvePRIOREPAIR(Net, 30);
					break;
				case 5:
					DM.solvePRIOREPAIR(Net, 20);
					break;
				case 4:
					DM.solvePRIOREPAIR(Net, 10);
					break;
				case 3:
					DM.solvePRIOREPAIR(Net, 0);
					break;
				case 2:
					DM.solveFRFS(Net);
					break;
				case 1:
					DM.solveFSFS(Net);
					break;
				default:
					throw new Exception("unexpected value for best");
			}
			String[] methods = {"FSFS", "FRFS", "PRIOREPAIR-0",
          "PRIOREPAIR-10", "PRIOREPAIR-20",
          "PRIOREPAIR-30", "PRIOREPAIR-40",
          "PRIOREPAIR-50", "PRIOREPAIR-60",
          "PRIOREPAIR-70", "PRIOREPAIR-80",
          "PRIOREPAIR-90", "PRIOREPAIR-100"};
			if (VERBOSE)
			{
				System.out.println("\nbest method is " + methods[best-1] + "\n");
				System.out.println("Used optimization method: "+opt_method);
			}
			bestOfAllObjectives.setStringValue("dm_best_method", methods[best - 1]);
			if (writeBestOfAllObjectives) {
				bestOfAllObjectives.writeStatistic(new File(bestOfAllObjectivesFile));
			}
		}
		else
			throw new Exception("SolveDM: unknown method: " + method);

		if (VERBOSE)
			System.out.println("SolveDM: writing disposition timetable to file " + dispoFile);
		outputDispoTimetable(Net);
	}



	private static void readConfig() throws Exception
	{
		Config config = new Config(new File("basis/Config.cnf"));

		method = config.getStringValue("DM_method");
		dispoFile = config.getStringValue("default_disposition_timetable_file");
		dispoFileHeader = config.getStringValue("timetable_header_disposition");
		percentage = config.getIntegerValue("DM_method_prio_percentage");
		DEBUG = config.getBooleanValue("DM_debug");
		VERBOSE = config.getBooleanValue("DM_verbose");
		maxwait = config.getIntegerValue("DM_propagate_maxwait");
		swapHeadways = config.getBooleanValue("DM_propagate_swapHeadways");
		opt_method = config.getStringValue("DM_opt_method_for_heuristic");
		writeBestOfAllObjectives = config.getBooleanValue("DM_best_of_all_write_objectives");
		if (writeBestOfAllObjectives) {
			bestOfAllObjectivesFile = config.getStringValue("filename_dm_best_of_all_objectives");
		}
		else {
			bestOfAllObjectivesFile = "";
		}

		if (percentage < 0)
			throw new Exception("SolveDM: DM_method_prio_percentage must not be negative");

		if (DEBUG)
			VERBOSE = true;

		if (VERBOSE)
		{
			System.out.println("SolveDM: using the following configuration:");
			System.out.println("  delay management method: " + method);
			System.out.println("  optimization method: "+opt_method);
			if (method.equals("PRIORITY"))
				System.out.println("  using percentage of " + percentage + " for PRIORITY heuristic");
			if (method.equals("PRIOREPAIR"))
				System.out.println("  using percentage of " + percentage + " for PRIOREPAIR heuristic");
			System.out.println("  disposition timetable output file: " + new File(dispoFile).getAbsolutePath());
		}
	}



	private static void outputDispoTimetable(NonPeriodicEANetwork Net) throws IOException
	{
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dispoFile)));
		out.println("#" + dispoFileHeader);
		for (NonPeriodicEvent e: Net.getEvents())
			out.println(e.getID() + "; " + e.getDispoTime());
		out.close();
	}
}

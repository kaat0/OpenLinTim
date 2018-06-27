package net.lintim.main.timetabling.periodic;

import net.lintim.algorithm.timetabling.periodic.PespSolver;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.exception.InputFileException;
import net.lintim.io.*;
import net.lintim.model.Graph;
import net.lintim.model.PeriodicActivity;
import net.lintim.model.PeriodicEvent;
import net.lintim.model.PeriodicTimetable;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.logging.Logger;

/**
 * Main class for computing a solution for the periodic timetabling problem using a PESP IP solver. This will use a
 * very straight forward ip formulation that is not very efficient!
 */
public class PespIp {

	/**
	 * Start the computation of the periodic timetabling solution.
	 * @param args one argument, the name of the config file to read
	 */
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException,
			InstantiationException {
		Logger logger = Logger.getLogger(PespIp.class.getCanonicalName());
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if(args.length != 1){
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		try {
			new StatisticReader.Builder().build().read();
		}
		catch (InputFileException exc){
			logger.log(LogLevel.DEBUG, "Could not read statistic file, maybe it does not exist");
		}
		double timeUnitsPerMinute = Config.getDoubleValueStatic("time_units_per_minute");
		int periodLength = Config.getIntegerValueStatic("period_length");
		SolverType solverType = Config.getSolverTypeStatic("tim_solver");
		int timeLimit = Config.getIntegerValueStatic("tim_pesp_ip_timelimit");
		double mipGap = Config.getDoubleValueStatic("tim_pesp_ip_gap");
		double eanChangePenalty = Config.getDoubleValueStatic("ean_change_penalty");
		logger.log(LogLevel.INFO, "Finished reading configuration");

		logger.log(LogLevel.INFO, "Begin reading input data");
		Graph<PeriodicEvent, PeriodicActivity> ean = new PeriodicEANReader.Builder().build().read().getFirstElement();
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin execution of the periodic timetabling pesp ip solver");
		PeriodicTimetable<PeriodicEvent> timetable = new PeriodicTimetable<>(timeUnitsPerMinute, periodLength);
		PespSolver solver = PespSolver.getSolver(solverType);
		boolean optimalSolutionFound = solver.solveTimetablingPespModel(ean, timetable, eanChangePenalty, timeLimit,
				mipGap);

		logger.log(LogLevel.INFO, "Finished execution of the periodic timetabling pesp ip solverl");

		if (!optimalSolutionFound) {
			throw new AlgorithmStoppingCriterionException("pesp ip periodic timetabling");
		}

		logger.log(LogLevel.INFO, "Begin writing output data");
		new PeriodicEANWriter.Builder(ean).writeEvents(false).writeActivities(false).build().write();
		new StatisticWriter.Builder().build().write();
		logger.log(LogLevel.INFO, "Finished writing output data");
	}
}

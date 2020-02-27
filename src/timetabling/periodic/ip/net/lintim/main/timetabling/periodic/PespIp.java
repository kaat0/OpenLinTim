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
import net.lintim.util.Logger;
import net.lintim.util.SolverType;

/**
 * Main class for computing a solution for the periodic timetabling problem using a PESP IP solver. This will use a
 * very straight forward ip formulation that is not very efficient!
 */
public class PespIp {
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException,
			InstantiationException {
		Logger logger = new Logger(PespIp.class.getCanonicalName());
		logger.info("Begin reading configuration");
		if(args.length != 1){
			throw new ConfigNoFileNameGivenException();
		}
		String configFileName = args[0];
		new ConfigReader.Builder(configFileName).build().read();
		try {
			new StatisticReader.Builder().build().read();
		}
		catch (InputFileException exc){
			logger.debug("Could not read statistic file, maybe it does not exist");
		}
		double timeUnitsPerMinute = Config.getDoubleValueStatic("time_units_per_minute");
		int periodLength = Config.getIntegerValueStatic("period_length");
		SolverType solverType = Config.getSolverTypeStatic("tim_solver");
		int timeLimit = Config.getIntegerValueStatic("tim_pesp_ip_timelimit");
		double mipGap = Config.getDoubleValueStatic("tim_pesp_ip_gap");
		double eanChangePenalty = Config.getDoubleValueStatic("ean_change_penalty");
		logger.info("Finished reading configuration");

		logger.info("Begin reading input data");
		Graph<PeriodicEvent, PeriodicActivity> ean = new PeriodicEANReader.Builder().build().read().getFirstElement();
		logger.info("Finished reading input data");
		logger.info("Begin execution of the periodic timetabling pesp ip solver");
		PeriodicTimetable<PeriodicEvent> timetable = new PeriodicTimetable<>(timeUnitsPerMinute, periodLength);
		PespSolver solver = PespSolver.getSolver(solverType);
		boolean optimalSolutionFound = solver.solveTimetablingPespModel(ean, timetable, eanChangePenalty, timeLimit,
				mipGap);

		logger.info("Finished execution of the periodic timetabling pesp ip solverl");

		if (!optimalSolutionFound) {
			throw new AlgorithmStoppingCriterionException("pesp ip periodic timetabling");
		}

		logger.info("Begin writing output data");
		new PeriodicEANWriter.Builder(ean).writeEvents(false).writeActivities(false).build().write();
		new StatisticWriter.Builder().build().write();
		logger.info("Finished writing output data");
	}
}

package net.lintim.main.evaluation;

import net.lintim.evaluation.VehicleScheduleEvaluator;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.exception.InputFileException;
import net.lintim.io.*;
import net.lintim.model.*;
import net.lintim.util.LogLevel;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Main class for evaluating an aperiodic vehicle schedule
 */
public class VehicleScheduleMain {
	public static void main(String[] args) {
		Logger logger = Logger.getLogger(VehicleScheduleMain.class.getCanonicalName());
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if(args.length < 1){
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		logger.log(LogLevel.INFO, "Finished reading configuration");
		logger.log(LogLevel.INFO, "Begin reading input data");
		Graph<Stop, Link> ptn = new PTNReader.Builder().build().read();
		LinePool lineConcept = new LineReader.Builder(ptn).readFrequencies(true).readCosts(false).build().read();
		Collection<Trip> trips = new TripReader.Builder().build().read();
		VehicleSchedule vehicleSchedule = new VehicleScheduleReader.Builder().build().read();
		try{
			new StatisticReader.Builder().build().read();
		}
		catch (InputFileException e){
			//When there is no statistic file, ignore it. We will write a new one
		}
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin vehicle schedule evaluation");
		VehicleScheduleEvaluator.evaluateVehicleSchedule(vehicleSchedule, trips, ptn, lineConcept);
		logger.log(LogLevel.INFO, "Finished vehicle schedule evaluation");
		logger.log(LogLevel.INFO, "Writing output data");
		new StatisticWriter.Builder().build().write();
		logger.log(LogLevel.INFO, "Finished writing output data");

	}
}

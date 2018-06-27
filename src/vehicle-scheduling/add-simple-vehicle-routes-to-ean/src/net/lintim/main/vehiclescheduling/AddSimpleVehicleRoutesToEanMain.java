package net.lintim.main.vehiclescheduling;

import net.lintim.algorithm.vehiclescheduling.AddToEan;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.*;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;

import java.util.logging.Logger;

/**
 * Main class for adding simple vehicle routes to a periodic ean. Will connect every repetition of a line with the
 * corresponding reverse direction
 */
public class AddSimpleVehicleRoutesToEanMain {
	/**
	 * Start the computation of the simple vehicle routes and add them to the existing ean.
	 * @param args One argument needed, the path to the LinTim config file to read
	 */
	public static void main(String[] args) {
		Logger logger = Logger.getLogger("net.lintim.main.vehiclescheduling");
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if(args.length < 1){
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		int turnOverTime = Config.getIntegerValueStatic("vs_turn_over_time");
		int periodLength = Config.getIntegerValueStatic("period_length");
		String eanConstructionTargetModelFrequency = Config.getStringValueStatic
				("ean_construction_target_model_frequency");
		boolean eanContainsFrequencies = eanConstructionTargetModelFrequency.equals("FREQUENCY_AS_MULTIPLICITY");
		int vsMaximumBufferTime = Config.getIntegerValueStatic("vs_maximum_buffer_time");
		logger.log(LogLevel.INFO, "Finished reading configuration");
		logger.log(LogLevel.INFO, "Begin reading input data");
		Graph<Stop, Link> ptn = new PTNReader.Builder().build().read();
		LinePool lineConcept = new LineReader.Builder(ptn).readFrequencies(true).readCosts(false).build().read();
		Graph<PeriodicEvent, PeriodicActivity> ean = new PeriodicEANReader.Builder().build().read().getFirstElement();
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin vehicle schedule computation");
		AddToEan.addSimpleVehicleSchedulesToEan(ean, lineConcept, turnOverTime, eanContainsFrequencies, periodLength,
				vsMaximumBufferTime);
		logger.log(LogLevel.INFO, "Finished vehicle schedule computation");
		logger.log(LogLevel.INFO, "Writing output data");
		new PeriodicEANWriter.Builder(ean).writeTimetable(false).build().write();
		logger.log(LogLevel.INFO, "Finished writing output data");
	}
}

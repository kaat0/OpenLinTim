package net.lintim.main.vehiclescheduling;

import net.lintim.algorithms.vehiclescheduling.SimpleVehicleSchedule;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.ConfigReader;
import net.lintim.io.TripReader;
import net.lintim.io.VehicleScheduleWriter;
import net.lintim.model.Trip;
import net.lintim.model.VehicleSchedule;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Create a simple vehicle schedule, i.e., a vehicle schedule containing only circulations serving one line. A
 * vehicle will serve the next other direction of the same line after finishing a trip.
 */
public class SimpleVehicleScheduleMain {

	public static void main(String[] args) {
		Logger logger = Logger.getLogger("net.lintim.main.vehiclescheduling");
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if(args.length < 1){
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		int turnOverTime = Config.getIntegerValueStatic("vs_turn_over_time");
		logger.log(LogLevel.INFO, "Finished reading configuration");
		logger.log(LogLevel.INFO, "Begin reading input data");
		Collection<Trip> trips = new TripReader.Builder().build().read();
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin simple vehicle schedule computation");
		VehicleSchedule vehicleSchedule = SimpleVehicleSchedule.createSimpleVehicleSchedule(trips, turnOverTime);
		logger.log(LogLevel.INFO, "Finished simple vehicle schedule computation");
		logger.log(LogLevel.INFO, "Writing output data");
		new VehicleScheduleWriter.Builder(vehicleSchedule).build().write();
		logger.log(LogLevel.INFO, "Finished writing output data");
	}
}

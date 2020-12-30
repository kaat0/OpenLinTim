package net.lintim.main.vehiclescheduling;

import net.lintim.algorithm.vehiclescheduling.IPModelSolver;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.ConfigReader;
import net.lintim.io.PTNReader;
import net.lintim.io.TripReader;
import net.lintim.io.VehicleScheduleWriter;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.Logger;
import net.lintim.util.vehiclescheduling.Parameters;

import java.io.IOException;
import java.util.Collection;


/**
 * Main class for computing a vehicle schedule using an IP solver
 */
public class IPModelMain {
	private static final Logger logger = new Logger(IPModelMain.class.getCanonicalName());

	public static void main(String[] args) throws IllegalAccessException, InstantiationException,
			ClassNotFoundException {
		logger.info("Begin reading configuration");
		if (args.length < 1) {
			throw new ConfigNoFileNameGivenException();
		}
		Config config = new ConfigReader.Builder(args[0]).build().read();
        Parameters parameters = new Parameters(config);
		logger.info("Finished reading configuration");
		logger.info("Begin reading input data");
		Graph<Stop, Link> ptn = new PTNReader.Builder().build().read();
		Collection<Trip> trips = new TripReader.Builder().build().read();
		logger.info("Finished reading input data");
		logger.info("Begin ip vehicle schedule computation");
		IPModelSolver solver = IPModelSolver.getVehicleSchedulingIpSolver(parameters.getSolverType());
		VehicleSchedule vehicleSchedule = solver.solveVehicleSchedulingIPModel(ptn, trips, parameters);
		logger.info("Finished ip vehicle schedule computation");
		if(vehicleSchedule != null) {
			logger.info("Writing output data");
			new VehicleScheduleWriter.Builder(vehicleSchedule).build().write();
			logger.info("Finished writing output data");
		}
		else {
			logger.info("Finish program without writing output");
		}
	}
}

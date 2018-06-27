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
import net.lintim.util.SolverType;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;


/**
 * Main class for computing a vehicle schedule using an IP solver
 */
public class IPModelMain {
	private static Logger logger = Logger.getLogger("net.lintim.main.vehiclescheduling.IPModelMain");

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException,
			ClassNotFoundException {
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if (args.length < 1) {
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		SolverType solverType = Config.getSolverTypeStatic("vs_solver");
		logger.log(LogLevel.INFO, "Finished reading configuration");
		logger.log(LogLevel.INFO, "Begin reading input data");
		Graph<Stop, Link> ptn = new PTNReader.Builder().build().read();
		Collection<Trip> trips = new TripReader.Builder().build().read();
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin ip vehicle schedule computation");
		IPModelSolver solver = IPModelSolver.getVehicleSchedulingIpSolver(solverType);
		VehicleSchedule vehicleSchedule = solver.solveVehicleSchedulingIPModel(ptn, trips, Config.getDefaultConfig());
		logger.log(LogLevel.INFO, "Finished ip vehicle schedule computation");
		if(vehicleSchedule != null) {
			logger.log(LogLevel.INFO, "Writing output data");
			new VehicleScheduleWriter.Builder(vehicleSchedule).build().write();
			logger.log(LogLevel.INFO, "Finished writing output data");
		}
		else {
			logger.log(LogLevel.WARN, "Finish program without writing output");
		}
	}
}

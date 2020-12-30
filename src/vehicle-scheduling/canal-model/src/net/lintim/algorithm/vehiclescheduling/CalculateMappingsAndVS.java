package net.lintim.algorithm.vehiclescheduling;

import net.lintim.io.ConfigReader;
import net.lintim.io.vehiclescheduling.IO;
import net.lintim.main.vehiclescheduling.VS;
import net.lintim.model.vehiclescheduling.*;
import net.lintim.util.Config;

import java.util.ArrayList;
import java.util.HashMap;

public class CalculateMappingsAndVS {

	public static void main(String[] args) throws Exception {

		Config config = new ConfigReader.Builder(args[0]).build().read();

		IO.initialize(config);

		if (config.getStringValue("vs_model").toUpperCase()
				.equals("CANAL_MODEL")) {
			CanalNetwork network = IO.calculateCanalNetwork();
			ArrayList<CTransfer> occuringTransfers = IO
					.readVehicleFlowTransfers();

			ArrayList<Integer> occuringCanalEvents = IO
					.readVehicleFlowCanals();

			Canal[] newCanals = VS.calculateCanalsWithOccuringEvents(network,
					occuringCanalEvents);

			HashMap<Canal, HashMap<CEvent, CEvent>> mappings = VS.calculateCanalMappings(newCanals);

			IO.calculateVSFileCanal(newCanals, network.getTrips(),
					occuringTransfers, mappings);
		} else {

			ArrayList<Trip> trips;
			ArrayList<Transfer> transfers;

			trips = IO.readTrips();
			transfers = IO.readTransfers();

			IO.calculateVSFile(trips, transfers);
		}
	}
}

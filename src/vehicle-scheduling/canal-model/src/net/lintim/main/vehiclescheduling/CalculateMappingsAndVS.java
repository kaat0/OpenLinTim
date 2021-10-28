package net.lintim.main.vehiclescheduling;

import net.lintim.algorithm.vehiclescheduling.VS;
import net.lintim.io.ConfigReader;
import net.lintim.io.vehiclescheduling.IO;
import net.lintim.model.vehiclescheduling.*;
import net.lintim.util.Config;
import net.lintim.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class CalculateMappingsAndVS {

    private static final Logger logger = new Logger(CalculateMappingsAndVS.class);

    public static void main(String[] args) throws Exception {

        logger.info("Begin rereading configuration and input data after calling mosel");

        Config config = new ConfigReader.Builder(args[0]).build().read();

        IO.initialize(config);

        logger.info("Finished reading configuration and input data");

        logger.info("Begin calculating solution from mosel output");

        if (config.getStringValue("vs_model")
            .equalsIgnoreCase("CANAL_MODEL")) {
            CanalNetwork network = IO.calculateCanalNetwork();
            ArrayList<CTransfer> occuringTransfers = IO
                .readVehicleFlowTransfers();

            ArrayList<Integer> occuringCanalEvents = IO
                .readVehicleFlowCanals();

            Canal[] newCanals = VS.calculateCanalsWithOccuringEvents(network,
                occuringCanalEvents);

            HashMap<Canal, HashMap<CEvent, CEvent>> mappings = VS.calculateCanalMappings(newCanals);

            logger.info("Finished computation of vehicle schedule");

            logger.info("Begin writing output data");

            IO.calculateVSFileCanal(newCanals, network.getTrips(),
                occuringTransfers, mappings);

            logger.info("Finished writing output data");
        } else {

            ArrayList<Trip> trips;
            ArrayList<Transfer> transfers;

            trips = IO.readTrips();
            transfers = IO.readTransfers();

            logger.info("Finished computation of vehicle schedule");

            logger.info("Begin writing output data");

            IO.calculateVSFile(trips, transfers);

            logger.info("Finished writing output data");
        }
    }
}

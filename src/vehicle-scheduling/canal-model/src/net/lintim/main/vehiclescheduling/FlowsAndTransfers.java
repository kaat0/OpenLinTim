package net.lintim.main.vehiclescheduling;

import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.ConfigReader;
import net.lintim.io.vehiclescheduling.IO;
import net.lintim.util.Config;
import net.lintim.util.Logger;

public class FlowsAndTransfers {

    private static Logger logger = new Logger(FlowsAndTransfers.class);

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            throw new ConfigNoFileNameGivenException();
        }
        logger.info("Begin reading configuration and input data");

        Config config = new ConfigReader.Builder(args[0]).build().read();
        IO.initialize(config);
        logger.info("Finished reading configuration and input data");
        logger.info("Begin preparing mosel input for vehicle scheduling computation");
        IO.calculateMoselInput();
        logger.info("Finished preparing mosel input, calling mosel");


    }
}

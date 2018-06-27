package net.lintim.main.tools;

import net.lintim.algorithm.tools.PTNLoadGenerator;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.*;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;

import java.io.IOException;
import java.util.logging.Logger;

/**
 */
public class PTNLoadGeneratorMain {
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger("net.lintim.main.tools");
        logger.log(LogLevel.INFO, "Begin reading configuration");
        if (args.length != 1) {
            throw new ConfigNoFileNameGivenException();
        }
        new ConfigReader.Builder(args[0]).build().read();
        boolean iterateWithCg = Config.getBooleanValueStatic("load_generator_use_cg");
        logger.log(LogLevel.INFO, "Finished reading configuration");
        logger.log(LogLevel.INFO, "Begin reading input data");
        Graph<Stop, Link> ptn = new PTNReader.Builder().build().read();
        OD od = new ODReader.Builder(ptn.getNodes().size()).build().read();
        LinePool linePool;
        if(iterateWithCg) {
            linePool = new LineReader.Builder(ptn).build().read();
        }
        else {
            linePool = null;
        }
        PTNLoadGenerator loadGenerator = new PTNLoadGenerator.Builder(ptn, od, linePool, Config.getDefaultConfig())
            .build();
        loadGenerator.computeLoad();
        logger.log(LogLevel.INFO, "Finished computing load distribution");
        //Output
        logger.log(LogLevel.INFO, "Begin writing output data");
        new PTNWriter.Builder(ptn).writeStops(false).writeLinks(false).writeLoads(true).build().write();
        logger.log(LogLevel.INFO, "Finished writing output data");
    }

    public enum PTNLoadGeneratorType {
        SHORTEST_PATH, REWARD, REDUCTION, ITERATIVE
    }
}

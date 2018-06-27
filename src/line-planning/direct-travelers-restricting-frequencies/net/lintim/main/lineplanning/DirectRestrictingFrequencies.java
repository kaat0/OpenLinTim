package net.lintim.main.lineplanning;

import net.lintim.algorithm.lineplanning.DirectRestrictingFrequenciesGurobi;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.*;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;

import java.util.logging.Logger;

/**
 * Implementation of initialization code for the direct traveller model in line planning. Will use
 * {@link DirectRestrictingFrequenciesGurobi#solveLinePlanningDirect(Graph, OD, LinePool, Config)} for
 * the actual computation. This class is only the entry point and responsible for all the setup, e.g. reading and
 * writing.
 */
public class DirectRestrictingFrequencies {
	/**
	 * Start the computation of a solution to the direct travellers model for line planning. The only command line
	 * argument should be the name of the config file to read
	 * @param args the command line arguments. Will only works if this only contains one element, the path to the
	 *                config file to read
	 */
	public static void main(String[] args){
		Logger logger = Logger.getLogger("net.lintim.main.lineplanning");
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if (args.length != 1) {
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		logger.log(LogLevel.INFO, "Finished reading configuration");
		logger.log(LogLevel.INFO, "Begin reading input data");
        Graph<Stop, Link> ptn = new PTNReader.Builder().readLoads(true).build().read();
        LinePool linePool = new LineReader.Builder(ptn).build().read();
		OD od = new ODReader.Builder(ptn.getNodes().size()).build().read();
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin execution of line planning direct model restricting frequencies");
		boolean feasibleSolutionFound = DirectRestrictingFrequenciesGurobi.solveLinePlanningDirect(ptn, od, linePool,
				Config.getDefaultConfig());
		logger.log(LogLevel.INFO, "Finished execution of line planning direct model restricting frequencies");
		if (!feasibleSolutionFound) {
			throw new AlgorithmStoppingCriterionException("direct model line planning restricting frequencies");
		}
		logger.log(LogLevel.INFO, "Begin writing output data");
		new LineWriter.Builder(linePool).build().write();
		logger.log(LogLevel.INFO, "Finished writing output data");
	}
}

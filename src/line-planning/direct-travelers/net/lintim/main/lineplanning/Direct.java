package net.lintim.main.lineplanning;

import net.lintim.algorithm.lineplanning.DirectGurobi;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.*;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.DirectSolutionDescriptor;
import net.lintim.util.LinePlanningHelper;
import net.lintim.util.LogLevel;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of initialization code for the direct traveller model in line planning. Will use
 * {@link net.lintim.algorithm.lineplanning.DirectGurobi#solveLinePlanningDirect(Graph, OD, LinePool, Config, int)} for
 * the actual computation. This class is only the entry point and responsible for all the setup, e.g. reading and
 * writing.
 */
public class Direct {
	private static Logger logger = Logger.getLogger("net.lintim.main.lineplanning");

	/**
	 * Start the computation of a solution to the direct travellers model for line planning. The only command line
	 * argument should be the name of the config file to read
	 * @param args the command line arguments. Will only works if this only contains one element, the path to the
	 *                config file to read
	 */
	public static void main(String[] args){
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if (args.length != 1) {
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		int commonFrequencyDivisor = Config.getIntegerValueStatic("lc_common_frequency_divisor");
		int periodLength = Config.getIntegerValueStatic("period_length");
		logger.log(LogLevel.INFO, "Finished reading configuration");
		logger.log(LogLevel.INFO, "Begin reading input data");

		Graph<Stop, Link> ptn = new PTNReader.Builder().readLoads(true).build().read();
		LinePool linePool = new LineReader.Builder(ptn).build().read();
		OD od = new ODReader.Builder(ptn.getNodes().size()).build().read();
		logger.log(LogLevel.INFO, "Finished reading input data");
		logger.log(LogLevel.INFO, "Begin execution of line planning direct model");
		// Solve the problem for all possible system frequencies and determine the best
		Set<Integer> possibleSystemFrequencies = LinePlanningHelper.determinePossibleSystemFrequencies
				(commonFrequencyDivisor, periodLength);
		boolean solutionFound = false;
		double bestObjective = Double.NEGATIVE_INFINITY;
		HashMap<Integer, Integer> bestFrequencies = null;
		for(int commonFrequency : possibleSystemFrequencies) {
			DirectSolutionDescriptor solutionWrapper = DirectGurobi.solveLinePlanningDirect(ptn, od, linePool,
                Config.getDefaultConfig(), commonFrequency);
			if(solutionWrapper.isFeasible()) {
				solutionFound = true;
				if(solutionWrapper.getObjectiveValue() > bestObjective) {
					bestFrequencies = LinePlanningHelper.getFrequencies(linePool);
					bestObjective = solutionWrapper.getObjectiveValue();
				}
			}
		}

		logger.log(LogLevel.INFO, "Finished execution of line planning direct model");
		if (!solutionFound) {
			throw new AlgorithmStoppingCriterionException("direct model line planning");
		}
		LinePlanningHelper.setFrequencies(linePool, bestFrequencies);
		logger.log(LogLevel.INFO, "Begin writing output data");
		new LineWriter.Builder(linePool).build().write();
		logger.log(LogLevel.INFO, "Finished writing output data");
	}
}

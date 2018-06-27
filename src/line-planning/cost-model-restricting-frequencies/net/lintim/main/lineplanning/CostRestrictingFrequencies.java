package net.lintim.main.lineplanning;

import net.lintim.algorithm.lineplanning.CostRestrictingFrequenciesSolver;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.ConfigReader;
import net.lintim.io.LineReader;
import net.lintim.io.LineWriter;
import net.lintim.io.PTNReader;
import net.lintim.model.Graph;
import net.lintim.model.LinePool;
import net.lintim.model.Link;
import net.lintim.model.Stop;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.logging.Logger;

/**
 * Main class to solve the cost model with restricted frequencies of line planning.
 */
public class CostRestrictingFrequencies {
    public static void main(String args[]) throws ClassNotFoundException, IllegalAccessException,
        InstantiationException {
        Logger logger = Logger.getLogger(CostRestrictingFrequencies.class.getCanonicalName());
        logger.log(LogLevel.INFO, "Begin reading configuration");
        if (args.length != 1) {
            throw new ConfigNoFileNameGivenException();
        }
        new ConfigReader.Builder(args[0]).build().read();
        int timeLimit = Config.getIntegerValueStatic("lc_timelimit");
        int numberOfPossibleFrequencies = Config.getIntegerValueStatic("lc_number_of_possible_frequencies");
        int maximalFrequency = Config.getIntegerValueStatic("lc_maximal_frequency");
        SolverType solverType = Config.getSolverTypeStatic("lc_solver");
        logger.log(LogLevel.INFO, "Finished reading configuration");

        logger.log(LogLevel.INFO, "Begin reading input data");
        Graph<Stop, Link> ptn = new PTNReader.Builder().readLoads(true).build().read();
        LinePool linePool = new LineReader.Builder(ptn).build().read();
        logger.log(LogLevel.INFO, "Finished reading input data");

        logger.log(LogLevel.INFO, "Begin execution of line planning cost model restricting frequencies");
        CostRestrictingFrequenciesSolver solver = CostRestrictingFrequenciesSolver.getSolver(solverType);
        boolean feasibleSolutionFound = solver.solveLinePlanningCost(ptn, linePool, timeLimit,
            numberOfPossibleFrequencies, maximalFrequency);

        logger.log(LogLevel.INFO, "Finished execution of line planning cost model restricting frequencies");

        if (!feasibleSolutionFound) {
            throw new AlgorithmStoppingCriterionException("cost model line planning");
        }

        logger.log(LogLevel.INFO, "Begin writing output data");
        new LineWriter.Builder(linePool).build().write();
        logger.log(LogLevel.INFO, "Finished writing output data");
    }
}

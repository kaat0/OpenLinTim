package net.lintim.main.lineplanning;

import net.lintim.algorithm.lineplanning.LinePlanningCostSolver;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.exception.LinTimException;
import net.lintim.io.*;
import net.lintim.model.Graph;
import net.lintim.model.LinePool;
import net.lintim.model.Link;
import net.lintim.model.Stop;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.logging.Logger;

/**
 * Main class to solve the cost model of line planning.
 */
public class Cost {

    private static Logger logger = Logger.getLogger("net.lintim.main.lineplanning.Cost");

    public static void main(String args[]) {
        logger.log(LogLevel.INFO, "Begin reading configuration");
        if (args.length != 1) {
            throw new ConfigNoFileNameGivenException();
        }
        new ConfigReader.Builder(args[0]).build().read();
        int timeLimit = Config.getIntegerValueStatic("lc_timelimit");
        SolverType solverType = Config.getSolverTypeStatic("lc_solver");
        logger.log(LogLevel.INFO, "Finished reading configuration");

        logger.log(LogLevel.INFO, "Begin reading input data");
        Graph<Stop, Link> ptn = new PTNReader.Builder().readLoads(true).build().read();
        LinePool linePool = new LineReader.Builder(ptn).build().read();
        logger.log(LogLevel.INFO, "Finished reading input data");

        logger.log(LogLevel.INFO, "Begin execution of line planning cost model");
        LinePlanningCostSolver solver;
        try {
            solver = LinePlanningCostSolver.getLinePlanningCostSolver(solverType);
        } catch (Exception e) {
            logger.log(LogLevel.ERROR, "Could not load solver " + solverType + ", can you use it on your system?");
            throw new LinTimException(e.getMessage());
        }
        boolean optimalSolutionFound = solver.solveLinePlanningCost(ptn, linePool, timeLimit);
        logger.log(LogLevel.INFO, "Finished execution of line planning cost model");

        if (!optimalSolutionFound) {
            throw new AlgorithmStoppingCriterionException("cost model line planning");
        }

        logger.log(LogLevel.INFO, "Begin writing output data");
        new LineWriter.Builder(linePool).build().write();
        logger.log(LogLevel.INFO, "Finished writing output data");
    }
}

package net.lintim.main.lineplanning;

import net.lintim.algorithm.lineplanning.ExtendedCostSolver;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.*;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LinePlanningHelper;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import static net.lintim.util.LinePlanningHelper.setFrequencies;

/**
 * Main class to solve the cost model of line planning.
 */
public class ExtendedCost {

    private static Logger logger = Logger.getLogger("net.lintim.main.lineplanning.ExtendedCost");

    public static void main(String args[]) throws ClassNotFoundException, IllegalAccessException,
        InstantiationException {

        logger.log(LogLevel.INFO, "Begin reading configuration");
        if (args.length != 1) {
            throw new ConfigNoFileNameGivenException();
        }
        new ConfigReader.Builder(args[0]).build().read();
        int timeLimit = Config.getIntegerValueStatic("lc_timelimit");
        int commonFrequencyDivisor = Config.getIntegerValueStatic("lc_common_frequency_divisor");
        SolverType solverType = Config.getSolverTypeStatic("lc_solver");
        int periodLength = Config.getIntegerValueStatic("period_length");
        boolean respectFixedLines = Config.getBooleanValueStatic("lc_respect_fixed_lines");
        logger.log(LogLevel.INFO, "Finished reading configuration");

        logger.log(LogLevel.INFO, "Begin reading input data");
        Graph<Stop, Link> ptn = new PTNReader.Builder().readLoads(true).build().read();
        LinePool linePool = new LineReader.Builder(ptn).build().read();
        LinePool fixedLines = null;
        if(respectFixedLines) {
            fixedLines = new LineReader.Builder(ptn).setLineFileName("filename_lc_fixed_lines").readFrequencies(true)
                .readCosts(false).build().read();
            preprocessForFixedLines(fixedLines, linePool);
        }
        logger.log(LogLevel.INFO, "Finished reading input data");

        logger.log(LogLevel.INFO, "Begin execution of line planning extended cost model");
        ExtendedCostSolver solver = ExtendedCostSolver.getExtendedCostSolver(solverType);
        // Solve the problem for all possible system frequencies and determine the best
        Set<Integer> possibleSystemFrequencies = LinePlanningHelper.determinePossibleSystemFrequencies
            (commonFrequencyDivisor, periodLength);
        boolean optimalSolutionFound = false;
        double bestObjective = Double.POSITIVE_INFINITY;
        double objective;
        HashMap<Integer, Integer> bestFrequencies = null;
        for(int commonFrequency : possibleSystemFrequencies) {
            if(solver.solveLinePlanningCost(ptn, linePool, timeLimit,commonFrequency)) {
                optimalSolutionFound = true;
                objective = getCost(linePool);
                if(objective < bestObjective) {
                    bestObjective = objective;
                    bestFrequencies = LinePlanningHelper.getFrequencies(linePool);
                }
            }
        }
        logger.log(LogLevel.INFO, "Finished execution of line planning extended cost model");

        if (!optimalSolutionFound) {
            throw new AlgorithmStoppingCriterionException("cost model line planning");
        }

        setFrequencies(linePool, bestFrequencies);
        if (respectFixedLines) {
            postprocessForFixedLines(fixedLines, linePool);
        }

        logger.log(LogLevel.INFO, "Begin writing output data");
        new LineWriter.Builder(linePool).build().write();
        logger.log(LogLevel.INFO, "Finished writing output data");
    }

    /**
     * Get the cost of the line concept. The cost of a line is multiplied by its frequency and then summed up.
     * @param lineConcept the line concept to calculate the costs of.
     * @return the costs of the line concept.
     */
    private static double getCost(LinePool lineConcept) {
        return lineConcept.getLines().stream().mapToDouble(line -> line.getFrequency() * line.getCost()).sum();
    }

    private static void preprocessForFixedLines(LinePool fixedLines, LinePool allLines) {
        // Reduce the load for all already covered ptn edges
        // We assume the following: There is a separate transportation system, given by the fixed lines. Therefore,
        // the lower frequency bounds on the edges are changed by the fixed lines, i.e., there need to be less lines
        // in a feasible solution. But on the other hand, the upper frequency bound is not changed!
        for(Line line : fixedLines.getLines()) {
            for (Link link : line.getLinePath().getEdges()) {
                link.setLowerFrequencyBound(Math.max(0, link.getLowerFrequencyBound() - line.getFrequency()));
            }
        }
        for (Line line : fixedLines.getLines()) {
            allLines.removeLine(line.getId());
        }
    }

    private static void postprocessForFixedLines(LinePool fixedLines, LinePool allLines) {
       for (Line line : fixedLines.getLines()) {
           allLines.addLine(line);
       }
    }
}

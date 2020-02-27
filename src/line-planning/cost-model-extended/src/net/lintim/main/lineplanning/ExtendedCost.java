package net.lintim.main.lineplanning;

import net.lintim.algorithm.lineplanning.ExtendedCostSolver;
import net.lintim.exception.AlgorithmStoppingCriterionException;
import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.ConfigReader;
import net.lintim.io.LineReader;
import net.lintim.io.LineWriter;
import net.lintim.io.PTNReader;
import net.lintim.io.lineplanning.FixedLineCapacityReader;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LinePlanningHelper;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.HashMap;
import java.util.Map;
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
        boolean respectFixedLines = Config.getBooleanValueStatic("lc_respect_fixed_lines");
        int passengersPerVehicle = -1;
        String fixedLinesFileName = null;
        if (respectFixedLines) {
            fixedLinesFileName = Config.getStringValueStatic("filename_lc_fixed_lines");
            passengersPerVehicle = Config.getIntegerValueStatic("gen_passengers_per_vehicle");
        }
        int timeLimit = Config.getIntegerValueStatic("lc_timelimit");
        int commonFrequencyDivisor = Config.getIntegerValueStatic("lc_common_frequency_divisor");
        SolverType solverType = Config.getSolverTypeStatic("lc_solver");
        int periodLength = Config.getIntegerValueStatic("period_length");
        logger.log(LogLevel.INFO, "Finished reading configuration");

        logger.log(LogLevel.INFO, "Begin reading input data");
        Graph<Stop, Link> ptn = new PTNReader.Builder().readLoads(true).build().read();
        LinePool linePool = new LineReader.Builder(ptn).build().read();
        LinePool fixedLines = null;
        Map<Line, Integer> fixedLineCapacities = null;
        if(respectFixedLines) {
            fixedLines = new LineReader.Builder(ptn).setLineFileName(fixedLinesFileName).readFrequencies(true)
                .readCosts(false).build().read();
            fixedLineCapacities = new FixedLineCapacityReader.Builder(fixedLines).build().read();
        }
        logger.log(LogLevel.INFO, "Finished reading input data");

        logger.log(LogLevel.INFO, "Begin execution of line planning extended cost model");
        if (respectFixedLines) {
            preprocessForFixedLines(fixedLines, linePool, fixedLineCapacities, passengersPerVehicle);
        }
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

    private static void preprocessForFixedLines(LinePool fixedLines, LinePool allLines, Map<Line, Integer>
        capacities, int normalCapacity) {
        // Reduce the load for all already covered ptn edges
        // We assume the following: There is a separate transportation system, given by the fixed lines. Therefore,
        // the lower frequency bounds on the edges are changed by the fixed lines, i.e., there need to be less lines
        // in a feasible solution. But on the other hand, the upper frequency bound is not changed!
        // Gather data for debug output
        double maxLoad = 0;
        for (Line line : allLines.getLines()) {
            for (Link link : line.getLinePath().getEdges()) {
                maxLoad = Math.max(maxLoad, link.getLoad());
            }
        }
        logger.log(LogLevel.DEBUG, String.format("Max load is %f", maxLoad));
        maxLoad = 0;
        // Iterate all fixed lines and update bounds
        for(Line line : fixedLines.getLines()) {
            for (Link link : line.getLinePath().getEdges()) {
                double oldLoad = link.getLoad();
                double newLoad = oldLoad - line.getFrequency() * capacities.get(line);
                // Update debug information
                maxLoad = Math.max(maxLoad, newLoad);
                // Set new information
                link.setLoad(newLoad);
                link.setLowerFrequencyBound(Math.max(0, (int) Math.ceil(newLoad / normalCapacity)));
            }
        }
        logger.log(LogLevel.DEBUG, String.format("New max load on fixed lines is %f", maxLoad));
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

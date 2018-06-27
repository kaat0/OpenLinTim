package net.lintim.algorithm.lineplanning;

import gurobi.*;
import net.lintim.exception.SolverGurobiException;
import net.lintim.model.*;
import net.lintim.util.LogLevel;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A class to solve the cost model with restricted frequencies of line planning using Gurobi.
 */
public class CostRestrictingFrequenciesGurobi extends CostRestrictingFrequenciesSolver {

    @Override
    public boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timelimit, int
        numberOfPossibleFrequencies, int maximalFrequency) {
        Logger logger = Logger.getLogger("net.lintim.algorithm.lineplanning.CostRestrictingFrequenciesGurobi");
        Level logLevel = LogManager.getLogManager().getLogger("").getLevel();
        GRBModel costModel;
        HashMap<Integer, HashMap<Integer, GRBVar>> frequencies = new HashMap<>();
        try {
            GRBEnv env = new GRBEnv();
            costModel = new GRBModel(env);
            costModel.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            double solverTimelimit = timelimit <= 0 ? GRB.INFINITY : timelimit;
            logger.log(LogLevel.DEBUG, "Set Gurobi timelimit to " + solverTimelimit);
            costModel.set(GRB.DoubleParam.TimeLimit, solverTimelimit);

            if (logLevel.equals(LogLevel.DEBUG)) {
                costModel.set(GRB.IntParam.LogToConsole, 1);
                costModel.set(GRB.StringParam.LogFile, "CostModelGurobi.log");
            } else {
                costModel.set(GRB.IntParam.OutputFlag, 0);
            }

            //Add variables
            logger.log(LogLevel.DEBUG, "Add variables");
            for (Line line : linePool.getLines()) {
                frequencies.put(line.getId(), new HashMap<>());
                GRBLinExpr lineFreqConstraint = new GRBLinExpr();
                for(int freq = 0; freq <= maximalFrequency; freq++){
                    GRBVar lineUsesFreq = costModel.addVar(0, 1, freq * line.getCost(), GRB.BINARY,
                        "f_" + line.getId() + "_" + freq);
                    frequencies.get(line.getId()).put(freq, lineUsesFreq);
                    lineFreqConstraint.addTerm(1, lineUsesFreq);
                }
                costModel.addConstr(lineFreqConstraint, GRB.EQUAL, 1, "line_" + line.getId() + "_uses_one_freq");
            }
            logger.log(LogLevel.DEBUG, "Update model");
            costModel.update();
            if(numberOfPossibleFrequencies != -1) {
                GRBLinExpr freqConstraint = new GRBLinExpr();
                for (int freq = 1; freq <= maximalFrequency; freq++) {
                    GRBVar freqUsed = costModel.addVar(0, 1, 0, GRB.BINARY, "freq_" + freq + "_used");
                    freqConstraint.addTerm(1, freqUsed);
                    for (Line line : linePool.getLines()) {
                        costModel.addConstr(freqUsed, GRB.GREATER_EQUAL, frequencies.get(line.getId()).get(freq),
                            "line_" + line.getId() + "_uses_freq_" + freq);
                    }
                }
                costModel.addConstr(freqConstraint, GRB.LESS_EQUAL, numberOfPossibleFrequencies, "freq_bound");
            }
            //Add constraints
            logger.log(LogLevel.DEBUG, "Add frequency constraints");
            GRBLinExpr sumFreqPerLine;
            for (Link link : ptn.getEdges()) {
                sumFreqPerLine = new GRBLinExpr();
                for (Line line : linePool.getLines()) {
                    if (line.getLinePath().getEdges().contains(link)) {
                        for(int freq = 1; freq <= maximalFrequency; freq++){
                            sumFreqPerLine.addTerm(freq, frequencies.get(line.getId()).get(freq));
                        }
                    }
                }
                costModel.addConstr(sumFreqPerLine, GRB.GREATER_EQUAL, link.getLowerFrequencyBound(), "lowerBound_" +
                    link.getId());
                costModel.addConstr(sumFreqPerLine, GRB.LESS_EQUAL, link.getUpperFrequencyBound(), "upperBound_" +
                    link.getId());

            }
            if(logLevel == LogLevel.DEBUG){
                logger.log(LogLevel.DEBUG, "Writing model file");
                costModel.write("gurobi_lc.lp");
            }
            logger.log(LogLevel.DEBUG, "Start optimization");
            costModel.optimize();
            logger.log(LogLevel.DEBUG, "End optimization");

            int status = costModel.get(GRB.IntAttr.Status);
            if(status == GRB.OPTIMAL){
                logger.log(LogLevel.DEBUG, "Optimal solution found");
            }
            else if(status == GRB.TIME_LIMIT){
                logger.log(LogLevel.DEBUG, "Timelimit hit");
            }
            else {
                logger.log(LogLevel.DEBUG, "No solution found");
                if(logLevel == LogLevel.DEBUG){
                    logger.log(LogLevel.DEBUG, "Computing IIS");
                    costModel.computeIIS();
                    costModel.write("gurobi_lc.ilp");
                }
                return false;
            }
            logger.log(LogLevel.DEBUG, "Read back solution");
            for (Line line : linePool.getLines()) {
                for(int freq = 0; freq <= maximalFrequency; freq++){
                    if(Math.round(frequencies.get(line.getId()).get(freq).get(GRB.DoubleAttr.X)) > 0){
                        line.setFrequency(freq);
                        break;
                    }
                }
            }
            return true;

        } catch (GRBException e) {
            throw new SolverGurobiException(e.toString());
        }
    }

}

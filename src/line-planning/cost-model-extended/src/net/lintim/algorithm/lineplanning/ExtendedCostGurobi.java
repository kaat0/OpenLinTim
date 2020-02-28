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
 * A class to solve the cost model of line planning using Gurobi.
 */
public class ExtendedCostGurobi extends ExtendedCostSolver {

    @Override
    public boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timelimit, int
        commonFrequencyDivisor) {
        Logger logger = Logger.getLogger("net.lintim.algorithm.lineplanning.CostGurobi");
        Level logLevel = LogManager.getLogManager().getLogger("").getLevel();
        GRBModel costModel;

        try {
            GRBEnv env = new GRBEnv();
            costModel = new GRBModel(env);
            costModel.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            double solverTimelimit = timelimit == -1 ? GRB.INFINITY : timelimit;
            logger.log(LogLevel.DEBUG, "Set Gurobi timelimit to " + solverTimelimit);
            costModel.set(GRB.DoubleParam.TimeLimit, solverTimelimit);

            if (logLevel.equals(LogLevel.DEBUG)) {
                costModel.set(GRB.IntParam.LogToConsole, 1);
                costModel.set(GRB.StringParam.LogFile, "CostModelGurobi.log");
            } else {
                costModel.set(GRB.IntParam.OutputFlag, 0);
            }

            //Add variables
            logger.log(LogLevel.DEBUG, "Add variables and system frequency constraint");
            HashMap<Integer, GRBVar> frequencies = new HashMap<>();
            for (Line line : linePool.getLines()) {
                GRBVar frequency = costModel.addVar(0, GRB.INFINITY, line.getCost(), GRB.INTEGER, "f_" + line.getId());
                frequencies.put(line.getId(), frequency);
                GRBVar systemFrequencyDivisor = costModel.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "g_" +
                    line.getId());
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(commonFrequencyDivisor, systemFrequencyDivisor);
                costModel.addConstr(frequency, GRB.EQUAL, rhs, "systemFrequency_" + line.getId());
            }
            logger.log(LogLevel.DEBUG, "Update model");
            costModel.update();

            //Add constraints
            logger.log(LogLevel.DEBUG, "Add frequency constraints");
            GRBLinExpr sumFreqPerLine;
            for (Link link : ptn.getEdges()) {
                sumFreqPerLine = new GRBLinExpr();
                for (Line line : linePool.getLines()) {
                    if (line.getLinePath().getEdges().contains(link)) {
                        sumFreqPerLine.addTerm(1, frequencies.get(line.getId()));
                    }
                }
                costModel.addConstr(sumFreqPerLine, GRB.GREATER_EQUAL, link.getLowerFrequencyBound(), "lowerBound_" +
                    link.getId());
                costModel.addConstr(sumFreqPerLine, GRB.LESS_EQUAL, link.getUpperFrequencyBound(), "upperBound_" +
                    link.getId());

            }


            logger.log(LogLevel.DEBUG, "Start optimization");
            costModel.optimize();
            logger.log(LogLevel.DEBUG, "End optimization");
            int status = costModel.get(GRB.IntAttr.Status);
            if (status == GRB.OPTIMAL) {
                logger.log(LogLevel.DEBUG, "Optimal solution found");
                for (Line line : linePool.getLines()) {
                    line.setFrequency((int) Math.round(frequencies.get(line.getId()).get(GRB.DoubleAttr.X)));
                }
                return true;
            }
            logger.log(LogLevel.DEBUG, "No optimal solution found");
            if (status == GRB.INFEASIBLE && logLevel == LogLevel.DEBUG) {
                costModel.computeIIS();
                costModel.write("extended-cost.ilp");
            }
            return false;
        } catch (GRBException e) {
            throw new SolverGurobiException(e.toString());
        }
    }
}

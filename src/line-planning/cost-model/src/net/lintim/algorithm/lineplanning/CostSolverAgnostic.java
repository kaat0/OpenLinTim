package net.lintim.algorithm.lineplanning;

import net.lintim.model.*;
import net.lintim.solver.*;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Solver agnostic implementation of the cost model. The solver to use is given in the constructor
 * {@link #CostSolverAgnostic(SolverType)}.
 */
public class CostSolverAgnostic extends LinePlanningCostSolver{

    private static Logger logger = Logger.getLogger(CostSolverAgnostic.class.getCanonicalName());

    private final SolverType solverType;

    /**
     * Create a new solver class, dependent on the given solver type. An instance of the given solver will be used to
     * solver the models in {@link #solveLinePlanningCost(Graph, LinePool, int)} and
     * {@link #solveLinePlanningCost(Graph, LinePool, int, Level)}.
     * @param solverType the solver type to use
     */
    public CostSolverAgnostic(SolverType solverType) {
        this.solverType = solverType;
    }

    @Override
    public boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timeLimit) {
        Level logLevel = LogManager.getLogManager().getLogger("").getLevel();
        return solveLinePlanningCost(ptn, linePool, timeLimit, logLevel);
    }

    @Override
    public boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timeLimit, Level logLevel) {
        Model costModel = new Model();

        // Add variables
        logger.log(LogLevel.DEBUG, "Add variables");
        HashMap<Integer, Variable> frequencies = new HashMap<>();
        for (Line line : linePool.getLines()) {
            Variable frequency = costModel.addVariable("f_" + line.getId(), Variable.VariableType.INTEGER, 0,
                Double.POSITIVE_INFINITY, line.getCost());
            frequencies.put(line.getId(), frequency);
        }

        // Add constraints
        logger.log(LogLevel.DEBUG, "Add frequency constraints");
        LinearExpression sumFreqPerLine;
        for (Link link : ptn.getEdges()) {
            sumFreqPerLine = new LinearExpression();
            for (Line line : linePool.getLines()) {
                if (line.getLinePath().getEdges().contains(link)) {
                    sumFreqPerLine.addTerm(1, frequencies.get(line.getId()));
                }
            }
            costModel.addConstraint(sumFreqPerLine, Constraint.ConstraintSense.GREATER_EQUAL,
                link.getLowerFrequencyBound(), "lowerBound_" + link.getId());
            costModel.addConstraint(sumFreqPerLine, Constraint.ConstraintSense.LESS_EQUAL,
                link.getUpperFrequencyBound(), "upperBound" + link.getId());
        }

        // Transform model
        logger.log(LogLevel.DEBUG, "Transforming model to solver " + solverType.toString());
        Solver solver = Solver.getSolver(costModel, solverType);
        solver.setIntParam(Solver.IntParam.TIMELIMIT, timeLimit);
        solver.setIntParam(Solver.IntParam.OUTPUT_LEVEL, logLevel.intValue());
        solver.transformModel();
        if (logLevel.equals(LogLevel.DEBUG)) {
            logger.log(LogLevel.DEBUG, "Writing lp file");
            solver.writeSolverSpecificLpFile("costModel");
            logger.log(LogLevel.DEBUG, "Done");
        }

        logger.log(LogLevel.DEBUG, "Start optimization");
        solver.optimize();
        logger.log(LogLevel.DEBUG, "End optimization");

        // Read back solution
        int status = solver.getIntAttribute(Solver.IntAttribute.STATUS);
        if (status == Solver.OPTIMAL || status == Solver.FEASIBLE) {
            if (status == Solver.OPTIMAL) {
                logger.log(LogLevel.DEBUG, "Optimal solution found");
            }
            else {
                logger.log(LogLevel.DEBUG, "Feasible solution found");
            }
            for (Line line : linePool.getLines()) {
                line.setFrequency((int) Math.round(solver.getValue(frequencies.get(line.getId()))));
            }
            return true;
        }
        logger.log(LogLevel.DEBUG, "No optimal solution found");
        if (status == Solver.INFEASIBLE) {
            logger.log(LogLevel.DEBUG, "The problem is infeasible!");
            if (logLevel == LogLevel.DEBUG) {
                solver.computeIIS("cost-model");
            }
        }
        return false;
    }
}

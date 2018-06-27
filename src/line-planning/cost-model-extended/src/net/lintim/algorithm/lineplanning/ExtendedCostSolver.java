package net.lintim.algorithm.lineplanning;

import net.lintim.exception.SolverNotSupportedException;
import net.lintim.model.Graph;
import net.lintim.model.LinePool;
import net.lintim.model.Link;
import net.lintim.model.Stop;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.logging.Logger;

/**
 * Abstraction class for the different solver classes, i.e., {@link ExtendedCostGurobi} and {@link ExtendedCostXpress}.
 */
public abstract class ExtendedCostSolver {

    private static Logger logger = Logger.getLogger(ExtendedCostSolver.class.getCanonicalName());

    public static ExtendedCostSolver getExtendedCostSolver(SolverType solverType) throws ClassNotFoundException,
        InstantiationException, IllegalAccessException {
        String solverClassName;
        switch (solverType) {
            case GUROBI:
                logger.log(LogLevel.DEBUG, "Will use Gurobi for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.ExtendedCostGurobi";
                break;
            case XPRESS:
                logger.log(LogLevel.DEBUG, "Will use Xpress for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.ExtendedCostXpress";
                break;
            default:
                throw new SolverNotSupportedException(solverType.toString(), "line planning cost model");
        }
        Class<?> solverClass = Class.forName(solverClassName);
        return (ExtendedCostSolver) solverClass.newInstance();
    }

    /**
     * Solve the line planning problem for the given data. This is just an abstract method, use
     * {@link ExtendedCostGurobi#solveLinePlanningCost(Graph, LinePool, int, int)} or
     * {@link ExtendedCostXpress#solveLinePlanningCost(Graph, LinePool, int, int)} for the actual implementation.
     * @param ptn the underlying ptn to compute the line concept for
     * @param linePool the linepool to choose the lines from
     * @param timelimit the timelimit for the solver
     * @param commonFrequencyDivisor the common divisor of all used frequencies
     * @return whether an optimal/feasible solution could be found.
     */
    public abstract boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timelimit, int
        commonFrequencyDivisor);
}

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
 * Abstraction class for the different solvers, i.e., {@link CostRestrictingFrequenciesGurobi} and
 * {@link CostRestrictingFrequenciesXpress}.
 */
public abstract class CostRestrictingFrequenciesSolver {

    private static Logger logger = Logger.getLogger(CostRestrictingFrequenciesSolver.class.getCanonicalName());

    /**
     * Solve the line planning problem for the given data. This is just an abstract method, use
     * {@link CostRestrictingFrequenciesGurobi#solveLinePlanningCost(Graph, LinePool, int, int, int)} or
     * {@link CostRestrictingFrequenciesXpress#solveLinePlanningCost(Graph, LinePool, int, int, int)} for the actual implementation.
     * @param ptn the underlying ptn to compute the line concept for
     * @param linePool the linepool to choose the lines from
     * @param timelimit the timelimit for the solver
     * @param numberOfPossibleFrequencies the number of possible frequencies
     * @param maximalFrequency the maximal allowed frequency
     * @return whether an optimal/feasible solution could be found.
     */
    public abstract boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timelimit, int
        numberOfPossibleFrequencies, int maximalFrequency);

    public static CostRestrictingFrequenciesSolver getSolver(SolverType solverType) throws ClassNotFoundException,
        IllegalAccessException, InstantiationException {
        String solverClassName = "";
        switch (solverType) {
            case GUROBI:
                logger.log(LogLevel.DEBUG, "Will use Gurobi for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.CostRestrictingFrequenciesGurobi";
                break;
            case XPRESS:
                logger.log(LogLevel.DEBUG, "Will use Xpress for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.CostRestrictingFrequenciesXpress";
                break;
            default:
                throw new SolverNotSupportedException(solverType.toString(), "line planning cost model restricting frequencies");
        }
        Class<?> solverClass = Class.forName(solverClassName);
        return (CostRestrictingFrequenciesSolver) solverClass.newInstance();
    }
}

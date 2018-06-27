package net.lintim.algorithm.lineplanning;

import net.lintim.model.Graph;
import net.lintim.model.LinePool;
import net.lintim.model.Link;
import net.lintim.model.Stop;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic class to solve the line planning cost model in its basic form.
 */
public abstract class LinePlanningCostSolver {
    private static Logger logger = Logger.getLogger(LinePlanningCostSolver.class.getCanonicalName());

    /**
     * Solve the line planning problem for the given data. Will model the cost optimization problem for the given data
     * and solve it using the provided solver type.
     * @param ptn the underlying ptn to compute the line concept for
     * @param linePool the linepool to choose the lines from
     * @param timeLimit the timelimit for the solver
     * @param logLevel the log level to use. Using {@link LogLevel#DEBUG} will enable computation of an IIS, if the
     *                 model is infeasible
     * @return whether an optimal/feasible solution could be found.
     */
    public abstract boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timeLimit, Level
        logLevel);

    /**
     * Solve the line planning problem for the given data. Will model the cost optimization problem for the given data
     * and solve it using the provided solver type.
     * @param ptn the underlying ptn to compute the line concept for
     * @param linePool the linepool to choose the lines from
     * @param timeLimit the timelimit for the solver
     * @return whether an optimal/feasible solution could be found.
     */
    public abstract boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timeLimit);

    public static LinePlanningCostSolver getLinePlanningCostSolver(SolverType solverType)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException,
        InvocationTargetException {
        String solverClassName;
        Class<?> solverClass;
        switch (solverType) {
            case GUROBI:
                logger.log(LogLevel.DEBUG, "Will use Gurobi for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.CostGurobi";
                solverClass = Class.forName(solverClassName);
                return (LinePlanningCostSolver) solverClass.newInstance();
            case XPRESS:
                logger.log(LogLevel.DEBUG, "Will use Xpress for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.CostXpress";
                solverClass = Class.forName(solverClassName);
                return (LinePlanningCostSolver) solverClass.newInstance();
            default:
                logger.log(LogLevel.DEBUG, "Will use solver agnostic version for optimization");
                solverClassName = "net.lintim.algorithm.lineplanning.CostSolverAgnostic";
                solverClass = Class.forName(solverClassName);
                return (LinePlanningCostSolver) solverClass.getDeclaredConstructor(SolverType.class).newInstance
                    (solverType);
        }
    }
}

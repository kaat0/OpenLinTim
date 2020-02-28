package net.lintim.solver;

import net.lintim.exception.LinTimException;
import net.lintim.exception.SolverNotImplementedException;
import net.lintim.util.Logger;
import net.lintim.util.SolverType;

/**
 * Abstract class representing a solver. For actual implementations, see {@link net.lintim.solver.impl}. To get an
 * instance of a solver, use {@link #createSolver(SolverType)}. Afterwards, you can create a model using
 * {@link #createModel()}.
 */
public abstract class Solver {
    private static Logger logger = new Logger(Solver.class);
    protected Model model;

    public static Solver createSolver(SolverType type){
        String solverClassName = getSolverClassName(type);
        try {
            Class<?> solverClass = Class.forName(solverClassName);
            // newInstance is currently deprecated. In the future, use .getDeclaredConstructor().newInstance() but
            // we can only use this when dropping support for Java 8.
            return (Solver) solverClass.newInstance();
        } catch (Exception e) {
            logger.debug("Tried to load class " + solverClassName);
            logger.debug(e.getMessage());
            throw new LinTimException("Your solver " + type.toString() + " could not be loaded. Please make sure that" +
                " the corresponding jar files are in your classpath! For more information on how to find these, see " +
                "the documentation of your solver.");
        }
    }

    private static String getSolverClassName(SolverType type) {
        String solverClassName;
        switch (type) {
            case GUROBI:
                logger.debug("Using Gurobi for optimization");
                solverClassName = "net.lintim.solver.impl.GurobiSolver";
                break;
            case XPRESS:
                logger.debug("Using Xpress for optimization");
                solverClassName = "net.lintim.solver.impl.XpressSolver";
                break;
            case CPLEX:
                logger.debug("Using Cplex for optimization");
                solverClassName = "net.lintim.solver.impl.CplexSolver";
                break;
            default:
                throw new SolverNotImplementedException(type);
        }
        return solverClassName;
    }

    public abstract Model createModel();

    public static SolverType parseSolverType(String solverName) {
        switch (solverName.toUpperCase()) {
            case "XPRESS":
                return SolverType.XPRESS;
            case "GUROBI":
                return SolverType.GUROBI;
            case "CPLEX":
                return SolverType.CPLEX;
            case "GLPK":
                return SolverType.GLPK;
            default:
                throw new LinTimException("Solver " + solverName + " is unknown");
        }
    }


}


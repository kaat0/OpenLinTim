package net.lintim.solver;

import net.lintim.exception.LinTimException;
import net.lintim.exception.SolverNotImplementedException;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Abstract class representing a solver. For actual implementations, see {@link net.lintim.solver.impl}. To get an
 * instance of a solver, use {@link #getSolver(Model, SolverType)}. Afterwards, you can solve the model using
 * {@link #solveModel()}.
 */
public abstract class Solver {
    private static Logger logger = Logger.getLogger(Solver.class.getCanonicalName());
    protected Model model;
    protected HashMap<DoubleParam, Double> doubleParams;
    protected HashMap<IntParam, Integer> intParams;

    /**
     * Create a new solver for the given model. To obtain an actual implementation, use
     * {@link #getSolver(Model, SolverType)}.
     * @param model the model to get a solver for.
     */
    public Solver(Model model){
        this.model = model;
        this.doubleParams = new HashMap<>();
        this.intParams = new HashMap<>();
    }

    /**
     * Transform the model to the solver specific model.
     */
    abstract public void transformModel();

    /**
     * Solve the model with this solver.
     */
    public void optimize(){
        solveModel();
    }

    /**
     * Solve the model with this solver.
     */
    abstract public void solveModel();

    /**
     * Compute an IIS for the model and store it in the given file. The file ending will be depending of the used
     * solver.
     * @param fileName the file name ot use, without a file ending.
     */
    abstract public void computeIIS(String fileName);

    /**
     * Get the value of the given variable. Can only be queried, if there is a feasible solution, i.e., if
     * {@link IntAttribute#STATUS} is {@link #OPTIMAL} or {@link #TIMELIMIT}. Otherwise, the method will throw!
     * @param variable the variable to query.
     * @return the current value of the variable.
     */
    abstract public double getValue(Variable variable);

    /**
     * Get the value of the given attribute. For a list of attributes, see {@link DoubleAttribute}.
     * @param attribute the attribute to query.
     * @return the value of the given attribute
     */
    abstract public double getDoubleAttribute(DoubleAttribute attribute);

    /**
     * Get the value of the given attribute. For a list of attributes, see {@link IntAttribute}.
     * @param attribute the attribute to query.
     * @return the value of the given attribute
     */
    abstract public int getIntAttribute(IntAttribute attribute);

    /**
     * Write the solver specific lp file. For a generic lp file, see {@link Model#write(String)}. The file ending
     * will be dependent on the used solver.
     * @param lpFileName the file name to write, without a file ending
     */
    abstract public void writeSolverSpecificLpFile(String lpFileName);

    /**
     * Get a specific solver for the given model, dependent on the provided type. For possible values, see
     * {@link SolverType}.
     * @param model the model to provide a solver for
     * @param type the type of solver to initiate.
     * @return a new solver of the given type for the given model
     */
    public static Solver getSolver(Model model, SolverType type) {
        String solverClassName;
        switch (type) {
            case GUROBI:
                logger.log(LogLevel.DEBUG, "Using Gurobi for optimization");
                solverClassName = "net.lintim.solver.impl.GurobiSolver";
                break;
            case XPRESS:
                logger.log(LogLevel.DEBUG, "Using Xpress for optimization");
                solverClassName = "net.lintim.solver.impl.XpressSolver";
                break;
            case CPLEX:
                logger.log(LogLevel.DEBUG, "Using Cplex for optimization");
                solverClassName = "net.lintim.solver.impl.CplexSolver";
                break;
            default:
                throw new SolverNotImplementedException(type);
        }
        try {
            Class<?> solverClass = Class.forName(solverClassName);
            return (Solver) solverClass.getDeclaredConstructor(Model.class).newInstance(model);
        } catch (Exception e) {
            throw new LinTimException("Your solver " + type.toString() + " could not be loaded. Please make sure that" +
                " the corresponding jar files are in your classpath! For more information on how to find these, see " +
                "the documentation of your solver.");
        }
    }

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

    /**
     * Set the given int parameter. For a list of possible parameters, see {@link Solver.IntParam}.
     * @param param the parameter to set
     * @param value the value to set the parameter to.
     */
    public void setIntParam(Solver.IntParam param, int value) {
        this.intParams.put(param, value);
    }

    /**
     * Set the given double parameter. For a list of possible parameters, see {@link Solver.DoubleParam}.
     * @param param the parameter to set
     * @param value the value to set the parameter to.
     */
    public void setDoubleParam(Solver.DoubleParam param, double value) {
        this.doubleParams.put(param, value);
    }

    /**
     * Represents that an optimal solution could be found.
     */
    public static final int OPTIMAL = 1;
    /**
     * Represents that the model is infeasible.
     */
    public static final int INFEASIBLE = 2;
    /**
     * Represents that a feasible, but no optimal solution, was found. Probably due to hitting the timelimit.
     */
    public static final int FEASIBLE = 3;
    /**
     * Represents that the timelimit was hit during the optimization and no feasible solution was found beforehand.
     */
    public static final int TIMELIMIT = 4;


    /**
     * Possible double parameters to set.
     */
    public enum DoubleParam {
        /**
         * The mip gap to achieve. A mip gap of 0.1 represents 10%.
         */
        MIP_GAP,
    }

    /**
     * Possible integer parameters to set.
     */
    public enum IntParam {
        /**
         * The timelimit to set. A negative values is interpreted as no timelimit at all, positive integers
         * represent the number of seconds  allowed for solving. 0 will be treated as "abort as early as possible".
         */
        TIMELIMIT,
        /**
         * The desired output level of the solver. Use the values of {@link LogLevel}.
         */
        OUTPUT_LEVEL,
    }

    /**
     * Integer attributes of the solver.
     */
    public enum IntAttribute {
        /**
         * The status of the solver. Can only be queried after calling {@link #optimize()} or {@link #solveModel()}.
         * Possible values are {@link Solver#OPTIMAL}, {@link Solver#FEASIBLE}, {@link Solver#INFEASIBLE} and
         * {@link Solver#TIMELIMIT}.
         */
        STATUS,
    }

    /**
     * Double attributes of the solver.
     */
    public enum DoubleAttribute {
        /**
         * The objective value of the current solution. Can only be called on solvers with status
         * {@link Solver#OPTIMAL} or if the {@link Solver#TIMELIMIT} was hit and a feasible solution is present.
         */
        OBJ_VAL,
        /**
         * The mip gap of the current solution. Can only be called on solvers with status
         * {@link Solver#OPTIMAL} or if the {@link Solver#TIMELIMIT} was hit and a feasible solution is present.
         */
        MIP_GAP,
    }
}


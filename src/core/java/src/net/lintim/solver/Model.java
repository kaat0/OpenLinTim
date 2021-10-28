package net.lintim.solver;

import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

/**
 * Class containing a generic optimization model. Use an instance of this class to build a model and then solve it using
 * {@link Solver#createSolver(SolverType)} and {@link Solver#createModel()}.
 */
public interface Model {

    /**
     * Create a new variable and add it to the model.
     * @param lowerBound the lower bound of the variable
     * @param upperBound the upper bound of the variable
     * @param type the type of the variable
     * @param objective the objective of the variable
     * @param name the name of the variable. The variable will obtain a unique name, i.e., the name will be replaced
     *             if it is not unique in the model.
     * @return the newly created variable
     */
    Variable addVariable(double lowerBound, double upperBound, Variable.VariableType type, double objective, String name);

    /**
     * Create a new constraint and add it to the model
     * @param expression the left hand side expression of the constraint. To create an expression, use
     * {@link #createExpression()}.
     * @param sense the sense of the constraint
     * @param rhs the right hand side constant of the constraint
     * @param name the name of the constraint
     * @return the newly create constraint
     */
    Constraint addConstraint(LinearExpression expression, Constraint.ConstraintSense sense, double rhs, String name);


    /**
     * Get a variable by its name.
     * @param name the name of the variable
     * @return the variable with the given name
     */
    Variable getVariableByName(String name);


    /**
     * Set a starting value for the given variable.
     * @param variable the variable to set a starting value for
     * @param value the starting value to set
     */
    void setStartValue(Variable variable, double value);

    /**
     * Set the given objective with the given sense for the model.
     * @param objective the objective to set
     * @param sense the sense of the objective
     */
    void setObjective(LinearExpression objective, OptimizationSense sense);

    /**
     * Get the current objective function of the model
     * @return the objective
     */
    LinearExpression getObjective();

    /**
     * Create a new expression
     * @return the newly created expression
     */
    LinearExpression createExpression();

    /**
     * Get the sense of the objective function, i.e. {@link OptimizationSense#MINIMIZE} or
     * {@link OptimizationSense#MAXIMIZE}.
     * @return the sense of the objective function
     */
    OptimizationSense getSense();

    /**
     * Write the model to the given file.
     * @param filename the file to write to.
     */
    void write(String filename);

    /**
     * Set the optimization sense of this model.
     * @param sense the new sense
     */
    void setSense(OptimizationSense sense);

    /**
     * Possible optimization senses. Can be set using {@link #setSense(OptimizationSense)} or
     * {@link #setObjective(LinearExpression, OptimizationSense)}.
     */
    enum OptimizationSense {
        MINIMIZE, MAXIMIZE
    }

    /**
     * Different attributes of the model that can be queried using {@link #getIntAttribute(IntAttribute)}.
     *
     * The attributes have the following meaning:
     * <ul>
     *     <li>NUM_VARIABLES: The number of variables in the model</li>
     *     <li>NUM_CONSTRAINTS: The number of constraints in the model</li>
     *     <li>NUM_INT_VARIABLES: The number of integer variables in the model</li>
     *     <li>NUM_BIN_VARIABLES: The number of binary variables in the model</li>
     * </ul>
     */
    enum IntAttribute {
        /**
         * The number of variables in the model
         */
        NUM_VARIABLES,
        /**
         * The number of constraints in the model
         */
        NUM_CONSTRAINTS,
        /**
         * The number of integer variables in the model
         */
        NUM_INT_VARIABLES,
        /**
         * The number of binary variables in the model
         */
        NUM_BIN_VARIABLES,
        /**
         * The number of feasible solutions
         */
        NUM_SOLUTIONS
    }

    /**
     * Get the value of an int attribute for the model. For a description of the possible attributes, see
     * {@link IntAttribute}.
     * @param attribute the attribute to query
     * @return the value of the attribute
     */
    int getIntAttribute(IntAttribute attribute);/**
     * Solve the model with this solver.
     */

    Status getStatus();
    void solve();

    /**
     * Compute an IIS for the model and store it in the given file. The file ending will be depending of the used
     * solver.
     * @param fileName the file name ot use, without a file ending.
     */
    void computeIIS(String fileName);

    /**
     * Get the value of the given variable. Can only be queried, if there is a feasible solution, i.e., if
     * {@link #getStatus()} is {@link Status#OPTIMAL} or {@link Status#FEASIBLE}. Otherwise, the method will throw!
     * @param variable the variable to query.
     * @return the current value of the variable.
     */
    double getValue(Variable variable);

    /**
     * Get the value of the given attribute. For a list of attributes, see {@link Model.DoubleAttribute}.
     * @param attribute the attribute to query.
     * @return the value of the given attribute
     */
    double getDoubleAttribute(DoubleAttribute attribute);

    /**
     * Set the given int parameter. For a list of possible parameters, see {@link Model.IntParam}.
     * @param param the parameter to set
     * @param value the value to set the parameter to.
     */
    void setIntParam(IntParam param, int value);

    /**
     * Set the given double parameter. For a list of possible parameters, see {@link Model.DoubleParam}.
     * @param param the parameter to set
     * @param value the value to set the parameter to.
     */
    void setDoubleParam(DoubleParam param, double value);

    enum Status {
        /**
         * Represents that an optimal solution could be found.
         */
        OPTIMAL,
        /**
         * Represents that the model is infeasible.
         */
        INFEASIBLE,
        /**
         * Represents that a feasible, but no optimal solution, was found. Probably due to hitting the timelimit.
         */
        FEASIBLE,
    }


    /**
     * Possible double parameters to set.
     */
    enum DoubleParam {
        /**
         * The mip gap to achieve. A mip gap of 0.1 represents 10%.
         */
        MIP_GAP,
    }

    /**
     * Possible integer parameters to set.
     */
    enum IntParam {
        /**
         * The timelimit to set. A negative values is interpreted as no timelimit at all, positive integers
         * represent the number of seconds  allowed for solving. 0 will be treated as "abort as early as possible".
         */
        TIMELIMIT,
        /**
         * The desired output level of the solver. Use the values of {@link LogLevel}.
         */
        OUTPUT_LEVEL,
        /**
         * The number of threads the solver is allowed to use. Use -1 for no restriction.
         */
        THREAD_LIMIT,
    }

    /**
     * Double attributes of the solver.
     */
    enum DoubleAttribute {
        /**
         * The objective value of the current solution. Can only be called on solvers with status
         * {@link Status#OPTIMAL} or if the {@link Status#FEASIBLE} was hit and a feasible solution is present.
         */
        OBJ_VAL,
        /**
         * The mip gap of the current solution. Can only be called on solvers with status
         * {@link Status#OPTIMAL} or if the {@link Status#FEASIBLE} was hit and a feasible solution is present.
         */
        MIP_GAP,
    }
}

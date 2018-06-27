package net.lintim.solver;

import net.lintim.exception.LinTimException;
import net.lintim.io.LPWriter;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class containing a generic optimization model. Use an instance of this class to build a model and then solve it using
 * {@link Solver#getSolver(Model, SolverType)} and {@link Solver#solveModel()}. The solution can then be queried using
 * the {@link Solver} class.
 */
public class Model {
    private static Logger logger = Logger.getLogger(Model.class.getCanonicalName());
    private HashMap<String, Variable> variables;
    private HashMap<String, Constraint> constraints;
    private HashMap<Variable, Double> startValues;
    private LinearExpression objective;
    private OptimizationSense sense;

    /**
     * Create a new empty model. The default optimization sense is {@link OptimizationSense#MINIMIZE}.
     */
    public Model() {
        this.variables = new HashMap<>();
        this.constraints = new HashMap<>();
        this.objective = new LinearExpression();
        this.startValues = new HashMap<>();
        this.sense = OptimizationSense.MINIMIZE;
    }

    /**
     * Create a new variable and add it to the model.
     * @param name the name of the variable. The variable will obtain a unique name, i.e., the name will be replaced
     *             if it is not unique in the model.
     * @param type the type of the variable
     * @param lowerBound the lower bound of the variable
     * @param upperBound the upper bound of the variable
     * @param objective the objective of the variable
     * @return the newly created variable
     */
    public Variable addVariable(String name, Variable.VariableType type, double lowerBound, double upperBound,
                                double objective){
        name = findNextName(name, variables.keySet());
        Variable newVar = new Variable(name, type, lowerBound, upperBound);
        variables.put(name, newVar);
        if(objective != 0){
            this.objective.addTerm(objective, newVar);
        }
        return newVar;
    }

    /**
     * Create a new constraint and add it to the model.
     * @param expression the expression of the constraint. This should be the left hand side.
     * @param sense the constraint sense.
     * @param rhs the right hand side of the constraint
     * @param name the name of the constraint. The constraint will obtain a unique name, i.e., the name will be replaced
     *             if it is not unique in the model.
     * @return the newly created constraint
     */
    public Constraint addConstraint(LinearExpression expression, Constraint.ConstraintSense sense, double rhs, String name){
        name = findNextName(name, constraints.keySet());
        Constraint newCtr = new Constraint(name, expression, sense, rhs);
        constraints.put(name, newCtr);
        return newCtr;
    }

    /**
     * Get the constraints of the model.
     * @return the constraints
     */
    public Collection<Constraint> getConstraints() {
        return constraints.values();
    }

    /**
     * Get the coefficient of the variable in the constraint.
     * @param constraint the constraint to query
     * @param variable the variable to look up
     * @return the coefficient of the given variable in the given constraint
     */
    public double getCoefficient(Constraint constraint, Variable variable){
        return constraint.getCoefficient(variable);
    }

    /**
     * Alter the given constraint.
     * @param constraint the constraint to change
     * @param variable the variable to change the coefficient of
     * @param newCoefficient the new coefficient
     */
    public void changeCoefficient(Constraint constraint, Variable variable, double newCoefficient){
        constraint.changeCoefficient(variable, newCoefficient);
    }

    /**
     * Get the objective of the model. For the optimization sense, see {@link #getSense()}.
     * @return the objective
     */
    public LinearExpression getObjective(){
        return objective;
    }

    /**
     * Get the expression of the given constraint
     * @param constraint the constraint to look up
     * @return the expression
     */
    public LinearExpression getRow(Constraint constraint){
        return constraint.getExpression();
    }

    /**
     * Get a variable by its name.
     * @param name the name of the variable
     * @return the variable with the given name
     */
    public Variable getVariableByName(String name){
        return variables.get(name);
    }

    /**
     * Get the variables of the model.
     * @return the variables
     */
    public Collection<Variable> getVariables(){
        return variables.values();
    }

    /**
     * Remove the given constraint from the model
     * @param constraint the constraint to remove
     */
    public void remove(Constraint constraint){
        constraints.remove(constraint.getName());
    }

    /**
     * Remove the given variable from the model. The variable will also be removed from the objective and all
     * constraints.
     * @param variable the variable to remove
     */
    public void remove(Variable variable){
        objective.remove(variable);
        variables.remove(variable.getName());
        for(Constraint constraint : constraints.values()){
            constraint.getExpression().remove(variable);
        }
    }

    /**
     * Set a starting value for the given variable.
     * @param variable the variable to set a starting value for
     * @param value the starting value to set
     */
    public void setStartValue(Variable variable, double value) {
        this.startValues.put(variable, value);
    }

    /**
     * Get the starting values for this model.
     * @return the starting values
     */
    public Map<Variable, Double> getStartValues() {
        return startValues;
    }

    /**
     * Set the given objective with the given sense for the model.
     * @param objective the objective to set
     * @param sense the sense of the objective
     */
    public void setObjective(LinearExpression objective, OptimizationSense sense){
        this.objective = objective;
        this.sense = sense;
    }

    /**
     * Write the model to the given file.
     * @param filename the file to write to.
     */
    public void write(String filename){
        LPWriter.writeProblem(this, filename);
    }

    private String findNextName(String name, Set<String> names){
        String newName = name;
        if(names.contains(newName)){
            int index = 0;
            while (names.contains(newName + "_" + index)){
                index++;
            }
            newName = name + "_" + index;
            logger.log(LogLevel.INFO, "Name " + name + " was already taken, changed to " + newName);
        }
        return newName;
    }

    /**
     * Set the optimization sense of this model.
     * @param sense the new sense
     */
    public void setSense(OptimizationSense sense) {
        this.sense = sense;
    }

    /**
     * Get the optimization sense of the model.
     * @return the sense
     */
    public OptimizationSense getSense() {
        return sense;
    }

    /**
     * Possible optimization senses. Can be set using {@link #setSense(OptimizationSense)} or
     * {@link #setObjective(LinearExpression, OptimizationSense)}.
     */
    public enum OptimizationSense {
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
    public enum IntAttribute {
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
        NUM_BIN_VARIABLES
    }

    /**
     * Get the value of an int attribute for the model. For a description of the possible attributes, see
     * {@link IntAttribute}.
     * @param attribute the attribute to query
     * @return the value of the attribute
     */
    public int getIntAttribute(IntAttribute attribute) {
        switch (attribute) {
            case NUM_VARIABLES:
                return variables.size();
            case NUM_CONSTRAINTS:
                return constraints.size();
            case NUM_BIN_VARIABLES:
                return (int) variables.values().stream().filter(var -> var.getType() == Variable.VariableType.BINARY)
                    .count();
            case NUM_INT_VARIABLES:
                return (int) variables.values().stream().filter(var -> var.getType() == Variable.VariableType.INTEGER)
                    .count();
            default:
                throw new LinTimException("Unknown int attribute");
        }
    }
}

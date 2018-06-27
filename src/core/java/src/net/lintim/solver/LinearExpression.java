package net.lintim.solver;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class representing a linear expression of variables.
 */
public class LinearExpression {
    private HashMap<Variable, Double> coefficientMap;

    /**
     * Create a new, empty linear expression.
     */
    public LinearExpression(){
        this.coefficientMap = new HashMap<>();
    }

    /**
     * Add the given linear expression to this expression.
     * @param otherExpression the expression to add.
     */
    public void add(LinearExpression otherExpression){
        multiAdd(1, otherExpression);
    }

    /**
     * Add a multiple of the given linear expression to this expression.
     * @param multiple the multiple
     * @param otherExpression the expression to add
     */
    public void multiAdd(double multiple, LinearExpression otherExpression){
        for(Map.Entry<Variable, Double> entry : otherExpression.getEntries()){
            addTerm(entry.getValue() * multiple, entry.getKey());
        }
    }

    /**
     * Add the given term to the expression
     * @param coefficient the coefficient of the variable
     * @param variable the variable
     */
    public void addTerm(double coefficient, Variable variable){
        Double currentCoefficient = coefficientMap.get(variable);
        double newCoefficient = currentCoefficient != null ? currentCoefficient + coefficient : coefficient;
        coefficientMap.put(variable, newCoefficient);
    }

    /**
     * Clear this expression.
     */
    public void clear(){
        this.coefficientMap.clear();
    }

    /**
     * Get the number of variables in the expression.
     * @return the number of variables in the expression
     */
    public int getSize(){
        return coefficientMap.size();
    }

    /**
     * Remove the given variable from the expression
     * @param variable the variable to remove
     * @return whether the variable was present in the expression
     */
    public boolean remove(Variable variable){
        return coefficientMap.remove(variable) != null;
    }

    /**
     * Get the coefficient of the given variable. Will be 0, if the variable is not present in the expression.
     * @param variable the variable to query
     * @return the coefficient of the variable or 0, if its not present
     */
    public double getCoefficient(Variable variable){
        Double coefficient = coefficientMap.get(variable);
        return coefficient != null ? coefficient : 0;
    }

    /**
     * Get the entries of the expression.
     * @return the entries
     */
    public Set<Map.Entry<Variable, Double>> getEntries(){
        return coefficientMap.entrySet().stream().filter(e -> e.getValue() > 0).collect(Collectors.toSet());
    }

    /**
     * Change the coefficient of the given variable
     * @param variable the variable to change the coefficient for
     * @param newCoefficient the new coefficient
     */
    public void changeCoefficient(Variable variable, double newCoefficient){
        coefficientMap.put(variable, newCoefficient);
    }
}

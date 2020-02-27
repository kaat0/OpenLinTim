package net.lintim.solver;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class representing a linear expression of variables.
 */
public interface LinearExpression {

    /**
     * Add the given linear expression to this expression.
     * @param otherExpression the expression to add.
     */
    public void add(LinearExpression otherExpression);

    /**
     * Add a multiple of the given linear expression to this expression.
     * @param multiple the multiple
     * @param otherExpression the expression to add
     */
    public void multiAdd(double multiple, LinearExpression otherExpression);

    /**
     * Add the given term to the expression
     * @param coefficient the coefficient of the variable
     * @param variable the variable
     */
    public void addTerm(double coefficient, Variable variable);

    /**
     * Clear this expression.
     */
    public void clear();
}

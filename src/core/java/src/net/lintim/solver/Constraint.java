package net.lintim.solver;

/**
 * Class containing the model for a generic constraint. Can be included in a {@link Model}. Use
 * {@link Model#addConstraint(LinearExpression, ConstraintSense, double, String)} for the creation of a constraint.
 */
public class Constraint {
    private String name;
    private LinearExpression expression;
    private ConstraintSense sense;
    private double rhs;

    Constraint(String name, LinearExpression expression, ConstraintSense sense, double rhs) {
        this.name = name;
        this.expression = expression;
        this.sense = sense;
        this.rhs = rhs;
    }

    /**
     * Get the coefficient for the given variable.
     * @param variable the variable to query
     * @return the coefficient of the given variable
     */
    public double getCoefficient(Variable variable){
        return expression.getCoefficient(variable);
    }

    /**
     * Change the coefficient of the given variable to the new value.
     * @param variable the variable to change the coefficient for
     * @param newCoefficient the new coefficient
     */
    public void changeCoefficient(Variable variable, double newCoefficient){
        expression.changeCoefficient(variable, newCoefficient);
    }

    /**
     * Get the expression, i.e., the left hand side of the constraint.
     * @return the expression
     */
    public LinearExpression getExpression(){
        return expression;
    }

    /**
     * Get the name of the constraint.
     * @return the name of the constraint
     */
    public String getName() {
        return name;
    }

    /**
     * Get the sense of the constraint. For a list of possible senses, see {@link ConstraintSense}.
     * @return the sense of the constraint
     */
    public ConstraintSense getSense() {
        return sense;
    }

    /**
     * Get the right hand side of the constraint.
     * @return the right hand side
     */
    public double getRhs() {
        return rhs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Constraint that = (Constraint) o;

        if (Double.compare(that.rhs, rhs) != 0) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (getExpression() != null ? !getExpression().equals(that.getExpression()) : that.getExpression() != null)
            return false;
        return sense == that.sense;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /**
     * Possible senses for a constraint, i.e., whether the expression should be less or equal, equal, or greater or
     * equal than the right hand side.
     */
    public enum ConstraintSense {
        LESS_EQUAL, EQUAL, GREATER_EQUAL
    }
}

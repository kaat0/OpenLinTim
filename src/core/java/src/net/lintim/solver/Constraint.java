package net.lintim.solver;

/**
 * Class containing the model for a generic constraint. Can be included in a {@link Model}. Use
 * {@link Model#addConstraint(LinearExpression, ConstraintSense, double, String)} for the creation of a constraint.
 */
public interface Constraint {

    /**
     * Get the name of the constraint.
     * @return the name of the constraint
     */
    public String getName();

    /**
     * Get the sense of the constraint. For a list of possible senses, see {@link ConstraintSense}.
     * @return the sense of the constraint
     */
    public ConstraintSense getSense();

    /**
     * Get the right hand side of the constraint.
     * @return the right hand side
     */
    public double getRhs();

    /**
     * Possible senses for a constraint, i.e., whether the expression should be less or equal, equal, or greater or
     * equal than the right hand side.
     */
    public enum ConstraintSense {
        LESS_EQUAL, EQUAL, GREATER_EQUAL
    }
}

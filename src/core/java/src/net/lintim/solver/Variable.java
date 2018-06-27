package net.lintim.solver;

/**
 * Class implementing a generic variable. Create a variable by calling
 * {@link Model#addVariable(String, VariableType, double, double, double)}.
 */
public class Variable {
    private String name;
    private VariableType type;
    private double lowerBound;
    private double upperBound;

    Variable(String name, VariableType type, double lowerBound, double upperBound){
        this.name = name;
        this.type = type;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Get the name of the variable.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of the variable. For the possibilities, see {@link VariableType}.
     * @return the type of the variable
     */
    public VariableType getType() {
        return type;
    }

    /**
     * Get the lower bound of the variable.
     * @return the lower bound
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Get the upper bound of the variable.
     * @return the upper bound
     */
    public double getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable) o;

        if (Double.compare(variable.lowerBound, lowerBound) != 0) return false;
        if (Double.compare(variable.upperBound, upperBound) != 0) return false;
        if (name != null ? !name.equals(variable.name) : variable.name != null) return false;
        return type == variable.type;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        temp = Double.doubleToLongBits(lowerBound);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(upperBound);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Possible variable types.
     */
    public enum VariableType{
        INTEGER, CONTINOUS, BINARY
    }
}

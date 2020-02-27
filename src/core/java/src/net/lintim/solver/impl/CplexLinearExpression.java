package net.lintim.solver.impl;

import ilog.concert.*;
import net.lintim.exception.SolverCplexException;
import net.lintim.solver.LinearExpression;
import net.lintim.solver.Variable;

import java.util.ArrayList;
import java.util.List;

public class CplexLinearExpression implements LinearExpression {

    private IloLinearNumExpr expr;

    CplexLinearExpression(IloLinearNumExpr expr) {
        this.expr = expr;
    }

    @Override
    public void add(LinearExpression otherExpression) {
        GurobiModel.throwForNonGurobiExpression(otherExpression);
        IloLinearNumExpr other = ((CplexLinearExpression) otherExpression).expr;
        try {
            expr.add(other);
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public void multiAdd(double multiple, LinearExpression otherExpression) {
        CplexModel.throwForNonCplexExpression(otherExpression);
        IloLinearNumExpr other = ((CplexLinearExpression) otherExpression).expr;
        try {
            List<Double> multCoefficients = new ArrayList<>();
            List<IloNumVar> variables = new ArrayList<>();
            IloLinearNumExprIterator iterator = other.linearIterator();
            while (iterator.hasNext()) {
                variables.add(iterator.nextNumVar());
                multCoefficients.add(multiple * iterator.getValue());
            }
            double[] coefficientArray = new double[multCoefficients.size()];
            for (int index = 0; index < coefficientArray.length; index++) {
                coefficientArray[index] = multCoefficients.get(index);
            }
            IloNumVar[] variableArray = variables.toArray(new IloNumVar[0]);
            expr.addTerms(coefficientArray, variableArray);
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public void addTerm(double coefficient, Variable variable) {
        GurobiModel.throwForNonGurobiVariable(variable);
        IloNumVar cplexVariable = ((CplexVariable) variable).getVar();
        try {
            expr.addTerm(coefficient, cplexVariable);
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public void clear() {
        try {
            expr.clear();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    IloLinearNumExpr getExpr() {
        return expr;
    }
}

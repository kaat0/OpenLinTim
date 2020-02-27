package net.lintim.solver.impl;

import gurobi.GRBException;
import gurobi.GRBLinExpr;
import net.lintim.exception.LinTimException;
import net.lintim.exception.SolverGurobiException;
import net.lintim.solver.LinearExpression;
import net.lintim.solver.Variable;

public class GurobiLinearExpression implements LinearExpression {

    private GRBLinExpr expr;

    GurobiLinearExpression(GRBLinExpr expr){
        this.expr = expr;
    }

    @Override
    public void add(LinearExpression otherExpression) {
        GurobiModel.throwForNonGurobiExpression(otherExpression);
        GRBLinExpr other = ((GurobiLinearExpression) otherExpression).expr;
        try {
            this.expr.add(other);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void multiAdd(double multiple, LinearExpression otherExpression) {
        GurobiModel.throwForNonGurobiExpression(otherExpression);
        GRBLinExpr other = ((GurobiLinearExpression) otherExpression).expr;
        try {
            this.expr.multAdd(multiple, other);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void addTerm(double coefficient, Variable variable) {
        GurobiModel.throwForNonGurobiVariable(variable);
        this.expr.addTerm(coefficient, ((GurobiVariable) variable).getGRBVar());
    }

    @Override
    public void clear() {
        this.expr.clear();
    }

    GRBLinExpr getExpr() {
        return expr;
    }
}

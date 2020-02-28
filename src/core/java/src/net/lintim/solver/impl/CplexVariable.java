package net.lintim.solver.impl;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import net.lintim.exception.SolverCplexException;
import net.lintim.solver.Variable;

public class CplexVariable implements Variable {

    private IloNumVar var;

    CplexVariable(IloNumVar var) {
        this.var = var;
    }

    @Override
    public String getName() {
        return var.getName();
    }

    @Override
    public VariableType getType() {
        IloNumVarType type;
        try {
            type = var.getType();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
        if (type == IloNumVarType.Bool) {
            return VariableType.BINARY;
        }
        if (type == IloNumVarType.Float) {
            return VariableType.CONTINOUS;
        }
        if (type == IloNumVarType.Int) {
            return VariableType.INTEGER;
        }
        else {
            throw new SolverCplexException("Unknown cplex variable type " + type);
        }
    }

    @Override
    public double getLowerBound() {
        try {
            return var.getLB();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public double getUpperBound() {
        try {
            return var.getUB();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    IloNumVar getVar() {
        return var;
    }
}

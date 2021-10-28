package net.lintim.solver.impl;

import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBException;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import net.lintim.exception.SolverAttributeNotImplementedException;
import net.lintim.exception.SolverCplexException;
import net.lintim.exception.SolverGurobiException;
import net.lintim.solver.Constraint;
import net.lintim.util.SolverType;

public class CplexConstraint implements Constraint {

    private IloRange constr;

    CplexConstraint(IloRange constr) {
        this.constr = constr;
    }

    @Override
    public String getName() {
        return constr.getName();
    }

    @Override
    public ConstraintSense getSense() {
        try {
            if (constr.getLB() == constr.getUB()) {
                return ConstraintSense.EQUAL;
            }
            if (constr.getLB() == Double.NEGATIVE_INFINITY) {
                return ConstraintSense.LESS_EQUAL;
            }
            return ConstraintSense.GREATER_EQUAL;
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public double getRhs() {
        try {
            if (getSense() == ConstraintSense.GREATER_EQUAL) {
                return constr.getLB();
            }
            return constr.getUB();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public void setRhs(double value) {
        try {
            if(getSense() == ConstraintSense.GREATER_EQUAL) {
                constr.setLB(value);
            }
            else {
                constr.setUB(value);
            }
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }

    }
}

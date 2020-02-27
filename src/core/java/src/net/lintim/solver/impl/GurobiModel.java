package net.lintim.solver.impl;

import gurobi.*;
import net.lintim.exception.*;
import net.lintim.solver.Constraint;
import net.lintim.solver.LinearExpression;
import net.lintim.solver.Model;
import net.lintim.solver.Variable;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

public class GurobiModel implements Model {

    private GRBModel model;

    GurobiModel(GRBModel model) {
        this.model = model;
    }

    @Override
    public Variable addVariable(double lowerBound, double upperBound, Variable.VariableType type, double objective, String name) {
        char vType;
        switch (type) {
            case BINARY:
                vType = GRB.BINARY;
                break;
            case INTEGER:
                vType = GRB.INTEGER;
                break;
            case CONTINOUS:
                vType = GRB.CONTINUOUS;
                break;
            default:
                throw new SolverVariableTypeNotImplementedException(SolverType.GUROBI, type);
        }
        try {
            return new GurobiVariable(model.addVar(lowerBound, upperBound, objective, vType, name));
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public Constraint addConstraint(LinearExpression expression, Constraint.ConstraintSense sense, double rhs, String name) {
        char cSense;
        switch (sense) {
            case LESS_EQUAL:
                cSense = GRB.LESS_EQUAL;
                break;
            case EQUAL:
                cSense = GRB.EQUAL;
                break;
            case GREATER_EQUAL:
                cSense = GRB.GREATER_EQUAL;
                break;
            default:
                throw new LinTimException("Unknown constraint sense " + sense);
        }
        throwForNonGurobiExpression(expression);
        try {
            return new GurobiConstraint(model.addConstr(((GurobiLinearExpression) expression).getExpr(), cSense, rhs, name));
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public Variable getVariableByName(String name) {
        try {
            return new GurobiVariable(model.getVarByName(name));
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void setStartValue(Variable variable, double value) {
        throwForNonGurobiVariable(variable);
        try {
            ((GurobiVariable) variable).getGRBVar().set(GRB.DoubleAttr.Start, value);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void setObjective(LinearExpression objective, OptimizationSense sense) {
        throwForNonGurobiExpression(objective);
        int oSense = getGurobiSense(sense);
        try {
            model.setObjective(((GurobiLinearExpression) objective).getExpr(), oSense);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public LinearExpression getObjective() {
        try {
            GRBExpr objective = model.getObjective();
            if (!(objective instanceof GRBLinExpr)) {
                throw new SolverGurobiException("We only support linear objectives!");
            }
            return new GurobiLinearExpression((GRBLinExpr) objective);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public LinearExpression createExpression() {
        return new GurobiLinearExpression(new GRBLinExpr());
    }

    @Override
    public void write(String filename) {
        try {
            model.write(filename);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void setSense(OptimizationSense sense) {
        int oSense = getGurobiSense(sense);
        try {
            model.set(GRB.IntAttr.ModelSense, oSense);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public OptimizationSense getSense() {
        try {
            if (model.get(GRB.IntAttr.ModelSense) == GRB.MAXIMIZE) {
                return OptimizationSense.MAXIMIZE;
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
        return OptimizationSense.MINIMIZE;
    }

    private static int getGurobiSense(OptimizationSense sense) {
        int oSense;
        switch (sense) {
            case MAXIMIZE:
                oSense = GRB.MAXIMIZE;
                break;
            case MINIMIZE:
                oSense = GRB.MINIMIZE;
                break;
            default:
                throw new LinTimException("Unknown optimization sense " + sense);
        }
        return oSense;
    }

    @Override
    public int getIntAttribute(IntAttribute attribute) {
        try {
            model.update();
            switch (attribute) {
                case NUM_VARIABLES:
                    return model.get(GRB.IntAttr.NumVars);
                case NUM_CONSTRAINTS:
                    return model.get(GRB.IntAttr.NumConstrs);
                case NUM_BIN_VARIABLES:
                    return model.get(GRB.IntAttr.NumBinVars);
                case NUM_INT_VARIABLES:
                    return model.get(GRB.IntAttr.NumIntVars);
                default:
                    throw new SolverAttributeNotImplementedException(SolverType.GUROBI, attribute.name());
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public Status getStatus() {
        try {
            return transformGurobiStatus(model.get(GRB.IntAttr.Status));
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    private Status transformGurobiStatus(int gurobiStatus) throws GRBException {
        switch (gurobiStatus) {
            case GRB.OPTIMAL:
                return Status.OPTIMAL;
            case GRB.INFEASIBLE:
                return Status.INFEASIBLE;
            case GRB.TIME_LIMIT:
                if (model.get(GRB.IntAttr.SolCount) > 0) {
                    return Status.FEASIBLE;
                }
                else {
                    return Status.INFEASIBLE;
                }
            default:
                throw new SolverGurobiException("Got unsupported status code from gurobi solver");
        }
    }

    @Override
    public void solve() {
        try {
            model.optimize();
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void computeIIS(String fileName) {
        try {
            model.computeIIS();
            model.write(fileName);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public double getValue(Variable variable) {
        throwForNonGurobiVariable(variable);
        try {
            return ((GurobiVariable) variable).getGRBVar().get(GRB.DoubleAttr.X);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public double getDoubleAttribute(DoubleAttribute attribute) {
        try {
            switch (attribute) {
                case MIP_GAP:
                    return model.get(GRB.DoubleAttr.MIPGap);
                case OBJ_VAL:
                    return model.get(GRB.DoubleAttr.ObjVal);
                default:
                    throw new SolverAttributeNotImplementedException(SolverType.GUROBI, attribute.name());
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void setIntParam(IntParam param, int value) {
        try {
            switch (param) {
                case TIMELIMIT:
                    if (value >= 0) {
                        model.set(GRB.DoubleParam.TimeLimit, value);
                    }
                    break;
                case OUTPUT_LEVEL:
                    if (value == LogLevel.DEBUG.intValue()) {
                        model.set(GRB.IntParam.LogToConsole, 1);
                        model.set(GRB.StringParam.LogFile, "SolverGurobi.log");
                    }
                    else {
                        model.set(GRB.IntParam.OutputFlag, 0);
                    }
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.GUROBI, param.name());
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void setDoubleParam(DoubleParam param, double value) {
        try {
            if (param == DoubleParam.MIP_GAP) {
                model.set(GRB.DoubleParam.MIPGap, value);
            } else {
                throw new SolverParamNotImplementedException(SolverType.GUROBI, param.name());
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    static void throwForNonGurobiVariable(Variable var) {
        if(!(var instanceof GurobiVariable)) {
            throw new LinTimException("Try to work with non gurobi variable in gurobi context");
        }
    }

    static void throwForNonGurobiExpression(LinearExpression expr) {
        if(!(expr instanceof GurobiLinearExpression)) {
            throw new LinTimException("Try to work with non gurobi expression in gurobi context");
        }
    }
}

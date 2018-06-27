package net.lintim.solver.impl;

import gurobi.*;
import net.lintim.exception.*;
import net.lintim.solver.*;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.Map;

/**
 * Implementation of the abstract {@link Solver} class for gurobi. Use {@link Solver#getSolver(Model, SolverType)}
 * with type {@link SolverType#GUROBI} to obtain an instance of the class. Note that this file will only be compiled,
 * if gurobi is present on the system CLASSPATH.
 */
public class GurobiSolver extends Solver {
    private GRBModel grbModel;

    public GurobiSolver(Model model) {
        super(model);
    }

    @Override
    public void transformModel(){
        try {
            GRBEnv env = new GRBEnv();
            grbModel = new GRBModel(env);
            setParams();
            setVariables();
            setConstraints();
            setStartValues();
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void solveModel() {
        try {
            if (grbModel == null) {
                transformModel();
            }
            grbModel.optimize();
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    private void setVariables() throws GRBException {
        for(Variable variable : model.getVariables()) {
            char type;
            switch (variable.getType()) {
                case BINARY:
                    type = GRB.BINARY;
                    break;
                case INTEGER:
                    type = GRB.INTEGER;
                    break;
                case CONTINOUS:
                    type = GRB.CONTINUOUS;
                    break;
                default:
                    throw new SolverVariableTypeNotImplementedException(SolverType.GUROBI, variable.getType());
            }
            grbModel.addVar(variable.getLowerBound(), variable.getUpperBound(), model.getObjective().getCoefficient
                (variable), type, variable.getName());
        }
        grbModel.update();
    }

    private void setConstraints() throws GRBException {
        for (Constraint constraint : model.getConstraints()) {
            char sense;
            switch (constraint.getSense()) {
                case LESS_EQUAL:
                    sense = GRB.LESS_EQUAL;
                    break;
                case EQUAL:
                    sense = GRB.EQUAL;
                    break;
                case GREATER_EQUAL:
                    sense = GRB.GREATER_EQUAL;
                    break;
                default:
                    throw new LinTimException("Searched for constraint sense, found " + constraint.getSense().name()
                        + ". What?");
            }
            grbModel.addConstr(transform(constraint.getExpression()), sense, constraint.getRhs(), constraint.getName());

        }
    }

    private GRBLinExpr transform(LinearExpression expression) throws GRBException {
        GRBLinExpr result = new GRBLinExpr();
        for (Map.Entry<Variable, Double> entry: expression.getEntries()) {
            result.addTerm(entry.getValue(), grbModel.getVarByName(entry.getKey().getName()));
        }
        return result;
    }

    private void setParams() throws GRBException {
        int modelSense;
        if(model.getSense() == Model.OptimizationSense.MINIMIZE) {
            modelSense = GRB.MINIMIZE;
        }
        else {
            modelSense = GRB.MAXIMIZE;
        }
        grbModel.set(GRB.IntAttr.ModelSense, modelSense);
        for (Map.Entry<DoubleParam, Double> paramEntry : this.doubleParams.entrySet()) {
            switch (paramEntry.getKey()) {
                case MIP_GAP:
                    grbModel.set(GRB.DoubleParam.MIPGap, paramEntry.getValue());
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.GUROBI, paramEntry.getKey().name());
            }
        }
        for (Map.Entry<IntParam, Integer> paramEntry : this.intParams.entrySet()) {
            switch (paramEntry.getKey()) {
                case TIMELIMIT:
                    int timeLimit = paramEntry.getValue();
                    if (timeLimit < 0) {
                        break;
                    }
                    grbModel.set(GRB.DoubleParam.TimeLimit, timeLimit);
                    break;
                case OUTPUT_LEVEL:
                    if (paramEntry.getValue() == LogLevel.DEBUG.intValue()) {
                        grbModel.set(GRB.IntParam.LogToConsole, 1);
                        grbModel.set(GRB.StringParam.LogFile, "SolverGurobi.log");
                    }
                    else {
                        grbModel.set(GRB.IntParam.OutputFlag, 0);
                    }
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.GUROBI, paramEntry.getKey().name());
            }
        }
    }

    @Override
    public void computeIIS(String fileName) {
        try {
            if(grbModel.get(GRB.IntAttr.Status) != GRB.INFEASIBLE) {
                throw new SolverInvalidCallException("Cannot compute IIS on feasible grbModel!");
            }
            grbModel.computeIIS();
            grbModel.write(fileName + ".ilp");
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getValue(Variable variable) {
        try {
            if(grbModel.get(GRB.IntAttr.SolCount) == 0) {
                throw new SolverInvalidCallException("Can only read variables for feasible models!");
            }
            return grbModel.getVarByName(variable.getName()).get(GRB.DoubleAttr.X);
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public double getDoubleAttribute(DoubleAttribute attribute) {
        try{
            switch (attribute) {
                case MIP_GAP:
                    return grbModel.get(GRB.DoubleAttr.MIPGap);
                case OBJ_VAL:
                    return grbModel.get(GRB.DoubleAttr.ObjVal);
                default:
                    throw new SolverAttributeNotImplementedException(SolverType.GUROBI, attribute.name());
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public int getIntAttribute(IntAttribute attribute) {
        try{
            switch (attribute) {
                case STATUS:
                    switch (grbModel.get(GRB.IntAttr.Status)) {
                        case GRB.OPTIMAL:
                            return OPTIMAL;
                        case GRB.INFEASIBLE:
                            return INFEASIBLE;
                        case GRB.TIME_LIMIT:
                            if (grbModel.get(GRB.IntAttr.SolCount) > 0) {
                                return FEASIBLE;
                            }
                            return TIMELIMIT;
                        default:
                            throw new LinTimException("Unknown grbModel status " + grbModel.get(GRB.IntAttr.Status));
                    }
                default:
                    throw new SolverAttributeNotImplementedException(SolverType.GUROBI, attribute.name());
            }
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    @Override
    public void writeSolverSpecificLpFile(String lpFileName) {
        try {
            grbModel.write(lpFileName + ".lp");
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }

    private void setStartValues() throws GRBException {
        Map<Variable, Double> startValues = model.getStartValues();
        // First fill the arrays for gurobi
        GRBVar[] variables = new GRBVar[startValues.size()];
        double[] values = new double[startValues.size()];
        int index = 0;
        for(Map.Entry<Variable, Double> mapEntry : startValues.entrySet()) {
            variables[index] = grbModel.getVarByName(mapEntry.getKey().getName());
            values[index] = mapEntry.getValue();
            index += 1;
        }
        grbModel.set(GRB.DoubleAttr.Start, variables, values);
    }
}

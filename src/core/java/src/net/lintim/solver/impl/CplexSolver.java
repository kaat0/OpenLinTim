package net.lintim.solver.impl;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplexModeler;
import net.lintim.exception.*;
import net.lintim.solver.*;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.util.*;

/**
 * Implementation of the abstract {@link Solver} class for cplex. Use {@link Solver#getSolver(Model, SolverType)}
 * with type {@link SolverType#CPLEX} to obtain an instance of the class. Note that this file will only be compiled,
 * if cplex is present on the system CLASSPATH.
 */
public class CplexSolver extends Solver {
    private IloCplex cplexModel;
    private IloCplexModeler modeler;
    private HashMap<String, IloNumVar> variablesByName;

    public CplexSolver(Model model) {
        super(model);
        this.modeler = new IloCplexModeler();
        variablesByName = new HashMap<>();
    }

    @Override
    public void transformModel() {
        try {
            cplexModel = new IloCplex();
            setParams();
            setVariables();
            setObjective();
            setConstraints();
            setStartValues();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public void solveModel() {
        try {
            if (cplexModel == null) {
                transformModel();
            }
            cplexModel.solve();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }

    }

    private void setParams() throws IloException {
        for (Map.Entry<DoubleParam, Double> paramEntry : this.doubleParams.entrySet()) {
            switch (paramEntry.getKey()) {
                case MIP_GAP:
                    cplexModel.setParam(IloCplex.DoubleParam.EpGap, paramEntry.getValue());
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.XPRESS, paramEntry.getKey().name());
            }
        }
        for (Map.Entry<IntParam, Integer> paramEntry : this.intParams.entrySet()) {
            switch (paramEntry.getKey()) {
                case TIMELIMIT:
                    int timeLimit = paramEntry.getValue();
                    if (timeLimit < 0) {
                        break;
                    }
                    cplexModel.setParam(IloCplex.DoubleParam.TiLim, timeLimit);
                    break;
                case OUTPUT_LEVEL:
                    if (paramEntry.getValue() == LogLevel.DEBUG.intValue()) {
                        cplexModel.setOut(null);
                    }
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.XPRESS, paramEntry.getKey().name());
            }
        }
    }

    private void setVariables() throws IloException {
        for (Variable modelVariable : model.getVariables()) {
            IloNumVarType type;
            switch (modelVariable.getType()) {
                case CONTINOUS:
                    type = IloNumVarType.Float;
                    break;
                case INTEGER:
                    type = IloNumVarType.Int;
                    break;
                case BINARY:
                    type = IloNumVarType.Bool;
                    break;
                default:
                    throw new SolverVariableTypeNotImplementedException(SolverType.CPLEX, modelVariable.getType());
            }
            IloNumVar variable = modeler.numVar(modelVariable.getLowerBound(), modelVariable.getUpperBound(), type,
                modelVariable.getName());
            variablesByName.put(modelVariable.getName(), variable);
            cplexModel.add(variable);
        }
    }

    private IloLinearNumExpr transform(LinearExpression expression) throws IloException {
        IloLinearNumExpr result = modeler.linearNumExpr();
        for (Map.Entry<Variable, Double> entry: expression.getEntries()) {
            result.addTerm(entry.getValue(), variablesByName.get(entry.getKey().getName()));
        }
        return result;
    }

    private void setObjective() throws IloException {
        IloLinearNumExpr expression = transform(model.getObjective());
        // set opt sense
        IloObjective objective;
        if (model.getSense() == Model.OptimizationSense.MINIMIZE) {
            objective = modeler.minimize(expression);
        }
        else {
            objective = modeler.maximize(expression);
        }
        cplexModel.add(objective);
    }

    private void setConstraints() throws IloException {
        for (Constraint constraint : model.getConstraints()) {
            IloLinearNumExpr expr = transform(constraint.getExpression());
            IloRange range;
            switch (constraint.getSense()) {
                case GREATER_EQUAL:
                    range = modeler.le(constraint.getRhs(), expr, constraint.getName());
                    break;
                case LESS_EQUAL:
                    range = modeler.ge(constraint.getRhs(), expr, constraint.getName());
                    break;
                case EQUAL:
                    range = modeler.eq(constraint.getRhs(), expr, constraint.getName());
                    break;
                default:
                    throw new LinTimException("Searched for constraint sense, found " + constraint.getSense().name()
                        + ". What?");
            }
            cplexModel.add(range);
        }
    }

    private void setStartValues() throws IloException {
        Map<Variable, Double> startValues = model.getStartValues();
        IloNumVar[] variables = new IloNumVar[startValues.size()];
        double[] values = new double[startValues.size()];
        int index = 0;
        for(Map.Entry<Variable, Double> mapEntry : startValues.entrySet()) {
            variables[index] = variablesByName.get(mapEntry.getKey().getName());
            values[index] = mapEntry.getValue();
            index += 1;
        }
        cplexModel.addMIPStart(variables, values);
    }

    @Override
    public void computeIIS(String fileName) {
        try {
            List<IloConstraint> constraintList = new ArrayList<>(cplexModel.getNrows());
            Iterator iterator = cplexModel.rangeIterator();
            while (iterator.hasNext()) {
                IloConstraint constraint = (IloConstraint) iterator.next();
                constraintList.add(constraint);
            }
            IloConstraint[] constraints = constraintList.toArray(new IloConstraint[cplexModel.getNrows()]);
            double[] prefs = new double[cplexModel.getNrows()];
            Arrays.fill(prefs, 1);
            cplexModel.refineConflict(constraints, prefs);
            cplexModel.writeConflict(fileName + ".ilp");
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public double getValue(Variable variable) {
        try {
            if (!(cplexModel.getStatus() == IloCplex.Status.Optimal || cplexModel.getStatus() == IloCplex.Status
                .Feasible)) {
                throw new SolverInvalidCallException("Can only read variables for feasible models!");
            }
            return cplexModel.getValue(variablesByName.get(variable.getName()));
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public double getDoubleAttribute(DoubleAttribute attribute) {
        try {
            switch (attribute) {
                case OBJ_VAL:
                    return cplexModel.getObjValue();
                case MIP_GAP:
                    return cplexModel.getMIPRelativeGap();
                default:
                    throw new SolverAttributeNotImplementedException(SolverType.CPLEX, attribute.name());
            }
        }
        catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public int getIntAttribute(IntAttribute attribute) {
        try {
            switch (attribute) {
                case STATUS:
                    if (cplexModel.getStatus() == IloCplex.Status.Optimal) {
                        return OPTIMAL;
                    }
                    if (cplexModel.getStatus() == IloCplex.Status.Infeasible) {
                        return INFEASIBLE;
                    }
                    if (cplexModel.getStatus() == IloCplex.Status.Feasible) {
                        return FEASIBLE;
                    }
                    if (cplexModel.getStatus() == IloCplex.Status.Unknown) {
                        return TIMELIMIT;
                    }
                    else {
                        throw new LinTimException("Unknown cplexModel status " + cplexModel.getStatus());
                    }
                default:
                    throw new SolverAttributeNotImplementedException(SolverType.CPLEX, attribute.name());

            }
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public void writeSolverSpecificLpFile(String lpFileName) {
        try {
            cplexModel.exportModel(lpFileName + ".lp");
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }
}

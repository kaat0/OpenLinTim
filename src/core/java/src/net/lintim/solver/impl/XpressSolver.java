package net.lintim.solver.impl;

import com.dashoptimization.*;
import net.lintim.exception.*;
import net.lintim.solver.*;
import net.lintim.util.LogLevel;
import net.lintim.util.SolverType;

import java.io.IOException;
import java.util.Map;

/**
 * Implementation of the abstract {@link Solver} class for xpress. Use {@link Solver#getSolver(Model, SolverType)}
 * with type {@link SolverType#XPRESS} to obtain an instance of the class. Note that this file will only be compiled,
 * if xpress is present on the system CLASSPATH.
 */
public class XpressSolver extends Solver {
    private XPRBprob xpressProblem;

    public XpressSolver(Model model) {
        super(model);
    }

    @Override
    public void transformModel() {
        XPRS.init();
        XPRB bcl = new XPRB();
        xpressProblem = bcl.newProb("");
        setParams();
        setVariablesAndObjective();
        setConstraints();
        setStartingSolution();
    }

    @Override
    public void solveModel() {
        if (xpressProblem == null) {
            transformModel();
        }
        xpressProblem.mipOptimise();
    }

    private void setParams(){
        if (model.getSense() == Model.OptimizationSense.MAXIMIZE) {
            xpressProblem.setSense(XPRB.MAXIM);
        }
        else {
            xpressProblem.setSense(XPRB.MINIM);
        }
        for (Map.Entry<DoubleParam, Double> paramEntry : this.doubleParams.entrySet()) {
            switch (paramEntry.getKey()) {
                case MIP_GAP:
                    xpressProblem.getXPRSprob().setDblControl(XPRS.MIPRELSTOP, paramEntry.getValue());
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.XPRESS, paramEntry.getKey().name());
            }
        }
        for (Map.Entry<IntParam, Integer> paramEntry : this.intParams.entrySet()) {
            switch (paramEntry.getKey()) {
                case TIMELIMIT:
                    int timelimit = paramEntry.getValue();
                    // Transform the timelimit such that xpress mimics our desired behavior. The xpress behavior is
                    // documented as follows:
                    // timelimit == 0 -> no timelimit. This should be the case for a negative LinTim timelimit
                    // timelimit > 0 -> Abort after timelimit, but only if a feasible solution was already found
                    //                  We do not want this behavior!
                    // timelimit < 0 -> Abort after timelimit, even if no feasible solution was found. This is our
                    //                  desired behavior
                    // For a LinTim timelimit of 0, we want to abort as early as possible. For this, we set the Xpress
                    // timelimit to -1.
                    if (timelimit < 0) {
                        timelimit = 0;
                    }
                    else if (timelimit > 0) {
                        timelimit *= -1;
                    }
                    else {
                        timelimit = -1;
                    }
                    xpressProblem.getXPRSprob().setIntControl(XPRS.MAXTIME, timelimit);
                    break;
                case OUTPUT_LEVEL:
                    if (paramEntry.getValue() == LogLevel.DEBUG.intValue()) {
                        xpressProblem.setMsgLevel(4);
                    }
                    else if (paramEntry.getValue() == LogLevel.INFO.intValue() || paramEntry.getValue() == LogLevel
                        .WARN.intValue()) {
                        xpressProblem.setMsgLevel(2);
                    }
                    else {
                        xpressProblem.setMsgLevel(0);
                    }
                    break;
                default:
                    throw new SolverParamNotImplementedException(SolverType.XPRESS, paramEntry.getKey().name());
            }
        }
    }

    private void setVariablesAndObjective() {
        XPRBexpr xpressObjective = new XPRBexpr();
        LinearExpression modelObjective = model.getObjective();
        for (Variable modelvar: model.getVariables()) {
            int type;
            switch (modelvar.getType()) {
                case BINARY:
                    type = XPRB.BV;
                    break;
                case INTEGER:
                    type = XPRB.UI;
                    break;
                case CONTINOUS:
                    type = XPRB.PL;
                    break;
                default:
                    throw new SolverVariableTypeNotImplementedException(SolverType.XPRESS, modelvar.getType());
            }
            XPRBvar xpressVar = xpressProblem.newVar(modelvar.getName(), type, modelvar.getLowerBound(), modelvar
                .getUpperBound());
            xpressObjective.addTerm(modelObjective.getCoefficient(modelvar), xpressVar);
        }
        xpressProblem.setObj(xpressObjective);
    }

    private void setConstraints() {
        for (Constraint modelConstraint : model.getConstraints()) {
            XPRBexpr xpressExpression = transform(modelConstraint.getExpression());
            XPRBrelation xpressConstraint;
            switch (modelConstraint.getSense()) {
                case EQUAL:
                    xpressConstraint = xpressExpression.eql(modelConstraint.getRhs());
                    break;
                case LESS_EQUAL:
                    xpressConstraint = xpressExpression.lEql(modelConstraint.getRhs());
                    break;
                case GREATER_EQUAL:
                    xpressConstraint = xpressExpression.gEql(modelConstraint.getRhs());
                    break;
                default:
                    throw new LinTimException("Searched for constraint sense, found " + modelConstraint.getSense().name()
                        + ". What?");
            }
            xpressProblem.newCtr(modelConstraint.getName(), xpressConstraint);
        }
    }

    private XPRBexpr transform(LinearExpression expression) {
        XPRBexpr result = new XPRBexpr();
        for(Map.Entry<Variable, Double> expressionEntry : expression.getEntries()) {
            result.addTerm(expressionEntry.getValue(), xpressProblem.getVarByName(expressionEntry.getKey().getName()));
        }
        return result;
    }

    @Override
    public void computeIIS(String fileName) {
        // Try to find the most simple iis
        xpressProblem.getXPRSprob().firstIIS(1);
        xpressProblem.getXPRSprob().writeIIS(0, fileName + ".ilp", 0);
    }

    @Override
    public double getValue(Variable variable) {
        if (!(xpressProblem.getMIPStat() == XPRB.MIP_OPTIMAL || xpressProblem.getMIPStat() == XPRB.MIP_SOLUTION)) {
            throw new SolverInvalidCallException("Can only read variables for feasible models!");
        }
        return xpressProblem.getVarByName(variable.getName()).getSol();
    }

    @Override
    public double getDoubleAttribute(DoubleAttribute attribute) {
        switch (attribute) {
            case MIP_GAP:
                double bestObjective = xpressProblem.getXPRSprob().getDblAttrib(XPRS.MIPBESTOBJVAL);
                double bestBound = xpressProblem.getXPRSprob().getDblAttrib(XPRS.BESTBOUND);
                return Math.abs((bestObjective - bestBound)/bestObjective);
            case OBJ_VAL:
                return xpressProblem.getObjVal();
            default:
                throw new SolverAttributeNotImplementedException(SolverType.XPRESS, attribute.name());
        }
    }

    @Override
    public int getIntAttribute(IntAttribute attribute) {
        switch (attribute) {
            case STATUS:
                switch (xpressProblem.getMIPStat()) {
                    case XPRB.MIP_SOLUTION:
                        return FEASIBLE;
                    case XPRB.MIP_NO_SOL_FOUND:
                        return TIMELIMIT;
                    case XPRB.MIP_OPTIMAL:
                        return OPTIMAL;
                    case XPRB.MIP_INFEAS:
                        return INFEASIBLE;
                    default:
                        throw new LinTimException("Unknown xpressModel status " + xpressProblem.getMIPStat());
                }
            default:
                throw new SolverAttributeNotImplementedException(SolverType.XPRESS, attribute.name());
        }
    }

    @Override
    public void writeSolverSpecificLpFile(String lpFileName) {
        try {
            xpressProblem.exportProb(lpFileName + ".lp");
        } catch (IOException e) {
            throw new LinTimException(e.getMessage());
        }
    }

    private void setStartingSolution() {
        if (model.getStartValues().size() == 0) {
            return;
        }
        XPRBsol startingSolution = xpressProblem.newSol();
        for(Map.Entry<Variable, Double> mapEntry : model.getStartValues().entrySet()) {
            startingSolution.setVar(xpressProblem.getVarByName(mapEntry.getKey().getName()), mapEntry.getValue());
        }
        xpressProblem.addMIPSol(startingSolution, "starting solution");
    }
}

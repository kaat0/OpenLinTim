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
 * Implementation of the abstract {@link Solver} class for cplex. Use {@link Solver#createModel()}
 * to obtain a model. Note that this file will only be compiled, if cplex is present on the system CLASSPATH.
 */
public class CplexSolver extends Solver {

    private IloCplex cplex;

    public CplexSolver() {
        try {
            cplex = new IloCplex();
        } catch (IloException e) {
            throw new SolverCplexException(e.getMessage());
        }
    }

    @Override
    public Model createModel() {
        return new CplexModel(cplex);
    }
}

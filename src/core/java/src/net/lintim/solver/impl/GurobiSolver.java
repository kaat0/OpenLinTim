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

    private GRBEnv env;

    public GurobiSolver() {
        try {
            this.env = new GRBEnv();
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }
    @Override
    public Model createModel() {
        try {
            return new GurobiModel(new GRBModel(env));
        } catch (GRBException e) {
            throw new SolverGurobiException(e.getMessage());
        }
    }
}

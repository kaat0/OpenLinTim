from enum import Enum


class SolverType(Enum):
    """
    Enum for used solver types in LinTim
    """
    XPRESS = "XPRESS"
    GUROBI = "GUROBI"
    CPLEX = "CPLEX"
    GLPK = "GLPK"

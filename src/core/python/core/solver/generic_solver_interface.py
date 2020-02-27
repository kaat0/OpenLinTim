import logging
import math
from abc import ABCMeta, abstractmethod
from enum import Enum
from pydoc import locate


class VariableType(Enum):
    CONTINOUS = "CONTINOUS"
    INTEGER = "INTEGER"
    BINARY = "BINARY"


class Variable(metaclass=ABCMeta):

    @abstractmethod
    def getName(self) -> str:
        raise NotImplementedError

    @abstractmethod
    def getType(self) -> VariableType:
        raise NotImplementedError

    @abstractmethod
    def getLowerBound(self) -> float:
        raise NotImplementedError

    @abstractmethod
    def getUpperBound(self) -> float:
        raise NotImplementedError


class LinearExpression(metaclass=ABCMeta):

    @abstractmethod
    def add(self, other: "LinearExpression"):
        raise NotImplementedError

    @abstractmethod
    def multiAdd(self, multiple: float, other: "LinearExpression"):
        raise NotImplementedError

    @abstractmethod
    def addTerm(self, coefficient: float, variable: Variable):
        raise NotImplementedError

    @abstractmethod
    def clear(self):
        raise NotImplementedError

    @abstractmethod
    def addConstant(self, value: float):
        raise NotImplementedError


class ConstraintSense(Enum):
    LESS_EQUAL = "LESS_EQUAL"
    EQUAL = "EQUAL"
    GREATER_EQUAL = "GREATER_EQUAL"


class Constraint(metaclass=ABCMeta):

    @abstractmethod
    def getName(self) -> str:
        raise NotImplementedError

    @abstractmethod
    def getSense(self) -> ConstraintSense:
        raise NotImplementedError

    @abstractmethod
    def getRhs(self) -> float:
        raise NotImplementedError


class OptimizationSense(Enum):
    MAXIMIZE = "MAXIMIZE"
    MINIMIZE = "MINIMIZE"


class IntAttribute(Enum):
    NUM_VARIABLES = "NUM_VARIABLES"
    NUM_CONSTRAINTS = "NUM_CONSTRAINTS"
    NUM_INT_VARIABLES = "NUM_INT_VARIABLES"
    NUM_BIN_VARIABLES = "NUM_BIN_VARIABLES"
    STATUS = "STATUS"
    RUNTIME = "RUNTIME"


class DoubleAttribute(Enum):
    OBJ_VAL = "OBJ_VAL"
    MIP_GAP = "MIP_GAP"


class DoubleParam(Enum):
    MIP_GAP = "MIP_GAP"


class IntParam(Enum):
    TIMELIMIT = "TIMELIMIT"
    OUTPUT_LEVEL = "OUTPUTLEVEL"
    THREADS = "THREADS"


class Status(Enum):
    OPTIMAL = "OPTIMAL"
    INFEASIBLE = "INFEASIBLE"
    FEASIBLE = "FEASIBLE"


class Model(metaclass=ABCMeta):

    @abstractmethod
    def addVariable(self, lower_bound: float = 0, upper_bound: float = math.inf,
                    var_type: VariableType = VariableType.CONTINOUS, objective: float = 0, name: str = "") -> Variable:
        raise NotImplementedError

    @abstractmethod
    def addConstraint(self, expression: LinearExpression, sense: ConstraintSense, rhs: float, name: str) -> Constraint:
        raise NotImplementedError

    @abstractmethod
    def getVariableByName(self, name: str) -> Variable:
        raise NotImplementedError

    @abstractmethod
    def setStartValue(self, variable: Variable, value: float):
        raise NotImplementedError

    @abstractmethod
    def setObjective(self, objective: LinearExpression, sense: OptimizationSense):
        raise NotImplementedError

    @abstractmethod
    def getObjective(self) -> LinearExpression:
        raise NotImplementedError

    @abstractmethod
    def createExpression(self) -> LinearExpression:
        raise NotImplementedError

    @abstractmethod
    def getSense(self) -> OptimizationSense:
        raise NotImplementedError

    @abstractmethod
    def write(self, filename: str):
        raise NotImplementedError

    @abstractmethod
    def setSense(self, sense: OptimizationSense):
        raise NotImplementedError

    @abstractmethod
    def getIntAttribute(self, attribute: IntAttribute) -> int:
        raise NotImplementedError

    @abstractmethod
    def getStatus(self) -> Status:
        raise NotImplementedError

    @abstractmethod
    def solve(self):
        raise NotImplementedError

    @abstractmethod
    def computeIIS(self, filename: str):
        raise NotImplementedError

    @abstractmethod
    def getValue(self, variable: Variable):
        raise NotImplementedError

    @abstractmethod
    def getDoubleAttribute(self, attribute: DoubleAttribute) -> float:
        raise NotImplementedError

    @abstractmethod
    def setIntParam(self, param: IntParam, value: int):
        raise NotImplementedError

    @abstractmethod
    def setDoubleParam(self, param: DoubleParam, value: float):
        raise NotImplementedError


class SolverType(Enum):
    """
    Enum for used solver types in LinTim
    """
    XPRESS = "XPRESS"
    GUROBI = "GUROBI"
    CPLEX = "CPLEX"
    GLPK = "GLPK"


def parseSolverType(solver_name: str) -> SolverType:
    if solver_name == "XPRESS":
        return SolverType.XPRESS
    elif solver_name == "GUROBI":
        return SolverType.GUROBI
    elif solver_name == "CPLEX":
        return SolverType.CPLEX
    elif solver_name == "GLPK":
        return SolverType.GLPK


class Solver(metaclass=ABCMeta):
    logger = logging.getLogger(__name__)

    @staticmethod
    def createSolver(solver_type: SolverType) -> "Solver":
        class_string = Solver.get_solver_name_from_type(solver_type)
        solver_class = locate(class_string)
        return solver_class()

    @staticmethod
    def get_solver_name_from_type(solver_type: SolverType):
        if solver_type == SolverType.GUROBI:
            return "core.solver.impl.gurobi_interface.GurobiSolver"
        elif solver_type == SolverType.CPLEX:
            return "core.solver.impl.cplex_interface.CplexSolver"
        elif solver_type == SolverType.XPRESS:
            return "core.solver.impl.xpress_interface.XpressSolver"

    @abstractmethod
    def createModel(self) -> Model:
        raise NotImplementedError

from core.exceptions.exceptions import LinTimException
from core.exceptions.solver_exceptions import SolverAttributeNotImplementedException, SolverParamNotImplementedException
from core.solver import generic_solver_interface
from core.solver.generic_solver_interface import Solver, Variable, VariableType, LinearExpression, Constraint, \
    ConstraintSense, DoubleParam, IntParam, DoubleAttribute, Status, IntAttribute, OptimizationSense, SolverType

from gurobipy import *


class GurobiVariable(Variable):

    def __init__(self, var: Var):
        self._var = var

    def getName(self) -> str:
        return self._var.varname

    def getType(self) -> VariableType:
        type = self._var.vtype
        if type == GRB.CONTINUOUS:
            return VariableType.CONTINOUS
        if type == GRB.BINARY:
            return VariableType.BINARY
        if type == GRB.INTEGER:
            return VariableType.INTEGER
        else:
            raise LinTimException(f"Unknown variable type {type}")

    def getLowerBound(self) -> float:
        return self._var.lb

    def getUpperBound(self) -> float:
        return self._var.up


class GurobiLinearExpression(LinearExpression):

    def __init__(self, expr: LinExpr):
        self._expr = expr

    def add(self, other: "LinearExpression"):
        raise_for_non_gurobi_expression(other)
        self._expr.add(other._expr)

    def multiAdd(self, multiple: float, other: "LinearExpression"):
        raise_for_non_gurobi_expression(other)
        self._expr.add(other._expr, multiple)

    def addTerm(self, coefficient: float, variable: Variable):
        raise_for_non_gurobi_variable(variable)
        self._expr.add(variable._var, coefficient)

    def clear(self):
        self._expr.clear()

    def addConstant(self, value: float):
        self._expr.addConstant(value)


class GurobiConstraint(Constraint):

    def __init__(self, constr: Constr):
        self._constr = constr

    def getName(self) -> str:
        return self._constr.constrname

    def getSense(self) -> ConstraintSense:
        sense = self._constr.sense
        if sense == GRB.GREATER_EQUAL:
            return ConstraintSense.GREATER_EQUAL
        if sense == GRB.LESS_EQUAL:
            return ConstraintSense.LESS_EQUAL
        if sense == GRB.EQUAL:
            return ConstraintSense.EQUAL
        raise Solver

    def getRhs(self) -> float:
        return self._constr.rhs


def raise_for_non_gurobi_variable(var: Variable):
    if not isinstance(var, GurobiVariable):
        raise LinTimException("Try to work with non gurobi variable in gurobi context")


def raise_for_non_gurobi_expression(expr: LinearExpression):
    if not isinstance(expr, GurobiLinearExpression):
        raise LinTimException("Try to work with non gurobi expression in gurobi context")


class GurobiModel(generic_solver_interface.Model):

    def __init__(self, model: Model):
        self._model = model

    def addVariable(self, lower_bound: float = 0, upper_bound: float = math.inf,
                    var_type: VariableType = VariableType.CONTINOUS, objective: float = 0, name: str = "") -> Variable:
        var_type = GurobiModel.transform_var_type(var_type)
        var = self._model.addVar(lower_bound, upper_bound, objective, var_type, name)
        return GurobiVariable(var)

    @staticmethod
    def transform_var_type(type: VariableType) -> str:
        if type == VariableType.CONTINOUS:
            return GRB.CONTINUOUS
        if type == VariableType.INTEGER:
            return GRB.INTEGER
        if type == VariableType.BINARY:
            return GRB.BINARY

    def addConstraint(self, expression: LinearExpression, sense: ConstraintSense, rhs: float, name: str) -> Constraint:
        con_sense = GurobiModel.transform_constraint_sense(sense)
        raise_for_non_gurobi_expression(expression)
        constr = self._model.addConstr(expression._expr, con_sense, rhs, name)
        return GurobiConstraint(constr)

    @staticmethod
    def transform_constraint_sense(sense: ConstraintSense) -> str:
        if sense == ConstraintSense.GREATER_EQUAL:
            return GRB.GREATER_EQUAL
        if sense == ConstraintSense.LESS_EQUAL:
            return GRB.LESS_EQUAL
        if sense == ConstraintSense.EQUAL:
            return GRB.EQUAL
        else:
            raise LinTimException(f"Unknown constraint sense {sense}")

    def getVariableByName(self, name: str) -> Variable:
        return GurobiVariable(self._model.getVarByName(name))

    def setStartValue(self, variable: Variable, value: float):
        raise_for_non_gurobi_variable(variable)
        variable._var.start = value

    def setObjective(self, objective: LinearExpression, sense: OptimizationSense):
        raise_for_non_gurobi_expression(objective)
        obj_sense = GurobiModel.transform_objective_sense(sense)
        self._model.setObjective(objective._expr, obj_sense)

    @staticmethod
    def transform_objective_sense(sense: OptimizationSense):
        if sense == OptimizationSense.MAXIMIZE:
            return GRB.MAXIMIZE
        if sense == OptimizationSense.MINIMIZE:
            return GRB.MINIMIZE
        raise LinTimException(f"Unknown optimization sense {sense}")

    def getObjective(self) -> LinearExpression:
        objective = self._model.getObjective()
        if not isinstance(objective, LinExpr):
            raise LinTimException("We only support linear objectives")
        return GurobiLinearExpression(objective)

    def createExpression(self) -> LinearExpression:
        return GurobiLinearExpression(LinExpr())

    def getSense(self) -> OptimizationSense:
        sense = self._model.modelsense
        if sense == GRB.MINIMIZE:
            return OptimizationSense.MINIMIZE
        else:
            return OptimizationSense.MAXIMIZE

    def write(self, filename: str):
        self._model.write(filename)

    def setSense(self, sense: OptimizationSense):
        obj_sense = GurobiModel.transform_objective_sense(sense)
        self._model.modelsense = obj_sense

    def getStatus(self) -> Status:
        return self.transform_gurobi_status(self._model.status)

    def transform_gurobi_status(self, status: int) -> Status:
        if status == GRB.OPTIMAL:
            return Status.OPTIMAL
        if status == GRB.INFEASIBLE:
            return Status.INFEASIBLE
        if self._model.solcount > 0:
            return Status.FEASIBLE
        return Status.INFEASIBLE

    def solve(self):
        self._model.optimize()

    def computeIIS(self, filename: str):
        self._model.computeIIS()
        self._model.write(filename)

    def getValue(self, variable: Variable):
        raise_for_non_gurobi_variable(variable)
        return variable._var.x

    def getIntAttribute(self, attribute: IntAttribute) -> int:
        self._model.update()
        if attribute == IntAttribute.NUM_VARIABLES:
            return self._model.numvars
        if attribute == IntAttribute.NUM_CONSTRAINTS:
            return self._model.numconstrs
        if attribute == IntAttribute.NUM_BIN_VARIABLES:
            return self._model.numbinvars
        if attribute == IntAttribute.NUM_INT_VARIABLES:
            return self._model.numintvars
        if attribute == IntAttribute.RUNTIME:
            return self._model.runtime
        raise SolverAttributeNotImplementedException(SolverType.GUROBI, attribute.name)

    def getDoubleAttribute(self, attribute: DoubleAttribute):
        if attribute == DoubleAttribute.MIP_GAP:
            return self._model.mipgap
        if attribute == DoubleAttribute.OBJ_VAL:
            return self._model.objval
        raise SolverAttributeNotImplementedException(SolverType.GUROBI, attribute.name)

    def setIntParam(self, param: IntParam, value: int):
        if param == IntParam.TIMELIMIT:
            if value >= 0:
                self._model.Params.timelimit = value
        elif param == IntParam.OUTPUT_LEVEL:
            if value == logging.DEBUG:
                self._model.Params.logtoconsole = 1
                self._model.Params.logfile = "SolverGurobi.log"
            else:
                self._model.Params.logtoconsole = 0
        elif param == IntParam.THREADS:
            if value > 0:
                self._model.Params.threads = value
        else:
            raise SolverParamNotImplementedException(SolverType.GUROBI, param.name)

    def setDoubleParam(self, param: DoubleParam, value: float):
        if param == DoubleParam.MIP_GAP:
            if value >= 0:
                self._model.Params.mipgap = value
        else:
            raise SolverParamNotImplementedException(SolverType.GUROBI, param.name)


class GurobiSolver(Solver):

    def createModel(self) -> generic_solver_interface.Model:
        return GurobiModel(Model())



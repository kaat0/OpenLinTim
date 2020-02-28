from core.exceptions.exceptions import LinTimException
from core.solver.generic_solver_interface import SolverType


class SolverAttributeNotImplementedException(LinTimException):

    def __init__(self, type: SolverType, attribute_name: str):
        super().__init__(f"Error S5: Attribute {attribute_name} is not implemented for {type.name} yet")


class SolverParamNotImplementedException(LinTimException):

    def __init__(self, type: SolverType, param_name: str):
        super().__init__(f"Error S6: Parameter {param_name} is not implemented for {type.name} yet")
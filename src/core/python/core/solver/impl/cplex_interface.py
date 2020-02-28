from core.solver.generic_solver_interface import Solver


class CplexSolver(Solver):

    def __init__(self):
        print("Initialized cplex solver")

    def test_hello(self):
        print("Hello Cplex")
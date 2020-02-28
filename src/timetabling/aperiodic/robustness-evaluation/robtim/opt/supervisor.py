#!/usr/bin/env python3
# -*- coding: utf-8 -*-

__all__ = [
        "Supervisor",
        "DefaultSupervisor",
        "MatrixRobustnessSupervisor",
        "RobustnessSupervisor",
        ]

from robtim import Dataset
from robtim.eval import RobustnessEvaluator

class Supervisor:
    """
    Abstract super class of all supervisors.
    
    A supervisor takes care of interruption an optimization if the objective is reached
    and of collecting statistical information.
    """
    def initialize(self, dataset : Dataset):
        """
        Initializes the supervisor. This is when LinTim components should be configured.
        
        :param dataset: dataset which the optimization is done on
        """
        pass
    def interrupt(self, dataset : Dataset, iteration : int, ean : dict) -> bool:
        """
        This function is called in every loop of the optimization and should do
        two things:
            
        - store statistical information about the optimization
        - interrupt the optimization when the objective is reached
        
        :param dataset: dataset which the optimization is done on
        :param iteration: number of the current iteration counting from 1
        :param ean: dict with information about the current EAN, provided by 
            :func:`EANGenerator.eans`
        :return: True, if the optimization should be interrupted, False otherwise
        """
        pass
    def report(self, dataset : Dataset, iteration : int, ean : dict):
        """
        Returns the collected statistical information at the end of the optimization.
        
        :param dataset: dataset which the optimization is done on
        :param iteration: number of the current iteration counting from 1
        :param ean: dict with information about the current ean, provided by
            :func:`EANGenerator.eans`
        :return: info about the optimization
        """
        pass
    
class DefaultSupervisor(Supervisor):
    """
    Supervisor which only looks at the number of iterations and stops at a certain point
    
    :param n: number of iterations which should be done
    """
    def __init__(self, n : int):
        self.n = n
    def interrupt(self, dataset : Dataset, iteration : int, ean : dict) -> bool:
        return iteration >= self.n
    def report(self, dataset : Dataset, iteration : int, ean : dict):
        return iteration

class MatrixRobustnessSupervisor(Supervisor):
    """
    Supervisor which uses robustness evaluation for interrupting the optimization.
    It supports the following stop criteria:
    
    - a function which checks if the current result is good enough
    - a fixed maximal number of iterations.
    
    Data is stored in a matrix where every row stands for an iteration of the 
    RobustnessEvaluator. In every row the following data is stored in this order:
        
    1. info about the EAN, provided by :func:`EANGenerator.eans`
    2. info about the nominal timetable, provided by :class:`robtim.eval.RobustnessEvaluator`
    3. info about the delayed timetable, provided by :class:`robtim.eval.RobustnessEvaluator`
    
    This function expects the robustness evaluator to return a tuple (nominal, delayed)
    where delayed is a matrix where every row represents one iteration of the evaluator.
    See :class:`robtim.eval.MatrixStatistician` for a statistian which works in this way.
    
    If you use :class:`robtim.eval.MatrixStatistician` the order of columns in the
    returned matrix is as follows:
    
    1. info about the EAN
    2. info about the nominal timetable
    3. info about the delay scenario
    4. info about the delayed timetable
    
    :example:
    .. code-block:: python
    
        evaluator = RobustnessEvaluator(
        scenarios = ScenarioScheduler(TreeOnTrackScenarioGenerator(0.8, 3000), iterations=[evalIterations]),
        statistician = MatrixStatistician([], ["dm_time_average"])
        )
    
        opt = RobustnessOptimizer(
                IncreasingSlackEANGenerator(5, True, ["change"]), 
                MatrixRobustnessSupervisor(evaluator, n = 15)
            )
        
        ean, report = opt.optimize(d)
    
    :param rob: RobustnessEvaluator
    :type rob: :class:`robtim.eval.RobustnessEvaluator`
    :param condition: function which receives the tuple (nominal, delayed), i.e. the result
        of the RobustnessEvaluator and returns True if the optimization should be interrupted,
        default is False, which means that this criterion will not be used
    :param n: maximal amount of iterations or -1 if this criterion should not be used
    """
    def __init__(self, rob : RobustnessEvaluator, condition = False, n : int = -1):
        self.rob = rob
        self.n = n
        self.condition = lambda x : False if condition == False else condition
        self.results = []
    def interrupt(self, dataset : Dataset, iteration : int, ean : dict) -> bool:
        nominal, delayed = self.rob.evaluate(dataset)
        for row in delayed:
            self.results.append([*list(ean.values()), *nominal, *row])
        if self.n > 0 and iteration >= self.n:
            return True
        return self.condition((nominal, delayed))
    def report(self, dataset : Dataset, iteration : int, ean : dict):
        return self.results

class RobustnessSupervisor(MatrixRobustnessSupervisor):
    """
    Supervisor which works basically like :class:`MatrixRobustnessSupervisor` but does not
    make any assumptions about the format of the evaluation result.

    The result is matrix where every row represents one iteration of the optimization.
    Every row contains firstly information about the EAN and secondly the result
    of the robustness evaluation.
    
    :param rob: RobustnessEvaluator
    :type rob: :class:`robtim.eval.RobustnessEvaluator`
    :param condition: function which receives the tuple (nominal, delayed), i.e. the result
        of the RobustnessEvaluator and returns True if the optimization should be interrupted,
        default is False, which means that this criterion will not be used
    :param n: maximal amount of iterations or -1 if this criterion should not be used    
    """
    def interrupt(self, dataset : Dataset, iteration : int, ean : dict) -> bool:
        result = self.rob.evaluate(dataset)
        self.results.append([*list(ean.values()), result])
        if self.n > 0 and iteration >= self.n:
            return True
        return not self.condition(result)
    def report(self, dataset : Dataset, iteration : int, ean : dict):
        return self.results
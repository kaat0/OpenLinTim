#!/usr/bin/env python3
# -*- coding: utf-8 -*-
__all__ = [
        "RobustnessEvaluator", 
        "evaluateRobustness",
        "TreeOnTrackEvaluator",
    ]

import itertools

from robtim import Dataset
from robtim.eval import ScenarioScheduler, ScenarioGenerator, ScAlbertU1, TreeOnTrackScenarioGenerator
from robtim.eval import DelayManager, OrdinaryDelayManager
from robtim.eval import Statistician, OrdinaryStatistician, CoefficientStatistician

def evaluateRobustness(dataset : Dataset, scenarios : ScenarioGenerator = None, delayManager : DelayManager = None, statistician : Statistician = None, silent : bool = False):
   """
   static method for calling the RobustnessEvaluator
   """
   evaluator = RobustnessEvaluator(scenarios, delayManager, statistician, silent)
   return evaluator.evaluate(dataset)

class RobustnessEvaluator:
    """
    Evaluates the Robustness of the current timetable stored in the dataset.
    
    This class assumes that ro-rollout (and all previous planning steps) was done.
    
    Please call :func:`evaluate` do actually evaluate the timetable
    
    :example: RobustnessEvaluator().evaluate(dataset) will evaluate the robustness
        of the current timetable in the dataset which default scenario generator (ScAlbertU1),
        default delay manager (OrdinaryDelayManager) and default statistician (OrdinaryStatistician).
        Please see these classes for details.
    
    :param scenario: instance of :class:`ScenarioGenerator` which models the set of scenarios which should be taken into accunt
    :param delayManager: instance of :class:`DelayManager` which models the delay management planning step
    :param statistician: instance of :class:`Statistician` which takes care of the statistical evaluation of the collected data
    :param silent: whether there should be no additional messages about the progress
    
    """
    def __init__(self, scenarios : ScenarioGenerator = None, delayManager : DelayManager = None, statistician : Statistician = None, silent : bool = False):
        self.scenarios = scenarios
        self.delayManager = delayManager
        self.statistician = statistician
        self.silent = silent
        
        if self.scenarios is None:
            self.scenarios = ScenarioScheduler(ScAlbertU1(600), iterations=[2])
        if self.delayManager is None:
            self.delayManager = OrdinaryDelayManager()
        if self.statistician is None:
            self.statistician = OrdinaryStatistician()
        
    def evaluate(self, dataset : Dataset):
        """
        Evaluates the robustness of the timetable in the dataset.
        
        The following steps are done:

        1. ScenarioGenerator, DelayManager, Statistican are initialized, e.g. the respective functions of the objects called
        2. Statistical information about the nominal timetable is collected and handed over to statistician.collectNominal
        3. For all scenarios provided by :func:`ScenarioGenerator.scenarios` the following is done:
        	- delay management
        	- statistical data is collected
        4. After exhausting the scenario set the statistican is asked for its results which are then returned
	  
        :param dataset: dataset which the robustness of the timetable is evaluated on
        :returns: result of :func:`Statistician.interpret`
        """
        
        if not self.silent:
            print("[RobustnessEvaluator] Initialize evaluation...")
        
        self.scenarios.initialize(dataset)
        self.delayManager.initialize(dataset)
        self.statistician.initialize(dataset)
        
        self.scenarios.reset(dataset)
        dataset.copy("delay-management/Timetable-expanded.tim","delay-management/Timetable-disposition.tim")
        self.statistician.collectNominal(dataset)
        
        if not self.silent:
            print("[RobustnessEvaluator] Initialization done.")
        
        for (sc, i) in zip(self.scenarios.scenarios(dataset), itertools.count(1)):
            self.delayManager.dispositionTimetable(dataset)
            self.statistician.collectStats(dataset, sc)
        
            if not self.silent:
                print("[RobustnessEvaluator] Loop", i, "done.")
        
        return self.statistician.interpret(dataset)
    
class TreeOnTrackEvaluator(RobustnessEvaluator):
    """
    RobustnessEvaluator which uses the TreeOnTrackScenarioGenerator together with
    the CoefficientStatistician. It returns the average robustness coefficient
    of all delays in the sample.
    
    :param p: TreeOnTrack's p parameter
    :param beta: TreeOnTrack's beta parameter
    :param n: number of iterations which should be used
    """
    def __init__(self, p, beta, n):
        super().__init__(
                scenarios = ScenarioScheduler(TreeOnTrackScenarioGenerator(p, beta), iterations=[n]), 
                statistician = CoefficientStatistician(["dm_time_average"]))

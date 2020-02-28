#!/usr/bin/env python3
# -*- coding: utf-8 -*-
__all__ = ["Statistician", 
           "OrdinaryStatistician",
           "MatrixStatistician",
           "CoefficientStatistician",
          ]

import os, numpy

from robtim import Dataset

class Statistician:
    """
    Abstract super class of all statisticians. A statistician collects, stores and interprets
    statistical data about delays and their effects.
    """
    def initialize(self, dataset : Dataset):
        """
        Initializes the statistician. This is where configuration changes for LinTim
        components should be applied.
        
        :param dataset: dataset which statistics should be done on
        """
        pass
    def collectNominal(self, dataset: Dataset):
        """
        Collects data about the nominal timetable, i.e. without delays.
        
        :param dataset: dataset which statistics should be done on
        """
        pass
    def collectStats(self, dataset : Dataset, scenario : dict):
        """
        Collects data about a disposition timetable, i.e. with delays.
        
        :param dataset: dataset which statistics should be done on
        :param scenario: dict with information about the delay scenario provided by the scenario generator,
            i.e. an instance of :class:`ScenarioGenerator`.
        """
        pass
    def interpret(self, dataset : Dataset):
        """
        Interprets the data and returns the final result of the analysis.
        
        :param dataset: dataset which statistics should be done on
        """
        pass

class OrdinaryStatistician(Statistician):
    """
    Statistician which stupidly stores everything what it gets.
    
    The result is a tuple (nominal, delayed) where
    
    - nominal is a dict of information about the nominal timetable
    - delayed is a list of tuples where the first entry is the dict of scenario info and the second entry is the dict of evaluation info
    
    :param extended: whether extended info should be included (DM_eval_extended)
    """
    def __init__(self, extended : bool = False):
        self.extended = extended
    def initialize(self, dataset : Dataset):
        self.result = []
        dataset.applyConfig({"DM_eval_extended" : self.extended})
        if not os.path.isdir(dataset.realPath("statistic")):
            os.mkdir(dataset.realPath("statistic"))    
    def collectNominal(self, dataset: Dataset):
        dataset.make("dm-disposition-timetable-evaluate")
        stats = dataset.statistics()
        self.nominal = stats
    def collectStats(self, dataset : Dataset, scenario : dict):
        dataset.make("dm-disposition-timetable-evaluate")
        stats = dataset.statistics()
        self.result.append([scenario, stats])
    def interpret(self, dataset : Dataset):
        return self.nominal, self.result
    
class MatrixStatistician(OrdinaryStatistician):
    """
    Statistician which filters the data and strores it in a matrix.
    
    The result is a tuple (nominal, delayed) where
    
    - nominal is a list of the selected info about the nominal timetable
    - delayed is a list of lists of the selected info about the delayed timetables
    
    :param keysScenario: properties of the scenarios which should be included
    :param keysEvaluation: properties of the evaluations which should be included
    :param extended: whether extended info should be included (DM_eval_extended)
    """
    def __init__(self, keysScenario : list, keysEvaluation : list, extended : bool = False):
        super().__init__(extended)
        self.keysScenario = keysScenario
        self.keysEvaluation = keysEvaluation
    def collectNominal(self, dataset: Dataset):
        dataset.make("dm-disposition-timetable-evaluate")
        stats = dataset.statistics()
        self.nominal = [stats[key] for key in self.keysEvaluation]
    def collectStats(self, dataset : Dataset, scenario : dict):
        dataset.make("dm-disposition-timetable-evaluate")
        stats = dataset.statistics()
        self.result.append(
                [scenario[key] for key in self.keysScenario] +
                [stats[key] for key in self.keysEvaluation]
            )

class CoefficientStatistician(MatrixStatistician):
    """
    Statistician which computes the averages of a evaluation keys and devides them
    by the corresponding values for the nominal timetable.
    
    :param keysEvaluation: list of evaluation keys which should be used
    :param extended: use extended evaluation
    """
    def __init__(self, keysEvaluation : list, extended : bool = False):
        super().__init__([], keysEvaluation, extended)
    def interpret(self, dataset : Dataset):
        nominal, delayed = super().interpret(dataset)
        delayed = numpy.array(delayed)
        avgs = numpy.fromiter(map(numpy.average, delayed.T), dtype=float)
        return numpy.array([avg / nom for (avg, nom) in zip(avgs, nominal)])

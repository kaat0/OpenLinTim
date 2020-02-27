#!/usr/bin/env python3
# -*- coding: utf-8 -*-

__all__ = [
        "RobustnessOptimizer",
    ]

import itertools

from robtim import Dataset
from robtim.opt import EANGenerator, Supervisor, Timetabler
from robtim.opt import DefaultTimetabler

class RobustnessOptimizer:
    """
    Optimizes the robustness of a timetable of a dataset.
    
    It is assumed that lc-line-concept and all previous planning steps have been done.
    
    To run the optimization please call :func:`optimize`.
    
    :param ean_generator: EANGenerator which generates new (more robust) EANs
    :param supervisor: Supervisor which takes care of the interruption of the (possibly infinite) optimization and stores statistical data.
    :param silent: whether there should be additional output or not
    """
    def __init__(self, ean_generator : EANGenerator, supervisor : Supervisor, timetabler : Timetabler = DefaultTimetabler(), silent : bool = False):
        self.ean_generator = ean_generator
        self.supervisor = supervisor
        self.timetabler = timetabler
        self.silent = silent
        
    def optimize(self, dataset : Dataset):
        """
        Optimizes the EAN in dataset for robustness.

        The following steps are done:
            1. EANGenerator, Supervisor and Timetabler are initialized, i.e. the
               the respective functions on the objects are called.
            2. For all EANs provided by :func:`EANGenerator.eans` the following steps
               are done:
              - timetabling, i.e. :func:`Timetabler.timetable`, which 
                computes a periodic and aperiodic timetable for the current EAN.
              - statistical data is collected by the supervisor.
              - the supervisor is asked whether the optimization should be 
                interrupted, i.e. if the objective is reached.
                See :func:`Supervisor.interrupt`.
            3. Information about the final EAN and the report of the supervisor
               are returned to the callee.
        
        :param dataset: dataset which the optimization should be done on
        :return: tuple (ean, report) where ean is a dictionary with information
            about the final EAN and report the result of :func:`Supervisor.report`.
        """
        self.ean_generator.initialize(dataset)
        self.supervisor.initialize(dataset)
        self.timetabler.initialize(dataset)
        
        for (ean, it) in zip(self.ean_generator.eans(dataset), itertools.count(1)):
            self.timetabler.timetable(dataset)
            
            interrupt = self.supervisor.interrupt(dataset, it, ean)
            
            if not self.silent:
                print("[RobustnessOptimizer]", "Loop", it, "done.")
            
            if interrupt:
                break
        return ean, self.supervisor.report(dataset, it, ean)
            
            

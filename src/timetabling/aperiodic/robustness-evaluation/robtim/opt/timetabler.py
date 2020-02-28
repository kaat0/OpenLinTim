#!/usr/bin/env python3
# -*- coding: utf-8 -*-

__all__ = [
     "Timetabler",
     "ConfigurableTimetabler",
     "DefaultTimetabler",
    ]

from robtim import Dataset

class Timetabler:
    """
    Abstract super class of all timetablers. A timetabler should take care of the 
    planning steps tim-timetable and ro-rollout and provide functionality similar to the 
    functionality of the related LinTim make target.    
    """
    def initialize(self, dataset : Dataset):
        """
        Initializes the timetabler. This is where the configuration of the
        used LinTim components should be done.
        
        :param dataset: Dataset which the timetabling is done on
        """
        pass
    def timetable(self, dataset : Dataset):
        """
        Function which is called by the optimizer when a timetable
        is required.
        
        :param dataset: Dataset which the timetabling is done on
        """
        pass

class ConfigurableTimetabler(Timetabler):
    """
    Simple implementation of a :class:`Timetabler`. It calls the LinTim make target
    tim-timetable and ro-rollout with the given configuration parameters
    
    :param config: dict of configuration paramters for LinTim
    """
    def __init__(self, config : dict):
        self.config = config
    def initialize(self, dataset : Dataset):
        dataset.applyConfig(self.config)
    def timetable(self, dataset : Dataset):
        dataset.make("tim-timetable")
        dataset.make("ro-rollout")

class DefaultTimetabler(ConfigurableTimetabler):
    """
    Simplifies the :class:`ConfigurableTimetabler` by allowing only certain configuration
    parameters.
    
    :param model: value for LinTim's tim_model paramter
    """
    def __init__(self, model : str = "MATCH"):
        super().__init__({"tim_model" : model})

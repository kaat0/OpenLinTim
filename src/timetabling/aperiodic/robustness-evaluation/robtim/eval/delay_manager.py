#!/usr/bin/env python3
# -*- coding: utf-8 -*-
__all__ = ["DelayManager", 
           "ConfigurableDelayManager", 
           "OrdinaryDelayManager"
          ]

from robtim import Dataset

class DelayManager:
    """
    Abstract super class of all delay managers. A delay manager takes care of the 
    planning step dm-delay-management and provides functionality similar to the 
    functionality of the related LinTim component.    
    """
    def initialize(self, dataset : Dataset):
        """
        Initializes the delay manager. This is where the configuration of the
        used LinTim components should be done.
        
        :param dataset: Dataset which the delay management is done on
        """
        pass
    def dispositionTimetable(self, dataset : Dataset):
        """
        Function which is called by the evaluator when a disposition timetable
        is required.
        
        :param dataset: Dataset which the delay management is done on
        """
        pass

class ConfigurableDelayManager(DelayManager):
    """
    Simple implementation of a :class:`DelayManager`. It calls the LinTim make target
    dm-disposition-timtable with the given configuration parameters
    
    :param config: dict of configuration paramters for LinTim
    """
    def __init__(self, config : dict):
        self.config = config
    def initialize(self, dataset : Dataset):
        dataset.applyConfig(self.config)
    def dispositionTimetable(self, dataset : Dataset):
        dataset.make("dm-disposition-timetable")

class OrdinaryDelayManager(ConfigurableDelayManager):
    """
    Simplifies the :class:`ConfigurableDelayManager` by allowing only certain configuration
    parameters.
    
    :param method: value for LinTim's DM_method paramter
    """
    def __init__(self, method : str = "propagate"):
        super().__init__({"DM_method" : method})
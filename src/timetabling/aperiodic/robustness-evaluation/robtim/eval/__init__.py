#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
The `robtim.eval` package contains all functionality which is related to
evaluating the robustness of a timetable. In order to provide a general 
framework which can be used for the large variety of robustness concepts
and settings it has a modular design. 

The main class is the :class:`RobustnessEvaluator` which takes care of the general 
process and coordinates the work of the different components. These components are:
    - scenario generators which generate a set of delay scenarios
    - delay managers which compute disposition timetables based on the delays and the given timetable
    - statisticians which collect and analyze statistical data about the timetables, e.g. in order to compute a robustness coefficient.
	
How these three components work must be specified by extending the classes
:class:`ScenarioGenerator`, :class:`DelayManager` and :class:`Statistician`.

Apart from the general classes this package provides several concrete implementations.
Most interesting for the user is probably the :class:`TreeOnTrackEvaluator` which 
combines the components used in the report.
"""

# First import submodules which do not depend on other modules in this module
from robtim.eval.scenario_generator import *
from robtim.eval.delay_manager import *
from robtim.eval.statistician import *

# Import other submodules
from robtim.eval.evaluator import *

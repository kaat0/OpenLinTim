#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
The `robtim.opt` package contains all functionality which is related to
the optimization of a timetable by robustness. In order to provide a general 
framework which can be used for the large variety of robustness concepts
and settings it has a modular design. 

The main class is the :class:`RobustnessOptimizer` which takes care of the general 
process and coordinates the work of the different components. These components are:
    - EAN generators compute new, e.g. more robust, event activity networks
    - timetablers compute periodic and aperiodic timetables based on the EAN
    - supervisors provide a stop criterion and collect and analyze statistical data
      about the optimization.
    
How these three components work must be specified by extending the classes
:class:`EANGenerator`, :class:`Timetabler` and :class:`Supervisor`.

"""
# First import submodules which do not depend on other modules in this module
from robtim.opt.ean_generator import *
from robtim.opt.timetabler import *
from robtim.opt.supervisor import *

# Import other submodules
from robtim.opt.optimizer import *
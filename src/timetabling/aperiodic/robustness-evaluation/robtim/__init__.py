#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
This package contains all modules which have been developed within the scientific
computing pratical by Anna Pessarrodona Marfà <a.pessarrodonamarf@stud.uni-goettingen.de> 
and Tim Seppelt <t.seppelt@stud.uni-goettingen.de> in the summer semester 2018.

Please see the related report for further information on the background and the functionality.

The whole project is based on the LinTim framework which is developed at the Universität Göttingen.

The package is structured in the following way:
    - `robtim` contains general functionality for example for input/output, e.g. :class:`Dataset`
    - `robtim.eval` contains all functionality related to the evaluation of 
      robustness. This includes:
          - the :class:`robtim.eval.RobustnessEvaluator` which manages the evaluation
          - several scenario generators which generate delay scenarios
          - several delay managers which take care of the delay management
          - severel statisticians which collect and analyze statistical data during the evaluation.
    - `robtim.opt` contains all functionality related to the optimization of a 
      timetable by robustness. This includes:
          - the :class:`robtim.opt.RobustnessOptimizer` which manages the optimization
          - several EAN generators which generate event activitiy networks
          - several timetablers which compute timetables based on the EANs
          - several supervisors which provide a stop criterion and collect / analyze statistical data
          

"""

from robtim.io import *

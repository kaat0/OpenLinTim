#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Standardized configuration of the system environment etc.

This module is not used by any modules in the robtim package. It's just for 
main_*.py modules.

"""

import sys, os, getpass

user = getpass.getuser()

# Execution environment for make
env = os.environ.copy()
env["GUROBI_HOME"] = "/home/"+ user +"/gurobi800/linux64/"
env["PATH"] = env["PATH"] + ":" + env["GUROBI_HOME"]
env["LD_LIBRARY_PATH"] = env["GUROBI_HOME"] + "lib/"
env["GRB_LICENSE_FILE"] = "/home/"+ user +"/gurobi.lic"
env["CLASSPATH"] = env["GUROBI_HOME"] + "/lib/gurobi.jar"

# Add LinTim's python core to PYPATH
sys.path.append("/home/"+ user +"/LinTim/src/core/python/")

# Config for LinTim components
config = {
    "lc_solver": "GUROBI", 
    "DM_solver": "Gurobi", 
    "DM_method" : "propagate",
    "tim_solver": "gurobi",
    }

# Path to LinTim's dataset directory
path = "/home/"+ user +"/LinTim/datasets"
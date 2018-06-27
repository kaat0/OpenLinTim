#!/usr/bin/env bash
PATH_ARRAY=($buildInputs)
GUROBI_PATH=${PATH_ARRAY[0]}
export LD_LIBRARY_PATH=${GUROBI_PATH}/lib:${LD_LIBRARY_PATH}
export CLASSPATH=${GUROBI_PATH}/lib/gurobi.jar:${CLASSPATH}
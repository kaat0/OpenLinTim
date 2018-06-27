#!/usr/bin/env bash
PROGRAMPATH=`dirname $0`
PYTHON_CORE_PATH=${PROGRAMPATH}/../../../core/python
PYTHONPATH="${PROGRAMPATH}/src:${PYTHON_CORE_PATH}"
python3 ${PROGRAMPATH}/src/visum_transformer/main/read_fixed_times.py $1
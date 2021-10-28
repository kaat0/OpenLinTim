#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

if [[ `type mosel 2>&1 | grep "not found"` ]]; then
    echo "Error: xpress-mosel not available or \"mosel\" not contained in "\
    "the PATH environment variable.";
    exit 1;
fi

VS_MODEL=`"${CONFIGCMD[@]}" -s vs_model -u`

if [[ ${VS_MODEL} == "CANAL_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/vehicle_flow" | mosel -s
else
	echo "The model you choosed is ${VS_MODEL}, but a vehicle flow can only be calculated for the model CANAL"
fi

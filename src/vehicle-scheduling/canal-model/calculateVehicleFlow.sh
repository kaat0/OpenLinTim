#!/bin/bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

if [[ `type mosel 2>&1 | grep "not found"` ]]; then
	echo "Error: xpress-mosel not available or \"mosel\" not contained in "\
	"the PATH environment variable.";
	exit 1;
fi

SLIST="vs_model
vs_vehicle_costs
vs_min_distance
vs_penalty_costs
vs_depot_index
vs_turn_over_time
vs_verbose"

VARLIST=`for setting in ${SLIST}; do
	VALUE=\`${CONFIGCMD} -s ${setting} -u\`
	echo -n ${setting}=${VALUE},
done`

VS_MODEL=`${CONFIGCMD} -s vs_model -u`

vs_vehicle_costs=`${CONFIGCMD} -s vs_vehicle_costs -u`
vs_min_distance=`${CONFIGCMD} -s vs_min_distance -u`
vs_penalty_costs=`${CONFIGCMD} -s vs_penalty_costs -u`
vs_depot_index=`${CONFIGCMD} -s vs_depot_index -u`
vs_turn_over_time=`${CONFIGCMD} -s vs_turn_over_time -u`
vs_verbose=`${CONFIGCMD} -s vs_verbose -u`

if [[ ${VS_MODEL} == "CANAL_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/vehicle_flow" | mosel -s
else 
	echo "The model you choosed is ${VS_MODEL}, but a vehicle flow can only be calculated for the model CANAL"
fi

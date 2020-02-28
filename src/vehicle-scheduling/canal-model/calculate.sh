#!/usr/bin/env bash

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
	VALUE=\`"${CONFIGCMD[@]}" -s ${setting} -u\`
	echo -n ${setting}=${VALUE},
done`

VS_MODEL=`"${CONFIGCMD[@]}" -s vs_model -u`

vs_vehicle_costs=`"${CONFIGCMD[@]}" -s vs_vehicle_costs -u`
vs_min_distance=`"${CONFIGCMD[@]}" -s vs_min_distance -u`
vs_penalty_costs=`"${CONFIGCMD[@]}" -s vs_penalty_costs -u`
vs_depot_index=`"${CONFIGCMD[@]}" -s vs_depot_index -u`
vs_turn_over_time=`"${CONFIGCMD[@]}" -s vs_turn_over_time -u`
vs_verbose=`"${CONFIGCMD[@]}" -s vs_verbose -u`

if [[ ${VS_MODEL} == "MDM1" ]]; then
	echo "exec ${PROGRAMPATH}/mdm1" | mosel -s || exit 1

elif [[ ${VS_MODEL} == "MDM2" ]]; then
	echo "exec ${PROGRAMPATH}/mdm2" | mosel -s || exit 1

elif [[ ${VS_MODEL} == "ASSIGNMENT_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/am" | mosel -s || exit 1

elif [[ ${VS_MODEL} == "TRANSPORTATION_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/tm" | mosel -s || exit 1

elif [[ ${VS_MODEL} == "NETWORK_FLOW_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/nm" | mosel -s || exit 1

elif [[ ${VS_MODEL} == "CANAL_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/vehicle_flow" | mosel -s || exit 1
else 
	echo "VS_MODEL ${VS_MODEL} unknown"
	exit 1
fi

#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

if [[ `type mosel 2>&1 | grep "not found"` ]]; then
	echo "Error: xpress-mosel not available or \"mosel\" not contained in "\
	"the PATH environment variable.";
	exit 1;
fi

VS_MODEL=`"${CONFIGCMD[@]}" -s vs_model -u`

if [[ ${VS_MODEL} == "MDM1" ]]; then
	echo "exec ${PROGRAMPATH}/mdm1" | mosel -s || exit 1
	rm ${PROGRAMPATH}/data_mdm1

elif [[ ${VS_MODEL} == "MDM2" ]]; then
	echo "exec ${PROGRAMPATH}/mdm2" | mosel -s || exit 1
	rm ${PROGRAMPATH}/data_mdm2

elif [[ ${VS_MODEL} == "ASSIGNMENT_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/am" | mosel -s || exit 1
	rm ${PROGRAMPATH}/data_am

elif [[ ${VS_MODEL} == "TRANSPORTATION_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/tm" | mosel -s || exit 1
	rm ${PROGRAMPATH}/data_tm

elif [[ ${VS_MODEL} == "NETWORK_FLOW_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/nm" | mosel -s || exit 1
	rm ${PROGRAMPATH}/data_nm

elif [[ ${VS_MODEL} == "CANAL_MODEL" ]]; then
	echo "exec ${PROGRAMPATH}/vehicle_flow" | mosel -s || exit 1
	rm ${PROGRAMPATH}/data_vehicle_flow
else
	echo "VS_MODEL ${VS_MODEL} unknown"
	exit 1
fi

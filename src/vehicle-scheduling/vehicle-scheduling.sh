#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

vs_model=`"${CONFIGCMD[@]}" -s vs_model -u`
if [[ ${vs_model} = "MDM1" || ${vs_model} = "MDM2" || ${vs_model} = "ASSIGNMENT_MODEL" || ${vs_model} = "TRANSPORTATION_MODEL" || ${vs_model} = "NETWORK_FLOW_MODEL" || ${vs_model} = "CANAL_MODEL" ]]; then
	ant -q -f ${PROGRAMPATH}/canal-model/build.xml
	java  ${JFLAGS[@]} -classpath ${PROGRAMPATH}/../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/canal-model/build net.lintim.main.vehiclescheduling.FlowsAndTransfers $1
	${PROGRAMPATH}/canal-model/calculate.sh
	java  ${JFLAGS[@]} -classpath ${PROGRAMPATH}/../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/canal-model/build net.lintim.main.vehiclescheduling.CalculateMappingsAndVS $1
elif [[ ${vs_model} = "LINE_BASED" ]]; then
	bash ./${PROGRAMPATH}/Line-Based/run.sh $1
elif [[ ${vs_model} = "SIMPLE" ]]; then
	ant -q -f ${PROGRAMPATH}/simple/build.xml
	java  ${JFLAGS[@]} -classpath ${PROGRAMPATH}/simple/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.vehiclescheduling.SimpleVehicleScheduleMain $1
elif [[ ${vs_model} = "IP" ]]; then
	ant -q -f ${PROGRAMPATH}/ip-model/build.xml
	java  ${JFLAGS[@]} -classpath ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/ip-model/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.vehiclescheduling.IPModelMain $1
else
	echo "Error: Invalid vs_model argument: ${vs_model}"
	exit 1;
fi
#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

vs_model=`"${CONFIGCMD[@]}" -s vs_model -u`
echo "${vs_model}"
if [[ ${vs_model} = "MDM1" || ${vs_model} = "MDM2" || ${vs_model} = "ASSIGNMENT_MODEL" || ${vs_model} = "TRANSPORTATION_MODEL" || ${vs_model} = "NETWORK_FLOW_MODEL" || ${vs_model} = "CANAL_MODEL" ]]; then
	ant -q -f ${PROGRAMPATH}/canal-model/build.xml
	time java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -classpath ${PROGRAMPATH}/../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/canal-model net.lintim.algorithm.vehiclescheduling.FlowsAndTransfers basis/Config.cnf
	time ${PROGRAMPATH}/canal-model/calculate.sh
	time java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -classpath ${PROGRAMPATH}/../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/canal-model net.lintim.algorithm.vehiclescheduling.CalculateMappingsAndVS basis/Config.cnf
elif [[ ${vs_model} = "LINE_BASED" ]]; then
	bash ./${PROGRAMPATH}/Line-Based/run.sh
elif [[ ${vs_model} = "SIMPLE" ]]; then
	ant -q -f ${PROGRAMPATH}/simple/build.xml
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -classpath ${PROGRAMPATH}/simple/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.vehiclescheduling.SimpleVehicleScheduleMain basis/Config.cnf
elif [[ ${vs_model} = "IP" ]]; then
	ant -q -f ${PROGRAMPATH}/ip-model/build.xml
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -classpath ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/ip-model/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.vehiclescheduling.IPModelMain basis/Config.cnf
else
	echo "Error: Invalid vs_model argument: ${vs_model}"
	exit 1;
fi
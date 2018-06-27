#!/bin/bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

TIM_MODEL=`${CONFIGCMD} -s tim_model -u`

if [[ ${TIM_MODEL} == "javatools" ]]; then
  sh ${PROGRAMPATH}/../../essentials/javatools/runner.sh PeriodicTimetable basis/Config.cnf || exit 1

elif [[ ${TIM_MODEL} == "network_simplex" ]]; then
	make -C ${PROGRAMPATH}/modulo-simplex/Release || exit 1
	echo "Using network simplex method."
	${PROGRAMPATH}/modulo-simplex/Release/networksimplex

elif [[ ${TIM_MODEL} == "con_prop" ]]; then
	make -C ${PROGRAMPATH}/constraint-prop/src || exit 1
	echo "Using constraint propagation."
	${PROGRAMPATH}/constraint-prop/src/conprop

elif [[ ${TIM_MODEL} == "con_ns" ]]; then
	make -C ${PROGRAMPATH}/constraint-prop/src || exit 1
	make -C ${PROGRAMPATH}/modulo-simplex/Release || exit 1
	echo "Using constraint propagation + network simplex."
	${PROGRAMPATH}/constraint-prop/src/conprop
	if [[ $? == 0 ]]; then
	   ${PROGRAMPATH}/modulo-simplex/Release/networksimplex
	fi

elif [[ ${TIM_MODEL} == "ns_improve" ]]; then
	make -C ${PROGRAMPATH}/modulo-simplex/Release || exit 1
	echo "Improving a given timetable by network simplex algorithm."
	${PROGRAMPATH}/modulo-simplex/Release/networksimplex

elif [[ ${TIM_MODEL} == "csp" ]]; then
	make -C ${PROGRAMPATH}/csp/ean2csp ean2csp || exit 1
	make -C ${PROGRAMPATH}/csp/abscon/tools/saucy-1.1 saucy || exit 1
	echo "Finding a feasible timetable by using a csp solver."
	PERIOD=`${CONFIGCMD} -s period_length -u`
	ACTIVITYFILE=`${CONFIGCMD} -s default_activities_periodic_file -u`
	${PROGRAMPATH}/csp/ean2csp/build/ean2csp ${PERIOD} ${ACTIVITYFILE} ${PROGRAMPATH}/csp/temp.xml
	java -cp ${PROGRAMPATH}/csp/abscon/abscon112V4.jar abscon.Resolution ${PROGRAMPATH}/csp/abscon/mac.xml 1 XSax ${PROGRAMPATH}/csp/temp.xml | tee ${PROGRAMPATH}/csp/solution.txt
	echo "#Feasible timetable found with csp." > `${CONFIGCMD} -s default_timetable_periodic_file -u`
	grep '^v' ${PROGRAMPATH}/csp/solution.txt | sed 's/v //g' | sed 's/\ /\n/g' | grep -v '^$' | sed = | sed 'N;s/\n/; /' | sed '$d' >> `${CONFIGCMD} -s default_timetable_periodic_file -u`

elif [[ ${TIM_MODEL} == "csp_ns" ]]; then
	make -C ${PROGRAMPATH}/modulo-simplex/Release || exit 1
	make -C ${PROGRAMPATH}/csp/ean2csp ean2csp || exit 1
	make -C ${PROGRAMPATH}/csp/abscon/tools/saucy-1.1 saucy || exit 1
	echo "Using csp solver + network simplex."
	PERIOD=`${CONFIGCMD} -s period_length -u`
	ACTIVITYFILE=`${CONFIGCMD} -s default_activities_periodic_file -u`
	${PROGRAMPATH}/csp/ean2csp/build/ean2csp ${PERIOD} ${ACTIVITYFILE} ${PROGRAMPATH}/csp/temp.xml
	java -cp ${PROGRAMPATH}/csp/abscon/abscon112V4.jar abscon.Resolution ${PROGRAMPATH}/csp/abscon/mac.xml 1 XSax ${PROGRAMPATH}/csp/temp.xml | tee ${PROGRAMPATH}/csp/solution.txt
	echo "#Feasible timetable found with csp." > `${CONFIGCMD} -s default_timetable_periodic_file -u`
	grep '^v' ${PROGRAMPATH}/csp/solution.txt | sed 's/v //g' | sed 's/\ /\n/g' | grep -v '^$' | sed = | sed 'N;s/\n/; /' | sed '$d' >> `${CONFIGCMD} -s default_timetable_periodic_file -u`
	if [[ $? == 0 ]]; then
	   ${PROGRAMPATH}/modulo-simplex/Release/networksimplex
	fi
elif [[ ${TIM_MODEL} == "matching" ]]; then
	sh ${PROGRAMPATH}/matching/run-matching.sh
elif [[ ${TIM_MODEL} == "matching_merge" ]]; then
    make -C ${PROGRAMPATH}/matching_merge/Release || exit 1
    echo "Using matching-merge algorithm."
    ${PROGRAMPATH}/matching_merge/Release/matching_merge

elif [[ ${TIM_MODEL} == "rptts" ]]; then
    make -C ${PROGRAMPATH}/rptts/Release || exit 1
    echo "Using rptts."
    ${PROGRAMPATH}/rptts/Release/rptts
elif [[ ${TIM_MODEL} == "MATCH" ]]; then
    make -C ${PROGRAMPATH}/performance_rptts/Release || exit 1
    echo "Using MATCH."
    ${PROGRAMPATH}/performance_rptts/Release/solve

elif [[ ${TIM_MODEL} == "rptts_ns" ]]; then
    make -C ${PROGRAMPATH}/modulo-simplex/Release || exit 1
    make -C ${PROGRAMPATH}/rptts/Release || exit 1
    echo "Using rptts + network_simplex."
    ${PROGRAMPATH}/rptts/Release/rptts
    if [[ $? == 0 ]]; then
	   ${PROGRAMPATH}/modulo-simplex/Release/networksimplex
	fi
elif [[ ${TIM_MODEL} == "ip" ]]; then
	ant -q -f ${PROGRAMPATH}/ip/build.xml
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/ip/build${PATHSEP}${PROGRAMPATH}/../../core/java/lintim-core.jar net.lintim.main.timetabling.periodic.PespIp basis/Config.cnf

else
	echo "Error: Requested TIM_MODEL \"${TIM_MODEL}\" not available!"
	exit 1
fi

EXITSTATUS=$?

exit ${EXITSTATUS}

#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

lc_model=`"${CONFIGCMD[@]}" -s lc_model -u`
default_lines_file=`"${CONFIGCMD[@]}" -s default_lines_file -u`
lc_minimal_global_frequency=`"${CONFIGCMD[@]}" -s lc_minimal_global_frequency -u`
statistic_file=`"${CONFIGCMD[@]}" -s default_statistic_file -u`
lc_respect_fixed_lines=`"${CONFIGCMD[@]}" -s lc_respect_fixed_lines -u`
lc_common_frequency_divisor=`"${CONFIGCMD[@]}" -s lc_common_frequency_divisor -u`

if [[ ${lc_model} == from_default ]]; then
	lc_model_default_file=`"${CONFIGCMD[@]}" -s default_lines_default_file -u`
	cp ${lc_model_default_file} ${default_lines_file} || (
			echo "Failed to copy default line concept"
			exit 1 )
elif [[ ${lc_model} == cost_greedy_1 ]] || [[ ${lc_model} == cost_greedy_2 ]]; then
	make -C ${PROGRAMPATH}/cost-heuristics || exit 1
	${PROGRAMPATH}/cost-heuristics/cost_heuristics basis/Config.cnf

elif [[ ${lc_model} == cost ]] && ( [[ ${lc_respect_fixed_lines} == true ]] || [[ ${lc_common_frequency_divisor} != 1 ]] ); then
    ant -q -f ${PROGRAMPATH}/cost-model-extended/build.xml
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -cp ${CLASSPATH}:${PROGRAMPATH}/cost-model-extended/build:${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.lineplanning.ExtendedCost basis/Config.cnf

elif [[ ${lc_model} == cost ]]; then
  ant -q -f ${PROGRAMPATH}/cost-model/build.xml
  java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -cp "${CLASSPATH}${PATHSEP}${PROGRAMPATH}/cost-model/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar" net.lintim.main.lineplanning.Cost basis/Config.cnf

elif [[ ${lc_model} == cost_restricting_frequencies ]]; then
	ant -q -f ${PROGRAMPATH}/cost-model-restricting-frequencies/build.xml
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/cost-model-restricting-frequencies/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.lineplanning.CostRestrictingFrequencies basis/Config.cnf

elif [[ ${lc_model} == game ]]; then
	make -C ${PROGRAMPATH} || exit 1
	if [[ `type mosel 2>&1 | grep "not found"` ]]; then
		echo "Error: xpress-mosel not available or \"mosel\" not contained in "\
		"the PATH environment variable.";
		exit 1;
	fi

	${PROGRAMPATH}/cost-model/Conv_Game &&
	echo "exec ${PROGRAMPATH}/cost-model/line_game.mos lc_minimal_global_frequency=${lc_minimal_global_frequency}" | mosel -s &&
	${PROGRAMPATH}/cost-model/SolConv || exit 1
	rm -f line-planning/xpresssol
	rm -f line-planning/Moseldaten

	cd ${OLDPWD}

elif [[ ${lc_model} == direct ]]; then
	ant -q -f ${PROGRAMPATH}/direct-travelers/build.xml build-direct-travelers
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/direct-travelers/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.lineplanning.Direct basis/Config.cnf

elif [[ ${lc_model} == direct_restricting_frequencies ]]; then
	ant -q -f ${PROGRAMPATH}/direct-travelers-restricting-frequencies/build.xml
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/direct-travelers-restricting-frequencies/build${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar net.lintim.main.lineplanning.DirectRestrictingFrequencies basis/Config.cnf

elif [[ ${lc_model} == direct_relaxation ]]; then
	ant -q -f ${PROGRAMPATH}/direct-travelers-relaxation/build.xml build-direct-travelers-relaxation
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/direct-travelers-relaxation${PATHSEP}${PROGRAMPATH}/../essentials/config Direct
elif [[ ${lc_model} == direct ]] || [[ ${lc_model} == direct_relaxation ]]; then
	ant -q -f ${PROGRAMPATH}/direct-travellers/build.xml build-direct-travellers
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/direct-travellers${PATHSEP}${PROGRAMPATH}/../essentials/config Direct

elif [[ ${lc_model} == mult-cost-direct || ${lc_model} == mult-cost-direct-relax ]]; then
	ant -q -f ${PROGRAMPATH}/cost-model-direct-travellers/build.xml build-cost-model-direct-travellers
	java -Djava.util.logging.config.file=${PROGRAMPATH}/../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/cost-model-direct-travellers${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar CostDirect basis/Config.cnf

elif [[ ${lc_model} == traveling-time-cg ]]; then
	ant -q -f ${PROGRAMPATH}/traveling-time/column-generation-approach/build.xml build-traveling-time
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/traveling-time/column-generation-approach${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar Run

elif [[ ${lc_model} == minchanges_ip ]]; then
	ant -q -f ${PROGRAMPATH}/min-changes/build.xml build-min-changes
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/min-changes/build${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH}/../essentials/shortest-paths/src${PATHSEP}${PROGRAMPATH}/../../libs/k-shortest-paths/build${PATHSEP}${PROGRAMPATH}/direct-travellers${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/sl-helper/PTNTools MinChangesIP "basis/Config.cnf"

elif [[ ${lc_model} == minchanges_cg ]]; then
	ant -q -f ${PROGRAMPATH}/min-changes/build.xml build-min-changes
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/min-changes/build${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH}/../essentials/shortest-paths/src${PATHSEP}${PROGRAMPATH}/../../libs/k-shortest-paths/build${PATHSEP}${PROGRAMPATH}/direct-travellers${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/sl-helper/PTNTools MinChangesColGen "basis/Config.cnf"

else
	echo "Error: Invalid lc_model argument: ${lc_model}"
	exit 1
fi

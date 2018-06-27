#!/bin/bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

ant -q -f ${PROGRAMPATH}/build.xml build-linepool

lpool_model=`${CONFIGCMD} -s lpool_model -u`

if [[ ${lpool_model} == tree_based ]]; then
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/lp-helper${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/../line-planning/cost-model/cost-model.jar${PATHSEP}${PROGRAMPATH} CreateLinepool ${1}
elif [[ ${lpool_model} == k_shortest_paths ]]; then
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/lp-helper${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH} CreateLinepoolSP ${1}
elif [[ ${lpool_model} == restricted_line_duration ]]; then
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/lp-helper${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH}/../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/../line-planning/cost-model/cost-model.jar${PATHSEP}${PROGRAMPATH} CreateLinepoolDurationRestrictions ${1}
else
	echo "Error: Invalid lpool_model argument: ${lpool_model}"
	exit 1
fi

EXITSTATUS=$?

exit ${EXITSTATUS}

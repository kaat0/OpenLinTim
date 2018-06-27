#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

load_model=`${CONFIGCMD} -s load_generator_model -u`

if [[ ${load_model} == LOAD_FROM_PTN ]]; then
    ant -q -f ${PROGRAMPATH}/build.xml build
    java -Djava.util.logging.config.file=${PROGRAMPATH}/../../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/build${PATHSEP}${PROGRAMPATH}/../../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH}/../../core/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/../../line-planning/cost-model/cost-model.jar net.lintim.main.tools.PTNLoadGeneratorMain basis/Config.cnf

elif [[ ${load_model} == LOAD_FROM_EAN ]]; then
    sh ${PROGRAMPATH}/../../essentials/javatools/runner.sh RegenerateLoad basis/Config.cnf

else
	echo "Error: Invalid load_generator_model argument: ${load_model}"
	exit 1
fi


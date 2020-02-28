#!/usr/bin/env bash

set -e

PROGRAMPATH=`dirname ${0}`

source ${PROGRAMPATH}/../../base.sh

ant -q -f "${PROGRAMPATH}/custom-build.xml" || exit 1

MOD_CLASSPATH=${CLASSPATH}${PATHSEP}${PROGRAMPATH}/bin${PATHSEP}../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}../../libs/apache-commons/commons-math-2.1.jar${PATHSEP}../../libs/super-csv/super-csv-2.4.0.jar

java -cp ${MOD_CLASSPATH} net.lintim.main.${1} ${2}

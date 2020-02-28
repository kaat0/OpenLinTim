#!/usr/bin/env bash

PROGRAMPATH=`dirname ${0}`

source ${PROGRAMPATH}/../../base.sh

MOD_CLASSPATH=${CLASSPATH}${PATHSEP}${PROGRAMPATH}/bin

ant -q -f "${PROGRAMPATH}/build.xml" || exit 1

MOD_CLASSPATH=${MOD_CLASSPATH}${PATHSEP}${PROGRAMPATH}/../../essentials/javatools/lib/supercsv-with_src-1.52.jar
MOD_CLASSPATH=${MOD_CLASSPATH}${PATHSEP}${PROGRAMPATH}/../../essentials/javatools/bin
java -server -Xbatch -XX:+AggressiveOpts -cp ${MOD_CLASSPATH} net.lintim.main.${1} ${2}

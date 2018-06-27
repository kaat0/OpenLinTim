#!/bin/bash

PROGRAMPATH=`dirname ${0}`

source ${PROGRAMPATH}/../../base.sh
ant -q -f ${PROGRAMPATH}/build.xml build-rollout
java ${JFLAGS} -classpath ${CLASSPATH}${PATHSEP}${PROGRAMPATH}${PATHSEP}${PROGRAMPATH}/../config${PATHSEP}${PROGRAMPATH}/../dm-helper/Tools${PATHSEP}${PROGRAMPATH}/../dm-helper/EAN Rollout

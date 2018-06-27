#!/bin/bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh


ant -q -f ${PROGRAMPATH}/build.xml build-create-od
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../config${PATHSEP}${PROGRAMPATH}/../sl-helper/PTNTools${PATHSEP}${PROGRAMPATH}/../shortest-paths/src/${PATHSEP}${PROGRAMPATH}/../../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH} CreateOD $1


EXITSTATUS=$?

exit ${EXITSTATUS}
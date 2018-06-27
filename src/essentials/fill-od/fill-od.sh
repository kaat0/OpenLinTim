#!/bin/bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

ant -q -f ${PROGRAMPATH}/build.xml build-fill-od
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../config${PATHSEP}${PROGRAMPATH} FillOD $1


EXITSTATUS=$?

exit ${EXITSTATUS}
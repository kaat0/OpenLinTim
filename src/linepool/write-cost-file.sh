#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

ant -q -f ${PROGRAMPATH}/build.xml build-linepool
java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/lp-helper${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar${PATHSEP}${PROGRAMPATH} WriteCostFile ${1}


EXITSTATUS=$?

exit ${EXITSTATUS}

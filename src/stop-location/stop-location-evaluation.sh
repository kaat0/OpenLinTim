#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`
CONFIGPROG=${PROGRAMPATH}/../essentials/config/config.sh

source ${PROGRAMPATH}/../base.sh

ant -q -f ${PROGRAMPATH}/../essentials/shortest-paths/build.xml build

ant -q -f ${PROGRAMPATH}/evaluation/build.xml build-stop-location-evaluation
	java -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/sl-helper/PTNTools${PATHSEP}${PROGRAMPATH}/evaluation${PATHSEP}${PROGRAMPATH}/../essentials/shortest-paths/src/${PATHSEP}${PROGRAMPATH}/../essentials/statistic${PATHSEP}${PROGRAMPATH}/../../libs/jgrapht/jgrapht-core-1.1.0.jar EvaluateSL ${2}


EXITSTATUS=$?

exit ${EXITSTATUS}

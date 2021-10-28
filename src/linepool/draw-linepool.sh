#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

ant -q -f ${PROGRAMPATH}/build.xml build-linepool
java "${JFLAGS[@]}" -cp ${CLASSPATH}${PATHSEP}${CORE_DIR}/java/lintim-core.jar${PATHSEP}${PROGRAMPATH}/../essentials/lp-helper${PATHSEP}${PROGRAMPATH} DrawLinepool "$@"



EXITSTATUS=$?

exit ${EXITSTATUS}

#!/usr/bin/env bash

PROGRAMPATH=`dirname ${0}`

source ${PROGRAMPATH}/../../base.sh

ant -q -f ${PROGRAMPATH}/build.xml build-circulations-to-ean
java "${JFLAGS[@]}" -classpath ${CLASSPATH}${PATHSEP}${SRC_DIR}/essentials/config${PATHSEP}${SRC_DIR}/essentials/dm-helper/Tools${PATHSEP}${SRC_DIR}/essentials/dm-helper/EAN${PATHSEP}${PROGRAMPATH}/ CirculationsToEAN

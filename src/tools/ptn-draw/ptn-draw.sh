#!/usr/bin/env bash
set -e

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

default_ptn_graph_file=`"${CONFIGCMD[@]}" -s default_ptn_graph_file -u`

ant -q -f ${PROGRAMPATH}/build.xml
java -Djava.util.logging.config.file=${PROGRAMPATH}/../../core/java/logging.properties -cp ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/build${PATHSEP}${PROGRAMPATH}/../../core/java/lintim-core.jar net.lintim.util.draw.TransformPtnToDot basis/Config.cnf

neato -n -Tpng graphics/ptn-graph.dot -o graphics/ptn-graph.png
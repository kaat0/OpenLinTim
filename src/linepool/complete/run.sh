#!/usr/bin/env bash
set -e
PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

mvn -f ${PROGRAMPATH}/pom.xml package
java "${JFLAGS[@]}" -jar ${PROGRAMPATH}/target/complete-linepool-1.0-SNAPSHOT.jar basis/Config.cnf

#MAVEN_OPTS=${JFLAGS[@]} mvn -f ${PROGRAMPATH}/pom.xml exec:java -Dexec.args="basis/Config.cnf"
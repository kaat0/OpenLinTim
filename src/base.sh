#!/usr/bin/env bash

declare -r SRC_DIR=`python3 -c 'import os,sys;print(os.path.dirname(os.path.realpath(sys.argv[1])))' ${BASH_SOURCE[0]}`

CORE_DIR="${SRC_DIR}/core"


PATHSEP=":"
if [[ $OSTYPE == "cygwin" ]] ; then
	PATHSEP=";"
fi

CONFIGPROG="${SRC_DIR}/essentials/config/config.sh"
# First, check if the shell script is available and executable
if [[ ! -x ${CONFIGPROG} ]]; then
	echo "Error: the config command is not usable. Make sure that "
	echo ${CONFIGPROG}
	echo "exists and is executable."
	exit 1;
fi
if [[ ${#} -eq 1 ]]; then
	if [[ -r ${1} ]]; then
		CONFIGCMD=("${CONFIGPROG}" -c "${1}") #"${CONFIGPROG} -c ${1}"
	else
		echo "Error: The first argument should be a configuration file, "\
		"but the file \"${1}\" either does not exist or is not readable."
		exit 1
	fi
else
	CONFIGCMD=("${CONFIGPROG}" -c "basis/Config.cnf")
	if [[ ! -e "basis/Config.cnf" ]]; then
		echo "Error: no configuration file found. Make sure that "\
		"basis/Config.cnf exists."
		exit 1;
	fi
fi

generic_64_bit_only=`"${CONFIGCMD[@]}" -s generic_64_bit_only -u`

if [ ${generic_64_bit_only} = "true" ] && [ ! `uname -m` = "x86_64" ]; then
	echo -n "Error: your system is 32 bit. Either get yourself a 64 bit "
	echo    "capable computer or set generic_64_bit_only to false."
	exit 1
fi

JFLAGS=(-XX:+UseParallelGC -Djava.util.logging.config.file="${SRC_DIR}/core/java/logging.properties")

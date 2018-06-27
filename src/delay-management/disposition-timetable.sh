#!/bin/bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../base.sh

dm_method=`${CONFIGCMD} -s DM_method -u`

if [[ ${dm_method} == "DM2" || ${dm_method} == "DM1" || ${dm_method} == "DM2-pre" ||   ${dm_method} == "FSFS" ||   ${dm_method} == "FRFS" ||   ${dm_method} == "EARLYFIX" ||   ${dm_method} == "PRIORITY" ||   ${dm_method} == "PRIOREPAIR" ||   ${dm_method} == "best-of-all" ||   ${dm_method} == "PASSENGERPRIOFIX" ||   ${dm_method} == "PASSENGERFIX" ||   ${dm_method} == "FIXFSFS" ||   ${dm_method} == "FIXFRFS" ||   ${dm_method} == "propagate" ]]; then
	make  --quiet -C ${PROGRAMPATH}/../essentials/config config_cmd
	${PROGRAMPATH}/ip-based/Solve/update_solver.sh
	ant -q -f ${PROGRAMPATH}/ip-based/build.xml build-delay-management
	java ${JFLAGS} -classpath ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/ip-based/Solve${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/dm-helper/Tools${PATHSEP}${PROGRAMPATH}/../essentials/dm-helper/EAN${PATHSEP}${PROGRAMPATH}/../essentials/statistic SolveDM
elif [[ ${dm_method} == "online-dm" ]]; then
	ant -q -f ${PROGRAMPATH}/online-dm/build.xml build-online-delay-management
	java ${JFLAGS} -classpath ${CLASSPATH}${PATHSEP}${PROGRAMPATH}/online-dm${PATHSEP}${PROGRAMPATH}/../essentials/config${PATHSEP}${PROGRAMPATH}/../essentials/dm-helper/Tools${PATHSEP}${PROGRAMPATH}/../essentials/dm-helper/EAN ODM
else
	echo "Error: Invalid dm_method argument: ${dm_method}"
	exit 1;
fi

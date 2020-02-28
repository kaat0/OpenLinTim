#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

#if [[ $# -lt 1 ]]; then
#    echo "Invocation: $0 [Config-file [dataset-db-name]]"
#    echo ""
#    echo "dataset-db-name defaults to dataset directory name unless provided"
#    echo "Warning: existing data for the dataset-db-name will be truncated!"
#    return
#fi

#DATASETDB=`dirname $1 | sed -e 's/.*\/datasets\/([^\/]+)\/.*/\1/p'`
DATASETDB=`pwd | sed -r 's/.*\/datasets\/([^\/]+)/\1/'`
if [[ $# -ge 2 ]]; then
    DATASETDB=$2
fi
DEMAND_FILE=`"${CONFIGCMD[@]}" -s default_demand_file -u`
STOPS_FILE=`"${CONFIGCMD[@]}" -s default_stops_file -u`
EDGES_FILE=`"${CONFIGCMD[@]}" -s default_edges_file -u`
OD_FILE=`"${CONFIGCMD[@]}" -s default_od_file -u`
STOPS_FILE=`"${CONFIGCMD[@]}" -s default_stops_file -u`

cat "${PROGRAMPATH}/insert-dataset.sql" | psql -v dataset="$DATASETDB"

if [[ -r ${DEMAND_FILE} ]]; then
    cat "${PROGRAMPATH}/import-demand-before.sql" "${DEMAND_FILE}" "${PROGRAMPATH}/import-demand-after.sql" | sed -e '/^\s*#/d;/^\s*$/d' | psql -v dataset=$DATASETDB
fi

if [[ -r ${STOPS_FILE} ]]; then
    cat "${PROGRAMPATH}/import-stops-before.sql" "${STOPS_FILE}" "${PROGRAMPATH}/import-stops-after.sql" | sed -e '/^\s*#/d;/^\s*$/d' | psql -v dataset=$DATASETDB
fi

if [[ -r ${EDGES_FILE} ]]; then
    cat "${PROGRAMPATH}/import-edges-before.sql" "${EDGES_FILE}" "${PROGRAMPATH}/import-edges-after.sql" | sed -e '/^\s*#/d;/^\s*$/d' | psql -v dataset=$DATASETDB
fi

if [[ -r ${OD_FILE} ]]; then
    cat "${PROGRAMPATH}/import-od-before.sql" "${OD_FILE}" "${PROGRAMPATH}/import-od-after.sql" | sed -e '/^\s*#/d;/^\s*$/d' | psql -v dataset=$DATASETDB
fi


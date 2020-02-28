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
STOPS_FILE=`"${CONFIGCMD[@]}" -s default_stops_file -u`

grep "^#" "${STOPS_FILE}" > "${STOPS_FILE}.new"
cat "${PROGRAMPATH}/export-stops-wgs84-to-gk3.sql" | psql -q -v dataset="$DATASETDB" -v srid=31467 >> "${STOPS_FILE}.new"
mv "${STOPS_FILE}" "${STOPS_FILE}.old"
mv "${STOPS_FILE}.new" "${STOPS_FILE}"

cat "${PROGRAMPATH}/export-stops-id-x-y.sql" | psql -q -v dataset="$DATASETDB" > "${STOPS_FILE}.geo"

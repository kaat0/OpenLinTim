#!/usr/bin/env bash
set -e

# Enter your data here
TARGET_DATASET=lowersaxony
COMMANDS_TO_RUN="make ptn-regenerate-load && make lpool-line-pool && make lc-line-concept && make ean && make tim-timetable && make ro-rollout && make ro-trips && make vs-vehicle-schedules && make ro-trips-evaluate && make vs-vehicle-schedules-evaluate && make tim-transform-to-visum && make ptn-draw"
ADDITIONAL_FILES_TO_COMPARE=("graphics/ptn-graph.dot" "timetabling/Timetable-visum-nodes.tim")

# STATIC PART OF THE SCRIPT
SCRIPT_LOCATION=`dirname $(readlink -f ${BASH_SOURCE[0]})`
echo ------------------------Executing test `basename ${SCRIPT_LOCATION}`
TIMESTAMP=`date +"%Y-%m-%d_%H-%M-%S"`
TARGET_LOCATION=../../datasets/${TARGET_DATASET}_${TIMESTAMP}
BASE_TARGET_LOCATION=../../datasets/${TARGET_DATASET}
cp -r ${BASE_TARGET_LOCATION} ${TARGET_LOCATION}
cp -r * ${TARGET_LOCATION}
cd ${TARGET_LOCATION}
eval ${COMMANDS_TO_RUN}
python3 ${SCRIPT_LOCATION}/../util/evaluate_statistics.py ${SCRIPT_LOCATION}/expected-statistic.sta statistic/statistic.sta
for file in "${ADDITIONAL_FILES_TO_COMPARE}"
do
    python3 ${SCRIPT_LOCATION}/../util/compare_files.py ${SCRIPT_LOCATION}/$file $file
done
echo "File comparison successful"
cd ${SCRIPT_LOCATION}
rm -rf ${TARGET_LOCATION}

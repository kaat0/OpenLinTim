#!/usr/bin/env bash
set -e

# Enter your data here
TARGET_DATASET=grid
COMMANDS_TO_RUN="make lc-line-concept && make lc-line-concept-evaluate"
ADDITIONAL_FILES_TO_COMPARE=()

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

    if [ ${file} ]; then
        python3 ${SCRIPT_LOCATION}/../util/compare_files.py ${SCRIPT_LOCATION}/$file $file
    fi
done
echo "File comparison successful"
cd ${SCRIPT_LOCATION}
rm -rf ${TARGET_LOCATION}

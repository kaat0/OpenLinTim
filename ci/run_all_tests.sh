#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"
# First, run the unit tests
bash run_unit_tests.sh
# Now run every test in this directory
count=0
for test_directory in `find . -maxdepth 1 -type d ! -name template ! -name util ! -name .`
do
    cd ${test_directory}
    if test -f "DISABLE"; then
        count=$((count+1))
    else
        ./run.sh
    fi
    cd ..
done
echo "Skipped ${count} tests"
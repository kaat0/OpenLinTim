#!/usr/bin/env bash
set -e

# Try to source xpress file, if it exists
if [[ -f ~/xpress.sh ]]; then
    echo "Manually enabling Xpress"
    source ~/xpress.sh
fi

cd "$(dirname "$0")"
# First, run the unit tests
bash run_unit_tests.sh
# Now run every test in this directory
python3 util/test_framework.py .
#!/usr/bin/env bash
set -e

if [[ -f ~/xpress.sh ]]; then
    echo "Manually enabling Xpress"
    source ~/xpress.sh
fi

cd "$(dirname "$0")"
# Now run every test in this directory
python3 util/test_framework.py $1
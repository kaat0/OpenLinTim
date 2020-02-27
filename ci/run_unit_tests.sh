#!/usr/bin/env bash
set -e
echo "Running python core unit tests"
cd ../src/core/python
python3 -m unittest
echo "Running java core unit tests"
cd ../java
ant -q build-tests
java -cp build/:../../../libs/junit/junit-4.12.jar:../../../libs/hamcrest/hamcrest-core-1.3.jar net.lintim.main.TestRunner
#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

cd ${PROGRAMPATH}/src/

DEST_FOLDER="bin/"

rm -r $DEST_FOLDER

# chekc if dest folder exisits, create if not
mkdir -p $DEST_FOLDER

# compile all shit to one location
javac -d $DEST_FOLDER MakeVehicleSchedule.java

# compile javadoc neat
#API_FOLDER="api/"
#rm -r $API_FOLDER

#mkdir -p $API_FOLDER

#javadoc -d $API_FOLDER MakeVehicleSchedule.java vehicleScheduling

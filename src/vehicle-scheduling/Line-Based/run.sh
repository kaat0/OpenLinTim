#! /bin/sh

PROGRAMPATH=`dirname $0`

source ${PROGRAMPATH}/../../base.sh

# Set the parameters

# param1 is the dataset for which the vehicle schedule should be computed
PARAM_ONE=${PWD}

# arameter param2 describes the program type and should be either 4,3 or 2
PARAM_TWO=`${CONFIGCMD} -s vs_line_based_method -u`

# param3 is a value for alpha
PARAM_THREE=`${CONFIGCMD} -s vs_line_based_alpha -u`

# Executes it

bash ${PROGRAMPATH}/build.sh

# Change to the folder bin

cd ${PROGRAMPATH}/src/bin/

# Execute the program
java MakeVehicleSchedule $PARAM_ONE $PARAM_TWO $PARAM_THREE

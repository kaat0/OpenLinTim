#!/usr/bin/env bash

PROGRAMPATH=`dirname $0`

#if [ $1 == ]; then
#
#echo "exec ${PROGRAMPATH}/my_bsp" | mosel -s
#fi

if [ "$1" == "mdm1" ]; then

echo "exec ${PROGRAMPATH}/mdm1" | mosel -s

elif [ "$1" == "mdm2" ]; then

echo "exec ${PROGRAMPATH}/mdm2" | mosel -s

elif [ "$1" == "am" ]; then

echo "exec ${PROGRAMPATH}/am" | mosel -s

elif [ "$1" == "tm" ]; then

echo "exec ${PROGRAMPATH}/tm" | mosel -s

elif [ "$1" == "nm" ]; then

echo "exec ${PROGRAMPATH}/nm" | mosel -s

elif [ "$1" == "mdm1_my_bsp" ]; then

echo "exec ${PROGRAMPATH}/my_bsp_mdm1" | mosel -s

elif [ "$1" == "mdm2_my_bsp" ]; then

echo "exec ${PROGRAMPATH}/my_bsp_mdm2" | mosel -s

elif [ "$1" == "am_my_bsp" ]; then

echo "exec ${PROGRAMPATH}/my_bsp_am" | mosel -s

elif [ "$1" == "tm_my_bsp" ]; then

echo "exec ${PROGRAMPATH}/my_bsp_tm" | mosel -s

elif [ "$1" == "nm_my_byp" ]; then

echo "exec ${PROGRAMPATH}/my_bsp_nm" | mosel -s

fi


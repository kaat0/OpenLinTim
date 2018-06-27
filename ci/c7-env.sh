#!/usr/bin/env bash
set -e

# Set environment variables for usage on c7
if [ -z "$XPRESSDIR" ]; then
    export XPRESSDIR=/opt/xpressmp
fi
if [ -z "$XPRESS" ]; then
    export XPRESS=/opt/xpressmp/bin
fi
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${XPRESSDIR}/lib
export DYLD_LIBRARY_PATH=${XPRESSDIR}/lib:${DYLD_LIBRARY_PATH}
export SHLIB_PATH=${XPRESSDIR}/lib:${SHLIB_PATH}
export LIBPATH=${XPRESSDIR}/lib:${LIBPATH}
export PYTHONPATH=${XPRESSDIR}/lib:${PYTHONPATH}
export CLASSPATH=${XPRESSDIR}/lib/xprs.jar:${CLASSPATH}
export CLASSPATH=${XPRESSDIR}/lib/xprb.jar:${CLASSPATH}
export CLASSPATH=${XPRESSDIR}/lib/xprm.jar:${CLASSPATH}
export PATH=${XPRESSDIR}/bin:${PATH}

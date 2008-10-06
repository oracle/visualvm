#!/bin/sh

# This script expects CVM_HOME to point to the correct CVM installation
# In case you need to customize it, please uncomment and modify the following lines

# CVM_HOME=/opt/cvm
# export CVM_HOME

OLD_PWD=`pwd`
cd `dirname $0`
INSTALL_DIR=`pwd`
cd $OLD_PWD
unset OLD_PWD

$CVM_HOME/bin/cvm -Djava.library.path=$CVM_HOME/lib:$INSTALL_DIR/../lib/deployed/cvm/linux  -classpath $INSTALL_DIR/../lib/jfluid-server.jar:$INSTALL_DIR/../lib/jfluid-server-cvm.jar org.netbeans.lib.profiler.server.ProfilerCalibrator

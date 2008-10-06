#!/bin/sh

# This script expects CVM_HOME to point to the correct CVM installation
# In case you need to customize it, please uncomment and modify the following lines

# CVM_HOME=/opt/cvm
# export CVM_HOME

# Determine the location of the profile script as an absolute directory
ORIG_DIR=`pwd`
PROG_NAME=`type $0 | awk '{print $3}'`
INSTALL_DIR=`dirname $PROG_NAME`
cd $INSTALL_DIR
INSTALL_DIR=`pwd`
cd $ORIG_DIR

$CVM_HOME/bin/cvm -agentpath:$INSTALL_DIR/../lib/deployed/cvm/linux/libprofilerinterface.so=$INSTALL_DIR/../lib/,5140 $@

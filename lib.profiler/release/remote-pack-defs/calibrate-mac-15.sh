#!/bin/sh

# This script expects JAVA_HOME to point to the correct JDK 5.0 installation
# In case you need to customize it, please uncomment and modify the following lines

JAVA_HOME=/Library/Java/Home
export JAVA_HOME

OLD_PWD=`pwd`
cd `dirname $0`
INSTALL_DIR=`pwd`
cd $OLD_PWD
unset OLD_PWD

$JAVA_HOME/bin/java -javaagent:$INSTALL_DIR/../lib/jfluid-server-15.jar org.netbeans.lib.profiler.server.ProfilerCalibrator

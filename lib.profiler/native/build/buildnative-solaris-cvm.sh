#!/usr/bin/bash
export PROC=`uname -p`
echo PROC is $PROC

if [ $PROC = "i386" ]; then 
  export PROC_FLAGS="-xregs=no%frameptr"
elif [ $PROC = "sparc" ]; then
  export PROC_FLAGS="-xregs=no%appl -xmemalign=4s -xarch=v8"
else 
  echo "Invalid architecture " $PROC
fi
mkdir -p ../../dist/deployed/cvm/solaris

CC_FLAGS="-I$CVM_HOME/src/share/javavm/export -I$CVM_HOME/src/share \
-I$CVM_HOME/src/solaris -I$CVM_HOME/src -I$CVM_HOME/src/solaris-x86 \
-DSOLARIS -G -lrt -xO2 -v -mt -xc99=%none -xCC -Xa -xstrconst"
 
cc $CC_FLAGS $PROC_FLAGS \
-o ../../dist/deployed/cvm/solaris-$PROC/libprofilerinterface.so \
../src-jdk15/class_file_cache.c \
../src-jdk15/attach.c \
../src-jdk15/Classes.c \
../src-jdk15/Timers.c \
../src-jdk15/GC.c \
../src-jdk15/Threads.c \
../src-jdk15/Stacks.c \
../src-jdk15/common_functions.c

cc $CC_FLAGS -g $PROC_FLAGS \
-o ../../dist/deployed/cvm/solaris-$PROC/libprofilerinterface_g.so \
../src-jdk15/class_file_cache.c \
../src-jdk15/attach.c \
../src-jdk15/Classes.c \
../src-jdk15/Timers.c \
../src-jdk15/GC.c \
../src-jdk15/Threads.c \
../src-jdk15/Stacks.c \
../src-jdk15/common_functions.c


cc $CC_FLAGS $PROC_FLAGS \
-o ../../dist/deployed/cvm/solaris-$PROC/libclient.so \
../src/ProfilerClient.c

cc $CC_FLAGS -g $PROC_FLAGS \
-o ../../dist/deployed/cvm/solaris-$PROC/libclient_g.so \
../src/ProfilerClient.c

rm -f *.o

#!/bin/sh

BuildForCVM()
{
        CVM_HOME=$1
        JDK_ID=$2
        echo $CVM_HOME $JDK_ID
	gcc -I$CVM_HOME/src/share/javavm/export -I$CVM_HOME/src/share -I$CVM_HOME/src/linux -I$CVM_HOME/src -I$CVM_HOME/src/linux-x86 -DLINUX -DCVM -pthread -fPIC -shared -O3 -Wall \
        -o ../../release/lib/deployed/$JDK_ID/linux/libprofilerinterface.so \
        ../src-jdk15/class_file_cache.c \
        ../src-jdk15/attach.c \
        ../src-jdk15/Classes.c \
        ../src-jdk15/Timers.c \
        ../src-jdk15/GC.c \
        ../src-jdk15/Threads.c \
        ../src-jdk15/Stacks.c \
        ../src-jdk15/common_functions.c

        rm -f *.o

	gcc -I$CVM_HOME/src/share/javavm/export -I$CVM_HOME/src/share -I$CVM_HOME/src/linux -I$CVM_HOME/src -I$CVM_HOME/src/linux-x86 -DLINUX -DCVM -pthread -fPIC -shared -Wall  -g \
        -o ../../release/lib/deployed/$JDK_ID/linux/libprofilerinterface_g.so \
        ../src-jdk15/class_file_cache.c \
        ../src-jdk15/attach.c \
        ../src-jdk15/Classes.c \
        ../src-jdk15/Timers.c \
        ../src-jdk15/GC.c \
        ../src-jdk15/Threads.c \
        ../src-jdk15/Stacks.c \
        ../src-jdk15/common_functions.c

        rm -f *.o
}

mkdir -p ../../release/lib/deployed/cvm/linux

BuildForCVM "$CVM_HOME" "cvm"


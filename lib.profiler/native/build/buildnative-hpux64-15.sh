#!/bin/sh

BuildForJDK()
{
        JAVA_HOME=$1
        JDK_ID=$2
	echo $JAVA_HOME $JDK_ID
	cc -I$JAVA_HOME/include -I$JAVA_HOME/include/hp-ux +DD64 -Ae +u4 +z -D_HPUX -mt -O3 -Wall \
	-b -o ../../release/lib/deployed/$JDK_ID/hpux-pa_risc2.0w/libprofilerinterface.sl \
	../src-jdk15/class_file_cache.c \
	../src-jdk15/attach.c \
	../src-jdk15/Classes.c \
	../src-jdk15/HeapDump.c \
	../src-jdk15/Timers.c \
	../src-jdk15/GC.c \
	../src-jdk15/Threads.c \
	../src-jdk15/Stacks.c \
	../src-jdk15/common_functions.c

	rm -f *.o
}

BuildForJDK "$JAVA_HOME_15" "jdk15"
BuildForJDK "$JAVA_HOME_16" "jdk16"


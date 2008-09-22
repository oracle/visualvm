#!/usr/bin/bash

BuildForJDK()
{
        JAVA_HOME=$1
        JDK_ID=$2
        echo $JAVA_HOME $JDK_ID
	CC_FLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/solaris -DSOLARIS -G -lrt \
	-xO2 -v -mt -xc99=%none -xCC -Xa -xstrconst"

	cc $CC_FLAGS $PROC_FLAGS \
	-o ../../release/lib/deployed/$JDK_ID/solaris-$PROC/libprofilerinterface.so \
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

export PROC=`uname -p`
echo PROC is $PROC

if [ $PROC = "i386" ]; then 
  export PROC_FLAGS="-xregs=no%frameptr"
elif [ $PROC = "sparc" ]; then
  export PROC_FLAGS="-xregs=no%appl -xmemalign=4s -xarch=v8"
else 
  echo "Invalid architecture " $PROC
fi

BuildForJDK "$JAVA_HOME_15" "jdk15"
BuildForJDK "$JAVA_HOME_16" "jdk16"


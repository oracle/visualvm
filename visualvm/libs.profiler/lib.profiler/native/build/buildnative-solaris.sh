#!/usr/bin/bash

# Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

BuildForJDK()
{
        JAVA_HOME=$1
        JDK_ID=$2
        echo $JAVA_HOME $JDK_ID
        # Use Solaris Studio 12
	CC_FLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/solaris -DSOLARIS -G -lrt \
	-xO2 -v -mt -xc99=none -Kpic -xCC -Xa -xstrconst"

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


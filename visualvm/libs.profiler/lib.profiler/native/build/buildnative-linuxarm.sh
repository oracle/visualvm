#!/bin/sh

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
        ARCH_DIR=$3
        ARCH_TOOLS=$4
        ARCH_FLAGS=$5
	echo $JAVA_HOME $JDK_ID $ARCH_DIR $ARCH_TOOLS $ARCH_FLAGS
	$ARCH_TOOLS/bin/gcc -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -DLINUX -pthread -fPIC -shared -O3 $ARCH_FLAGS -Wall \
	-o ../../release/lib/deployed/$JDK_ID/$ARCH_DIR/libprofilerinterface.so \
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

BuildForJDK "$JAVA_HOME_16" "jdk16" "linux-arm" "$ARM_SFLT_TOOLS" "-msoft-float -march=armv5t"
BuildForJDK "$JAVA_HOME_16" "jdk16" "linux-arm-vfp-hflt" "$ARM_VFP_HFLT_TOOLS" "-marm -mfloat-abi=hard -mfpu=vfp"



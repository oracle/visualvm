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
	SYSROOT=$1
	JDK_ID=$2
	JDK_VER=$3
	echo $SYSROOT $JDK_ID $SYSROOT/System/Library/Frameworks/JavaVM.framework/Versions/$JDK_VER/Headers

	CC_OPTS="-c -isystem $SYSROOT/System/Library/Frameworks/JavaVM.framework/Versions/$JDK_VER/Headers \
        -pipe -Wno-trigraphs -fpascal-strings -fasm-blocks \
	-Os -Wunused-variable -fmessage-length=0 -mmacosx-version-min=10.4 -Wmost \
	-Wno-four-char-constants -Wno-unknown-pragmas -isysroot $SYSROOT -DLINUX"

	LD_OPTS="-framework JavaVM  -Wl,-single_module -compatibility_version 1 -current_version 1 \
	-dynamiclib -mmacosx-version-min=10.4 -isysroot $SYSROOT"

	SOURCES="../src-jdk15/class_file_cache.c \
	../src-jdk15/attach.c \
	../src-jdk15/Classes.c \
	../src-jdk15/HeapDump.c \
	../src-jdk15/Timers.c \
	../src-jdk15/GC.c \
	../src-jdk15/Threads.c \
	../src-jdk15/Stacks.c \
	../src-jdk15/common_functions.c"

	OBJ_FILES="class_file_cache.o \
	attach.o \
	Classes.o \
	HeapDump.o \
	Timers.o \
	GC.o \
	Threads.o \
	Stacks.o \
	common_functions.o"

	BuildForArch "ppc"
        BuildForArch "ppc64"
	BuildForArch "i386"
        BuildForArch "x86_64"

	lipo -create libprofilerinterface_ppc.jnilib libprofilerinterface_i386.jnilib \
	libprofilerinterface_ppc64.jnilib libprofilerinterface_x86_64.jnilib \
	-output ../../release/lib/deployed/$JDK_ID/mac/libprofilerinterface.jnilib

	rm *.jnilib
}

BuildForArch()
{
        ARCH=$1
        echo "ARCH "$ARCH

        gcc-4.0 $CC_OPTS -arch $ARCH $SOURCES
        gcc-4.0 $LD_OPTS -arch $ARCH -o libprofilerinterface_$ARCH.jnilib $OBJ_FILES

        rm *.o
}

BuildForJDK "/Developer/SDKs/MacOSX10.4u.sdk" "jdk15" "1.5"
BuildForJDK "/Developer/SDKs/MacOSX10.5.sdk" "jdk16" "1.6"


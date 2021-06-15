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

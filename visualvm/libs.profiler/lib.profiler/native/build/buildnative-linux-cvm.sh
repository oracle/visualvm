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


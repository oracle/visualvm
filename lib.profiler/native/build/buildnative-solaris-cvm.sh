#!/usr/bin/bash

# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
#
# Oracle and Java are registered trademarks of Oracle and/or its affiliates.
# Other names may be trademarks of their respective owners.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common
# Development and Distribution License("CDDL") (collectively, the
# "License"). You may not use this file except in compliance with the
# License. You can obtain a copy of the License at
# http://www.netbeans.org/cddl-gplv2.html
# or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
# specific language governing permissions and limitations under the
# License.  When distributing the software, include this License Header
# Notice in each file and include the License file at
# nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the GPL Version 2 section of the License file that
# accompanied this code. If applicable, add the following below the
# License Header, with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#
# Contributor(s):
#
# The Original Software is NetBeans. The Initial Developer of the Original
# Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
# Microsystems, Inc. All Rights Reserved.
#
# If you wish your version of this file to be governed by only the CDDL
# or only the GPL Version 2, indicate your decision by adding
# "[Contributor] elects to include this software in this distribution
# under the [CDDL or GPL Version 2] license." If you do not indicate a
# single choice of license, a recipient has the option to distribute
# your version of this file under either the CDDL, the GPL Version 2 or
# to extend the choice of license to its licensees as provided above.
# However, if you add GPL Version 2 code and therefore, elected the GPL
# Version 2 license, then the option applies only if the new code is
# made subject to such option by the copyright holder.

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

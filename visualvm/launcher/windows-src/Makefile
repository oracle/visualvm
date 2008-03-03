# -*- Makefile -*-
#
# Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Sun designates this
# particular file as subject to the "Classpath" exception as provided
# by Sun in the LICENSE file that accompanied this code.
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
# Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
# CA 95054 USA or visit www.sun.com if you need additional information or
# have any questions.
#

LIBS =
#LIBS = advapi32.lib user32.lib

CFLAGS = -s -mno-cygwin
#!ifdef DEBUG
#	CFLAGS = $(CFLAGS) -Zi
#!endif

all : visualvm.exe visualvm_w.exe

visualvm.exe : visualvm.cpp visualvm.res
	gcc $(CFLAGS) $(LIBS) -mwindows -DWINMAIN visualvm.cpp -o visualvm.exe visualvm.res
	mv visualvm.exe ../

visualvm_w.exe : visualvm.cpp
	gcc $(CFLAGS) $(LIBS) visualvm.cpp -o visualvm_w.exe
	mv visualvm_w.exe ../

visualvm.res : visualvm.rc
	windres -o$@ -Ocoff visualvm.rc

clean :
	-rm *.res *.obj *.pdb *.ilk *~ 2>NUL

distclean : clean
	-rm *.exe 2>NUL

test :
	../visualvm.exe

test_w :
	../visualvm_w.exe

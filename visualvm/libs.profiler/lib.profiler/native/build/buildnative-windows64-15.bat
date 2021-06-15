rem Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
rem DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
rem
rem This code is free software; you can redistribute it and/or modify it
rem under the terms of the GNU General Public License version 2 only, as
rem published by the Free Software Foundation.  Oracle designates this
rem particular file as subject to the "Classpath" exception as provided
rem by Oracle in the LICENSE file that accompanied this code.
rem
rem This code is distributed in the hope that it will be useful, but WITHOUT
rem ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
rem FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
rem version 2 for more details (a copy is included in the LICENSE file that
rem accompanied this code).
rem
rem You should have received a copy of the GNU General Public License version
rem 2 along with this work; if not, write to the Free Software Foundation,
rem Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
rem
rem Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
rem or visit www.oracle.com if you need additional information or have any
rem questions.

SET BUILD_SRC_15=..\src-jdk15
SET BUILD_SRC=..\src
SET BUILD_JDK=C:\PROGRA~1\java\jdk1.5.0_15
SET BUILD_OUTPUT=%TEMP%\dist
SET BUILD_DEPLOY=..\..\release\lib

mkdir %BUILD_OUTPUT%\deployed\jdk15\windows-amd64

cl /I%BUILD_JDK%\include /I%BUILD_JDK%\include\win32 ^
%BUILD_SRC_15%\class_file_cache.c ^
%BUILD_SRC_15%\attach.c ^
%BUILD_SRC_15%\Classes.c ^
%BUILD_SRC_15%\HeapDump.c ^
%BUILD_SRC_15%\Timers.c ^
%BUILD_SRC_15%\GC.c ^
%BUILD_SRC_15%\Threads.c ^
%BUILD_SRC_15%\Stacks.c ^
%BUILD_SRC_15%\common_functions.c ^
/D WIN32 /MD /Ox /c

rc /fo version.res %BUILD_SRC_15%\windows\version.rc

link /DLL /MAP:%BUILD_OUTPUT%\deployed\jdk15\windows-amd64\profilerinterface.map /OUT:%BUILD_OUTPUT%\deployed\jdk15\windows-amd64\profilerinterface.dll ^
Classes.obj HeapDump.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj version.res

del vc60.pdb
del *.obj *.res
del %BUILD_OUTPUT%\deployed\jdk15\windows-amd64\*.lib %BUILD_OUTPUT%\deployed\jdk15\windows-amd64\*.exp %BUILD_OUTPUT%\deployed\jdk15\windows-amd64\*.ilk %BUILD_OUTPUT%\deployed\jdk15\windows-amd64\*.pdb

copy %BUILD_OUTPUT%\deployed\jdk15\windows-amd64\*.dll %BUILD_DEPLOY%\deployed\jdk15\windows-amd64
copy %BUILD_OUTPUT%\deployed\jdk15\windows-amd64\*.map %BUILD_DEPLOY%\deployed\jdk15\windows-amd64

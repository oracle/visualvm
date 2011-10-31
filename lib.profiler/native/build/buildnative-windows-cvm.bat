rem DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
rem
rem Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
rem
rem Oracle and Java are registered trademarks of Oracle and/or its affiliates.
rem Other names may be trademarks of their respective owners.
rem
rem The contents of this file are subject to the terms of either the GNU
rem General Public License Version 2 only ("GPL") or the Common
rem Development and Distribution License("CDDL") (collectively, the
rem "License"). You may not use this file except in compliance with the
rem License. You can obtain a copy of the License at
rem http://www.netbeans.org/cddl-gplv2.html
rem or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
rem specific language governing permissions and limitations under the
rem License.  When distributing the software, include this License Header
rem Notice in each file and include the License file at
rem nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
rem particular file as subject to the "Classpath" exception as provided
rem by Oracle in the GPL Version 2 section of the License file that
rem accompanied this code. If applicable, add the following below the
rem License Header, with the fields enclosed by brackets [] replaced by
rem your own identifying information:
rem "Portions Copyrighted [year] [name of copyright owner]"
rem
rem Contributor(s):
rem
rem The Original Software is NetBeans. The Initial Developer of the Original
rem Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
rem Microsystems, Inc. All Rights Reserved.
rem
rem If you wish your version of this file to be governed by only the CDDL
rem or only the GPL Version 2, indicate your decision by adding
rem "[Contributor] elects to include this software in this distribution
rem under the [CDDL or GPL Version 2] license." If you do not indicate a
rem single choice of license, a recipient has the option to distribute
rem your version of this file under either the CDDL, the GPL Version 2 or
rem to extend the choice of license to its licensees as provided above.
rem However, if you add GPL Version 2 code and therefore, elected the GPL
rem Version 2 license, then the option applies only if the new code is
rem made subject to such option by the copyright holder.

SET BUILD_SRC_15=..\src-jdk15
SET BUILD_OUTPUT=dist
SET BUILD_DEPLOY=..\..\release\lib

set VS2005_COMMON_PATH=C:\Program Files\Microsoft Visual Studio 8\vc\bin

call "%VS2005_COMMON_PATH%\vcvars32"

mkdir %BUILD_OUTPUT%\deployed\cvm\windows

cl /I%CVM_HOME%/src/share/javavm/export /I%CVM_HOME%/src/share /I%CVM_HOME%/src ^
/I%CVM_HOME%/src/win32-x86 /I%CVM_HOME%/src/win32 ^
%BUILD_SRC_15%\class_file_cache.c ^
%BUILD_SRC_15%\attach.c ^
%BUILD_SRC_15%\Classes.c ^
%BUILD_SRC_15%\Timers.c ^
%BUILD_SRC_15%\GC.c ^
%BUILD_SRC_15%\Threads.c ^
%BUILD_SRC_15%\Stacks.c ^
%BUILD_SRC_15%\common_functions.c ^
/D WIN32 /D CVM /MD /Ox /c

link /DLL /MAP:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.map /OUT:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.dll ^
Classes.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj
mt.exe -nologo -manifest %BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.dll.manifest -outputresource:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface.dll;#2

cl /I%CVM_HOME%/src/share/javavm/export /I%CVM_HOME%/src/share /I%CVM_HOME%/src ^
/I%CVM_HOME%/src/win32-x86 /I%CVM_HOME%/src/win32 ^
%BUILD_SRC_15%\class_file_cache.c ^
%BUILD_SRC_15%\attach.c ^
%BUILD_SRC_15%\Classes.c ^
%BUILD_SRC_15%\Timers.c ^
%BUILD_SRC_15%\GC.c ^
%BUILD_SRC_15%\Threads.c ^
%BUILD_SRC_15%\Stacks.c ^
%BUILD_SRC_15%\common_functions.c ^
/D WIN32 /D CVM /MDd /Zi /c

link /DLL /DEBUG /MAP:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.map /OUT:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.dll ^
Classes.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj
mt.exe -nologo -manifest %BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.dll.manifest -outputresource:%BUILD_OUTPUT%\deployed\cvm\windows\profilerinterface_g.dll;#2

del vc60.pdb
del *.obj
del %BUILD_OUTPUT%\deployed\cvm\windows\*.lib %BUILD_OUTPUT%\deployed\cvm\windows\*.exp %BUILD_OUTPUT%\deployed\cvm\windows\*.ilk %BUILD_OUTPUT%\deployed\cvm\windows\*.pdb

copy %BUILD_OUTPUT%\deployed\cvm\windows\*.dll %BUILD_DEPLOY%\deployed\cvm\windows
copy %BUILD_OUTPUT%\deployed\cvm\windows\*.map %BUILD_DEPLOY%\deployed\cvm\windows


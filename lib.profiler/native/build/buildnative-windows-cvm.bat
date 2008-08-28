
SET BUILD_SRC_15=..\src-jdk15
SET BUILD_JDK=C:\PROGRA~1\java\jdk1.5.0_10
SET BUILD_DEPLOY=..\..\release\lib

set VS2005_COMMON_PATH=C:\Program Files\Microsoft Visual Studio 8\vc\bin

call "%VS2005_COMMON_PATH%\vcvars32"

mkdir %BUILD_DEPLOY%\deployed\cvm\windows

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

link /DLL /MAP:%BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface.map /OUT:%BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface.dll ^
Classes.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj
mt.exe -nologo -manifest %BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface.dll.manifest -outputresource:%BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface.dll;#2

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

link /DLL /DEBUG /MAP:%BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface_g.map /OUT:%BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface_g.dll ^
Classes.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj
mt.exe -nologo -manifest %BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface_g.dll.manifest -outputresource:%BUILD_DEPLOY%\deployed\cvm\windows\profilerinterface_g.dll;#2


del *.obj


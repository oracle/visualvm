
SET BUILD_SRC_15=..\src-jdk15
SET BUILD_JDK=C:\PROGRA~1\java\jdk1.6.0_16
SET BUILD_OUTPUT=dist
SET BUILD_DEPLOY=..\..\release\lib

mkdir %BUILD_OUTPUT%\deployed\jdk16\windows

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

link /DLL /MAP:%BUILD_OUTPUT%\deployed\jdk16\windows\profilerinterface.map /OUT:%BUILD_OUTPUT%\deployed\jdk16\windows\profilerinterface.dll ^
Classes.obj HeapDump.obj Timers.obj GC.obj Threads.obj Stacks.obj common_functions.obj class_file_cache.obj attach.obj

del vc60.pdb
del *.obj
del %BUILD_OUTPUT%\deployed\jdk16\windows\*.lib %BUILD_OUTPUT%\deployed\jdk16\windows\*.exp %BUILD_OUTPUT%\deployed\jdk16\windows\*.ilk %BUILD_OUTPUT%\deployed\jdk16\windows\*.pdb

copy %BUILD_OUTPUT%\deployed\jdk16\windows\*.dll %BUILD_DEPLOY%\deployed\jdk16\windows
copy %BUILD_OUTPUT%\deployed\jdk16\windows\*.map %BUILD_DEPLOY%\deployed\jdk16\windows

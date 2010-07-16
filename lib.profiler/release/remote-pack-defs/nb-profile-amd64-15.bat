@echo off

REM This script expects that NetBeans IDE runs under JDK 5.0 update 4 (or later) by default.
REM If you need to specify the JDK explicitely, you can force the IDE to start with specific JDK
REM by passing -jdkhome <path> parameter to nb.exe/netbeans.exe e.g. "nb.exe -jdkhome C:\Software\jdk15_04"

nb.exe -J-agentpath:"%~dp0\..\profiler\lib\deployed\jdk15\windows-amd64\profilerinterface.dll"="\"%~dp0\..\profiler\lib\"",5140 %*

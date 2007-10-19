@echo off

REM This script expects JAVA_HOME to point to the correct JDK 6.0 installation
REM In case you need to customize it, please uncomment and modify the following line
REM set JAVA_HOME=C:\Software\jdk16

"%JAVA_HOME%\bin\java.exe" -agentpath:"%~dp0\..\lib\deployed\jdk16\windows\profilerinterface.dll"="\"%~dp0\..\lib\"",5140 %*

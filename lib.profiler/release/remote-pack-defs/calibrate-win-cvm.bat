@echo off

REM This script expects CVM_HOME to point to the correct CVM installation
REM In case you need to customize it, please uncomment and modify the following line
REM set CVM_HOME=C:\Software\CVM


"%CVM_HOME%\bin\cvm.exe" -cp "%~dp0\..\lib\jfluid-server.jar;%~dp0\..\lib\jfluid-server-cvm.jar" -Djava.library.path="%CVM_HOME%\lib;%~dp0\..\lib\deployed\cvm\windows" org.netbeans.lib.profiler.server.ProfilerCalibrator

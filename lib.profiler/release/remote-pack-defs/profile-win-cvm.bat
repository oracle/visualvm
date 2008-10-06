@echo off

REM This script expects CVM_HOME to point to the correct CVM installation
REM In case you need to customize it, please uncomment and modify the following line
REM set CVM_HOME=C:\Software\CVM

"%CVM_HOME%\bin\cvm.exe" -agentpath:"%~dp0\..\lib\deployed\cvm\windows\profilerinterface.dll"="\"%~dp0\..\lib\"",5140 %*

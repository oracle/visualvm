rem A script to generate JNI *.h files for classes that contain native methods

SET BUILD_SRC_15=..\src-jdk15
SET BUILD_JDK=C:\PROGRA~1\java\jdk1.5.0_10

%BUILD_JDK%\bin\javah -d %BUILD_SRC_15% -classpath ..\..\src;..\..\src-jdk15 org.netbeans.lib.profiler.server.system.Classes org.netbeans.lib.profiler.server.system.HeapDump org.netbeans.lib.profiler.server.system.GC org.netbeans.lib.profiler.server.system.Timers org.netbeans.lib.profiler.server.system.Stacks org.netbeans.lib.profiler.server.system.Threads

/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.jfluid;

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.client.RuntimeProfilingPoint;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.jfluid.marker.Marker;
import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;


/**
 * Global profiler session and engine settings, that are used in various parts of the system.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public final class ProfilerEngineSettings implements CommonConstants, Cloneable {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private InstrumentationFilter instrumentationFilter = new InstrumentationFilter();
    private Marker methodMarker = Marker.DEFAULT;
    private String jFluidRootDirName;
    private String jvmArgs; // the arguments for JVM running the TA
    private String mainArgs; // the TA arguments
    private String mainClassName; // the TA main class name

    // Target application setup-specific settings
    private String mainClassPath = ""; // NOI18N           // the classpath for starting TA
    private String remoteHost = ""; // NOI18N                  // Remote host name in case of remote profiling, "" for local profiling
    private String targetJDKVersion = Platform.JDK_15_STRING; // the target JVM version string: see Platform.JDK_xx_STRING constants
    private String targetJVMExeFile; // the JVM executable to run the TA
    private String targetJVMStartupDirName; // the working directory for TA we are attaching to
    private String workingDir = System.getProperty("user.dir"); // NOI18N // the TA working directory
    private ClientUtils.SourceCodeSelection[] instrumentationRootMethods = new ClientUtils.SourceCodeSelection[0];
    private RuntimeProfilingPoint[] profilingPoints = new RuntimeProfilingPoint[0];

    // THE FOLLOWING DATA IS REGENERATED ON EACH NEW JFLUID RUN AND/OR PROFILING SESSION WITHIN ONE RUN
    private String[] vmClassPaths = new String[3]; // Target VM's java.class.path, java.ext.dirs and sun.boot.class.path
    private boolean absoluteTimerOn = true;
    private boolean dontShowZeroLiveObjAllocPaths = true;
    private boolean excludeWaitTime = true;
    private boolean instrumentEmptyMethods = false;

    // JFluid and instrumentation-specific settings
    private boolean instrumentGetterSetterMethods = false;
    private boolean instrumentMethodInvoke = true;
    private boolean instrumentSpawnedThreads = false;
    private boolean runGCOnGetResultsInMemoryProfiling = false;
    private boolean instrumentObjectInit = false;

    // If false, the exec command is issued so that there is no visible console for the TA
    private boolean separateConsole = true;
    private boolean sortResultsByThreadCPUTime = false;
    private boolean suspendTargetApp = false;
    private boolean targetWindowRemains = false;
    private boolean threadCPUTimerOn = false;
    private boolean threadsMonitoringEnabled;
    private boolean lockContentionMonitoringEnabled;
    private boolean threadsSamplingEnabled;
    private int allocStackTraceLimit = -5; // Negative number means full (unlimited) depth actually used, although the limit is preserved
    private int maxStringLength = MAX_STRING_LENGTH_DEFAULT;
    private int allocTrackEvery = 10;
    private int architecture; // system architecture 32bit/64bit
    private int codeRegionCPUResBufSize = 1000;
    private int cpuProfilingType = CPU_INSTR_FULL;
    private int instrScheme = INSTRSCHEME_LAZY; // See CommonConstants for definitions
    private int nProfiledThreadsLimit = 32;
    private int stackDepthLimit = Integer.MAX_VALUE;
    private int portNo = 5140;
    private int samplingInterval = 10;
    private int samplingFrequency = 10;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAbsoluteTimerOn(boolean v) {
        absoluteTimerOn = v;
    }

    public boolean getAbsoluteTimerOn() {
        return absoluteTimerOn;
    }

    public void setAllocStackTraceLimit(int depth) {
        allocStackTraceLimit = depth;
    }

    public int getAllocStackTraceLimit() {
        return allocStackTraceLimit;
    }

    public void setAllocTrackEvery(int interval) {
        if (interval < 1) {
            interval = 1;
        }

        allocTrackEvery = interval;
    }

    public int getAllocTrackEvery() {
        return allocTrackEvery;
    }

    public void setCPUProfilingType(int v) {
        cpuProfilingType = v;
    }

    public int getCPUProfilingType() {
        return cpuProfilingType;
    }

    public void setCodeRegionCPUResBufSize(int size) {
        codeRegionCPUResBufSize = size;
    }

    public int getCodeRegionCPUResBufSize() {
        return codeRegionCPUResBufSize;
    }

    public void setDontShowZeroLiveObjAllocPaths(boolean v) {
        dontShowZeroLiveObjAllocPaths = v;
    } // TODO CHECK: unused method

    public boolean getDontShowZeroLiveObjAllocPaths() {
        return dontShowZeroLiveObjAllocPaths;
    } // TODO CHECK: unused method

    public void setExcludeWaitTime(boolean b) {
        excludeWaitTime = b;
    }

    public boolean getExcludeWaitTime() {
        return excludeWaitTime;
    }

    public void setInstrScheme(int scheme) {
        instrScheme = scheme;
    }

    public int getInstrScheme() {
        return instrScheme;
    }

    public void setInstrumentEmptyMethods(boolean b) {
        instrumentEmptyMethods = b;
    }

    public boolean getInstrumentEmptyMethods() {
        return instrumentEmptyMethods;
    }

    public void setInstrumentGetterSetterMethods(boolean b) {
        instrumentGetterSetterMethods = b;
    }

    public boolean getInstrumentGetterSetterMethods() {
        return instrumentGetterSetterMethods;
    }

    public void setInstrumentMethodInvoke(boolean b) {
        instrumentMethodInvoke = b;
    }

    public boolean getInstrumentMethodInvoke() {
        return instrumentMethodInvoke;
    }

    public void setInstrumentSpawnedThreads(boolean b) {
        instrumentSpawnedThreads = b;
    }

    public boolean getInstrumentSpawnedThreads() {
        return instrumentSpawnedThreads;
    }

    public void setInstrumentationFilter(GenericFilter f) {
        instrumentationFilter = new InstrumentationFilter(f);
    }

    public InstrumentationFilter getInstrumentationFilter() {
        return instrumentationFilter;
    }

    public void setInstrumentationRootMethods(ClientUtils.SourceCodeSelection[] methods) {
        instrumentationRootMethods = methods;
    }

    public ClientUtils.SourceCodeSelection[] getInstrumentationRootMethods() {
        return instrumentationRootMethods;
    }

    public String getJFluidRootDirName() {
        return jFluidRootDirName;
    } // TODO: move elsewhere

    public void setJVMArgs(String args) {
        jvmArgs = args;
    }

    public String[] getJVMArgs() {
        return StringUtils.parseArgsString(jvmArgs);
    }

    public String getJVMArgsAsSingleString() {
        return jvmArgs;
    }

    public void setMainArgs(String args) {
        mainArgs = args;
    }

    public String[] getMainArgs() {
        return StringUtils.parseArgsString(mainArgs);
    }

    public String getMainArgsAsSingleString() {
        return mainArgs;
    }

    public void setMainClass(String name) {
        mainClassName = name;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public void setMainClassPath(String cp) {
        mainClassPath = cp;
    }

    public String getMainClassPath() {
        return mainClassPath;
    }

    public void setMethodMarker(Marker marker) {
        methodMarker = marker;
    }

    public Marker getMethodMarker() {
        return methodMarker;
    }

    public void setNProfiledThreadsLimit(int num) {
        nProfiledThreadsLimit = num;
    }

    public int getNProfiledThreadsLimit() {
        return nProfiledThreadsLimit;
    }

    public void setMaxStringLength(int maxLength) {
        if (maxLength > MAX_STRING_LENGTH_TOP_LIMIT)
            throw new IllegalArgumentException(maxLength+" is over top limit "+MAX_STRING_LENGTH_TOP_LIMIT);    // NOI18N
        maxStringLength = maxLength;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public void setStackDepthLimit(int num) {
        stackDepthLimit = num;
    }

    public int getStackDepthLimit() {
        return stackDepthLimit;
    }

    public void setPortNo(int pNo) {
        portNo = pNo;
    }

    public int getPortNo() {
        return portNo;
    }

    public void setRemoteHost(String host) {
        remoteHost = host;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRunGCOnGetResultsInMemoryProfiling(boolean v) {
        runGCOnGetResultsInMemoryProfiling = v;
    }

    public boolean getRunGCOnGetResultsInMemoryProfiling() {
        return runGCOnGetResultsInMemoryProfiling;
    }

    public boolean isInstrumentArrayAllocation() {
        return instrumentationFilter.hasArray();
    }

    public boolean isInstrumentObjectInit() {
        return instrumentObjectInit;
    }

    public void setInstrumentObjectInit(boolean v) {
        instrumentObjectInit = v;
    }
    
    public void setRuntimeProfilingPoints(RuntimeProfilingPoint[] profilingPoints) {
        this.profilingPoints = profilingPoints;
    }

    public RuntimeProfilingPoint[] getRuntimeProfilingPoints() {
        return profilingPoints;
    }

    public void setSamplingInterval(int num) {
        samplingInterval = num;
    }

    public int getSamplingInterval() {
        return samplingInterval;
    }

    public void setSamplingFrequency(int num) {
        samplingFrequency = num;
    }

    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public void setSeparateConsole(boolean separateConsole) {
        this.separateConsole = separateConsole;
    }

    public boolean getSeparateConsole() {
        return separateConsole;
    }

    public void setSortResultsByThreadCPUTime(boolean v) {
        sortResultsByThreadCPUTime = v;
    }

    public boolean getSortResultsByThreadCPUTime() {
        return sortResultsByThreadCPUTime;
    }

    public void setSuspendTargetApp(boolean b) {
        suspendTargetApp = b;
    } // TODO CHECK: unused method

    public boolean getSuspendTargetApp() {
        return suspendTargetApp;
    } // TODO CHECK: unused method

    public void setSystemArchitecture(int arch) {
        architecture = arch;
    }

    public int getSystemArchitecture() {
        return architecture;
    }

    public void setTargetJDKVersionString(String ver) {
        targetJDKVersion = ver;
    }

    public String getTargetJDKVersionString() {
        return targetJDKVersion;
    }

    public void setTargetJVMExeFile(String name) {
        targetJVMExeFile = name;
    }

    public String getTargetJVMExeFile() {
        return targetJVMExeFile;
    }

    public void setTargetJVMStartupDirName(String dir) {
        targetJVMStartupDirName = dir;
    } // TODO CHECK: unused method

    public String getTargetJVMStartupDirName() {
        return targetJVMStartupDirName;
    } // TODO CHECK: unused method

    public void setTargetWindowRemains(boolean b) {
        targetWindowRemains = b;
    }

    public boolean getTargetWindowRemains() {
        return targetWindowRemains;
    }

    public void setThreadCPUTimerOn(boolean v) {
        threadCPUTimerOn = v;
    }

    public boolean getThreadCPUTimerOn() {
        return threadCPUTimerOn;
    }

    public void setThreadsMonitoringEnabled(boolean b) {
        threadsMonitoringEnabled = b;
    }

    public boolean isThreadsMonitoringEnabled() {
        return threadsMonitoringEnabled;
    }
    
    public void setLockContentionMonitoringEnabled(boolean b) {
        lockContentionMonitoringEnabled = b;
    }

    public boolean isLockContentionMonitoringEnabled() {
        return lockContentionMonitoringEnabled;
    }

    public void setThreadsSamplingEnabled(boolean b) {
        threadsSamplingEnabled = b;
    }

    public boolean isThreadsSamplingEnabled() {
        return threadsSamplingEnabled;
    }

    public void setVMClassPaths(String javaClassPath, String javaExtDirs, String bootClassPath) {
        vmClassPaths[0] = MiscUtils.getLiveClassPathSubset(javaClassPath, getWorkingDir());
        vmClassPaths[1] = javaExtDirs;
        vmClassPaths[2] = bootClassPath;

        // Now set the JFluid class path to match that in the target JVM
        try {
            setMainClassPath(vmClassPaths[0]);
        } catch (Exception ex) {
            throw new InternalError("Should not happen"); // NOI18N
        }
    }

    public String[] getVMClassPaths() {
        // Make sure that updates to the main class path work both ways
        vmClassPaths[0] = getMainClassPath();

        return vmClassPaths;
    }

    public void setWorkingDir(String name) {
        workingDir = name;

        if ((workingDir == null) || "".equals(workingDir)) { // NOI18N
            workingDir = System.getProperty("user.dir"); // NOI18N
        }
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public Object clone() {
        ProfilerEngineSettings clone = null;

        try {
            clone = (ProfilerEngineSettings) super.clone();

            // clone array of instrumentatio root methods one by one
            clone.instrumentationRootMethods = new ClientUtils.SourceCodeSelection[instrumentationRootMethods.length];

            for (int i = 0; i < instrumentationRootMethods.length; i++) {
                clone.instrumentationRootMethods[i] = (ClientUtils.SourceCodeSelection) instrumentationRootMethods[i].clone();
            }

            // clone instrumentation filter
            clone.instrumentationFilter = new InstrumentationFilter(instrumentationFilter);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Should never happen: ProfilerEngineSettings.clone"); // NOI18N
        }
    }

    public void initialize(String rootDirName) throws RuntimeException, IOException {
        String jFluidNativeLibFullName = Platform.getAgentNativeLibFullName(rootDirName, false, null, -1);

        String jFluidNativeLibDirName = jFluidNativeLibFullName.substring(0, jFluidNativeLibFullName.lastIndexOf('/')); // NOI18N

        String checkedPath = ""; // NOI18N   // Needed only for error reporting

        try {
            File rootDir = MiscUtils.checkDirForName(checkedPath = rootDirName);
            MiscUtils.checkDirForName(checkedPath = jFluidNativeLibDirName);
            MiscUtils.checkFile(new File(checkedPath = jFluidNativeLibFullName), false);

            jFluidRootDirName = rootDir.getCanonicalPath();
        } catch (IOException e) {
            throw new IOException("Problem with a required JFluid installation directory or file " + checkedPath, e);
        }
    }
}

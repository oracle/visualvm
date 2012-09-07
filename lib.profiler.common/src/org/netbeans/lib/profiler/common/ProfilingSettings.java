/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.common;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.filters.FilterSet;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.lib.profiler.common.filters.GlobalFilters;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * A Class holding a single named profiling configuration settings within the IDE.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class ProfilingSettings {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.Bundle"); // NOI18N
    static final String DEFAULT_PROFILING_SETTINGS_NAME = bundle.getString("ProfilingSettings_DefaultProfilingSettingsName"); // NOI18N
    private static final String UNKNOWN_PROFILING_SETTINGS_NAME = bundle.getString("ProfilingSettings_UnknownProfilingSettingsName"); // NOI18N
                                                                                                                                      // -----

    // Profiling modes
    public static final int PROFILE_MONITOR = 1; // just monitoring
    public static final int PROFILE_MEMORY_ALLOCATIONS = 2; // memory: allocations
    public static final int PROFILE_MEMORY_LIVENESS = 4; // memory: liveness
    public static final int PROFILE_CPU_ENTIRE = 8; // cpu: entire app (root = main)
    public static final int PROFILE_CPU_PART = 16; // cpu: root methods
    public static final int PROFILE_CPU_STOPWATCH = 32; // cpu: code fragment
    public static final int PROFILE_CPU_SAMPLING = 64; // cpu: sampled profiling
    public static final int PROFILE_MEMORY_SAMPLING = 128; // memory: sampling 
    public static final boolean QUICK_FILTER_INCLUSIVE = true;
    public static final boolean QUICK_FILTER_EXCLUSIVE = false;
    public static final String LINES_PREFIX = "[lines]"; //NOI18N
    public static final String PROP_OVERRIDE_GLOBAL_SETTINGS = "profiler.settings.override"; //NOI18N
    public static final String PROP_WORKING_DIR = "profiler.settings.override.working.dir"; //NOI18N
    public static final String PROP_JVM_ARGS = "profiler.settings.override.jvm.args"; //NOI18N
    public static final String PROP_JAVA_PLATFORM = "profiler.settings.override.java.platform"; //NOI18N
    public static final String PROP_IS_PRESET = "profiler.settigns.ispreset"; // NOI18N
    public static final String PROP_SETTINGS_NAME = "profiler.settings.settings.name"; //NOI18N
    public static final String PROP_PROFILING_TYPE = "profiler.settings.profiling.type"; //NOI18N
    public static final String PROP_THREADS_MONITORING_ENABLED = "profiler.settings.threads.monitoring.enabled"; //NOI18N
    public static final String PROP_THREADS_SAMPLING_ENABLED = "profiler.settings.threads.sampling.enabled"; //NOI18N
    public static final String PROP_CPU_PROFILING_TYPE = "profiler.settings.cpu.profiling.type"; //NOI18N
    public static final String PROP_EXCLUDE_WAIT_TIME = "profiler.settings.cpu.exclude.wait.time"; // NOI18N
    public static final String PROP_INSTR_SCHEME = "profiler.settings.instr.scheme"; //NOI18N
    public static final String PROP_THREAD_CPU_TIMER_ON = "profiler.settings.thread.cpu.timer.on"; //NOI18N
    public static final String PROP_INSTRUMENT_GETTER_SETTER_METHODS = "profiler.settings.istrument.getter.setter.methods"; //NOI18N
    public static final String PROP_INSTRUMENT_EMPTY_METHODS = "profiler.settings.instrument.empty.methods"; //NOI18N
    public static final String PROP_INSTRUMENT_METHOD_INVOKE = "profiler.settings.instrument.method.invoke"; //NOI18N
    public static final String PROP_INSTRUMENT_SPAWNED_THREADS = "profiler.settings.instrument.spawned.threads"; //NOI18N
    public static final String PROP_N_PROFILED_THREADS_LIMIT = "profiler.settings.n.profiled.threads.limit"; //NOI18N
    public static final String PROP_SORT_RESULTS_BY_THREAD_CPU_TIME = "profiler.settings.sort.results.by.thread.cpu.time"; //NOI18N
    public static final String PROP_SAMPLING_INTERVAL = "profiler.settings.sampling.interval"; //NOI18N
    public static final String PROP_INSTRUMENTATION_ROOT_METHODS_SIZE = "profiler.settings.instrumentation.root.methods.size"; //NOI18N
    public static final String PROP_INSTRUMENTATION_ROOT_METHODS_PREFIX = "profiler.settings.istrumentation.root.methods-"; //NOI18N
    public static final String PROP_INSTRUMENTATION_MARKER_METHODS_SIZE = "profiler.settings.instrumentation.marker.methods.size"; //NOI18N
    public static final String PROP_INSTRUMENTATION_MARKER_METHODS_PREFIX = "profiler.settings.istrumentation.marker.methods-"; //NOI18N
    public static final String PROP_FRAGMENT_SELECTION = "profiler.settings.fragment.selection"; //NOI18N
    public static final String PROP_CODE_REGION_CPU_RES_BUF_SIZE = "profiler.settings.code.region.cpu.res.buf.size"; //NOI18N
    public static final String PROP_RUN_GC_ON_GET_RESULTS_IN_MEMORY_PROFILING = "profiler.settings.run.gc.on.get.results.in.memory.profiling"; //NOI18N
    public static final String PROP_OBJ_ALLOC_STACK_SAMPLING_INTERVAL = "profiler.settings.obj.alloc.stack.sampling.interval"; //NOI18N
    public static final String PROP_OBJ_ALLOC_STACK_SAMPLING_DEPTH = "profiler.settings.obj.alloc.stack.sampling.depth"; //NOI18N
    public static final String PROP_SELECTED_INSTR_FILTER = "profiler.settings.instrumentation.filter.selected"; //NOI18N
    public static final String PROP_PROFILE_UNDERLYING_FRAMEWORK = "profiler.settings.profile.underlying.framework"; // NOI18N
    public static final String PROP_PROFILING_POINTS_ENABLED = "profiler.settings.profilingpoints.enabled"; //NOI18N
    public static final String PROP_QUICK_FILTER = "profiler.settings.cpu.quick.filter"; //NOI18N
    public static final String PROP_SAMPLING_FREQUENCY = "profiler.settings.cpu.sampling.frequency"; //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // CPU Profiling: Code Fragment
    private ClientUtils.SourceCodeSelection fragmentSelection = null;
    private List instrumentationMarkerMethods = new ArrayList();

    // CPU Profiling: Part of Application
    private List instrumentationRootMethods = new ArrayList();

    // CPU instrumentation filter related settings
    private Object selectedInstrumentationFilter = SimpleFilter.NO_FILTER; //NOI18N
                                                                           // QuickFilter: just for persistence
    private SimpleFilter quickFilter = FilterUtils.QUICK_FILTER;
    private String jvmArgs = ""; //NOI18N
    private String platformName = null; // from project
    private String settingsName = DEFAULT_PROFILING_SETTINGS_NAME;
    private String workingDir = ""; //NOI18N

    // CPU and Code Fragment common
    private boolean excludeWaitTime = true;
    private boolean instrumentEmptyMethods = false;
    private boolean instrumentGetterSetterMethods = false;
    private boolean instrumentMethodInvoke = true;
    private boolean instrumentSpawnedThreads = false;

    // General (global) settings
    private boolean isPreset = false;
    private boolean overrideGlobalSettings = false;
    private boolean profileUnderlyingFramework = false;

    // -- Memory profiling settings
    private boolean runGCOnGetResultsInMemoryProfiling = true;
    private boolean sortResultsByThreadCPUTime = false;
    private boolean threadCPUTimerOn = false;
    private boolean threadsMonitoringEnabled = false;
    private boolean threadsSamplingEnabled = true;

    // General CPU Profiling settings
    private boolean useProfilingPoints = true;
    private int allocStackTraceLimit = 0; // 0 means no stack sampling performed
    private int allocTrackEvery = 10; // limits the number of allocations tracked to each n-th
    private int codeRegionCPUResBufSize = 1000;
    private int cpuProfilingType = CommonConstants.CPU_INSTR_FULL;
    private int instrScheme = CommonConstants.INSTRSCHEME_LAZY;
    private int nProfiledThreadsLimit = 32;
    private int profilingType = PROFILE_CPU_SAMPLING;

    // CPU Profiling: Sampled
    private int samplingInterval = 10; // hybrid
    private int samplingFrequency = 10; // pure sampling

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // -- Constructors ---------------------------------------------------------------------------------------------------
    public ProfilingSettings() {
    }

    public ProfilingSettings(final String name) {
        this.settingsName = name;
    }
    
    
    // -- Static methods ---
    
    public static boolean isCPUSettings(ProfilingSettings settings) {
        if (settings == null) {
            return false;
        }

        return isCPUSettings(settings.getProfilingType());
    }

    public static boolean isCPUSettings(int type) {
        return (type == ProfilingSettings.PROFILE_CPU_ENTIRE) || (type == ProfilingSettings.PROFILE_CPU_PART)
               || (type == ProfilingSettings.PROFILE_CPU_STOPWATCH || type == PROFILE_CPU_SAMPLING);
    }

    public static boolean isMemorySettings(ProfilingSettings settings) {
        if (settings == null) {
            return false;
        }

        return isMemorySettings(settings.getProfilingType());
    }

    public static boolean isMemorySettings(int type) {
        return (type == PROFILE_MEMORY_ALLOCATIONS) || (type == PROFILE_MEMORY_LIVENESS) || (type == PROFILE_MEMORY_SAMPLING);
    }

    public static boolean isMonitorSettings(ProfilingSettings settings) {
        if (settings == null) {
            return false;
        }

        return isMonitorSettings(settings.getProfilingType());
    }

    public static boolean isMonitorSettings(int type) {
        return type == ProfilingSettings.PROFILE_MONITOR;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAllocStackTraceLimit(final int allocStackTraceLimit) {
        this.allocStackTraceLimit = allocStackTraceLimit;
    }

    public int getAllocStackTraceLimit() {
        return allocStackTraceLimit;
    }

    public void setAllocTrackEvery(final int value) {
        this.allocTrackEvery = value;
    }

    public int getAllocTrackEvery() {
        return allocTrackEvery;
    }

    /** @param cpuProfilingType Type of CPU instrumentation.
     * @see CommonConstants.CPU_INSTR_FULL
     * @see CommonConstants.CPU_INSTR_SAMPLED
     * @see CommonConstants.CPU_SAMPLED
     */
    public void setCPUProfilingType(final int cpuProfilingType) {
        this.cpuProfilingType = cpuProfilingType;
    }

    // -- General CPU Profiling settings ---------------------------------------------------------------------------------

    /** @return Type of CPU instrumentation
     * @see CommonConstants.CPU_INSTR_FULL
     * @see CommonConstants.CPU_INSTR_SAMPLED
     * @see CommonConstants.CPU_SAMPLED
     */
    public int getCPUProfilingType() {
        return cpuProfilingType;
    }

    /** @param fragmentSel code fragment to profile, can be null which indicates no code fragment selected */
    public void setCodeFragmentSelection(final ClientUtils.SourceCodeSelection fragmentSel) {
        this.fragmentSelection = fragmentSel;
    }

    /** @return code fragment to profile, can be null which indicates no code fragment selected */
    public ClientUtils.SourceCodeSelection getCodeFragmentSelection() {
        return fragmentSelection;
    }

    /** @param codeRegionCPUResBufSize Buffer size for code region results */
    public void setCodeRegionCPUResBufSize(final int codeRegionCPUResBufSize) {
        this.codeRegionCPUResBufSize = codeRegionCPUResBufSize;
    }

    /** @return Buffer size for code region results */
    public int getCodeRegionCPUResBufSize() {
        return codeRegionCPUResBufSize;
    }

    public void setExcludeWaitTime(boolean value) {
        excludeWaitTime = value;
    }

    // -- CPU and Code Fragment Profiling settings -----------------------------------------------------------------------
    public boolean getExcludeWaitTime() {
        return excludeWaitTime;
    }

    public void setInstrScheme(final int instrScheme) {
        this.instrScheme = instrScheme;
    }

    public int getInstrScheme() {
        return instrScheme;
    }

    public void setInstrumentEmptyMethods(final boolean instrumentEmptyMethods) {
        this.instrumentEmptyMethods = instrumentEmptyMethods;
    }

    public boolean getInstrumentEmptyMethods() {
        return instrumentEmptyMethods;
    }

    public void setInstrumentGetterSetterMethods(final boolean instrumentGetterSetterMethods) {
        this.instrumentGetterSetterMethods = instrumentGetterSetterMethods;
    }

    public boolean getInstrumentGetterSetterMethods() {
        return instrumentGetterSetterMethods;
    }

    public void setInstrumentMethodInvoke(final boolean instrumentMethodInvoke) {
        this.instrumentMethodInvoke = instrumentMethodInvoke;
    }

    public boolean getInstrumentMethodInvoke() {
        return instrumentMethodInvoke;
    }

    public void setInstrumentSpawnedThreads(final boolean instrumentSpawnedThreads) {
        this.instrumentSpawnedThreads = instrumentSpawnedThreads;
    }

    public boolean getInstrumentSpawnedThreads() {
        return instrumentSpawnedThreads;
    }

    public void setInstrumentationMarkerMethods(final ClientUtils.SourceCodeSelection[] markers) {
        instrumentationMarkerMethods.clear();

        for (int i = 0; i < markers.length; i++) {
            ClientUtils.SourceCodeSelection marker = markers[i];

            if (marker.isMarkerMethod()) {
                instrumentationMarkerMethods.add(marker);
            }
        }
    }

    public ClientUtils.SourceCodeSelection[] getInstrumentationMarkerMethods() {
        return (ClientUtils.SourceCodeSelection[]) instrumentationMarkerMethods.toArray(new ClientUtils.SourceCodeSelection[instrumentationMarkerMethods
                                                                                                                            .size()]);
    }

    public ClientUtils.SourceCodeSelection[] getInstrumentationMethods() {
        Set methods = new HashSet();
        // Keep the order:
        // 1. Root methods; 2. Marker methods
        methods.addAll(instrumentationRootMethods);
        methods.addAll(instrumentationMarkerMethods);

        return (ClientUtils.SourceCodeSelection[]) methods.toArray(new ClientUtils.SourceCodeSelection[methods.size()]);
    }

    public void setInstrumentationRootMethods(final ClientUtils.SourceCodeSelection[] roots) {
        instrumentationRootMethods.clear();

        for (int i = 0; i < roots.length; i++) {
            ClientUtils.SourceCodeSelection root = roots[i];

            if (!root.isMarkerMethod()) {
                instrumentationRootMethods.add(root);
            }
        }
    }

    public ClientUtils.SourceCodeSelection[] getInstrumentationRootMethods() {
        return (ClientUtils.SourceCodeSelection[]) instrumentationRootMethods.toArray(new ClientUtils.SourceCodeSelection[instrumentationRootMethods
                                                                                                                          .size()]);
    }
    
    public void setSamplingFrequency(int samplingFrequency) {
        this.samplingFrequency = samplingFrequency;
    }
    
    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public void setIsPreset(boolean isPreset) {
        this.isPreset = isPreset;
    }

    public void setJVMArgs(final String args) {
        this.jvmArgs = args;
    }

    public String getJVMArgs() {
        return jvmArgs;
    }

    public void setJavaPlatformName(String value) {
        platformName = value;
    }

    public String getJavaPlatformName() {
        return platformName;
    }

    public void setNProfiledThreadsLimit(final int nProfiledThreadsLimit) {
        this.nProfiledThreadsLimit = nProfiledThreadsLimit;
    }

    public int getNProfiledThreadsLimit() {
        return nProfiledThreadsLimit;
    }

    public void setOverrideGlobalSettings(final boolean override) {
        overrideGlobalSettings = override;
    }

    public boolean getOverrideGlobalSettings() {
        return overrideGlobalSettings;
    }

    // -- General (global) settings --------------------------------------------------------------------------------------
    public boolean isPreset() {
        return isPreset;
    }

    public void setProfileUnderlyingFramework(final boolean profileUF) {
        profileUnderlyingFramework = profileUF;
    }

    public boolean getProfileUnderlyingFramework() {
        return profileUnderlyingFramework;
    }

    public void setProfilingType(final int profilingType) {
        this.profilingType = profilingType;
    }

    public int getProfilingType() {
        return profilingType;
    }

    public void setQuickFilter(SimpleFilter quickFilter) {
        this.quickFilter = quickFilter;
    }

    public SimpleFilter getQuickFilter() {
        return quickFilter;
    }

    public void setRunGCOnGetResultsInMemoryProfiling(final boolean runGCOnGetResultsInMemoryProfiling) {
        this.runGCOnGetResultsInMemoryProfiling = runGCOnGetResultsInMemoryProfiling;
    }

    // -- Memory profiling settings --------------------------------------------------------------------------------------
    public boolean getRunGCOnGetResultsInMemoryProfiling() {
        return runGCOnGetResultsInMemoryProfiling;
    }

    public void setSamplingInterval(final int samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    public int getSamplingInterval() {
        return samplingInterval;
    }

    public void setSelectedInstrumentationFilter(final Object sif) {
        selectedInstrumentationFilter = sif != null ? sif : SimpleFilter.NO_FILTER;
    }

    public Object getSelectedInstrumentationFilter() {
        return selectedInstrumentationFilter;
    }

    public void setSettingsName(final String name) {
        this.settingsName = name;
    }

    // -- Constructors ---------------------------------------------------------------------------------------------------
    public String getSettingsName() {
        return settingsName;
    }

    public void setSortResultsByThreadCPUTime(final boolean sortResultsByThreadCPUTime) {
        this.sortResultsByThreadCPUTime = sortResultsByThreadCPUTime;
    }

    public boolean getSortResultsByThreadCPUTime() {
        return sortResultsByThreadCPUTime;
    }

    public void setThreadCPUTimerOn(final boolean threadCPUTimerOn) {
        this.threadCPUTimerOn = threadCPUTimerOn;
    }

    public boolean getThreadCPUTimerOn() {
        return threadCPUTimerOn;
    }

    public void setThreadsMonitoringEnabled(final boolean enabled) {
        threadsMonitoringEnabled = enabled;
    }

    public boolean getThreadsMonitoringEnabled() {
        return threadsMonitoringEnabled;
    }
    
    public void setThreadsSamplingEnabled(final boolean enabled) {
        threadsSamplingEnabled = enabled;
    }

    public boolean getThreadsSamplingEnabled() {
        return threadsSamplingEnabled;
    }

    public void setUseProfilingPoints(boolean enabled) {
        useProfilingPoints = enabled;
    }

    public void setWorkingDir(final String workingDir) {
        this.workingDir = workingDir;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void addRootMethod(final String className, final String methodName, final String signature) {
        ClientUtils.SourceCodeSelection scs = new ClientUtils.SourceCodeSelection(className, methodName, signature);

        if (!instrumentationRootMethods.contains(scs)) {
            instrumentationRootMethods.add(scs);
        }
    }

    public void addRootMethods(final ClientUtils.SourceCodeSelection[] selections) {
        for (int i = 0; i < selections.length; i++) {
            if (!instrumentationRootMethods.contains(selections[i])) {
                instrumentationRootMethods.add(selections[i]);
            }
        }
    }

    public void applySettings(final ProfilerEngineSettings settings) {
        if (getOverrideGlobalSettings()) {
            settings.setWorkingDir(getWorkingDir());
            settings.setJVMArgs(getJVMArgs());

            if (getJavaPlatformName() != null) {
                settings.setTargetJVMExeFile(Profiler.getDefault().getPlatformJavaFile(getJavaPlatformName()));
                settings.setTargetJDKVersionString(Profiler.getDefault().getPlatformJDKVersion(getJavaPlatformName()));
                settings.setSystemArchitecture(Profiler.getDefault().getPlatformArchitecture(getJavaPlatformName()));
            }
        }

        settings.setExcludeWaitTime(getExcludeWaitTime());
        settings.setCPUProfilingType(getCPUProfilingType());
        settings.setInstrScheme(getInstrScheme());
        settings.setAbsoluteTimerOn(true);
        settings.setThreadCPUTimerOn(getThreadCPUTimerOn());
        settings.setInstrumentGetterSetterMethods(getInstrumentGetterSetterMethods());
        settings.setInstrumentEmptyMethods(getInstrumentEmptyMethods());
        settings.setInstrumentMethodInvoke(getInstrumentMethodInvoke());
        settings.setInstrumentSpawnedThreads(getInstrumentSpawnedThreads());
        settings.setThreadsMonitoringEnabled(getThreadsMonitoringEnabled());
        settings.setThreadsSamplingEnabled(getThreadsSamplingEnabled());

        if (getNProfiledThreadsLimit() > 0) {
            settings.setNProfiledThreadsLimit(getNProfiledThreadsLimit());
        } else {
            settings.setNProfiledThreadsLimit(Integer.MAX_VALUE); // zero or negative value means we do not limit it, just remember value for the UI
        }

        settings.setSortResultsByThreadCPUTime(getSortResultsByThreadCPUTime());

        settings.setSamplingInterval(getSamplingInterval());
        settings.setSamplingFrequency(getSamplingFrequency());

        settings.setCodeRegionCPUResBufSize(getCodeRegionCPUResBufSize());

        settings.setRunGCOnGetResultsInMemoryProfiling(getRunGCOnGetResultsInMemoryProfiling());
        settings.setAllocTrackEvery(getAllocTrackEvery());
        settings.setAllocStackTraceLimit(getAllocStackTraceLimit());

        //    Set rootMethods = new HashSet();
        //    // Keep the order:
        //    // 1. Root methods; 2. Marker methods
        //    rootMethods.addAll(Arrays.asList(getInstrumentationRootMethods()));
        //    rootMethods.addAll(Arrays.asList(getInstrumentationMarkerMethods()));
        //    for(Iterator iter=rootMethods.iterator();iter.hasNext();) {
        //      ((ClientUtils.SourceCodeSelection)iter.next()).setMarkerMethod(true);
        //    }
        //    settings.setInstrumentationRootMethods((ClientUtils.SourceCodeSelection[])rootMethods.toArray(new ClientUtils.SourceCodeSelection[rootMethods.size()]));
        settings.setInstrumentationRootMethods(getInstrumentationMethods());

        // Now applySettings the filters to the Engine's instrumentation filter
        final InstrumentationFilter instrumentationFilter = settings.getInstrumentationFilter();
        instrumentationFilter.clearFilter(); // lets start from scratch

        // No filter
        if (getSelectedInstrumentationFilter().equals(FilterUtils.NONE_FILTER)) {
            instrumentationFilter.setFilterType(InstrumentationFilter.INSTR_FILTER_NONE);
            instrumentationFilter.setFilterStrings(""); //NOI18N

            return;
        }

        // Quick Filter
        if (getSelectedInstrumentationFilter().equals(quickFilter)) {
            if (quickFilter.getFilterValue().length() > 0) {
                // Quick Filter defined
                instrumentationFilter.setFilterType((quickFilter.getFilterType() == SimpleFilter.SIMPLE_FILTER_EXCLUSIVE)
                                                    ? InstrumentationFilter.INSTR_FILTER_EXCLUSIVE
                                                    : InstrumentationFilter.INSTR_FILTER_INCLUSIVE);
                instrumentationFilter.setFilterStrings(quickFilter.getFilterValue());
            } else {
                // Quick Filter cancelled and no previous filter defined => filterType=INSTR_FILTER_NONE
                instrumentationFilter.setFilterType(InstrumentationFilter.INSTR_FILTER_NONE);
                instrumentationFilter.setFilterStrings(""); //NOI18N
            }

            return;
        }

        // Filter defined by ProjectTypeProfiler
        if (getSelectedInstrumentationFilter() instanceof SimpleFilter) {
            SimpleFilter ptpFilter = (SimpleFilter) getSelectedInstrumentationFilter();
            instrumentationFilter.setFilterType((ptpFilter.getFilterType() == SimpleFilter.SIMPLE_FILTER_EXCLUSIVE)
                                                ? InstrumentationFilter.INSTR_FILTER_EXCLUSIVE
                                                : InstrumentationFilter.INSTR_FILTER_INCLUSIVE);
            instrumentationFilter.setFilterStrings(ptpFilter.getFilterValue());

            return;
        }

        // Filter Set
        if (getSelectedInstrumentationFilter() instanceof FilterSet) {
            FilterSet filterSet = (FilterSet) getSelectedInstrumentationFilter();
            GlobalFilters globalFilters = Profiler.getDefault().getGlobalFilters();

            // set filter type
            instrumentationFilter.setFilterType((filterSet.getFilterSetType() == FilterSet.FILTER_SET_EXCLUSIVE)
                                                ? InstrumentationFilter.INSTR_FILTER_EXCLUSIVE
                                                : InstrumentationFilter.INSTR_FILTER_INCLUSIVE);

            // set filter value
            final StringBuffer flatFilterStringsBuffer = new StringBuffer();
            final String[] activeGlobalFilters = filterSet.getActiveGlobalFilters();

            for (int i = 0; i < activeGlobalFilters.length; i++) {
                final String activeGlobalFilterValue = globalFilters.getFilterValue(activeGlobalFilters[i]);

                if (activeGlobalFilterValue != null) {
                    flatFilterStringsBuffer.append(activeGlobalFilterValue);
                    flatFilterStringsBuffer.append(" "); //NOI18N
                }
            }

            instrumentationFilter.setFilterStrings(flatFilterStringsBuffer.toString());

            return;
        }

        // Unknown or no filter
        instrumentationFilter.setFilterType(InstrumentationFilter.INSTR_FILTER_NONE);
        instrumentationFilter.setFilterStrings(""); //NOI18N
    }

    // -- Settings duplication -------------------------------------------------------------------------------------------

    /**
     * Copies only profiling settings (not session-related ones) into the given
     * ProfilingSettings instance
     *
     * @param settings the instance to copy the current settings into
     */
    public void copySettingsInto(final ProfilingSettings settings) {
        //    settings.setIsPreset(isPreset()); // Preset flag should not be copied, copy isn't preset
        settings.setProfilingType(getProfilingType());
        settings.setOverrideGlobalSettings(getOverrideGlobalSettings());
        settings.setWorkingDir(getWorkingDir());
        settings.setJVMArgs(getJVMArgs());
        settings.setJavaPlatformName(getJavaPlatformName());
        settings.setThreadsMonitoringEnabled(getThreadsMonitoringEnabled());
        settings.setThreadsSamplingEnabled(getThreadsSamplingEnabled());
        settings.setUseProfilingPoints(useProfilingPoints());

        settings.setExcludeWaitTime(getExcludeWaitTime());
        settings.setCPUProfilingType(getCPUProfilingType());
        settings.setInstrScheme(getInstrScheme());
        settings.setThreadCPUTimerOn(getThreadCPUTimerOn());
        settings.setInstrumentGetterSetterMethods(getInstrumentGetterSetterMethods());
        settings.setInstrumentEmptyMethods(getInstrumentEmptyMethods());
        settings.setInstrumentMethodInvoke(getInstrumentMethodInvoke());
        settings.setInstrumentSpawnedThreads(getInstrumentSpawnedThreads());
        settings.setNProfiledThreadsLimit(getNProfiledThreadsLimit());
        settings.setSortResultsByThreadCPUTime(getSortResultsByThreadCPUTime());

        settings.setSamplingInterval(getSamplingInterval());
        settings.setSamplingFrequency(getSamplingFrequency());
        settings.setInstrumentationRootMethods(getInstrumentationRootMethods());

        settings.setCodeFragmentSelection(getCodeFragmentSelection());
        settings.setCodeRegionCPUResBufSize(getCodeRegionCPUResBufSize());

        settings.setRunGCOnGetResultsInMemoryProfiling(getRunGCOnGetResultsInMemoryProfiling());
        settings.setAllocTrackEvery(getAllocTrackEvery());
        settings.setAllocStackTraceLimit(getAllocStackTraceLimit());

        settings.setSelectedInstrumentationFilter(getSelectedInstrumentationFilter());
        settings.setQuickFilter(getQuickFilter());

        settings.setProfileUnderlyingFramework(getProfileUnderlyingFramework());
    }

    public String debug() {
        final StringBuffer sb = new StringBuffer();
        sb.append("isPreset: ").append(isPreset()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("name: ").append(getSettingsName()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("profilingType: ").append(getProfilingType()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("overrideGlobalSettings: ").append(getOverrideGlobalSettings()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("workingDir: ").append(getWorkingDir()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("jvmArgs: ").append(getJVMArgs()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("javaPlatform: ").append((getJavaPlatformName() == null) ? "<project>" : getJavaPlatformName()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("threadsMonitoringEnabled: ").append(getThreadsMonitoringEnabled()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("threadsSamplingEnabled: ").append(getThreadsSamplingEnabled()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("useProfilingPoints: ").append(useProfilingPoints()); // NOI18N
        sb.append('\n'); //NOI18N
        sb.append("excludeWaitTime: ").append(getExcludeWaitTime()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("cpuProfilingType: ").append(getCPUProfilingType()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("instrScheme: ").append(getInstrScheme()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("threadCPUTimerOn: ").append(getThreadCPUTimerOn()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("instrumentGetterSetterMethods: ").append(getInstrumentGetterSetterMethods()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("instrumentEmptyMethods: ").append(getInstrumentEmptyMethods()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("instrumentMethodInvoke: ").append(getInstrumentMethodInvoke()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("instrumentSpawnedThreads: ").append(getInstrumentSpawnedThreads()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("nProfiledThreadsLimit: ").append(getNProfiledThreadsLimit()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("sortResultsByThreadCPUTime: ").append(getSortResultsByThreadCPUTime()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("samplingInterval: ").append(getSamplingInterval()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("samplingFrequency: ").append(getSamplingFrequency()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("instrumentationRootMethods: ").append(instrumentationRootMethods); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("codeFragmentSelection: ").append(getCodeFragmentSelection()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("codeRegionCPUResBufSize: ").append(getCodeRegionCPUResBufSize()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("runGCOnGetResultsInMemoryProfiling: ").append(getRunGCOnGetResultsInMemoryProfiling()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("allocTrackEvery: ").append(getAllocTrackEvery()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("allocStackTraceLimit: ").append(getAllocStackTraceLimit()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("selectedInstrFilter: ").append(getSelectedInstrumentationFilter()); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("profileUnderlyingFramework: ").append(getProfileUnderlyingFramework()); //NOI18N
        sb.append('\n'); //NOI18N

        return sb.toString();
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public void load(final Map props) {
        load(props, ""); //NOI18N
    }

    public void load(final Map props, final String prefix) {
        setIsPreset(Boolean.valueOf(getProperty(props, prefix + PROP_IS_PRESET, "false")).booleanValue()); //NOI18N
        setSettingsName(getProperty(props, prefix + PROP_SETTINGS_NAME, UNKNOWN_PROFILING_SETTINGS_NAME));
        setProfilingType(Integer.parseInt(getProperty(props, prefix + PROP_PROFILING_TYPE, "8"))); //NOI18N
        setOverrideGlobalSettings(Boolean.valueOf(getProperty(props, prefix + PROP_OVERRIDE_GLOBAL_SETTINGS, "false"))
                                         .booleanValue()); //NOI18N
        setWorkingDir(getProperty(props, prefix + PROP_WORKING_DIR, "")); //NOI18N
        setJVMArgs(getProperty(props, prefix + PROP_JVM_ARGS, "")); //NOI18N

        setJavaPlatformName(getProperty(props, prefix + PROP_JAVA_PLATFORM, null));

        setThreadsMonitoringEnabled(Boolean.valueOf(getProperty(props, prefix + PROP_THREADS_MONITORING_ENABLED, "false")) //NOI18N
                                           .booleanValue());
        
        setThreadsSamplingEnabled(Boolean.valueOf(getProperty(props, prefix + PROP_THREADS_SAMPLING_ENABLED, "true")) //NOI18N
                                           .booleanValue());

        // CPU and Code Fragment common
        // default for exclude wait time is false, to reflect the setting stored in snapshots before the wait time
        // exclusion was introduced
        setExcludeWaitTime(Boolean.valueOf(getProperty(props, prefix + PROP_EXCLUDE_WAIT_TIME, "false")).booleanValue());

        // General CPU Profiling settings
        setCPUProfilingType(Integer.parseInt(getProperty(props, prefix + PROP_CPU_PROFILING_TYPE, "0"))); //NOI18N
        setInstrScheme(Integer.parseInt(getProperty(props, prefix + PROP_INSTR_SCHEME, "1"))); //NOI18N
        setThreadCPUTimerOn(Boolean.valueOf(getProperty(props, prefix + PROP_THREAD_CPU_TIMER_ON, "false")).booleanValue()); //NOI18N
        setInstrumentGetterSetterMethods(Boolean.valueOf(getProperty(props, prefix + PROP_INSTRUMENT_GETTER_SETTER_METHODS,
                                                                     "false")).booleanValue()); //NOI18N
        setInstrumentEmptyMethods(Boolean.valueOf(getProperty(props, prefix + PROP_INSTRUMENT_EMPTY_METHODS, "false"))
                                         .booleanValue()); //NOI18N
        setInstrumentMethodInvoke(Boolean.valueOf(getProperty(props, prefix + PROP_INSTRUMENT_METHOD_INVOKE, "true"))
                                         .booleanValue()); //NOI18N
        setInstrumentSpawnedThreads(Boolean.valueOf(getProperty(props, prefix + PROP_INSTRUMENT_SPAWNED_THREADS, "false"))
                                           .booleanValue()); //NOI18N
        setNProfiledThreadsLimit(Integer.parseInt(getProperty(props, prefix + PROP_N_PROFILED_THREADS_LIMIT, "32"))); //NOI18N
        setSortResultsByThreadCPUTime(Boolean.valueOf(getProperty(props, prefix + PROP_SORT_RESULTS_BY_THREAD_CPU_TIME, "false"))
                                             .booleanValue()); //NOI18N
        setProfileUnderlyingFramework(Boolean.valueOf(getProperty(props, prefix + PROP_PROFILE_UNDERLYING_FRAMEWORK, "false"))
                                             .booleanValue()); //NOI18N
        setSamplingFrequency(Integer.parseInt(getProperty(props, prefix + PROP_SAMPLING_FREQUENCY, "10"))); // NOI18N

        Object iFilter = FilterUtils.loadFilter(props, prefix + PROP_SELECTED_INSTR_FILTER);

        if (iFilter == null) {
            iFilter = SimpleFilter.NO_FILTER; // if loading fails
        }

        setSelectedInstrumentationFilter(iFilter);

        SimpleFilter qFilter = (SimpleFilter) FilterUtils.loadFilter(props, prefix + PROP_QUICK_FILTER);

        if (qFilter == null) {
            qFilter = FilterUtils.QUICK_FILTER; // if loading fails
        }

        setQuickFilter(qFilter);

        if (getSelectedInstrumentationFilter() == null) {
            setSelectedInstrumentationFilter(SimpleFilter.NO_FILTER);
        }

        // CPU Profiling: Sampled
        setSamplingInterval(Integer.parseInt(getProperty(props, prefix + PROP_SAMPLING_INTERVAL, "10"))); //NOI18N

        // CPU Profiling: Part of Application
        final int instrumentationRootMethodsSize = Integer.parseInt(getProperty(props,
                                                                                prefix + PROP_INSTRUMENTATION_ROOT_METHODS_SIZE,
                                                                                "0")); //NOI18N

        for (int i = 0; i < instrumentationRootMethodsSize; i++) {
            final ClientUtils.SourceCodeSelection scs = ClientUtils.stringToSelection(getProperty(props,
                                                                                                  prefix
                                                                                                  + PROP_INSTRUMENTATION_ROOT_METHODS_PREFIX
                                                                                                  + i, null));

            if (scs != null) {
                instrumentationRootMethods.add(scs);
            }
        }

        final int instrumentationMarkerMethodsSize = Integer.parseInt(getProperty(props,
                                                                                  prefix
                                                                                  + PROP_INSTRUMENTATION_MARKER_METHODS_SIZE, "0")); //NOI18N

        for (int i = 0; i < instrumentationMarkerMethodsSize; i++) {
            final ClientUtils.SourceCodeSelection scs = ClientUtils.stringToSelection(getProperty(props,
                                                                                                  prefix
                                                                                                  + PROP_INSTRUMENTATION_MARKER_METHODS_PREFIX
                                                                                                  + i, null));

            if (scs != null) {
                scs.setMarkerMethod(true);
                instrumentationMarkerMethods.add(scs);
            }
        }

        // CPU Profiling: Code Fragment
        setCodeFragmentSelection(ClientUtils.stringToSelection(getProperty(props, prefix + PROP_FRAGMENT_SELECTION, ""))); //NOI18N
        setCodeRegionCPUResBufSize(Integer.parseInt(getProperty(props, prefix + PROP_CODE_REGION_CPU_RES_BUF_SIZE, "1000"))); //NOI18N

        // Memory profiling settings
        setRunGCOnGetResultsInMemoryProfiling(Boolean.valueOf(getProperty(props,
                                                                          prefix + PROP_RUN_GC_ON_GET_RESULTS_IN_MEMORY_PROFILING,
                                                                          "true")).booleanValue()); //NOI18N
        setAllocTrackEvery(Integer.parseInt(getProperty(props, prefix + PROP_OBJ_ALLOC_STACK_SAMPLING_INTERVAL, "10"))); //NOI18N
        setAllocStackTraceLimit(Integer.parseInt(getProperty(props, prefix + PROP_OBJ_ALLOC_STACK_SAMPLING_DEPTH, "-5"))); //NOI18N

        setUseProfilingPoints(Boolean.valueOf(getProperty(props, prefix + PROP_PROFILING_POINTS_ENABLED, "false")).booleanValue()); //NOI18N
    }

    /** Only used for global storage of UI setting in SelectTaskPanel. TODO [ian]: refactor */
    public static void saveRootMethods(final ClientUtils.SourceCodeSelection[] roots, final Map props) {
        props.put(PROP_INSTRUMENTATION_ROOT_METHODS_SIZE, Integer.toString(roots.length));

        for (int i = 0; i < roots.length; i++) {
            props.put(PROP_INSTRUMENTATION_ROOT_METHODS_PREFIX + i,
                      ClientUtils.selectionToString((ClientUtils.SourceCodeSelection) roots[i]));
        }
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public void store(final Map props) {
        store(props, ""); //NOI18N
    }

    public void store(final Map props, final String prefix) {
        props.put(prefix + PROP_IS_PRESET, Boolean.toString(isPreset()));
        props.put(prefix + PROP_SETTINGS_NAME, getSettingsName());
        props.put(prefix + PROP_PROFILING_TYPE, Integer.toString(getProfilingType()));
        props.put(prefix + PROP_OVERRIDE_GLOBAL_SETTINGS, Boolean.toString(getOverrideGlobalSettings()));
        props.put(prefix + PROP_WORKING_DIR, getWorkingDir());
        props.put(prefix + PROP_JVM_ARGS, getJVMArgs());

        if (getJavaPlatformName() != null) {
            props.put(prefix + PROP_JAVA_PLATFORM, getJavaPlatformName());
        }

        props.put(prefix + PROP_THREADS_MONITORING_ENABLED, Boolean.toString(getThreadsMonitoringEnabled()));
        props.put(prefix + PROP_THREADS_SAMPLING_ENABLED, Boolean.toString(getThreadsSamplingEnabled()));

        // CPU and Code Fragment common
        props.put(prefix + PROP_EXCLUDE_WAIT_TIME, Boolean.toString(getExcludeWaitTime()));

        // General CPU Profiling settings
        props.put(prefix + PROP_CPU_PROFILING_TYPE, Integer.toString(getCPUProfilingType()));
        props.put(prefix + PROP_INSTR_SCHEME, Integer.toString(getInstrScheme()));
        props.put(prefix + PROP_THREAD_CPU_TIMER_ON, Boolean.toString(getThreadCPUTimerOn()));
        props.put(prefix + PROP_INSTRUMENT_GETTER_SETTER_METHODS, Boolean.toString(getInstrumentGetterSetterMethods()));
        props.put(prefix + PROP_INSTRUMENT_EMPTY_METHODS, Boolean.toString(getInstrumentEmptyMethods()));
        props.put(prefix + PROP_INSTRUMENT_METHOD_INVOKE, Boolean.toString(getInstrumentMethodInvoke()));
        props.put(prefix + PROP_INSTRUMENT_SPAWNED_THREADS, Boolean.toString(getInstrumentSpawnedThreads()));
        props.put(prefix + PROP_N_PROFILED_THREADS_LIMIT, Integer.toString(getNProfiledThreadsLimit()));
        props.put(prefix + PROP_SORT_RESULTS_BY_THREAD_CPU_TIME, Boolean.toString(getSortResultsByThreadCPUTime()));
        props.put(prefix + PROP_SAMPLING_FREQUENCY, Integer.toString(getSamplingFrequency()));

        FilterUtils.storeFilter(props, getSelectedInstrumentationFilter(), prefix + PROP_SELECTED_INSTR_FILTER);
        FilterUtils.storeFilter(props, getQuickFilter(), prefix + PROP_QUICK_FILTER);

        props.put(prefix + PROP_PROFILE_UNDERLYING_FRAMEWORK, Boolean.toString(getProfileUnderlyingFramework()));

        // CPU Profiling: Sampled
        props.put(prefix + PROP_SAMPLING_INTERVAL, Integer.toString(getSamplingInterval()));

        // CPU Profiling: Part of Application
        props.put(prefix + PROP_INSTRUMENTATION_ROOT_METHODS_SIZE, Integer.toString(instrumentationRootMethods.size()));

        for (int i = 0; i < instrumentationRootMethods.size(); i++) {
            props.put(prefix + PROP_INSTRUMENTATION_ROOT_METHODS_PREFIX + i,
                      ClientUtils.selectionToString((ClientUtils.SourceCodeSelection) instrumentationRootMethods.get(i)));
        }

        props.put(prefix + PROP_INSTRUMENTATION_MARKER_METHODS_SIZE, Integer.toString(instrumentationMarkerMethods.size()));

        for (int i = 0; i < instrumentationMarkerMethods.size(); i++) {
            props.put(prefix + PROP_INSTRUMENTATION_MARKER_METHODS_PREFIX + i,
                      ClientUtils.selectionToString((ClientUtils.SourceCodeSelection) instrumentationMarkerMethods.get(i)));
        }

        // CPU Profiling: Code Fragment
        if (getCodeFragmentSelection() != null) {
            props.put(prefix + PROP_FRAGMENT_SELECTION, ClientUtils.selectionToString(getCodeFragmentSelection()));
        }

        props.put(prefix + PROP_CODE_REGION_CPU_RES_BUF_SIZE, Integer.toString(getCodeRegionCPUResBufSize()));

        // Memory profiling settings
        props.put(prefix + PROP_RUN_GC_ON_GET_RESULTS_IN_MEMORY_PROFILING,
                  Boolean.toString(getRunGCOnGetResultsInMemoryProfiling()));
        props.put(prefix + PROP_OBJ_ALLOC_STACK_SAMPLING_INTERVAL, Integer.toString(getAllocTrackEvery()));
        props.put(prefix + PROP_OBJ_ALLOC_STACK_SAMPLING_DEPTH, Integer.toString(getAllocStackTraceLimit()));

        props.put(prefix + PROP_PROFILING_POINTS_ENABLED, Boolean.toString(useProfilingPoints()));
    }

    // -------------------------------------------------------------------------------------------------------------------
    // debug & print stuff
    public String toString() {
        return getSettingsName();
    }

    public boolean useProfilingPoints() {
        return useProfilingPoints;
    }

    static String getProperty(final Map props, final Object key, final String defaultValue) {
        final Object ret = props.get(key);

        return (ret != null) ? (String) ret : defaultValue;
    }
}

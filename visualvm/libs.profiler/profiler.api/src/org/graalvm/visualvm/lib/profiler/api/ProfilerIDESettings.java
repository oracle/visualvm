/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;
import org.graalvm.visualvm.lib.common.GlobalProfilingSettings;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;


/**
 * SystemOption to store UI settings for profiler
 */
public final class ProfilerIDESettings implements GlobalProfilingSettings {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String DO_NOT_SHOW_ATTACH_SETTINGS = "dns-attach-settings"; // NOI18N
    public static final String DO_NOT_SHOW_JDK_DIALOG = "dns-jdk-dialog"; // NOI18N
    public static final String DO_NOT_SHOW_PID_WINDOWS = "dns-pid-windows4"; // NOI18N
    public static final int CPU_ENTIRE_APP = 0;
    public static final int CPU_PART_APP = 1;
    public static final int CPU_STARTUP = 2;
    public static final int CPU_PROFILING_POINTS = 3;
    public static final int OOME_DETECTION_NONE = 0;
    public static final int OOME_DETECTION_PROJECTDIR = 1;
    public static final int OOME_DETECTION_TEMPDIR = 2;
    public static final int OOME_DETECTION_CUSTOMDIR = 3;
    public static final int SNAPSHOT_WINDOW_OPEN_NEVER = 0;
    public static final int SNAPSHOT_WINDOW_OPEN_PROFILER = 1;
    public static final int SNAPSHOT_WINDOW_SHOW_PROFILER = 2;
    public static final int SNAPSHOT_WINDOW_OPEN_FIRST = 3;
    public static final int SNAPSHOT_WINDOW_OPEN_EACH = 4;
    public static final int SNAPSHOT_WINDOW_CLOSE_NEVER = 0;
    public static final int SNAPSHOT_WINDOW_CLOSE_PROFILER = 1;
    public static final int SNAPSHOT_WINDOW_HIDE_PROFILER = 2;

    /** The Window automatically opens always when profiling starts */
    public static final int OPEN_ALWAYS = 1;

    /** The Window automatically opens when profiling starts using only Monitoring */
    public static final int OPEN_MONITORING = 2;

    /** The Windows does not automatically open */
    public static final int OPEN_NEVER = 3;

    // --- Singleton pattern ---
    private static final ProfilerIDESettings defaultInstance = new ProfilerIDESettings();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Map<String, String> dnsaMap;
    
    // Keys for tracked properties
    private final String AUTO_OPEN_SNAPSHOT_KEY_55 = "autoOpenSnapshot"; // NOI18N
    private final String AUTO_SAVE_SNAPSHOT_KEY_55 = "autoSaveSnapshot"; // NOI18N
    private final String CALIBRATION_PORT_NO_KEY_55 = "calibrationPortNo"; // NOI18N
    private final String CPU_TASK_KEY_55 = "cpuTaskDefault"; // NOI18N
    private final String LIVE_CPU_KEY_55 = "displayLiveResultsCPU"; // NOI18N
    private final String LIVE_FRAGMENT_KEY_55 = "displayLiveResultsFragment"; // NOI18N
    private final String LIVE_MEMORY_KEY_55 = "displayLiveResultsMemory"; // NOI18N
    private final String MEMORY_TASK_ALLOCATIONS_KEY_55 = "memoryTaskAllocationsDefault"; // NOI18N
    private final String PLATFORM_NAME_KEY_55 = "javaPlatformForProfiling"; // NOI18N
    private final String PORT_NO_KEY_55 = "portNo"; // NOI18N
    private final String RECORD_STACK_TRACES_KEY_55 = "recordStackTracesDefault"; // NOI18N
    private final String THREADS_MONITORING_KEY_55 = "threadsMonitoringDefault"; // NOI18N
    private final String TO_BEHAVIOR_KEY_55 = "telemetryOverviewBehavior"; // NOI18N
    private final String TRACK_EVERY_KEY_55 = "trackEveryDefault"; // NOI18N
    private final String TV_BEHAVIOR_KEY_55 = "threadsViewBehavior"; // NOI18N

    // Keys for tracked properties
    private final String AUTO_OPEN_SNAPSHOT_KEY = "AUTO_OPEN_SNAPSHOT"; // NOI18N
    private final String AUTO_SAVE_SNAPSHOT_KEY = "AUTO_SAVE_SNAPSHOT"; // NOI18N
    private final String CALIBRATION_PORT_NO_KEY = "CALIBRATION_PORT_NO"; // NOI18N
    private final String CPU_TASK_KEY = "CPU_TASK"; // NOI18N
    private final String CUSTOM_HEAPDUMP_PATH_KEY = "CUSTOM_HEAPDUMP_PATH"; // NOI18N
    private final String DNSA_SETTINGS_KEY = "DNSA_SETTINGS"; // NOI18N
    private final String HEAPWALKER_ANALYSIS_ENABLED_KEY = "HEAPWALKER_ANALYSIS_ENABLED"; // NOI18N
    private final String INSTR_FILTER_KEY = "INSTR_FILTER"; // NOI18N
    private final String LIVE_CPU_KEY = "LIVE_CPU"; // NOI18N
    private final String LIVE_FRAGMENT_KEY = "LIVE_FRAGMENT"; // NOI18N
    private final String LIVE_MEMORY_KEY = "LIVE_MEMORY"; // NOI18N
    private final String MEMORY_TASK_ALLOCATIONS_KEY = "MEMORY_TASK_ALLOCATIONS"; // NOI18N
    private final String OOME_DETECTION_MODE_KEY = "OOME_DETECTION_MODE"; // NOI18N
    private final String PLATFORM_NAME_KEY = "PLATFORM_NAME"; // NOI18N
    private final String PORT_NO_KEY = "PORT_NO"; // NOI18N
    private final String PPOINTS_DEPENDENCIES_INCLUDE_KEY = "PPOINTS_DEPENDENCIES_INCLUDE"; // NOI18N
    private final String RECORD_STACK_TRACES_KEY = "RECORD_STACK_TRACES"; // NOI18N
    private final String THREADS_MONITORING_KEY = "THREADS_MONITORING"; // NOI18N
    private final String TO_BEHAVIOR_KEY = "TO_BEHAVIOR"; // NOI18N
    private final String TRACK_EVERY_KEY = "TRACK_EVERY"; // NOI18N
    private final String TV_BEHAVIOR_KEY = "TV_BEHAVIOR"; // NOI18N
    private final String LCV_BEHAVIOR_KEY = "LCV_BEHAVIOR"; // NOI18N
    private final String NO_DATA_HINT_KEY = "NO_DATA_HINT"; // NOI18N
    private final String SNAPSHOT_WINDOW_OPEN_POLICY_KEY = "SNAPSHOT_WINDOW_OPEN_POLICY"; // NOI18N
    private final String SNAPSHOT_WINDOW_CLOSE_POLICY_KEY = "SNAPSHOT_WINDOW_CLOSE_POLICY"; // NOI18N
    private final String ENABLE_EXPERT_SETTINGS_KEY = "ENABLE_EXPERT_SETTINGS"; // NOI18N
    private final String LOG_PROFILER_STATUS_KEY = "LOG_PROFILER_STATUS"; // NOI18N
    private final String REOPEN_SNAPSHOTS_KEY = "REOPEN_SNAPSHOTS_KEY"; // NOI18N
    private final String REOPEN_HDUMPS_KEY = "REOPEN_HDUMPS_KEY"; // NOI18N
    private final String SOURCES_COLORING_KEY = "SOURCES_COLORING_KEY"; // NOI18N
    
    // Defaults for tracked properties
    private final String CUSTOM_HEAPDUMP_PATH_DEFAULT = ""; // NOI18N
    private final String PLATFORM_NAME_DEFAULT = "PLATFORM_NAME_DEFAULT"; // NOI18N // replaces original null, means platform defined by project
    private final String INSTR_FILTER_DEFAULT = "###"; // NOI18N // Shouldn't be matched, this is intention (logic for default is in CPUPerformanceConfigPanelLarge)
    private final boolean AUTO_OPEN_SNAPSHOT_DEFAULT = true;
    private final boolean AUTO_SAVE_SNAPSHOT_DEFAULT = false;
    private final boolean HEAPWALKER_ANALYSIS_ENABLED_DEFAULT = false;
    private final boolean LIVE_CPU_DEFAULT = false;
    private final boolean LIVE_FRAGMENT_DEFAULT = false;
    private final boolean LIVE_MEMORY_DEFAULT = false;
    private final boolean MEMORY_TASK_ALLOCATIONS_DEFAULT = true;
    private final boolean PPOINTS_DEPENDENCIES_INCLUDE_DEFAULT = true;
    private final boolean RECORD_STACK_TRACES_DEFAULT = false;
    private final boolean THREADS_MONITORING_DEFAULT = true;
    private final int CALIBRATION_PORT_NO_DEFAULT = -1;
    private final int CPU_TASK_DEFAULT = CPU_ENTIRE_APP;
    private final int OOME_DETECTION_MODE_DEFAULT = OOME_DETECTION_PROJECTDIR;
    private final int PORT_NO_DEFAULT = 5140;
    private final int TO_BEHAVIOR_DEFAULT = OPEN_MONITORING;
    private final int TRACK_EVERY_DEFAULT = 10;
    private final int TV_BEHAVIOR_DEFAULT = OPEN_ALWAYS;
    private final int LCV_BEHAVIOR_DEFAULT = OPEN_ALWAYS;
    private final boolean NO_DATA_HINT_DEFAULT = true;
    private final int SNAPSHOT_WINDOW_OPEN_DEFAULT = SNAPSHOT_WINDOW_OPEN_FIRST;
    private final int SNAPSHOT_WINDOW_CLOSE_DEFAULT = SNAPSHOT_WINDOW_CLOSE_NEVER;
    private final boolean ENABLE_EXPERT_SETTINGS_DEFAULT = false;
    private final boolean LOG_PROFILER_STATUS_DEFAULT = false;
    private final boolean REOPEN_SNAPSHOTS_DEFAULT = false;
    private final boolean REOPEN_HDUMPS_DEFAULT = false;
    private final boolean SOURCES_COLORING_DEFAULT = false;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ProfilerIDESettings getInstance() {
        return defaultInstance;
    }
    
    private ProfilerIDESettings() {}
    
    
    // ProfilingSettings -------------------------------------------------------------------------------------------------
    
    private ProfilingSettings pSettings;
    
    public ProfilingSettings getDefaultProfilingSettings() {
        if (pSettings == null) pSettings = loadProfilingSettings();
        return pSettings;
    }
    
    public void saveDefaultProfilingSettings() {
        if (pSettings != null) storeProfilingSettings(pSettings);
    }
    
    public ProfilingSettings createDefaultProfilingSettings() {
        ProfilingSettings defaultSettings = new ProfilingSettings();
        getDefaultProfilingSettings().copySettingsInto(defaultSettings);
        return defaultSettings;
    }
    
    private ProfilingSettings loadProfilingSettings() {
        Preferences pref = getPreferences();
        ProfilingSettings settings = new ProfilingSettings();
        
        settings.setSamplingFrequency(pref.getInt(ProfilingSettings.PROP_SAMPLING_FREQUENCY, 10));
        settings.setCPUProfilingType(pref.getInt(ProfilingSettings.PROP_CPU_PROFILING_TYPE, CommonConstants.CPU_INSTR_FULL));
        settings.setSamplingInterval(pref.getInt(ProfilingSettings.PROP_SAMPLING_INTERVAL, -10));
        settings.setExcludeWaitTime(pref.getBoolean(ProfilingSettings.PROP_EXCLUDE_WAIT_TIME, true));
        settings.setInstrumentSpawnedThreads(pref.getBoolean(ProfilingSettings.PROP_INSTRUMENT_SPAWNED_THREADS, false));
        settings.setNProfiledThreadsLimit(pref.getInt(ProfilingSettings.PROP_N_PROFILED_THREADS_LIMIT, 128));
        settings.setInstrScheme(pref.getInt(ProfilingSettings.PROP_INSTR_SCHEME, CommonConstants.INSTRSCHEME_LAZY));
        settings.setInstrumentMethodInvoke(pref.getBoolean(ProfilingSettings.PROP_INSTRUMENT_METHOD_INVOKE, true));
        settings.setInstrumentGetterSetterMethods(pref.getBoolean(ProfilingSettings.PROP_INSTRUMENT_GETTER_SETTER_METHODS, false));
        settings.setInstrumentEmptyMethods(pref.getBoolean(ProfilingSettings.PROP_INSTRUMENT_EMPTY_METHODS, false));
        
        settings.setAllocTrackEvery(pref.getInt(ProfilingSettings.PROP_OBJ_ALLOC_STACK_SAMPLING_INTERVAL, 1));
        settings.setRunGCOnGetResultsInMemoryProfiling(pref.getBoolean(ProfilingSettings.PROP_RUN_GC_ON_GET_RESULTS_IN_MEMORY_PROFILING, false));
        
        settings.setThreadsSamplingEnabled(pref.getBoolean(ProfilingSettings.PROP_THREADS_SAMPLING_ENABLED, false));
        
        return settings;
    }
    
    private void storeProfilingSettings(ProfilingSettings settings) {
        Preferences pref = getPreferences();
        
        pref.putInt(ProfilingSettings.PROP_SAMPLING_FREQUENCY, settings.getSamplingFrequency());
        pref.putInt(ProfilingSettings.PROP_CPU_PROFILING_TYPE, settings.getCPUProfilingType());
        pref.putInt(ProfilingSettings.PROP_SAMPLING_INTERVAL, settings.getSamplingInterval());
        pref.putBoolean(ProfilingSettings.PROP_EXCLUDE_WAIT_TIME, settings.getExcludeWaitTime());
        pref.putBoolean(ProfilingSettings.PROP_INSTRUMENT_SPAWNED_THREADS, settings.getInstrumentSpawnedThreads());
        pref.putInt(ProfilingSettings.PROP_N_PROFILED_THREADS_LIMIT, settings.getNProfiledThreadsLimit());
        pref.putInt(ProfilingSettings.PROP_INSTR_SCHEME, settings.getInstrScheme());
        pref.putBoolean(ProfilingSettings.PROP_INSTRUMENT_METHOD_INVOKE, settings.getInstrumentMethodInvoke());
        pref.putBoolean(ProfilingSettings.PROP_INSTRUMENT_GETTER_SETTER_METHODS, settings.getInstrumentGetterSetterMethods());
        pref.putBoolean(ProfilingSettings.PROP_INSTRUMENT_EMPTY_METHODS, settings.getInstrumentEmptyMethods());
        
        pref.putInt(ProfilingSettings.PROP_OBJ_ALLOC_STACK_SAMPLING_INTERVAL, settings.getAllocTrackEvery());
        pref.putBoolean(ProfilingSettings.PROP_RUN_GC_ON_GET_RESULTS_IN_MEMORY_PROFILING, settings.getRunGCOnGetResultsInMemoryProfiling());
        
        pref.putBoolean(ProfilingSettings.PROP_THREADS_SAMPLING_ENABLED, settings.getThreadsSamplingEnabled());
    }
    
    // Properties --------------------------------------------------------------------------------------------------------
    
    /** Determines whether snapshots are automatically opened.
     * @param value true if snapshot should be automatically opened after taking it, false otherwise
     */
    public void setAutoOpenSnapshot(final boolean value) {
        getPreferences().putBoolean(AUTO_OPEN_SNAPSHOT_KEY, value);
    }

    /** Determines whether snapshots are automatically opened.
     * @return true if snapshot should be automatically opened after taking it, false otherwise
     */
    public boolean getAutoOpenSnapshot() {
        return getPreferences().getBoolean(AUTO_OPEN_SNAPSHOT_KEY, AUTO_OPEN_SNAPSHOT_DEFAULT);
    }

    /** Determines whether snapshots are automatically saved.
     * @param value true if snapshot should be automatically saved after taking it, false otherwise
     */
    public void setAutoSaveSnapshot(final boolean value) {
        getPreferences().putBoolean(AUTO_SAVE_SNAPSHOT_KEY, value);
    }

    /** Determines whether snapshots are automatically saved.
     *
     * @return true if snapshot should be automatically saved after taking it, false otherwise
     */
    public boolean getAutoSaveSnapshot() {
        return getPreferences().getBoolean(AUTO_SAVE_SNAPSHOT_KEY, AUTO_SAVE_SNAPSHOT_DEFAULT);
    }

    public void setCalibrationPortNo(final int value) {
        getPreferences().putInt(CALIBRATION_PORT_NO_KEY, value);
    }

    public int getCalibrationPortNo() {
        int calibrationPort = getPreferences().getInt(CALIBRATION_PORT_NO_KEY, CALIBRATION_PORT_NO_DEFAULT);

        if (calibrationPort == -1) {
            return getPortNo() + 1;
        } else {
            return calibrationPort;
        }
    }

    public void setCpuTaskDefault(int value) {
        getPreferences().putInt(CPU_TASK_KEY, value);
    }

    public int getCpuTaskDefault() {
        return getPreferences().getInt(CPU_TASK_KEY, CPU_TASK_DEFAULT);
    }

    /**
     * The custom path to a heapdump generated by -XX:+HeapDumpOnOutOfMemoryError option
     */
    public void setCustomHeapdumpPath(String heapDumpCustomDir) {
        getPreferences().put(CUSTOM_HEAPDUMP_PATH_KEY, heapDumpCustomDir);
    }

    /**
     * The custom path to a heapdump generated by -XX:+HeapDumpOnOutOfMemoryError option
     */
    public String getCustomHeapdumpPath() {
        return getPreferences().get(CUSTOM_HEAPDUMP_PATH_KEY, CUSTOM_HEAPDUMP_PATH_DEFAULT);
    }

    public void setDisplayLiveResultsCPU(final boolean value) {
        getPreferences().putBoolean(LIVE_CPU_KEY, value);
    }

    public boolean getDisplayLiveResultsCPU() {
        return getPreferences().getBoolean(LIVE_CPU_KEY, LIVE_CPU_DEFAULT);
    }

    public void setDisplayLiveResultsFragment(final boolean value) {
        getPreferences().putBoolean(LIVE_FRAGMENT_KEY, value);
    }

    public boolean getDisplayLiveResultsFragment() {
        return getPreferences().getBoolean(LIVE_FRAGMENT_KEY, LIVE_FRAGMENT_DEFAULT);
    }

    public void setDisplayLiveResultsMemory(final boolean value) {
        getPreferences().putBoolean(LIVE_MEMORY_KEY, value);
    }

    public boolean getDisplayLiveResultsMemory() {
        return getPreferences().getBoolean(LIVE_MEMORY_KEY, LIVE_MEMORY_DEFAULT);
    }
    
    public void setShowNoDataHint(boolean value) {
        getPreferences().putBoolean(NO_DATA_HINT_KEY, value);
    }
    
    public boolean getShowNoDataHint() {
        return getPreferences().getBoolean(NO_DATA_HINT_KEY, NO_DATA_HINT_DEFAULT);
    }
    
    public void setSnapshotWindowOpenPolicy(int policy) {
        getPreferences().putInt(SNAPSHOT_WINDOW_OPEN_POLICY_KEY, policy);
    }
    
    public int getSnapshotWindowOpenPolicy() {
        return getPreferences().getInt(SNAPSHOT_WINDOW_OPEN_POLICY_KEY, SNAPSHOT_WINDOW_OPEN_DEFAULT);
    }
    
    public void setSnapshotWindowClosePolicy(int policy) {
        getPreferences().putInt(SNAPSHOT_WINDOW_CLOSE_POLICY_KEY, policy);
    }
    
    public int getSnapshotWindowClosePolicy() {
        return getPreferences().getInt(SNAPSHOT_WINDOW_CLOSE_POLICY_KEY, SNAPSHOT_WINDOW_CLOSE_DEFAULT);
    }
    
    public void setEnableExpertSettings(boolean value) {
        getPreferences().putBoolean(ENABLE_EXPERT_SETTINGS_KEY, true);
//        getPreferences().putBoolean(ENABLE_EXPERT_SETTINGS_KEY, value);
    }
    
    public boolean getEnableExpertSettings() {
        return true;
//        return getPreferences().getBoolean(ENABLE_EXPERT_SETTINGS_KEY, ENABLE_EXPERT_SETTINGS_DEFAULT);
    }
    
    public void setLogProfilerStatus(boolean value) {
        getPreferences().putBoolean(LOG_PROFILER_STATUS_KEY, value);
    }
    
    public boolean getLogProfilerStatus() {
        return getPreferences().getBoolean(LOG_PROFILER_STATUS_KEY, LOG_PROFILER_STATUS_DEFAULT);
    }
    
    public void setReopenSnapshots(boolean value) {
        getPreferences().putBoolean(REOPEN_SNAPSHOTS_KEY, value);
    }
    
    public boolean getReopenSnapshots() {
        return getPreferences().getBoolean(REOPEN_SNAPSHOTS_KEY, REOPEN_SNAPSHOTS_DEFAULT);
    }
    
    public void setReopenHeapDumps(boolean value) {
        getPreferences().putBoolean(REOPEN_HDUMPS_KEY, value);
    }
    
    public boolean getReopenHeapDumps() {
        return getPreferences().getBoolean(REOPEN_HDUMPS_KEY, REOPEN_HDUMPS_DEFAULT);
    }
    
    private Boolean sourcesColoringEnabled; // needs to be as fast as possible for rendering tables
    public void setSourcesColoringEnabled(boolean value) {
        getPreferences().putBoolean(SOURCES_COLORING_KEY, value);
        sourcesColoringEnabled = value;
    }
    
    public boolean isSourcesColoringEnabled() {
        if (sourcesColoringEnabled == null) sourcesColoringEnabled =
                getPreferences().getBoolean(SOURCES_COLORING_KEY, SOURCES_COLORING_DEFAULT);
        return sourcesColoringEnabled;
    }

    /**
     * Used by the DNSAConfirmation.
     *
     * Allows to set or clear persistent do not show again value associated with given notification identified by the
     * provided key.
     *
     * @param key A key that uniquely identifies the notification
     * @param value The value that should be used without displaying the notification or null to cleat the Do not show
     *              again (i.e. start displaying the notifications again.
     *
     * @see org.graalvm.visualvm.lib.profiler.impl.ProfilerDialogs.DNSAConfirmation
     * @see org.graalvm.visualvm.lib.profiler.impl.ProfilerDialogs.DNSAMessage
     */
    public void setDoNotShowAgain(final String key, final String value) {
        if (value != null) {
            getDNSAMap().put(key, value);
        } else {
            getDNSAMap().remove(key);
        }

        storeDNSAMap();
    }

    /**
     * Used by the DNSAConfirmation.
     *
     * Allows to get persistent do not show again value associated with given notification identified by the provided key.
     *
     * @param  key A key that uniquely identifies the notification
     * @return The value that should be used without displaying the notification or null if the notification should
     *         be displayed
     *
     * @see org.graalvm.visualvm.lib.profiler.impl.ProfilerDialogs.DNSAConfirmation
     * @see org.graalvm.visualvm.lib.profiler.impl.ProfilerDialogs.DNSAMessage
     */
    public String getDoNotShowAgain(final String key) {
        return getDNSAMap().get(key);
    }

    public void setHeapWalkerAnalysisEnabled(boolean value) {
        getPreferences().putBoolean(HEAPWALKER_ANALYSIS_ENABLED_KEY, value);
    }

    public boolean getHeapWalkerAnalysisEnabled() {
        return getPreferences().getBoolean(HEAPWALKER_ANALYSIS_ENABLED_KEY, HEAPWALKER_ANALYSIS_ENABLED_DEFAULT);
    }

    public void setIncludeProfilingPointsDependencies(boolean value) {
        getPreferences().putBoolean(PPOINTS_DEPENDENCIES_INCLUDE_KEY, value);
    }

    public boolean getIncludeProfilingPointsDependencies() {
        return getPreferences().getBoolean(PPOINTS_DEPENDENCIES_INCLUDE_KEY, PPOINTS_DEPENDENCIES_INCLUDE_DEFAULT);
    }

    public void setInstrFilterDefault(String value) {
        getPreferences().put(INSTR_FILTER_KEY, value);
    }

    public String getInstrFilterDefault() {
        return getPreferences().get(INSTR_FILTER_KEY, INSTR_FILTER_DEFAULT);
    }

    /**
     * @param value Name of Java platform to use for profiling. Null value indicates no global platform is selected
     */
    public void setJavaPlatformForProfiling(String value) {
        getPreferences().put(PLATFORM_NAME_KEY, (value == null) ? PLATFORM_NAME_DEFAULT : value);
    }

    /** @return Name of Java platform to use for profiling. Null value indicates no global platform is selected */
    public String getJavaPlatformForProfiling() {
        String platformName = getPreferences().get(PLATFORM_NAME_KEY, PLATFORM_NAME_DEFAULT);

        return PLATFORM_NAME_DEFAULT.equals(platformName) ? null : platformName;
    }

    public void setMemoryTaskAllocationsDefault(boolean value) {
        getPreferences().putBoolean(MEMORY_TASK_ALLOCATIONS_KEY, value);
    }

    public boolean getMemoryTaskAllocationsDefault() {
        return getPreferences().getBoolean(MEMORY_TASK_ALLOCATIONS_KEY, MEMORY_TASK_ALLOCATIONS_DEFAULT);
    }

    /**
     * The flag specifying whether all profiled applications should be started with -XX:+HeapDumpOnOutOfMemoryError option
     */
    public boolean isOOMDetectionEnabled() {
        return getOOMDetectionMode() != OOME_DETECTION_NONE;
    }

    /**
     * The flag specifying whether all profiled applications should be started with -XX:+HeapDumpOnOutOfMemoryError option
     */
    public void setOOMDetectionMode(int oomeDetectionMode) {
        getPreferences().putInt(OOME_DETECTION_MODE_KEY, oomeDetectionMode);
    }

    /**
     * The flag specifying whether all profiled applications should be started with -XX:+HeapDumpOnOutOfMemoryError option
     */
    public int getOOMDetectionMode() {
        return getPreferences().getInt(OOME_DETECTION_MODE_KEY, OOME_DETECTION_MODE_DEFAULT);
    }

    public void setPortNo(final int value) {
        getPreferences().putInt(PORT_NO_KEY, value);
    }

    public int getPortNo() {
        return getPreferences().getInt(PORT_NO_KEY, PORT_NO_DEFAULT);
    }

    public void setRecordStackTracesDefault(boolean value) {
        getPreferences().putBoolean(RECORD_STACK_TRACES_KEY, value);
    }

    public boolean getRecordStackTracesDefault() {
        return getPreferences().getBoolean(RECORD_STACK_TRACES_KEY, RECORD_STACK_TRACES_DEFAULT);
    }

    public void setTelemetryOverviewBehavior(final int value) {
        getPreferences().putInt(TO_BEHAVIOR_KEY, value);
    }

    public int getTelemetryOverviewBehavior() {
        return getPreferences().getInt(TO_BEHAVIOR_KEY, TO_BEHAVIOR_DEFAULT);
    }

    public void setThreadsMonitoringDefault(boolean value) {
        getPreferences().putBoolean(THREADS_MONITORING_KEY, value);
    }

    // SelectTaskPanel
    public boolean getThreadsMonitoringDefault() {
        return getPreferences().getBoolean(THREADS_MONITORING_KEY, THREADS_MONITORING_DEFAULT);
    }

    public void setThreadsViewBehavior(final int value) {
        getPreferences().putInt(TV_BEHAVIOR_KEY, value);
    }

    public int getThreadsViewBehavior() {
        return getPreferences().getInt(TV_BEHAVIOR_KEY, TV_BEHAVIOR_DEFAULT);
    }
    
    public void setLockContentionViewBehavior(final int value) {
        getPreferences().putInt(LCV_BEHAVIOR_KEY, value);
    }

    public int getLockContentionViewBehavior() {
        return getPreferences().getInt(LCV_BEHAVIOR_KEY, LCV_BEHAVIOR_DEFAULT);
    }

    public void setTrackEveryDefault(int value) {
        getPreferences().putInt(TRACK_EVERY_KEY, value);
    }

    public int getTrackEveryDefault() {
        return getPreferences().getInt(TRACK_EVERY_KEY, TRACK_EVERY_DEFAULT);
    }

    /**
     * Used by the DNSAConfirmation.
     *
     * Clears the Do not show again, so that all confirmations are displayed again.
     *
     * @see org.graalvm.visualvm.lib.profiler.impl.ProfilerDialogs.DNSAConfirmation
     * @see org.graalvm.visualvm.lib.profiler.impl.ProfilerDialogs.DNSAMessage
     */
    public void clearDoNotShowAgainMap() {
        getDNSAMap().clear();
        storeDNSAMap();
    }

    // -------------------------
    @NbBundle.Messages({
        "ProfilerIDESettings_Name=Profiler Settings"
    })
    public String displayName() {
        return Bundle.ProfilerIDESettings_Name();
    }

    private Map<String, String> getDNSAMap() {
        if (dnsaMap != null) {
            return dnsaMap;
        }

        dnsaMap = new HashMap();

        String allPairs = getPreferences().get(DNSA_SETTINGS_KEY, null);

        if (allPairs != null) {
            String[] pairs = allPairs.split(":"); //NOI18N
            for (String pair : pairs) {
                String[] elems = pair.split(","); //NOI18N
                assert elems.length == 2;
                dnsaMap.put(elems[0], elems[1]);
            }
        }

        return dnsaMap;
    }

    // --- Private stuff ---------------------------------------------------------
    private Preferences getPreferences() {
        return NbPreferences.forModule(ProfilerIDESettings.class);
    }

    private void storeDNSAMap() {
        StringBuilder sb = new StringBuilder();

        for (Iterator it = getDNSAMap().entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            sb.append(e.getKey()).append(",").append(e.getValue()); //NOI18N

            if (it.hasNext()) {
                sb.append(":"); //NOI18N
            }
        }

        String toStore = sb.toString();

        if (toStore.isEmpty()) {
            getPreferences().remove(DNSA_SETTINGS_KEY);
        } else {
            getPreferences().put(DNSA_SETTINGS_KEY, toStore);
        }
    }
}

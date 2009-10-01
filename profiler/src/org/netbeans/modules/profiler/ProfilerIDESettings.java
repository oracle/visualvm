/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler;

import java.util.prefs.BackingStoreException;
import org.netbeans.lib.profiler.common.GlobalProfilingSettings;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;


/**
 * SystemOption to store UI settings for profiler
 */
public final class ProfilerIDESettings implements GlobalProfilingSettings {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String PROFILER_IDE_SETTINGS_NAME = NbBundle.getMessage(ProfilerIDESettings.class,
                                                                                 "ProfilerIDESettings_Name" // NOI18N
    );

    // -----
    public static final String DO_NOT_SHOW_ATTACH_SETTINGS = "dns-attach-settings"; // NOI18N
    public static final String DO_NOT_SHOW_JDK_DIALOG = "dns-jdk-dialog"; // NOI18N
    public static final String DO_NOT_SHOW_PID_WINDOWS = "dns-pid-windows4"; // NOI18N
    public static final int CPU_ENTIRE_APP = 0;
    public static final int CPU_PART_APP = 1;
    public static final int CPU_STARTUP = 2;
    public static final int CPU_PROFILING_POINTS = 3;
    public static final int OOME_DETECTION_NONE = 0;
    public static final int OOME_DETECTION_TEMPDIR = 1;
    public static final int OOME_DETECTION_PROJECTDIR = 2;
    public static final int OOME_DETECTION_CUSTOMDIR = 3;

    /** The Window automatically opens always when profiling starts */
    public static final int OPEN_ALWAYS = 1;

    /** The Window automatically opens when profiling starts using only Monitoring */
    public static final int OPEN_MONITORING = 2;

    /** The Windows does not automatically open */
    public static final int OPEN_NEVER = 3;

    // --- Singleton pattern ---
    private static ProfilerIDESettings defaultInstance = new ProfilerIDESettings();

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

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ProfilerIDESettings getInstance() {
        return defaultInstance;
    }
    
    private ProfilerIDESettings() {
        try {
            if (contains55Settings()) convert55Settings();
        } catch (BackingStoreException e) {
            // Silently skip exception in NB55 settings
        }
    }
    
    
    //~ Conversion of 5.5 / 5.5.1 settings ---------------------------------------------------------------------------------------
    
    private boolean contains55Settings() {
        return getPreferences().getInt(PORT_NO_KEY_55, Integer.MIN_VALUE) != Integer.MIN_VALUE;
    }
    
    private void convert55Settings() throws BackingStoreException {
        Preferences preferences = getPreferences();
        String[] keys = preferences.keys();
        
        for (String key : keys) {
            if (AUTO_OPEN_SNAPSHOT_KEY_55.equals(key))
                convertBoolean(AUTO_OPEN_SNAPSHOT_KEY_55, AUTO_OPEN_SNAPSHOT_KEY, AUTO_OPEN_SNAPSHOT_DEFAULT, preferences);
            else if (AUTO_SAVE_SNAPSHOT_KEY_55.equals(key))
                convertBoolean(AUTO_SAVE_SNAPSHOT_KEY_55, AUTO_SAVE_SNAPSHOT_KEY, AUTO_SAVE_SNAPSHOT_DEFAULT, preferences);
            else if (CALIBRATION_PORT_NO_KEY_55.equals(key))
                convertInteger(CALIBRATION_PORT_NO_KEY_55, CALIBRATION_PORT_NO_KEY, CALIBRATION_PORT_NO_DEFAULT, preferences);
            else if (CPU_TASK_KEY_55.equals(key))
                convertInteger(CPU_TASK_KEY_55, CPU_TASK_KEY, CPU_TASK_DEFAULT, preferences);
            else if (LIVE_CPU_KEY_55.equals(key))
                convertBoolean(LIVE_CPU_KEY_55, LIVE_CPU_KEY, LIVE_CPU_DEFAULT, preferences);
            else if (LIVE_FRAGMENT_KEY_55.equals(key))
                convertBoolean(LIVE_FRAGMENT_KEY_55, LIVE_FRAGMENT_KEY, LIVE_FRAGMENT_DEFAULT, preferences);
            else if (LIVE_MEMORY_KEY_55.equals(key))
                convertBoolean(LIVE_MEMORY_KEY_55, LIVE_MEMORY_KEY, LIVE_MEMORY_DEFAULT, preferences);
            else if (MEMORY_TASK_ALLOCATIONS_KEY_55.equals(key))
                convertBoolean(MEMORY_TASK_ALLOCATIONS_KEY_55, MEMORY_TASK_ALLOCATIONS_KEY, MEMORY_TASK_ALLOCATIONS_DEFAULT, preferences);
            else if (PLATFORM_NAME_KEY_55.equals(key))
                convertString(PLATFORM_NAME_KEY_55, PLATFORM_NAME_KEY, PLATFORM_NAME_DEFAULT, preferences);
            else if (PORT_NO_KEY_55.equals(key))
                convertInteger(PORT_NO_KEY_55, PORT_NO_KEY, PORT_NO_DEFAULT, preferences);
            else if (RECORD_STACK_TRACES_KEY_55.equals(key))
                convertBoolean(RECORD_STACK_TRACES_KEY_55, RECORD_STACK_TRACES_KEY, RECORD_STACK_TRACES_DEFAULT, preferences);
            else if (THREADS_MONITORING_KEY_55.equals(key))
                convertBoolean(THREADS_MONITORING_KEY_55, THREADS_MONITORING_KEY, THREADS_MONITORING_DEFAULT, preferences);
            else if (TO_BEHAVIOR_KEY_55.equals(key))
                convertInteger(TO_BEHAVIOR_KEY_55, TO_BEHAVIOR_KEY, TO_BEHAVIOR_DEFAULT, preferences);
            else if (TRACK_EVERY_KEY_55.equals(key))
                convertInteger(TRACK_EVERY_KEY_55, TRACK_EVERY_KEY, TRACK_EVERY_DEFAULT, preferences);
            else if (TV_BEHAVIOR_KEY_55.equals(key))
                convertInteger(TV_BEHAVIOR_KEY_55, TV_BEHAVIOR_KEY, TV_BEHAVIOR_DEFAULT, preferences);
        }
    }
    
    private static void convertBoolean(String oldKey, String newKey, boolean defaultValue, Preferences preferences) {
        boolean value = preferences.getBoolean(oldKey, defaultValue);
        preferences.remove(oldKey);
        preferences.putBoolean(newKey, value);
    }
    
    private static void convertInteger(String oldKey, String newKey, int defaultValue, Preferences preferences) {
        int value = preferences.getInt(oldKey, defaultValue);
        preferences.remove(oldKey);
        preferences.putInt(newKey, value);
    }
    
    private static void convertString(String oldKey, String newKey, String defaultValue, Preferences preferences) {
        String value = preferences.get(oldKey, defaultValue);
        preferences.remove(oldKey);
        preferences.put(newKey, value);
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
     * @see org.netbeans.modules.profiler.ui.ProfilerDialogs.DNSAConfirmation
     * @see org.netbeans.modules.profiler.ui.ProfilerDialogs.DNSAMessage
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
     * @see org.netbeans.modules.profiler.ui.ProfilerDialogs.DNSAConfirmation
     * @see org.netbeans.modules.profiler.ui.ProfilerDialogs.DNSAMessage
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
     * @see org.netbeans.modules.profiler.ui.ProfilerDialogs.DNSAConfirmation
     * @see org.netbeans.modules.profiler.ui.ProfilerDialogs.DNSAMessage
     */
    public void clearDoNotShowAgainMap() {
        getDNSAMap().clear();
        storeDNSAMap();
    }

    // -------------------------
    public String displayName() {
        return PROFILER_IDE_SETTINGS_NAME;
    }

    private Map<String, String> getDNSAMap() {
        if (dnsaMap != null) {
            return dnsaMap;
        }

        dnsaMap = new HashMap();

        String allPairs = getPreferences().get(DNSA_SETTINGS_KEY, null);

        if (allPairs != null) {
            String[] pairs = allPairs.split(":"); //NOI18N

            for (int i = 0; i < pairs.length; i++) {
                String[] elems = pairs[i].split(","); //NOI18N
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
        StringBuffer sb = new StringBuffer();

        for (Iterator it = getDNSAMap().entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            sb.append(e.getKey()).append(",").append(e.getValue()); //NOI18N

            if (it.hasNext()) {
                sb.append(":"); //NOI18N
            }
        }

        String toStore = sb.toString();

        if (toStore.length() > 0) {
            getPreferences().put(DNSA_SETTINGS_KEY, toStore);
        } else {
            getPreferences().remove(DNSA_SETTINGS_KEY);
        }
    }
}

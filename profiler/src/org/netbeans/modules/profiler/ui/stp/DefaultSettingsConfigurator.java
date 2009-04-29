/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ui.stp;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.modules.profiler.ppoints.ui.ProfilingPointsDisplayer;
import org.netbeans.modules.profiler.spi.ProjectTypeProfiler;
import org.openide.filesystems.FileObject;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class DefaultSettingsConfigurator implements SelectProfilingTask.SettingsConfigurator {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class CPUContents implements SettingsContainerPanel.Contents, ChangeListener {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final boolean ENABLE_THREAD_CPU_TIMER = Boolean.getBoolean("org.netbeans.lib.profiler.enableThreadCPUTimer"); // NOI18N

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private CPUSettingsAdvancedPanel advancedSettingsPanel;
        private CPUSettingsBasicPanel basicSettingsPanel;
        private FileObject profiledFile;
        private ProfilingSettings settings;
        private Project project;
        private ProjectTypeProfiler ptp;
        private Vector<ChangeListener> changeListeners = new Vector();
        private boolean enableOverride;
        private boolean internalChange = false;
        private boolean isAttach;
        private boolean isModify;
        private boolean isPreset;
        private boolean useCPUTimer;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CPUContents() {
            basicSettingsPanel = new CPUSettingsBasicPanel();
            advancedSettingsPanel = new CPUSettingsAdvancedPanel();

            basicSettingsPanel.addChangeListener(this);
            advancedSettingsPanel.addChangeListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public JPanel getAdvancedSettingsPanel() {
            return advancedSettingsPanel;
        }

        public JPanel getBasicSettingsPanel() {
            return basicSettingsPanel;
        }

        public void setContext(Project project, FileObject profiledFile, boolean isAttach, boolean isModify,
                               boolean enableOverride) {
            this.project = project;
            ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);
            this.profiledFile = profiledFile;
            this.enableOverride = enableOverride;
            this.isAttach = isAttach;
            this.isModify = isModify;

            useCPUTimer = ENABLE_THREAD_CPU_TIMER || isAttach || Platform.isSolaris();
        }

        public float getProfilingOverhead() {
            if (settings == null) {
                return 0f;
            }

            synchronizeBasicAdvancedPanels();

            return ptp.getProfilingOverhead(createFinalSettings());
        }

        public void setSettings(ProfilingSettings settings) {
            this.settings = settings;
            isPreset = settings.isPreset();

            internalChange = true;

            // basicSettingsPanel
            basicSettingsPanel.setContext(project, SelectProfilingTask.getDefault().getPredefinedInstrFilterKeys(),
                                          new Runnable() {
                    public void run() {
                        synchronizeSettings();
                        ProfilingPointsDisplayer.displayProfilingPoints(project, CPUContents.this.settings);
                    }
                });
            basicSettingsPanel.setProfilingType(settings.getProfilingType());
            basicSettingsPanel.setRootMethods(settings.getInstrumentationRootMethods());
            basicSettingsPanel.setQuickFilter(settings.getQuickFilter());
            basicSettingsPanel.setInstrumentationFilter(settings.getSelectedInstrumentationFilter());
            basicSettingsPanel.setUseProfilingPoints(settings.useProfilingPoints() && (project != null));

            // advancedSettingsPanel
            if (!settings.isPreset()) {
                advancedSettingsPanel.enableAll();
            }

            advancedSettingsPanel.setCPUProfilingType(settings.getCPUProfilingType());
            advancedSettingsPanel.setSamplingInterval(settings.getSamplingInterval());
            advancedSettingsPanel.setExcludeThreadTime(settings.getExcludeWaitTime());
            advancedSettingsPanel.setProfileSpawnedThreads(settings.getInstrumentSpawnedThreads());
            advancedSettingsPanel.setUseCPUTimer(settings.getThreadCPUTimerOn(), useCPUTimer);
            advancedSettingsPanel.setInstrumentMethodInvoke(settings.getInstrumentMethodInvoke());
            advancedSettingsPanel.setInstrumentGettersSetters(settings.getInstrumentGetterSetterMethods());
            advancedSettingsPanel.setInstrumentEmptyMethods(settings.getInstrumentEmptyMethods());
            advancedSettingsPanel.setInstrumentationScheme(settings.getInstrScheme());
            advancedSettingsPanel.setProfiledThreadsLimit(settings.getNProfiledThreadsLimit());
            advancedSettingsPanel.setProfileFramework(settings.getProfileUnderlyingFramework());

            advancedSettingsPanel.setThreadsMonitoring(settings.getThreadsMonitoringEnabled());
            advancedSettingsPanel.setOverrideAvailable(enableOverride);
            advancedSettingsPanel.setOverrideSettings(settings.getOverrideGlobalSettings());
            advancedSettingsPanel.setWorkingDirectory(settings.getWorkingDir());
            advancedSettingsPanel.setJavaPlatformName(settings.getJavaPlatformName());
            advancedSettingsPanel.setVMArguments(settings.getJVMArgs());

            if (settings.isPreset()) {
                advancedSettingsPanel.disableAll();
            }

            internalChange = false;

            fireSettingsChanged();
        }

        public void addChangeListener(ChangeListener listener) {
            if (!changeListeners.contains(listener)) {
                changeListeners.add(listener);
            }
        }

        public ProfilingSettings createFinalSettings() {
            ProfilingSettings finalSettings = ProfilingSettingsPresets.createCPUPreset(settings.getProfilingType());

            finalSettings.setIsPreset(settings.isPreset());
            finalSettings.setSettingsName(settings.getSettingsName());

            // basicSettingsPanel
            finalSettings.setProfilingType(basicSettingsPanel.getProfilingType());

            finalSettings.setSelectedInstrumentationFilter(basicSettingsPanel.getInstrumentationFilter());
            finalSettings.setUseProfilingPoints(basicSettingsPanel.getUseProfilingPoints());

            // advancedSettingsPanel
            finalSettings.setCPUProfilingType(advancedSettingsPanel.getCPUProfilingType());
            finalSettings.setSamplingInterval(advancedSettingsPanel.getSamplingInterval());
            finalSettings.setExcludeWaitTime(advancedSettingsPanel.getExcludeThreadTime());
            finalSettings.setProfileUnderlyingFramework(advancedSettingsPanel.getProfileFramework());
            finalSettings.setInstrumentSpawnedThreads(advancedSettingsPanel.getProfileSpawnedThreads());
            finalSettings.setThreadCPUTimerOn(useCPUTimer && advancedSettingsPanel.getUseCPUTimer());
            finalSettings.setInstrumentMethodInvoke(advancedSettingsPanel.getInstrumentMethodInvoke());
            finalSettings.setInstrumentGetterSetterMethods(advancedSettingsPanel.getInstrumentGettersSetters());
            finalSettings.setInstrumentEmptyMethods(advancedSettingsPanel.getInstrumentEmptyMethods());
            finalSettings.setInstrScheme(advancedSettingsPanel.getInstrumentationScheme());
            finalSettings.setNProfiledThreadsLimit(advancedSettingsPanel.getProfiledThreadsLimit());

            finalSettings.setThreadsMonitoringEnabled(advancedSettingsPanel.getThreadsMonitoring());
            finalSettings.setOverrideGlobalSettings(advancedSettingsPanel.getOverrideSettings());
            finalSettings.setWorkingDir(advancedSettingsPanel.getWorkingDirectory());
            finalSettings.setJavaPlatformName(advancedSettingsPanel.getJavaPlatformName());
            finalSettings.setJVMArgs(advancedSettingsPanel.getVMArguments());

            // generated settings
            if (finalSettings.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE) {
                //finalSettings.setInstrumentationRootMethods(ProjectUtilities.getProjectTypeProfiler(project).getDefaultRootMethods(project, profiledFile, false));
                finalSettings.setInstrumentationRootMethods(new ClientUtils.SourceCodeSelection[0]);
                finalSettings.instrRootMethodsPending = true;
            } else {
                finalSettings.setInstrumentationRootMethods(basicSettingsPanel.getRootMethods());
            }

            return finalSettings;
        }

        public void removeChangeListener(ChangeListener listener) {
            changeListeners.remove(listener);
        }

        public void reset() {
            settings = null;
            project = null;
            ptp = null;
            profiledFile = null;
            enableOverride = false;
            isAttach = false;
            isModify = false;
            isPreset = false;
            useCPUTimer = false;
        }

        public void stateChanged(ChangeEvent e) {
            fireSettingsChanged();
        }

        public void synchronizeBasicAdvancedPanels() {
            //      if (isPreset()) {
            if (basicSettingsPanel.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE) {
                advancedSettingsPanel.setEntireAppDefaults(isPreset);
            } else {
                advancedSettingsPanel.setPartOfAppDefaults(isPreset);
            }

            //      }
        }

        public void synchronizeSettings() {
            synchronizeBasicAdvancedPanels();

            // basicSettingsPanel
            settings.setProfilingType(basicSettingsPanel.getProfilingType());
            settings.setInstrumentationRootMethods(basicSettingsPanel.getRootMethods());
            settings.setQuickFilter(basicSettingsPanel.getQuickFilter());
            settings.setSelectedInstrumentationFilter(basicSettingsPanel.getInstrumentationFilter());
            settings.setUseProfilingPoints(basicSettingsPanel.getUseProfilingPoints());

            // advancedSettingsPanel
            settings.setCPUProfilingType(advancedSettingsPanel.getCPUProfilingType());
            settings.setSamplingInterval(advancedSettingsPanel.getSamplingInterval());
            settings.setExcludeWaitTime(advancedSettingsPanel.getExcludeThreadTime());
            settings.setProfileUnderlyingFramework(advancedSettingsPanel.getProfileFramework());
            settings.setInstrumentSpawnedThreads(advancedSettingsPanel.getProfileSpawnedThreads());
            settings.setThreadCPUTimerOn(advancedSettingsPanel.getUseCPUTimer());
            settings.setInstrumentMethodInvoke(advancedSettingsPanel.getInstrumentMethodInvoke());
            settings.setInstrumentGetterSetterMethods(advancedSettingsPanel.getInstrumentGettersSetters());
            settings.setInstrumentEmptyMethods(advancedSettingsPanel.getInstrumentEmptyMethods());
            settings.setInstrScheme(advancedSettingsPanel.getInstrumentationScheme());
            settings.setNProfiledThreadsLimit(advancedSettingsPanel.getProfiledThreadsLimit());

            settings.setThreadsMonitoringEnabled(advancedSettingsPanel.getThreadsMonitoring());
            settings.setOverrideGlobalSettings(advancedSettingsPanel.getOverrideSettings());
            settings.setWorkingDir(advancedSettingsPanel.getWorkingDirectory());
            settings.setJavaPlatformName(advancedSettingsPanel.getJavaPlatformName());
            settings.setJVMArgs(advancedSettingsPanel.getVMArguments());
        }

        private void fireSettingsChanged() {
            if (!internalChange) {
                for (ChangeListener listener : changeListeners) {
                    listener.stateChanged(new ChangeEvent(this));
                }
            }
        }
    }

    public static class MemoryContents implements SettingsContainerPanel.Contents, ChangeListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private FileObject profiledFile;
        private MemorySettingsAdvancedPanel advancedSettingsPanel = new MemorySettingsAdvancedPanel();
        private MemorySettingsBasicPanel basicSettingsPanel = new MemorySettingsBasicPanel();
        private ProfilingSettings settings;
        private Project project;
        private ProjectTypeProfiler ptp;
        private Vector<ChangeListener> changeListeners = new Vector();
        private boolean enableOverride;
        private boolean internalChange = false;
        private boolean isAttach;
        private boolean isModify;
        private boolean isPreset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public MemoryContents() {
            basicSettingsPanel = new MemorySettingsBasicPanel();
            advancedSettingsPanel = new MemorySettingsAdvancedPanel();

            basicSettingsPanel.addChangeListener(this);
            advancedSettingsPanel.addChangeListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public JPanel getAdvancedSettingsPanel() {
            return advancedSettingsPanel;
        }

        public JPanel getBasicSettingsPanel() {
            return basicSettingsPanel;
        }

        public void setContext(Project project, FileObject profiledFile, boolean isAttach, boolean isModify,
                               boolean enableOverride) {
            this.project = project;
            ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);
            this.profiledFile = profiledFile;
            this.enableOverride = enableOverride;
            this.isAttach = isAttach;
            this.isModify = isModify;
        }

        public float getProfilingOverhead() {
            if (settings == null) {
                return 0f;
            }

            synchronizeBasicAdvancedPanels();

            return ptp.getProfilingOverhead(createFinalSettings());
        }

        public void setSettings(ProfilingSettings settings) {
            this.settings = settings;
            isPreset = settings.isPreset();

            internalChange = true;

            SelectProfilingTask.getDefault().enableSubmitButton();

            // basicSettingsPanel
            basicSettingsPanel.setContext(project,
                                          new Runnable() {
                    public void run() {
                        synchronizeSettings();
                        ProfilingPointsDisplayer.displayProfilingPoints(project, MemoryContents.this.settings);
                    }
                });
            basicSettingsPanel.setProfilingType(settings.getProfilingType());
            basicSettingsPanel.setTrackEvery(settings.getAllocTrackEvery());
            basicSettingsPanel.setRecordStackTrace(settings.getAllocStackTraceLimit() != 0);
            basicSettingsPanel.setUseProfilingPoints(settings.useProfilingPoints() && (project != null));

            // advancedSettingsPanel
            if (!settings.isPreset()) {
                advancedSettingsPanel.enableAll();
            }

            advancedSettingsPanel.setAllocStackTraceLimit(settings.getAllocStackTraceLimit());
            advancedSettingsPanel.setRunGC(settings.getRunGCOnGetResultsInMemoryProfiling());

            advancedSettingsPanel.setThreadsMonitoring(settings.getThreadsMonitoringEnabled());
            advancedSettingsPanel.setOverrideAvailable(enableOverride);
            advancedSettingsPanel.setOverrideSettings(settings.getOverrideGlobalSettings());
            advancedSettingsPanel.setWorkingDirectory(settings.getWorkingDir());
            advancedSettingsPanel.setJavaPlatformName(settings.getJavaPlatformName());
            advancedSettingsPanel.setVMArguments(settings.getJVMArgs());

            if (settings.isPreset()) {
                advancedSettingsPanel.disableAll();
            }

            internalChange = false;

            fireSettingsChanged();
        }

        public void addChangeListener(ChangeListener listener) {
            if (!changeListeners.contains(listener)) {
                changeListeners.add(listener);
            }
        }

        public ProfilingSettings createFinalSettings() {
            ProfilingSettings finalSettings = ProfilingSettingsPresets.createMemoryPreset();

            finalSettings.setIsPreset(settings.isPreset());
            finalSettings.setSettingsName(settings.getSettingsName());

            // basicSettingsPanel
            finalSettings.setProfilingType(basicSettingsPanel.getProfilingType());
            finalSettings.setAllocTrackEvery(basicSettingsPanel.getTrackEvery());
            finalSettings.setUseProfilingPoints(basicSettingsPanel.getUseProfilingPoints());

            // advancedSettingsPanel
            finalSettings.setAllocStackTraceLimit(basicSettingsPanel.getRecordStackTrace()
                                                  ? advancedSettingsPanel.getAllocStackTraceLimit() : 0);
            finalSettings.setRunGCOnGetResultsInMemoryProfiling(advancedSettingsPanel.getRunGC());

            finalSettings.setThreadsMonitoringEnabled(advancedSettingsPanel.getThreadsMonitoring());
            finalSettings.setOverrideGlobalSettings(advancedSettingsPanel.getOverrideSettings());
            finalSettings.setWorkingDir(advancedSettingsPanel.getWorkingDirectory());
            finalSettings.setJavaPlatformName(advancedSettingsPanel.getJavaPlatformName());
            finalSettings.setJVMArgs(advancedSettingsPanel.getVMArguments());

            return finalSettings;
        }

        public void removeChangeListener(ChangeListener listener) {
            changeListeners.remove(listener);
        }

        public void reset() {
            settings = null;
            project = null;
            ptp = null;
            profiledFile = null;
            enableOverride = false;
            isAttach = false;
            isModify = false;
            isPreset = false;
        }

        public void stateChanged(ChangeEvent e) {
            fireSettingsChanged();
        }

        public void synchronizeBasicAdvancedPanels() {
            boolean recordStackTrace = basicSettingsPanel.getRecordStackTrace();
            advancedSettingsPanel.setRecordStackTrace(recordStackTrace);
            advancedSettingsPanel.updateRunGC(basicSettingsPanel.getProfilingType() ==
                                              ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS);
        }

        public void synchronizeSettings() {
            synchronizeBasicAdvancedPanels();

            // basicSettingsPanel
            settings.setProfilingType(basicSettingsPanel.getProfilingType());
            settings.setAllocTrackEvery(basicSettingsPanel.getTrackEvery());
            settings.setUseProfilingPoints(basicSettingsPanel.getUseProfilingPoints());

            // advancedSettingsPanel
            settings.setAllocStackTraceLimit(basicSettingsPanel.getRecordStackTrace()
                                             ? advancedSettingsPanel.getAllocStackTraceLimit() : 0);
            settings.setRunGCOnGetResultsInMemoryProfiling(advancedSettingsPanel.getRunGC());

            settings.setThreadsMonitoringEnabled(advancedSettingsPanel.getThreadsMonitoring());
            settings.setOverrideGlobalSettings(advancedSettingsPanel.getOverrideSettings());
            settings.setWorkingDir(advancedSettingsPanel.getWorkingDirectory());
            settings.setJavaPlatformName(advancedSettingsPanel.getJavaPlatformName());
            settings.setJVMArgs(advancedSettingsPanel.getVMArguments());
        }

        private void fireSettingsChanged() {
            if (!internalChange) {
                for (ChangeListener listener : changeListeners) {
                    listener.stateChanged(new ChangeEvent(this));
                }
            }
        }
    }

    // --- Public contents to be reused by SelectProfilingTask.SettingsConfigurator implementors
    public static class MonitorContents implements SettingsContainerPanel.Contents, ChangeListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private FileObject profiledFile;
        private MonitorSettingsAdvancedPanel advancedSettingsPanel;
        private MonitorSettingsBasicPanel basicSettingsPanel;
        private ProfilingSettings settings;
        private Project project;
        private ProjectTypeProfiler ptp;
        private Vector<ChangeListener> changeListeners = new Vector();
        private boolean enableOverride;
        private boolean internalChange = false;
        private boolean isAttach;
        private boolean isModify;
        private boolean isPreset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public MonitorContents() {
            basicSettingsPanel = new MonitorSettingsBasicPanel();
            advancedSettingsPanel = new MonitorSettingsAdvancedPanel();

            basicSettingsPanel.addChangeListener(this);
            advancedSettingsPanel.addChangeListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public JPanel getAdvancedSettingsPanel() {
            return advancedSettingsPanel;
        }

        public JPanel getBasicSettingsPanel() {
            return basicSettingsPanel;
        }

        public void setContext(Project project, FileObject profiledFile, boolean isAttach, boolean isModify,
                               boolean enableOverride) {
            this.project = project;
            ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);
            this.profiledFile = profiledFile;
            this.enableOverride = enableOverride;
            this.isAttach = isAttach;
            this.isModify = isModify;
        }

        public float getProfilingOverhead() {
            if (settings == null) {
                return 0f;
            }

            synchronizeBasicAdvancedPanels();

            return ptp.getProfilingOverhead(createFinalSettings());
        }

        public void setSettings(ProfilingSettings settings) {
            this.settings = settings;
            isPreset = settings.isPreset();

            internalChange = true;

            SelectProfilingTask.getDefault().enableSubmitButton();

            // basicSettingsPanel
            basicSettingsPanel.setThreadsMonitoring(settings.getThreadsMonitoringEnabled());

            // advancedSettingsPanel
            if (!settings.isPreset()) {
                advancedSettingsPanel.enableAll();
            }

            advancedSettingsPanel.setOverrideAvailable(enableOverride);
            advancedSettingsPanel.setOverrideSettings(settings.getOverrideGlobalSettings());
            advancedSettingsPanel.setWorkingDirectory(settings.getWorkingDir());
            advancedSettingsPanel.setJavaPlatformName(settings.getJavaPlatformName());
            advancedSettingsPanel.setVMArguments(settings.getJVMArgs());

            if (settings.isPreset()) {
                advancedSettingsPanel.disableAll();
            }

            internalChange = false;

            fireSettingsChanged();
        }

        public void addChangeListener(ChangeListener listener) {
            if (!changeListeners.contains(listener)) {
                changeListeners.add(listener);
            }
        }

        public ProfilingSettings createFinalSettings() {
            ProfilingSettings finalSettings = ProfilingSettingsPresets.createMonitorPreset();

            finalSettings.setIsPreset(settings.isPreset());
            finalSettings.setSettingsName(settings.getSettingsName());

            // basicSettingsPanel
            finalSettings.setThreadsMonitoringEnabled(basicSettingsPanel.getThreadsMonitoring());

            // advancedSettingsPanel
            finalSettings.setOverrideGlobalSettings(advancedSettingsPanel.getOverrideSettings());
            finalSettings.setWorkingDir(advancedSettingsPanel.getWorkingDirectory());
            finalSettings.setJavaPlatformName(advancedSettingsPanel.getJavaPlatformName());
            finalSettings.setJVMArgs(advancedSettingsPanel.getVMArguments());

            return finalSettings;
        }

        public void removeChangeListener(ChangeListener listener) {
            changeListeners.remove(listener);
        }

        public void reset() {
            settings = null;
            project = null;
            ptp = null;
            profiledFile = null;
            enableOverride = false;
            isAttach = false;
            isModify = false;
            isPreset = false;
        }

        public void stateChanged(ChangeEvent e) {
            fireSettingsChanged();
        }

        public void synchronizeBasicAdvancedPanels() {
        }

        public void synchronizeSettings() {
            synchronizeBasicAdvancedPanels();

            // basicSettingsPanel
            settings.setThreadsMonitoringEnabled(basicSettingsPanel.getThreadsMonitoring());

            // advancedSettingsPanel
            settings.setOverrideGlobalSettings(advancedSettingsPanel.getOverrideSettings());
            settings.setWorkingDir(advancedSettingsPanel.getWorkingDirectory());
            settings.setJavaPlatformName(advancedSettingsPanel.getJavaPlatformName());
            settings.setJVMArgs(advancedSettingsPanel.getVMArguments());
        }

        private void fireSettingsChanged() {
            if (!internalChange) {
                for (ChangeListener listener : changeListeners) {
                    listener.stateChanged(new ChangeEvent(this));
                }
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // --- Shared instance -------------------------------------------------------
    public static final DefaultSettingsConfigurator SHARED_INSTANCE = new DefaultSettingsConfigurator();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CPUContents cpuContents = new CPUContents();
    private FileObject profiledFile;
    private MemoryContents memoryContents = new MemoryContents();
    private MonitorContents monitorContents = new MonitorContents();

    // --- Instance variables ----------------------------------------------------
    private ProfilingSettings settings;
    private Project project;
    private boolean enableOverride;
    private boolean isAttach;
    private boolean isModify;
    private boolean isPreset;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public SettingsContainerPanel.Contents getCPUConfigurator() {
        return cpuContents;
    }

    public void setContext(Project project, FileObject profiledFile, boolean isAttach, boolean isModify, boolean enableOverride) {
        this.project = project;
        this.profiledFile = profiledFile;
        this.enableOverride = enableOverride;
        this.isAttach = isAttach;
        this.isModify = isModify;

        monitorContents.setContext(project, profiledFile, isAttach, isModify, enableOverride);
        cpuContents.setContext(project, profiledFile, isAttach, isModify, enableOverride);
        memoryContents.setContext(project, profiledFile, isAttach, isModify, enableOverride);
    }

    public JPanel getCustomSettingsPanel() {
        return null;
    }

    public SettingsContainerPanel.Contents getMemoryConfigurator() {
        return memoryContents;
    }

    // --- SettingsConfigurator implementation -----------------------------------
    public SettingsContainerPanel.Contents getMonitorConfigurator() {
        return monitorContents;
    }

    // Initializes UI according to the settings
    public void setSettings(ProfilingSettings settings) {
        this.settings = settings;
        isPreset = settings.isPreset();

        if (Utils.isMonitorSettings(settings)) {
            monitorContents.setSettings(settings);
        } else if (Utils.isCPUSettings(settings)) {
            cpuContents.setSettings(settings);
        } else if (Utils.isMemorySettings(settings)) {
            memoryContents.setSettings(settings);
        }
    }

    public ProfilingSettings getSettings() {
        return settings;
    }

    public ProfilingSettings createFinalSettings() {
        //////    return getSettings(); // TODO: create settings to be used for profiling
        if (Utils.isMonitorSettings(settings)) {
            return monitorContents.createFinalSettings();
        } else if (Utils.isCPUSettings(settings)) {
            return cpuContents.createFinalSettings();
        } else if (Utils.isMemorySettings(settings)) {
            return memoryContents.createFinalSettings();
        }

        return null;
    }

    public void loadCustomSettings(Properties properties) {
    }

    public void reset() {
        settings = null;
        project = null;
        profiledFile = null;
        enableOverride = false;
        isAttach = false;
        isModify = false;
        isPreset = false;
        monitorContents.reset();
        cpuContents.reset();
        memoryContents.reset();
    }

    public void storeCustomSettings(Properties properties) {
    }

    // Updates settings according to the UI
    public void synchronizeSettings() {
        if (Utils.isMonitorSettings(settings)) {
            monitorContents.synchronizeSettings();
        } else if (Utils.isCPUSettings(settings)) {
            cpuContents.synchronizeSettings();
        } else if (Utils.isMemorySettings(settings)) {
            memoryContents.synchronizeSettings();
        }
    }

    protected boolean isAttach() {
        return isAttach;
    }

    protected boolean isModify() {
        return isModify;
    }

    protected boolean isPreset() {
        return isPreset;
    }

    protected FileObject getProfiledFile() {
        return profiledFile;
    }

    // --- Protected interface ---------------------------------------------------
    protected Project getProject() {
        return project;
    }

    protected boolean enableOverride() {
        return enableOverride;
    }
}

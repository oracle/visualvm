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

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.components.ImagePanel;
import org.netbeans.lib.profiler.ui.components.VerticalLayout;
import org.netbeans.lib.profiler.ui.components.XPStyleBorder;
import org.netbeans.modules.profiler.spi.ProjectTypeProfiler;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.results.cpu.marking.MarkingEngine;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.categories.Categorization;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;
import org.openide.util.Lookup;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class SelectProfilingTask extends JPanel implements TaskChooser.Listener, HelpCtx.Provider {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    // --- SettingsConfigurator Interface ----------------------------------------
    public static interface SettingsConfigurator {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        //    public SettingsContainerPanel.Contents getAnalyzerConfigurator();
        public SettingsContainerPanel.Contents getCPUConfigurator();

        public void setContext(Project project, FileObject profiledFile, boolean isAttach, boolean isModify,
                               boolean enableOverride);

        // Provides extra component for ProjectTypeProfiler-specific settings
        public JPanel getCustomSettingsPanel();

        public SettingsContainerPanel.Contents getMemoryConfigurator();

        public SettingsContainerPanel.Contents getMonitorConfigurator();

        // Initializes UI according to the settings
        public void setSettings(ProfilingSettings settings);

        // Returns settings set by setSettings
        public ProfilingSettings getSettings();

        // Creates ProfilingSettings to be used for profiling based on settings set by setSettings
        public ProfilingSettings createFinalSettings();

        // Loads custom settings from project's properties
        public void loadCustomSettings(Properties properties);

        // Resets the context
        public void reset();

        // Stores custom settings from project's properties
        public void storeCustomSettings(Properties properties);

        // Updates settings according to the UI
        public void synchronizeSettings();
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // --- Innerclass for passing results ----------------------------------------
    public static class Configuration {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private AttachSettings attachSettings;
        private ProfilingSettings profilingSettings;
        private Project project;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        Configuration(Project project, ProfilingSettings profilingSettings, AttachSettings attachSettings) {
            this.project = project;
            this.profilingSettings = profilingSettings;
            this.attachSettings = attachSettings;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public AttachSettings getAttachSettings() {
            return attachSettings;
        }

        public ProfilingSettings getProfilingSettings() {
            return profilingSettings;
        }

        public Project getProject() {
            return project;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SELECT_PROJECT_TO_ATTACH_STRING = NbBundle.getMessage(SelectProfilingTask.class,
                                                                                      "SelectProfilingTask_SelectProjectToAttachString"); // NOI18N
    public static final String EXTERNAL_APPLICATION_STRING = NbBundle.getMessage(SelectProfilingTask.class,
                                                                                 "SelectProfilingTask_ExternalApplicationString"); // NOI18N
    private static final String PROFILE_DIALOG_CAPTION = NbBundle.getMessage(SelectProfilingTask.class,
                                                                             "SelectProfilingTask_ProfileDialogCaption"); // NOI18N
    private static final String ATTACH_DIALOG_CAPTION = NbBundle.getMessage(SelectProfilingTask.class,
                                                                            "SelectProfilingTask_AttachDialogCaption"); // NOI18N
    private static final String MODIFY_DIALOG_CAPTION = NbBundle.getMessage(SelectProfilingTask.class,
                                                                            "SelectProfilingTask_ModifyDialogCaption"); // NOI18N
    private static final String MONITOR_STRING = NbBundle.getMessage(SelectProfilingTask.class,
                                                                     "SelectProfilingTask_MonitorString"); // NOI18N
    private static final String CPU_STRING = NbBundle.getMessage(SelectProfilingTask.class, "SelectProfilingTask_CpuString"); // NOI18N
    private static final String MEMORY_STRING = NbBundle.getMessage(SelectProfilingTask.class, "SelectProfilingTask_MemoryString"); // NOI18N
    private static final String ATTACH_LABEL_TEXT = NbBundle.getMessage(SelectProfilingTask.class,
                                                                        "SelectProfilingTask_AttachLabelText"); // NOI18N
    private static final String RUN_BUTTON_TEXT = NbBundle.getMessage(SelectProfilingTask.class,
                                                                      "SelectProfilingTask_RunButtonText"); // NOI18N
    private static final String ATTACH_BUTTON_TEXT = NbBundle.getMessage(SelectProfilingTask.class,
                                                                         "SelectProfilingTask_AttachButtonText"); // NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(SelectProfilingTask.class, "SelectProfilingTask_OkButtonText"); // NOI18N
    private static final String CANCEL_BUTTON_TEXT = NbBundle.getMessage(SelectProfilingTask.class,
                                                                         "SelectProfilingTask_CancelButtonText"); // NOI18N
    private static final String INIT_SESSION_STRING = NbBundle.getMessage(SelectProfilingTask.class,
                                                                          "SelectProfilingTask_InitSessionString"); // NOI18N
    private static final String CHOOSER_COMBO_ACCESS_DESCR = NbBundle.getMessage(SelectProfilingTask.class,
                                                                                 "SelectProfilingTask_ChooserComboAccessDescr"); // NOI18N
    private static final String WORKDIR_INVALID_MSG = NbBundle.getMessage(SelectProfilingTask.class,
                                                                                 "SelectProfilingTask_WorkDirInvalidMsg"); // NOI18N
                                                                                                                                 // -----

    // --- Constants declaration -------------------------------------------------
    public static Color BACKGROUND_COLOR;
    public static Color BACKGROUND_COLOR_INACTIVE;
    public static Color DARKLINK_COLOR;
    public static Color DARKLINK_COLOR_INACTIVE;
    
    static { initColors(); }

    // --- Instance variables declaration ----------------------------------------
    private static SelectProfilingTask defaultInstance;

    // --- UI components declaration ---------------------------------------------
    private static final Image BACKGROUND_IMAGE = UIUtils.isNimbus() ? null : ImageUtilities.loadImage("org/netbeans/modules/profiler/ui/stp/resources/sptBar.png"); // NOI18N
    private static final Icon MONITOR_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ui/resources/monitoring.png", false); // NOI18N
    private static final Icon CPU_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ui/resources/cpu.png", false); // NOI18N
    private static final Icon MEMORY_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ui/resources/memory.png", false); // NOI18N
    private static final Icon RUN_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/actions/resources/runButton.gif", false); // NOI18N
    private static final Icon ATTACH_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/actions/resources/attachButton.gif", false); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AttachSettingsPanel attachSettingsPanel;
    private DialogDescriptor dd;
    private FileObject profiledFile;
    private JPanel taskChooserPanel;
    private JButton attachButton;
    private JButton cancelButton;
    private JButton modifyButton;
    private JButton runButton;
    private JButton submitButton;
    private JComboBox projectsChooserCombo;
    private JLabel projectsChooserLabel;
    private JPanel attachSettingsPanelContainer;
    private JPanel contentsPanel;
    private JPanel customSettingsPanelContainer;
    private JPanel extraSettingsPanel;
    private JPanel projectsChooserComboContainer;
    private JPanel projectsChooserPanel;
    private JSeparator attachSettingsPanelSeparator;
    private JSeparator customSettingsPanelSeparator;
    private JSeparator extraSettingsPanelSeparator;
    private JSeparator projectsChooserSeparator;
    private List<SimpleFilter> predefinedInstrFilterKeys;
    private Object lastAttachProject; // Actually may be also EXTERNAL_APPLICATION_STRING, is reset to null when project is closed
    private Project project;
    private SettingsConfigurator configurator;
    private SettingsContainerPanel settingsContainerPanel;
    private TaskChooser taskChooser;
    private TaskPresenter selectedTask;

    //private TaskPresenter taskAnalyzer;
    private TaskPresenter taskCPU;
    private TaskPresenter taskMemory;
    private TaskPresenter taskMonitor;
    private WeakReference<WelcomePanel> welcomePanelReference;
    private SimpleFilter[] predefinedInstrFilters;

    // used for lazy-computing default root methods & project-related instrumentation filters
    private String[][] projectPackages;
    private boolean enableOverride;
    private boolean internalComboChange = false;
    private boolean isAttach;
    private boolean isModify;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Private implementation ------------------------------------------------
    private SelectProfilingTask() {
        initClosedProjectHook();
        initComponents();
        initTasks();
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initTaskChooserSize();
                    initPreferredSize();
                }
            });
    }
    
    private static void initColors() {
        Color systemBackgroundColor = UIUtils.getProfilerResultsBackground();
        
        int backgroundRed = systemBackgroundColor.getRed(); 
        int backgroundGreen = systemBackgroundColor.getGreen();
        int backgroundBlue = systemBackgroundColor.getBlue();
        boolean inverseColors = backgroundRed < 18 || backgroundGreen < 18 || backgroundBlue < 18;
        
        if (inverseColors) {
            BACKGROUND_COLOR = UIUtils.getSafeColor(backgroundRed + 11, backgroundGreen + 11, backgroundBlue + 11);
            BACKGROUND_COLOR_INACTIVE = UIUtils.getSafeColor(backgroundRed + 18, backgroundGreen + 18, backgroundBlue + 18);
        } else {
            BACKGROUND_COLOR = UIUtils.getSafeColor(backgroundRed - 11 /*244*/, backgroundGreen - 11 /*244*/, backgroundBlue - 11 /*244*/);
            BACKGROUND_COLOR_INACTIVE = UIUtils.getSafeColor(backgroundRed - 18 /*237*/, backgroundGreen - 18 /*237*/, backgroundBlue - 18 /*237*/);
        }
        
        boolean textInverse = BACKGROUND_COLOR_INACTIVE.getRed() - Color.DARK_GRAY.getRed() < 50;
        
        if (textInverse) {
            int darklinkExtent = Color.DARK_GRAY.getRed() - Color.BLACK.getRed();
            int darklinkInverse = Color.WHITE.getRed() - darklinkExtent;
            DARKLINK_COLOR = Color.WHITE;
            DARKLINK_COLOR_INACTIVE = UIUtils.getSafeColor(darklinkInverse, darklinkInverse, darklinkInverse);
        } else {
            DARKLINK_COLOR = Color.BLACK;
            DARKLINK_COLOR_INACTIVE = Color.DARK_GRAY;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static Configuration selectAttachProfilerTask(Project project) { // profiledFile = null, enableOverride = false,
        // Running this code in EDT would cause deadlock
        assert !SwingUtilities.isEventDispatchThread();

        final SelectProfilingTask spt = getDefault();
        spt.setSubmitButton(spt.attachButton);
        spt.setupAttachProfiler(project);

        spt.dd = new DialogDescriptor(spt, ATTACH_DIALOG_CAPTION, true, new Object[] { spt.attachButton, spt.cancelButton },
                                      spt.attachButton, 0, null, null);

        final CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Dialog d = ProfilerDialogs.createDialog(spt.dd);
                d.pack();
                d.setVisible(true);
                latch.countDown();
            }
        });

        try {
            latch.await();

            Configuration result = null;

            if (spt.dd.getValue() == spt.attachButton) {
                result = new Configuration(spt.project, spt.createFinalSettings(), spt.getAttachSettings());
            }

            spt.cleanup();

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public static Configuration selectModifyProfilingTask(Project project, FileObject profiledFile, boolean isAttach) { // profiledFile = null, enableOverride = false,
        // Running this code in EDT would cause deadlock
        assert !SwingUtilities.isEventDispatchThread();

        final SelectProfilingTask spt = getDefault();
        spt.setSubmitButton(spt.modifyButton);
        spt.setupModifyProfiling(project, profiledFile, isAttach);

        spt.dd = new DialogDescriptor(spt,
                                      MessageFormat.format(MODIFY_DIALOG_CAPTION, new Object[] { Utils.getProjectName(project) }),
                                      true, new Object[] { spt.modifyButton, spt.cancelButton }, spt.modifyButton, 0, null, null);

        final CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Dialog d = ProfilerDialogs.createDialog(spt.dd);
                d.pack();
                d.setVisible(true);
                latch.countDown();
            }
        });

        try {
            latch.await();

            Configuration result = null;

            if (spt.dd.getValue() == spt.modifyButton) {
                result = new Configuration(project, spt.createFinalSettings(), null);
            }

            spt.cleanup();

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    // --- Public interface ------------------------------------------------------
    public static Configuration selectProfileProjectTask(Project project, FileObject profiledFile, boolean enableOverride) {
        // Running this code in EDT would cause deadlock
        assert !SwingUtilities.isEventDispatchThread();
        
        final SelectProfilingTask spt = getDefault();
        spt.setSubmitButton(spt.runButton);
        spt.setupProfileProject(project, profiledFile, enableOverride);

        String targetName = Utils.getProjectName(project) + ((profiledFile == null) ? "" : (": " + profiledFile.getNameExt())); // NOI18N
        spt.dd = new DialogDescriptor(spt, MessageFormat.format(PROFILE_DIALOG_CAPTION, new Object[] { targetName }), true,
                                      new Object[] { spt.runButton, spt.cancelButton }, spt.runButton, 0, null, null);

        final CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Dialog d = ProfilerDialogs.createDialog(spt.dd);
                d.pack();
                d.setVisible(true);
                latch.countDown();
            }
        });

        try {
            latch.await();

            Configuration result = null;

            if (spt.dd.getValue() == spt.runButton) {
                ProfilingSettings settings = spt.createFinalSettings();
                if (settings.getOverrideGlobalSettings()) {
                    String workDir = settings.getWorkingDir().trim();
                    if (workDir.length() != 0 && !new java.io.File(workDir).exists()) {
                        settings.setWorkingDir(""); // NOI18N
                        NetBeansProfiler.getDefaultNB().displayWarning(WORKDIR_INVALID_MSG);
                    }
                }
                result = new Configuration(project, settings, null);
            }

            spt.cleanup();

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public HelpCtx getHelpCtx() {
        return settingsContainerPanel.getHelpCtx();
    }

    public void itemCollapsed(TaskChooser.Item item) {
    }

    public void itemExpanded(TaskChooser.Item item) {
        selectedTask.selectProfilingSettings(((TaskPresenter) item).getSelectedProfilingSettings());
    } // Workaround to focus selected settings after expanding the task

    public void itemWillCollapse(TaskChooser.Item item) {
    }

    // --- TaskChooser.Listener implementation -----------------------------------
    public void itemWillExpand(TaskChooser.Item item) {
        selectProfilingSettings(((TaskPresenter) item).getSelectedProfilingSettings());
    }

    // --- Internal interface ----------------------------------------------------
    static SelectProfilingTask getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new SelectProfilingTask();
        }

        return defaultInstance;
    }

    List<SimpleFilter> getPredefinedInstrFilterKeys() {
        return predefinedInstrFilterKeys;
    }

    SimpleFilter getResolvedPredefinedFilter(SimpleFilter key) {
        ProjectTypeProfiler ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);

        int resolvedIndex = predefinedInstrFilterKeys.indexOf(key); // takes some time for long filter values

        if (resolvedIndex == -1) {
            return null; // Should never happen
        }

        if (predefinedInstrFilters[resolvedIndex] == null) {
            predefinedInstrFilters[resolvedIndex] = ptp.computePredefinedInstrumentationFilter(project, key, projectPackages);
        }

        return predefinedInstrFilters[resolvedIndex];
    }

    void setSubmitButton(JButton submitButton) {
        this.submitButton = submitButton;
    }

    void disableSubmitButton() {
        if (submitButton != null) {
            submitButton.setEnabled(false);
        }
    }

    void enableSubmitButton() {
        if (submitButton != null) {
            submitButton.setEnabled(true);
        }
    }

    // Currently selected settings are updated according to current configuration (Basic & Advanced settings panels)
    void synchronizeCurrentSettings() {
        if ((configurator != null) && (configurator.getSettings() != null)) {
            configurator.synchronizeSettings();
        }
    }

    void updateHelpCtx() {
        if (dd != null) {
            dd.setHelpCtx(getHelpCtx());
        }
    }

    private AttachSettings getAttachSettings() {
        return attachSettingsPanel.getSettings();
    }

    private TaskPresenter getTaskPresenter(ProfilingSettings profilingSettings) {
        if (profilingSettings == null) {
            return null;
        }

        // NOTE: ideally TaskPresenter.createFinalSettings().contains should be used but this is cheaper
        if (Utils.isMonitorSettings(profilingSettings)) {
            return taskMonitor;
        } else if (Utils.isCPUSettings(profilingSettings)) {
            return taskCPU;
        } else if (Utils.isMemorySettings(profilingSettings)) {
            return taskMemory;
        } else {
            return null;
        }
    }

    private WelcomePanel getWelcomePanel() {
        WelcomePanel welcomePanel;

        if ((welcomePanelReference == null) || (welcomePanelReference.get() == null)) {
            welcomePanel = new WelcomePanel();
            welcomePanelReference = new WeakReference(welcomePanel);
        } else {
            welcomePanel = welcomePanelReference.get();
        }

        return welcomePanel;
    }

    private void cleanup() {
        // store settings if project is selected
        if (!projectsChooserPanel.isVisible() || (projectsChooserCombo.getSelectedItem() != SELECT_PROJECT_TO_ATTACH_STRING)) {
            storeCurrentSettings();
        }

        projectCleanup();

        project = null;
        profiledFile = null;
        enableOverride = false;
        isAttach = false;
        isModify = false;

        contentsPanel.removeAll();
        customSettingsPanelContainer.removeAll();
        internalComboChange = true;
        projectsChooserCombo.removeAllItems();
        internalComboChange = false;

        submitButton = null;

        projectPackages = null;
        predefinedInstrFilters = null;
        predefinedInstrFilterKeys = null;
        dd = null;

        // Persist customized dialog size
        contentsPanel.setPreferredSize(contentsPanel.getSize());
    }

    private ProfilingSettings createFinalSettings() {
        if (configurator != null) {
            synchronizeCurrentSettings();

            ProfilingSettings settings = configurator.createFinalSettings();

            final ProgressHandle pHandle = ProgressHandleFactory.createHandle(INIT_SESSION_STRING);
            pHandle.setInitialDelay(0);
            pHandle.start();
            
            try {

                if (project != null) {
                    boolean rootMethodsPending = settings.instrRootMethodsPending;
                    boolean predefinedFilterPending = predefinedInstrFilterKeys.contains(settings.getSelectedInstrumentationFilter());

                    if (rootMethodsPending || predefinedFilterPending) {
                        // Lazily compute default root methods
                        if (rootMethodsPending) {
                            settings.setInstrumentationRootMethods(org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project)
                                                                                   .getDefaultRootMethods(project, profiledFile,
                                                                                                          settings
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           .getProfileUnderlyingFramework(),
                                                                                                          projectPackages));
                        }

                        // Lazily compute instrumentation filters
                        if (predefinedFilterPending) {
                            settings.setSelectedInstrumentationFilter(getResolvedPredefinedFilter((SimpleFilter) settings
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .getSelectedInstrumentationFilter()));
                        }
                    }

                    configureMarkerEngine(settings);
                }
                
                return settings;
            
            } finally {
                SwingUtilities.invokeLater(new Runnable() { // use SwingUtilities to give the UI some time when result is computed too soon
                    public void run() { pHandle.finish(); }
                });
            }
        } else {
            return null;
        }
    }

    private void configureMarkerEngine(ProfilingSettings settings) {
        boolean isMarksEnabled = (settings.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE) || (settings.getProfilingType() == ProfilingSettings.PROFILE_CPU_PART);
        isMarksEnabled &= Categorization.isAvailable(project);

        if (isMarksEnabled) {
            Categorization ctg = new Categorization(project);
            ctg.reset();
            MarkingEngine.getDefault().configure(ctg.getMappings(), Lookup.getDefault().lookupAll(MarkingEngine.StateObserver.class));
        } else {
            MarkingEngine.getDefault().deconfigure();
        }
    }
    
    private void initClosedProjectHook() {
        OpenProjects.getDefault().addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (lastAttachProject == null) {
                        return;
                    }

                    if (OpenProjects.PROPERTY_OPEN_PROJECTS.equals(evt.getPropertyName())) {
                        Project[] openedProjects = ProjectUtilities.getOpenedProjects();

                        for (Project openedProject : openedProjects) {
                            if (lastAttachProject == openedProject) {
                                return;
                            }
                        }

                        // lastAttachProject points to a closed project
                        lastAttachProject = null; // NOTE: projectsChooserCombo should not be opened, no need to remove the project
                    }
                }
            });
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        // projectsChooserLabel
        projectsChooserLabel = new JLabel(ATTACH_LABEL_TEXT);
        projectsChooserLabel.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 0));
        projectsChooserLabel.setOpaque(false);

        // projectsChoserCombo
        projectsChooserCombo = new JComboBox();
        projectsChooserCombo.setRenderer(org.netbeans.modules.profiler.ppoints.Utils.getProjectListRenderer());
        projectsChooserLabel.setLabelFor(projectsChooserCombo);
        projectsChooserCombo.getAccessibleContext().setAccessibleDescription(CHOOSER_COMBO_ACCESS_DESCR);

        // projectsChooserComboContainer
        projectsChooserComboContainer = new JPanel(new BorderLayout());
        projectsChooserComboContainer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        projectsChooserComboContainer.setOpaque(false);
        projectsChooserComboContainer.add(projectsChooserCombo, BorderLayout.CENTER);

        // projectsChooserSeparator
        if (!UIUtils.isNimbus()) projectsChooserSeparator = Utils.createHorizontalSeparator();

        // projectsChooserPanel
        projectsChooserPanel = new JPanel(new BorderLayout());
        projectsChooserPanel.add(projectsChooserLabel, BorderLayout.WEST);
        projectsChooserPanel.add(projectsChooserComboContainer, BorderLayout.CENTER);
        if (projectsChooserSeparator != null) projectsChooserPanel.add(projectsChooserSeparator, BorderLayout.SOUTH);

        // taskChooser
        taskChooser = new TaskChooser();
        taskChooser.addItemListener(this);

        // taskChooserPanel
        taskChooserPanel = BACKGROUND_IMAGE != null ? new ImagePanel(BACKGROUND_IMAGE, SwingConstants.BOTTOM) : new JPanel(null);
        taskChooserPanel.setLayout(new BorderLayout());
        taskChooserPanel.add(taskChooser, BorderLayout.NORTH);

        // settingsContainerPanel
        settingsContainerPanel = new SettingsContainerPanel();

        // contentsPanel
        contentsPanel = new JPanel(new BorderLayout());

        // customSettingsPanelSeparator
        customSettingsPanelSeparator = Utils.createHorizontalSeparator();

        // extraSettingsPanel
        customSettingsPanelContainer = new JPanel(new BorderLayout());
        //    customSettingsPanelContainer.add(customSettingsPanelSeparator, BorderLayout.SOUTH);

        // attachSettingsPanel
        attachSettingsPanel = new AttachSettingsPanel();

        // attachSettingsPanelSeparator
        attachSettingsPanelSeparator = Utils.createHorizontalSeparator();

        // attachSetingsPanelContainer
        attachSettingsPanelContainer = new JPanel(new BorderLayout());
        attachSettingsPanelContainer.add(attachSettingsPanel, BorderLayout.CENTER);
        attachSettingsPanelContainer.add(attachSettingsPanelSeparator, BorderLayout.SOUTH);

        // extraSettingsPanelSeparator
        extraSettingsPanelSeparator = Utils.createHorizontalSeparator();

        // extraSettingsPanel
        extraSettingsPanel = new JPanel(new BorderLayout());
        extraSettingsPanel.add(extraSettingsPanelSeparator, BorderLayout.NORTH);
        extraSettingsPanel.add(customSettingsPanelContainer, BorderLayout.CENTER);
        extraSettingsPanel.add(attachSettingsPanelContainer, BorderLayout.SOUTH);

        // runButton
        runButton = UIUtils.isNimbus() ? new JButton(RUN_BUTTON_TEXT) :
                                         new JButton(RUN_BUTTON_TEXT, RUN_ICON);

        // attachButton
        attachButton = UIUtils.isNimbus() ? new JButton(ATTACH_BUTTON_TEXT) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, runButton.getPreferredSize().height);
                }
            } : new JButton(ATTACH_BUTTON_TEXT, ATTACH_ICON) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, runButton.getPreferredSize().height);
                }
            };

        // modifyButton
        modifyButton = new JButton(OK_BUTTON_TEXT) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, runButton.getPreferredSize().height);
                }
            };

        // cancelButton
        cancelButton = new JButton(CANCEL_BUTTON_TEXT) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, runButton.getPreferredSize().height);
                }
            };

        // this
        setLayout(new BorderLayout());
        add(projectsChooserPanel, BorderLayout.NORTH);
        add(taskChooserPanel, BorderLayout.WEST);
        add(contentsPanel, BorderLayout.CENTER);
        add(extraSettingsPanel, BorderLayout.SOUTH);

        // UI tweaks
        projectsChooserPanel.setOpaque(true);
        projectsChooserPanel.setBackground(taskChooserPanel.getBackground());

        // Listeners
        projectsChooserCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (internalComboChange) {
                        return;
                    }

                    Object comboSelection = projectsChooserCombo.getSelectedItem();

                    // Store settings of last project
                    if (lastAttachProject != null) {
                        storeSettings((lastAttachProject == EXTERNAL_APPLICATION_STRING) ? null : (Project) lastAttachProject);
                    }

                    if ((comboSelection == null) || (comboSelection == SELECT_PROJECT_TO_ATTACH_STRING)) {
                        return;
                    }

                    if ((comboSelection != SELECT_PROJECT_TO_ATTACH_STRING)
                            && (projectsChooserCombo.getItemAt(0) == SELECT_PROJECT_TO_ATTACH_STRING)) {
                        projectsChooserCombo.removeItemAt(0);
                    }

                    if (comboSelection == EXTERNAL_APPLICATION_STRING) {
                        updateProject(null);
                        lastAttachProject = EXTERNAL_APPLICATION_STRING;
                    } else if (comboSelection instanceof Project) {
                        updateProject((Project) comboSelection);
                        lastAttachProject = comboSelection;
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                projectsChooserCombo.requestFocusInWindow();
                            } // Moves focus from selected settings back to projectsChooserCombo
                        });
                }
            });
    }

    private void initPreferredSize() {
        // TODO: should be called whenever dialog is displayed, should respect last size
        Dimension preferredContentsSize = new Dimension(360, 215);

        Dimension monitorSettingsSize = DefaultSettingsConfigurator.SHARED_INSTANCE.getMonitorConfigurator()
                                                                                   .getBasicSettingsPanel().getPreferredSize();
        Dimension cpuSettingsSize = DefaultSettingsConfigurator.SHARED_INSTANCE.getCPUConfigurator().getBasicSettingsPanel()
                                                                               .getPreferredSize();
        Dimension memorySettingsSize = DefaultSettingsConfigurator.SHARED_INSTANCE.getMemoryConfigurator().getBasicSettingsPanel()
                                                                                  .getPreferredSize();

        preferredContentsSize.setSize(Math.max(preferredContentsSize.width, monitorSettingsSize.width),
                                      Math.max(preferredContentsSize.height, monitorSettingsSize.height));
        preferredContentsSize.setSize(Math.max(preferredContentsSize.width, cpuSettingsSize.width),
                                      Math.max(preferredContentsSize.height, cpuSettingsSize.height));
        preferredContentsSize.setSize(Math.max(preferredContentsSize.width, memorySettingsSize.width),
                                      Math.max(preferredContentsSize.height, memorySettingsSize.height));

        settingsContainerPanel.setPreferredContentsSize(preferredContentsSize);

        Dimension finalSize = settingsContainerPanel.getPreferredSize();

        contentsPanel.setPreferredSize(finalSize);
    }

    private void initTaskChooserSize() {
        Dimension preferredTaskChooserSize = new Dimension(200, taskChooserPanel.getPreferredSize().height);
        int vgap = ((VerticalLayout) taskChooser.getLayout()).getVGap() * 2;
        int insets = XPStyleBorder.getBorderInsets().left + XPStyleBorder.getBorderInsets().right;

        Dimension monitorSettingsSize = taskMonitor.getSmallComponent().getPreferredSize();
        Dimension cpuSettingsSize = taskCPU.getSmallComponent().getPreferredSize();
        Dimension memorySettingsSize = taskMemory.getSmallComponent().getPreferredSize();

        preferredTaskChooserSize.setSize(Math.max(preferredTaskChooserSize.width, monitorSettingsSize.width + vgap + insets),
                                         preferredTaskChooserSize.height);
        preferredTaskChooserSize.setSize(Math.max(preferredTaskChooserSize.width, cpuSettingsSize.width + vgap + insets),
                                         preferredTaskChooserSize.height);
        preferredTaskChooserSize.setSize(Math.max(preferredTaskChooserSize.width, memorySettingsSize.width + vgap + insets),
                                         preferredTaskChooserSize.height);

        taskChooserPanel.setPreferredSize(preferredTaskChooserSize);
    }

    private void initTasks() {
        TaskPresenter.Context context = new TaskPresenter.Context() {
            public void selectSettings(ProfilingSettings settings) {
                selectProfilingSettings(settings);
            }

            public void refreshLayout() {
                taskChooser.refreshLayout();
            }
        };

        taskMonitor = new TaskPresenter(MONITOR_STRING, MONITOR_ICON, context);
        taskCPU = new TaskPresenter(CPU_STRING, CPU_ICON, context);
        taskMemory = new TaskPresenter(MEMORY_STRING, MEMORY_ICON, context);

        taskChooser.add(taskMonitor);
        taskChooser.add(taskCPU);
        taskChooser.add(taskMemory);
    }

    private void projectCleanup() {
        taskChooser.reset(); // prevents layout problems when configurations differ between invocations

        taskMonitor.resetProfilingSettings();
        //taskAnalyzer.resetProfilingSettings();
        taskCPU.resetProfilingSettings();
        taskMemory.resetProfilingSettings();

        attachSettingsPanel.resetSettings();

        selectedTask = null;

        if (configurator != null) {
            configurator.reset();
        }

        configurator = null;
    }

    private void selectProfilingSettings(ProfilingSettings profilingSettings) {
        // Persist changes in current settigs
        synchronizeCurrentSettings();

        // Determine which task contains the settings
        TaskPresenter newSelectedTask = getTaskPresenter(profilingSettings);

        // Change settingsContainerPanel if needed
        if ((newSelectedTask == null) || (newSelectedTask != selectedTask)) {
            JPanel newContentsPanel = null;

            if (newSelectedTask == null) {
                WelcomePanel welcomePanel = getWelcomePanel();
                // TODO: set profile/attach/modify? mode
                newContentsPanel = welcomePanel;
            } else {
                if (newSelectedTask == taskMonitor) {
                    settingsContainerPanel.setContents(configurator.getMonitorConfigurator());
                } else if (newSelectedTask == taskCPU) {
                    settingsContainerPanel.setContents(configurator.getCPUConfigurator());
                } else if (newSelectedTask == taskMemory) {
                    settingsContainerPanel.setContents(configurator.getMemoryConfigurator());
                }

                newContentsPanel = settingsContainerPanel;
            }

            contentsPanel.removeAll();
            contentsPanel.add(newContentsPanel, BorderLayout.CENTER);
            contentsPanel.doLayout();
            contentsPanel.repaint();

            selectedTask = newSelectedTask;

            //      updateHelpCtx();      
        }

        // Select profilingSettings
        if (profilingSettings != null) {
            settingsContainerPanel.setShowingPreset(profilingSettings.isPreset());
            settingsContainerPanel.switchToBasicSettings(); // NOTE: this might be confusing when comparing advanced settings within a task
            settingsContainerPanel.setCaption(profilingSettings.getSettingsName());
            selectedTask.selectProfilingSettings(profilingSettings);
            configurator.setSettings(profilingSettings);
        }
    }

    private void setupAttachProfiler(Project project) {
        if ((project == null) && lastAttachProject instanceof Project) {
            project = (Project) lastAttachProject;
        }

        this.profiledFile = null;
        this.enableOverride = false;
        this.isAttach = true;
        this.isModify = false;

        projectsChooserLabel.setEnabled(true);
        projectsChooserCombo.setEnabled(true);
        projectsChooserPanel.setVisible(true);
        attachSettingsPanel.setEnabled(true);
        attachSettingsPanelContainer.setVisible(true);

        if (lastAttachProject == null) {
            lastAttachProject = EXTERNAL_APPLICATION_STRING; // Preselect external application by default
        }

        updateProjectsCombo((project != null) ? project : lastAttachProject);
        updateProject(project);
    }

    private void setupModifyProfiling(Project project, FileObject profiledFile, boolean isAttach) {
        this.profiledFile = profiledFile;
        this.enableOverride = false;
        this.isAttach = isAttach;
        this.isModify = true;

        projectsChooserLabel.setEnabled(false);
        projectsChooserCombo.setEnabled(false);
        projectsChooserPanel.setVisible(isAttach);
        attachSettingsPanel.setEnabled(false);
        attachSettingsPanelContainer.setVisible(isAttach);

        if (isAttach) {
            updateProjectsCombo((project != null) ? project : EXTERNAL_APPLICATION_STRING);
        }

        updateProject(project);
    }

    private void setupProfileProject(Project project, FileObject profiledFile, boolean enableOverride) {
        this.profiledFile = profiledFile;
        this.enableOverride = enableOverride;
        this.isAttach = false;
        this.isModify = false;

        projectsChooserPanel.setVisible(false);
        attachSettingsPanelContainer.setVisible(false);

        updateProject(project);
    }

    private void storeCurrentSettings() {
        storeSettings(project);
    }

    private void storeSettings(final Project targetProject) {
        synchronizeCurrentSettings();

        final ArrayList<ProfilingSettings> profilingSettings = new ArrayList();

        profilingSettings.addAll(taskMonitor.getProfilingSettings());
        //profilingSettings.addAll(taskAnalyzer.createFinalSettings());
        profilingSettings.addAll(taskCPU.getProfilingSettings());
        profilingSettings.addAll(taskMemory.getProfilingSettings());

        final ProfilingSettings selectedProfilingSettings = (selectedTask == null) ? null
                                                                                   : ((TaskPresenter) selectedTask)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            .getSelectedProfilingSettings();

        IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    ProfilingSettingsManager.getDefault()
                                            .storeProfilingSettings(profilingSettings.toArray(new ProfilingSettings[profilingSettings
                                                                                                                    .size()]),
                                                                    selectedProfilingSettings, targetProject);
                }
            });
    }

    private void updateProject(final Project project) {
        Runnable projectUpdater = new Runnable() {
            public void run() {
                projectCleanup();

                SelectProfilingTask.this.project = project;

                if (project != null) {
                    projectPackages = new String[2][];
                    predefinedInstrFilterKeys = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project)
                                                                .getPredefinedInstrumentationFilters(project);
                    predefinedInstrFilters = new SimpleFilter[predefinedInstrFilterKeys.size()];
                } else {
                    projectPackages = null;
                    predefinedInstrFilters = null;
                    predefinedInstrFilterKeys = null;
                }

                if (projectsChooserPanel.isVisible() && (projectsChooserCombo.getSelectedItem() == SELECT_PROJECT_TO_ATTACH_STRING)) {
                    // Attach, no project selected
                    taskChooser.setEnabled(false);

                    // TODO: cleanup
                    contentsPanel.removeAll();
                    contentsPanel.add(getWelcomePanel(), BorderLayout.CENTER);
                    contentsPanel.doLayout();
                    contentsPanel.repaint();
                } else {
                    configurator = Utils.getSettingsConfigurator(project);
                    configurator.setContext(project, profiledFile, isAttach, isModify, enableOverride);

                    JPanel customSettings = configurator.getCustomSettingsPanel();

                    if (customSettings != null) {
                        customSettingsPanelContainer.removeAll();
                        customSettingsPanelContainer.add(customSettings, BorderLayout.NORTH);
                        customSettingsPanelContainer.add(customSettingsPanelSeparator, BorderLayout.SOUTH);
                        customSettingsPanelContainer.setVisible(true);
                    } else {
                        customSettingsPanelContainer.removeAll();
                        customSettingsPanelContainer.setVisible(false);
                    }

                    // Project selected
                    taskChooser.setEnabled(true);

                    ProfilingSettings[] profilingSettings = new ProfilingSettings[0];
                    ProfilingSettings lastSelectedSettings = null;

                    ProfilingSettingsManager.ProfilingSettingsDescriptor profilingSettingsDescriptor = ProfilingSettingsManager.getDefault()
                                                                                                                               .getProfilingSettings(project);
                    profilingSettings = profilingSettingsDescriptor.getProfilingSettings();
                    lastSelectedSettings = profilingSettingsDescriptor.getLastSelectedProfilingSettings();

                    ArrayList<ProfilingSettings> monitorSettings = new ArrayList();

                    //ArrayList<ProfilingSettings> analyzerSettings = new ArrayList();
                    ArrayList<ProfilingSettings> cpuSettings = new ArrayList();
                    ArrayList<ProfilingSettings> memorySettings = new ArrayList();

                    for (ProfilingSettings settings : profilingSettings) {
                        if (Utils.isMonitorSettings(settings)) {
                            monitorSettings.add(settings);
                        }
                        //else if (Utils.isAnalyzerSettings(settings)) analyzerSettings.add(settings);
                        else if (Utils.isCPUSettings(settings)) {
                            cpuSettings.add(settings);
                        } else if (Utils.isMemorySettings(settings)) {
                            memorySettings.add(settings);
                        }
                    }

                    taskMonitor.setProfilingSettings(monitorSettings);
                    //taskAnalyzer.setProfilingSettings(analyzerSettings);
                    taskCPU.setProfilingSettings(cpuSettings);
                    taskMemory.setProfilingSettings(memorySettings);

                    // TODO: keep/change lastSelectedSettings to null if Welcome Screen is about to be displayed
                    if (lastSelectedSettings == null) {
                        // NOTE: If no lastSelectedSettings then CPU preset will be selected by default
                        //       Monitor preset would be more correct but this one looks better
                        for (ProfilingSettings cpuSettingsPreset : cpuSettings) {
                            if (cpuSettingsPreset.isPreset()) {
                                lastSelectedSettings = cpuSettingsPreset;
                            }
                        }
                    }

                    // Expand appropriate task for lastSelectedSettings
                    if (lastSelectedSettings != null) {
                        TaskPresenter taskPresenter = getTaskPresenter(lastSelectedSettings);

                        if (taskPresenter != null) {
                            taskChooser.expandImmediately(taskPresenter);
                        }
                    }

                    selectProfilingSettings(lastSelectedSettings);
                }

                if (attachSettingsPanelContainer.isVisible()) {
                    attachSettingsPanel.setSettings(project, projectsChooserCombo.getSelectedItem() != SELECT_PROJECT_TO_ATTACH_STRING);
                }
            }
        };
        IDEUtils.runInEventDispatchThread(projectUpdater);
    }

    private void updateProjectsCombo(Object projectToSelect) { // Actually may be also EXTERNAL_APPLICATION_STRING
        internalComboChange = true;

        Project[] projects = ProjectUtilities.getSortedProjects(getOpenedProjectsForAttach());

        if (projectToSelect == null) {
            projectsChooserCombo.addItem(SELECT_PROJECT_TO_ATTACH_STRING);
        }

        projectsChooserCombo.addItem(EXTERNAL_APPLICATION_STRING);

        for (Project project : projects) {
            projectsChooserCombo.addItem(project);
        }

        if (projectToSelect == null) {
            projectsChooserCombo.setSelectedIndex(0);
        } else {
            projectsChooserCombo.setSelectedItem(projectToSelect);
        }

        internalComboChange = false;
    }
    
    private static Project[] getOpenedProjectsForAttach() {
        Project[] projects = ProjectUtilities.getOpenedProjects();
        ArrayList<Project> projectsArray = new ArrayList(projects.length);

        for (int i = 0; i < projects.length; i++) {
            if (isProjectTypeSupportedForAttach(projects[i])) {
                projectsArray.add(projects[i]);
            }
        }

        return projectsArray.toArray(new Project[projectsArray.size()]);
    }

    private static boolean isProjectTypeSupported(final Project project) {
        ProjectTypeProfiler ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);

        if (ptp.isProfilingSupported(project)) {
            return true;
        }

        return ProjectUtilities.hasAction(project, "profile"); //NOI18N
    }
    
    private static boolean isProjectTypeSupportedForAttach(Project project) {
        ProjectTypeProfiler ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);
        return ptp.isAttachSupported(project);
    }
}

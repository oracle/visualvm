/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.v2.features;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.SmallButton;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.profiler.ResultsListener;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.api.ProjectUtilities;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.api.java.SourceClassInfo;
import org.graalvm.visualvm.lib.profiler.api.java.SourceMethodInfo;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerFeature;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerSession;
import org.graalvm.visualvm.lib.profiler.v2.impl.WeakProcessor;
import org.graalvm.visualvm.lib.profiler.v2.ui.SettingsPanel;
import org.graalvm.visualvm.lib.profiler.v2.ui.TitledMenuSeparator;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MethodsFeature_name=Methods",
    "MethodsFeature_description=Profile method execution times and invocation counts, including call trees",
    "MethodsFeature_modeReset=<html><b>Current mode ''{0}'' is not configured properly.</b><br><br>Default ''{1}'' mode has been selected and applied instead.<br><br></html>"
})
final class MethodsFeature extends ProfilerFeature.Basic {
    
    private final WeakProcessor processor;
    
    private FeatureMode currentMode;
    private FeatureMode appliedMode;
    
    private MethodsFeatureModes.AllClassesMode allClassesMode;
    private MethodsFeatureModes.ProjectClassesMode projectClassesMode;
    private MethodsFeatureModes.SelectedClassesMode selectedClassesMode;
    private MethodsFeatureModes.SelectedMethodsMode selectedMethodsMode;
    private MethodsFeatureModes.CustomClassesMode definedClassesMode;
    
    
    private MethodsFeature(ProfilerSession session) {
        super(Icons.getIcon(ProfilerIcons.CPU), Bundle.MethodsFeature_name(),
              Bundle.MethodsFeature_description(), 12, session);
        
        assert !SwingUtilities.isEventDispatchThread();
        
        Lookup.Provider project = session.getProject();
        String projectName = project == null ? "External Process" : // NOI18N
                             ProjectUtilities.getDisplayName(project);
        processor = new WeakProcessor("MethodsFeature Processor for " + projectName); // NOI18N

        initModes();
    }
    
    
    // --- Configuration -------------------------------------------------------
    
    public boolean supportsConfiguration(Lookup configuration) {
        return configuration.lookup(SourceMethodInfo.class) != null ||
               configuration.lookup(SourceClassInfo.class) != null ||
               configuration.lookup(ClientUtils.SourceCodeSelection.class) != null;
    }
    
    public void configure(Lookup configuration) {
        // Handle Profile Method action from editor
        SourceMethodInfo methodInfo = configuration.lookup(SourceMethodInfo.class);
        if (methodInfo != null) selectMethodForProfiling(methodInfo);
        
        // Handle Profile Class action from editor
        SourceClassInfo classInfo = configuration.lookup(SourceClassInfo.class);
        if (classInfo != null) selectClassForProfiling(classInfo);
        
        // Handle Profile Class/Method action from snapshot
        ClientUtils.SourceCodeSelection sel = configuration.lookup(ClientUtils.SourceCodeSelection.class);
        if (sel != null) selectForProfiling(sel);
    }
    
    
    private void selectMethodForProfiling(SourceMethodInfo methodInfo) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(methodInfo.getClassName(),
                           methodInfo.getName(), methodInfo.getSignature()));
    }
    
    private void selectClassForProfiling(SourceClassInfo classInfo) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(classInfo.getQualifiedName(),
                           Wildcards.ALLWILDCARD, null));
    }
    
    private void selectForProfiling(ClientUtils.SourceCodeSelection sel) {
        if (Wildcards.ALLWILDCARD.equals(sel.getMethodName())) {
            selectedClassesMode.getSelection().add(sel);
        } else {
            selectedMethodsMode.getSelection().add(sel);
        }
    }
    
    
    // --- Mode ----------------------------------------------------------------
    
    private static final String MODE_FLAG = "MODE_FLAG"; // NOI18N
    
    private void initModes() {
        allClassesMode = new MethodsFeatureModes.AllClassesMode() {
            String readFlag(String flag, String defaultValue) {
                return MethodsFeature.this.readFlag(getID() + "_" + flag, defaultValue); // NOI18N
            }
            void storeFlag(String flag, String value) {
                MethodsFeature.this.storeFlag(getID() + "_" + flag, value); // NOI18N
            }
            void settingsChanged() {
                MethodsFeature.this.settingsChanged();
            }
        };
        allClassesMode.initialize();
        
        if (getSession().getProject() != null) projectClassesMode = new MethodsFeatureModes.ProjectClassesMode() {
            String readFlag(String flag, String defaultValue) {
                return MethodsFeature.this.readFlag(getID() + "_" + flag, defaultValue); // NOI18N
            }
            void storeFlag(String flag, String value) {
                MethodsFeature.this.storeFlag(getID() + "_" + flag, value); // NOI18N
            }
            void settingsChanged() {
                MethodsFeature.this.settingsChanged();
            }
            Lookup.Provider getProject() {
                return MethodsFeature.this.getSession().getProject();
            }
        };
        if (projectClassesMode != null) projectClassesMode.initialize();
        
        selectedClassesMode = new MethodsFeatureModes.SelectedClassesMode() {
            String readFlag(String flag, String defaultValue) {
                return MethodsFeature.this.readFlag(getID() + "_" + flag, defaultValue); // NOI18N
            }
            void storeFlag(String flag, String value) {
                MethodsFeature.this.storeFlag(getID() + "_" + flag, value); // NOI18N
            }
            ProfilerSession getSession() {
                return MethodsFeature.this.getSession();
            }
            void selectForProfiling(Collection<SourceClassInfo> classInfos) {
                for (SourceClassInfo classInfo : classInfos)
                    MethodsFeature.this.selectClassForProfiling(classInfo);
            }
            void settingsChanged() {
                MethodsFeature.this.settingsChanged();
            }
            void selectionChanging() {
                MethodsFeature.this.setMode(this);
                MethodsFeature.this.getSettingsUI().setVisible(true);
            }
            void selectionChanged() {
                MethodsFeature.this.selectionChanged();
            }
        };
        selectedClassesMode.initialize();
        
        selectedMethodsMode = new MethodsFeatureModes.SelectedMethodsMode() {
            String readFlag(String flag, String defaultValue) {
                return MethodsFeature.this.readFlag(getID() + "_" + flag, defaultValue); // NOI18N
            }
            void storeFlag(String flag, String value) {
                MethodsFeature.this.storeFlag(getID() + "_" + flag, value); // NOI18N
            }
            ProfilerSession getSession() {
                return MethodsFeature.this.getSession();
            }
            void selectForProfiling(Collection<SourceMethodInfo> methodInfos) {
                for (SourceMethodInfo methodInfo : methodInfos)
                    MethodsFeature.this.selectMethodForProfiling(methodInfo);
            }
            void settingsChanged() {
                MethodsFeature.this.settingsChanged();
            }
            void selectionChanging() {
                MethodsFeature.this.setMode(this);
                MethodsFeature.this.getSettingsUI().setVisible(true);
            }
            void selectionChanged() {
                MethodsFeature.this.selectionChanged();
                if (MethodsFeature.this.ui != null && MethodsFeature.this.ui.hasResultsUI())
                    MethodsFeature.this.ui.getResultsUI().repaint();
            }
        };
        selectedMethodsMode.initialize();
        
        if (ProfilerIDESettings.getInstance().getEnableExpertSettings()) {
            definedClassesMode = new MethodsFeatureModes.CustomClassesMode() {
                String readFlag(String flag, String defaultValue) {
                    return MethodsFeature.this.readFlag(getID() + "_" + flag, defaultValue); // NOI18N
                }
                void storeFlag(String flag, String value) {
                    MethodsFeature.this.storeFlag(getID() + "_" + flag, value); // NOI18N
                }
                void settingsChanged() {
                    MethodsFeature.this.settingsChanged();
                }
            };
        }
        if (definedClassesMode != null) definedClassesMode.initialize();
        
//        currentMode = allClassesMode;
        String _currentMode = readFlag(MODE_FLAG, allClassesMode.getID());
        if (projectClassesMode != null && _currentMode.equals(projectClassesMode.getID())) currentMode = projectClassesMode;
        else if (_currentMode.equals(selectedClassesMode.getID())) currentMode = selectedClassesMode;
        else if (_currentMode.equals(selectedMethodsMode.getID())) currentMode = selectedMethodsMode;
        else if (definedClassesMode != null && _currentMode.equals(definedClassesMode.getID())) currentMode = definedClassesMode;
        else currentMode = allClassesMode;
        
        appliedMode = currentMode;
    }
    
    private void saveMode() {
        storeFlag(MODE_FLAG, currentMode.getID());
    }
    
    private void setMode(FeatureMode newMode) {
        if (currentMode == newMode) return;
        currentMode = newMode;
        modeChanged();
    }
    
    private void confirmMode() {
        appliedMode = currentMode;
    }
    
    private void modeChanged() {
        updateModeName();
        updateModeUI();
        configurationChanged();
        saveMode();
    }
    
    
    // --- Settings ------------------------------------------------------------
    
    public boolean supportsSettings(ProfilingSettings psettings) {
        return !ProfilingSettings.isMemorySettings(psettings) &&
               !ProfilingSettings.isJDBCSettings(psettings);
    }

    public void configureSettings(ProfilingSettings psettings) {
        currentMode.configureSettings(psettings);
    }
    
    public boolean currentSettingsValid() {
        return currentMode.currentSettingsValid();
    }
    
    private void submitChanges() {
        confirmMode();
        confirmSettings();
        fireChange();
    }
    
    // Changes to current settings are pending
    private boolean pendingChanges() {
        if (appliedMode != currentMode) return true;
        return currentMode.pendingChanges();
    }
    
    // Profiling settings defined by this feature have changed
    private void configurationChanged() {
        assert isActivated();
        
        ProfilerSession session = getSession();
        
        if (!session.inProgress()) submitChanges();
        else updateApplyButton(session.getState());
    }
    
    private void confirmSettings() {
        currentMode.confirmSettings();
    }
    
    private void confirmAllSettings() {
        if (allClassesMode != null) allClassesMode.confirmSettings();
        if (projectClassesMode != null) projectClassesMode.confirmSettings();
        if (selectedClassesMode != null) selectedClassesMode.confirmSettings();
        if (selectedMethodsMode != null) selectedMethodsMode.confirmSettings();
        if (definedClassesMode != null) definedClassesMode.confirmSettings();
    }
    
    private void settingsChanged() {
        configurationChanged();
    }
    
    private void selectionChanged() {
        configurationChanged();
    }
    
    
    // --- Settings UI ---------------------------------------------------------
    
    private static final String SETTINGS_FLAG = "SETTINGS_FLAG"; // NOI18N
    
    private JPanel settingsUI;
    private JButton modeButton;
    private JPanel settingsContainer;
    private JButton applyButton;
    
    public JPanel getSettingsUI() {
        if (settingsUI == null) {
            settingsUI = new JPanel(new GridBagLayout()) {
                public void setVisible(boolean visible) {
                    if (visible && getComponentCount() == 0) populateSettingsUI();
                    super.setVisible(visible);
                    storeFlag(SETTINGS_FLAG, visible ? Boolean.TRUE.toString() : null);
                }
                public Dimension getPreferredSize() {
                    if (getComponentCount() == 0) return new Dimension();
                    else return super.getPreferredSize();
                }
            };
            
            String _vis = readFlag(SETTINGS_FLAG, null);
            boolean vis = _vis == null ? false : Boolean.parseBoolean(_vis);
            settingsUI.setVisible(vis || currentMode != allClassesMode);
//            settingsUI.setVisible(false);
        }
        return settingsUI;
    }
    
    private void populateSettingsUI() {
        settingsUI.setOpaque(false);
        settingsUI.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints c;
        
        JPanel profilePanel = new SettingsPanel();
        profilePanel.add(new JLabel(Bundle.ObjectsFeature_profileMode()));
        profilePanel.add(Box.createHorizontalStrut(5));
        
        // Mode button
        modeButton = new PopupButton(currentMode.getName()) {
            protected void populatePopup(JPopupMenu popup) {
                popup.add(new TitledMenuSeparator(Bundle.ObjectsFeature_samplingModes()));
                popup.add(new JRadioButtonMenuItem(allClassesMode.getName(), currentMode == allClassesMode) {
                    protected void fireActionPerformed(ActionEvent e) { setMode(allClassesMode); }
                });
                if (projectClassesMode != null) popup.add(new JRadioButtonMenuItem(projectClassesMode.getName(), currentMode == projectClassesMode) {
                    protected void fireActionPerformed(ActionEvent e) { setMode(projectClassesMode); }
                });

                popup.add(new TitledMenuSeparator(Bundle.ObjectsFeature_instrModes()));
                popup.add(new JRadioButtonMenuItem(selectedClassesMode.getName(), currentMode == selectedClassesMode) {
                    protected void fireActionPerformed(ActionEvent e) { setMode(selectedClassesMode); }
                });
                popup.add(new JRadioButtonMenuItem(selectedMethodsMode.getName(), currentMode == selectedMethodsMode) {
                    protected void fireActionPerformed(ActionEvent e) { setMode(selectedMethodsMode); }
                });
                
                if (definedClassesMode != null) popup.add(new JRadioButtonMenuItem(definedClassesMode.getName(), currentMode == definedClassesMode) {
                    protected void fireActionPerformed(ActionEvent e) { setMode(definedClassesMode); }
                });
            }
        };
        profilePanel.add(modeButton);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.NORTHWEST;
        settingsUI.add(profilePanel, c);
        
        // Settings container
        settingsContainer = new JPanel(new BorderLayout());
        settingsContainer.setOpaque(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(0, 10, 0, 0);
        c.anchor = GridBagConstraints.NORTHWEST;
        settingsUI.add(settingsContainer, c);
        
        JPanel buttonsPanel = new SettingsPanel();
        
        final Component space = Box.createHorizontalStrut(10);
        buttonsPanel.add(space);
        
        // Apply button
        applyButton = new SmallButton(Bundle.ObjectsFeature_applyButton()) {
            protected void fireActionPerformed(ActionEvent e) {
                stopResults();
                resetResults();
                submitChanges();
                unpauseResults();
            }
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                space.setVisible(visible);
            }
        };
        buttonsPanel.add(applyButton);
        
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.NORTHEAST;
        settingsUI.add(buttonsPanel, c);
        
        updateModeUI();
        updateApplyButton(getSession().getState());
    }
    
    private void updateModeName() {
        if (modeButton != null) modeButton.setText(currentMode.getName());
    }
    
    private void updateModeUI() {
        if (settingsContainer != null) {
            settingsContainer.removeAll();

            JComponent modeUI = currentMode.getUI();
            if (modeUI != null) settingsContainer.add(modeUI);
            settingsContainer.doLayout();
            settingsContainer.repaint();
        }
    }
    
    private void updateApplyButton(int state) {
        if (applyButton != null) {
            boolean visible = state != Profiler.PROFILING_INACTIVE;
            applyButton.setVisible(visible);
            if (visible) applyButton.setEnabled(currentSettingsValid() && pendingChanges());
        }
    }
    
    
    // --- Toolbar & Results UI ------------------------------------------------
    
    private MethodsFeatureUI ui;
    
    public JPanel getResultsUI() {
        return getUI().getResultsUI();
    }
    
    public ProfilerToolbar getToolbar() {
        return getUI().getToolbar();
    }
    
    private MethodsFeatureUI getUI() {
        if (ui == null) ui = new MethodsFeatureUI() {
            Set<ClientUtils.SourceCodeSelection> getClassesSelection() {
                return selectedClassesMode.getSelection();
            }
            Set<ClientUtils.SourceCodeSelection> getMethodsSelection() {
                return selectedMethodsMode.getSelection();
            }
            void selectForProfiling(ClientUtils.SourceCodeSelection value) {
                MethodsFeature.this.selectForProfiling(value);
            }
            Lookup.Provider getProject() {
                return MethodsFeature.this.getSession().getProject();
            }
            ProfilerClient getProfilerClient() {
                Profiler profiler = MethodsFeature.this.getSession().getProfiler();
                return profiler.getTargetAppRunner().getProfilerClient();
            }
            int getSessionState() {
                return MethodsFeature.this.getSessionState();
            }
            void refreshResults() {
                MethodsFeature.this.refreshResults();
            }
        };
        return ui;
    }
    
    
    // --- Live results --------------------------------------------------------
    
    private Runnable refresher;
    private volatile boolean running;
    
    
    private void startResults() {
        if (running) return;
        running = true;
        
        refresher = new Runnable() {
            public void run() {
                if (running) {
                    refreshView();
                    refreshResults(1500);
                }
            }
        };
        
        refreshResults(1000);
    }

    private void refreshView() {
        if (ui != null && ResultsManager.getDefault().resultsAvailable()
            || getSession().getProfilingSettings().getCPUProfilingType() == CommonConstants.CPU_SAMPLED) {
            try {
                ui.refreshData();
            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                stopResults();
            }
        }
    }
    
    private void refreshResults() {
        if (running) processor.post(new Runnable() {
            public void run() {
                if (ui != null) ui.setForceRefresh();
                refreshView();
            }
        });
    }
    
    private void refreshResults(int delay) {
        if (running && refresher != null) processor.post(refresher, delay);
    }
    
    private void resetResults() {
        if (ui != null) ui.resetData();
    }
    
    private void stopResults() {
        if (refresher != null) {
            running = false;
            refresher = null;
        }
    }
    
    private void unpauseResults() {
        if (ui != null) ui.resetPause();
    }
    
    
    // --- Session lifecycle ---------------------------------------------------
    
    private MethodsResetter resetter;
    
    public void notifyActivated() {
        resetResults();
        
        resetter = Lookup.getDefault().lookup(MethodsResetter.class);
        resetter.controller = this;
        
        if (getSession().inProgress() && !currentMode.currentSettingsValid()) {
            final String oldMode = currentMode.getName();
            final String newMode = allClassesMode.getName();
            setMode(allClassesMode);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ProfilerDialogs.displayInfo(Bundle.MethodsFeature_modeReset(
                                                oldMode, newMode));
                }
            });
        }
    }
    
    public void notifyDeactivated() {
        resetResults();
        
        if (resetter != null) {
            resetter.controller = null;
            resetter = null;
        }
        
        if (ui != null) {
            ui.cleanup();
            ui = null;
        }
        
        settingsUI = null;
    }
    
    
    protected void profilingStateChanged(int oldState, int newState) {
        if (newState == Profiler.PROFILING_INACTIVE || newState == Profiler.PROFILING_IN_TRANSITION) {
            stopResults();
            confirmAllSettings();
        } else if (isActivated() && newState == Profiler.PROFILING_RUNNING) {
            startResults();
        } else if (newState == Profiler.PROFILING_STARTED) {
            resetResults();
            unpauseResults();
        }
        
        if (ui != null) ui.sessionStateChanged(getSessionState());
        
        updateApplyButton(newState);
    }
    
    
    @ServiceProvider(service=ResultsListener.class)
    public static final class MethodsResetter implements ResultsListener {
        private MethodsFeature controller;
        public void resultsAvailable() { /*if (controller != null) controller.refreshView();*/ }
        public void resultsReset() { if (controller != null && controller.ui != null) controller.ui.resetData(); }
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    @ServiceProvider(service=ProfilerFeature.Provider.class)
    public static final class Provider extends ProfilerFeature.Provider {
        public ProfilerFeature getFeature(ProfilerSession session) {
            return new MethodsFeature(session);
        }
    }
    
}

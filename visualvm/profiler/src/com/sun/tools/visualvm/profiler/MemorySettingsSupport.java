/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class MemorySettingsSupport {
    
    private static final String PROP_PREFIX = "ProfilerMemorySettings_"; // NOI18N
    
    static final String SNAPSHOT_VERSION = PROP_PREFIX + "version"; // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1"; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0"; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    public static final String PROP_MODE = PROP_PREFIX + "mode"; // NOI18N
    public static final String PROP_STACKTRACES = PROP_PREFIX + "stacktraces"; // NOI18N
    public static final String PROP_RUNGC = PROP_PREFIX + "rungc"; // NOI18N
    
    private JPanel panel;
    private JRadioButton allocRadioButton;
    private JRadioButton livenessRadioButton;
    private JCheckBox stackTracesCheckBox;
    private JCheckBox runGCCheckBox;
    private JButton resetDefaultsButton;
    
    private Application application;
    
    
    public MemorySettingsSupport(Application application) {
        this.application = application;
    }
    
    public DataViewComponent.DetailsView getDetailsView() {
        return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Memory_settings"), null, 20, getPanel(), null); // NOI18N
    }
    
    public void setUIEnabled(boolean enabled) {
        if (panel == null) return;
        
        panel.setEnabled(enabled);
        allocRadioButton.setEnabled(enabled);
        livenessRadioButton.setEnabled(enabled);
        stackTracesCheckBox.setEnabled(enabled);
        runGCCheckBox.setEnabled(enabled);
        resetDefaultsButton.setEnabled(enabled);
    }
    
    public ProfilingSettings getSettings() {
        if (panel == null) return null;
        
        ProfilingSettings settings = allocRadioButton.isSelected() ?
            ProfilingSettingsPresets.createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS) :
            ProfilingSettingsPresets.createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_LIVENESS);
        settings.setAllocStackTraceLimit(stackTracesCheckBox.isSelected() ? -1 : 0);
        settings.setRunGCOnGetResultsInMemoryProfiling(runGCCheckBox.isSelected());
        
        return settings;
    }
    
    public void saveSettings() {
        if (application == null) return;
        Storage storage = application.getStorage();
        
        storage.setCustomProperty(SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);
        storage.setCustomProperty(PROP_MODE, Integer.toString(allocRadioButton.isSelected() ?
            ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS : ProfilingSettings.PROFILE_MEMORY_LIVENESS));
        storage.setCustomProperty(PROP_STACKTRACES, Integer.toString(stackTracesCheckBox.isSelected() ? -1 : 0));
        storage.setCustomProperty(PROP_RUNGC, Boolean.toString(runGCCheckBox.isSelected()));
    }
    
    
    private void loadSettings() {
        if (application == null) return;
        Storage storage = application.getStorage();
        
        String profilingMode = storage.getCustomProperty(PROP_MODE);
        if (profilingMode != null) try {
            int profilingModeInt = Integer.parseInt(profilingMode);
            if (profilingModeInt == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS) allocRadioButton.setSelected(true);
            else if (profilingModeInt == ProfilingSettings.PROFILE_MEMORY_LIVENESS) livenessRadioButton.setSelected(true);
        } catch (Exception e) {}
        
        String stackTraces = storage.getCustomProperty(PROP_STACKTRACES);
        if (stackTraces != null) try {
            int stackTracesInt = Integer.parseInt(stackTraces);
            stackTracesCheckBox.setSelected(stackTracesInt != 0);
        } catch (Exception e) {}
        
        String runGC = storage.getCustomProperty(PROP_RUNGC);
        if (runGC != null) try {
            boolean runGCBool = Boolean.parseBoolean(runGC);
            runGCCheckBox.setSelected(runGCBool);
        } catch (Exception e) {}
    }
    
    private void setDefaults() {
        livenessRadioButton.setSelected(true);
        stackTracesCheckBox.setSelected(false);
        runGCCheckBox.setSelected(true);
    }
    
    private JPanel getPanel() {
        if (panel == null) {
            panel = createPanel();
            setDefaults();
            loadSettings();
        }
        return panel;
    }
    
    private JPanel createPanel() {
        JPanel panelImpl = new JPanel();
        panelImpl.setLayout(new GridBagLayout());
        panelImpl.setOpaque(false);
        
        JLabel referenceLabel = new JLabel("X"); // NOI18N
        
        ButtonGroup modesRadioGroup = new ButtonGroup();
        GridBagConstraints constraints;
        
        allocRadioButton = new JRadioButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profile_Allocations")); // NOI18N
        allocRadioButton.setOpaque(false);
        allocRadioButton.setBorder(referenceLabel.getBorder());
        modesRadioGroup.add(allocRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 10, 10);
        panelImpl.add(allocRadioButton, constraints);
        
        livenessRadioButton = new JRadioButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profile_AllocationsGC")); // NOI18N
        livenessRadioButton.setOpaque(false);
        livenessRadioButton.setBorder(referenceLabel.getBorder());
        modesRadioGroup.add(livenessRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 10, 10);
        panelImpl.add(livenessRadioButton, constraints);
        
        stackTracesCheckBox = new JCheckBox(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Record_Stacktraces")); // NOI18N
        stackTracesCheckBox.setOpaque(false);
        stackTracesCheckBox.setBorder(referenceLabel.getBorder());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 10, 10);
        panelImpl.add(stackTracesCheckBox, constraints);
        
        runGCCheckBox = new JCheckBox(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Run_GC")); // NOI18N
        runGCCheckBox.setOpaque(false);
        runGCCheckBox.setBorder(referenceLabel.getBorder());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 10, 10);
        panelImpl.add(runGCCheckBox, constraints);
        
        JPanel filler = new JPanel(null);
        filler.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        panelImpl.add(filler, constraints);
        
        resetDefaultsButton = new JButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Restore_Defaults")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) { setDefaults(); }
        };
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 5, 6, 10);
        panelImpl.add(resetDefaultsButton, constraints);
        
        return panelImpl;
    }

}

/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.profiling.presets;

import com.sun.tools.visualvm.core.ui.components.Spacer;
import com.sun.tools.visualvm.uisupport.JExtendedSpinner;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerMemoryPanel extends JPanel {
    
    private JRadioButton allocRadioButton;
    private JRadioButton livenessRadioButton;
    private JCheckBox stackTracesCheckBox;
    private JLabel trackEveryLabel1;
    private JLabel trackEveryLabel2;
    private JSpinner trackEverySpinner;
    
    private boolean internalChange;
    
    
    public ProfilerMemoryPanel() {
        this(false);
    }
    
    ProfilerMemoryPanel(boolean mnemonics) {
        initComponents(mnemonics);
    }
    
    
    public ProfilingSettings getSettings() {
        ProfilingSettings settings = allocRadioButton.isSelected() ?
            ProfilingSettingsPresets.createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS) :
            ProfilingSettingsPresets.createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_LIVENESS);
        settings.setAllocStackTraceLimit(stackTracesCheckBox.isSelected() ? -1 : 0);
        settings.setAllocTrackEvery(((Integer) trackEverySpinner.getValue()).intValue());
        
        return settings;
    }
    
    
    public boolean settingsValid() { return true; }
    
    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) return;

        internalChange = true;
        allocRadioButton.setSelected(!preset.getMemoryModeP());
        livenessRadioButton.setSelected(preset.getMemoryModeP());
        stackTracesCheckBox.setSelected(preset.getStacksP());
        trackEverySpinner.setValue(preset.getAllocP());
        internalChange = false;
    }
    
    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) return;
        
        preset.setMemoryModeP(livenessRadioButton.isSelected());
        preset.setStacksP(stackTracesCheckBox.isSelected());
        preset.setAllocP((Integer)trackEverySpinner.getValue());
    }
    
    public abstract void settingsChanged();
    
    private void syncUI() {
        if (internalChange) return;
        settingsChanged();
    }
    
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) c.setEnabled(enabled);
    }
    
    private void initComponents(boolean mnemonics) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        
        JLabel referenceLabel = new JLabel("X"); // NOI18N
        
        ButtonGroup modesRadioGroup = new ButtonGroup();
        GridBagConstraints constraints;
        
        allocRadioButton = new JRadioButton() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(allocRadioButton, NbBundle.getMessage(ProfilerMemorySettings.class,
                "LBL_Profile_Allocations"), mnemonics); // NOI18N
        allocRadioButton.setToolTipText(NbBundle.getMessage(ProfilerMemorySettings.class, "TOOLTIP_Allocations")); // NOI18N
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
        add(allocRadioButton, constraints);
        
        livenessRadioButton = new JRadioButton() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(livenessRadioButton, NbBundle.getMessage(ProfilerMemorySettings.class,
                "LBL_Profile_AllocationsGC"), mnemonics); // NOI18N
        livenessRadioButton.setToolTipText(NbBundle.getMessage(ProfilerMemorySettings.class, "TOOLTIP_Allocations_GC")); // NOI18N
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
        add(livenessRadioButton, constraints);
        
        // trackEveryContainer - definition
        JPanel trackEveryContainer = new JPanel(new GridBagLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };

        // trackEveryLabel1
        trackEveryLabel1 = new JLabel();
        setText(trackEveryLabel1, NbBundle.getMessage(ProfilerMemorySettings.class,
                "LBL_Track_Every1"), mnemonics); // NOI18N
        trackEveryLabel1.setToolTipText(NbBundle.getMessage(ProfilerMemorySettings.class, "TOOLTIP_Track_Every")); // NOI18N
        trackEveryLabel1.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 5);
        trackEveryContainer.add(trackEveryLabel1, constraints);

        // trackEverySpinner
        trackEverySpinner = new JExtendedSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1)) {
            public Dimension getPreferredSize() {
                return new Dimension(55, super.getPreferredSize().height);
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void processFocusEvent(FocusEvent e) {
                super.processFocusEvent(e);
                syncUI();
            }
        };
        trackEverySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { syncUI(); }
        });
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)trackEverySpinner.getEditor();
        editor.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { try { trackEverySpinner.commitEdit(); } catch (Exception ex) {} }
            public void removeUpdate(DocumentEvent e) { try { trackEverySpinner.commitEdit(); } catch (Exception ex) {} }
            public void changedUpdate(DocumentEvent e) { try { trackEverySpinner.commitEdit(); } catch (Exception ex) {} }
        });
        trackEverySpinner.setToolTipText(NbBundle.getMessage(ProfilerMemorySettings.class, "TOOLTIP_Track_Every")); // NOI18N
        trackEveryLabel1.setLabelFor(trackEverySpinner);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        trackEveryContainer.add(trackEverySpinner, constraints);

        // trackEveryLabel2
        trackEveryLabel2 = new JLabel(NbBundle.getMessage(ProfilerMemorySettings.class, "LBL_Track_Every2")); // NOI18N
        trackEveryLabel2.setToolTipText(NbBundle.getMessage(ProfilerMemorySettings.class, "TOOLTIP_Track_Every")); // NOI18N
        trackEveryLabel2.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 0);
        trackEveryContainer.add(trackEveryLabel2, constraints);

        // trackEveryContainer - customization
        trackEveryContainer.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 10, 10, 10);
        add(trackEveryContainer, constraints);
        
        stackTracesCheckBox = new JCheckBox() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(stackTracesCheckBox, NbBundle.getMessage(ProfilerMemorySettings.class,
                "LBL_Record_Stacktraces"), mnemonics); // NOI18N
        stackTracesCheckBox.setToolTipText(NbBundle.getMessage(ProfilerMemorySettings.class, "TOOLTIP_Stack_Traces")); // NOI18N
        stackTracesCheckBox.setOpaque(false);
        stackTracesCheckBox.setBorder(referenceLabel.getBorder());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 10, 10);
        add(stackTracesCheckBox, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(Spacer.create(), constraints);
    }
    
    
    private static void setText(JLabel l, String text, boolean mnemonics) {
        if (mnemonics) Mnemonics.setLocalizedText(l, text);
        else l.setText(text.replace("&", "")); // NOI18N
    }
    
    private static void setText(AbstractButton b, String text, boolean mnemonics) {
        if (mnemonics) Mnemonics.setLocalizedText(b, text);
        else b.setText(text.replace("&", "")); // NOI18N
    }

}

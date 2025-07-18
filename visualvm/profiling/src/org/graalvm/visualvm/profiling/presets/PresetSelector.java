/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiling.presets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public final class PresetSelector extends JPanel {
    
    private final Runnable presetSynchronizer;
    
    private JLabel presetsLabel;
    private JComboBox<ProfilerPreset> presetsCombo;
    private JButton presetsButton;
    
    private final DefaultComboBoxModel<ProfilerPreset> selectorsModel;
    private final List<PresetSelector> allSelectors;
    
    private boolean savingCustom;
    
    private boolean customSelected;
    private boolean internalChange = false;
    
    
    PresetSelector(DefaultComboBoxModel<ProfilerPreset> selectorsModel, List<PresetSelector> allSelectors,
                   Runnable presetSynchronizer) {
        
        this.selectorsModel = selectorsModel;
        this.allSelectors = allSelectors;
        
        this.presetSynchronizer = presetSynchronizer;
        
        initComponents();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { notifySynchronizer(); }
        });
    }
    
    
    public ProfilerPreset getSelectedPreset() {
        return (ProfilerPreset)presetsCombo.getSelectedItem();
    }
    
    public ProfilerPreset customize(boolean presetValid) {
        ProfilerPreset custom = customPreset(true);
        custom.setValid(presetValid);
        internalChange = true;
        for (PresetSelector pSelector : allSelectors) pSelector.presetsCombo.setSelectedIndex(1);
        internalChange = false;
        return custom;
    }
    
    
    DefaultComboBoxModel<ProfilerPreset> getModel() {
        return selectorsModel;
    }
    
    boolean checkSavingCustom() {
        boolean ret = savingCustom;
        savingCustom = false;
        return ret;
    }
    
    
    private void selectedPresetChanged() {
        Object selected = presetsCombo.getSelectedItem();
        if (selected == null) return;
        
        boolean custom = selected == customPreset(false);
        if (customSelected != custom) {
            customSelected = custom;
            presetsButton.setText(custom ? NbBundle.getMessage(PresetSelector.class,
                                  "BTN_Save") : NbBundle.getMessage(PresetSelector.class, // NOI18N
                                  "BTN_Edit")); // NOI18N
        }
//        updatePresetsButton(true);
        if (internalChange) return;
        notifySynchronizer();
    }
    
    private void actionRequested() {
        if (customSelected) {
            savingCustom = true;
            ProfilerPresets.getInstance().savePreset(new ProfilerPreset(customPreset(true)));
        } else {
            ProfilerPresets.getInstance().editPresets(getSelectedPreset());
        }
    }
    
    private void notifySynchronizer() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { presetSynchronizer.run(); }
        });
    }
    
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        presetsLabel.setEnabled(enabled);
        presetsCombo.setEnabled(enabled);
        presetsButton.setEnabled(enabled);
//        updatePresetsButton(enabled);
    }
    
//    private void updatePresetsButton(boolean enabled) {
//        ProfilerPreset preset = getSelectedPreset();
//        for (PresetSelector pselector : allSelectors)
//            pselector.presetsButton.setEnabled(enabled && preset != null && preset.isValid());
//    }
    
    
    private ProfilerPreset customPreset(boolean create) {
        ProfilerPreset custom = new ProfilerPreset((ProfilerPreset)selectorsModel.getSelectedItem());
        custom.setName(NbBundle.getMessage(PresetSelector.class, "LBL_Custom")); // NOI18N
        
        if (selectorsModel.getSize() > 1) {
            ProfilerPreset customO = selectorsModel.getElementAt(1);
            if (custom.equals(customO)) return customO;
        }
        
        if (!create) return null;
        
        custom.setSelector((selectorsModel.getElementAt(0)).getSelector());
        
        internalChange = true;
        selectorsModel.insertElementAt(custom, 1);
        internalChange = false;
        
        return custom;
    }
    
    public static boolean isCustomPreset(ProfilerPreset preset) {
        return NbBundle.getMessage(PresetSelector.class, "LBL_Custom").equals(preset.getName()); // NOI18N
    }
    
    
    private void initComponents() {
        setOpaque(false);
        setLayout(new BorderLayout(5, 0));
        
        // presetsLabel
        presetsLabel = new JLabel(NbBundle.getMessage(PresetSelector.class,
                                  "LBL_Preset")); // NOI18N
        presetsLabel.setToolTipText(NbBundle.getMessage(PresetSelector.class,
                                    "TOOLTIP_Defined_presets")); // NOI18N
        add(presetsLabel, BorderLayout.WEST);
        
        // presetsCombo
        presetsCombo = new JComboBox<>(selectorsModel);
        presetsCombo.setToolTipText(NbBundle.getMessage(PresetSelector.class,
                                    "TOOLTIP_Defined_presets")); // NOI18N
        presetsCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { selectedPresetChanged(); }
        });
        add(presetsCombo, BorderLayout.CENTER);
        
        // presetsButton
        presetsButton = new JButton(NbBundle.getMessage(PresetSelector.class, "BTN_Save")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) { actionRequested(); }
        };
        presetsButton.setToolTipText(NbBundle.getMessage(PresetSelector.class,
                                     "TOOLTIP_Manage_presets")); // NOI18N
        add(presetsButton, BorderLayout.EAST);
        
        // UI tweaks
        Dimension dim1 = presetsButton.getPreferredSize();
        presetsButton.setText(NbBundle.getMessage(PresetSelector.class, "BTN_Edit")); // NOI18N
        Dimension dim2 = presetsButton.getPreferredSize();
        dim1.width = Math.max(dim1.width, dim2.width);
        dim1.height = Math.max(dim1.height, dim2.height);
        presetsButton.setPreferredSize(dim1);
    }

}

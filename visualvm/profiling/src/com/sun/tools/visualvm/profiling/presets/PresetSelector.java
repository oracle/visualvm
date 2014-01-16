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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JComboBox presetsCombo;
    private JButton presetsButton;
    
    private PresetSelector refSelector;
    
    private final ProfilerPreset defaultPreset;
    private ProfilerPreset customPreset;
    
    private final String selector;
    private boolean savingCustom;
    
    private boolean customSelected;
    private boolean internalChange = false;
    
    
    PresetSelector(final PresetSelector refSelector, ProfilerPreset defaultPreset,
                   ProfilerPreset customPreset, final ProfilerPreset toSelect,
                   Runnable presetSynchronizer, String selector) {
        
        this.refSelector = refSelector;
        if (refSelector != null) this.customPreset = refSelector.customPreset;
        
        this.selector = selector;
        
        this.presetSynchronizer = presetSynchronizer;
        this.defaultPreset = defaultPreset;
        this.customPreset = customPreset;
        
        initComponents(refSelector);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (refSelector != null)
                    refSelector.refSelector = PresetSelector.this;
                updatePresets(toSelect);
            }
        });
    }
    
    
    public ProfilerPreset getSelectedPreset() {
        return (ProfilerPreset)presetsCombo.getSelectedItem();
    }
    
    public void synchronizeWith(final PresetSelector selector) {
        // Need to invokeLater, to be called after updatePresets() in constructor
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (selector.customPreset != null) {
                    customPreset = new ProfilerPreset(selector.customPreset);
                    if (refSelector != null) refSelector.customPreset = customPreset;
                }
                updatePresets(selector.getSelectedPreset());
            }
        });
    }
    
    public ProfilerPreset customize(boolean presetValid) {
        if (customPreset == null) {
            customPreset = new ProfilerPreset(NbBundle.getMessage(
                    PresetSelector.class, "LBL_Custom"), null); // NOI18N
            if (refSelector != null) refSelector.customPreset = customPreset;
        }
        customPreset.setValid(presetValid);
        internalChange = true;
        if (presetsCombo.getItemCount() < 2 ||
            presetsCombo.getItemAt(1) != customPreset)
            presetsCombo.insertItemAt(customPreset, 1);
        presetsCombo.setSelectedIndex(1);
        internalChange = false;
        return customPreset;
    }
    
    
    void presetsChanged(ProfilerPreset selectedPreset) {
        updatePresets(savingCustom ? selectedPreset : null);
        savingCustom = false;
    }
    
    
    private void selectedPresetChanged() {
        Object selected = presetsCombo.getSelectedItem();
        if (selected == null) return;
        
        boolean custom = selected == customPreset;
        if (customSelected != custom) {
            customSelected = custom;
            presetsButton.setText(custom ? NbBundle.getMessage(PresetSelector.class,
                                  "BTN_Save") : NbBundle.getMessage(PresetSelector.class, // NOI18N
                                  "BTN_Edit")); // NOI18N
        }
        updatePresetsButton(true);
        if (internalChange) return;
        notifySynchronizer();
    }
    
    private void actionRequested() {
        if (customSelected) {
            ProfilerPreset preset = new ProfilerPreset(customPreset);
            preset.setSelector(selector);
            savingCustom = true;
            ProfilerPresets.getInstance().savePreset(preset);
        } else {
            ProfilerPresets.getInstance().editPresets(getSelectedPreset());
        }
    }
    
    private void notifySynchronizer() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { presetSynchronizer.run(); }
        });
    }
    
    private void updatePresets(ProfilerPreset toSelect) {
        if (toSelect == null) toSelect =
                (ProfilerPreset)presetsCombo.getSelectedItem();
        internalChange = true;
        
        presetsCombo.removeAllItems();
        presetsCombo.addItem(defaultPreset);
        if (savingCustom) customPreset = null;
        if (customPreset != null) presetsCombo.addItem(customPreset);
        ProfilerPresets.PresetsModel presets =
                ProfilerPresets.getInstance().getPresets();
        for (int i = 0; i < presets.size(); i++)
            presetsCombo.addItem(presets.get(i));
        
        if (toSelect != null) presetsCombo.setSelectedItem(toSelect);
        else presetsCombo.setSelectedIndex(0);
        
        internalChange = false;
        notifySynchronizer();
    }
    
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        presetsLabel.setEnabled(enabled);
        presetsCombo.setEnabled(enabled);
        updatePresetsButton(enabled);
    }
    
    private void updatePresetsButton(boolean enabled) {
        ProfilerPreset preset = getSelectedPreset();
        presetsButton.setEnabled(enabled && preset != null && preset.isValid());
        if (refSelector != null)
            refSelector.presetsButton.setEnabled(presetsButton.isEnabled());
    }
    
    
    private void initComponents(PresetSelector refSelector) {
        setOpaque(false);
        setLayout(new BorderLayout(5, 0));
        
        // presetsLabel
        presetsLabel = new JLabel(NbBundle.getMessage(PresetSelector.class,
                                  "LBL_Preset")); // NOI18N
        presetsLabel.setToolTipText(NbBundle.getMessage(PresetSelector.class,
                                    "TOOLTIP_Defined_presets")); // NOI18N
        add(presetsLabel, BorderLayout.WEST);
        
        // presetsCombo
        presetsCombo = refSelector == null ? new JComboBox() :
                       new JComboBox(refSelector.presetsCombo.getModel());
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

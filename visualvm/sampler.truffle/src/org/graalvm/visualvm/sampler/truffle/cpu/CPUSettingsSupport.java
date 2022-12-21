/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle.cpu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.profiling.presets.PresetSelector;
import org.graalvm.visualvm.profiling.presets.SamplerCPUPanel;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class CPUSettingsSupport {
    
    private static final String PROP_MODE = "CPUSettingsSupport.Mode"; // NOI18N
    private static final String PROP_SPLIT_COMPILED_INLINED = "CPUSettingsSupport.SplitCompiledInlined"; // NOI18N
    private JComboBox<Mode> modeCombo;
    private JLabel modeLabel;
    private Spacer space;
    
    private static enum Mode {
        EXCLUDE_INLINED_ROOTS { @Override public String toString() { return NbBundle.getMessage(CPUSettingsSupport.class, "MODE_EXCLUDE_INLINED_ROOTS"); } }, // NOI18N
        ROOTS { @Override public String toString() { return NbBundle.getMessage(CPUSettingsSupport.class, "MODE_ROOTS"); } }, // NOI18N
        STATEMENTS { @Override public String toString() { return NbBundle.getMessage(CPUSettingsSupport.class, "MODE_STATEMENTS"); } }, // NOI18N
    };
    
    
    private JPanel container;
    private SamplerCPUPanel panel;
    private PresetSelector selector;
    
    private DataViewComponent.DetailsView detailsView;
    
    
    public DataViewComponent.DetailsView getDetailsView() {
        if (detailsView == null) {
            detailsView = new DataViewComponent.DetailsView(NbBundle.getMessage(
                          CPUSettingsSupport.class, "LBL_Cpu_settings"), null, 10, // NOI18N
                          new ScrollableContainer(createPanel()), null);
        }
        return detailsView;
    }
    
    
    public ProfilingSettings getSettings() { return panel.getSettings(); }

    public int getSamplingRate() { return panel.getSamplingRate(); }
    
    public int getRefreshRate() { return panel.getRefreshRate(); }
    
    public String getMode() {
        return NbPreferences.forModule(CPUSettingsSupport.class).get(PROP_MODE, Mode.EXCLUDE_INLINED_ROOTS.name());
    }
    
    public void enableMode(boolean enable) {
        if (panel != null) {
            modeLabel.setVisible(enable);
            modeCombo.setVisible(enable);
            space.setVisible(enable);
            container.revalidate();
        }
    }

    public boolean isSplitCompiledInlined() {
        return NbPreferences.forModule(CPUSettingsSupport.class).getBoolean(PROP_SPLIT_COMPILED_INLINED, false);
    }
    
    public void saveSettings() {
        // NOTE: might save custom configuration here
    }
    
    public abstract boolean presetValid();
    
    public boolean settingsValid() { return panel.settingsValid(); }
    
    public void showSettings(DataViewComponent dvc) {
        dvc.selectDetailsView(getDetailsView());
    }
    
    public abstract PresetSelector createSelector(Runnable presetSynchronizer);
    
    
    public void setEnabled(boolean enabled) {
        if (container != null) container.setEnabled(enabled);
    }
    
    private JPanel createPanel() {
        panel = new SamplerCPUPanel() {
            public void settingsChanged() {
                panel.saveToPreset(selector.customize(presetValid()));
            }
        };
        
        selector = createSelector(new Runnable() {
            public void run() { panel.loadFromPreset(selector.getSelectedPreset()); }
        });
        selector.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        container = new JPanel(new BorderLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents()) c.setEnabled(enabled);
            }
        };
        container.setOpaque(false);
        container.add(panel, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents()) c.setEnabled(enabled);
            }
        };
        southPanel.setOpaque(false);
        southPanel.add(selector, BorderLayout.NORTH);
        
        JPanel engineSettingsPanel = new JPanel(new GridBagLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents()) c.setEnabled(enabled);
            }
        };
        engineSettingsPanel.setOpaque(false);
        
        SectionSeparator section = new SectionSeparator(NbBundle.getMessage(CPUSettingsSupport.class, "SEP_EngineSettings"), new JLabel().getFont()); // NOI18N
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(25, 10, 5, 5);
        engineSettingsPanel.add(section, constraints);
        
        modeLabel = new JLabel();
        modeLabel.setText(NbBundle.getMessage(CPUSettingsSupport.class, "LBL_Mode")); // NOI18N
        modeLabel.setToolTipText(NbBundle.getMessage(CPUSettingsSupport.class, "TOOLTIP_Mode")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 5, 5);
        engineSettingsPanel.add(modeLabel, constraints);

        modeCombo = new JComboBox<Mode>(Mode.values()) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        modeCombo.setSelectedItem(Mode.valueOf(getMode()));
        modeLabel.setLabelFor(modeCombo);
        modeCombo.setToolTipText(NbBundle.getMessage(CPUSettingsSupport.class, "TOOLTIP_Mode")); // NOI18N
        modeCombo.setEditable(false);
        modeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Mode selected = (Mode)modeCombo.getSelectedItem();
                NbPreferences.forModule(CPUSettingsSupport.class).put(PROP_MODE, selected.name());
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 0, 5, 5);
        engineSettingsPanel.add(modeCombo, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 5, 0);
        space = Spacer.create();
        engineSettingsPanel.add(space, constraints);
        
        
        final JCheckBox splitChoice = new JCheckBox();
        splitChoice.setSelected(isSplitCompiledInlined());
        splitChoice.setText(NbBundle.getMessage(CPUSettingsSupport.class,"LBL_Split")); // NOI18N
        splitChoice.setToolTipText(NbBundle.getMessage(CPUSettingsSupport.class, "TOOLTIP_Split")); // NOI18N
        splitChoice.setOpaque(false);
        splitChoice.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                NbPreferences.forModule(CPUSettingsSupport.class).putBoolean(PROP_SPLIT_COMPILED_INLINED, splitChoice.isSelected());
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(1, 10, 10, 5);
        engineSettingsPanel.add(splitChoice, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 10, 0);
        engineSettingsPanel.add(Spacer.create(), constraints);
        
        southPanel.add(engineSettingsPanel, BorderLayout.SOUTH);
        
        container.add(southPanel, BorderLayout.SOUTH);
        
        return container;
    }

}

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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SamplerMemoryPanel extends JPanel {
    
    private JLabel refreshRateLabel;
    private JLabel refreshUnitsLabel;
    private JComboBox refreshCombo;
    
    private boolean internalChange;
    
    
    public SamplerMemoryPanel() {
        this(false);
    }
    
    SamplerMemoryPanel(boolean mnemonics) {
        initComponents(mnemonics);
    }
    
    
    public ProfilingSettings getSettings() {
        return null;
    }
    
    public int getSamplingRate() {
        return (Integer)refreshCombo.getSelectedItem();
    }

    public int getRefreshRate() {
        return getSamplingRate();
    }
    
    
    public boolean settingsValid() { return true; }
    
    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) return;

        internalChange = true;
        refreshCombo.setSelectedItem(preset.getSamplingRefreshRateS());
        internalChange = false;
    }
    
    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) return;
        
        preset.setSamplingRefreshRateS((Integer)refreshCombo.getSelectedItem());
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

        GridBagConstraints constraints;

        refreshRateLabel = new JLabel();
        setText(refreshRateLabel, NbBundle.getMessage(SamplerMemoryPanel.class,
                "LBL_Sampling_refresh"), mnemonics); // NOI18N
        refreshRateLabel.setToolTipText(NbBundle.getMessage(SamplerMemoryPanel.class,
                "TOOLTIP_Sampling_refresh")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(7, 10, 10, 5);
        add(refreshRateLabel, constraints);

        Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        refreshCombo = new JComboBox(refreshRates) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        refreshRateLabel.setLabelFor(refreshCombo);
        refreshCombo.setToolTipText(NbBundle.getMessage(SamplerMemoryPanel.class,
                "TOOLTIP_Sampling_refresh")); // NOI18N
        refreshCombo.setEditable(false);
        refreshCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { syncUI(); }
        });
        refreshCombo.setRenderer(new ComboRenderer(refreshCombo));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(7, 0, 10, 5);
        add(refreshCombo, constraints);

        refreshUnitsLabel = new JLabel(NbBundle.getMessage(SamplerMemoryPanel.class,
                "LBL_units_ms")); // NOI18N
        refreshUnitsLabel.setToolTipText(NbBundle.getMessage(SamplerMemoryPanel.class,
                "TOOLTIP_Sampling_refresh")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(7, 0, 10, 5);
        add(refreshUnitsLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(7, 0, 10, 0);
        add(Spacer.create(), constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
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


    private static class ComboRenderer implements ListCellRenderer {

        private ListCellRenderer renderer;

        ComboRenderer(JComboBox combo) {
            renderer = combo.getRenderer();
            if (renderer instanceof JLabel)
                ((JLabel)renderer).setHorizontalAlignment(JLabel.TRAILING);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            return renderer.getListCellRendererComponent(list, NumberFormat.
                    getInstance().format(value), index, isSelected, cellHasFocus);
        }

    }

}

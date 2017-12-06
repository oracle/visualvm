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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.filters.JavaTypeFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SamplerCPUPanel extends JPanel {
    
    private JRadioButton inclFilterRadioButton;
    private JRadioButton exclFilterRadioButton;
    private TextAreaComponent filtersArea;
    private JLabel sampleRateLabel;
    private JComboBox sampleRateCombo;
    private JLabel sampleRateUnitsLabel;
    private JLabel refreshRateLabel;
    private JLabel refreshUnitsLabel;
    private JComboBox refreshRateCombo;
    
    private final Runnable validator;
    private boolean filtersValid = true;
    private boolean internalChange;
    
    
    public SamplerCPUPanel() {
        this(null, false);
    }
    
    SamplerCPUPanel(Runnable validator, boolean mnemonics) {
        this.validator = validator;
        initComponents(mnemonics);
    }
    
    
    public ProfilingSettings getSettings() {
        ProfilingSettings settings = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
        settings.setProfilingType(ProfilingSettings.PROFILE_CPU_SAMPLING);
        settings.setCPUProfilingType(CommonConstants.CPU_SAMPLED);
        
        String filter = getFilterValue();
        if (filter.isEmpty() || "*".equals(filter) || "**".equals(filter)) { // NOI18N
            settings.setInstrumentationFilter(new JavaTypeFilter());
        } else {
            int filterType = inclFilterRadioButton.isSelected() ?
                             JavaTypeFilter.TYPE_INCLUSIVE : JavaTypeFilter.TYPE_EXCLUSIVE;
            String filterValue = PresetsUtils.normalizeValue(filter);
            settings.setInstrumentationFilter(new JavaTypeFilter(filterValue, filterType));
        }
        
        return settings;
    }
    
    public int getSamplingRate() {
        return (Integer)sampleRateCombo.getSelectedItem();
    }
    
    public int getRefreshRate() {
        return (Integer)refreshRateCombo.getSelectedItem();
    }
    
    
    public boolean settingsValid() { return filtersValid; }
    
    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) return;

        internalChange = true;
        inclFilterRadioButton.setSelected(!preset.getFilterModeS());
        exclFilterRadioButton.setSelected(preset.getFilterModeS());
        filtersArea.getTextArea().setText(preset.getFilterS());
        sampleRateCombo.setSelectedItem(preset.getSamplingRateS());
        refreshRateCombo.setSelectedItem(preset.getRefreshRateS());
        internalChange = false;
        
        checkFilterValidity();
    }
    
    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) return;
        
        preset.setFilterModeS(exclFilterRadioButton.isSelected());
        preset.setFilterS(filtersArea.getTextArea().getText());
        preset.setSamplingRateS((Integer)sampleRateCombo.getSelectedItem());
        preset.setRefreshRateS((Integer)refreshRateCombo.getSelectedItem());
    }
    
    public abstract void settingsChanged();
    
    private void syncUI() {
        if (internalChange) return;
        settingsChanged();
    }
    
    
    private void checkFilterValidity() {
        filtersValid = isFilterValueValid();
        filtersArea.getTextArea().setForeground(filtersValid ?
            UIManager.getColor("TextArea.foreground") : Color.RED); // NOI18N
        if (validator != null) validator.run();
    }

    public boolean isFilterValueValid() {
        String filterValue = PresetsUtils.normalizeValue(getFilterValue());
        return PresetsUtils.isValidJavaValue(filterValue, true, false);
    }

    private String getFilterValue() {
        return filtersArea.getTextArea().getText().trim();
    }
    
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) c.setEnabled(enabled);
    }
    
    private void initComponents(boolean mnemonics) {
        setOpaque(false);
        setLayout(new GridBagLayout());

        ButtonGroup filterRadiosGroup = new ButtonGroup();
        GridBagConstraints constraints;

        JLabel referenceLabel = new JLabel("X"); // NOI18N

        JPanel radiosPanel = new JPanel(new GridBagLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };
        radiosPanel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(radiosPanel, constraints);

        inclFilterRadioButton = new JRadioButton() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(inclFilterRadioButton, NbBundle.getMessage(SamplerCPUPanel.class,
                "LBL_Profile_Incl_S"), mnemonics); // NOI18N
        inclFilterRadioButton.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Inclusive_Filter_S")); // NOI18N
        inclFilterRadioButton.setOpaque(false);
        inclFilterRadioButton.setBorder(referenceLabel.getBorder());
        Dimension d1 = inclFilterRadioButton.getPreferredSize();
        d1.height = Math.max(d1.height, referenceLabel.getPreferredSize().height);
        inclFilterRadioButton.setPreferredSize(d1);
        filterRadiosGroup.add(inclFilterRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 5, 5);
        radiosPanel.add(inclFilterRadioButton, constraints);

        exclFilterRadioButton = new JRadioButton() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(exclFilterRadioButton, NbBundle.getMessage(SamplerCPUPanel.class,
                "LBL_Profile_Excl_S"), mnemonics); // NOI18N
        exclFilterRadioButton.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Exclusive_Filter_S")); // NOI18N
        exclFilterRadioButton.setOpaque(false);
        exclFilterRadioButton.setBorder(referenceLabel.getBorder());
        Dimension d2 = exclFilterRadioButton.getPreferredSize();
        d2.height = Math.max(d2.height, referenceLabel.getPreferredSize().height);
        exclFilterRadioButton.setPreferredSize(d2);
        filterRadiosGroup.add(exclFilterRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 5, 5, 10);
        radiosPanel.add(exclFilterRadioButton, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 0, 5, 0);
        radiosPanel.add(Spacer.create(), constraints);

        filtersArea = createTextArea(2);
        filtersArea.getTextArea().setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Instrumentation_Filter_S")); // NOI18N
        filtersArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkFilterValidity(); syncUI(); }
            public void removeUpdate(DocumentEvent e) { checkFilterValidity(); syncUI(); }
            public void changedUpdate(DocumentEvent e) { checkFilterValidity(); syncUI(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 10, 10, 10);
        add(filtersArea, constraints);

        sampleRateLabel = new JLabel();
        setText(sampleRateLabel, NbBundle.getMessage(SamplerCPUPanel.class,
                "LBL_Sampling_rate"), mnemonics); // NOI18N
        sampleRateLabel.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Sampling_rate")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 5, 5);
        add(sampleRateLabel, constraints);

        Integer[] samplingRates =
            new Integer[] { 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000 };
        sampleRateCombo = new JComboBox(samplingRates) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        sampleRateLabel.setLabelFor(sampleRateCombo);
        sampleRateCombo.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Sampling_rate")); // NOI18N
        sampleRateCombo.setEditable(false);
        sampleRateCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { syncUI(); }
        });
        sampleRateCombo.setRenderer(new ComboRenderer(sampleRateCombo));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 0, 5, 5);
        add(sampleRateCombo, constraints);

        sampleRateUnitsLabel = new JLabel(NbBundle.getMessage(
                SamplerCPUPanel.class, "LBL_units_ms")); // NOI18N
        sampleRateUnitsLabel.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Sampling_rate")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 0, 5, 5);
        add(sampleRateUnitsLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 5, 0);
        add(Spacer.create(), constraints);

        refreshRateLabel = new JLabel();
        setText(refreshRateLabel, NbBundle.getMessage(SamplerCPUPanel.class,
                "LBL_Refresh_rate"), mnemonics); // NOI18N
        refreshRateLabel.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Refresh_rate")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(1, 10, 10, 5);
        add(refreshRateLabel, constraints);

        Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        refreshRateCombo = new JComboBox(refreshRates) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        refreshRateLabel.setLabelFor(refreshRateCombo);
        refreshRateCombo.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Refresh_rate")); // NOI18N
        refreshRateCombo.setEditable(false);
        refreshRateCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { syncUI(); }
        });
        refreshRateCombo.setRenderer(new ComboRenderer(refreshRateCombo));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(1, 0, 10, 5);
        add(refreshRateCombo, constraints);

        refreshUnitsLabel = new JLabel(NbBundle.getMessage(
                SamplerCPUPanel.class, "LBL_units_ms")); // NOI18N
        refreshUnitsLabel.setToolTipText(NbBundle.getMessage(
                SamplerCPUPanel.class, "TOOLTIP_Refresh_rate")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(1, 0, 10, 5);
        add(refreshUnitsLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 7;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(1, 0, 10, 0);
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
    
    
    private static TextAreaComponent createTextArea(int rows) {
        final JTextArea rootsArea = new JTextArea();
        rootsArea.setFont(new Font("Monospaced", Font.PLAIN, // NOI18N
                UIManager.getFont("Label.font").getSize())); // NOI18N
        TextAreaComponent rootsAreaScrollPane = new TextAreaComponent(rootsArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                rootsArea.setEnabled(enabled);
            }
        };
        rootsAreaScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JTextArea referenceArea = new JTextArea("X"); // NOI18N
        referenceArea.setFont(rootsArea.getFont());
        referenceArea.setRows(rows);
        Insets insets = rootsAreaScrollPane.getInsets();
        rootsAreaScrollPane.setPreferredSize(new Dimension(1,
                referenceArea.getPreferredSize().height + (insets != null ?
                 insets.top + insets.bottom : 0)));
        return rootsAreaScrollPane;
    }
    
    private static class TextAreaComponent extends JScrollPane {
        public TextAreaComponent(JTextArea textArea, int vPolicy, int hPolicy) {
            super(textArea, vPolicy, hPolicy);
        }
        public JTextArea getTextArea() {
            return (JTextArea)getViewport().getView();
        }
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

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

package org.graalvm.visualvm.profiling.presets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.filters.JavaTypeFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerCPUPanel extends JPanel {
    
    private JLabel rootClassesLabel;
    private TextAreaComponent rootsArea;
//    private JCheckBox runnablesCheckBox;
    private JRadioButton inclFilterRadioButton;
    private JRadioButton exclFilterRadioButton;
    private TextAreaComponent filtersArea;
    
    private final Runnable validator;
    private boolean rootsValid = true;
    private boolean filtersValid = true;
    private boolean internalChange;
    
    
    public ProfilerCPUPanel() {
        this(null, false);
    }
    
    ProfilerCPUPanel(Runnable validator, boolean mnemonics) {
        this.validator = validator;
        initComponents(mnemonics);
    }
    
    
    public ProfilingSettings getSettings() {
        ProfilingSettings settings = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
        settings.setProfilingType(ProfilingSettings.PROFILE_CPU_PART);
        settings.setCPUProfilingType(settings.getSamplingInterval() <= 0 ?
                                     CommonConstants.CPU_INSTR_FULL :
                                     CommonConstants.CPU_INSTR_SAMPLED);
        
        String[] rootsValues = GenericFilter.values(PresetsUtils.normalizeValue(getRootsValue()));
        ClientUtils.SourceCodeSelection[] roots = (rootsValues.length == 1 && rootsValues[0].isEmpty()) ?
            new ClientUtils.SourceCodeSelection[0] :
            new ClientUtils.SourceCodeSelection[rootsValues.length];
        for (int i = 0; i < roots.length; i++)
            roots[i] = new ClientUtils.SourceCodeSelection(rootsValues[i], "*", null); // NOI18N
        settings.addRootMethods(roots);
        
        String filter = getFilterValue();
        if (filter.isEmpty() || "*".equals(filter) || "**".equals(filter)) { // NOI18N
            settings.setInstrumentationFilter(new JavaTypeFilter());
        } else {
            int filterType = inclFilterRadioButton.isSelected() ?
                             JavaTypeFilter.TYPE_INCLUSIVE : JavaTypeFilter.TYPE_EXCLUSIVE;
            String filterValue = PresetsUtils.normalizeValue(filter);
            settings.setInstrumentationFilter(new JavaTypeFilter(filterValue, filterType));
        }
        
        settings.setStackDepthLimit(Integer.MAX_VALUE);
        
        return settings;
    }
    
    
    public boolean settingsValid() { return rootsValid && filtersValid; }
    
    public void highlightInvalid() {
        if (getRootsValue().isEmpty()) rootsArea.getTextArea().setText(ProfilerPresets.DEFINE_CLASSES);
    }
    
    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) return;

        internalChange = true;
        rootsArea.getTextArea().setText(preset.getRootsP());
//        runnablesCheckBox.setSelected(preset.getRunnablesP());
        inclFilterRadioButton.setSelected(!preset.getFilterModeP());
        exclFilterRadioButton.setSelected(preset.getFilterModeP());
        filtersArea.getTextArea().setText(preset.getFilterP());
        highlightInvalid();
        internalChange = false;
        
        checkRootValidity();
        checkFilterValidity();
    }
    
    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) return;
        
        preset.setRootsP(getRootsValue());
//        preset.setRunnablesP(runnablesCheckBox.isSelected());
        preset.setFilterModeP(exclFilterRadioButton.isSelected());
        preset.setFilterP(getFilterValue());
    }
    
    public abstract void settingsChanged();
    
    private void syncUI() {
        if (internalChange) return;
        settingsChanged();
    }
    
    
    private void checkRootValidity() {
        rootsValid = isRootValueValid();
        rootsArea.getTextArea().setForeground(rootsValid ?
            UIManager.getColor("TextArea.foreground") : Color.RED); // NOI18N
        if (validator != null) validator.run();
    }
    
    public boolean isRootValueValid() {
        String rootsValue = PresetsUtils.normalizeValue(getRootsValue());
        return PresetsUtils.isValidJavaValue(rootsValue, false, false);
    }
    
    private String getRootsValue() {
        return rootsArea.getTextArea().getText().trim();
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
        
        rootClassesLabel = new JLabel();
        setText(rootClassesLabel, NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Root_Classes"), mnemonics); // NOI18N
        Dimension d = rootClassesLabel.getPreferredSize();
        JRadioButton refRadion = new JRadioButton(NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Root_Classes")); // NOI18N
        refRadion.setBorder(rootClassesLabel.getBorder());
        d.height = Math.max(d.height, refRadion.getPreferredSize().height);
        rootClassesLabel.setPreferredSize(d);
        rootClassesLabel.setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Root_Classes")); // NOI18N
        rootClassesLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 5, 10);
        add(rootClassesLabel, constraints);
        
        rootsArea = createTextArea(2);
        rootClassesLabel.setLabelFor(rootsArea.getTextArea());
        rootsArea.getTextArea().setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Root_Classes")); // NOI18N
        rootsArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkRootValidity(); syncUI(); }
            public void removeUpdate(DocumentEvent e) { checkRootValidity(); syncUI(); }
            public void changedUpdate(DocumentEvent e) { checkRootValidity(); syncUI(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 0.65;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 10, 10, 10);
        add(rootsArea, constraints);
        
//        runnablesCheckBox = new JCheckBox() {
//            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
//        };
//        setText(runnablesCheckBox, NbBundle.getMessage(ProfilerCPUSettings.class,
//                "LBL_Profile_Runnables"), mnemonics);
//        runnablesCheckBox.setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_New_Runnables")); // NOI18N
//        runnablesCheckBox.setOpaque(false);
//        runnablesCheckBox.setBorder(rootClassesLabel.getBorder());
//        constraints = new GridBagConstraints();
//        constraints.gridx = 0;
//        constraints.gridy = 3;
//        constraints.gridwidth = GridBagConstraints.REMAINDER;
//        constraints.anchor = GridBagConstraints.WEST;
//        constraints.fill = GridBagConstraints.NONE;
//        constraints.insets = new Insets(0, 10, 10, 10);
//        add(runnablesCheckBox, constraints);
        
        inclFilterRadioButton = new JRadioButton() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(inclFilterRadioButton, NbBundle.getMessage(ProfilerCPUSettings.class,
                "LBL_Profile_Incl"), mnemonics);
        inclFilterRadioButton.setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Inclusive_Filter")); // NOI18N
        inclFilterRadioButton.setOpaque(false);
        inclFilterRadioButton.setBorder(rootClassesLabel.getBorder());
        filterRadiosGroup.add(inclFilterRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 5, 5);
        add(inclFilterRadioButton, constraints);
        
        exclFilterRadioButton = new JRadioButton() {
            protected void fireActionPerformed(ActionEvent e) { syncUI(); }
        };
        setText(exclFilterRadioButton, NbBundle.getMessage(ProfilerCPUSettings.class,
                "LBL_Profile_Excl"), mnemonics);
        exclFilterRadioButton.setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Exclusive_Filter")); // NOI18N
        exclFilterRadioButton.setOpaque(false);
        exclFilterRadioButton.setBorder(rootClassesLabel.getBorder());
        filterRadiosGroup.add(exclFilterRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 5, 5, 10);
        add(exclFilterRadioButton, constraints);
        
        filtersArea = createTextArea(2);
        filtersArea.getTextArea().setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Instrumentation_Filter")); // NOI18N
        filtersArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkFilterValidity(); syncUI(); }
            public void removeUpdate(DocumentEvent e) { checkFilterValidity(); syncUI(); }
            public void changedUpdate(DocumentEvent e) { checkFilterValidity(); syncUI(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weighty = 0.35;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 10, 10, 10);
        add(filtersArea, constraints);
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
        rootsArea.setFont(new Font("Monospaced", Font.PLAIN, UIManager.getFont("Label.font").getSize())); // NOI18N
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
        rootsAreaScrollPane.setPreferredSize(new Dimension(1, referenceArea.getPreferredSize().height + 
                (insets != null ? insets.top + insets.bottom : 0)));
        return rootsAreaScrollPane;
    }
    
    private static class TextAreaComponent extends JScrollPane {
        public TextAreaComponent(JTextArea textArea, int vPolicy, int hPolicy) { super(textArea, vPolicy, hPolicy); }
        public JTextArea getTextArea() { return (JTextArea)getViewport().getView(); }
    }

}

/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.uisupport.JExtendedSpinner;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.filters.JavaTypeFilter;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerMemoryPanel extends JPanel {
    
    private TextAreaComponent filtersArea;
    private JCheckBox lifecycleCheckbox;
    private JCheckBox outgoingCheckbox;
    private JExtendedSpinner outgoingSpinner;
    private JLabel unlimited;
    private JLabel noAllocs;
    
    private final Runnable validator;
    private boolean rootsValid = true;
    private boolean internalChange;
    
    
    public ProfilerMemoryPanel() {
        this(null, false);
    }
    
    ProfilerMemoryPanel(Runnable validator, boolean mnemonics) {
        this.validator = validator;
        initComponents(mnemonics);
    }
    
    
    public ProfilingSettings getSettings() {
        ProfilingSettings settings = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
        
        settings.setProfilingType(lifecycleCheckbox.isSelected() ? ProfilingSettings.PROFILE_MEMORY_LIVENESS :
                                                                ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS);
        
        String filterValue = PresetsUtils.normalizeValue(getFilterValue());
        settings.setInstrumentationFilter(new JavaTypeFilter(filterValue, JavaTypeFilter.TYPE_INCLUSIVE));
        
        boolean limitAlloc = outgoingCheckbox.isSelected();
        int limit = (Integer)outgoingSpinner.getValue();
        settings.setAllocStackTraceLimit(!limitAlloc ? -10 : limit);
        
        return settings;
    }
    
    
    public boolean settingsValid() { return rootsValid; }
    
    public void highlighInvalid() {
        if (getFilterValue().isEmpty()) filtersArea.getTextArea().setText(ProfilerPresets.DEFINE_CLASSES);
    }
    
    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) return;

        internalChange = true;
        filtersArea.getTextArea().setText(preset.getMemoryFilterP().trim());
        lifecycleCheckbox.setSelected(preset.getMemoryModeP());
        outgoingCheckbox.setSelected(preset.getStacksP());
        outgoingSpinner.setValue(preset.getAllocP());
        highlighInvalid();
        internalChange = false;
        
        checkRootValidity();
        
        updateAllocControls();
    }
    
    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) return;
        
        preset.setMemoryFilterP(getFilterValue());
        preset.setMemoryModeP(lifecycleCheckbox.isSelected());
        preset.setStacksP(outgoingCheckbox.isSelected());
        preset.setAllocP((Integer)outgoingSpinner.getValue());
    }
    
    public abstract void settingsChanged();
    
    private void syncUI() {
        if (internalChange) return;
        settingsChanged();
    }
    
    private void updateAllocControls() {
        boolean selected = outgoingCheckbox.isSelected();
        unlimited.setVisible(!selected);
        outgoingSpinner.setVisible(selected);
        noAllocs.setVisible(selected && (Integer)outgoingSpinner.getValue() == 0);
    }
    
    
    private void checkRootValidity() {
        rootsValid = isRootValueValid();
        filtersArea.getTextArea().setForeground(rootsValid ?
            UIManager.getColor("TextArea.foreground") : Color.RED); // NOI18N
        if (validator != null) validator.run();
    }
    
    public boolean isRootValueValid() {
        String filterValue = PresetsUtils.normalizeValue(getFilterValue());
        return PresetsUtils.isValidJavaValue(filterValue, false, true);
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
        
        GridBagConstraints constraints;
        
        JLabel filtersLabel = new JLabel();
        setText(filtersLabel, NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Root_Classes"), mnemonics); // NOI18N
        Dimension d = filtersLabel.getPreferredSize();
        JRadioButton refRadion = new JRadioButton(NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Root_Classes")); // NOI18N
        refRadion.setBorder(filtersLabel.getBorder());
        d.height = Math.max(d.height, refRadion.getPreferredSize().height);
        filtersLabel.setPreferredSize(d);
        filtersLabel.setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Root_Classes")); // NOI18N
        filtersLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 5, 10);
        add(filtersLabel, constraints);
        
        filtersArea = createTextArea(2);
        filtersLabel.setLabelFor(filtersArea.getTextArea());
        filtersArea.getTextArea().setToolTipText(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_TOOLTIP_Filter")); // NOI18N
        filtersArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
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
        add(filtersArea, constraints);
        
        
        lifecycleCheckbox = new JCheckBox(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_BTN_Track_live")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                syncUI();
            }
        };
        lifecycleCheckbox.setToolTipText(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_TOOLTIP_Track_live")); // NOI18N
        lifecycleCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 0, 5);
        add(lifecycleCheckbox, constraints);
        
        outgoingCheckbox = new JCheckBox(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_BTN_Limit_alloc")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                updateAllocControls();
                syncUI();
            }
        };
        outgoingCheckbox.setToolTipText(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_TOOLTIP_Limit_alloc")); // NOI18N
        outgoingCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 5, 5);
        add(outgoingCheckbox, constraints);
        
        outgoingSpinner = new JExtendedSpinner(new SpinnerNumberModel(Math.abs(10), 0, 99, 1)) {
            protected void fireStateChanged() { updateAllocControls(); syncUI(); super.fireStateChanged(); }
        };
        outgoingSpinner.setToolTipText(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_TOOLTIP_Limit_alloc2")); // NOI18N
        JComponent editor = outgoingSpinner.getEditor();
        JTextField field = editor instanceof JSpinner.DefaultEditor ?
                ((JSpinner.DefaultEditor)editor).getTextField() : null;
        if (field != null) field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { change(); }
            public void removeUpdate(DocumentEvent e) { change(); }
            public void changedUpdate(DocumentEvent e) { change(); }
            private void change() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        noAllocs.setVisible(outgoingSpinner.isVisible() &&
                                            (Integer)outgoingSpinner.getValue() == 0);
                    }
                });
                updateAllocControls();
                syncUI();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 0, 5, 5);
        add(outgoingSpinner, constraints);
        
        unlimited = new GrayLabel(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_LBL_unlimited")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 0, 5, 5);
        add(unlimited, constraints);
        
        noAllocs = new GrayLabel(NbBundle.getMessage(ProfilerMemoryPanel.class, "ProfilerMemoryPanel_LBL_No_alloc")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 5, 5, 5);
        add(noAllocs, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.weighty = 0.35;
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
    
//    private static void setText(AbstractButton b, String text, boolean mnemonics) {
//        if (mnemonics) Mnemonics.setLocalizedText(b, text);
//        else b.setText(text.replace("&", "")); // NOI18N
//    }
    
    private static TextAreaComponent createTextArea(int rows) {
        final JTextArea rootsArea = new JTextArea();
        rootsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Label.font").getSize())); // NOI18N
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
        TextAreaComponent(JTextArea textArea, int vPolicy, int hPolicy) { super(textArea, vPolicy, hPolicy); }
        public JTextArea getTextArea() { return (JTextArea)getViewport().getView(); }
    }

}

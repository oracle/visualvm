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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
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
import org.graalvm.visualvm.lib.jfluid.filters.TextFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcCCTProvider;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerJDBCPanel extends JPanel {
    
    private static final String[] JDBC_MARKER_CLASSES = {
        JdbcCCTProvider.DRIVER_INTERFACE,
        JdbcCCTProvider.CONNECTION_INTERFACE,
        JdbcCCTProvider.STATEMENT_INTERFACE,
        JdbcCCTProvider.PREPARED_STATEMENT_INTERFACE,
        JdbcCCTProvider.CALLABLE_STATEMENT_INTERFACE
    };
    
    
    private JLabel filterLabel;
    private TextAreaComponent filterArea;
    
    private boolean internalChange;
    
    
    public ProfilerJDBCPanel() {
        this(false);
    }
    
    ProfilerJDBCPanel(boolean mnemonics) {
        initComponents(mnemonics);
    }
    
    
    public ProfilingSettings getSettings() {
        ProfilingSettings settings = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
        settings.setProfilingType(ProfilingSettings.PROFILE_CPU_JDBC);
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);

        ClientUtils.SourceCodeSelection[] roots = new ClientUtils.SourceCodeSelection[JDBC_MARKER_CLASSES.length];
        for (int i = 0; i < JDBC_MARKER_CLASSES.length; i++) {
            roots[i] = new ClientUtils.SourceCodeSelection(JDBC_MARKER_CLASSES[i], "*", null); // NOI18N
            roots[i].setMarkerMethod(true);
        }
        settings.addRootMethods(roots);

        String filter = PresetsUtils.normalizeValue(getFilterValue());
        settings.setInstrumentationFilter(new TextFilter(filter, TextFilter.TYPE_INCLUSIVE, false));
        
        return settings;
    }
    
    
    public boolean settingsValid() { return true; }
    
    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) return;

        internalChange = true;
        filterArea.getTextArea().setText(preset.getJDBCFilterP().trim());
        internalChange = false;
    }
    
    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) return;
        preset.setJDBCFilterP(getFilterValue());
    }
    
    public abstract void settingsChanged();
    
    private void syncUI() {
        if (internalChange) return;
        settingsChanged();
    }
    
    
    private String getFilterValue() {
        return filterArea.getTextArea().getText().trim();
    }
    
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) c.setEnabled(enabled);
    }
    
    private void initComponents(boolean mnemonics) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        
        GridBagConstraints constraints;
        
        filterLabel = new JLabel();
        setText(filterLabel, NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Query_Filter"), mnemonics); // NOI18N
        Dimension d = filterLabel.getPreferredSize();
        JRadioButton refRadion = new JRadioButton(NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Root_Classes")); // NOI18N
        refRadion.setBorder(filterLabel.getBorder());
        d.height = Math.max(d.height, refRadion.getPreferredSize().height);
        filterLabel.setPreferredSize(d);
        filterLabel.setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Query_Filter")); // NOI18N
        filterLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 5, 10);
        add(filterLabel, constraints);
        
        filterArea = createTextArea(2);
        filterLabel.setLabelFor(filterArea.getTextArea());
        filterArea.getTextArea().setToolTipText(NbBundle.getMessage(ProfilerCPUSettings.class, "TOOLTIP_Query_Filter")); // NOI18N
        filterArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { syncUI(); }
            public void removeUpdate(DocumentEvent e) { syncUI(); }
            public void changedUpdate(DocumentEvent e) { syncUI(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 0.65;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 10, 5, 10);
        add(filterArea, constraints);
        
        JLabel hintLabel = new JLabel(NbBundle.getMessage(ProfilerCPUSettings.class, "LBL_Query_Hint")) { // NOI18N
            {
                super.setEnabled(false);
            }
            public void setEnabled(boolean b) {
                super.setEnabled(false);
            }
        };
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 5, 10);
        add(hintLabel, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
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
        TextAreaComponent(JTextArea textArea, int vPolicy, int hPolicy) { super(textArea, vPolicy, hPolicy); }
        public JTextArea getTextArea() { return (JTextArea)getViewport().getView(); }
    }

}

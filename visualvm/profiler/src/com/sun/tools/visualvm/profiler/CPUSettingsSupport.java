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
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class CPUSettingsSupport {
    
    private static final String PROP_PREFIX = "ProfilerCPUSettings_"; // NOI18N
    
    static final String SNAPSHOT_VERSION = PROP_PREFIX + "version"; // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1"; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0"; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    public static final String PROP_ROOT_CLASSES = PROP_PREFIX + "root_classes"; // NOI18N
    public static final String PROP_PROFILE_RUNNABLES = PROP_PREFIX + "profile_runnables"; // NOI18N
    public static final String PROP_FILTER_TYPE = PROP_PREFIX + "filter_type"; // NOI18N
    public static final String PROP_FILTER_VALUE = PROP_PREFIX + "filter_value"; // NOI18N
    
    private JPanel panel;
    private JLabel rootClassesLabel;
    private TextAreaComponent rootsArea;
    private JCheckBox runnablesCheckBox;
    private JRadioButton inclFilterRadioButton;
    private JRadioButton exclFilterRadioButton;
    private TextAreaComponent filtersArea;
    private JButton resetDefaultsButton;
    
    private Application application;
    private String defaultRootClasses;
    private String defaultInstrFilter;
    
    
    public CPUSettingsSupport(Application application) {
        this.application = application;
        initDefaults();
    }
    
    public DataViewComponent.DetailsView getDetailsView() {
        return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Cpu_settings"), null, 10, new ScrollableContainer(getPanel()), null); // NOI18N
    }
    
    public void setUIEnabled(boolean enabled) {
        if (panel == null) return;
        
        panel.setEnabled(enabled);
        rootClassesLabel.setEnabled(enabled);
        rootsArea.setEnabled(enabled);
        rootsArea.getTextArea().setEnabled(enabled);
        runnablesCheckBox.setEnabled(enabled);
        inclFilterRadioButton.setEnabled(enabled);
        exclFilterRadioButton.setEnabled(enabled);
        filtersArea.getTextArea().setEnabled(enabled);
        resetDefaultsButton.setEnabled(enabled);
    }
    
    public ProfilingSettings getSettings() {
        if (panel == null) return null;
        
        ProfilingSettings settings = ProfilingSettingsPresets.createCPUPreset();
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
        
        String instrFilterString = getFilterValue();
        SimpleFilter instrFilter = (instrFilterString.isEmpty() || "*".equals(instrFilterString)) ? SimpleFilter.NO_FILTER : // NOI18N
            new SimpleFilter(instrFilterString, inclFilterRadioButton.isSelected() ?
            SimpleFilter.SIMPLE_FILTER_INCLUSIVE : SimpleFilter.SIMPLE_FILTER_EXCLUSIVE, instrFilterString);
        settings.setSelectedInstrumentationFilter(instrFilter);
        
        String[] rootValues = getRootValue().split(","); // NOI18N
        ClientUtils.SourceCodeSelection[] roots = (rootValues.length == 1 && rootValues[0].isEmpty()) ?
            new ClientUtils.SourceCodeSelection[0] :
            new ClientUtils.SourceCodeSelection[rootValues.length];
        for (int i = 0; i < roots.length; i++)
            roots[i] = new ClientUtils.SourceCodeSelection(rootValues[i], "*", null); // NOI18N
        settings.setInstrumentationRootMethods(roots);
        
        settings.setInstrumentSpawnedThreads(runnablesCheckBox.isSelected());
        
        return settings;
    }
    
    public void saveSettings() {
        if (application == null) return;
        Storage storage = application.getStorage();
        
        storage.setCustomProperty(SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);
        storage.setCustomProperty(PROP_ROOT_CLASSES, rootsArea.getTextArea().getText());
        storage.setCustomProperty(PROP_PROFILE_RUNNABLES, Boolean.toString(runnablesCheckBox.isSelected()));
        storage.setCustomProperty(PROP_FILTER_TYPE, Integer.toString(inclFilterRadioButton.isSelected() ?
            SimpleFilter.SIMPLE_FILTER_INCLUSIVE : SimpleFilter.SIMPLE_FILTER_EXCLUSIVE));
        storage.setCustomProperty(PROP_FILTER_VALUE, filtersArea.getTextArea().getText());
    }
    
    
    private void loadSettings() {
        if (application == null) return;
        Storage storage = application.getStorage();
        
        String rootClasses = storage.getCustomProperty(PROP_ROOT_CLASSES);
        if (rootClasses != null) rootsArea.getTextArea().setText(rootClasses);
        
        String profileRunnables = storage.getCustomProperty(PROP_PROFILE_RUNNABLES);
        if (profileRunnables != null) try {
            boolean profileRunnablesBool = Boolean.parseBoolean(profileRunnables);
            runnablesCheckBox.setSelected(profileRunnablesBool);
        } catch (Exception e) {}
        
        String filterType = storage.getCustomProperty(PROP_FILTER_TYPE);
        if (filterType != null) try {
            int filterTypeInt = Integer.parseInt(filterType);
            if (filterTypeInt == SimpleFilter.SIMPLE_FILTER_INCLUSIVE) inclFilterRadioButton.setSelected(true);
            else if (filterTypeInt == SimpleFilter.SIMPLE_FILTER_EXCLUSIVE) exclFilterRadioButton.setSelected(true);
        } catch (Exception e) {}
        
        String filterValue = storage.getCustomProperty(PROP_FILTER_VALUE);
        if (filterValue != null) filtersArea.getTextArea().setText(filterValue);
    }
    
    private void initDefaults() {
        defaultRootClasses = "";
        Jvm jvm = JvmFactory.getJVMFor(application);
        String mainClass = jvm.getMainClass();
        if (mainClass == null || mainClass.trim().length() == 0 || mainClass.endsWith(".jar")) mainClass = ""; // NOI18N
        int dotIndex = mainClass.lastIndexOf("."); // NOI18N
        if (dotIndex != -1) defaultRootClasses = mainClass.substring(0, dotIndex + 1) + "*"; // NOI18N
        
        if (defaultRootClasses.isEmpty()) {
            defaultInstrFilter = Utilities.isMac() ?
                "sun.*, sunw.*, com.sun.*,\ncom.apple.*, apple.awt.*" : "sun.*, sunw.*, com.sun.*"; // NOI18N
        } else {
            defaultInstrFilter = Utilities.isMac() ?
                "java.*, javax.*,\nsun.*, sunw.*, com.sun.*,\ncom.apple.*, apple.awt.*" : "java.*, javax.*,\nsun.*, sunw.*, com.sun.*"; // NOI18N
        }
    }
    
    private void setDefaults() {
        rootsArea.getTextArea().setText(defaultRootClasses);
        runnablesCheckBox.setSelected(true);
        exclFilterRadioButton.setSelected(true);
        filtersArea.getTextArea().setText(defaultInstrFilter);
    }
    
    private JPanel getPanel() {
        if (panel == null) {
            panel = createPanel();
            setDefaults();
            loadSettings();
        }
        return panel;
    }
    
    
    private void checkRootValidity() {
        rootsArea.getTextArea().setForeground(isRootValueValid() ?
            UIManager.getColor("TextArea.foreground") : Color.RED); // NOI18N
    }
    
    public boolean isRootValueValid() {
        String[] rootParts = FilterUtils.getSeparateFilters(getRootValue());

        for (int i = 0; i < rootParts.length; i++)
            if (!FilterUtils.isValidProfilerFilter(rootParts[i]))
                if (rootParts[i].endsWith("**")) { // NOI18N
                    if (!FilterUtils.isValidProfilerFilter(rootParts[i].substring(0, rootParts[i].length() - 1))) return false;
                } else {
                    return false;
                }

        return true;
    }
    
    private String getRootValue() {
        StringBuffer convertedValue = new StringBuffer();

        String[] rootValues = getRootsValues();

        for (int i = 0; i < rootValues.length; i++) {
            String filterValue = rootValues[i].trim();

            if ((i != (rootValues.length - 1)) && !filterValue.endsWith(",")) { // NOI18N
                filterValue = filterValue + ","; // NOI18N
            }

            convertedValue.append(filterValue);
        }

        return convertedValue.toString(); // NOI18N
    }
    
    private String[] getRootsValues() {
        return rootsArea.getTextArea().getText().split("\\n"); // NOI18N
    }
    
    private void checkFilterValidity() {
        filtersArea.getTextArea().setForeground(isFilterValueValid() ?
            UIManager.getColor("TextArea.foreground") : Color.RED); // NOI18N
    }
    
    public boolean isFilterValueValid() {
        String[] filterParts = FilterUtils.getSeparateFilters(getFilterValue());

        for (int i = 0; i < filterParts.length; i++)
            if (!FilterUtils.isValidProfilerFilter(filterParts[i])) return false;

        return true;
    }
    
    private String getFilterValue() {
        StringBuffer convertedValue = new StringBuffer();

        String[] filterValues = getFilterValues();

        for (int i = 0; i < filterValues.length; i++) {
            String filterValue = filterValues[i].trim();

            if ((i != (filterValues.length - 1)) && !filterValue.endsWith(",")) { // NOI18N
                filterValue = filterValue + ", "; // NOI18N
            }

            convertedValue.append(filterValue);
        }

        return convertedValue.toString();
    }

    private String[] getFilterValues() {
        return filtersArea.getTextArea().getText().split("\\n"); // NOI18N
    }
    
    private JPanel createPanel() {
        JPanel panelImpl = new JPanel();
        panelImpl.setLayout(new GridBagLayout());
        panelImpl.setOpaque(false);
        
        ButtonGroup filterRadiosGroup = new ButtonGroup();
        GridBagConstraints constraints;
        
        rootClassesLabel = new JLabel(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Root_Classes")); // NOI18N
        rootClassesLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(10, 10, 5, 10);
        panelImpl.add(rootClassesLabel, constraints);
        
        rootsArea = createTextArea(3);
        rootsArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkRootValidity(); }
            public void removeUpdate(DocumentEvent e) { checkRootValidity(); }
            public void changedUpdate(DocumentEvent e) { checkRootValidity(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 0.65;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 10, 7, 10);
        panelImpl.add(rootsArea, constraints);
        
        runnablesCheckBox = new JCheckBox(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profile_Runnables")); // NOI18N
        runnablesCheckBox.setOpaque(false);
        runnablesCheckBox.setBorder(rootClassesLabel.getBorder());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 10, 10);
        panelImpl.add(runnablesCheckBox, constraints);
        
        inclFilterRadioButton = new JRadioButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profile_Incl")); // NOI18N
        inclFilterRadioButton.setOpaque(false);
        inclFilterRadioButton.setBorder(rootClassesLabel.getBorder());
        filterRadiosGroup.add(inclFilterRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 10, 5, 5);
        panelImpl.add(inclFilterRadioButton, constraints);
        
        exclFilterRadioButton = new JRadioButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Profile_Excl")); // NOI18N
        exclFilterRadioButton.setOpaque(false);
        exclFilterRadioButton.setBorder(rootClassesLabel.getBorder());
        filterRadiosGroup.add(exclFilterRadioButton);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 5, 5, 10);
        panelImpl.add(exclFilterRadioButton, constraints);
        
        filtersArea = createTextArea(2);
        filtersArea.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkFilterValidity(); }
            public void removeUpdate(DocumentEvent e) { checkFilterValidity(); }
            public void changedUpdate(DocumentEvent e) { checkFilterValidity(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weighty = 0.35;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 10, 10, 10);
        panelImpl.add(filtersArea, constraints);
        
        resetDefaultsButton = new JButton(NbBundle.getMessage(ApplicationProfilerView.class, "LBL_Restore_Defaults")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) { setDefaults(); }
        };
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 5, 5, 10);
        panelImpl.add(resetDefaultsButton, constraints);
        
        return panelImpl;
    }
    
    private static TextAreaComponent createTextArea(int rows) {
        JTextArea rootsArea = new JTextArea();
        rootsArea.setFont(new Font("Monospaced", Font.PLAIN, UIManager.getFont("Label.font").getSize())); // NOI18N
        TextAreaComponent rootsAreaScrollPane = new TextAreaComponent(rootsArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
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

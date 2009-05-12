/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.ui.stp;

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.ui.components.JExtendedRadioButton;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class NewCustomConfiguration extends JPanel implements ChangeListener, ListSelectionListener, DocumentListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MONITOR_STRING = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                     "NewCustomConfiguration_MonitorString"); // NOI18N
    private static final String CPU_STRING = NbBundle.getMessage(NewCustomConfiguration.class, "NewCustomConfiguration_CpuString"); // NOI18N
    private static final String MEMORY_STRING = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                    "NewCustomConfiguration_MemoryString"); // NOI18N
    private static final String NEW_CONFIG_DIALOG_CAPTION = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                                "NewCustomConfiguration_NewConfigDialogCaption"); // NOI18N
    private static final String DUPLICATE_CONFIG_DIALOG_CAPTION = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                                      "NewCustomConfiguration_DuplicateConfigDialogCaption"); // NOI18N
    private static final String RENAME_CONFIG_DIALOG_CAPTION = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                                   "NewCustomConfiguration_RenameConfigDialogCaption"); // NOI18N
    private static final String NEW_CONFIG_STRING = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                        "NewCustomConfiguration_NewConfigString"); // NOI18N
    private static final String NEW_MONITOR_STRING = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                         "NewCustomConfiguration_NewMonitorString"); // NOI18N
    private static final String NEW_CPU_STRING = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                     "NewCustomConfiguration_NewCpuString"); // NOI18N
    private static final String NEW_MEMORY_STRING = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                        "NewCustomConfiguration_NewMemoryString"); // NOI18N
    private static final String TYPE_LABEL_TEXT = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                      "NewCustomConfiguration_TypeLabelText"); // NOI18N
    private static final String NAME_LABEL_TEXT = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                      "NewCustomConfiguration_NameLabelText"); // NOI18N
    private static final String NAME_LABEL_ACCESS_DESCR = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                      "NewCustomConfiguration_NameLabelAccessDescr"); // NOI18N
    private static final String INIT_SETTINGS_LABEL_TEXT = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                               "NewCustomConfiguration_InitSettingsLabelText"); // NOI18N
    private static final String DEFAULT_RADIO_TEXT = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                         "NewCustomConfiguration_DefaultRadioText"); // NOI18N
    private static final String DEFAULT_RADIO_ACCESS_DESCR = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                         "NewCustomConfiguration_DefaultRadioAccessDescr"); // NOI18N
    private static final String EXISTING_RADIO_TEXT = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                          "NewCustomConfiguration_ExistingRadioText"); // NOI18N
    private static final String EXISTING_RADIO_ACCESS_DESCR = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                          "NewCustomConfiguration_ExistingRadioAccessDescr"); // NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(NewCustomConfiguration.class,
                                                                     "NewCustomConfiguration_OkButtonText"); // NOI18N
                                                                                                             // -----

    // --- Constants declaration -------------------------------------------------
    private static final int MODE_NEW_ANY = 0;
    private static final int MODE_NEW_TYPE = 1;
    private static final int MODE_DUPLICATE = 2;
    private static final int MODE_RENAME = 4;

    // --- Instance variables declaration ----------------------------------------
    private static NewCustomConfiguration defaultInstance;

    // --- UI components declaration ---------------------------------------------
    private static final Icon ICON_MONITOR = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/telemetryWindow.png", false); // NOI18N
    private static final Icon ICON_CPU = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/cpu.png", false); // NOI18N
    private static final Icon ICON_MEMORY = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/memory.png", false); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DefaultListModel existingSettingsListModel;
    private JButton okButton;
    private JLabel nameLabel;
    private JLabel settingsLabel;
    private JLabel typeLabel;
    private JList existingSettingsList;
    private JPanel bottomRenameSpacer;
    private JRadioButton cpuTypeRadio;
    private JRadioButton defaultSettingsRadio;
    private JRadioButton existingSettingsRadio;
    private JRadioButton memoryTypeRadio;
    private JRadioButton monitorTypeRadio;
    private JScrollPane existingSettingsScrollPane;
    private JTextField nameTextfield;
    private ProfilingSettings originalSettings = null;
    private ProfilingSettings[] availableSettings;
    private int mode;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Private implementation ------------------------------------------------
    private NewCustomConfiguration() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ProfilingSettings createDuplicateConfiguration(ProfilingSettings originalConfiguration,
                                                                 ProfilingSettings[] availableConfigurations) {
        NewCustomConfiguration ncc = getDefault();
        ncc.setupDuplicateConfiguration(originalConfiguration, availableConfigurations);

        final DialogDescriptor dd = new DialogDescriptor(ncc,
                                                         MessageFormat.format(DUPLICATE_CONFIG_DIALOG_CAPTION,
                                                                              new Object[] { originalConfiguration.getSettingsName() }));
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack();
        d.setVisible(true);

        ProfilingSettings newSettings = null;

        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            newSettings = ncc.getProfilingSettings();
        }

        return newSettings;
    }

    // --- Public interface ------------------------------------------------------
    public static ProfilingSettings createNewConfiguration(ProfilingSettings[] availableConfigurations) {
        NewCustomConfiguration ncc = getDefault();
        ncc.setupUniversalConfiguration(availableConfigurations);

        final DialogDescriptor dd = new DialogDescriptor(ncc, NEW_CONFIG_DIALOG_CAPTION);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack();
        d.setVisible(true);

        ProfilingSettings newSettings = null;

        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            newSettings = ncc.getProfilingSettings();
        }

        return newSettings;
    }

    public static ProfilingSettings createNewConfiguration(int type, ProfilingSettings[] availableConfigurations) { // Use ProfilingSettings.getProfilingType() value

        NewCustomConfiguration ncc = getDefault();
        ncc.setupTypeConfiguration(type, availableConfigurations);

        String typeString = ""; // NOI18N

        if (Utils.isMonitorSettings(type)) {
            typeString = " (" + MONITOR_STRING + ")"; // NOI18N
        } else if (Utils.isCPUSettings(type)) {
            typeString = " (" + CPU_STRING + ")"; // NOI18N
        } else if (Utils.isMemorySettings(type)) {
            typeString = " (" + MEMORY_STRING + ")"; // NOI18N
        }
        
        // Remove mnemonics wildcard
        typeString = typeString.replace("&", ""); // NOI18N

        final DialogDescriptor dd = new DialogDescriptor(ncc, NEW_CONFIG_DIALOG_CAPTION + typeString, true,
                                                         new Object[] { ncc.okButton, DialogDescriptor.CANCEL_OPTION },
                                                         ncc.okButton, 0, null, null);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack();
        d.setVisible(true);

        ProfilingSettings newSettings = null;

        if (dd.getValue() == ncc.okButton) {
            newSettings = ncc.getProfilingSettings();
        }

        return newSettings;
    }

    public static ProfilingSettings renameConfiguration(ProfilingSettings originalConfiguration,
                                                        ProfilingSettings[] availableConfigurations) {
        NewCustomConfiguration ncc = getDefault();
        ncc.setupRenameConfiguration(originalConfiguration, availableConfigurations);

        final DialogDescriptor dd = new DialogDescriptor(ncc,
                                                         MessageFormat.format(RENAME_CONFIG_DIALOG_CAPTION,
                                                                              new Object[] { originalConfiguration.getSettingsName() }));
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack();
        d.setVisible(true);

        ProfilingSettings newSettings = null;

        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            newSettings = ncc.getProfilingSettings();
        }

        return newSettings;
    }

    public void changedUpdate(DocumentEvent e) {
        updateOKButton();
    }

    public void insertUpdate(DocumentEvent e) {
        updateOKButton();
    }

    // --- Static tester frame ---------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
            //      UIManager.setLookAndFeel("plaf.metal.MetalLookAndFeel"); //NOI18N
            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
        } catch (Exception e) {
        }

        ;

        //    NewCustomConfiguration.getDefault().createNewConfiguration();

        //    JFrame frame = new JFrame("Tester Frame");
        //    JPanel contents = new NewCustomConfiguration();
        ////    contents.setPreferredSize(new Dimension(375, 255));
        //    frame.getContentPane().add(contents);
        //    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //    frame.pack();
        //    frame.setVisible(true);
    }

    public void removeUpdate(DocumentEvent e) {
        updateOKButton();
    }

    // --- ChangeListener & ListSelectionListener & DocumentListner implementation
    public void stateChanged(ChangeEvent e) {
        existingSettingsList.setEnabled(existingSettingsRadio.isSelected());

        if (existingSettingsRadio.isEnabled() && defaultSettingsRadio.isSelected()) {
            existingSettingsList.clearSelection();
        }

        updateOKButton();
    }

    public void valueChanged(ListSelectionEvent e) {
        updateOKButton();
    }

    private static NewCustomConfiguration getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new NewCustomConfiguration();
        }

        return defaultInstance;
    }

    private ProfilingSettings getProfilingSettings() {
        ProfilingSettings newSettings = null;

        if (mode == MODE_RENAME) {
            // rename settings
            newSettings = originalSettings;
        } else if ((mode == MODE_DUPLICATE)
                       || (((mode == MODE_NEW_ANY) || (mode == MODE_NEW_TYPE)) && existingSettingsRadio.isSelected())) {
            // duplicate settings (new based on existing or duplicate)
            newSettings = new ProfilingSettings();
            availableSettings[existingSettingsList.getSelectedIndex()].copySettingsInto(newSettings);
        } else {
            // new default settings
            if (monitorTypeRadio.isSelected()) {
                newSettings = ProfilingSettingsPresets.createMonitorPreset();
                newSettings.setIsPreset(false);
            } else if (cpuTypeRadio.isSelected()) {
                newSettings = ProfilingSettingsPresets.createCPUPreset();
                newSettings.setIsPreset(false);
            } else if (memoryTypeRadio.isSelected()) {
                newSettings = ProfilingSettingsPresets.createMemoryPreset();
                newSettings.setIsPreset(false);
            }
        }

        newSettings.setSettingsName(nameTextfield.getText().trim());

        return newSettings;
    }

    private String createSettingsName(ProfilingSettings[] availableConfigurations) {
        String nameBasis = NEW_CONFIG_STRING;

        if (monitorTypeRadio.isSelected()) {
            nameBasis = NEW_MONITOR_STRING;
        } else if (cpuTypeRadio.isSelected()) {
            nameBasis = NEW_CPU_STRING;
        } else if (memoryTypeRadio.isSelected()) {
            nameBasis = NEW_MEMORY_STRING;
        }

        List<String> configurationsNames = new ArrayList(availableConfigurations.length);

        for (ProfilingSettings settings : availableConfigurations) {
            configurationsNames.add(settings.getSettingsName());
        }

        int index = 0;
        String indexStr = ""; // NOI18N

        while (configurationsNames.contains(nameBasis + indexStr)) {
            indexStr = " " + Integer.toString(++index); // NOI18N
        }

        return nameBasis + indexStr;
    }
    
    private void showTypeSettings() {
        typeLabel.setVisible(true);
        monitorTypeRadio.setVisible(true);
        cpuTypeRadio.setVisible(true);
        memoryTypeRadio.setVisible(true);
    }
    
    private void hideTypeSettings() {
        typeLabel.setVisible(false);
        monitorTypeRadio.setVisible(false);
        cpuTypeRadio.setVisible(false);
        memoryTypeRadio.setVisible(false);
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;
        ButtonGroup typeRadiosGroup = new ButtonGroup();
        ButtonGroup settingsRadiosGroup = new ButtonGroup();

        // typeLabel
        typeLabel = new JLabel(TYPE_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 10, 0, 0);
        add(typeLabel, constraints);

        // monitorTypeRadio
        monitorTypeRadio = new JExtendedRadioButton(MONITOR_STRING, ICON_MONITOR);
        org.openide.awt.Mnemonics.setLocalizedText(monitorTypeRadio, MONITOR_STRING);
        typeRadiosGroup.add(monitorTypeRadio);
        monitorTypeRadio.setSelected(true);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 5, 0, 0);
        add(monitorTypeRadio, constraints);

        // cpuTypeRadio
        cpuTypeRadio = new JExtendedRadioButton(CPU_STRING, ICON_CPU);
        org.openide.awt.Mnemonics.setLocalizedText(cpuTypeRadio, CPU_STRING);
        typeRadiosGroup.add(cpuTypeRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 5, 0, 0);
        add(cpuTypeRadio, constraints);

        // memoryTypeRadio
        memoryTypeRadio = new JExtendedRadioButton(MEMORY_STRING, ICON_MEMORY);
        org.openide.awt.Mnemonics.setLocalizedText(memoryTypeRadio, MEMORY_STRING);
        typeRadiosGroup.add(memoryTypeRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 5, 0, 10);
        add(memoryTypeRadio, constraints);

        // nameLabel
        nameLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, NAME_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 10, 0, 0);
        add(nameLabel, constraints);

        // nameTextfield
        nameTextfield = new JTextField();
        nameTextfield.getDocument().addDocumentListener(this);
        nameTextfield.setPreferredSize(new Dimension(250, nameTextfield.getPreferredSize().height));
        nameTextfield.getAccessibleContext().setAccessibleDescription(NAME_LABEL_ACCESS_DESCR);
        nameLabel.setLabelFor(nameTextfield);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 5, 0, 10);
        add(nameTextfield, constraints);

        // settingsLabel
        settingsLabel = new JLabel(INIT_SETTINGS_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 10, 0, 10);
        add(settingsLabel, constraints);

        // defaultSettingsRadio
        defaultSettingsRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(defaultSettingsRadio, DEFAULT_RADIO_TEXT);
        settingsRadiosGroup.add(defaultSettingsRadio);
        defaultSettingsRadio.getAccessibleContext().setAccessibleDescription(DEFAULT_RADIO_ACCESS_DESCR);
        defaultSettingsRadio.addChangeListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 10);
        add(defaultSettingsRadio, constraints);

        // existingSettingsRadio
        existingSettingsRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(existingSettingsRadio, EXISTING_RADIO_TEXT);
        settingsRadiosGroup.add(existingSettingsRadio);
        existingSettingsRadio.getAccessibleContext().setAccessibleDescription(EXISTING_RADIO_ACCESS_DESCR);
        existingSettingsRadio.setSelected(true);
        existingSettingsRadio.addChangeListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(1, 5, 0, 10);
        add(existingSettingsRadio, constraints);

        // existingSettingsList
        existingSettingsListModel = new DefaultListModel();
        existingSettingsList = new JList(existingSettingsListModel);
        existingSettingsList.setVisibleRowCount(5);
        existingSettingsList.addListSelectionListener(this);

        // existingSettingsScrollPane
        existingSettingsScrollPane = new JScrollPane(existingSettingsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(4, 5, 15, 10);
        add(existingSettingsScrollPane, constraints);

        // bottomRenameSpacer
        bottomRenameSpacer = Utils.createFillerPanel();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 15, 0);
        add(bottomRenameSpacer, constraints);

        // okButton
        okButton = new JButton(OK_BUTTON_TEXT);

        // UI tweaks
        addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if (((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) && isShowing()) {
                        nameTextfield.requestFocusInWindow();
                        nameTextfield.selectAll();
                    }
                }
            });
    }

    private void setupDuplicateConfiguration(ProfilingSettings originalConfiguration, ProfilingSettings[] availableConfigurations) {
        mode = MODE_DUPLICATE;
        originalSettings = originalConfiguration;
        availableSettings = availableConfigurations;

        monitorTypeRadio.setEnabled(Utils.isMonitorSettings(originalSettings));
        monitorTypeRadio.setSelected(monitorTypeRadio.isEnabled());
        cpuTypeRadio.setEnabled(Utils.isCPUSettings(originalSettings));
        cpuTypeRadio.setSelected(cpuTypeRadio.isEnabled());
        memoryTypeRadio.setEnabled(Utils.isMemorySettings(originalSettings));
        memoryTypeRadio.setSelected(memoryTypeRadio.isEnabled());
        hideTypeSettings();

        settingsLabel.setVisible(true);
        defaultSettingsRadio.setVisible(true);
        existingSettingsRadio.setVisible(true);
        existingSettingsScrollPane.setVisible(true);
        bottomRenameSpacer.setVisible(false);

        defaultSettingsRadio.setEnabled(false);
        existingSettingsRadio.setEnabled(false);
        existingSettingsRadio.setSelected(true);
        existingSettingsList.setEnabled(false);

        nameTextfield.setText(createSettingsName(availableConfigurations));
        updateAvailableSettings();

        for (int i = 0; i < availableConfigurations.length; i++) {
            if (originalSettings == availableConfigurations[i]) {
                existingSettingsList.setSelectedIndex(i);

                return;
            }
        }

        updateOKButton();
    }

    private void setupRenameConfiguration(ProfilingSettings originalConfiguration, ProfilingSettings[] availableConfigurations) {
        mode = MODE_RENAME;
        originalSettings = originalConfiguration;
        availableSettings = availableConfigurations;

        monitorTypeRadio.setEnabled(Utils.isMonitorSettings(originalSettings));
        monitorTypeRadio.setSelected(monitorTypeRadio.isEnabled());
        cpuTypeRadio.setEnabled(Utils.isCPUSettings(originalSettings));
        cpuTypeRadio.setSelected(cpuTypeRadio.isEnabled());
        memoryTypeRadio.setEnabled(Utils.isMemorySettings(originalSettings));
        memoryTypeRadio.setSelected(memoryTypeRadio.isEnabled());
        hideTypeSettings();

        settingsLabel.setVisible(false);
        defaultSettingsRadio.setVisible(false);
        existingSettingsRadio.setVisible(false);
        existingSettingsScrollPane.setVisible(false);
        bottomRenameSpacer.setVisible(true);

        nameTextfield.setText(originalConfiguration.getSettingsName());
        updateAvailableSettings();
        updateOKButton();
    }

    private void setupTypeConfiguration(int type, ProfilingSettings[] availableConfigurations) {
        mode = MODE_NEW_TYPE;
        originalSettings = null;
        availableSettings = availableConfigurations;

        monitorTypeRadio.setEnabled(Utils.isMonitorSettings(type));
        monitorTypeRadio.setSelected(monitorTypeRadio.isEnabled());
        cpuTypeRadio.setEnabled(Utils.isCPUSettings(type));
        cpuTypeRadio.setSelected(cpuTypeRadio.isEnabled());
        memoryTypeRadio.setEnabled(Utils.isMemorySettings(type));
        memoryTypeRadio.setSelected(memoryTypeRadio.isEnabled());
        hideTypeSettings();

        settingsLabel.setVisible(true);
        defaultSettingsRadio.setVisible(true);
        existingSettingsRadio.setVisible(true);
        existingSettingsScrollPane.setVisible(true);
        bottomRenameSpacer.setVisible(false);

        defaultSettingsRadio.setEnabled(true);
        defaultSettingsRadio.setSelected(true);
        existingSettingsRadio.setEnabled(true);

        nameTextfield.setText(createSettingsName(availableConfigurations));
        updateAvailableSettings();
        updateOKButton();
    }

    private void setupUniversalConfiguration(ProfilingSettings[] availableConfigurations) {
        mode = MODE_NEW_ANY;
        originalSettings = null;
        availableSettings = availableConfigurations;

        monitorTypeRadio.setEnabled(true);
        cpuTypeRadio.setEnabled(true);
        cpuTypeRadio.setSelected(true);
        memoryTypeRadio.setEnabled(true);
        showTypeSettings();

        settingsLabel.setVisible(true);
        defaultSettingsRadio.setVisible(true);
        existingSettingsRadio.setVisible(true);
        existingSettingsScrollPane.setVisible(true);
        bottomRenameSpacer.setVisible(false);

        defaultSettingsRadio.setEnabled(true);
        defaultSettingsRadio.setSelected(true);
        existingSettingsRadio.setEnabled(true);

        nameTextfield.setText(createSettingsName(availableConfigurations));
        updateAvailableSettings();
        updateOKButton();
    }

    private void updateAvailableSettings() {
        existingSettingsListModel.removeAllElements();

        for (ProfilingSettings settings : availableSettings) {
            existingSettingsListModel.addElement(settings.getSettingsName());
        }
    }

    private void updateOKButton() {
        okButton.setEnabled((nameTextfield.getText().trim().length() > 0)
                            && (defaultSettingsRadio.isSelected() || (existingSettingsList.getSelectedIndex() != -1)));
    }
}

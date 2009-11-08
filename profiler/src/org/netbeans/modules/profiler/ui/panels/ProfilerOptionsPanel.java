/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ui.panels;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.PlatformsCustomizer;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.actions.JavaPlatformSelector;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;


/**
 * A panel used to edit the global settings of the profiler.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public final class ProfilerOptionsPanel extends JPanel implements ActionListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class CategorySeparator extends JPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JLabel captionLabel;
        private JSeparator captionSeparator;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CategorySeparator(String caption) {
            initComponents();
            captionLabel.setText(caption);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void initComponents() {
            setLayout(new GridBagLayout());

            GridBagConstraints constraints;

            // captionLabel
            captionLabel = new JLabel();
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(captionLabel, constraints);

            // captionSeparator
            captionSeparator = new JSeparator();
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(0, 4, 0, 0);
            add(captionSeparator, constraints);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String USE_PROJECT_JVM_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                           "ProfilerOptionsPanel_UseProjectJvmText"); //NOI18N
    private static final String KEY_OPEN_ALWAYS = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                      "ProfilerOptionsPanel_KeyOpenAlways"); //NOI18N
    private static final String KEY_OPEN_MONITORING = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                          "ProfilerOptionsPanel_KeyOpenMonitoring"); //NOI18N
    private static final String KEY_OPEN_NEVER = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                     "ProfilerOptionsPanel_KeyOpenNever"); //NOI18N
    private static final String ENGINE_SETTINGS_BORDER_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                  "ProfilerOptionsPanel_EngineSettingsBorderText"); //NOI18N
    private static final String JAVA_PLATFORM_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                               "ProfilerOptionsPanel_JavaPlatformLabelText"); //NOI18N
    private static final String MANAGE_PLATFORMS_BUTTON_NAME = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                   "ProfilerOptionsPanel_ManagePlatformsButtonName"); //NOI18N
    private static final String COMM_PORT_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                           "ProfilerOptionsPanel_CommPortLabelText"); //NOI18N
    private static final String WINDOWS_SETTINGS_BORDER_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                   "ProfilerOptionsPanel_WindowsSettingsBorderText"); //NOI18N
    private static final String TELEMETRY_OVERVIEW_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                    "ProfilerOptionsPanel_TelemetryOverviewLabelText"); //NOI18N
    private static final String THREADS_VIEW_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                              "ProfilerOptionsPanel_ThreadsViewLabelText"); //NOI18N
    private static final String THREADS_VIEW_HINT_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                             "ProfilerOptionsPanel_ThreadsViewHintText"); //NOI18N
    private static final String LIVE_RESULTS_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                              "ProfilerOptionsPanel_LiveResultsLabelText"); //NOI18N
    private static final String CPU_CHCKBOX_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                       "ProfilerOptionsPanel_CpuChckBoxText"); //NOI18N
    private static final String MEMORY_CHCKBOX_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                          "ProfilerOptionsPanel_MemoryChckBoxText"); //NOI18N
    private static final String SNAPSHOTS_SETTINGS_BORDER_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                     "ProfilerOptionsPanel_SnapshotsSettingsBorderText"); //NOI18N
    private static final String OPEN_SNAPSHOT_RADIO_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                               "ProfilerOptionsPanel_OpenSnapshotRadioText"); //NOI18N
    private static final String SAVE_SNAPSHOT_RADIO_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                               "ProfilerOptionsPanel_SaveSnapshotRadioText"); //NOI18N
    private static final String OPEN_SAVE_SNAPSHOT_RADIO_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                    "ProfilerOptionsPanel_OpenSaveSnapshotRadioText"); //NOI18N
    private static final String RESET_HINT_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                      "ProfilerOptionsPanel_ResetHintText"); //NOI18N
    private static final String RESET_BUTTON_NAME = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                        "ProfilerOptionsPanel_ResetButtonName"); //NOI18N
    private static final String PORT_NO_SPINNER_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                   "ProfilerOptionsPanel_PortNoSpinnerAccessDescr"); //NOI18N
    private static final String CPU_LIVE_RESULTS_CHECKBOX_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                             "ProfilerOptionsPanel_CpuLiveResultsCheckboxAccessDescr"); //NOI18N
    private static final String MEMORY_LIVE_RESULTS_CHECKBOX_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                                "ProfilerOptionsPanel_MemoryLiveResultsCheckboxAccessDescr"); //NOI18N
    private static final String TELEMETRY_OVERVIEW_COMBO_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                            "ProfilerOptionsPanel_TelemetryOverviewComboAccessDescr"); //NOI18N
    private static final String THREADS_VIEW_COMBO_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                      "ProfilerOptionsPanel_ThreadsViewComboAccessDescr"); //NOI18N  
    private static final String OOME_BORDER_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                       "ProfilerOptionsPanel_OomeBorderText"); //NOI18N  
    private static final String OOME_NOTHING_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                        "ProfilerOptionsPanel_OomeNothingText"); //NOI18N  
    private static final String OOME_PROJECT_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                        "ProfilerOptionsPanel_OomeProjectText"); //NOI18N  
    private static final String OOME_CUSTOM_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                       "ProfilerOptionsPanel_OomeCustomText"); //NOI18N  
    private static final String OOME_TEMP_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                     "ProfilerOptionsPanel_OomeTempText"); //NOI18N  
    private static final String OOME_CUSTOM_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                               "ProfilerOptionsPanel_OomeCustomAccessDescr"); //NOI18N 
    private static final String OOME_CUSTOM_TEXTFIELD_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                         "ProfilerOptionsPanel_OomeCustomTextfieldAccessDescr"); //NOI18N 
    private static final String OOME_CUSTOM_BUTTON_ACCESS_NAME = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                     "ProfilerOptionsPanel_OomeCustomButtonAccessName"); //NOI18N 
    private static final String CHOOSE_DUMPDIR_CAPTION = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                             "ProfilerOptionsPanel_ChooseDumpDirCaption"); //NOI18N 
    private static final String ENABLE_ANALYSIS_CHECKBOX = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                               "ProfilerOptionsPanel_EnableAnalysisCheckbox"); //NOI18N 
    private static final String TAKING_SNAPSHOT_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                 "ProfilerOptionsPanel_TakingSnapshotLabelText"); //NOI18N 
    private static final String TAKING_SNAPSHOT_COMBO_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                         "ProfilerOptionsPanel_TakingSnapshotComboAccessDescr"); //NOI18N 
    private static final String OOME_COMBO_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                              "ProfilerOptionsPanel_OomeComboAccessDescr"); //NOI18N 
    private static final String HEAPWALKER_LABEL_TEXT = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                            "ProfilerOptionsPanel_HeapWalkerLabelText"); //NOI18N 
    private static final String JAVA_PLAFORM_COMBO_ACCESS_DESCR = NbBundle.getMessage(ProfilerOptionsPanel.class,
                                                                                      "ProfilerOptionsPanel_JavaPlatformComboAccessDescr"); //NOI18N 
                                                                                                                                            // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList supportedJavaPlatforms = new ArrayList();
    private ArrayList supportedJavaPlatformsNames = new ArrayList();
    private JButton managePlatformsButton;
    private JButton oomeDetectionChooseDirButton;
    private JButton resetConfirmationsButton;
    private JCheckBox cpuLiveResultsCheckbox;
    private JCheckBox enableHeapWalkerAnalysisCheckbox;
    private JCheckBox memoryLiveResultsCheckbox;
    private JComboBox javaPlatformCombo;
    private JComboBox oomeCombo;
    private JComboBox openThreadsViewCombo;
    private JComboBox takingSnapshotCombo;
    private JComboBox telemetryOverviewCombo;
    private JExtendedSpinner portNoSpinner;
    private JTextField oomeDetectionDirTextField;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProfilerOptionsPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JavaPlatform getSelectedJavaPlatform() {
        int selectedJavaPlatformIndex = javaPlatformCombo.getSelectedIndex();

        if ((selectedJavaPlatformIndex == -1) || (selectedJavaPlatformIndex == 0)) {
            return null; // not selected, or <use project> selected
        }

        selectedJavaPlatformIndex--;

        return (JavaPlatform) supportedJavaPlatforms.get(selectedJavaPlatformIndex);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == resetConfirmationsButton) {
            ProfilerIDESettings.getInstance().clearDoNotShowAgainMap();
            resetConfirmationsButton.setEnabled(false);
        } else if (e.getSource() == managePlatformsButton) {
            JavaPlatform platform = getSelectedJavaPlatform();
            PlatformsCustomizer.showCustomizer(platform);
            updateJavaPlatformComboItems();
        } else if (e.getSource() == oomeDetectionChooseDirButton) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new java.io.File(oomeDetectionDirTextField.getText()));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setDialogTitle(CHOOSE_DUMPDIR_CAPTION);

            if (chooser.showOpenDialog(SwingUtilities.getRoot(this)) == JFileChooser.APPROVE_OPTION) {
                oomeDetectionDirTextField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    public void applySettings(ProfilerIDESettings pis) {
        // GlobalProfilingSettings
        pis.setPortNo(((Number) portNoSpinner.getValue()).intValue());

        JavaPlatform sel = getSelectedJavaPlatform();

        pis.setJavaPlatformForProfiling((sel == null) ? null : sel.getDisplayName());

        // ProfilerIDESettings
        pis.setDisplayLiveResultsCPU(cpuLiveResultsCheckbox.isSelected());
        pis.setDisplayLiveResultsMemory(memoryLiveResultsCheckbox.isSelected());

        Object takingSnapshotSelected = takingSnapshotCombo.getSelectedItem();
        pis.setAutoOpenSnapshot((takingSnapshotSelected == OPEN_SNAPSHOT_RADIO_TEXT)
                                || (takingSnapshotSelected == OPEN_SAVE_SNAPSHOT_RADIO_TEXT));
        pis.setAutoSaveSnapshot((takingSnapshotSelected == SAVE_SNAPSHOT_RADIO_TEXT)
                                || (takingSnapshotSelected == OPEN_SAVE_SNAPSHOT_RADIO_TEXT));

        Object oomeSelected = oomeCombo.getSelectedItem();

        if (oomeSelected == OOME_NOTHING_TEXT) {
            pis.setOOMDetectionMode(pis.OOME_DETECTION_NONE);
        } else if (oomeSelected == OOME_PROJECT_TEXT) {
            pis.setOOMDetectionMode(pis.OOME_DETECTION_PROJECTDIR);
        } else if (oomeSelected == OOME_TEMP_TEXT) {
            pis.setOOMDetectionMode(pis.OOME_DETECTION_TEMPDIR);
        } else if (oomeSelected == OOME_CUSTOM_TEXT) {
            pis.setOOMDetectionMode(pis.OOME_DETECTION_CUSTOMDIR);
        }

        pis.setCustomHeapdumpPath(oomeDetectionDirTextField.getText());

        pis.setHeapWalkerAnalysisEnabled(enableHeapWalkerAnalysisCheckbox.isSelected());

        if (telemetryOverviewCombo.getSelectedItem() == KEY_OPEN_ALWAYS) {
            pis.setTelemetryOverviewBehavior(ProfilerIDESettings.OPEN_ALWAYS);
        } else if (telemetryOverviewCombo.getSelectedItem() == KEY_OPEN_MONITORING) {
            pis.setTelemetryOverviewBehavior(ProfilerIDESettings.OPEN_MONITORING);
        } else {
            pis.setTelemetryOverviewBehavior(ProfilerIDESettings.OPEN_NEVER);
        }

        if (openThreadsViewCombo.getSelectedItem() == KEY_OPEN_ALWAYS) {
            pis.setThreadsViewBehavior(ProfilerIDESettings.OPEN_ALWAYS);
        } else if (openThreadsViewCombo.getSelectedItem() == KEY_OPEN_MONITORING) {
            pis.setThreadsViewBehavior(ProfilerIDESettings.OPEN_MONITORING);
        } else {
            pis.setThreadsViewBehavior(ProfilerIDESettings.OPEN_NEVER);
        }
    }

    public boolean currentSettingsEquals(ProfilerIDESettings settings) {
        if (((Number) portNoSpinner.getValue()).intValue() != settings.getPortNo()) {
            return false;
        }

        if (cpuLiveResultsCheckbox.isSelected() != settings.getDisplayLiveResultsCPU()) {
            return false;
        }

        if (memoryLiveResultsCheckbox.isSelected() != settings.getDisplayLiveResultsMemory()) {
            return false;
        }

        if (settings.getAutoOpenSnapshot() && settings.getAutoSaveSnapshot()
                && (takingSnapshotCombo.getSelectedItem() != OPEN_SAVE_SNAPSHOT_RADIO_TEXT)) {
            return false;
        }

        if (settings.getAutoOpenSnapshot() && (takingSnapshotCombo.getSelectedItem() != OPEN_SNAPSHOT_RADIO_TEXT)) {
            return false;
        }

        if (settings.getAutoSaveSnapshot() && (takingSnapshotCombo.getSelectedItem() != SAVE_SNAPSHOT_RADIO_TEXT)) {
            return false;
        }

        if ((settings.getOOMDetectionMode() == settings.OOME_DETECTION_NONE)
                && (oomeCombo.getSelectedItem() != OOME_NOTHING_TEXT)) {
            return false;
        }

        if ((settings.getOOMDetectionMode() == settings.OOME_DETECTION_PROJECTDIR)
                && (oomeCombo.getSelectedItem() != OOME_PROJECT_TEXT)) {
            return false;
        }

        if ((settings.getOOMDetectionMode() == settings.OOME_DETECTION_TEMPDIR)
                && (oomeCombo.getSelectedItem() != OOME_TEMP_TEXT)) {
            return false;
        }

        if ((settings.getOOMDetectionMode() == settings.OOME_DETECTION_CUSTOMDIR)
                && (oomeCombo.getSelectedItem() != OOME_CUSTOM_TEXT)) {
            return false;
        }

        if (!oomeDetectionDirTextField.getText().equals(settings.getCustomHeapdumpPath())) {
            return false;
        }

        if (telemetryOverviewCombo.getSelectedItem() == KEY_OPEN_ALWAYS) {
            if (settings.getTelemetryOverviewBehavior() != ProfilerIDESettings.OPEN_ALWAYS) {
                return false;
            }
        } else if (telemetryOverviewCombo.getSelectedItem() == KEY_OPEN_MONITORING) {
            if (settings.getTelemetryOverviewBehavior() != ProfilerIDESettings.OPEN_MONITORING) {
                return false;
            }
        } else if (telemetryOverviewCombo.getSelectedItem() == KEY_OPEN_NEVER) {
            if (settings.getTelemetryOverviewBehavior() != ProfilerIDESettings.OPEN_NEVER) {
                return false;
            }
        }

        if (openThreadsViewCombo.getSelectedItem() == KEY_OPEN_ALWAYS) {
            if (settings.getThreadsViewBehavior() != ProfilerIDESettings.OPEN_ALWAYS) {
                return false;
            }
        } else if (openThreadsViewCombo.getSelectedItem() == KEY_OPEN_MONITORING) {
            if (settings.getThreadsViewBehavior() != ProfilerIDESettings.OPEN_MONITORING) {
                return false;
            }
        } else if (openThreadsViewCombo.getSelectedItem() == KEY_OPEN_NEVER) {
            if (settings.getThreadsViewBehavior() != ProfilerIDESettings.OPEN_NEVER) {
                return false;
            }
        }

        JavaPlatform sel = getSelectedJavaPlatform();

        if (sel == null) {
            if (settings.getJavaPlatformForProfiling() != null) {
                return false;
            }
        } else {
            if (!sel.getDisplayName().equals(settings.getJavaPlatformForProfiling())) {
                return false;
            }
        }

        if (settings.getHeapWalkerAnalysisEnabled() != enableHeapWalkerAnalysisCheckbox.isSelected()) {
            return false;
        }

        return true;
    }

    public void init(ProfilerIDESettings pis) {
        resetConfirmationsButton.setEnabled(true);
        updateJavaPlatformComboItems();

        // GlobalProfilingSettings
        portNoSpinner.setValue(new Integer(pis.getPortNo()));

        if (pis.getJavaPlatformForProfiling() != null) {
            javaPlatformCombo.setSelectedItem(pis.getJavaPlatformForProfiling());
        } else {
            javaPlatformCombo.setSelectedIndex(0);
        }

        // ProfilerIDESettings
        cpuLiveResultsCheckbox.setSelected(pis.getDisplayLiveResultsCPU());
        memoryLiveResultsCheckbox.setSelected(pis.getDisplayLiveResultsMemory());

        if (pis.getAutoOpenSnapshot() && pis.getAutoSaveSnapshot()) {
            takingSnapshotCombo.setSelectedItem(OPEN_SAVE_SNAPSHOT_RADIO_TEXT);
        } else if (pis.getAutoOpenSnapshot()) {
            takingSnapshotCombo.setSelectedItem(OPEN_SNAPSHOT_RADIO_TEXT);
        } else if (pis.getAutoSaveSnapshot()) {
            takingSnapshotCombo.setSelectedItem(SAVE_SNAPSHOT_RADIO_TEXT);
        }

        if (pis.getOOMDetectionMode() == pis.OOME_DETECTION_NONE) {
            oomeCombo.setSelectedItem(OOME_NOTHING_TEXT);
        } else if (pis.getOOMDetectionMode() == pis.OOME_DETECTION_PROJECTDIR) {
            oomeCombo.setSelectedItem(OOME_PROJECT_TEXT);
        } else if (pis.getOOMDetectionMode() == pis.OOME_DETECTION_TEMPDIR) {
            oomeCombo.setSelectedItem(OOME_TEMP_TEXT);
        } else if (pis.getOOMDetectionMode() == pis.OOME_DETECTION_CUSTOMDIR) {
            oomeCombo.setSelectedItem(OOME_CUSTOM_TEXT);
        }

        oomeDetectionDirTextField.setText(pis.getCustomHeapdumpPath());

        enableHeapWalkerAnalysisCheckbox.setSelected(pis.getHeapWalkerAnalysisEnabled());

        switch (pis.getTelemetryOverviewBehavior()) {
            case ProfilerIDESettings.OPEN_ALWAYS:
                telemetryOverviewCombo.setSelectedItem(KEY_OPEN_ALWAYS);

                break;
            case ProfilerIDESettings.OPEN_MONITORING:
                telemetryOverviewCombo.setSelectedItem(KEY_OPEN_MONITORING);

                break;
            default:
                telemetryOverviewCombo.setSelectedItem(KEY_OPEN_NEVER);

                break;
        }

        switch (pis.getThreadsViewBehavior()) {
            case ProfilerIDESettings.OPEN_ALWAYS:
                openThreadsViewCombo.setSelectedItem(KEY_OPEN_ALWAYS);

                break;
            case ProfilerIDESettings.OPEN_MONITORING:
                openThreadsViewCombo.setSelectedItem(KEY_OPEN_MONITORING);

                break;
            default:
                openThreadsViewCombo.setSelectedItem(KEY_OPEN_NEVER);

                break;
        }

        updateEnabling();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints;
        ButtonGroup oomeRadiosGroup = new ButtonGroup();

        // --- General -------------------------------------------------------------

        // General caption
        CategorySeparator generalSeparator = new CategorySeparator(ENGINE_SETTINGS_BORDER_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(2, 0, 0, 6);
        add(generalSeparator, gridBagConstraints);

        // javaPlatformLabel
        JLabel javaPlatformLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(javaPlatformLabel, JAVA_PLATFORM_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(javaPlatformLabel, gridBagConstraints);

        // javaPlatformCombo
        javaPlatformCombo = new JComboBox() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        javaPlatformCombo.getAccessibleContext().setAccessibleDescription(JAVA_PLAFORM_COMBO_ACCESS_DESCR);
        javaPlatformLabel.setLabelFor(javaPlatformCombo);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(javaPlatformCombo, gridBagConstraints);

        // managePlatformsButton
        managePlatformsButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(managePlatformsButton, MANAGE_PLATFORMS_BUTTON_NAME);
        managePlatformsButton.getAccessibleContext().setAccessibleDescription(MANAGE_PLATFORMS_BUTTON_NAME);
        managePlatformsButton.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 3, 0, 6);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(managePlatformsButton, gridBagConstraints);

        // portNoLabel
        JLabel portNoLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(portNoLabel, COMM_PORT_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(portNoLabel, gridBagConstraints);

        // portNoSpinner
        portNoSpinner = new JExtendedSpinner() {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width,
                                         org.netbeans.modules.profiler.ui.stp.Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        portNoLabel.setLabelFor(portNoSpinner);

        if (portNoSpinner.getAccessibleContext() != null) {
            portNoSpinner.getAccessibleContext().setAccessibleDescription(PORT_NO_SPINNER_ACCESS_DESCR);
        }

        portNoSpinner.fixAccessibility();
        portNoSpinner.setModel(new SpinnerNumberModel(5140, 1, 65535, 1));
        portNoSpinner.setPreferredSize(new Dimension(portNoSpinner.getPreferredSize().width,
                                                     javaPlatformCombo.getPreferredSize().height));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(portNoSpinner, gridBagConstraints);

        // --- When Profiling Starts -----------------------------------------------

        // Profiling Start caption
        CategorySeparator profilingStartSeparator = new CategorySeparator(WINDOWS_SETTINGS_BORDER_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(7, 0, 0, 6);
        add(profilingStartSeparator, gridBagConstraints);

        // telemetryOverviewLabel
        JLabel telemetryOverviewLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(telemetryOverviewLabel, TELEMETRY_OVERVIEW_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(telemetryOverviewLabel, gridBagConstraints);

        // telemetryOverviewCombo
        telemetryOverviewCombo = new JComboBox() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        telemetryOverviewLabel.setLabelFor(telemetryOverviewCombo);
        telemetryOverviewCombo.getAccessibleContext().setAccessibleDescription(TELEMETRY_OVERVIEW_COMBO_ACCESS_DESCR);
        telemetryOverviewCombo.setModel(new DefaultComboBoxModel(new String[] { KEY_OPEN_ALWAYS, KEY_OPEN_MONITORING, KEY_OPEN_NEVER }));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(telemetryOverviewCombo, gridBagConstraints);

        // openThreadsViewLabel
        JLabel openThreadsViewLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(openThreadsViewLabel, THREADS_VIEW_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(openThreadsViewLabel, gridBagConstraints);

        // openThreadsViewCombo
        openThreadsViewCombo = new JComboBox() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        openThreadsViewLabel.setLabelFor(openThreadsViewCombo);
        openThreadsViewCombo.getAccessibleContext()
                            .setAccessibleDescription(THREADS_VIEW_COMBO_ACCESS_DESCR + THREADS_VIEW_HINT_TEXT);
        openThreadsViewCombo.setModel(new DefaultComboBoxModel(new String[] { KEY_OPEN_ALWAYS, KEY_OPEN_MONITORING, KEY_OPEN_NEVER }));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(openThreadsViewCombo, gridBagConstraints);

        int maxWidth = Math.max(telemetryOverviewCombo.getPreferredSize().width, openThreadsViewCombo.getPreferredSize().width)
                       + 15;
        int maxHeight = Math.max(telemetryOverviewCombo.getPreferredSize().height, openThreadsViewCombo.getPreferredSize().height);
        telemetryOverviewCombo.setPreferredSize(new Dimension(maxWidth, maxHeight));
        openThreadsViewCombo.setPreferredSize(new Dimension(maxWidth, maxHeight));

        // liveResultsLabel
        JLabel liveResultsLabel = new JLabel(LIVE_RESULTS_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(liveResultsLabel, gridBagConstraints);

        // liveResultsPanel
        JPanel liveResultsPanel = new JPanel();
        liveResultsPanel.setLayout(new GridLayout(1, 2, 0, 0));

        // cpuLiveResultsCheckbox
        cpuLiveResultsCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(cpuLiveResultsCheckbox, CPU_CHCKBOX_TEXT);
        cpuLiveResultsCheckbox.getAccessibleContext().setAccessibleDescription(CPU_LIVE_RESULTS_CHECKBOX_ACCESS_DESCR);
        liveResultsPanel.add(cpuLiveResultsCheckbox);

        // memoryLiveResultsCheckbox
        memoryLiveResultsCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(memoryLiveResultsCheckbox, MEMORY_CHCKBOX_TEXT);
        memoryLiveResultsCheckbox.getAccessibleContext().setAccessibleDescription(MEMORY_LIVE_RESULTS_CHECKBOX_ACCESS_DESCR);
        liveResultsPanel.add(memoryLiveResultsCheckbox);

        // liveResultsLabel placing
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(liveResultsPanel, gridBagConstraints);

        // --- Miscellaneous -------------------------------------------------------

        // Miscellaneous caption
        CategorySeparator miscellaneousSeparator = new CategorySeparator(SNAPSHOTS_SETTINGS_BORDER_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 0, 0, 6);
        add(miscellaneousSeparator, gridBagConstraints);

        // takingSnapshotLabel
        JLabel takingSnapshotLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(takingSnapshotLabel, TAKING_SNAPSHOT_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(takingSnapshotLabel, gridBagConstraints);

        // takingSnapshotCombo
        takingSnapshotCombo = new JComboBox() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        takingSnapshotLabel.setLabelFor(takingSnapshotCombo);
        takingSnapshotCombo.getAccessibleContext().setAccessibleDescription(TAKING_SNAPSHOT_COMBO_ACCESS_DESCR);
        takingSnapshotCombo.setModel(new DefaultComboBoxModel(new String[] {
                                                                  OPEN_SNAPSHOT_RADIO_TEXT, SAVE_SNAPSHOT_RADIO_TEXT,
                                                                  OPEN_SAVE_SNAPSHOT_RADIO_TEXT
                                                              }));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(takingSnapshotCombo, gridBagConstraints);

        // oomeDetectionLabel
        JLabel oomeDetectionLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(oomeDetectionLabel, OOME_BORDER_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(oomeDetectionLabel, gridBagConstraints);

        // oomeCombo
        oomeCombo = new JComboBox() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        oomeDetectionLabel.setLabelFor(oomeCombo);
        oomeCombo.getAccessibleContext().setAccessibleDescription(OOME_COMBO_ACCESS_DESCR);
        oomeCombo.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    updateEnabling();
                }
            });
        oomeCombo.setModel(new DefaultComboBoxModel(new String[] {
                                                        OOME_NOTHING_TEXT, OOME_PROJECT_TEXT, OOME_TEMP_TEXT, OOME_CUSTOM_TEXT
                                                    }));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(oomeCombo, gridBagConstraints);

        int maxWidth1 = Math.max(takingSnapshotCombo.getPreferredSize().width, oomeCombo.getPreferredSize().width);
        int maxHeight1 = Math.max(takingSnapshotCombo.getPreferredSize().height, oomeCombo.getPreferredSize().height);
        takingSnapshotCombo.setPreferredSize(new Dimension(maxWidth1, maxHeight1));
        oomeCombo.setPreferredSize(new Dimension(maxWidth1, maxHeight1));

        // oomeDetectionPanel
        JPanel oomeDetectionPanel = new JPanel(new GridBagLayout());

        // oomeDetectionDirTextField
        oomeDetectionDirTextField = new JTextField() {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, oomeDetectionChooseDirButton.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return new Dimension(super.getMinimumSize().width, getPreferredSize().height);
                }
            };
        oomeDetectionDirTextField.getAccessibleContext().setAccessibleName(OOME_CUSTOM_ACCESS_DESCR);
        oomeDetectionDirTextField.getAccessibleContext().setAccessibleDescription(OOME_CUSTOM_TEXTFIELD_ACCESS_DESCR);
        oomeDetectionDirTextField.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        oomeDetectionPanel.add(oomeDetectionDirTextField, gridBagConstraints);

        // oomeDetectionChooseDirButton
        oomeDetectionChooseDirButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(oomeDetectionChooseDirButton, "&..."); // NOI18N
        oomeDetectionChooseDirButton.getAccessibleContext().setAccessibleName(OOME_CUSTOM_BUTTON_ACCESS_NAME);
        oomeDetectionChooseDirButton.addActionListener(this);
        oomeDetectionChooseDirButton.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 3, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        oomeDetectionPanel.add(oomeDetectionChooseDirButton, gridBagConstraints);

        // oomeDetectionPanel
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new Insets(5, 0, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(oomeDetectionPanel, gridBagConstraints);

        // heapWalkerLabel
        JLabel heapWalkerLabel = new JLabel(HEAPWALKER_LABEL_TEXT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new Insets(5, 10, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        add(heapWalkerLabel, gridBagConstraints);

        // enableHeapWalkerAnalysisCheckbox
        enableHeapWalkerAnalysisCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(enableHeapWalkerAnalysisCheckbox, ENABLE_ANALYSIS_CHECKBOX);
        enableHeapWalkerAnalysisCheckbox.getAccessibleContext().setAccessibleDescription(ENABLE_ANALYSIS_CHECKBOX);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        add(enableHeapWalkerAnalysisCheckbox, gridBagConstraints);

        // resetConfirmationsPanel
        JPanel resetConfirmationsPanel = new JPanel(new GridBagLayout());

        // resetConfirmationsArea
        JTextArea resetConfirmationsArea = new JTextArea(RESET_HINT_TEXT) {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = 1;
                return size;
            }
        };
        resetConfirmationsArea.setOpaque(false);
        resetConfirmationsArea.setWrapStyleWord(true);
        resetConfirmationsArea.setLineWrap(true);
        resetConfirmationsArea.setEnabled(false);
        resetConfirmationsArea.setFont(UIManager.getFont("Label.font")); //NOI18N
        resetConfirmationsArea.setDisabledTextColor(UIManager.getColor("Label.foreground")); //NOI18N
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        resetConfirmationsPanel.add(resetConfirmationsArea, gridBagConstraints);

        // resetConfirmationsButton
        resetConfirmationsButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(resetConfirmationsButton, RESET_BUTTON_NAME);
        resetConfirmationsButton.getAccessibleContext().setAccessibleDescription(RESET_HINT_TEXT);
        resetConfirmationsButton.addActionListener(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        resetConfirmationsPanel.add(resetConfirmationsButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.insets = new Insets(5, 10, 0, 6);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(resetConfirmationsPanel, gridBagConstraints);
    }

    private void updateEnabling() {
        boolean customOOMEdirSelected = oomeCombo.getSelectedItem() == OOME_CUSTOM_TEXT;
        oomeDetectionDirTextField.setEnabled(customOOMEdirSelected);
        oomeDetectionChooseDirButton.setEnabled(customOOMEdirSelected);
    }

    private void updateJavaPlatformComboItems() {
        updateJavaPlatforms();

        Object selectedJavaPlatform = javaPlatformCombo.getSelectedItem();

        javaPlatformCombo.removeAllItems();

        DefaultComboBoxModel javaPlatformComboModel = new DefaultComboBoxModel(supportedJavaPlatformsNames.toArray());
        javaPlatformComboModel.insertElementAt(USE_PROJECT_JVM_TEXT, 0);

        javaPlatformCombo.setModel(javaPlatformComboModel);

        if (selectedJavaPlatform != null) {
            javaPlatformCombo.setSelectedItem(selectedJavaPlatform);
        }
    }

    private void updateJavaPlatforms() {
        supportedJavaPlatforms.clear();
        supportedJavaPlatformsNames.clear();

        Iterator supportedPlatforms = JavaPlatformSelector.getSupportedPlatforms().iterator();

        JavaPlatform supportedJavaPlatform;
        String supportedJavaPlatformName;

        while (supportedPlatforms.hasNext()) {
            supportedJavaPlatform = (JavaPlatform) supportedPlatforms.next();
            supportedJavaPlatformName = supportedJavaPlatform.getDisplayName();

            if (!supportedJavaPlatformsNames.contains(supportedJavaPlatformName)) {
                supportedJavaPlatforms.add(supportedJavaPlatform);
                supportedJavaPlatformsNames.add(supportedJavaPlatformName);
            }
        }

        supportedJavaPlatforms.addAll(JavaPlatformSelector.getSupportedPlatforms());
    }
}

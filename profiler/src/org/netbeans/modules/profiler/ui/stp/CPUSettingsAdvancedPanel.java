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

package org.netbeans.modules.profiler.ui.stp;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.modules.profiler.actions.JavaPlatformSelector;
import org.netbeans.modules.profiler.ui.HyperlinkLabel;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class CPUSettingsAdvancedPanel extends DefaultSettingsPanel implements HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SCHEME_COMBOBOX_ITEM_LAZY = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                "CPUSettingsAdvancedPanel_SchemeComboBoxItemLazy"); // NOI18N
    private static final String SCHEME_COMBOBOX_ITEM_EAGER = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                 "CPUSettingsAdvancedPanel_SchemeComboBoxItemEager"); // NOI18N
    private static final String SCHEME_COMBOBOX_ITEM_TOTAL = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                 "CPUSettingsAdvancedPanel_SchemeComboBoxItemTotal"); // NOI18N
    private static final String DO_NOT_OVERRIDE_STRING = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                             "CPUSettingsAdvancedPanel_DoNotOverrideString"); // NOI18N
    private static final String CHOOSE_WORKDIR_DIALOG_CAPTION = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                    "CPUSettingsAdvancedPanel_ChooseWorkDirDialogCaption"); // NOI18N
    private static final String SETTINGS_CAPTION = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                       "CPUSettingsAdvancedPanel_SettingsCaption"); // NOI18N
    private static final String METHODS_TRACKING_LABEL_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                  "CPUSettingsAdvancedPanel_MethodsTrackingLabelText"); // NOI18N
    private static final String INSTR_RADIO_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                       "CPUSettingsAdvancedPanel_InstrRadioText"); // NOI18N
    private static final String SAMPLING_RADIO_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                          "CPUSettingsAdvancedPanel_SamplingRadioText"); // NOI18N
    private static final String EXCLUDE_TIME_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                 "CPUSettingsAdvancedPanel_ExcludeTimeCheckboxText"); // NOI18N
    private static final String PROFILE_FRAMEWORK_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                      "CPUSettingsAdvancedPanel_ProfileFrameworkCheckboxText"); // NOI18N
    private static final String PROFILE_THREADS_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                    "CPUSettingsAdvancedPanel_ProfileThreadsCheckboxText"); // NOI18N
    private static final String LIMIT_THREADS_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                  "CPUSettingsAdvancedPanel_LimitThreadsCheckboxText"); // NOI18N
    private static final String THREAD_TIMER_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                 "CPUSettingsAdvancedPanel_ThreadTimerCheckboxText"); // NOI18N
    private static final String INSTR_SCHEME_LABEL_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                              "CPUSettingsAdvancedPanel_InstrSchemeLabelText"); // NOI18N
    private static final String INSTRUMENT_LABEL_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                            "CPUSettingsAdvancedPanel_InstrumentLabelText"); // NOI18N
    private static final String METHOD_INVOKE_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                  "CPUSettingsAdvancedPanel_MethodInvokeCheckboxText"); // NOI18N
    private static final String GETTER_SETTER_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                  "CPUSettingsAdvancedPanel_GetterSetterCheckboxText"); // NOI18N
    private static final String EMPTY_METHODS_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                  "CPUSettingsAdvancedPanel_EmptyMethodsCheckboxText"); // NOI18N
    private static final String THREADS_CAPTION = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                      "CPUSettingsAdvancedPanel_ThreadsCaption"); // NOI18N
    private static final String ENABLE_THREADS_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                   "CPUSettingsAdvancedPanel_EnableThreadsCheckboxText"); // NOI18N
    private static final String GLOBAL_SETTINGS_CAPTION = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                              "CPUSettingsAdvancedPanel_GlobalSettingsCaption"); // NOI18N
    private static final String OVERRIDE_SETTINGS_CHECKBOX_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                      "CPUSettingsAdvancedPanel_OverrideSettingsCheckboxText"); // NOI18N
    private static final String WORKDIR_LABEL_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                         "CPUSettingsAdvancedPanel_WorkDirLabelText"); // NOI18N
    private static final String CHOOSE_WORKDIR_LINK_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "CPUSettingsAdvancedPanel_ChooseWorkDirLinkText"); // NOI18N
    private static final String JAVA_PLATFORM_LABEL_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "CPUSettingsAdvancedPanel_JavaPlatformLabelText"); // NOI18N
    private static final String JVM_ARGUMENTS_LABEL_TEXT = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "CPUSettingsAdvancedPanel_JvmArgumentsLabelText"); // NOI18N
    private static final String STP_MONITOR_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpMonitorTooltip"); // NOI18N
    private static final String STP_OVERRIDE_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpOverrideTooltip"); // NOI18N
    private static final String STP_WORKDIR_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpWorkDirTooltip"); // NOI18N
    private static final String STP_JPLATFORM_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpJPlatformTooltip"); // NOI18N
    private static final String STP_VMARGS_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpVmArgsTooltip"); // NOI18N
    private static final String STP_EXACTTIMING_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                              "StpExactTimingTooltip"); // NOI18N
    private static final String STP_SAMPLEDTIMING_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                                "StpSampledTimingTooltip"); // NOI18N
    private static final String STP_SLEEPWAIT_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpSleepWaitTooltip"); // NOI18N
    private static final String STP_FRAMEWORK_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpFrameworkTooltip"); // NOI18N
    private static final String STP_SPAWNED_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpSpawnedTooltip"); // NOI18N
    private static final String STP_LIMITTHREADS_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "StpLimitThreadsTooltip"); // NOI18N
    private static final String STP_CPUTIMER_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class, "StpCpuTimerTooltip"); // NOI18N
    private static final String STP_INSTRSCHEME_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                              "StpInstrSchemeTooltip"); // NOI18N
    private static final String STP_METHODINVOKE_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "StpMethodInvokeTooltip"); // NOI18N
    private static final String STP_GETTERSETTER_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "StpGetterSetterTooltip"); // NOI18N
    private static final String STP_EMPTYMETHODS_TOOLTIP = NbBundle.getMessage(CPUSettingsAdvancedPanel.class,
                                                                               "StpEmptyMethodsTooltip"); // NOI18N
                                                                                                          // -----
    private static final String HELP_CTX_KEY = "CPUSettings.Advanced.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HyperlinkLabel workingDirectorySelectLink;
    private JCheckBox excludeTimeCheckbox;
    private JCheckBox instrumentEmptyMethodsCheckbox;
    private JCheckBox instrumentGettersSettersCheckbox;
    private JCheckBox instrumentMethodInvokeCheckbox;
    private JCheckBox limitThreadsCheckbox;
    private JCheckBox overrideSettingsCheckbox;
    private JCheckBox profileFrameworkCheckbox;
    private JCheckBox profileSpawnedThreadsCheckbox;
    private JCheckBox threadsMonitoringCheckbox;
    private JCheckBox useCPUTimerCheckbox;
    private JComboBox instrumentationSchemeCombo;
    private JComboBox javaPlatformCombo;
    private JLabel instrumentLabel;
    private JLabel instrumentationSchemeLabel;
    private JLabel javaPlatformLabel;
    private JLabel methodsTrackingLabel;
    private JLabel sampledTimingLabel;
    private JLabel vmArgumentsLabel;
    private JLabel workingDirectoryLabel;
    private JPanel globalSettingsPanel;

    // --- UI components declaration ---------------------------------------------
    private JPanel settingsPanel;
    private JPanel threadsSettingsPanel;
    private JRadioButton exactTimingRadio;
    private JRadioButton sampledTimingRadio;
    private JSpinner limitThreadsSpinner;
    private JSpinner sampledTimingSpinner;
    private JTextField vmArgumentsTextField;
    private JTextField workingDirectoryTextField;
    private WeakReference<JFileChooser> workingDirectoryChooserReference;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public CPUSettingsAdvancedPanel() {
        super();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCPUProfilingType(int type) { // CommonConstants.INSTR_RECURSIVE_FULL or SAMPLED
        exactTimingRadio.setSelected(type == CommonConstants.CPU_INSTR_FULL);
        sampledTimingRadio.setSelected(type == CommonConstants.CPU_INSTR_SAMPLED);
        sampledTimingSpinner.setEnabled(sampledTimingRadio.isSelected());
    }

    public int getCPUProfilingType() {
        if (exactTimingRadio.isSelected()) {
            return CommonConstants.CPU_INSTR_FULL;
        } else {
            return CommonConstants.CPU_INSTR_SAMPLED;
        }
    }

    public void setEntireAppDefaults(boolean isPreset) {
        if (isPreset) {
            profileSpawnedThreadsCheckbox.setSelected(false);
        }

        if (isPreset) {
            instrumentationSchemeCombo.setSelectedItem(SCHEME_COMBOBOX_ITEM_TOTAL);
        }

        if (isPreset) {
            profileFrameworkCheckbox.setSelected(false);
        }

        if (!isPreset) {
            profileFrameworkCheckbox.setEnabled(true);
        }
    }

    public void setExcludeThreadTime(boolean exclude) {
        excludeTimeCheckbox.setSelected(exclude);
    }

    public boolean getExcludeThreadTime() {
        return excludeTimeCheckbox.isSelected();
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public void setInstrumentEmptyMethods(boolean instrument) {
        instrumentEmptyMethodsCheckbox.setSelected(instrument);
    }

    public boolean getInstrumentEmptyMethods() {
        return instrumentEmptyMethodsCheckbox.isSelected();
    }

    public void setInstrumentGettersSetters(boolean instrument) {
        instrumentGettersSettersCheckbox.setSelected(instrument);
    }

    public boolean getInstrumentGettersSetters() {
        return instrumentGettersSettersCheckbox.isSelected();
    }

    public void setInstrumentMethodInvoke(boolean instrument) {
        instrumentMethodInvokeCheckbox.setSelected(instrument);
    }

    public boolean getInstrumentMethodInvoke() {
        return instrumentMethodInvokeCheckbox.isSelected();
    }

    public void setInstrumentationScheme(int scheme) {
        if (scheme == CommonConstants.INSTRSCHEME_LAZY) {
            instrumentationSchemeCombo.setSelectedItem(SCHEME_COMBOBOX_ITEM_LAZY);
        } else if (scheme == CommonConstants.INSTRSCHEME_EAGER) {
            instrumentationSchemeCombo.setSelectedItem(SCHEME_COMBOBOX_ITEM_EAGER);
        } else {
            instrumentationSchemeCombo.setSelectedItem(SCHEME_COMBOBOX_ITEM_TOTAL);
        }
    }

    public int getInstrumentationScheme() {
        Object selectedScheme = instrumentationSchemeCombo.getSelectedItem();

        if (selectedScheme == SCHEME_COMBOBOX_ITEM_LAZY) {
            return CommonConstants.INSTRSCHEME_LAZY;
        } else if (selectedScheme == SCHEME_COMBOBOX_ITEM_EAGER) {
            return CommonConstants.INSTRSCHEME_EAGER;
        } else {
            return CommonConstants.INSTRSCHEME_TOTAL;
        }
    }

    public void setJavaPlatformName(String javaPlatformName) {
        updateJavaPlatformCombo(javaPlatformName);
    }

    public String getJavaPlatformName() {
        int selIndex = javaPlatformCombo.getSelectedIndex();

        if (selIndex == 0) {
            return null;
        } else {
            return javaPlatformCombo.getSelectedItem().toString();
        }
    }

    public void setOverrideAvailable(boolean enableOverride) { // should be called before setOverrideSettings() to allow correct enabling/disabling of controls
        overrideSettingsCheckbox.setEnabled(enableOverride);
        workingDirectoryLabel.setEnabled(enableOverride);
        workingDirectoryTextField.setEnabled(enableOverride);
        workingDirectorySelectLink.setEnabled(enableOverride);
        javaPlatformLabel.setEnabled(enableOverride);
        javaPlatformCombo.setEnabled(enableOverride);
        vmArgumentsLabel.setEnabled(enableOverride);
        vmArgumentsTextField.setEnabled(enableOverride);
    }

    public void setOverrideSettings(boolean override) {
        overrideSettingsCheckbox.setSelected(override);
        updateEnabling();
    }

    public boolean getOverrideSettings() {
        return overrideSettingsCheckbox.isSelected();
    }

    public void setPartOfAppDefaults(boolean isPreset) {
        if (isPreset) {
            profileSpawnedThreadsCheckbox.setSelected(false);
        }

        if (isPreset) {
            instrumentationSchemeCombo.setSelectedItem(SCHEME_COMBOBOX_ITEM_LAZY);
        }

        profileFrameworkCheckbox.setSelected(false);
        profileFrameworkCheckbox.setEnabled(false);
    }

    public void setProfileFramework(boolean profile) {
        profileFrameworkCheckbox.setSelected(profile);
    }

    public boolean getProfileFramework() {
        return profileFrameworkCheckbox.isSelected();
    }

    public void setProfileSpawnedThreads(boolean profile) {
        profileSpawnedThreadsCheckbox.setSelected(profile);
    }

    public boolean getProfileSpawnedThreads() {
        return profileSpawnedThreadsCheckbox.isSelected();
    }

    public void setProfiledThreadsLimit(int limit) {
        limitThreadsCheckbox.setSelected(limit > 0);
        limitThreadsSpinner.setValue(Math.abs(Integer.valueOf(limit)));
        limitThreadsSpinner.setEnabled(limitThreadsCheckbox.isSelected());
    }

    public int getProfiledThreadsLimit() {
        if (limitThreadsCheckbox.isSelected()) {
            return ((Integer) limitThreadsSpinner.getValue()).intValue();
        } else {
            return -((Integer) limitThreadsSpinner.getValue()).intValue();
        }
    }

    public void setSamplingInterval(int samplingInterval) {
        sampledTimingSpinner.setValue(Integer.valueOf(samplingInterval));
    }

    public int getSamplingInterval() {
        return ((Integer) sampledTimingSpinner.getValue()).intValue();
    }

    public void setThreadsMonitoring(boolean enabled) {
        threadsMonitoringCheckbox.setSelected(enabled);
    }

    public boolean getThreadsMonitoring() {
        return threadsMonitoringCheckbox.isSelected();
    }

    public void setUseCPUTimer(boolean use, boolean available) {
        useCPUTimerCheckbox.setSelected(use);
        useCPUTimerCheckbox.setEnabled(available);
    }

    public boolean getUseCPUTimer() {
        return useCPUTimerCheckbox.isSelected();
    }

    public void setVMArguments(String vmArguments) {
        vmArgumentsTextField.setText(vmArguments);
    }

    public String getVMArguments() {
        return vmArgumentsTextField.getText().trim();
    }

    public void setWorkingDirectory(String workingDirectory) {
        workingDirectoryTextField.setText(workingDirectory);
    }

    public String getWorkingDirectory() {
        return workingDirectoryTextField.getText().trim();
    }

    public void disableAll() {
        methodsTrackingLabel.setEnabled(false);
        exactTimingRadio.setEnabled(false);
        sampledTimingRadio.setEnabled(false);
        sampledTimingSpinner.setEnabled(false);
        sampledTimingLabel.setEnabled(false);
        excludeTimeCheckbox.setEnabled(false);
        profileFrameworkCheckbox.setEnabled(false);
        profileSpawnedThreadsCheckbox.setEnabled(false);
        limitThreadsCheckbox.setEnabled(false);
        limitThreadsSpinner.setEnabled(false);
        useCPUTimerCheckbox.setEnabled(false);
        instrumentationSchemeLabel.setEnabled(false);
        instrumentationSchemeCombo.setEnabled(false);
        instrumentLabel.setEnabled(false);
        instrumentMethodInvokeCheckbox.setEnabled(false);
        instrumentGettersSettersCheckbox.setEnabled(false);
        instrumentEmptyMethodsCheckbox.setEnabled(false);

        threadsSettingsPanel.setEnabled(false);
        threadsMonitoringCheckbox.setEnabled(false);

        globalSettingsPanel.setEnabled(false);
        overrideSettingsCheckbox.setEnabled(false);
        workingDirectoryLabel.setEnabled(false);
        workingDirectoryTextField.setEnabled(false);
        workingDirectorySelectLink.setEnabled(false);
        javaPlatformLabel.setEnabled(false);
        javaPlatformCombo.setEnabled(false);
        vmArgumentsLabel.setEnabled(false);
        vmArgumentsTextField.setEnabled(false);
    }

    public void enableAll() {
        methodsTrackingLabel.setEnabled(true);
        exactTimingRadio.setEnabled(true);
        sampledTimingRadio.setEnabled(true);
        sampledTimingSpinner.setEnabled(true);
        sampledTimingLabel.setEnabled(true);
        excludeTimeCheckbox.setEnabled(true);
        profileFrameworkCheckbox.setEnabled(true);
        profileSpawnedThreadsCheckbox.setEnabled(true);
        limitThreadsCheckbox.setEnabled(true);
        limitThreadsSpinner.setEnabled(true);
        useCPUTimerCheckbox.setEnabled(true);
        instrumentationSchemeLabel.setEnabled(true);
        instrumentationSchemeCombo.setEnabled(true);
        instrumentLabel.setEnabled(true);
        instrumentMethodInvokeCheckbox.setEnabled(true);
        instrumentGettersSettersCheckbox.setEnabled(true);
        instrumentEmptyMethodsCheckbox.setEnabled(true);

        threadsSettingsPanel.setEnabled(true);
        threadsMonitoringCheckbox.setEnabled(true);

        globalSettingsPanel.setEnabled(true);
        overrideSettingsCheckbox.setEnabled(true);
        workingDirectoryLabel.setEnabled(true);
        workingDirectoryTextField.setEnabled(true);
        workingDirectorySelectLink.setEnabled(true);
        javaPlatformLabel.setEnabled(true);
        javaPlatformCombo.setEnabled(true);
        vmArgumentsLabel.setEnabled(true);
        vmArgumentsTextField.setEnabled(true);
    }

    // --- Static tester frame ---------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("plaf.metal.MetalLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
        } catch (Exception e) {
        }

        ;

        JFrame frame = new JFrame("Tester Frame"); //NOI18N
        JPanel contents = new CPUSettingsAdvancedPanel();
        contents.setPreferredSize(new Dimension(375, 255));
        frame.getContentPane().add(contents);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private JFileChooser getFileChooser() {
        JFileChooser chooser;

        if ((workingDirectoryChooserReference == null) || (workingDirectoryChooserReference.get() == null)) {
            chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setDialogTitle(CHOOSE_WORKDIR_DIALOG_CAPTION);
            workingDirectoryChooserReference = new WeakReference(chooser);
        } else {
            chooser = workingDirectoryChooserReference.get();
        }

        return chooser;
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        ButtonGroup methodsTrackingRadiosGroup = new ButtonGroup();

        // settingsPanel
        settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setOpaque(false);
        settingsPanel.setBorder(BorderFactory.createTitledBorder(SETTINGS_CAPTION));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 5, 10, 5);
        add(settingsPanel, constraints);

        // methodsTrackingLabel
        methodsTrackingLabel = new JLabel(METHODS_TRACKING_LABEL_TEXT);
        methodsTrackingLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 7, 0, 0);
        settingsPanel.add(methodsTrackingLabel, constraints);

        // exactTimingRadio
        exactTimingRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(exactTimingRadio, INSTR_RADIO_TEXT);
        exactTimingRadio.setToolTipText(STP_EXACTTIMING_TOOLTIP);
        methodsTrackingRadiosGroup.add(exactTimingRadio);
        exactTimingRadio.addActionListener(getSettingsChangeListener());
        exactTimingRadio.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 19, 0, 0);
        settingsPanel.add(exactTimingRadio, constraints);

        // sampledTimingContainer - definition
        JPanel sampledTimingContainer = new JPanel(new GridBagLayout());

        // sampledTimingRadio
        sampledTimingRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(sampledTimingRadio, SAMPLING_RADIO_TEXT);
        sampledTimingRadio.setToolTipText(STP_SAMPLEDTIMING_TOOLTIP);
        methodsTrackingRadiosGroup.add(sampledTimingRadio);
        sampledTimingRadio.setOpaque(false);
        sampledTimingRadio.setSelected(true);
        sampledTimingRadio.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    sampledTimingSpinner.setEnabled(sampledTimingRadio.isSelected());
                    sampledTimingLabel.setEnabled(sampledTimingRadio.isSelected());
                }
            });
        sampledTimingRadio.addActionListener(getSettingsChangeListener());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 5);
        sampledTimingContainer.add(sampledTimingRadio, constraints);

        // sampledTimingSpinner
        sampledTimingSpinner = new JExtendedSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1)) {
                public Dimension getPreferredSize() {
                    return new Dimension(55, Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        sampledTimingSpinner.addChangeListener(getSettingsChangeListener());
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        sampledTimingContainer.add(sampledTimingSpinner, constraints);

        // sampledTimingLabel
        sampledTimingLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(sampledTimingLabel, "&ms"); // NOI18N
        sampledTimingLabel.setLabelFor(sampledTimingSpinner);
        sampledTimingSpinner.setToolTipText(STP_SAMPLEDTIMING_TOOLTIP);
        sampledTimingLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 0);
        sampledTimingContainer.add(sampledTimingLabel, constraints);

        // sampledTimingContainer - customization
        sampledTimingContainer.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(1, 19, 5, 0);
        settingsPanel.add(sampledTimingContainer, constraints);

        // excludeTimeCheckbox
        excludeTimeCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(excludeTimeCheckbox, EXCLUDE_TIME_CHECKBOX_TEXT);
        excludeTimeCheckbox.setToolTipText(STP_SLEEPWAIT_TOOLTIP);
        excludeTimeCheckbox.addActionListener(getSettingsChangeListener());
        excludeTimeCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 0, 0);
        settingsPanel.add(excludeTimeCheckbox, constraints);

        // profileFrameworkCheckbox
        profileFrameworkCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(profileFrameworkCheckbox, PROFILE_FRAMEWORK_CHECKBOX_TEXT);
        profileFrameworkCheckbox.setToolTipText(STP_FRAMEWORK_TOOLTIP);
        profileFrameworkCheckbox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    updateEnabling();
                }
            });
        profileFrameworkCheckbox.addActionListener(getSettingsChangeListener());
        profileFrameworkCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 0, 0);
        settingsPanel.add(profileFrameworkCheckbox, constraints);

        // profileSpawnedThreadsCheckbox
        profileSpawnedThreadsCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(profileSpawnedThreadsCheckbox, PROFILE_THREADS_CHECKBOX_TEXT);
        profileSpawnedThreadsCheckbox.setToolTipText(STP_SPAWNED_TOOLTIP);
        profileSpawnedThreadsCheckbox.addActionListener(getSettingsChangeListener());
        profileSpawnedThreadsCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 0, 0);
        settingsPanel.add(profileSpawnedThreadsCheckbox, constraints);

        // limitThreadsContainer - definition
        JPanel limitThreadsContainer = new JPanel(new GridBagLayout());

        // limitThreadsCheckbox
        limitThreadsCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(limitThreadsCheckbox, LIMIT_THREADS_CHECKBOX_TEXT);
        limitThreadsCheckbox.setToolTipText(STP_LIMITTHREADS_TOOLTIP);
        limitThreadsCheckbox.addActionListener(getSettingsChangeListener());
        limitThreadsCheckbox.setOpaque(false);
        limitThreadsCheckbox.setSelected(true);
        limitThreadsCheckbox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    limitThreadsSpinner.setEnabled(limitThreadsCheckbox.isSelected());
                }
            });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 5);
        limitThreadsContainer.add(limitThreadsCheckbox, constraints);

        // limitThreadsSpinner
        limitThreadsSpinner = new JExtendedSpinner(new SpinnerNumberModel(50, 1, Integer.MAX_VALUE, 1)) {
                public Dimension getPreferredSize() {
                    return new Dimension(55, Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        limitThreadsSpinner.setToolTipText(STP_LIMITTHREADS_TOOLTIP);
        limitThreadsSpinner.addChangeListener(getSettingsChangeListener());
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        limitThreadsContainer.add(limitThreadsSpinner, constraints);

        // limitThreadsContainer - customization
        limitThreadsContainer.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 0, 0);
        settingsPanel.add(limitThreadsContainer, constraints);

        // useCPUTimerCheckbox
        useCPUTimerCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(useCPUTimerCheckbox, THREAD_TIMER_CHECKBOX_TEXT);
        useCPUTimerCheckbox.setToolTipText(STP_CPUTIMER_TOOLTIP);
        useCPUTimerCheckbox.addActionListener(getSettingsChangeListener());
        useCPUTimerCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 0, 0);
        settingsPanel.add(useCPUTimerCheckbox, constraints);

        // instrumentationSchemeContainer - definition
        JPanel instrumentationSchemeContainer = new JPanel(new GridBagLayout());

        // instrumentationSchemeLabel
        instrumentationSchemeLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(instrumentationSchemeLabel, INSTR_SCHEME_LABEL_TEXT);
        instrumentationSchemeLabel.setToolTipText(STP_INSTRSCHEME_TOOLTIP);
        instrumentationSchemeLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 5);
        instrumentationSchemeContainer.add(instrumentationSchemeLabel, constraints);

        // instrumentationSchemeCombo
        instrumentationSchemeCombo = new JComboBox(new String[] {
                                                       SCHEME_COMBOBOX_ITEM_LAZY, SCHEME_COMBOBOX_ITEM_EAGER,
                                                       SCHEME_COMBOBOX_ITEM_TOTAL
                                                   }) {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        instrumentationSchemeLabel.setLabelFor(instrumentationSchemeCombo);
        instrumentationSchemeCombo.setToolTipText(STP_INSTRSCHEME_TOOLTIP);
        instrumentationSchemeCombo.addActionListener(getSettingsChangeListener());
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        instrumentationSchemeContainer.add(instrumentationSchemeCombo, constraints);

        // instrumentationSchemeContainer - customization
        instrumentationSchemeContainer.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 2, 0);
        settingsPanel.add(instrumentationSchemeContainer, constraints);

        // instrumentLabel
        instrumentLabel = new JLabel(INSTRUMENT_LABEL_TEXT);
        instrumentLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 7, 0, 0);
        settingsPanel.add(instrumentLabel, constraints);

        // instrumentMethodInvokeCheckbox
        instrumentMethodInvokeCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(instrumentMethodInvokeCheckbox, METHOD_INVOKE_CHECKBOX_TEXT);
        instrumentMethodInvokeCheckbox.setToolTipText(STP_METHODINVOKE_TOOLTIP);
        instrumentMethodInvokeCheckbox.addActionListener(getSettingsChangeListener());
        instrumentMethodInvokeCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 19, 0, 0);
        settingsPanel.add(instrumentMethodInvokeCheckbox, constraints);

        // instrumentGettersSettersCheckbox
        instrumentGettersSettersCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(instrumentGettersSettersCheckbox, GETTER_SETTER_CHECKBOX_TEXT);
        instrumentGettersSettersCheckbox.setToolTipText(STP_GETTERSETTER_TOOLTIP);
        instrumentGettersSettersCheckbox.addActionListener(getSettingsChangeListener());
        instrumentGettersSettersCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(1, 19, 0, 0);
        settingsPanel.add(instrumentGettersSettersCheckbox, constraints);

        // instrumentEmptyMethodsCheckbox
        instrumentEmptyMethodsCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(instrumentEmptyMethodsCheckbox, EMPTY_METHODS_CHECKBOX_TEXT);
        instrumentEmptyMethodsCheckbox.setToolTipText(STP_EMPTYMETHODS_TOOLTIP);
        instrumentEmptyMethodsCheckbox.addActionListener(getSettingsChangeListener());
        instrumentEmptyMethodsCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(1, 19, 3, 0);
        settingsPanel.add(instrumentEmptyMethodsCheckbox, constraints);

        // threadsSettingsPanel
        threadsSettingsPanel = new JPanel(new GridBagLayout());
        threadsSettingsPanel.setOpaque(false);
        threadsSettingsPanel.setBorder(BorderFactory.createTitledBorder(THREADS_CAPTION));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 5, 10, 5);
        add(threadsSettingsPanel, constraints);

        // threadsMonitoringCheckbox
        threadsMonitoringCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(threadsMonitoringCheckbox, ENABLE_THREADS_CHECKBOX_TEXT);
        threadsMonitoringCheckbox.setToolTipText(STP_MONITOR_TOOLTIP);
        threadsMonitoringCheckbox.addActionListener(getSettingsChangeListener());
        threadsMonitoringCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 7, 3, 0);
        threadsSettingsPanel.add(threadsMonitoringCheckbox, constraints);

        // globalSettingsPanel
        globalSettingsPanel = new JPanel(new GridBagLayout());
        globalSettingsPanel.setOpaque(false);
        globalSettingsPanel.setBorder(BorderFactory.createTitledBorder(GLOBAL_SETTINGS_CAPTION));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        add(globalSettingsPanel, constraints);

        // overrideSettingsCheckbox
        overrideSettingsCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(overrideSettingsCheckbox, OVERRIDE_SETTINGS_CHECKBOX_TEXT);
        overrideSettingsCheckbox.setToolTipText(STP_OVERRIDE_TOOLTIP);
        overrideSettingsCheckbox.setOpaque(false);
        overrideSettingsCheckbox.setSelected(true);
        overrideSettingsCheckbox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    updateEnabling();
                }
            });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 7, 0, 0);
        globalSettingsPanel.add(overrideSettingsCheckbox, constraints);

        // workingDirectoryLabel
        workingDirectoryLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(workingDirectoryLabel, WORKDIR_LABEL_TEXT);
        workingDirectoryLabel.setToolTipText(STP_WORKDIR_TOOLTIP);
        workingDirectoryLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 19, 0, 0);
        globalSettingsPanel.add(workingDirectoryLabel, constraints);

        // workingDirectoryTextField
        workingDirectoryTextField = new JTextField() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        workingDirectoryLabel.setLabelFor(workingDirectoryTextField);
        workingDirectoryTextField.setToolTipText(STP_WORKDIR_TOOLTIP);
        workingDirectoryTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { checkWorkingDirectory(); }
            public void removeUpdate(DocumentEvent e)  { checkWorkingDirectory(); }
            public void changedUpdate(DocumentEvent e) { checkWorkingDirectory(); }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        globalSettingsPanel.add(workingDirectoryTextField, constraints);

        // workingDirectorySelectLink
        Color linkColor = Color.RED;
        String colorText = "rgb(" + linkColor.getRed() + "," + linkColor.getGreen() + "," + linkColor.getBlue() + ")"; //NOI18N
        workingDirectorySelectLink = new HyperlinkLabel("<a href='#'>" + CHOOSE_WORKDIR_LINK_TEXT + "</a>", // NOI18N
                                                        "<a href='#' color=\"" + colorText + "\">" + CHOOSE_WORKDIR_LINK_TEXT
                                                        + "</a>", // NOI18N
                                                        new Runnable() {
                public void run() {
                    JFileChooser chooser = getFileChooser();
                    chooser.setCurrentDirectory(new File(workingDirectoryTextField.getText().trim()));

                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        workingDirectoryTextField.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                }
            });
        workingDirectorySelectLink.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 4, 0, 3);
        globalSettingsPanel.add(workingDirectorySelectLink, constraints);

        // javaPlatformLabel
        javaPlatformLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(javaPlatformLabel, JAVA_PLATFORM_LABEL_TEXT);
        javaPlatformLabel.setToolTipText(STP_JPLATFORM_TOOLTIP);
        javaPlatformLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 19, 0, 0);
        globalSettingsPanel.add(javaPlatformLabel, constraints);

        // javaPlatformCombo
        javaPlatformCombo = new JComboBox(new Object[] { DO_NOT_OVERRIDE_STRING }) {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        javaPlatformLabel.setLabelFor(javaPlatformCombo);
        javaPlatformCombo.setToolTipText(STP_JPLATFORM_TOOLTIP);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        globalSettingsPanel.add(javaPlatformCombo, constraints);

        // vmArgumentsLabel
        vmArgumentsLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(vmArgumentsLabel, JVM_ARGUMENTS_LABEL_TEXT);
        vmArgumentsLabel.setToolTipText(STP_VMARGS_TOOLTIP);
        vmArgumentsLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 19, 8, 0);
        globalSettingsPanel.add(vmArgumentsLabel, constraints);

        // vmArgumentsTextField
        vmArgumentsTextField = new JTextField() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        vmArgumentsLabel.setLabelFor(vmArgumentsTextField);
        vmArgumentsTextField.setToolTipText(STP_VMARGS_TOOLTIP);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 8, 0);
        globalSettingsPanel.add(vmArgumentsTextField, constraints);

        // fillerPanel
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(Utils.createFillerPanel(), constraints);
    }

    private void updateEnabling() {
        boolean enableOverride = overrideSettingsCheckbox.isSelected() && overrideSettingsCheckbox.isEnabled();
        workingDirectoryLabel.setEnabled(enableOverride);
        workingDirectoryTextField.setEnabled(enableOverride);
        workingDirectorySelectLink.setEnabled(enableOverride);
        javaPlatformLabel.setEnabled(enableOverride);
        javaPlatformCombo.setEnabled(enableOverride);
        vmArgumentsLabel.setEnabled(enableOverride);
        vmArgumentsTextField.setEnabled(enableOverride);

        if (profileFrameworkCheckbox.isSelected()) {
            profileSpawnedThreadsCheckbox.setSelected(true);
            profileSpawnedThreadsCheckbox.setEnabled(false);
            instrumentationSchemeCombo.setSelectedItem(SCHEME_COMBOBOX_ITEM_TOTAL);
            instrumentationSchemeCombo.setEnabled(false);
        } else {
            profileSpawnedThreadsCheckbox.setEnabled(methodsTrackingLabel.isEnabled()); // Just a hack to detect settings for preset (always disabled)
            instrumentationSchemeCombo.setEnabled(methodsTrackingLabel.isEnabled()); // Just a hack to detect settings for preset (always disabled)
        }
    }
    
    private void checkWorkingDirectory() {
        String workDir = workingDirectoryTextField.getText().trim();
        if (workDir.length() == 0 || new File(workDir).exists()) {
            workingDirectoryTextField.setForeground(UIManager.getColor("TextField.foreground")); // NOI18N
        } else {
            workingDirectoryTextField.setForeground(Color.RED);
        }
    }

    // --- Private implementation ------------------------------------------------
    private void updateJavaPlatformCombo(String platformNameToSelect) {
        List<JavaPlatform> supportedPlatforms = JavaPlatformSelector.getSupportedPlatforms();
        String[] supportedPlatformNames = new String[supportedPlatforms.size() + 1];
        supportedPlatformNames[0] = DO_NOT_OVERRIDE_STRING;

        for (int i = 1; i < supportedPlatformNames.length; i++) {
            supportedPlatformNames[i] = supportedPlatforms.get(i - 1).getDisplayName();
        }

        javaPlatformCombo.setModel(new DefaultComboBoxModel(supportedPlatformNames));

        if (platformNameToSelect != null) {
            javaPlatformCombo.setSelectedItem(platformNameToSelect);
        } else {
            javaPlatformCombo.setSelectedIndex(0);
        }
    }
}

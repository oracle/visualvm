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

import javax.swing.event.DocumentEvent;
import org.netbeans.api.java.platform.JavaPlatform;
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
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class MonitorSettingsAdvancedPanel extends DefaultSettingsPanel implements HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String DO_NOT_OVERRIDE_STRING = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                             "MonitorSettingsAdvancedPanel_DoNotOverrideString"); // NOI18N
    private static final String CHOOSE_WORKDIR_DIALOG_CAPTION = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                                    "MonitorSettingsAdvancedPanel_ChooseWorkDirDialogCaption"); // NOI18N
    private static final String GLOBAL_SETTINGS_CAPTION = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                              "MonitorSettingsAdvancedPanel_GlobalSettingsCaption"); // NOI18N
    private static final String OVERRIDE_SETTINGS_CHECKBOX_TEXT = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                                      "MonitorSettingsAdvancedPanel_OverrideSettingsCheckboxText"); // NOI18N
    private static final String WORKDIR_LABEL_TEXT = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                         "MonitorSettingsAdvancedPanel_WorkDirLabelText"); // NOI18N
    private static final String CHOOSE_WORKDIR_LINK_TEXT = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                               "MonitorSettingsAdvancedPanel_ChooseWorkDirLinkText"); // NOI18N
    private static final String JAVA_PLATFORM_LABEL_TEXT = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                               "MonitorSettingsAdvancedPanel_JavaPlatformLabelText"); // NOI18N
    private static final String JVM_ARGUMENTS_LABEL_TEXT = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                               "MonitorSettingsAdvancedPanel_JvmArgumentsLabelText"); // NOI18N
    private static final String STP_OVERRIDE_TOOLTIP = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                           "StpOverrideTooltip"); // NOI18N
    private static final String STP_WORKDIR_TOOLTIP = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class, "StpWorkDirTooltip"); // NOI18N
    private static final String STP_JPLATFORM_TOOLTIP = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class,
                                                                            "StpJPlatformTooltip"); // NOI18N
    private static final String STP_VMARGS_TOOLTIP = NbBundle.getMessage(MonitorSettingsAdvancedPanel.class, "StpVmArgsTooltip"); // NOI18N
                                                                                                                                  // -----
    private static final String HELP_CTX_KEY = "MonitorSettings.Advanced.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HyperlinkLabel workingDirectorySelectLink;
    private JCheckBox overrideSettingsCheckbox;
    private JComboBox javaPlatformCombo;
    private JLabel javaPlatformLabel;
    private JLabel vmArgumentsLabel;
    private JLabel workingDirectoryLabel;

    // --- UI components declaration ---------------------------------------------
    private JPanel globalSettingsPanel;
    private JTextField vmArgumentsTextField;
    private JTextField workingDirectoryTextField;
    private WeakReference<JFileChooser> workingDirectoryChooserReference;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public MonitorSettingsAdvancedPanel() {
        super();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
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
        JPanel contents = new MonitorSettingsAdvancedPanel();
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

        // globalSettingsPanel
        globalSettingsPanel = new JPanel(new GridBagLayout());
        globalSettingsPanel.setOpaque(false);
        globalSettingsPanel.setBorder(BorderFactory.createTitledBorder(GLOBAL_SETTINGS_CAPTION));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
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
        workingDirectorySelectLink = new HyperlinkLabel("<a href='#'>" + CHOOSE_WORKDIR_LINK_TEXT + "</a>", //NOI18N
                                                        "<a href='#' color=\"" + colorText + "\">" + CHOOSE_WORKDIR_LINK_TEXT
                                                        + "</a>", //NOI18N
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
        constraints.gridy = 1;
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

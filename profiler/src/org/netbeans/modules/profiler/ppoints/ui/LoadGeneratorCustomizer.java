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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.api.project.Project;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.spi.LoadGenPlugin;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.lib.profiler.ui.UIUtils;


/**
 *
 * @author Jiri Sedlacek
 */
public class LoadGeneratorCustomizer extends ValidityAwarePanel implements ActionListener, ChangeListener, DocumentListener,
                                                                           ValidityListener, HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NAME_LABEL_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                      "LoadGeneratorCustomizer_NameLabelText"); // NOI18N
    private static final String SETTINGS_LABEL_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                          "LoadGeneratorCustomizer_SettingsLabelText"); // NOI18N
    private static final String SCRIPT_LABEL_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                        "LoadGeneratorCustomizer_ScriptLabelText"); // NOI18N
    private static final String BROWSE_BUTTON_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                         "LoadGeneratorCustomizer_BrowseButtonText"); // NOI18N
    private static final String STOP_LABEL_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                      "LoadGeneratorCustomizer_StopLabelText"); // NOI18N
    private static final String DEFINE_RADIO_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                        "LoadGeneratorCustomizer_DefineRadioText"); // NOI18N
    private static final String STOP_RADIO_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                      "LoadGeneratorCustomizer_StopRadioText"); // NOI18N
    private static final String LOCATION_BEGIN_LABEL_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                                "LoadGeneratorCustomizer_LocationBeginLabelText"); // NOI18N
    private static final String LOCATION_END_LABEL_TEXT = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                              "LoadGeneratorCustomizer_LocationEndLabelText"); // NOI18N
    private static final String CHOOSE_SCRIPT_DIALOG_CAPTION = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                                   "LoadGeneratorCustomizer_ChooseScriptDialogCaption"); // NOI18N
    private static final String SCRIPT_FIELD_ACCESS_DESCR = NbBundle.getMessage(LoadGeneratorCustomizer.class,
                                                                                "LoadGeneratorCustomizer_ScriptFieldAccessDescr"); // NOI18N
                                                                                                                                   // -----
    private static final String HELP_CTX_KEY = "LoadGeneratorCustomizer.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static int defaultTextComponentHeight = -1;
    private static JFileChooser fileChooser;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JButton scriptButton;
    private JLabel captionLabel;
    private JLabel locationBeginHeaderLabel;
    private JLabel locationEndHeaderLabel;
    private JLabel nameLabel;
    private JLabel scriptLabel;
    private JLabel settingsHeaderLabel;
    private JLabel stopLabel;
    private JPanel captionPanel;
    private JPanel firstLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JPanel secondLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JPanel thirdLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JRadioButton stopDefineRadio;
    private JRadioButton stopOnStopRadio;
    private JSeparator locationBeginHeaderSeparator;
    private JSeparator locationEndHeaderSeparator;
    private JSeparator settingsHeaderSeparator;
    private JTextField nameTextField;
    private JTextField scriptTextField;
    private LocationCustomizer locationBeginCustomizer;
    private LocationCustomizer locationEndCustomizer;
    private Project project;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LoadGeneratorCustomizer(String caption, Icon icon) {
        initComponents(caption, icon);
        normalizeCaptionAreaWidth();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public Component getInitialFocusTarget() {
        return nameTextField;
    }

    public void setPPEndLocation(CodeProfilingPoint.Location location) {
        if (location == null) {
            stopOnStopRadio.setSelected(true);
            locationEndCustomizer.setPPLocation(CodeProfilingPoint.Location.EMPTY);
        } else {
            stopDefineRadio.setSelected(true);
            locationEndCustomizer.setPPLocation(location);
        }
    }

    public CodeProfilingPoint.Location getPPEndLocation() {
        if (stopOnStopRadio.isSelected()) {
            return null;
        } else {
            return locationEndCustomizer.getPPLocation();
        }
    }

    public void setPPName(String ppName) {
        nameTextField.setText(ppName);
    }

    public String getPPName() {
        return nameTextField.getText();
    }

    public void setPPStartLocation(CodeProfilingPoint.Location location) {
        locationBeginCustomizer.setPPLocation(location);
    }

    public CodeProfilingPoint.Location getPPStartLocation() {
        return locationBeginCustomizer.getPPLocation();
    }

    public int getPreferredCaptionAreaWidth() {
        int ownCaptionAreaWidth = nameLabel.getPreferredSize().width - 12; // nameLabel starts at 8, locationCustomizer at 20 => -12
        ownCaptionAreaWidth = Math.max(ownCaptionAreaWidth, scriptLabel.getPreferredSize().width);
        ownCaptionAreaWidth = Math.max(ownCaptionAreaWidth, stopLabel.getPreferredSize().width);

        return Math.max(ownCaptionAreaWidth, locationBeginCustomizer.getPreferredCaptionAreaWidth());
    }

    public void setProject(Project aProject) {
        project = aProject;
    }

    public void setScriptFile(String fileName) {
        scriptTextField.setText(fileName);
    }

    public String getScriptFile() {
        return scriptTextField.getText();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == scriptButton) {
            JFileChooser fileChooser = getFileChooser();
            File currentDir = null;
            String scriptPath = scriptTextField.getText();

            if (scriptPath.length() > 0) {
                fileChooser.setCurrentDirectory(new File(scriptPath).getParentFile());
            } else {
                fileChooser.setCurrentDirectory(FileUtil.toFile(project.getProjectDirectory()));
            }

            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                scriptTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    public void changedUpdate(DocumentEvent e) {
        updateValidity();
    }

    //  private int getDefaultTextComponentHeight() {
    //    if (defaultTextComponentHeight == -1) defaultTextComponentHeight = new JComboBox().getPreferredSize().height;
    //    return defaultTextComponentHeight;
    //  }
    public void initComponents(String caption, Icon icon) {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // captionPanel
        captionPanel = new JPanel(new BorderLayout(0, 0));
        captionPanel.setOpaque(true);
        captionPanel.setBackground(UIUtils.getProfilerResultsBackground());

        // captionLabel
        captionLabel = new JLabel(caption, icon, SwingConstants.LEADING);
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD));
        captionLabel.setOpaque(false);
        captionLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 6));
        captionPanel.add(captionLabel, BorderLayout.WEST);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 16, 0);
        add(captionPanel, constraints);

        // --- next row ----------------------------------------------------------

        // nameLabel
        nameLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, NAME_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 8, 10, 5);
        add(nameLabel, constraints);

        // firstLineCaptionSpacer
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(firstLineCaptionSpacer, constraints);

        // nameTextField
        nameTextField = new JTextField("") { // NOI18N
                public Dimension getPreferredSize() {
                    return (LoadGeneratorCustomizer.this.getParent() instanceof JViewport) ? getMinimumSize()
                                                                                           : new Dimension(400,
                                                                                                           super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return new Dimension(super.getMinimumSize().width, super.getPreferredSize().height);
                }
            };
        nameLabel.setLabelFor(nameTextField);
        nameTextField.getDocument().addDocumentListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 10, 13);
        add(nameTextField, constraints);

        // --- next row ----------------------------------------------------------
        JPanel settingsHeaderContainer = new JPanel(new GridBagLayout());

        // settingsHeaderLabel
        settingsHeaderLabel = new JLabel(SETTINGS_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        settingsHeaderContainer.add(settingsHeaderLabel, constraints);

        // settingsHeaderSeparator
        settingsHeaderSeparator = new JSeparator() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        settingsHeaderContainer.add(settingsHeaderSeparator, constraints);

        // locationHeaderContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 8, 5, 8);
        add(settingsHeaderContainer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel scriptSettingsContainer = new JPanel(new GridBagLayout());

        // scriptLabel
        scriptLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(scriptLabel, SCRIPT_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        scriptSettingsContainer.add(scriptLabel, constraints);

        // secondLineCaptionSpacer
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        scriptSettingsContainer.add(secondLineCaptionSpacer, constraints);

        // scriptTextField
        scriptTextField = new JTextField("") { // NOI18N
                public Dimension getPreferredSize() {
                    return new Dimension(super.getMinimumSize().width, super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        scriptLabel.setLabelFor(scriptTextField);
        scriptTextField.getAccessibleContext().setAccessibleDescription(SCRIPT_FIELD_ACCESS_DESCR);
        scriptTextField.getDocument().addDocumentListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 5);
        scriptSettingsContainer.add(scriptTextField, constraints);

        // scriptButton
        scriptButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(scriptButton, BROWSE_BUTTON_TEXT);
        scriptButton.addActionListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        scriptSettingsContainer.add(scriptButton, constraints);

        // scriptSettingsContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 20, 5, 13);
        add(scriptSettingsContainer, constraints);

        // --- next row ----------------------------------------------------------
        ButtonGroup stopRadiosGroup = new ButtonGroup();
        JPanel stopSettingsContainer = new JPanel(new GridBagLayout());

        // stopLabel
        stopLabel = new JLabel(STOP_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        stopSettingsContainer.add(stopLabel, constraints);

        // thirdLineCaptionSpacer
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        stopSettingsContainer.add(thirdLineCaptionSpacer, constraints);

        // stopDefineRadio
        stopDefineRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(stopDefineRadio, DEFINE_RADIO_TEXT);
        stopRadiosGroup.add(stopDefineRadio);
        stopDefineRadio.addChangeListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        stopSettingsContainer.add(stopDefineRadio, constraints);

        // stopOnStopRadio
        stopOnStopRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(stopOnStopRadio, STOP_RADIO_TEXT);
        stopOnStopRadio.addChangeListener(this);
        stopRadiosGroup.add(stopOnStopRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        stopSettingsContainer.add(stopOnStopRadio, constraints);

        // measureSettingsContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 20, 10, 13);
        add(stopSettingsContainer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel locationBeginHeaderContainer = new JPanel(new GridBagLayout());

        // locationBeginHeaderLabel
        locationBeginHeaderLabel = new JLabel(LOCATION_BEGIN_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        locationBeginHeaderContainer.add(locationBeginHeaderLabel, constraints);

        // locationBeginHeaderSeparator
        locationBeginHeaderSeparator = new JSeparator() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        locationBeginHeaderContainer.add(locationBeginHeaderSeparator, constraints);

        // locationBeginHeaderContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 8, 5, 8);
        add(locationBeginHeaderContainer, constraints);

        // --- next row ----------------------------------------------------------

        // locationBeginCustomizer
        locationBeginCustomizer = new LocationCustomizer();
        locationBeginCustomizer.addValidityListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 20, 12, 13);
        add(locationBeginCustomizer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel locationEndHeaderContainer = new JPanel(new GridBagLayout());

        // locationEndHeaderLabel
        locationEndHeaderLabel = new JLabel(LOCATION_END_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        locationEndHeaderContainer.add(locationEndHeaderLabel, constraints);

        // locationEndHeaderSeparator
        locationEndHeaderSeparator = new JSeparator() {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        locationEndHeaderContainer.add(locationEndHeaderSeparator, constraints);

        // locationEndHeaderContainer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 8, 5, 8);
        add(locationEndHeaderContainer, constraints);

        // --- next row ----------------------------------------------------------

        // locationEndCustomizer
        locationEndCustomizer = new LocationCustomizer();
        locationEndCustomizer.resetMnemonic();
        locationEndCustomizer.addValidityListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 20, 20, 13);
        add(locationEndCustomizer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel fillerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(fillerPanel, constraints);
    }

    public void insertUpdate(DocumentEvent e) {
        updateValidity();
    }

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

        LoadGeneratorCustomizer main = new LoadGeneratorCustomizer("Load Generator", null); // NOI18N
        main.addValidityListener(new ValidityListener() {
                public void validityChanged(boolean isValid) {
                    System.err.println(">>> Validity changed to " + isValid);
                } // NOI18N
            });

        JFrame frame = new JFrame("Customize Profiling Point"); // NOI18N
        frame.getContentPane().add(main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void normalizeCaptionAreaWidth() {
        int requiredCaptionAreaWidth1 = nameLabel.getPreferredSize().width - 12; // nameLabel starts at 8, locationCustomizer at 20 => -12
        int diffCaptionAreaWidth1 = getPreferredCaptionAreaWidth() - requiredCaptionAreaWidth1;
        int normalizedCaptionAreaWidth1 = (diffCaptionAreaWidth1 > 0) ? diffCaptionAreaWidth1 : 0;

        int requiredCaptionAreaWidth2 = scriptLabel.getPreferredSize().width;
        int diffCaptionAreaWidth2 = getPreferredCaptionAreaWidth() - requiredCaptionAreaWidth2;
        int normalizedCaptionAreaWidth2 = (diffCaptionAreaWidth2 > 0) ? diffCaptionAreaWidth2 : 0;

        int requiredCaptionAreaWidth3 = stopLabel.getPreferredSize().width;
        int diffCaptionAreaWidth3 = getPreferredCaptionAreaWidth() - requiredCaptionAreaWidth3;
        int normalizedCaptionAreaWidth3 = (diffCaptionAreaWidth3 > 0) ? diffCaptionAreaWidth3 : 0;

        firstLineCaptionSpacer.setBorder(BorderFactory.createEmptyBorder(0, normalizedCaptionAreaWidth1, 0, 0));
        secondLineCaptionSpacer.setBorder(BorderFactory.createEmptyBorder(0, normalizedCaptionAreaWidth2, 0, 0));
        thirdLineCaptionSpacer.setBorder(BorderFactory.createEmptyBorder(0, normalizedCaptionAreaWidth3, 0, 0));
        locationBeginCustomizer.normalizeCaptionAreaWidth(getPreferredCaptionAreaWidth());
        locationEndCustomizer.normalizeCaptionAreaWidth(getPreferredCaptionAreaWidth());
    }

    public void removeUpdate(DocumentEvent e) {
        updateValidity();
    }

    public void stateChanged(ChangeEvent e) {
        if ((e.getSource() == stopDefineRadio) || (e.getSource() == stopOnStopRadio)) {
            boolean selected = stopDefineRadio.isSelected();
            locationEndCustomizer.setEnabled(selected);
            locationEndHeaderLabel.setEnabled(selected);
            locationEndHeaderSeparator.setEnabled(selected);
            updateValidity();
        }
    }

    public void validityChanged(boolean isValid) {
        updateValidity();
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setDialogTitle(CHOOSE_SCRIPT_DIALOG_CAPTION);

            chooser.setFileFilter(new FileFilter() {
                    private Set<String> extensions = new HashSet<String>();

                    {
                        LoadGenPlugin lg = Lookup.getDefault().lookup(LoadGenPlugin.class);
                        extensions = lg.getSupportedExtensions();
                    }

                    public boolean accept(File f) {
                        return f.isDirectory() || extensions.contains(FileUtil.getExtension(f.getPath()));
                    }

                    public String getDescription() {
                        return NbBundle.getMessage(LoadGeneratorCustomizer.class, "LoadGeneratorCustomizer_SupportedFiles"); // NOI18N
                    }
                });

            fileChooser = chooser;
        }

        return fileChooser;
    }

    private boolean isNameEmpty() {
        return nameTextField.getText().trim().length() == 0;
    }

    private boolean isScriptValid() {
        String fileName = scriptTextField.getText();
        File file = new File(fileName);

        if (file.exists() && file.isFile()) {
            LoadGenPlugin lg = Lookup.getDefault().lookup(LoadGenPlugin.class);

            if (lg == null) {
                return false;
            }

            String ext = FileUtil.getExtension(scriptTextField.getText());

            return lg.getSupportedExtensions().contains(ext);
        }

        return false;
    }

    private boolean areEndLocationSettingsValid() {
        return stopOnStopRadio.isSelected() || locationEndCustomizer.areSettingsValid();
    }

    private void updateValidity() {
        boolean isValid = !isNameEmpty() && isScriptValid() && locationBeginCustomizer.areSettingsValid()
                          && areEndLocationSettingsValid();

        if (isValid != areSettingsValid()) {
            fireValidityChanged(isValid);
        }
    }
}

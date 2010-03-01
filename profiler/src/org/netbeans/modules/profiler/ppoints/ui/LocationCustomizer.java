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

import java.awt.Color;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.util.NbBundle;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;


/**
 *
 * @author Jiri Sedlacek
 */
public class LocationCustomizer extends ValidityAwarePanel implements ActionListener, ChangeListener, DocumentListener,
                                                                      HierarchyListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class HTMLButton extends JButton {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setEnabled(boolean enabled) {
            setForeground(enabled ? SystemColor.textText : SystemColor.textInactiveText);
            super.setEnabled(enabled);
        }

        //    public HTMLButton(String text) {
        //      super("<html><center>" + text.replaceAll("\\n", "<br>") + "</center></html>"); // NOI18N
        //      getAccessibleContext().setAccessibleName(text);
        //    }
        public void setText(String value) {
            super.setText("<html><center><nobr>" + value.replace("\\n", "<br>") + "</nobr></center></html>"); // NOI18N
            getAccessibleContext().setAccessibleName(value);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String FILE_LABEL_TEXT = NbBundle.getMessage(LocationCustomizer.class, "LocationCustomizer_FileLabelText"); // NOI18N
    private static final String BROWSE_BUTTON_TEXT = NbBundle.getMessage(LocationCustomizer.class,
                                                                         "LocationCustomizer_BrowseButtonText"); // NOI18N
    private static final String CURRENT_LINE_BUTTON_TEXT = NbBundle.getMessage(LocationCustomizer.class,
                                                                               "LocationCustomizer_CurrentLineButtonText"); // NOI18N
    private static final String LINE_LABEL_TEXT = NbBundle.getMessage(LocationCustomizer.class, "LocationCustomizer_LineLabelText"); // NOI18N
    private static final String BEGIN_RADIO_TEXT = NbBundle.getMessage(LocationCustomizer.class,
                                                                       "LocationCustomizer_BeginRadioText"); // NOI18N
    private static final String END_RADIO_TEXT = NbBundle.getMessage(LocationCustomizer.class, "LocationCustomizer_EndRadioText"); // NOI18N
    private static final String OFFSET_RADIO_TEXT = NbBundle.getMessage(LocationCustomizer.class,
                                                                        "LocationCustomizer_OffsetRadioText"); // NOI18N
    private static final String CHOOSE_FILE_DIALOG_CAPTION = NbBundle.getMessage(LocationCustomizer.class,
                                                                                 "LocationCustomizer_ChooseFileDialogCaption"); // NOI18N
    private static final String FILE_DIALOG_FILTER_NAME = NbBundle.getMessage(LocationCustomizer.class,
                                                                              "LocationCustomizer_FileDialogFilterName"); // NOI18N
                                                                                                                          // -----

    // --- Implementation --------------------------------------------------------
    private static int defaultTextComponentHeight = -1;
    private static JFileChooser fileChooser;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JButton fileButton;
    private JButton fromEditorButton;
    private JLabel fileLabel;
    private JLabel lineLabel;
    private JPanel firstLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JPanel secondLineCaptionSpacer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private JRadioButton lineBeginRadio;
    private JRadioButton lineEndRadio;
    private JRadioButton lineOffsetRadio;
    private JSeparator fromEditorSeparator;
    private JSpinner lineNumberSpinner;
    private JSpinner lineOffsetSpinner;
    private JTextField fileTextField;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LocationCustomizer() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        fileLabel.setEnabled(enabled);
        fileTextField.setEnabled(enabled);
        fileButton.setEnabled(enabled);
        lineLabel.setEnabled(enabled);
        lineNumberSpinner.setEnabled(enabled);
        lineBeginRadio.setEnabled(enabled);
        lineEndRadio.setEnabled(enabled);
        lineOffsetRadio.setEnabled(enabled);
        lineOffsetSpinner.setEnabled((enabled == false) ? false : lineOffsetRadio.isSelected());
        fromEditorButton.setEnabled(enabled);
    }

    public void setPPLocation(CodeProfilingPoint.Location location) {
        fileTextField.setText(location.getFile());
        lineNumberSpinner.setValue(location.getLine());

        int offset = location.getOffset();

        if (offset == CodeProfilingPoint.Location.OFFSET_START) {
            lineBeginRadio.setSelected(true);
        } else if (offset == CodeProfilingPoint.Location.OFFSET_END) {
            lineEndRadio.setSelected(true);
        } else {
            lineOffsetRadio.setSelected(true);
            lineOffsetSpinner.setValue(offset);
        }
    }

    public CodeProfilingPoint.Location getPPLocation() {
        int offset = ((Integer) lineOffsetSpinner.getValue()).intValue();

        if (lineBeginRadio.isSelected()) {
            offset = CodeProfilingPoint.Location.OFFSET_START;
        } else if (lineEndRadio.isSelected()) {
            offset = CodeProfilingPoint.Location.OFFSET_END;
        }

        return new CodeProfilingPoint.Location(fileTextField.getText(), ((Integer) lineNumberSpinner.getValue()).intValue(),
                                               offset);
    }

    public int getPreferredCaptionAreaWidth() {
        return Math.max(fileLabel.getPreferredSize().width, lineLabel.getPreferredSize().width);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fileButton) {
            JFileChooser fileChooser = getFileChooser();
            fileChooser.setCurrentDirectory(new File(fileTextField.getText()));

            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getSource() == fromEditorButton) {
            if (lineBeginRadio.isSelected()) {
                setPPLocation(Utils.getCurrentLocation(CodeProfilingPoint.Location.OFFSET_START));
            } else if (lineEndRadio.isSelected()) {
                setPPLocation(Utils.getCurrentLocation(CodeProfilingPoint.Location.OFFSET_END));
            } else {
                setPPLocation(Utils.getCurrentLocation(((Integer) lineOffsetSpinner.getValue()).intValue()));
            }
        }
    }

    public void changedUpdate(DocumentEvent e) {
        updateValidity();
    }

    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            Window window = SwingUtilities.getWindowAncestor(this);

            if (window instanceof Dialog && !((Dialog) window).isModal()) {
                showFromEditor();
            } else {
                hideFromEditor();
            }
        }
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

        LocationCustomizer main = new LocationCustomizer();

        //    main.addValidityListener(new ValidityListener() {
        //      public void validityChanged(boolean isValid) { System.err.println(">>> Validity changed to " + isValid); }
        //    });
        JFrame frame = new JFrame("Customize Profiling Point"); // NOI18N
        frame.getContentPane().add(main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void normalizeCaptionAreaWidth(int captionAreaWidth) {
        int requiredCaptionAreaWidth = getPreferredCaptionAreaWidth();
        int diffCaptionAreaWidth = captionAreaWidth - requiredCaptionAreaWidth;
        int normalizedCaptionAreaWidth = (diffCaptionAreaWidth > 0) ? diffCaptionAreaWidth : 0;

        firstLineCaptionSpacer.setBorder(BorderFactory.createEmptyBorder(0, normalizedCaptionAreaWidth, 0, 0));
        secondLineCaptionSpacer.setBorder(BorderFactory.createEmptyBorder(0, normalizedCaptionAreaWidth, 0, 0));
    }

    public void removeUpdate(DocumentEvent e) {
        updateValidity();
    }

    public void resetMnemonic() {
        fileLabel.setDisplayedMnemonic(0);
        fileLabel.setDisplayedMnemonicIndex(-1);

        fileButton.setMnemonic(0);
        fileButton.setDisplayedMnemonicIndex(-1);

        fromEditorButton.setMnemonic(0);
        fromEditorButton.setDisplayedMnemonicIndex(-1);

        lineLabel.setDisplayedMnemonic(0);
        lineLabel.setDisplayedMnemonicIndex(-1);

        lineBeginRadio.setMnemonic(0);
        lineBeginRadio.setDisplayedMnemonicIndex(-1);

        lineEndRadio.setMnemonic(0);
        lineEndRadio.setDisplayedMnemonicIndex(-1);

        lineOffsetRadio.setMnemonic(0);
        lineOffsetRadio.setDisplayedMnemonicIndex(-1);
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == lineOffsetRadio) {
            lineOffsetSpinner.setEnabled(lineOffsetRadio.isSelected());
        }
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setDialogTitle(CHOOSE_FILE_DIALOG_CAPTION);
            chooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
                    } // NOI18N

                    public String getDescription() {
                        return FILE_DIALOG_FILTER_NAME;
                    }
                });
            fileChooser = chooser;
        }

        return fileChooser;
    }

    private boolean isFileValid() {
        File file = new File(fileTextField.getText());

        return file.exists() && file.isFile();
    }

    private void hideFromEditor() {
        fromEditorSeparator.setVisible(false);
        fromEditorButton.setVisible(false);
    }

    //  private int getDefaultTextComponentHeight() {
    //    if (defaultTextComponentHeight == -1) defaultTextComponentHeight = new JComboBox().getPreferredSize().height;
    //    return defaultTextComponentHeight;
    //  }
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // fileLabel
        fileLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(fileLabel, FILE_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 5);
        add(fileLabel, constraints);

        // firstLineCaptionSpacer
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(firstLineCaptionSpacer, constraints);

        // fileTextField
        fileTextField = new JTextField("") { // NOI18N
                public Dimension getPreferredSize() {
                    return new Dimension(super.getMinimumSize().width, super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        fileLabel.setLabelFor(fileTextField);
        fileTextField.getDocument().addDocumentListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 5, 5);
        add(fileTextField, constraints);

        // fileButton
        fileButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(fileButton, BROWSE_BUTTON_TEXT);
        fileButton.addActionListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 0);
        add(fileButton, constraints);

        // fromEditorSeparator
        fromEditorSeparator = new JSeparator(SwingConstants.VERTICAL) {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.insets = new Insets(0, 8, 0, 0);
        add(fromEditorSeparator, constraints);

        // fromEditorButton
        fromEditorButton = new HTMLButton();
        org.openide.awt.Mnemonics.setLocalizedText(fromEditorButton, CURRENT_LINE_BUTTON_TEXT);
        fromEditorButton.addActionListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.insets = new Insets(0, 8, 0, 0);
        add(fromEditorButton, constraints);

        // --- next row ----------------------------------------------------------
        ButtonGroup lineRadiosGroup = new ButtonGroup();

        // lineLabel
        lineLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(lineLabel, LINE_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 5);
        add(lineLabel, constraints);

        // secondLineCaptionSpacer
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(secondLineCaptionSpacer, constraints);

        JPanel lineSettingsContainer = new JPanel(new GridBagLayout());

        // lineNumberSpinner
        lineNumberSpinner = new JExtendedSpinner() {
                public Dimension getPreferredSize() {
                    return new Dimension(Math.max(super.getPreferredSize().width, 55),
                                         org.netbeans.modules.profiler.ui.stp.Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        lineLabel.setLabelFor(lineNumberSpinner);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        lineSettingsContainer.add(lineNumberSpinner, constraints);

        // lineBeginRadio
        lineBeginRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(lineBeginRadio, BEGIN_RADIO_TEXT);
        lineBeginRadio.getAccessibleContext().setAccessibleDescription(LINE_LABEL_TEXT + BEGIN_RADIO_TEXT);
        lineRadiosGroup.add(lineBeginRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 3);
        lineSettingsContainer.add(lineBeginRadio, constraints);

        // lineEndRadio
        lineEndRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(lineEndRadio, END_RADIO_TEXT);
        lineEndRadio.getAccessibleContext().setAccessibleDescription(LINE_LABEL_TEXT + END_RADIO_TEXT);
        lineRadiosGroup.add(lineEndRadio);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 3);
        lineSettingsContainer.add(lineEndRadio, constraints);

        // lineOffsetRadio
        lineOffsetRadio = new JRadioButton();
        org.openide.awt.Mnemonics.setLocalizedText(lineOffsetRadio, OFFSET_RADIO_TEXT);
        lineOffsetRadio.getAccessibleContext().setAccessibleDescription(LINE_LABEL_TEXT + OFFSET_RADIO_TEXT);
        lineRadiosGroup.add(lineOffsetRadio);
        lineOffsetRadio.addChangeListener(this);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        //      lineSettingsContainer.add(lineOffsetRadio, constraints);

        // Placeholder for lineOffsetRadio and lineOffsetSpinner
        lineSettingsContainer.add(new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0)) {
                public Dimension getPreferredSize() {
                    return new Dimension(lineOffsetRadio.getPreferredSize().width + lineOffsetSpinner.getPreferredSize().width,
                                         Math.max(lineOffsetRadio.getPreferredSize().height,
                                                  lineOffsetSpinner.getPreferredSize().height));
                }
            }, constraints);

        // lineOffsetSpinner
        lineOffsetSpinner = new JExtendedSpinner() {
                public Dimension getPreferredSize() {
                    return new Dimension(Math.max(super.getPreferredSize().width, 55),
                                         org.netbeans.modules.profiler.ui.stp.Utils.getDefaultSpinnerHeight());
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        //      lineSettingsContainer.add(lineOffsetSpinner, constraints);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(lineSettingsContainer, constraints);

        // --- next row ----------------------------------------------------------
        JPanel fillerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(fillerPanel, constraints);

        addHierarchyListener(this);
    }

    private void showFromEditor() {
        fromEditorSeparator.setVisible(true);
        fromEditorButton.setVisible(true);
    }

    private void updateValidity() {
        boolean isValid = isFileValid();

        fileTextField.setForeground(isValid ? UIManager.getColor("TextField.foreground") : Color.RED); // NOI18N

        if (isValid != LocationCustomizer.this.areSettingsValid()) {
            fireValidityChanged(isValid);
        }
    }
}

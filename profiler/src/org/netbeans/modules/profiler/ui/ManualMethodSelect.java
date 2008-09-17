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

package org.netbeans.modules.profiler.ui;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.util.HelpCtx;


/**
 * The panel to enter method info (class/method name and signature) manually
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class ManualMethodSelect extends JPanel implements HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private final class MethodSelectDocumentListener implements DocumentListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void changedUpdate(final DocumentEvent e) {
            updateEnabledState();
        }

        public void insertUpdate(final DocumentEvent e) {
            updateEnabledState();
        }

        public void removeUpdate(final DocumentEvent e) {
            updateEnabledState();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SELECT_METHODS_DIALOG_CAPTION = NbBundle.getMessage(ManualMethodSelect.class,
                                                                                    "ManualMethodSelect_SelectMethodsDialogCaption" //NOI18N
    );
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(ManualMethodSelect.class, "ManualMethodSelect_OKButtonText"); //NOI18N
    private static final String CLASS_NAME_LABEL_TEXT = NbBundle.getMessage(ManualMethodSelect.class,
                                                                            "ManualMethodSelect_ClassNameLabelText"); //NOI18N
    private static final String METHOD_NAME_LABEL_TEXT = NbBundle.getMessage(ManualMethodSelect.class,
                                                                             "ManualMethodSelect_MethodNameLabelText"); //NOI18N
    private static final String METHOD_SIGNATURE_LABEL_TEXT = NbBundle.getMessage(ManualMethodSelect.class,
                                                                                  "ManualMethodSelect_MethodSignatureLabelText"); //NOI18N
    private static final String HINT_MSG = NbBundle.getMessage(ManualMethodSelect.class, "ManualMethodSelect_HintMsg"); //NOI18N
    private static final String CLASS_NAME_ACCESS_NAME = NbBundle.getMessage(ManualMethodSelect.class,
                                                                             "ManualMethodSelect_ClassNameAccessName"); //NOI18N
    private static final String METHOD_NAME_ACCESS_NAME = NbBundle.getMessage(ManualMethodSelect.class,
                                                                              "ManualMethodSelect_MethodNameAccessName"); //NOI18N
    private static final String METHOD_SIGNATURE_ACCESS_NAME = NbBundle.getMessage(ManualMethodSelect.class,
                                                                                   "ManualMethodSelect_MethodSignatureAccessName"); //NOI18N
                                                                                                                                    // -----
    
    private static final String HELP_CTX_KEY = "ManualMethodSelect.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    
    private static ManualMethodSelect mms;
    private static JButton okButton = new JButton(OK_BUTTON_TEXT);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HTMLTextArea hintArea;

    // -- Private implementation ------------------------------------------------------
    private JTextField className;
    private JTextField methodName;
    private JTextField methodSignature;
    private MethodSelectDocumentListener documentListener;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new form CodeFragmentSelect
     */
    private ManualMethodSelect() {
        documentListener = new MethodSelectDocumentListener();

        JLabel classNameLabel = new JLabel();
        className = new JTextField();

        JLabel methodNameLabel = new JLabel();
        methodName = new JTextField();

        JLabel methodSignatureLabel = new JLabel();
        methodSignature = new JTextField();
        hintArea = new HTMLTextArea() {
                public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                    return new Dimension(1, super.getPreferredSize().height);
                }
            };

        setLayout(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        GridBagConstraints fieldConstraints = new GridBagConstraints();

        labelConstraints.insets = new Insets(5, 0, 0, 0);
        labelConstraints.anchor = GridBagConstraints.WEST;

        fieldConstraints.insets = new Insets(5, 5, 0, 0);
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;

        classNameLabel.setLabelFor(className);
        org.openide.awt.Mnemonics.setLocalizedText(classNameLabel, CLASS_NAME_LABEL_TEXT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        add(classNameLabel, labelConstraints);

        className.getAccessibleContext().setAccessibleName(CLASS_NAME_ACCESS_NAME);
        className.setPreferredSize(new Dimension(290, className.getPreferredSize().height));
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 0;
        add(className, fieldConstraints);

        methodNameLabel.setLabelFor(methodName);
        org.openide.awt.Mnemonics.setLocalizedText(methodNameLabel, METHOD_NAME_LABEL_TEXT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        add(methodNameLabel, labelConstraints);

        methodName.getAccessibleContext().setAccessibleName(METHOD_NAME_ACCESS_NAME);
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 1;
        add(methodName, fieldConstraints);

        methodSignatureLabel.setLabelFor(methodSignature);
        org.openide.awt.Mnemonics.setLocalizedText(methodSignatureLabel, METHOD_SIGNATURE_LABEL_TEXT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 2;
        add(methodSignatureLabel, labelConstraints);

        methodSignature.getAccessibleContext().setAccessibleName(METHOD_SIGNATURE_ACCESS_NAME);
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 2;
        add(methodSignature, fieldConstraints);

        Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
        Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                                                    panelBackground.getBlue() - 10);

        // fill space
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        add(new JPanel(), constraints);

        // hintArea
        hintArea.setText(HINT_MSG); // NOI18N
        hintArea.setEnabled(false);
        hintArea.setDisabledTextColor(Color.darkGray);
        hintArea.setBackground(hintBackground);
        hintArea.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(18, 0, 0, 0);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        add(hintArea, constraints);

        setBorder(new EmptyBorder(12, 12, 0, 12));

        className.getDocument().addDocumentListener(documentListener);
        methodName.getDocument().addDocumentListener(documentListener);
        methodSignature.getDocument().addDocumentListener(documentListener);

        updateEnabledState();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static ClientUtils.SourceCodeSelection selectMethod() {
        if (mms == null) {
            mms = new ManualMethodSelect();
        }

        final DialogDescriptor dd = new DialogDescriptor(mms, SELECT_METHODS_DIALOG_CAPTION, true,
                                                         new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                         DialogDescriptor.BOTTOM_ALIGN, null, null);

        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack(); // To properly layout HTML hint area
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            return mms.getSelectedMethod();
        } else {
            return null;
        }
    }

    private String getClassName() {
        return className.getText().trim(); //NOI18N
    }

    private String getMethodName() {
        String ret = methodName.getText().trim();

        if ("".equals(ret)) {
            return null; //NOI18N
        } else {
            return ret;
        }
    }

    private String getMethodSignature() {
        String ret = methodSignature.getText().trim();

        if ("".equals(ret)) {
            return null; //NOI18N
        } else {
            return ret;
        }
    }

    private ClientUtils.SourceCodeSelection getSelectedMethod() {
        return new ClientUtils.SourceCodeSelection(getClassName(), getMethodName(), getMethodSignature());
    }

    private void updateEnabledState() {
        boolean enabled = true;

        // package/class name cannot be empty
        if ("".equals(className.getText().trim())) {
            enabled = false; //NOI18N
        }
        // method name cannot be empty
        else if ("".equals(methodName.getText().trim())) {
            enabled = false; //NOI18N
        }
        // method signature cannot be empty
        else if ("".equals(methodSignature.getText().trim())) {
            enabled = false; //NOI18N
        }
        // try to format method as a kind of heuristics if provided info is correct
        // TODO: className, methodName and methodSignature should be validated separately and exactly!
        else {
            try {
                //      new MethodNameFormatter(getClassName(), getMethodName(), getMethodSignature()).getFullFormattedClassAndMethod();
                methodSignature.setForeground(UIManager.getColor("Label.foreground")); // NOI18N // formatting method could fail only because of incorrect signature
            } catch (Exception e) {
                methodSignature.setForeground(Color.red); // formatting method could fail only because of incorrect signature
                enabled = false;
            }
        }

        okButton.setEnabled(enabled);
    }
}

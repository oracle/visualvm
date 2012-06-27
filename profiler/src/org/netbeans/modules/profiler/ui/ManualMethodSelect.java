/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;


/**
 * The panel to enter method info (class/method name and signature) manually
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "ManualMethodSelect_SelectMethodsDialogCaption=Define New Profiling Root",
    "ManualMethodSelect_EditMethodDialogCaption=Edit Profiling Root",
    "ManualMethodSelect_OKButtonText=OK",
    "ManualMethodSelect_ClassNameLabelText=&Class Name:",
    "ManualMethodSelect_MethodNameLabelText=&Method Name:",
    "ManualMethodSelect_MethodSignatureLabelText=Method &VM Signature:",
    "ManualMethodSelect_HintMsg=The <b>Method Signature</b> must be in VM format, this means that for method \"String toString()\" the signature looks like \"()Ljava/lang/String;\".",
    "ManualMethodSelect_ClassNameAccessName=Enter class name here.",
    "ManualMethodSelect_MethodNameAccessName=Enter method name here.",
    "ManualMethodSelect_MethodSignatureAccessName=Enter method signature in VM format here."
})
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
    
    private static final String HELP_CTX_KEY = "ManualMethodSelect.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    
    private static ManualMethodSelect mms;
    private static JButton okButton = new JButton(Bundle.ManualMethodSelect_OKButtonText());

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
        org.openide.awt.Mnemonics.setLocalizedText(classNameLabel, Bundle.ManualMethodSelect_ClassNameLabelText());
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        add(classNameLabel, labelConstraints);

        className.getAccessibleContext().setAccessibleName(Bundle.ManualMethodSelect_ClassNameAccessName());
        className.setPreferredSize(new Dimension(290, className.getPreferredSize().height));
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 0;
        add(className, fieldConstraints);

        methodNameLabel.setLabelFor(methodName);
        org.openide.awt.Mnemonics.setLocalizedText(methodNameLabel, Bundle.ManualMethodSelect_MethodNameLabelText());
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        add(methodNameLabel, labelConstraints);

        methodName.getAccessibleContext().setAccessibleName(Bundle.ManualMethodSelect_MethodNameAccessName());
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 1;
        add(methodName, fieldConstraints);

        methodSignatureLabel.setLabelFor(methodSignature);
        org.openide.awt.Mnemonics.setLocalizedText(methodSignatureLabel, Bundle.ManualMethodSelect_MethodSignatureLabelText());
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 2;
        add(methodSignatureLabel, labelConstraints);

        methodSignature.getAccessibleContext().setAccessibleName(Bundle.ManualMethodSelect_MethodSignatureAccessName());
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
        hintArea.setText(Bundle.ManualMethodSelect_HintMsg());
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
        return selectMethod(null);
    }

    public static ClientUtils.SourceCodeSelection selectMethod(ClientUtils.SourceCodeSelection method) {
        if (mms == null) {
            mms = new ManualMethodSelect();
        }
        
        if (method != null) {
            mms.setClassName(method.getClassName());
            mms.setMethodName(method.getMethodName());
            mms.setMethodSignature(method.getMethodSignature());
        } else {
            mms.setClassName(""); // NOI18N
            mms.setMethodName(""); // NOI18N
            mms.setMethodSignature(""); // NOI18N
        }

        final DialogDescriptor dd = new DialogDescriptor(mms, method == null ? 
                                                         Bundle.ManualMethodSelect_SelectMethodsDialogCaption() :
                                                            Bundle.ManualMethodSelect_EditMethodDialogCaption(), 
                                                         true,
                                                         new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                         DialogDescriptor.BOTTOM_ALIGN, null, null);

        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.pack(); // To properly layout HTML hint area
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            return mms.getSelectedMethod();
        } else {
            return null;
        }
    }
    
    private void setClassName(String cName) {
        className.setText(ClientUtils.formatClassName(cName.trim()));
    }

    private String getClassName() {
        return ClientUtils.parseClassName(className.getText().trim(), true);
    }
    
    private void setMethodName(String mName) {
        methodName.setText(mName.trim());
    }

    private String getMethodName() {
        String ret = methodName.getText().trim();

        if ("".equals(ret)) {
            return null; //NOI18N
        } else {
            return ret;
        }
    }
    
    private void setMethodSignature(String mSignature) {
        methodSignature.setText(mSignature.trim());
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
        
        String fClassName = className.getText().trim();
        String fMethodName = methodName.getText().trim();
        String fMethodSignature = methodName.getText().trim();

        // package/class name cannot be empty
        if (fClassName.isEmpty()) {
            enabled = false;
        }

        // method-signature is filled or not
        if(fMethodName.isEmpty() != fMethodSignature.isEmpty()) {
            enabled = false;
        }

        // check format of class name
        String cm = ClientUtils.parseClassName(fClassName, fMethodName.isEmpty() && fMethodSignature.isEmpty());
        if(cm == null)
        {
            enabled = false;
            className.setForeground(Color.red);
        } else {
            className.setForeground(UIManager.getColor("Label.foreground")); // NOI18N
        }
        
        okButton.setEnabled(enabled);
    }
}

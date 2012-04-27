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

import org.netbeans.lib.profiler.ui.UIConstants;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "FindDialog_FindInResultsDialogCaption=Find in Results",
    "FindDialog_FindWhatLabelString=Find What:",
    "FindDialog_FindButtonName=Find",
    "FindDialog_FindWhatFieldAccessDescr=First item starting with this string will be found in results."
})
public class FindDialog extends JPanel implements HelpCtx.Provider{
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "ProfilerUiFindDialog.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static FindDialog defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JButton findButton;
    private JLabel findWhatLabel;
    private JTextField findWhatField;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of FindDialog */
    private FindDialog() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public HelpCtx getHelpCtx()
    {
        return HELP_CTX;
    }
    
    public static String getFindString() {
        final FindDialog findDialog = getDefault();
        findDialog.findWhatField.selectAll();

        final DialogDescriptor dd = new DialogDescriptor(findDialog, Bundle.FindDialog_FindInResultsDialogCaption(), true,
                                                         new Object[] { findDialog.findButton, DialogDescriptor.CANCEL_OPTION },
                                                         findDialog.findButton, DialogDescriptor.BOTTOM_ALIGN, null, null);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);

        if (dd.getValue() == findDialog.findButton) {
            return findDialog.findWhatField.getText();
        } else {
            return null;
        }
    }

    private static FindDialog getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new FindDialog();
        }

        return defaultInstance;
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        findWhatLabel = new JLabel();
        findWhatField = new JTextField();
        findButton = new JButton();

        setLayout(new GridBagLayout());

        // findWhatLabel
        findWhatLabel.setText(Bundle.FindDialog_FindWhatLabelString());
        findWhatLabel.setLabelFor(findWhatField);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 0, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(findWhatLabel, gridBagConstraints);

        // findWhatField
        findWhatField.getAccessibleContext().setAccessibleDescription(Bundle.FindDialog_FindWhatFieldAccessDescr());
        findWhatField.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        findWhatField.setSelectedTextColor(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        findWhatField.setPreferredSize(new Dimension(260, findWhatField.getPreferredSize().height));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 10);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(findWhatField, gridBagConstraints);
        findWhatField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateFindButton(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateFindButton(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateFindButton(); }
        });

        // findButton
        findButton.setText(Bundle.FindDialog_FindButtonName());
        updateFindButton();

        // panel filling bottom space
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(new JPanel(), gridBagConstraints);
    }
    
    private void updateFindButton() {
        findButton.setEnabled(!findWhatField.getText().trim().isEmpty());
    }
}

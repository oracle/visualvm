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

import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.ProgressDisplayer;
import org.openide.DialogDisplayer;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProgressDisplayer_ProgressString=Progress...",
    "ProgressDisplayer_CancelButtonText=Cancel"
})
public class ProfilerProgressDisplayer extends JPanel implements ProgressDisplayer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    protected static final String PROGRESS_STRING = Bundle.ProgressDisplayer_ProgressString();
    protected static final String CANCEL_BUTTON_TEXT = Bundle.ProgressDisplayer_CancelButtonText();
    // -----

    // --- Private implementation ------------------------------------------------
    private static ProfilerProgressDisplayer defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Dialog owner;
    private JButton cancelButton;

    // --- UI components declaration ---------------------------------------------
    private JLabel progressLabel;
    private JProgressBar progressBar;

    // --- Instance variables declaration ----------------------------------------
    private ProgressController controller;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProfilerProgressDisplayer() {}

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    
    @Override
    public ProgressDisplayer showProgress(String message) {
        return showProgress(message, null);
    }

    @Override
    public ProgressDisplayer showProgress(String message, ProgressController controller) {
        return showProgress(PROGRESS_STRING, message, controller);
    }
    
    
    @Override
    public ProgressDisplayer showProgress(final String caption, final String message, final ProgressController controller) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (progressLabel == null) initComponents();
                
                DialogDescriptor dd = createDialogDescriptor(caption, message, controller);
                Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                d.pack();

                owner = d;
                if (owner instanceof JDialog) {
                    ((JDialog)owner).setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                }
                
                d.setVisible(true);
            }
        });
        return this;
    }

    public void close() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (owner != null) {
                    owner.setVisible(false);
                    owner.dispose();
                }

                cleanup();
            }
        });
    }

    public static ProfilerProgressDisplayer getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ProfilerProgressDisplayer();
        }

        return defaultInstance;
    }

    private void cleanup() {
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
        }

        controller = null;
        owner = null;
    }

    private DialogDescriptor createDialogDescriptor(String caption, String message, ProgressController controller) {
        this.controller = controller;

        progressLabel.setText(message);
        progressBar.setIndeterminate(true);

        DialogDescriptor dd = controller == null ?
            new DialogDescriptor(this, caption, true, new Object[0], null, 0, null, null) :
            new DialogDescriptor(this, caption, true, new Object[] { cancelButton }, null, 0, null, null);

        dd.setClosingOptions(new Object[0]);
        return dd;
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // progressLabel
        progressLabel = new JLabel();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 8, 0, 8);
        add(progressLabel, constraints);

        // progressBar
        progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setPreferredSize(new Dimension(300, progressBar.getPreferredSize().height));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 8, 15, 8);
        add(progressBar, constraints);

        // fillerPanel
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(UIUtils.createFillerPanel(), constraints);

        // cancelButton
        cancelButton = new JButton(CANCEL_BUTTON_TEXT);
        cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (controller != null) {
                        controller.cancel();
                    }
                }
            });
    }
}

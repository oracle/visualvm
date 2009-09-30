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

import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.ui.stp.Utils;
import org.openide.DialogDescriptor;
import org.openide.util.Cancellable;
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
import javax.swing.WindowConstants;


/**
 *
 * @author Jiri Sedlacek
 */
public class ProgressDisplayer extends JPanel {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    // --- ProgressController interface ------------------------------------------
    public static interface ProgressController extends Cancellable {
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String PROGRESS_STRING = NbBundle.getMessage(ProgressDisplayer.class, "ProgressDisplayer_ProgressString"); // NOI18N
    private static final String CANCEL_BUTTON_TEXT = NbBundle.getMessage(ProgressDisplayer.class,
                                                                         "ProgressDisplayer_CancelButtonText"); // NOI18N
                                                                                                                // -----
    private static final Object displayerLock = new Object();

    // --- Private implementation ------------------------------------------------
    private static ProgressDisplayer defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Dialog owner;
    private JButton cancelButton;

    // --- UI components declaration ---------------------------------------------
    private JLabel progressLabel;
    private JProgressBar progressBar;

    // --- Instance variables declaration ----------------------------------------
    private ProgressController controller;
    private boolean isOpened;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProgressDisplayer() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public static ProgressDisplayer showProgress(String message) {
        return showProgress(message, null);
    }

    public static ProgressDisplayer showProgress(String message, ProgressController controller) {
        return showProgress(PROGRESS_STRING, message, controller);
    }

    public static ProgressDisplayer showProgress(String caption, String message, ProgressController controller) {
        synchronized (displayerLock) {
            final ProgressDisplayer pd = ProgressDisplayer.getDefault();

            final DialogDescriptor dd = pd.createDialogDescriptor(caption, message, controller);
            final Dialog d = ProfilerDialogs.createDialog(dd);
            d.pack();

            pd.setOwner(d);

            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        pd.setOpened(true);
                        d.setVisible(true);
                    }
                });

            return pd;
        }
    }

    public boolean isOpened() {
        synchronized (displayerLock) {
            return isOpened;
        }
    }

    public void close() {
        synchronized (displayerLock) {
            if (owner != null) {
                owner.setVisible(false);
                owner.dispose();
            }

            cleanup();
        }
    }

    private static ProgressDisplayer getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ProgressDisplayer();
        }

        return defaultInstance;
    }

    private void setOpened(boolean isOpened) {
        this.isOpened = isOpened;
    }

    private void setOwner(Dialog owner) {
        this.owner = owner;

        if (owner instanceof JDialog) {
            ((JDialog) owner).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }
    }

    private void cleanup() {
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
        }

        isOpened = false;
        controller = null;
        owner = null;
    }

    private DialogDescriptor createDialogDescriptor(String caption, String message, ProgressController controller) {
        this.controller = controller;

        progressLabel.setText(message);
        progressBar.setIndeterminate(true);

        DialogDescriptor dd = null;

        if (controller == null) {
            dd = new DialogDescriptor(this, caption, true, new Object[0], null, 0, null, null);
        } else {
            dd = new DialogDescriptor(this, caption, true, new Object[] { cancelButton }, null, 0, null, null);
        }

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
        add(Utils.createFillerPanel(), constraints);

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

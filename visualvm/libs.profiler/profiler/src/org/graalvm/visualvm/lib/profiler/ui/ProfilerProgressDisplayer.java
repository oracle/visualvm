/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler.ui;

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
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.api.ProgressDisplayer;
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

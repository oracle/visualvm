/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.ui.UIConstants;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Hurka
 */
@NbBundle.Messages({
    "AttachToBugAction_ActionName=Attach IDE snapshot to bug...",
    "AttachToBugAction_ActionDescr=Attach IDE snapshot to bug...",
    "AttachToBugAction_ProgressMsg=Attaching snapshot...",
    "AttachToBugAction_AttachFailedMsg=Attaching snapshot failed.\n{0}",
    "AttachToBugAction_FileDescription=.npss file attached from NetBeans",
    "AttachToBugAction_Description=.npss file",
    "AttachToBugAction_IssueLabel=&Issue number:",
    "AttachToBugAction_DialogTitle=Select Issue Number"
})
final class AttachToBugAction extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(AttachToBugAction.class.getName());
    private static final String BUZILLA_CLASS = "org.netbeans.modules.bugzilla.api.NBBugzillaUtils"; // NOI18N
    private static final String ATTACH_FILE_METHOD = "attachFiles"; // NOI18N
    private static final String ICON_PATH = "org/graalvm/visualvm/lib/profiler/snaptracer/impl/icons/bugtracking.png"; // NOI18N
    private static final String NPSS_MINE = "application/x-npss"; // NOI18N
    private static Method ATTACH_FILE;
    private final File snapshotFile;

    static boolean isSupported() {
        try {
            Class bugzilla = Class.forName(BUZILLA_CLASS, true, Thread.currentThread().getContextClassLoader());
            ATTACH_FILE = bugzilla.getMethod(ATTACH_FILE_METHOD, String.class, String.class, String[].class, String[].class, File[].class);
        } catch (NoSuchMethodException ex) {
            LOG.log(Level.FINE, "isSupported", ex);     // NOI18N
        } catch (SecurityException ex) {
            LOG.log(Level.FINE, "isSupported", ex);     // NOI18N
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.FINE, "isSupported", ex);     // NOI18N
        }
        return ATTACH_FILE != null;
    }

    AttachToBugAction(File file) {
        snapshotFile = file;

        assert file.isFile();
        putValue(Action.NAME, Bundle.AttachToBugAction_ActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.AttachToBugAction_ActionDescr());
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon(ICON_PATH, true));
        putValue("iconBase", ICON_PATH);        // NOI18N
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                IssueNumberDialog issue = new IssueNumberDialog();
                String bugNumber = issue.getIssueString();

                if (bugNumber != null) {
                    attachtoBug(bugNumber, snapshotFile);
                }
            }
        });
    }

    // TODO: export also UI gestures file if available, preferably based on user option
    private static void attachtoBug(final String bugNumber, final File targetFile) {
        final ProgressHandle progress = ProgressHandle.createHandle(
                Bundle.AttachToBugAction_ProgressMsg());
        progress.setInitialDelay(500);
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                progress.start();
                try {
                    String[] comments = new String[]{Bundle.AttachToBugAction_FileDescription()};
                    String[] contentType = new String[]{NPSS_MINE};
                    File[] files = new File[]{targetFile};
                    ATTACH_FILE.invoke(null, bugNumber, Bundle.AttachToBugAction_Description(), comments, contentType, files);
                } catch (IllegalAccessException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Throwable oex = ex.getTargetException();
                    Logger.getLogger(getClass().getName()).log(Level.INFO, "attachtoBug", oex);   // NOI18N
                    ProfilerDialogs.displayError(Bundle.AttachToBugAction_AttachFailedMsg(oex.getLocalizedMessage()));
                    ProfilerLogger.log("Failed to attach NPSS snapshot: " + oex.getMessage()); // NOI18N
                } finally {
                    progress.finish();
                }
            }
        });
    }

    @NbBundle.Messages({
        "IssueNumberDialog_DialogCaption=Select Issue Number",
        "IssueNumberDialog_LabelString=&Issue number:",
        "IssueNumberDialog_ButtonName=&OK",
        "IssueNumberDialog_FieldAccessDescr=Select issue number."
    })
    private static class IssueNumberDialog extends JPanel {

        //~ Instance fields ----------------------------------------------------------------------------------------------------------
        private JButton okButton;
        private JLabel issueLabel;
        private JTextField issueField;

        //~ Constructors -------------------------------------------------------------------------------------------------------------

        private IssueNumberDialog() {
            initComponents();
        }

        //~ Methods ------------------------------------------------------------------------------------------------------------------
        public String getIssueString() {

            final DialogDescriptor dd = new DialogDescriptor(this, Bundle.IssueNumberDialog_DialogCaption(), true,
                    new Object[]{okButton, DialogDescriptor.CANCEL_OPTION},
                    okButton, DialogDescriptor.BOTTOM_ALIGN, null, null);
            final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
            d.setVisible(true);

            if (dd.getValue() == okButton) {
                return Integer.toString(Integer.parseInt(issueField.getText().trim()));
            } else {
                return null;
            }
        }

        private void initComponents() {
            GridBagConstraints gridBagConstraints;

            issueLabel = new JLabel();
            issueField = new JTextField();
            okButton = new JButton();

            setLayout(new GridBagLayout());

            // issueLabel
            issueLabel.setLabelFor(issueField);
            Mnemonics.setLocalizedText(issueLabel, Bundle.IssueNumberDialog_LabelString());

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.insets = new Insets(15, 10, 0, 10);
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            add(issueLabel, gridBagConstraints);

            // issueField
            issueField.getAccessibleContext().setAccessibleDescription(Bundle.IssueNumberDialog_FieldAccessDescr());
            issueField.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
            issueField.setSelectedTextColor(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
            issueField.setPreferredSize(new Dimension(150, issueField.getPreferredSize().height));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(15, 0, 0, 10);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            add(issueField, gridBagConstraints);
            issueField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateOkButton();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateOkButton();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateOkButton();
                }
            });

            // okButton
            Mnemonics.setLocalizedText(okButton, Bundle.IssueNumberDialog_ButtonName());
            updateOkButton();

            // panel filling bottom space
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(new JPanel(), gridBagConstraints);
        }

        private void updateOkButton() {
            String number = issueField.getText().trim();

            try {
                if (!number.isEmpty()) {
                    int n = Integer.parseInt(number);
                    if (n > 0 && n < 1000000) {
                        okButton.setEnabled(true);
                        return;
                    }
                }
            } catch (NumberFormatException ex) {
                // ignore
            }
            okButton.setEnabled(false);
        }
    }
}

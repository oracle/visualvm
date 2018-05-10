/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jmx.impl;

import org.graalvm.visualvm.core.ui.components.Spacer;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Luis-Miguel Alventosa
 */
public final class CredentialsConfigurator extends JPanel {

    public static CredentialsConfigurator supplyCredentials(String url) {
        CredentialsConfigurator asc = new CredentialsConfigurator();
        asc.setupDefineCredentials();

        final String title = NbBundle.getMessage(CredentialsConfigurator.class, "LBL_Supply_Security_Credentials") + url;   // NOI18N
        final DialogDescriptor dd = new DialogDescriptor(
                asc, title, true,
                new Object[]{asc.okButton, DialogDescriptor.CANCEL_OPTION},
                asc.okButton, 0, null, null);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.pack();
        d.setVisible(true);

        if (dd.getValue() == asc.okButton) {
            return asc;
        } else {
            return null;
        }
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    private CredentialsConfigurator() {
        initComponents();
        update();
    }

    private void setupDefineCredentials() {
        usernameField.setEnabled(true);
        usernameField.setText("");
        passwordField.setEnabled(true);
        passwordField.setText("");
    }

    private void update() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String username = getUsername();
                String password = getPassword();
                okButton.setEnabled(username.length() > 0 && password.length() > 0);
            }
        });
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints;
        
        // hintLabel1
        hintLabel1 = new JLabel();
        hintLabel1.setFont(hintLabel1.getFont().deriveFont(Font.BOLD));
        Mnemonics.setLocalizedText(hintLabel1, NbBundle.getMessage(CredentialsConfigurator.class, "LBL_CredentialsMsg1")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 10, 0, 10);
        add(hintLabel1, constraints);
        
        // hintLabel2
        hintLabel2 = new JLabel();
        Mnemonics.setLocalizedText(hintLabel2, NbBundle.getMessage(CredentialsConfigurator.class, "LBL_CredentialsMsg2")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 10, 5, 10);
        add(hintLabel2, constraints);

        // usernameLabel
        usernameLabel = new JLabel();
        Mnemonics.setLocalizedText(usernameLabel, NbBundle.getMessage(CredentialsConfigurator.class, "LBL_Username")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(15, 20, 0, 0);
        add(usernameLabel, constraints);

        // usernameField
        usernameField = new JTextField();
        usernameLabel.setLabelFor(usernameField);
        usernameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                update();
            }
            public void removeUpdate(DocumentEvent e) {
                update();
            }
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 5, 0, 10);
        add(usernameField, constraints);

        // passwordLabel
        passwordLabel = new JLabel();
        Mnemonics.setLocalizedText(passwordLabel, NbBundle.getMessage(CredentialsConfigurator.class, "LBL_Password")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 20, 0, 0);
        add(passwordLabel, constraints);

        // passwordField
        passwordField = new JPasswordField();
        passwordLabel.setLabelFor(passwordField);
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                update();
            }
            public void removeUpdate(DocumentEvent e) {
                update();
            }
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(8, 5, 0, 10);
        add(passwordField, constraints);

        // spacer
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 15, 0);
        add(Spacer.create(), constraints);

        // okButton
        okButton = new JButton(NbBundle.getMessage(CredentialsConfigurator.class, "LBL_OK"));   // NOI18N
    }

    private JLabel hintLabel1;
    private JLabel hintLabel2;
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JButton okButton;
}

/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.jmx.application;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.ui.stp.Utils;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import sun.net.util.IPAddressUtil;

/**
 *
 * @author Luis-Miguel Alventosa
 */
class JmxApplicationConfigurator extends JPanel {

    private boolean internalChange = false;

    public static JmxApplicationConfigurator addJmxConnection() {
        JmxApplicationConfigurator hc = getDefault();
        hc.setupDefineJmxConnection();

        final DialogDescriptor dd = new DialogDescriptor(hc, NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Add_JMX_Connection"),   // NOI18N
                true, new Object[]{hc.okButton, DialogDescriptor.CANCEL_OPTION},
                hc.okButton, 0, null, null);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack();
        d.setVisible(true);

        if (dd.getValue() == hc.okButton) {
            return hc;
        } else {
            return null;
        }
    }

    public String getConnection() {
        return connectionField.getText().trim();
    }

    public String getDisplayName() {
        return displaynameField.getText().trim();
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public boolean getSaveCredentialsFlag() {
        return saveCheckbox.isSelected();
    }

    private static JmxApplicationConfigurator defaultInstance;

    private JmxApplicationConfigurator() {
        initComponents();
        update();
    }

    private static JmxApplicationConfigurator getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new JmxApplicationConfigurator();
        }
        return defaultInstance;
    }

    private void setupDefineJmxConnection() {
        connectionField.setEnabled(true);
        connectionField.setText(""); // NOI18N
        displaynameCheckbox.setSelected(false);
        displaynameCheckbox.setEnabled(true);
        displaynameField.setText(""); // NOI18N
        securityCheckbox.setSelected(false);
        securityCheckbox.setEnabled(true);
        usernameField.setText(""); // NOI18N
        passwordField.setText(""); // NOI18N
        saveCheckbox.setSelected(false);
        saveCheckbox.setEnabled(false);
        
        Set<DataSource> selectedDataSources =
                ExplorerSupport.sharedInstance().getSelectedDataSources();
        if (selectedDataSources.size() != 1) return;
        DataSource selectedDataSource = selectedDataSources.iterator().next();
        if (!(selectedDataSource instanceof Host)) return;
        Host host = (Host)selectedDataSource;
        connectionField.setText(host.getHostName() + ":"); // NOI18N
    }

    private void update() {
        if (internalChange) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String username = getUsername();
                String url = getConnection();

                if (!displaynameCheckbox.isSelected()) {
                    internalChange = true;
                    displaynameField.setText(
                            (username.isEmpty() ? "" : username + "@") + url); // NOI18N
                    internalChange = false;
                }

                String displayname = getDisplayName();
                displaynameField.setEnabled(displaynameCheckbox.isSelected());

                usernameField.setEnabled(securityCheckbox.isSelected());
                passwordField.setEnabled(securityCheckbox.isSelected());
                saveCheckbox.setEnabled(securityCheckbox.isSelected());

                okButton.setEnabled(enableOkButton(url, displayname));
            }
        });
    }

    private boolean enableOkButton(String url, String displayname) {
        return isValidRemoteString(url) && displayname.length() > 0;
    }

    private static boolean isValidRemoteString(String txt) {
        boolean valid = false;
        if (txt != null) {
            txt = txt.trim();
            if (txt.startsWith("service:jmx:")) { // NOI18N
                if (txt.length() > "service:jmx:".length()) { // NOI18N
                    valid = true;
                }
            } else {
                //---------------------------------------
                // Supported host and port combinations:
                //     hostname:port
                //     IPv4Address:port
                //     [IPv6Address]:port
                //---------------------------------------

                // Is literal IPv6 address?
                //
                if (txt.startsWith("[")) { // NOI18N
                    int index = txt.indexOf("]:"); // NOI18N
                    if (index != -1) {
                        // Extract literal IPv6 address
                        //
                        String address = txt.substring(1, index);
                        if (IPAddressUtil.isIPv6LiteralAddress(address)) {
                            // Extract port
                            //
                            try {
                                String portStr = txt.substring(index + 2);
                                int port = Integer.parseInt(portStr);
                                if (port >= 0 && port <= 0xFFFF) {
                                    valid = true;
                                }
                            } catch (NumberFormatException ex) {
                                valid = false;
                            }
                        }
                    }
                } else {
                    String[] s = txt.split(":"); // NOI18N
                    if (s.length == 2) {
                        try {
                            int port = Integer.parseInt(s[1]);
                            if (port >= 0 && port <= 0xFFFF) {
                                valid = true;
                            }
                        } catch (NumberFormatException ex) {
                            valid = false;
                        }
                    }
                }
            }
        }
        return valid;
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints;

        // connectionLabel
        connectionLabel = new JLabel(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Connection"));  // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(15, 10, 0, 0);
        add(connectionLabel, constraints);

        // connectionField
        connectionField = new JTextField();
        connectionField.setPreferredSize(
                new Dimension(250, connectionField.getPreferredSize().height));
        connectionField.getDocument().addDocumentListener(new DocumentListener() {
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
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 5, 0, 10);
        add(connectionField, constraints);

        // usageLabel
        Font normalLabelFont = connectionLabel.getFont();
        Font smallLabelFont =
                normalLabelFont.deriveFont(normalLabelFont.getSize2D() - 1);
        usageLabel = new JLabel(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Usage"));    // NOI18N
        usageLabel.setFont(smallLabelFont);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 10);
        add(usageLabel, constraints);

        // displaynameCheckbox
        displaynameCheckbox = new JCheckBox(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Display_name")); // NOI18N
        displaynameCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
            };
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 10, 0, 0);
        add(displaynameCheckbox, constraints);

        // displaynameField
        displaynameField = new JTextField();
        displaynameField.setPreferredSize(
                new Dimension(250, displaynameField.getPreferredSize().height));
        displaynameField.getDocument().addDocumentListener(new DocumentListener() {
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
        constraints.insets = new Insets(8, 5, 0, 10);
        add(displaynameField, constraints);

        // securityCheckbox
        securityCheckbox = new JCheckBox(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Use_security_credentials"));    // NOI18N
        securityCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
            };
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(15, 6, 0, 0);
        add(securityCheckbox, constraints);

        // usernameLabel
        usernameLabel = new JLabel(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Username"));  // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 10, 0, 0);
        add(usernameLabel, constraints);

        // usernameField
        usernameField = new JTextField();
        usernameField.setPreferredSize(
                new Dimension(320, usernameField.getPreferredSize().height));
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
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(8, 5, 0, 10);
        add(usernameField, constraints);

        // passwordLabel
        passwordLabel = new JLabel(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Password"));  // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 10, 0, 0);
        add(passwordLabel, constraints);

        // passwordField
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(
                new Dimension(200, passwordField.getPreferredSize().height));
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
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(8, 5, 0, 10);
        add(passwordField, constraints);

        // saveCheckbox
        saveCheckbox = new JCheckBox(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_Save_security_credentials"));   // NOI18N
        saveCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
            };
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 40, 0, 0);
        add(saveCheckbox, constraints);

        // spacer
        JPanel spacer = Utils.createFillerPanel();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 15, 0);
        add(spacer, constraints);

        // okButton
        okButton = new JButton(NbBundle.getMessage(JmxApplicationConfigurator.class, "LBL_OK"));    // NOI18N
        okButton.setEnabled(false);

        // UI tweaks
        displaynameCheckbox.setBorder(connectionLabel.getBorder());
    }

    private JLabel connectionLabel;
    private JTextField connectionField;
    private JLabel usageLabel;
    private JCheckBox displaynameCheckbox;
    private JTextField displaynameField;
    private JCheckBox securityCheckbox;
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JCheckBox saveCheckbox;
    private JButton okButton;
}

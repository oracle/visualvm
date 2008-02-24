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

package com.sun.tools.visualvm.core.jmx;

import com.sun.tools.visualvm.core.application.JmxApplication;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.ui.stp.Utils;
import org.openide.DialogDescriptor;

/**
 *
 * @author Luis-Miguel Alventosa
 */
class JmxConnectionConfigurator extends JPanel {

    private boolean internalChange = false;

    public static JmxConnectionConfigurator defineJmxConnection() {
        JmxConnectionConfigurator hc = getDefault();
        hc.setupDefineJmxConnection();

        final DialogDescriptor dd = new DialogDescriptor(hc, "Add JMX connection",
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

    public static JmxConnectionConfigurator renameJmxConnection(JmxApplication app) {
        JmxConnectionConfigurator hc = getDefault();
        hc.setupRenameJmxConnection(app);

        final DialogDescriptor dd = new DialogDescriptor(hc, "Rename JMX connection",
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

    public String getJMXServiceURL() {
        return connectionField.getText().trim();
    }

    public String getDisplayName() {
        return displaynameField.getText().trim();
    }

    private static JmxConnectionConfigurator defaultInstance;

    private JmxConnectionConfigurator() {
        initComponents();
        update();
    }

    private static JmxConnectionConfigurator getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new JmxConnectionConfigurator();
        }
        return defaultInstance;
    }

    private void setupDefineJmxConnection() {
        connectionField.setEnabled(true);
        displaynameCheckbox.setSelected(false);
        displaynameCheckbox.setEnabled(true);
        connectionField.setText("");
        displaynameField.setText("");
    }

    private void setupRenameJmxConnection(JmxApplication app) {
        connectionField.setEnabled(false);
        displaynameCheckbox.setSelected(true);
        displaynameCheckbox.setEnabled(false);
        displaynameField.setText(app.getJMXServiceURL().toString());
        displaynameField.requestFocusInWindow();
        displaynameField.selectAll();
    }

    private void update() {
        if (internalChange) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String url = getJMXServiceURL();

                if (!displaynameCheckbox.isSelected()) {
                    internalChange = true;
                    displaynameField.setText(url);
                    internalChange = false;
                }

                String displayname = getDisplayName();
                displaynameField.setEnabled(displaynameCheckbox.isSelected());

                okButton.setEnabled(displayname.length() > 0);
            }
        });
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints;

        // connectionLabel
        connectionLabel = new JLabel("JMXServiceURL:");
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
        connectionField.setPreferredSize(new Dimension(220, connectionField.getPreferredSize().height));
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

        // displaynameCheckbox
        displaynameCheckbox = new JCheckBox("Display name:");
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
        displaynameField.setPreferredSize(new Dimension(220, displaynameField.getPreferredSize().height));
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

        // spacer
        JPanel spacer = Utils.createFillerPanel();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 15, 0);
        add(spacer, constraints);

        // okButton
        okButton = new JButton("OK");

        // UI tweaks
        displaynameCheckbox.setBorder(connectionLabel.getBorder());
    }
    private JLabel connectionLabel;
    private JTextField connectionField;
    private JCheckBox displaynameCheckbox;
    private JTextField displaynameField;
    private JButton okButton;
}

/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.security;

import com.sun.tools.visualvm.core.options.UISupport;
import com.sun.tools.visualvm.core.ui.components.SectionSeparator;
import com.sun.tools.visualvm.core.ui.components.Spacer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class SecurityOptionsPanel extends JPanel {

    private static final String PASSWORD_NOT_CHANGED = "----------"; // NOI18N

    private final SecurityOptionsPanelController controller;
    private boolean dataValid;

    private char[] keyStorePassword;
    private char[] trustStorePassword;
    

    SecurityOptionsPanel(SecurityOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        update();
    }


    void cleanup() {
        setKeyStorePassword(PASSWORD_NOT_CHANGED.toCharArray());
        setTrustStorePassword(PASSWORD_NOT_CHANGED.toCharArray());
    }

    boolean dataValid() {
        return dataValid;
    }

    boolean shouldRestart() {
        return restartCheckBox.isSelected();
    }

    void resetRestart() {
        restartCheckBox.setSelected(false);
    }


    String getKeyStore() {
        if (!keyStoreLocCheckBox.isSelected()) return null;
        return keyStoreLocField.getText().trim();
    }

    void setKeyStore(String keyStore) {
        keyStoreLocCheckBox.setSelected(keyStore != null);
        if (keyStore != null) keyStoreLocField.setText(keyStore);
        else keyStoreLocField.setText(""); // NOI18N
    }

    char[] getKeyStorePassword() {
        if (!keyStoreLocCheckBox.isSelected()) return null;
        char[] password = keyStorePassField.getPassword();
        if (Arrays.equals(password, PASSWORD_NOT_CHANGED.toCharArray()))
            return keyStorePassword;
        else
            return password;
    }

    void setKeyStorePassword(char[] keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        keyStorePassField.setText(PASSWORD_NOT_CHANGED);
    }

    String getKeyStoreType() {
        if (!keyStoreLocCheckBox.isSelected()) return null;
        if (keyStoreTypeCombo.getSelectedIndex() == 0) return null;
        return keyStoreTypeCombo.getSelectedItem().toString().trim();
    }

    void setKeyStoreType(String keyStoreType) {
        if (keyStoreType == null) keyStoreTypeCombo.setSelectedIndex(0);
        else keyStoreTypeCombo.setSelectedItem(keyStoreType);
    }

    String getTrustStore() {
        if (!trustStoreLocCheckBox.isSelected()) return null;
        return trustStoreLocField.getText().trim();
    }

    void setTrustStore(String trustStore) {
        trustStoreLocCheckBox.setSelected(trustStore != null);
        if (trustStore != null) trustStoreLocField.setText(trustStore);
        else trustStoreLocField.setText(""); // NOI18N
    }

    char[] getTrustStorePassword() {
        if (!trustStoreLocCheckBox.isSelected()) return null;
        char[] password = trustStorePassField.getPassword();
        if (Arrays.equals(password, PASSWORD_NOT_CHANGED.toCharArray()))
            return trustStorePassword;
        else
            return password;
    }

    void setTrustStorePassword(char[] trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        trustStorePassField.setText(PASSWORD_NOT_CHANGED);
    }

    String getTrustStoreType() {
        if (!trustStoreLocCheckBox.isSelected()) return null;
        if (trustStoreTypeCombo.getSelectedIndex() == 0) return null;
        return trustStoreTypeCombo.getSelectedItem().toString().trim();
    }

    void setTrustStoreType(String trustStoreType) {
        if (trustStoreType == null) trustStoreTypeCombo.setSelectedIndex(0);
        else trustStoreTypeCombo.setSelectedItem(trustStoreType);
    }

    String getEnabledProtocols() {
        if (!protocolsCheckBox.isSelected()) return null;
        return protocolsField.getText().trim();
    }

    void setEnabledProtocols(String enabledProtocols) {
        protocolsCheckBox.setSelected(enabledProtocols != null);
        if (enabledProtocols != null) protocolsField.setText(enabledProtocols);
        else protocolsField.setText(""); // NOI18N
    }

    String getEnabledCipherSuites() {
        if (!cipherSuitesCheckBox.isSelected()) return null;
        return cipherSuitesField.getText().trim();
    }

    void setEnabledCipherSuites(String enabledCipherSuites) {
        cipherSuitesCheckBox.setSelected(enabledCipherSuites != null);
        if (enabledCipherSuites != null) cipherSuitesField.setText(enabledCipherSuites);
        else cipherSuitesField.setText(""); // NOI18N
    }


    private void update() {
        SecurityModel model = controller.getModel();
        boolean externallyCustomized = model.environmentCustomized();

        boolean keyStoreCustomized = keyStoreLocCheckBox.isSelected();
        keyStoreLocCheckBox.setEnabled(!externallyCustomized);
        keyStoreLocField.setEnabled(!externallyCustomized && keyStoreCustomized);
        keyStoreLocButton.setEnabled(!externallyCustomized && keyStoreCustomized);
        keyStorePassLabel.setEnabled(!externallyCustomized && keyStoreCustomized);
        keyStorePassField.setEnabled(!externallyCustomized && keyStoreCustomized);
        keyStoreTypeLabel.setEnabled(!externallyCustomized && keyStoreCustomized);
        keyStoreTypeCombo.setEnabled(!externallyCustomized && keyStoreCustomized);
        boolean keyStoreValid = !keyStoreCustomized ||
                                new File(keyStoreLocField.getText().trim()).isFile();

        boolean trustStoreCustomized = trustStoreLocCheckBox.isSelected();
        trustStoreLocCheckBox.setEnabled(!externallyCustomized);
        trustStoreLocField.setEnabled(!externallyCustomized && trustStoreCustomized);
        trustStoreLocButton.setEnabled(!externallyCustomized && trustStoreCustomized);
        trustStorePassLabel.setEnabled(!externallyCustomized && trustStoreCustomized);
        trustStorePassField.setEnabled(!externallyCustomized && trustStoreCustomized);
        trustStoreTypeLabel.setEnabled(!externallyCustomized && trustStoreCustomized);
        trustStoreTypeCombo.setEnabled(!externallyCustomized && trustStoreCustomized);
        boolean trustStoreValid = !trustStoreCustomized ||
                                new File(trustStoreLocField.getText().trim()).isFile();

        boolean protocolsCustomized = protocolsCheckBox.isSelected();
        protocolsCheckBox.setEnabled(!externallyCustomized);
        protocolsField.setEnabled(!externallyCustomized && protocolsCustomized);
        protocolsButton.setEnabled(!externallyCustomized && protocolsCustomized);

        boolean cipherSuitesCustomized = cipherSuitesCheckBox.isSelected();
        cipherSuitesCheckBox.setEnabled(!externallyCustomized);
        cipherSuitesField.setEnabled(!externallyCustomized && cipherSuitesCustomized);
        cipherSuitesButton.setEnabled(!externallyCustomized && cipherSuitesCustomized);

        loadFromFileButton.setEnabled(!externallyCustomized);
        saveToFileButton.setEnabled(keyStoreCustomized || trustStoreCustomized ||
                                    protocolsCustomized || cipherSuitesCustomized);

        dataValid = keyStoreValid && trustStoreValid;
        controller.changed();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                hintPanel.setVisible(controller.differsFromEnv());
            }
        });
    }


    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints c;

        // --- Notification header ---------------------------------------------

        // notificationLabel
        notificationArea = new JTextArea(NbBundle.getMessage(SecurityOptionsPanel.class,
                                         "MSG_AlreadyDefined")); // NOI18N
        notificationArea.setEditable(false);
        notificationArea.setEnabled(false);
        notificationArea.setLineWrap(true);
        notificationArea.setWrapStyleWord(true);
        notificationArea.setOpaque(false);
        notificationArea.setDisabledTextColor(notificationArea.getForeground());
        notificationArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        notificationArea.setVisible(controller.getModel().environmentCustomized());
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 15, 0);
        add(notificationArea, c);

        // --- KeyStore --------------------------------------------------------

        // keyStoreSeparator
        SectionSeparator keyStoreSeparator = UISupport.createSectionSeparator(
                NbBundle.getMessage(SecurityOptionsPanel.class, "SEP_Certificates")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        add(keyStoreSeparator, c);

        // keyStoreLocCheckBox
        keyStoreLocCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(keyStoreLocCheckBox, NbBundle.getMessage(
                SecurityOptionsPanel.class, "CHK_KeyStore")); // NOI18N
        keyStoreLocCheckBox.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.KEYSTORE_LOCATION)); // NOI18N
        keyStoreLocCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                update();
            }
        });
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 10, 0, 0);
        add(keyStoreLocCheckBox, c);

        // keyStoreField
        keyStoreLocField = new JTextField();
        keyStoreLocField.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.KEYSTORE_LOCATION)); // NOI18N
        keyStoreLocField.setPreferredSize(
                new Dimension(250, keyStoreLocField.getPreferredSize().height));
        keyStoreLocField.getDocument().addDocumentListener(new DocumentListener() {
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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 5, 0, 0);
        add(keyStoreLocField, c);

        // keyStoreLocButton
        keyStoreLocButton = new JButton();
        Mnemonics.setLocalizedText(keyStoreLocButton,
                NbBundle.getMessage(SecurityOptionsPanel.class, "BTN_Browse1")); // NOI18N
        keyStoreLocButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File currentFile = new File(keyStoreLocField.getText().trim());
                File file = PersistenceSupport.chooseLoadFile(
                        NbBundle.getMessage(SecurityOptionsPanel.class,
                        "CAP_SelectKeyStore"), currentFile); // NOI18N
                if (file != null) keyStoreLocField.setText(file.toString());
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 5, 0, 0);
        add(keyStoreLocButton, c);

        // keyStoreSettingsPanel
        JPanel keyStoreSettingsPanel = new JPanel(new GridBagLayout());

        // keyStorePassLabel
        keyStorePassLabel = new JLabel();
        Mnemonics.setLocalizedText(keyStorePassLabel, NbBundle.getMessage(
                SecurityOptionsPanel.class, "LBL_Password1")); // NOI18N
        keyStorePassLabel.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.KEYSTORE_PASSWORD)); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        keyStoreSettingsPanel.add(keyStorePassLabel, c);

        // keyStorePassField
        keyStorePassField = new JPasswordField();
        keyStorePassLabel.setLabelFor(keyStorePassField);
        keyStorePassField.getDocument().addDocumentListener(new DocumentListener() {
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
        keyStorePassField.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.KEYSTORE_PASSWORD)); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        keyStoreSettingsPanel.add(keyStorePassField, c);

        // keyStoreTypeLabel
        keyStoreTypeLabel = new JLabel();
        Mnemonics.setLocalizedText(keyStoreTypeLabel, NbBundle.getMessage(
                SecurityOptionsPanel.class, "LBL_Type1")); // NOI18N
        keyStoreTypeLabel.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.KEYSTORE_TYPE)); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 25, 0, 0);
        keyStoreSettingsPanel.add(keyStoreTypeLabel, c);

        // keyStoreTypeCombo
        keyStoreTypeCombo = new JComboBox(new String[] { NbBundle.getMessage(
                SecurityOptionsPanel.class, "OPT_Default"), "jks", "pkcs12", "jceks" }) { // NOI18N
            public Dimension getMinimumSize() { return getPreferredSize(); }
        };
        keyStoreTypeLabel.setLabelFor(keyStoreTypeCombo);
        keyStoreTypeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });
        keyStoreTypeCombo.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.KEYSTORE_TYPE)); // NOI18N
        keyStoreTypeCombo.setEditable(true);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 5, 0, 0);
        keyStoreSettingsPanel.add(keyStoreTypeCombo, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 0, 0);
        add(keyStoreSettingsPanel, c);


        // --- TrustStore ------------------------------------------------------

        // trustStoreLocLabel
        trustStoreLocCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(trustStoreLocCheckBox, NbBundle.getMessage(
                SecurityOptionsPanel.class, "CHK_TrustStore")); // NOI18N
        trustStoreLocCheckBox.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.TRUSTSTORE_LOCATION)); // NOI18N
        trustStoreLocCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                update();
            }
        });
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 10, 0, 0);
        add(trustStoreLocCheckBox, c);

        // trustStoreField
        trustStoreLocField = new JTextField();
        trustStoreLocField.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.TRUSTSTORE_LOCATION)); // NOI18N
        trustStoreLocField.setPreferredSize(
                new Dimension(250, trustStoreLocField.getPreferredSize().height));
        trustStoreLocField.getDocument().addDocumentListener(new DocumentListener() {
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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 5, 0, 0);
        add(trustStoreLocField, c);

        // trustStoreLocButton
        trustStoreLocButton = new JButton();
        Mnemonics.setLocalizedText(trustStoreLocButton,
                NbBundle.getMessage(SecurityOptionsPanel.class, "BTN_Browse2")); // NOI18N
        trustStoreLocButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File currentFile = new File(trustStoreLocField.getText().trim());
                File file = PersistenceSupport.chooseLoadFile(
                        NbBundle.getMessage(SecurityOptionsPanel.class,
                        "CAP_SelectTrustStore"), currentFile); // NOI18N
                if (file != null) trustStoreLocField.setText(file.toString());
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 5;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 5, 0, 0);
        add(trustStoreLocButton, c);

        // trustStoreSettingsPanel
        JPanel trustStoreSettingsPanel = new JPanel(new GridBagLayout());

        // trustStorePassLabel
        trustStorePassLabel = new JLabel();
        Mnemonics.setLocalizedText(trustStorePassLabel, NbBundle.getMessage(
                SecurityOptionsPanel.class, "LBL_Password2")); // NOI18N
        trustStorePassLabel.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.TRUSTSTORE_PASSWORD)); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        trustStoreSettingsPanel.add(trustStorePassLabel, c);

        // trustStorePassField
        trustStorePassField = new JPasswordField();
        trustStorePassLabel.setLabelFor(trustStorePassField);
        trustStorePassField.getDocument().addDocumentListener(new DocumentListener() {
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
        trustStorePassField.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.TRUSTSTORE_PASSWORD)); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        trustStoreSettingsPanel.add(trustStorePassField, c);

        // trustStoreTypeLabel
        trustStoreTypeLabel = new JLabel();
        Mnemonics.setLocalizedText(trustStoreTypeLabel, NbBundle.getMessage(
                SecurityOptionsPanel.class, "LBL_Type2")); // NOI18N
        trustStoreTypeLabel.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.TRUSTSTORE_TYPE)); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 25, 0, 0);
        trustStoreSettingsPanel.add(trustStoreTypeLabel, c);

        // trustStoreTypeCombo
        trustStoreTypeCombo = new JComboBox(new String[] { NbBundle.getMessage(
                SecurityOptionsPanel.class, "OPT_Default"), "jks", "pkcs12", "jceks" }) { // NOI18N
            public Dimension getMinimumSize() { return getPreferredSize(); }
        };
        trustStoreTypeLabel.setLabelFor(trustStoreTypeCombo);
        trustStoreTypeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });
        trustStoreTypeCombo.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.TRUSTSTORE_TYPE)); // NOI18N
        trustStoreTypeCombo.setEditable(true);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 5, 0, 0);
        trustStoreSettingsPanel.add(trustStoreTypeCombo, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 6;
        c.weightx = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 0, 0);
        add(trustStoreSettingsPanel, c);


        // --- Protocols -------------------------------------------------------

        // protocolsSeparator
        SectionSeparator protocolsSeparator = UISupport.createSectionSeparator(
                NbBundle.getMessage(SecurityOptionsPanel.class, "SEP_Protocols")); // NOI18N
        c = new GridBagConstraints();
        c = new GridBagConstraints();
        c.gridy = 7;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(18, 0, 0, 0);
        add(protocolsSeparator, c);

        // protocolsCheckBox
        protocolsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(protocolsCheckBox, NbBundle.getMessage(
                SecurityOptionsPanel.class, "CHK_Protocols")); // NOI18N
        protocolsCheckBox.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.ENABLED_PROTOCOLS)); // NOI18N
        protocolsCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                update();
            }
        });
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 15, 0, 0);
        add(protocolsCheckBox, c);

        // protocolsField
        protocolsField = new JTextField();
        protocolsField.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.ENABLED_PROTOCOLS)); // NOI18N
        protocolsField.setPreferredSize(
                new Dimension(250, protocolsField.getPreferredSize().height));
        protocolsField.getDocument().addDocumentListener(new DocumentListener() {
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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 8;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 5, 0, 0);
        add(protocolsField, c);

        // protocolsButton
        protocolsButton = new JButton();
        Mnemonics.setLocalizedText(protocolsButton, NbBundle.getMessage(
                SecurityOptionsPanel.class, "BTN_Customize1")); // NOI18N
        protocolsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String protocols = ValuesCustomizer.customize(
                                   ValuesCustomizer.PROTOCOLS,
                                   protocolsField.getText().trim());
                if (protocols != null) protocolsField.setText(protocols);
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 8;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 5, 0, 0);
        add(protocolsButton, c);

        // cipherSuitesCheckBox
        cipherSuitesCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(cipherSuitesCheckBox, NbBundle.getMessage(
                SecurityOptionsPanel.class, "CHK_CipherSuites")); // NOI18N
        cipherSuitesCheckBox.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.ENABLED_CIPHER_SUITES)); // NOI18N
        cipherSuitesCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                update();
            }
        });
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 15, 0, 0);
        add(cipherSuitesCheckBox, c);

        // cipherSuitesField
        cipherSuitesField = new JTextField();
        cipherSuitesField.setToolTipText(NbBundle.getMessage(SecurityOptionsPanel.class,
                "MSG_ValueOf", SecurityModel.ENABLED_CIPHER_SUITES)); // NOI18N
        cipherSuitesField.setPreferredSize(
                new Dimension(250, cipherSuitesField.getPreferredSize().height));
        cipherSuitesField.getDocument().addDocumentListener(new DocumentListener() {
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
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 9;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 0);
        add(cipherSuitesField, c);

        // cipherSuitesButton
        cipherSuitesButton = new JButton();
        Mnemonics.setLocalizedText(cipherSuitesButton, NbBundle.getMessage(
                SecurityOptionsPanel.class, "BTN_Customize2")); // NOI18N
        cipherSuitesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cipherSuites = ValuesCustomizer.customize(
                                      ValuesCustomizer.CIPHER_SUITES,
                                      cipherSuitesField.getText().trim());
                if (cipherSuites != null) cipherSuitesField.setText(cipherSuites);
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 9;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 0);
        add(cipherSuitesButton, c);


        // --- Export / Import -----------------------------------------------------

        // exportImportSeparator
        SectionSeparator exportImportSeparator = UISupport.createSectionSeparator(
                NbBundle.getMessage(SecurityOptionsPanel.class, "SEP_ExportImport")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 10;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(18, 0, 0, 0);
        add(exportImportSeparator, c);

        // exportImportPanel
        JPanel exportImportPanel = new JPanel(new GridBagLayout());

        // loadFromFileButton
        loadFromFileButton = new JButton();
        Mnemonics.setLocalizedText(loadFromFileButton, NbBundle.getMessage(
                SecurityOptionsPanel.class, "BTN_LoadFromFile")); // NOI18N
        loadFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PersistenceSupport.loadFromFile(SecurityOptionsPanel.this);
            }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 5, 0, 0);
        exportImportPanel.add(loadFromFileButton, c);

        // saveToFileButton
        saveToFileButton = new JButton();
        Mnemonics.setLocalizedText(saveToFileButton, NbBundle.getMessage(
                SecurityOptionsPanel.class, "BTN_SaveToFile")); // NOI18N
        saveToFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PersistenceSupport.saveToFile(SecurityOptionsPanel.this);
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 5, 0, 0);
        exportImportPanel.add(saveToFileButton, c);

        c = new GridBagConstraints();
        c.gridy = 11;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(7, 0, 0, 0);
        add(exportImportPanel, c);

        // hintPanel
        hintPanel = new JPanel(new GridBagLayout());
        hintPanel.setVisible(false);

        // hintLabel
        JLabel hintLabel = new JLabel();
        Mnemonics.setLocalizedText(hintLabel, NbBundle.getMessage(
                SecurityOptionsPanel.class, "MSG_RestartVisualVM")); // NOI18N
        hintLabel.setIcon(ImageUtilities.loadImageIcon(
                "com/sun/tools/visualvm/modules/security/resources/infoIcon.png", false)); // NOI18N)
        hintLabel.setIconTextGap(10);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 0);
        hintPanel.add(hintLabel, c);

        // restartCheckBox
        restartCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(restartCheckBox, NbBundle.getMessage(
                            SecurityOptionsPanel.class, "CHK_RestartVisualVM")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 20, 0, 0);
        hintPanel.add(restartCheckBox, c);

        // --- Filler ---
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 12;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(Spacer.create(), c);

        c = new GridBagConstraints();
        c.gridy = 13;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(30, 0, 0, 0);
        add(hintPanel, c);

    }
    

    private JTextArea notificationArea;
    private JCheckBox keyStoreLocCheckBox;
    private JTextField keyStoreLocField;
    private JButton keyStoreLocButton;
    private JLabel keyStorePassLabel;
    private JPasswordField keyStorePassField;
    private JLabel keyStoreTypeLabel;
    private JComboBox keyStoreTypeCombo;
    private JCheckBox trustStoreLocCheckBox;
    private JTextField trustStoreLocField;
    private JButton trustStoreLocButton;
    private JLabel trustStorePassLabel;
    private JPasswordField trustStorePassField;
    private JLabel trustStoreTypeLabel;
    private JComboBox trustStoreTypeCombo;
    private JCheckBox protocolsCheckBox;
    private JTextField protocolsField;
    private JButton protocolsButton;
    private JCheckBox cipherSuitesCheckBox;
    private JTextField cipherSuitesField;
    private JButton cipherSuitesButton;
    private JButton loadFromFileButton;
    private JButton saveToFileButton;
    private JPanel hintPanel;
    private JCheckBox restartCheckBox;

}

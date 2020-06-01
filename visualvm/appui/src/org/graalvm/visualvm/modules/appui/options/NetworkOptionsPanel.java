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

package org.graalvm.visualvm.modules.appui.options;

import org.graalvm.visualvm.core.options.UISupport;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.uisupport.JExtendedSpinner;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class NetworkOptionsPanel extends JPanel {

    private final NetworkOptionsPanelController controller;
    private boolean passwordChanged;

    NetworkOptionsPanel(NetworkOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        initListeners();
    }


    void update() {
        NetworkOptionsModel model = controller.getModel();

        switch (model.getProxyType()) {
            case ProxySettings.DIRECT_CONNECTION:
                noProxyRadio.setSelected(true);
                break;
            case ProxySettings.AUTO_DETECT_PROXY:
            case ProxySettings.AUTO_DETECT_PAC:
                systemProxyRadio.setSelected(true);
                break;
            case ProxySettings.MANUAL_SET_PROXY:
                manualProxyRadio.setSelected(true);
                break;
            default:
                throw new IllegalArgumentException("Unknown proxy configuration"); // NOI18N
        }

        httpProxyField.setText(model.getHttpProxyHost());
        httpProxySpinnerModel.setValue(Integer.parseInt(model.getHttpProxyPort()));

        sameSettingsCheckBox.setSelected(model.useProxyAllProtocols());

        httpsProxyField.setText(model.getHttpsProxyHost());
        httpsProxySpinnerModel.setValue(Integer.parseInt(model.getHttpsProxyPort()));
        socksProxyField.setText(model.getSocksHost());
        socksProxySpinnerModel.setValue(Integer.parseInt(model.getSocksPort()));

        noProxyField.setText(model.getNonProxyHosts());

        authenticationCheckBox.setSelected(model.useProxyAuthentication());
        usernameField.setText(model.getProxyAuthenticationUsername());
        passwordField.setText(new String(model.getProxyAuthenticationPassword()));
        
        updateManualSettings(); // #581
    }

    void applyChanges() {
        NetworkOptionsModel model = controller.getModel();

        if (noProxyRadio.isSelected()) {
            model.setProxyType(ProxySettings.DIRECT_CONNECTION);
        } else if (systemProxyRadio.isSelected()) {
            model.setProxyType(ProxySettings.AUTO_DETECT_PROXY);
        } else if (manualProxyRadio.isSelected()) {
            model.setProxyType(ProxySettings.MANUAL_SET_PROXY);
        } else {
            throw new IllegalArgumentException("Unknown proxy configuration"); // NOI18N
        }

        model.setHttpProxyHost(httpProxyField.getText().trim());
        model.setHttpProxyPort(httpProxySpinnerModel.getValue().toString());
        
        model.setUseProxyAllProtocols(sameSettingsCheckBox.isSelected());

        model.setHttpsProxyHost(httpsProxyField.getText().trim());
        model.setHttpsProxyPort(httpsProxySpinnerModel.getValue().toString());
        model.setSocksHost(socksProxyField.getText().trim());
        model.setSocksPort(socksProxySpinnerModel.getValue().toString());

        model.setNonProxyHosts(noProxyField.getText().trim());

        model.setUseProxyAuthentication(authenticationCheckBox.isSelected());
        model.setAuthenticationUsername(usernameField.getText());
        model.setAuthenticationPassword(passwordField.getPassword());
        passwordChanged = false;
    }

    void cancel() {
    }

    boolean dataValid() {
        return true;
    }

    boolean isChanged() {
        NetworkOptionsModel model = controller.getModel();
        int proxyType = model.getProxyType();

        if (noProxyRadio.isSelected() &&
            proxyType != ProxySettings.DIRECT_CONNECTION) return true;
        if (systemProxyRadio.isSelected() &&
            (proxyType != ProxySettings.AUTO_DETECT_PROXY && proxyType != ProxySettings.AUTO_DETECT_PAC)) return true;
        if (manualProxyRadio.isSelected() &&
            proxyType != ProxySettings.MANUAL_SET_PROXY) return true;

        if (!httpProxyField.getText().equals(model.getHttpProxyHost())) return true;
        if (!httpProxySpinnerModel.getValue().toString().equals(model.getHttpProxyPort())) return true;

        if (sameSettingsCheckBox.isSelected() != model.useProxyAllProtocols()) return true;

        if (!httpsProxyField.getText().equals(model.getHttpsProxyHost())) return true;
        if (!httpsProxySpinnerModel.getValue().toString().equals(model.getHttpsProxyPort())) return true;
        if (!socksProxyField.getText().equals(model.getSocksHost())) return true;
        if (!socksProxySpinnerModel.getValue().toString().equals(model.getSocksPort())) return true;

        if (!noProxyField.getText().equals(model.getNonProxyHosts())) return true;

        if (authenticationCheckBox.isSelected() != model.useProxyAuthentication()) return true;
        if (!usernameField.getText().equals(model.getProxyAuthenticationUsername())) return true;
        if (passwordChanged) return true;

        return false;
    }


    private void initComponents() {
        ButtonGroup radiosGroup = new ButtonGroup();
        GridBagConstraints c;

        setLayout(new GridBagLayout());

        SectionSeparator sectionSeparator = UISupport.createSectionSeparator(
                NbBundle.getMessage(NetworkOptionsPanel.class,
                "NetworkOptionsPanel_ProxySettingsCaption")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(sectionSeparator, c);

        noProxyRadio = new JRadioButton();
        Mnemonics.setLocalizedText(noProxyRadio, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_NoProxyRadio")); // NOI18N
        radiosGroup.add(noProxyRadio);
        c = new GridBagConstraints();
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 10, 2, 0);
        add(noProxyRadio, c);

        systemProxyRadio = new JRadioButton();
        Mnemonics.setLocalizedText(systemProxyRadio, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_SystemProxyRadio")); // NOI18N
        radiosGroup.add(systemProxyRadio);
        c = new GridBagConstraints();
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 10, 2, 0);
        add(systemProxyRadio, c);

        manualProxyRadio = new JRadioButton();
        Mnemonics.setLocalizedText(manualProxyRadio, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_ManualProxyRadio")); // NOI18N
        radiosGroup.add(manualProxyRadio);
        c = new GridBagConstraints();
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 10, 2, 0);
        add(manualProxyRadio, c);

        // --- Manual settings -------------------------------------------------

        JPanel manualSettingsPanel = new JPanel(new GridBagLayout());
        manualSettingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 40, 0, 0));

        // --- Http proxy ---

        httpProxyLabel = new JLabel();
        Mnemonics.setLocalizedText(httpProxyLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_HttpProxy")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 5);
        manualSettingsPanel.add(httpProxyLabel, c);

        httpProxyField = new JTextField();
        httpProxyLabel.setLabelFor(httpProxyField);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpProxyField, c);

        httpProxyPortLabel = new JLabel();
        Mnemonics.setLocalizedText(httpProxyPortLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_HttpProxyPort")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 8, 3, 5);
        manualSettingsPanel.add(httpProxyPortLabel, c);

        httpProxySpinnerModel = new SpinnerNumberModel(0, 0, 65535, 1);
        httpProxyPortSpinner = new JExtendedSpinner(httpProxySpinnerModel);
        httpProxyPortLabel.setLabelFor(httpProxyPortSpinner);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpProxyPortSpinner, c);

        // --- Use same settings ---

        sameSettingsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(sameSettingsCheckBox, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_SameSettingsCheckbox")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        manualSettingsPanel.add(sameSettingsCheckBox, c);

        // --- Https proxy ---

        httpsProxyLabel = new JLabel();
        Mnemonics.setLocalizedText(httpsProxyLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_HttpsProxy")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 5);
        manualSettingsPanel.add(httpsProxyLabel, c);

        httpsProxyField = new JTextField();
        httpsProxyLabel.setLabelFor(httpsProxyField);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpsProxyField, c);

        httpsProxyPortLabel = new JLabel();
        Mnemonics.setLocalizedText(httpsProxyPortLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_HttpsProxyPort")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 8, 3, 5);
        manualSettingsPanel.add(httpsProxyPortLabel, c);

        httpsProxySpinnerModel = new SpinnerNumberModel(0, 0, 65535, 1);
        httpsProxyPortSpinner = new JExtendedSpinner(httpsProxySpinnerModel);
        httpsProxyPortLabel.setLabelFor(httpsProxyPortSpinner);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpsProxyPortSpinner, c);

        // --- Socks proxy ---

        socksProxyLabel = new JLabel();
        Mnemonics.setLocalizedText(socksProxyLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_SocksProxy")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 5);
        manualSettingsPanel.add(socksProxyLabel, c);

        socksProxyField = new JTextField();
        socksProxyLabel.setLabelFor(socksProxyField);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(socksProxyField, c);

        socksProxyPortLabel = new JLabel();
        Mnemonics.setLocalizedText(socksProxyPortLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_SocksProxyPort")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 8, 3, 5);
        manualSettingsPanel.add(socksProxyPortLabel, c);

        socksProxySpinnerModel = new SpinnerNumberModel(0, 0, 65535, 1);
        socksProxyPortSpinner = new JExtendedSpinner(socksProxySpinnerModel);
        socksProxyPortLabel.setLabelFor(socksProxyPortSpinner);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(socksProxyPortSpinner, c);

        // --- No proxy ---

        noProxyLabel = new JLabel();
        Mnemonics.setLocalizedText(noProxyLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_NoProxyHosts")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 0, 3, 5);
        manualSettingsPanel.add(noProxyLabel, c);

        noProxyField = new JTextField();
        noProxyLabel.setLabelFor(noProxyField);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 0, 3, 0);
        manualSettingsPanel.add(noProxyField, c);

        JLabel noProxyHintLabel = new JLabel();
        Mnemonics.setLocalizedText(noProxyHintLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_NoProxyHint")); // NOI18N
        noProxyHintLabel.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        manualSettingsPanel.add(noProxyHintLabel, c);

        // --- Authentication ---

        authenticationCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(authenticationCheckBox, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_AuthCheckbox")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 0, 0);
        manualSettingsPanel.add(authenticationCheckBox, c);
        
        usernameLabel = new JLabel();
        Mnemonics.setLocalizedText(usernameLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_Username")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 30, 3, 5);
        manualSettingsPanel.add(usernameLabel, c);

        usernameField = new JTextField();
        usernameLabel.setLabelFor(usernameField);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(usernameField, c);

        passwordLabel = new JLabel();
        Mnemonics.setLocalizedText(passwordLabel, NbBundle.getMessage(NetworkOptionsPanel.class,
                                   "NetworkOptionsPanel_Password")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 8;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 30, 0, 5);
        manualSettingsPanel.add(passwordLabel, c);

        passwordField = new JPasswordField();
        passwordLabel.setLabelFor(passwordField);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 8;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(3, 0, 0, 0);
        manualSettingsPanel.add(passwordField, c);

        // --- Filler ---

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 9;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        manualSettingsPanel.add(Spacer.create(), c);


        c = new GridBagConstraints();
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        add(manualSettingsPanel, c);
    }

    private void initListeners() {
        manualProxyRadio.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateManualSettings();
            }
        });
        httpProxyField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateManualSettings();
            }
            public void removeUpdate(DocumentEvent e) {
                updateManualSettings();
            }
            public void changedUpdate(DocumentEvent e) {
                updateManualSettings();
            }
        });
        httpProxySpinnerModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateManualSettings();
            }
        });
        sameSettingsCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateManualSettings();
            }
        });
        authenticationCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateManualSettings();
            }
        });
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                passwordChanged = true;
            }
            public void removeUpdate(DocumentEvent e) {
                passwordChanged = true;
            }
            public void changedUpdate(DocumentEvent e) {
                passwordChanged = true;
            }
        });
    }

    private void updateManualSettings() {
        boolean manual = manualProxyRadio.isSelected();
        boolean shared = sameSettingsCheckBox.isSelected();
        boolean passwd = authenticationCheckBox.isSelected();

        httpProxyLabel.setEnabled(manual);
        httpProxyField.setEnabled(manual);
        httpProxyPortLabel.setEnabled(manual);
        httpProxyPortSpinner.setEnabled(manual);

        sameSettingsCheckBox.setEnabled(manual);

        httpsProxyLabel.setEnabled(manual && !shared);
        httpsProxyField.setEnabled(manual && !shared);
        httpsProxyPortLabel.setEnabled(manual && !shared);
        httpsProxyPortSpinner.setEnabled(manual && !shared);

        socksProxyLabel.setEnabled(manual && !shared);
        socksProxyField.setEnabled(manual && !shared);
        socksProxyPortLabel.setEnabled(manual && !shared);
        socksProxyPortSpinner.setEnabled(manual && !shared);

        noProxyLabel.setEnabled(manual);
        noProxyField.setEnabled(manual);

        authenticationCheckBox.setEnabled(manual);
        usernameLabel.setEnabled(manual && passwd);
        usernameField.setEnabled(manual && passwd);
        passwordLabel.setEnabled(manual && passwd);
        passwordField.setEnabled(manual && passwd);

        if (shared) {
            String proxy = httpProxyField.getText();
            Object port  = httpProxySpinnerModel.getValue();
            httpsProxyField.setText(proxy);
            httpsProxySpinnerModel.setValue(port);
            socksProxyField.setText(proxy);
            socksProxySpinnerModel.setValue(port);
        }
    }

    void updateTestConnectionStatus(NetworkOptionsModel.TestingStatus testingStatus, Object object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private JRadioButton noProxyRadio;
    private JRadioButton systemProxyRadio;
    private JRadioButton manualProxyRadio;
    private JCheckBox sameSettingsCheckBox;
    private JLabel httpProxyLabel;
    private JTextField httpProxyField;
    private JLabel httpProxyPortLabel;
    private SpinnerModel httpProxySpinnerModel;
    private JSpinner httpProxyPortSpinner;
    private JLabel httpsProxyLabel;
    private JTextField httpsProxyField;
    private JLabel httpsProxyPortLabel;
    private SpinnerModel httpsProxySpinnerModel;
    private JSpinner httpsProxyPortSpinner;
    private JLabel socksProxyLabel;
    private JTextField socksProxyField;
    private JLabel socksProxyPortLabel;
    private SpinnerModel socksProxySpinnerModel;
    private JSpinner socksProxyPortSpinner;
    private JLabel noProxyLabel;
    private JTextField noProxyField;
    private JCheckBox authenticationCheckBox;
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;

}

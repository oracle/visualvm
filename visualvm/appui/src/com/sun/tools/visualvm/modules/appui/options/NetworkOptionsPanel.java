/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.appui.options;

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
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;

/**
 *
 * @author Jiri Sedlacek
 */
class NetworkOptionsPanel extends JPanel {

    private final NetworkOptionsPanelController controller;
    

    NetworkOptionsPanel(NetworkOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        initListeners();
    }


    void update() {
        NetworkOptionsModel model = controller.getModel();

        switch (model.getProxyType()) {
            case NetworkOptionsModel.DIRECT_CONNECTION:
                noProxyRadio.setSelected(true);
                break;
            case NetworkOptionsModel.AUTO_DETECT_PROXY:
                systemProxyRadio.setSelected(true);
                break;
            case NetworkOptionsModel.MANUAL_SET_PROXY:
                manualProxyRadio.setSelected(true);
                break;
            default:
                throw new IllegalArgumentException("Vole!");
        }

        httpProxyField.setText(model.getHttpHost());
        httpProxySpinnerModel.setValue(Integer.parseInt(model.getHttpPort()));

        sameSettingsCheckBox.setSelected(model.useProxyAllProtocols());

        httpsProxyField.setText(model.getHttpsHost());
        httpsProxySpinnerModel.setValue(Integer.parseInt(model.getHttpsPort()));
        socksProxyField.setText(model.getSocksHost());
        socksProxySpinnerModel.setValue(Integer.parseInt(model.getSocksPort()));

        noProxyField.setText(model.getNonProxyHosts());

        authenticationCheckBox.setSelected(model.useAuthentication());
        usernameField.setText(model.getAuthenticationUsername());
        passwordField.setText(new String(model.getAuthenticationPassword()));
    }

    void applyChanges() {
        NetworkOptionsModel model = controller.getModel();

        if (noProxyRadio.isSelected()) {
            model.setProxyType(NetworkOptionsModel.DIRECT_CONNECTION);
        } else if (systemProxyRadio.isSelected()) {
            model.setProxyType(NetworkOptionsModel.AUTO_DETECT_PROXY);
        } else if (manualProxyRadio.isSelected()) {
            model.setProxyType(NetworkOptionsModel.MANUAL_SET_PROXY);
        } else {
            throw new IllegalArgumentException("Vole!");
        }

        model.setHttpHost(httpProxyField.getText());
        model.setHttpPort(httpProxySpinnerModel.getValue().toString());
        
        model.setUseProxyAllProtocols(sameSettingsCheckBox.isSelected());

        model.setHttpsHost(httpsProxyField.getText());
        model.setHttpsPort(httpsProxySpinnerModel.getValue().toString());
        model.setSocksHost(socksProxyField.getText());
        model.setSocksPort(socksProxySpinnerModel.getValue().toString());

        model.setNonProxyHosts(noProxyField.getText());

        model.setUseAuthentication(authenticationCheckBox.isSelected());
        model.setAuthenticationUsername(usernameField.getText());
        model.setAuthenticationPassword(passwordField.getPassword());
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
            proxyType != NetworkOptionsModel.DIRECT_CONNECTION) return true;
        if (systemProxyRadio.isSelected() &&
            proxyType != NetworkOptionsModel.AUTO_DETECT_PROXY) return true;
        if (manualProxyRadio.isSelected() &&
            proxyType != NetworkOptionsModel.MANUAL_SET_PROXY) return true;

        if (!httpProxyField.getText().equals(model.getHttpHost())) return true;
        if (!httpProxySpinnerModel.getValue().equals(model.getHttpPort())) return true;

        if (sameSettingsCheckBox.isSelected() && !model.useProxyAllProtocols()) return true;

        if (!httpsProxyField.getText().equals(model.getHttpsHost())) return true;
        if (!httpsProxySpinnerModel.getValue().equals(model.getHttpsPort())) return true;
        if (!socksProxyField.getText().equals(model.getSocksHost())) return true;
        if (!socksProxySpinnerModel.getValue().equals(model.getSocksPort())) return true;

        if (!noProxyField.getText().equals(model.getNonProxyHosts())) return true;

        if (authenticationCheckBox.isSelected() && !model.useAuthentication()) return true;
        if (!usernameField.getText().equals(model.getAuthenticationUsername())) return true;
        if (!new String(passwordField.getPassword()).equals(new String(model.getAuthenticationPassword()))) return true;

        return false;
    }


    private void initComponents() {
        ButtonGroup radiosGroup = new ButtonGroup();
        GridBagConstraints c;

        setLayout(new GridBagLayout());

        noProxyRadio = new JRadioButton();
        noProxyRadio.setText("No proxy");
        radiosGroup.add(noProxyRadio);
        c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        add(noProxyRadio, c);

        systemProxyRadio = new JRadioButton();
        systemProxyRadio.setText("Use system proxy settings");
        radiosGroup.add(systemProxyRadio);
        c = new GridBagConstraints();
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        add(systemProxyRadio, c);

        manualProxyRadio = new JRadioButton();
        manualProxyRadio.setText("Manual proxy settings:");
        radiosGroup.add(manualProxyRadio);
        c = new GridBagConstraints();
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        add(manualProxyRadio, c);

        // --- Manual settings -------------------------------------------------

        JPanel manualSettingsPanel = new JPanel(new GridBagLayout());
        manualSettingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 30, 0, 0));

        // --- Http proxy ---

        httpProxyLabel = new JLabel();
        httpProxyLabel.setText("HTTP Proxy:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 5);
        manualSettingsPanel.add(httpProxyLabel, c);

        httpProxyField = new JTextField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpProxyField, c);

        httpProxyPortLabel = new JLabel();
        httpProxyPortLabel.setText("Port:");
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 8, 0, 5);
        manualSettingsPanel.add(httpProxyPortLabel, c);

        httpProxySpinnerModel = new SpinnerNumberModel(0, 0, 65535, 1);
        httpProxyPortSpinner = new JExtendedSpinner(httpProxySpinnerModel);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpProxyPortSpinner, c);

        // --- Use same settings ---

        sameSettingsCheckBox = new JCheckBox();
        sameSettingsCheckBox.setText("Use the same proxy settings for all protocols");
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        manualSettingsPanel.add(sameSettingsCheckBox, c);

        // --- Https proxy ---

        httpsProxyLabel = new JLabel();
        httpsProxyLabel.setText("HTTPS Proxy:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 5);
        manualSettingsPanel.add(httpsProxyLabel, c);

        httpsProxyField = new JTextField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpsProxyField, c);

        httpsProxyPortLabel = new JLabel();
        httpsProxyPortLabel.setText("Port:");
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 8, 0, 5);
        manualSettingsPanel.add(httpsProxyPortLabel, c);

        httpsProxySpinnerModel = new SpinnerNumberModel(0, 0, 65535, 1);
        httpsProxyPortSpinner = new JExtendedSpinner(httpsProxySpinnerModel);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(httpsProxyPortSpinner, c);

        // --- Socks proxy ---

        socksProxyLabel = new JLabel();
        socksProxyLabel.setText("SOCKS Proxy:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 5);
        manualSettingsPanel.add(socksProxyLabel, c);

        socksProxyField = new JTextField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(socksProxyField, c);

        socksProxyPortLabel = new JLabel();
        socksProxyPortLabel.setText("Port:");
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 8, 0, 5);
        manualSettingsPanel.add(socksProxyPortLabel, c);

        socksProxySpinnerModel = new SpinnerNumberModel(0, 0, 65535, 1);
        socksProxyPortSpinner = new JExtendedSpinner(socksProxySpinnerModel);
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(socksProxyPortSpinner, c);

        // --- No proxy ---

        noProxyLabel = new JLabel();
        noProxyLabel.setText("No Proxy hosts:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 5);
        manualSettingsPanel.add(noProxyLabel, c);

        noProxyField = new JTextField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 0, 3, 0);
        manualSettingsPanel.add(noProxyField, c);

        JLabel sameSettingsLabel = new JLabel();
        sameSettingsLabel.setText("(e.g. localhost, *.dev.java.net, 127.0.0.1)");
        sameSettingsLabel.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        manualSettingsPanel.add(sameSettingsLabel, c);

        // --- Authentication ---

        authenticationCheckBox = new JCheckBox();
        authenticationCheckBox.setText("Proxy requires authentication:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 0, 0);
        manualSettingsPanel.add(authenticationCheckBox, c);
        
        usernameLabel = new JLabel();
        usernameLabel.setText("Username:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 30, 0, 5);
        manualSettingsPanel.add(usernameLabel, c);

        usernameField = new JTextField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(usernameField, c);

        passwordLabel = new JLabel();
        passwordLabel.setText("Password:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 8;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 30, 0, 5);
        manualSettingsPanel.add(passwordLabel, c);

        passwordField = new JPasswordField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 8;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(3, 0, 3, 0);
        manualSettingsPanel.add(passwordField, c);

        // --- Filler ---

        JPanel fillerPanel = new JPanel(null);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 9;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        manualSettingsPanel.add(fillerPanel, c);


        c = new GridBagConstraints();
        c.gridy = 3;
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

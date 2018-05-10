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

package org.graalvm.visualvm.core.options;

import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.uisupport.JExtendedSpinner;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
final class GeneralOptionsPanel extends JPanel {

    private final GeneralOptionsPanelController controller;

    transient private final ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            controller.changed();
        }
    };
    
    GeneralOptionsPanel(GeneralOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        startTrackingChanges();
    }


    private void initComponents() {
        GridBagConstraints c;

        setLayout(new GridBagLayout());

        // --- Polling ---

        SectionSeparator pollingSection = UISupport.createSectionSeparator(NbBundle.getMessage
                                          (GeneralOptionsPanel.class, "LBL_Polling")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(pollingSection, c);

        monitoredHostPLabel = new JLabel();
        Mnemonics.setLocalizedText(monitoredHostPLabel, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Monitored_Host")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(monitoredHostPLabel, c);

        monitoredHostPSpinner = new JExtendedSpinner();
        monitoredHostPLabel.setLabelFor(monitoredHostPSpinner);
        monitoredHostPSpinner.setModel(new SpinnerNumberModel(3, 1, 99999, 1));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 40, 3, 4);
        add(monitoredHostPSpinner, c);

        monitoredHostPUnits = new JLabel();
        Mnemonics.setLocalizedText(monitoredHostPUnits, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Sec")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(monitoredHostPUnits, c);
        
        threadsPLabel = new JLabel();
        Mnemonics.setLocalizedText(threadsPLabel, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Threads")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(threadsPLabel, c);

        threadsPSpinner = new JExtendedSpinner();
        threadsPLabel.setLabelFor(threadsPSpinner);
        threadsPSpinner.setModel(new SpinnerNumberModel(1, 1, 99999, 1));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 40, 3, 4);
        add(threadsPSpinner, c);

        threadsPUnits = new JLabel();
        Mnemonics.setLocalizedText(threadsPUnits, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Sec")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(threadsPUnits, c);

        monitoredDataPLabel = new JLabel();
        Mnemonics.setLocalizedText(monitoredDataPLabel, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Monitored_Data")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(monitoredDataPLabel, c);

        monitoredDataPSpinner = new JExtendedSpinner();
        monitoredDataPLabel.setLabelFor(monitoredDataPSpinner);
        monitoredDataPSpinner.setModel(new SpinnerNumberModel(1, 1, 99999, 1));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 40, 3, 4);
        add(monitoredDataPSpinner, c);

        monitoredDataPUnits = new JLabel();
        Mnemonics.setLocalizedText(monitoredDataPUnits, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Sec")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(monitoredDataPUnits, c);

        // --- Charts cache ---

        SectionSeparator chartsCacheSection = UISupport.createSectionSeparator(NbBundle.getMessage
                                          (GeneralOptionsPanel.class, "LBL_Charts_Cache")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(chartsCacheSection, c);

        monitoredHostCLabel = new JLabel();
        Mnemonics.setLocalizedText(monitoredHostCLabel, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Monitored_Host2")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(monitoredHostCLabel, c);

        monitoredHostCSpinner = new JExtendedSpinner();
        monitoredHostCLabel.setLabelFor(monitoredHostCSpinner);
        monitoredHostCSpinner.setModel(new SpinnerNumberModel(60, 1, 99999, 1));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 40, 3, 4);
        add(monitoredHostCSpinner, c);

        monitoredHostCUnits = new JLabel();
        Mnemonics.setLocalizedText(monitoredHostCUnits, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_min")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 5;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(monitoredHostCUnits, c);

        monitoredDataCLabel = new JLabel();
        Mnemonics.setLocalizedText(monitoredDataCLabel, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_Monitored_Data2")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(monitoredDataCLabel, c);

        monitoredDataCSpinner = new JExtendedSpinner();
        monitoredDataCLabel.setLabelFor(monitoredDataCSpinner);
        monitoredDataCSpinner.setModel(new SpinnerNumberModel(60, 1, 99999, 1));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 6;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 40, 3, 4);
        add(monitoredDataCSpinner, c);

        monitoredDataCUnits = new JLabel();
        Mnemonics.setLocalizedText(monitoredDataCUnits, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "LBL_min")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 6;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(monitoredDataCUnits, c);

        // --- Misc ---

        SectionSeparator profilerSection = UISupport.createSectionSeparator(NbBundle.getMessage
                                          (GeneralOptionsPanel.class, "LBL_Miscellaneous")); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 7;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(profilerSection, c);

        JPanel resetDNSAPanel = new JPanel(new BorderLayout());

        resetDNSALabel = new JLabel();
        Mnemonics.setLocalizedText(resetDNSALabel, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "MSG_Do_Not_Show_Again")); // NOI18N
        resetDNSAPanel.add(resetDNSALabel, BorderLayout.CENTER);

        resetDNSAButton = new JButton();
        Mnemonics.setLocalizedText(resetDNSAButton, NbBundle.getMessage(
                                   GeneralOptionsPanel.class, "BTN_Reset")); // NOI18N
        resetDNSAPanel.add(resetDNSAButton, BorderLayout.EAST);

        c = new GridBagConstraints();
        c.gridy = 8;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 15, 2, 0);
        add(resetDNSAPanel, c);

        // --- Filler ---

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 10;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(Spacer.create(), c);
    }

    private void resetDNSAButtonAction() {
        ProfilerIDESettings.getInstance().clearDoNotShowAgainMap();
        resetDNSAButton.setEnabled(false);
    }

    void load() {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(CorePanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(CorePanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
        GlobalPreferences preferences = GlobalPreferences.sharedInstance();
        resetDNSAButton.setEnabled(true);
        monitoredHostPSpinner.setValue(preferences.getMonitoredHostPoll());
        monitoredDataPSpinner.setValue(preferences.getMonitoredDataPoll());
        threadsPSpinner.setValue(preferences.getThreadsPoll());
        monitoredHostCSpinner.setValue(preferences.getMonitoredHostCache());
        monitoredDataCSpinner.setValue(preferences.getMonitoredDataCache());
    }

    void store() {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(CorePanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(CorePanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        GlobalPreferences preferences = GlobalPreferences.sharedInstance();
        preferences.setMonitoredHostPoll((Integer) monitoredHostPSpinner.getValue());
        preferences.setMonitoredDataPoll((Integer) monitoredDataPSpinner.getValue());
        preferences.setThreadsPoll((Integer) threadsPSpinner.getValue());
        preferences.setMonitoredHostCache((Integer) monitoredHostCSpinner.getValue());
        preferences.setMonitoredDataCache((Integer) monitoredDataCSpinner.getValue());
        preferences.store();
    }

    boolean valid() {
        try {
            int mh = (Integer) monitoredHostPSpinner.getValue();
            int md = (Integer) monitoredDataPSpinner.getValue();
            int th = (Integer) threadsPSpinner.getValue();
            int mhc = (Integer) monitoredHostCSpinner.getValue();
            int mdc = (Integer) monitoredDataCSpinner.getValue();
            return mh > 0 && md > 0 && th > 0 && mhc > 0 && mdc > 0;
        } catch (Exception e) {
        }
        return false;
    }

    private void startTrackingChanges() {
        monitoredHostPSpinner.getModel().addChangeListener(changeListener);
        threadsPSpinner.getModel().addChangeListener(changeListener);
        monitoredDataPSpinner.getModel().addChangeListener(changeListener);
        monitoredHostCSpinner.getModel().addChangeListener(changeListener);
        monitoredDataCSpinner.getModel().addChangeListener(changeListener);
        resetDNSAButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetDNSAButtonAction();
            }
        });
    }


    private JLabel monitoredHostPLabel;
    private JSpinner monitoredHostPSpinner;
    private JLabel monitoredHostPUnits;
    private JLabel threadsPLabel;
    private JSpinner threadsPSpinner;
    private JLabel threadsPUnits;
    private JLabel monitoredDataPLabel;
    private JSpinner monitoredDataPSpinner;
    private JLabel monitoredDataPUnits;
    private JLabel monitoredHostCLabel;
    private JSpinner monitoredHostCSpinner;
    private JLabel monitoredHostCUnits;
    private JLabel monitoredDataCLabel;
    private JSpinner monitoredDataCSpinner;
    private JLabel monitoredDataCUnits;
    private JLabel resetDNSALabel;
    private JButton resetDNSAButton;
    
}

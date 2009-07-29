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

package com.sun.tools.visualvm.modules.mbeans.options;

import com.sun.tools.visualvm.core.options.UISupport;
import com.sun.tools.visualvm.core.ui.components.SectionSeparator;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class MBeansOptionsPanel extends JPanel {

    private final MBeansOptionsPanelController controller;

    private final ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            controller.changed();
        }
    };


    MBeansOptionsPanel(MBeansOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        startTrackingChanges();
    }

    void load() {
        // TODO read settings and initialize GUI
        // Example:
        // someCheckBox.setSelected(Preferences.userNodeForPackage(CorePanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(CorePanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
        plottersSpinner.setValue(GlobalPreferences.sharedInstance().getPlottersPoll());
        propertyListField.setText(GlobalPreferences.sharedInstance().getOrderedKeyPropertyList());
    }

    void store() {
        GlobalPreferences.sharedInstance().setPlottersPoll((Integer) plottersSpinner.getValue());
        GlobalPreferences.sharedInstance().setOrderedKeyPropertyList(propertyListField.getText());
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(CorePanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(CorePanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        GlobalPreferences.sharedInstance().store();
    }

    boolean valid() {
        try {
            return (Integer)plottersSpinner.getValue() > 0;
        } catch (Exception e) {}
        return false;
    }


    private void initComponents() {
        GridBagConstraints c;

        setLayout(new GridBagLayout());

        // pollingSeparator
        SectionSeparator pollingSeparator = UISupport.createSectionSeparator(
                NbBundle.getMessage(MBeansOptionsPanel.class, "LBL_MBeansBrowser"));  // NOI18N
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(pollingSeparator, c);

        // plottersLabel
        JLabel plottersLabel = new JLabel();
        Mnemonics.setLocalizedText(plottersLabel, NbBundle.getMessage(
                                   MBeansOptionsPanel.class, "LBL_Plotters")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(plottersLabel, c);

        // plottersSpinner
        plottersSpinner = new JSpinner();
        plottersLabel.setLabelFor(plottersSpinner);
        plottersSpinner.setModel(new SpinnerNumberModel(3, 1, 99999, 1));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 4);
        add(plottersSpinner, c);

        // plottersUnits
        JLabel plottersUnits = new JLabel();
        Mnemonics.setLocalizedText(plottersUnits, NbBundle.getMessage(
                MBeansOptionsPanel.class, "LBL_Sec")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);
        add(plottersUnits, c);

        // propertyListLabel
        JLabel propertyListLabel = new JLabel();
        Mnemonics.setLocalizedText(propertyListLabel, NbBundle.getMessage(
                MBeansOptionsPanel.class, "LBL_Ordered_Key_Property_List")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(propertyListLabel, c);

        // propertyListField
        propertyListField = new JTextField();
        propertyListLabel.setLabelFor(propertyListField);
        propertyListField.setToolTipText(NbBundle.getMessage(
                MBeansOptionsPanel.class, "MSG_CommaSeparatedListOfKeys")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        add(propertyListField, c);

        // filler
        JPanel fillerPanel = new JPanel(null);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(fillerPanel, c);
    }

    private void startTrackingChanges() {
        plottersSpinner.getModel().addChangeListener(changeListener);
    }

    
    private JSpinner plottersSpinner;
    private JTextField propertyListField;

}

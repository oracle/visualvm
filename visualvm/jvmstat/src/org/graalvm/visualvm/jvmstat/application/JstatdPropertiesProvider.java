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

package org.graalvm.visualvm.jvmstat.application;

import org.graalvm.visualvm.core.properties.PropertiesPanel;
import org.graalvm.visualvm.core.properties.PropertiesProvider;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModelFactory;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.openide.util.NbBundle;
import org.openide.awt.Mnemonics;

/**
 *
 * @author Jiri Sedlacek
 */
class JstatdPropertiesProvider extends PropertiesProvider<JvmstatApplication> {

    /**
     * Key for the "jstatd Connection" properties category.
     */
    public static final int CATEGORY_JSTATD_CONNECTION = 200;


    JstatdPropertiesProvider() {
        super(NbBundle.getMessage(JstatdPropertiesProvider.class, "LBL_JstatdAppProperties"), // NOI18N
              NbBundle.getMessage(JstatdPropertiesProvider.class, "DESCR_JstatdAppProperties"), // NOI18N
              CATEGORY_JSTATD_CONNECTION, 0);
    }


    public PropertiesPanel createPanel(JvmstatApplication dataSource) {
        PropertiesPanel panel = new PropertiesPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // connectionLabel
        JLabel connectionLabel = new JLabel();
        Mnemonics.setLocalizedText(connectionLabel, NbBundle.getMessage(
                JstatdPropertiesProvider.class, "LBL_Connection")); // NOI18N
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 0, 3, 5);
        panel.add(connectionLabel, constraints);

        // connectionField
        JTextField connectionField = new JTextField(JvmstatModelFactory.getJvmstatFor(dataSource).getConnectionId());
        connectionLabel.setLabelFor(connectionField);
        connectionField.setCaretPosition(0);
        connectionField.setEditable(false);
        Dimension size = connectionField.getPreferredSize();
        size.width = 1;
        connectionField.setPreferredSize(size);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 0, 3, 0);
        panel.add(connectionField, constraints);

        // Filler
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(Spacer.create(), constraints);

        return panel;
    }


    public boolean supportsDataSource(JvmstatApplication dataSource) {
        return dataSource != null && !PropertiesImpl.isLocalConnection(dataSource);
    }

    public void propertiesDefined(PropertiesPanel panel, JvmstatApplication dataSource) {}

    public void propertiesChanged(PropertiesPanel panel, JvmstatApplication dataSource) {}

    public void propertiesCancelled(PropertiesPanel panel, JvmstatApplication dataSource) {}

}

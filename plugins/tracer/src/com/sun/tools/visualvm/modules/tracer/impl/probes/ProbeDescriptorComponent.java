/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.probes;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProbeDescriptorComponent extends JPanel {

    public ProbeDescriptorComponent(final TracerProbeDescriptor descriptor,
                                    final TracerPackage p,
                                    final SelectionHandler handler) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        GridBagConstraints c;

        JCheckBox checkBox = new JCheckBox() {
            protected void fireActionPerformed(ActionEvent event) {
                if (isSelected()) handler.descriptorSelected(p, descriptor);
                else handler.descriptorUnselected(p, descriptor);
            }
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled && descriptor.isProbeAvailable());
            }
        };
        checkBox.setOpaque(false);
        checkBox.setEnabled(descriptor.isProbeAvailable());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 15, 3, 5);
        add(checkBox, c);

        JLabel icon = new JLabel() {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled && descriptor.isProbeAvailable());
            }
        };
        Icon ic = descriptor.getProbeIcon();
        icon.setIcon(ic);
        icon.setEnabled(descriptor.isProbeAvailable());
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 5, 3, 5);
        add(icon, c);

        JLabel name = new JLabel(descriptor.getProbeName()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled && descriptor.isProbeAvailable());
            }
        };
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        name.setEnabled(descriptor.isProbeAvailable());
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 10, 0, 5);
        add(name, c);

        JLabel descr = new JLabel(descriptor.getProbeDescription()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled && descriptor.isProbeAvailable());
            }
        };
        descr.setFont(descr.getFont().deriveFont(Font.PLAIN));
        descr.setEnabled(descriptor.isProbeAvailable());
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 10, 3, 5);
        add(descr, c);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Component[] components = getComponents();
        for (Component c : components) c.setEnabled(enabled);
    }


    public static interface SelectionHandler {

        public void descriptorSelected(TracerPackage<DataSource> p, TracerProbeDescriptor d);

        public void descriptorUnselected(TracerPackage<DataSource> p, TracerProbeDescriptor d);

    }

}

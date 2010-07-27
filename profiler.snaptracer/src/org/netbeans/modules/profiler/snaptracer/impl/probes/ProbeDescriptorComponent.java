/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.snaptracer.impl.probes;

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
import org.netbeans.modules.profiler.snaptracer.TracerPackage;
import org.netbeans.modules.profiler.snaptracer.TracerProbeDescriptor;

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

        public void descriptorSelected(TracerPackage p, TracerProbeDescriptor d);

        public void descriptorUnselected(TracerPackage p, TracerProbeDescriptor d);

    }

}

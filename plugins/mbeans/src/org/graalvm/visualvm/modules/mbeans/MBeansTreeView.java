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

package org.graalvm.visualvm.modules.mbeans;

import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

/**
 *
 * @author Jiri Sedlacek
 */
class MBeansTreeView extends JPanel implements PropertyChangeListener {

    private MBeansTab mbeansTab;

    public MBeansTreeView(MBeansTab mbeansTab) {
        this.mbeansTab = mbeansTab;
        initComponents();
    }

    public void dispose() {
        removePropertyChangeListener(this);
        mbeansTab.getTree().clearSelection();
        mbeansTab.getTree().setEnabled(false);
    }

    /* property change listener:  propertyChange */
    public void propertyChange(PropertyChangeEvent evt) {
        if (JmxModel.CONNECTION_STATE_PROPERTY.equals(evt.getPropertyName())) {
            ConnectionState newState = (ConnectionState) evt.getNewValue();
            switch (newState) {
                case DISCONNECTED:
                    dispose();
                    break;
            }
        }
    }


    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(220, 1));

        JTree mbeansTree = mbeansTab.getTree();
        mbeansTab.buildMBeanServerView();

        JScrollPane mbeansTreeScrollPane = new JScrollPane(mbeansTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mbeansTreeScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());

        add(mbeansTreeScrollPane, BorderLayout.CENTER);
    }
}

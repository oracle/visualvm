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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

class NetworkOptionsPanelController extends OptionsPanelController {

    private NetworkOptionsModel model;
    private NetworkOptionsPanel panel;
    private JComponent component;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;


    public void update() {
        getPanel().update ();
    }

    public void applyChanges() {
        getPanel().applyChanges ();
    }

    public void cancel() {
        getPanel().cancel ();
    }

    public boolean isValid() {
        return getPanel().dataValid ();
    }

    public boolean isChanged() {
        return getPanel().isChanged ();
    }


    public HelpCtx getHelpCtx() {
        return null;

    }


    public JComponent getComponent(Lookup masterLookup) {
        return getComponent();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }


    NetworkOptionsModel getModel() {
        if (model == null) model = new NetworkOptionsModel();
        return model;
    }


    private NetworkOptionsPanel getPanel() {
        if (panel == null) panel = new NetworkOptionsPanel(this);
        return panel;
    }

    private JComponent getComponent() {
        if (component == null) {
            component = UISupport.createScrollableContainer(getPanel());
        }
        return component;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}

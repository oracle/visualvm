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

package com.sun.tools.visualvm.modules.tracer.impl.options;

import com.sun.tools.visualvm.core.options.UISupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerOptionsPanelController extends OptionsPanelController {

    private TracerOptions options = TracerOptions.getInstance();
    private TracerOptionsPanel panel;
    private JComponent component;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;


    public void update() {
        TracerOptionsPanel p = getPanel();

        p.setInitiallyOpened(options.getInitiallyOpened());
        p.setOnProbeAdded(options.getOnProbeAdded());
        p.setOnSessionStart(options.getOnSessionStart());

        p.setZoomMode(options.getZoomMode());
        p.setMouseWheelAction(options.getMouseWheelAction());

        p.update();
    }

    public void applyChanges() {
        TracerOptionsPanel p = getPanel();

        options.setInitiallyOpened(p.getInitiallyOpened());
        options.setOnProbeAddded(p.getOnProbeAdded());
        options.setOnSessionStart(p.getOnSessionStart());

        options.setZoomMode(p.getZoomMode());
        options.setMouseWheelAction(p.getMouseWheelAction());
    }

    public void cancel() {}

    public boolean isValid() {
        return getPanel().dataValid();
    }

    public boolean isChanged() {
        TracerOptionsPanel p = getPanel();

        if (!options.getInitiallyOpened().equals(p.getInitiallyOpened())) return true;
        if (!options.getOnProbeAdded().equals(p.getOnProbeAdded())) return true;
        if (!options.getOnSessionStart().equals(p.getOnSessionStart())) return true;

        if (!options.getZoomMode().equals(p.getZoomMode())) return true;
        if (!options.getMouseWheelAction().equals(p.getMouseWheelAction())) return true;

        return false;
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


    private TracerOptionsPanel getPanel() {
        if (panel == null) panel = new TracerOptionsPanel(this);
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

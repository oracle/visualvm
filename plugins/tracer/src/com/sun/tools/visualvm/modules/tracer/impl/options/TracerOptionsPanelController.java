/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
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

        p.setProbesApp(options.getProbesApp());

        p.setRefresh(options.getRefresh());
        p.setRefreshCustomizable(options.isRefreshCustomizable());

        p.setShowValuesEnabled(options.isShowValuesEnabled());
        p.setShowLegendEnabled(options.isShowLegendEnabled());
        p.setRowsDecorationEnabled(options.isRowsDecorationEnabled());
        p.setRowsSelectionEnabled(options.isRowsSelectionEnabled());

        p.setInitiallyOpened(options.getInitiallyOpened());
        p.setOnProbeAdded(options.getOnProbeAdded());
        p.setOnProbeAdded2(options.getOnProbeAdded2());
        p.setOnSessionStart(options.getOnSessionStart());
        p.setOnRowSelected(options.getOnRowSelected());
        p.setOnRowSelected2(options.getOnRowSelected2());

        p.setZoomMode(options.getZoomMode());
        p.setMouseWheelAction(options.getMouseWheelAction());

        p.setTimelineToolbar(options.getTimelineToolbar());
        p.setSelectionToolbar(options.getSelectionToolbar());
        p.setExtraToolbar(options.getExtraToolbar());

        p.setClearSelection(options.isClearSelection());

        p.update();
    }

    public void applyChanges() {
        TracerOptionsPanel p = getPanel();

        options.setProbesApp(p.getProbesApp());

        options.setRefresh(p.getRefresh());
        options.setRefreshCustomizable(p.isRefreshCustomizable());

        options.setShowValuesEnabled(p.isShowValuesEnabled());
        options.setShowLegendEnabled(p.isShowLegendEnabled());
        options.setRowsDecorationEnabled(p.isRowsDecorationEnabled());
        options.setRowsSelectionEnabled(p.isRowsSelectionEnabled());

        options.setInitiallyOpened(p.getInitiallyOpened());
        options.setOnProbeAdded(p.getOnProbeAdded());
        options.setOnProbeAdded2(p.getOnProbeAdded2());
        options.setOnSessionStart(p.getOnSessionStart());
        options.setOnRowSelected(p.getOnRowSelected());
        options.setOnRowSelected2(p.getOnRowSelected2());

        options.setZoomMode(p.getZoomMode());
        options.setMouseWheelAction(p.getMouseWheelAction());

        options.setTimelineToolbar(p.getTimelineToolbar());
        options.setSelectionToolbar(p.getSelectionToolbar());
        options.setExtraToolbar(p.getExtraToolbar());

        options.setClearSelection(p.isClearSelection());
    }

    public void cancel() {}

    public boolean isValid() {
        return getPanel().dataValid();
    }

    public boolean isChanged() {
        TracerOptionsPanel p = getPanel();

        if (options.getProbesApp() != p.getProbesApp()) return true;

        if (options.getRefresh() != p.getRefresh()) return true;
        if (options.isRefreshCustomizable() != p.isRefreshCustomizable()) return true;

        if (options.isShowValuesEnabled() != p.isShowValuesEnabled()) return true;
        if (options.isShowLegendEnabled() != p.isShowLegendEnabled()) return true;
        if (options.isRowsDecorationEnabled() != p.isRowsDecorationEnabled()) return true;
        if (options.isRowsSelectionEnabled() != p.isRowsSelectionEnabled()) return true;

        if (!options.getInitiallyOpened().equals(p.getInitiallyOpened())) return true;
        if (!options.getOnProbeAdded().equals(p.getOnProbeAdded())) return true;
        if (!options.getOnProbeAdded2().equals(p.getOnProbeAdded2())) return true;
        if (!options.getOnSessionStart().equals(p.getOnSessionStart())) return true;
        if (!options.getOnRowSelected().equals(p.getOnRowSelected())) return true;
        if (!options.getOnRowSelected2().equals(p.getOnRowSelected2())) return true;

        if (!options.getZoomMode().equals(p.getZoomMode())) return true;
        if (!options.getMouseWheelAction().equals(p.getMouseWheelAction())) return true;

        if (options.getTimelineToolbar() != p.getTimelineToolbar()) return true;
        if (options.getSelectionToolbar() != p.getSelectionToolbar()) return true;
        if (options.getExtraToolbar() != p.getExtraToolbar()) return true;

        if (options.isClearSelection() != p.isClearSelection()) return true;

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

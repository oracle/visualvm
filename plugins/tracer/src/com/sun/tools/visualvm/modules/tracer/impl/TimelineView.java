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

package com.sun.tools.visualvm.modules.tracer.impl;

import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.TimelinePanel;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineView {

    private final TracerModel model;
    private TimelinePanel panel;

    private ViewListener viewListener;


    // --- Constructor ---------------------------------------------------------

    TimelineView(TracerModel model) {
        this.model = model;
    }


    // --- Internal interface --------------------------------------------------

    void reset() {
        if (panel != null) panel.reset();
    }

    Action zoomInAction() {
        if (panel != null) return panel.zoomInAction();
        return null;
    }

    Action zoomOutAction() {
        if (panel != null) return panel.zoomOutAction();
        return null;
    }

    Action toggleViewAction() {
        if (panel != null) return panel.toggleViewAction();
        return null;
    }

    AbstractButton mouseZoom() {
        if (panel != null) return panel.mouseZoom();
        return null;
    }

    AbstractButton mouseHScroll() {
        if (panel != null) return panel.mouseHScroll();
        return null;
    }

    AbstractButton mouseVScroll() {
        if (panel != null) return panel.mouseVScroll();
        return null;
    }


    void registerViewListener(ViewListener viewListener) {
        if (panel != null) {
            registerListener(panel, viewListener);
        } else {
            this.viewListener = viewListener;
        }

    }


    // --- UI implementation ---------------------------------------------------

    DataViewComponent.DetailsView getView() {
        panel = new TimelinePanel(model.getTimelineSupport());

        if (viewListener != null) {
            registerListener(panel, viewListener);
            viewListener = null;
        }

        return new DataViewComponent.DetailsView("Timeline", null, 10, panel, null);
    }


    // --- Private implementation ----------------------------------------------

    private static void registerListener(final JComponent c, final ViewListener l) {
        c.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (c.isShowing()) l.viewShown();
                    else l.viewHidden();
                }
            }
        });
    }

    
    // --- View visibility listener --------------------------------------------

    static interface ViewListener {

        public void viewShown();

        public void viewHidden();

    }

}

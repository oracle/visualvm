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

package org.netbeans.lib.profiler.ui.charts.xy;

import java.awt.event.ActionEvent;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.PaintersModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChart;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYChart extends SynchronousXYChart {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.charts.xy.Bundle"); // NOI18N
    private static final String ZOOM_IN_STRING = messages.getString("ProfilerXYChart_ZoomInString"); // NOI18N
    private static final String ZOOM_OUT_STRING = messages.getString("ProfilerXYChart_ZoomOutString"); // NOI18N
    private static final String FIXED_SCALE_STRING = messages.getString("ProfilerXYChart_FixedScaleString"); // NOI18N
    private static final String SCALE_TO_FIT_STRING = messages.getString("ProfilerXYChart_ScaleToFitString"); // NOI18N
    // -----

    private static final Icon ZOOM_IN_ICON =
            new ImageIcon(ProfilerXYChart.class.getResource(
            "/org/netbeans/lib/profiler/ui/resources/zoomIn.png")); // NOI18N
    private static final Icon ZOOM_OUT_ICON =
            new ImageIcon(ProfilerXYChart.class.getResource(
            "/org/netbeans/lib/profiler/ui/resources/zoomOut.png")); // NOI18N
    private static final Icon FIXED_SCALE_ICON =
            new ImageIcon(ProfilerXYChart.class.getResource(
            "/org/netbeans/lib/profiler/ui/resources/zoom.png")); // NOI18N
    private static final Icon SCALE_TO_FIT_ICON =
            new ImageIcon(ProfilerXYChart.class.getResource(
            "/org/netbeans/lib/profiler/ui/resources/scaleToFit.png")); // NOI18N


    private ZoomInAction zoomInAction;
    private ZoomOutAction zoomOutAction;
    private ToggleViewAction toggleViewAction;


    // --- Constructors --------------------------------------------------------

    public ProfilerXYChart(SynchronousXYItemsModel itemsModel,
                           PaintersModel paintersModel) {
        super(itemsModel, paintersModel);

        setBottomBased(true);
        setFitsHeight(true);

        setMousePanningEnabled(false);

        addConfigurationListener(new VisibleBoundsListener());
    }


    // --- Public interface ----------------------------------------------------

    public Action zoomInAction() {
        if (zoomInAction == null) zoomInAction = new ZoomInAction();
        return zoomInAction;
    }

    public Action zoomOutAction() {
        if (zoomOutAction == null) zoomOutAction = new ZoomOutAction();
        return zoomOutAction;
    }

    public Action toggleViewAction() {
        if (toggleViewAction == null) toggleViewAction = new ToggleViewAction();
        return toggleViewAction;
    }


    // --- Actions support -----------------------------------------------------

    private class ZoomInAction extends AbstractAction {

        private static final int ONE_SECOND_WIDTH_THRESHOLD = 200;

        public ZoomInAction() {
            super();

            putValue(SHORT_DESCRIPTION, ZOOM_IN_STRING);
            putValue(SMALL_ICON, ZOOM_IN_ICON);

            updateAction();
        }

        public void actionPerformed(ActionEvent e) {
            boolean followsWidth = currentlyFollowingDataWidth();
            zoom(getWidth() / 2, getHeight() / 2, 2d);
            if (followsWidth) setOffset(getMaxOffsetX(), getOffsetY());
            
            repaintDirty();
        }

        private void updateAction() {
            Timeline timeline = ((SynchronousXYItemsModel)getItemsModel()).getTimeline();
            setEnabled(timeline.getTimestampsCount() > 1 && !fitsWidth() &&
                       getViewWidth(1000) < ONE_SECOND_WIDTH_THRESHOLD);
        }

    }

    private class ZoomOutAction extends AbstractAction {

        private static final float USED_CHART_WIDTH_THRESHOLD = 0.33f;

        public ZoomOutAction() {
            super();

            putValue(SHORT_DESCRIPTION, ZOOM_OUT_STRING);
            putValue(SMALL_ICON, ZOOM_OUT_ICON);

            updateAction();
        }

        public void actionPerformed(ActionEvent e) {
            boolean followsWidth = currentlyFollowingDataWidth();
            zoom(getWidth() / 2, getHeight() / 2, 0.5d);
            if (followsWidth) setOffset(getMaxOffsetX(), getOffsetY());
            
            repaintDirty();
        }

        private void updateAction() {
            Timeline timeline = ((SynchronousXYItemsModel)getItemsModel()).getTimeline();
            setEnabled(timeline.getTimestampsCount() > 0 && !fitsWidth() &&
                       getContentsWidth() > getWidth() * USED_CHART_WIDTH_THRESHOLD);
        }

    }

    private class ToggleViewAction extends AbstractAction {

        private long origOffsetX  = -1;
        private double origScaleX = -1;

        public ToggleViewAction() {
            super();
            updateAction();
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e))
                        actionPerformed(null);
                }
            });
        }

        public void actionPerformed(ActionEvent e) {
            boolean fitsWidth = fitsWidth();

            if (!fitsWidth) {
                origOffsetX = getOffsetX();
                if (tracksDataWidth() && origOffsetX == getMaxOffsetX())
                    origOffsetX = Long.MAX_VALUE;
                origScaleX  = getScaleX();
            }

            setFitsWidth(!fitsWidth);
            
            if (fitsWidth && origOffsetX != -1 && origScaleX != -1) {
                setScale(origScaleX, getScaleY());
                setOffset(origOffsetX, getOffsetY());
            }

            updateAction();
            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();
            
            repaintDirty();
            
        }

        private void updateAction() {
            boolean fitsWidth = fitsWidth();
            Icon icon = fitsWidth ? FIXED_SCALE_ICON : SCALE_TO_FIT_ICON;
            String name = fitsWidth ? FIXED_SCALE_STRING : SCALE_TO_FIT_STRING;
            putValue(SHORT_DESCRIPTION, name);
            putValue(SMALL_ICON, icon);
        }

    }


    // --- ChartConfigurationListener implementation ---------------------------

    private class VisibleBoundsListener extends ChartConfigurationListener.Adapter {

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();
        }

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();
        }
    }

}

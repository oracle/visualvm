/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.charts.xy;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.PaintersModel;
import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYChart;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYChart extends SynchronousXYChart {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.charts.xy.Bundle"); // NOI18N
    private static final String ZOOM_IN_STRING = messages.getString("ProfilerXYChart_ZoomInString"); // NOI18N
    private static final String ZOOM_OUT_STRING = messages.getString("ProfilerXYChart_ZoomOutString"); // NOI18N
    private static final String FIXED_SCALE_STRING = messages.getString("ProfilerXYChart_FixedScaleString"); // NOI18N
    private static final String SCALE_TO_FIT_STRING = messages.getString("ProfilerXYChart_ScaleToFitString"); // NOI18N
    // -----

    private static final Icon ZOOM_IN_ICON = Icons.getIcon(GeneralIcons.ZOOM_IN);
    private static final Icon ZOOM_OUT_ICON = Icons.getIcon(GeneralIcons.ZOOM_OUT);
    private static final Icon FIXED_SCALE_ICON = Icons.getIcon(GeneralIcons.ZOOM);
    private static final Icon SCALE_TO_FIT_ICON = Icons.getIcon(GeneralIcons.SCALE_TO_FIT);


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

//        private static final int ONE_SECOND_WIDTH_THRESHOLD = 200;

        ZoomInAction() {
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
            setEnabled(timeline.getTimestampsCount() > 1 && !fitsWidth() /*&&
                       getViewWidth(1000) < ONE_SECOND_WIDTH_THRESHOLD*/);
            // #165429 - don't disable zoom icons until mouse zoom is also disabled
        }

    }

    private class ZoomOutAction extends AbstractAction {

//        private static final float USED_CHART_WIDTH_THRESHOLD = 0.33f;

        ZoomOutAction() {
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
            setEnabled(timeline.getTimestampsCount() > 1 && !fitsWidth() /*&&
                       getContentsWidth() > getWidth() * USED_CHART_WIDTH_THRESHOLD*/);
            // #165429 - don't disable zoom icons until mouse zoom is also disabled
        }

    }

    private class ToggleViewAction extends AbstractAction {

        private long origOffsetX  = -1;
        private double origScaleX = -1;

        ToggleViewAction() {
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

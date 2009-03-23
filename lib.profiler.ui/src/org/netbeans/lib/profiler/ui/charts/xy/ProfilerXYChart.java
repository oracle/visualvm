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
import org.netbeans.lib.profiler.charts.xy.XYTimeline;
import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.PaintersModel;
import java.awt.Rectangle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYChart extends ChartComponent {

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

    private final XYTimeline timeline;

    private ZoomInAction zoomInAction;
    private ZoomOutAction zoomOutAction;
    private ToggleViewAction toggleViewAction;

    private int firstVisibleIndex;
    private int lastVisibleIndex;


    // --- Constructors --------------------------------------------------------

    private ProfilerXYChart() {
        throw new UnsupportedOperationException(
                "new ProfilerXYChartComponent() not supported"); // NOI18N
    }

    public ProfilerXYChart(ProfilerXYItemsModel itemsModel,
                                    PaintersModel paintersModel) {
        super();

        setBottomBased(true);
        setFitsHeight(true);

        setMousePanningEnabled(false);

        setItemsModel(itemsModel);
        setPaintersModel(paintersModel);

        timeline = itemsModel.getTimeline();

        firstVisibleIndex = 0;
        lastVisibleIndex  = 0;

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


    // --- Protected implementation --------------------------------------------

    protected Context createChartContext() {
        return new Context(this);
    }


    // --- Private implementation ----------------------------------------------

    private void updateFirstIndex(boolean searchToRight) {
        if (timeline.getTimestampsCount() == 0) {
            firstVisibleIndex = 0;
        } else {
            if (searchToRight) {
                while(firstVisibleIndex + 1 < timeline.getTimestampsCount() &&
                      getViewX(timeline.getTimestamp(firstVisibleIndex)) < 0)
                    firstVisibleIndex++;
            } else {
                while(firstVisibleIndex > 0 &&
                      getViewX(timeline.getTimestamp(firstVisibleIndex - 1)) >= 0)
                    firstVisibleIndex--;
            }
        }
    }

    private void updateLastIndex(boolean searchToRight) {
        if (timeline.getTimestampsCount() == 0) {
            lastVisibleIndex = 0;
        } else {
            if (searchToRight) {
                while(lastVisibleIndex + 1 < timeline.getTimestampsCount() &&
                      getViewX(timeline.getTimestamp(lastVisibleIndex + 1)) <= getWidth())
                    lastVisibleIndex++;
            } else {
                while(lastVisibleIndex > 0 &&
                      getViewX(timeline.getTimestamp(lastVisibleIndex)) > getWidth())
                    lastVisibleIndex--;
            }
        }
    }

    private void resetFirstIndex() {
        firstVisibleIndex = 0;
    }

    private void resetLastIndex() {
        int timestampsCount = timeline.getTimestampsCount();
        lastVisibleIndex = timestampsCount == 0 ? 0 : timestampsCount - 1;
    }

    // Use in case of absolute panic, will always work
    private void recomputeVisibleBounds() {
        resetFirstIndex();
        resetLastIndex();
        updateFirstIndex(true);
        updateLastIndex(false);
    }

    protected void reshaped(Rectangle oldBounds, Rectangle newBounds) {
        if (!fitsWidth() && oldBounds.width != newBounds.width) {
            if (oldBounds.width < newBounds.width) {
                updateFirstIndex(false);
                updateLastIndex(true);
            } else {
                updateFirstIndex(true);
                updateLastIndex(false);
            }
        }

        super.reshaped(oldBounds, newBounds);
    }


//    private int[] getVisibleBounds() {
//        return new int[] { firstVisibleIndex, lastVisibleIndex };
//    }
    
    private int[] getVisibleBounds(Rectangle viewRect) {
        return new int[] { firstVisibleIndex, lastVisibleIndex };
    }


    public int getNearestTimestampIndex(int x, int y) {
        int timestampsCount = timeline.getTimestampsCount();

        if (timestampsCount == 0) return -1;
        if (timestampsCount == 1) return firstVisibleIndex;

        long dataX = (long)getDataX(x);

        int nearestIndex = firstVisibleIndex;
        long itemDataX = timeline.getTimestamp(nearestIndex);
        long nearestDistance = Math.abs(dataX - itemDataX);

        while(nearestIndex + 1 <= lastVisibleIndex) {
            itemDataX = timeline.getTimestamp(nearestIndex + 1);
            long distance = Math.abs(dataX - itemDataX);

            if (distance >= nearestDistance) break;

            nearestIndex++;
            nearestDistance = distance;
        }

        return nearestIndex;
    }


    // --- Actions support -----------------------------------------------------

    private class ZoomInAction extends AbstractAction {

        private static final int ONE_SECOND_WIDTH_THRESHOLD = 200;

        public ZoomInAction() {
            super();

            putValue(SHORT_DESCRIPTION, "Zoom In");
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
            setEnabled(timeline.getTimestampsCount() > 1 && !fitsWidth() &&
                       getViewWidth(1000) < ONE_SECOND_WIDTH_THRESHOLD);
        }

    }

    private class ZoomOutAction extends AbstractAction {

        private static final float USED_CHART_WIDTH_THRESHOLD = 0.33f;

        public ZoomOutAction() {
            super();

            putValue(SHORT_DESCRIPTION, "Zoom Out");
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
            String name = fitsWidth ? "Fixed Scale" : "Scale To Fit";
            putValue(SHORT_DESCRIPTION, name);
            putValue(SMALL_ICON, icon);
        }

    }


    // --- ChartConfigurationListener implementation ---------------------------

    private class VisibleBoundsListener implements ChartConfigurationListener {
        public void offsetChanged(long oldOffsetX, long oldOffsetY,
                                  long newOffsetX, long newOffsetY) {

            if (oldOffsetX != newOffsetX) {
                boolean searchToRight = newOffsetX > oldOffsetX;
                updateFirstIndex(searchToRight);
                updateLastIndex(searchToRight);
            }

        }

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();

            if (getContentsWidth() <= getWidth()) {
                resetFirstIndex();
                resetLastIndex();
            }

        }

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();

            if (!fitsWidth() && oldScaleX != newScaleX) {
                if (oldScaleX < newScaleX) {
                    updateFirstIndex(true);
                    updateLastIndex(false);
                } else {
                    updateFirstIndex(false);
                    updateLastIndex(true);
                }
            } else {
                resetFirstIndex();
                resetLastIndex();
            }

        }

        public void viewChanged(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY,
                                int shiftX, int shiftY) {}
    }


    // --- ChartContext implementation -----------------------------------------

    public static final class Context extends ChartComponent.DefaultContext {

        public Context(ProfilerXYChart chart) {
            super(chart);
        }

        protected ProfilerXYChart getChartComponent() {
            return (ProfilerXYChart)super.getChartComponent();
        }


//        public int[] getVisibleBounds() {
//            return getChartComponent().getVisibleBounds();
//        }

        public int[] getVisibleBounds(Rectangle viewRect) {
            return getChartComponent().getVisibleBounds(viewRect);
        }

        public int getNearestTimestampIndex(int x, int y) {
            return getChartComponent().getNearestTimestampIndex(x, y);
        }

    }

}

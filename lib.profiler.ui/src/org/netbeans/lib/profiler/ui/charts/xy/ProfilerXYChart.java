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
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.PaintersModel;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYChart extends ChartComponent {

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

    private static final int[] VISIBLE_NONE = new int[] { -1, -1 };

    private final Timeline timeline;

    private ZoomInAction zoomInAction;
    private ZoomOutAction zoomOutAction;
    private ToggleViewAction toggleViewAction;

    private int firstVisibleIndex[];
    private int lastVisibleIndex[];
    private Map<Rectangle, int[][]> indexesCache;

    private boolean visibleIndexesDirty;
    private boolean contentsWidthChanged;
    private int oldBoundsWidth, newBoundsWidth;
    private long oldOffsetX, newOffsetX;
    private double oldScaleX, newScaleX;


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

        indexesCache = new HashMap();

        firstVisibleIndex = VISIBLE_NONE;
        lastVisibleIndex  = VISIBLE_NONE;
        visibleIndexesDirty = true;

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

    // startIndex: any visible index
    private int[] findFirstVisibleL(int[] startIndex, int viewStart, int viewEnd) {
        int timestampsCount = timeline.getTimestampsCount();

        if (timestampsCount == 0 || startIndex == VISIBLE_NONE) return VISIBLE_NONE;

        double dataStart = getDataX(viewStart);

        int index = startIndex[0];
        if (index == -1) {
            index = startIndex[1];
            if (timeline.getTimestamp(index) < dataStart)
                return findFirstVisibleR(startIndex, viewStart, viewEnd);
        }

        while (index > 0) {
            long data = timeline.getTimestamp(index - 1);
            if (data < dataStart) return new int[] { index, -1 };
            index--;
        }

        return timeline.getTimestamp(index) >= dataStart ? new int[] { index, -1 } :
                                                           VISIBLE_NONE;
    }

    // startIndex: any invisible or last visible index
    private int[] findLastVisibleL(int[] startIndex, int viewStart, int viewEnd) {
        int timestampsCount = timeline.getTimestampsCount();

        if (timestampsCount == 0 || startIndex == VISIBLE_NONE) return VISIBLE_NONE;

        int index = startIndex[0];
        if (index == -1) index = startIndex[1];

        double dataStart = getDataX(viewStart);
        double dataEnd = getDataX(viewEnd);

        while (index >= 0) {
            long data = timeline.getTimestamp(index);
            if (data < dataStart) return new int[] { -1, index + 1 };
            if (data <= dataEnd) return new int[] { index, -1 };
            index--;
        }

        return VISIBLE_NONE;
    }

    // startIndex: any invisible or first visible index
    private int[] findFirstVisibleR(int[] startIndex, int viewStart, int viewEnd) {
        int timestampsCount = timeline.getTimestampsCount();

        if (timestampsCount == 0 || startIndex == VISIBLE_NONE) return VISIBLE_NONE;

        int index = startIndex[0];
        if (index == -1) index = startIndex[1];

        double dataStart = getDataX(viewStart);
        double dataEnd = getDataX(viewEnd);

        int maxIndex = timestampsCount - 1;
        while (index <= maxIndex) {
            long data = timeline.getTimestamp(index);
            if (data > dataEnd) return new int[] { -1, index - 1 };
            if (data >= dataStart) return new int[] { index, -1 };
            index++;
        }

        return VISIBLE_NONE;
    }

    // startIndex: any visible index
    private int[] findLastVisibleR(int[] startIndex, int viewStart, int viewEnd) {
        int timestampsCount = timeline.getTimestampsCount();

        if (timestampsCount == 0 || startIndex == VISIBLE_NONE) return VISIBLE_NONE;

        double dataEnd = getDataX(viewEnd);

        int index = startIndex[0];
        if (index == -1) {
            index = startIndex[1];
            if (timeline.getTimestamp(index) > dataEnd)
                return findLastVisibleL(startIndex, viewStart, viewEnd);
        }

        int maxIndex = timestampsCount - 1;
        while (index < maxIndex) {
            long data = timeline.getTimestamp(index + 1);
            if (data > dataEnd) return new int[] { index, -1 };
            index++;
        }

        return timeline.getTimestamp(index) <= dataEnd ? new int[] { index, -1 } :
                                                         VISIBLE_NONE;
    }

    // Use in case of absolute panic, will always work
    // Note: doesn't clear cache, indexesCache.clear() must be invoked explicitely
    private void recomputeVisibleBounds() {
        int timestampsCount = timeline.getTimestampsCount();
        if (timestampsCount == 0) {
            firstVisibleIndex = VISIBLE_NONE;
            lastVisibleIndex  = VISIBLE_NONE;
        } else {
            firstVisibleIndex = new int[] { 0, -1 };
            lastVisibleIndex  = new int[] { timestampsCount - 1, -1 };
            if (!fitsWidth()) {
                firstVisibleIndex = findFirstVisibleR(firstVisibleIndex, 0, getWidth());
                lastVisibleIndex  = findLastVisibleL(lastVisibleIndex, 0, getWidth());
            }
        }
    }

    protected void reshaped(Rectangle oldBounds, Rectangle newBounds) {
        if (!fitsWidth() && oldBounds.width != newBounds.width) {
            visibleIndexesDirty = true;
            oldBoundsWidth = oldBounds.width;
            newBoundsWidth = newBounds.width;
        }

        super.reshaped(oldBounds, newBounds);
    }


    private void updateVisibleIndexes() {
        if (!visibleIndexesDirty) return;

        indexesCache.clear();

        if (fitsWidth()) {
            recomputeVisibleBounds();
        } else if (contentsWidthChanged) {
            recomputeVisibleBounds();
        } else if (firstVisibleIndex == VISIBLE_NONE) {
            recomputeVisibleBounds();
        } else if (oldBoundsWidth != newBoundsWidth) {
            if (oldBoundsWidth < newBoundsWidth) {
                firstVisibleIndex = findFirstVisibleL(firstVisibleIndex, 0, getWidth());
                if (currentlyFollowingDataWidth()) {
                    lastVisibleIndex[0] = timeline.getTimestampsCount() - 1;
                    lastVisibleIndex[1] = -1;
                } else {
                    lastVisibleIndex = findLastVisibleR(firstVisibleIndex, 0, getWidth());
                }
            } else {
                firstVisibleIndex = findFirstVisibleR(firstVisibleIndex, 0, getWidth());
                if (currentlyFollowingDataWidth()) {
                    lastVisibleIndex[0] = timeline.getTimestampsCount() - 1;
                    lastVisibleIndex[1] = -1;
                } else {
                    lastVisibleIndex = findLastVisibleL(lastVisibleIndex, 0, getWidth());
                }
            }
        } else if (oldScaleX != newScaleX) {
            if (oldScaleX < newScaleX) {
                firstVisibleIndex = findFirstVisibleR(firstVisibleIndex, 0, getWidth());
                lastVisibleIndex = findLastVisibleL(lastVisibleIndex, 0, getWidth());
            } else {
                firstVisibleIndex = findFirstVisibleL(firstVisibleIndex, 0, getWidth());
                lastVisibleIndex = findLastVisibleR(lastVisibleIndex, 0, getWidth());
            }
        } else if (oldOffsetX != newOffsetX) {
            if (newOffsetX > oldOffsetX) {
                firstVisibleIndex = findFirstVisibleR(firstVisibleIndex, 0, getWidth());
                lastVisibleIndex = findLastVisibleR(lastVisibleIndex, 0, getWidth());
            } else {
                firstVisibleIndex = findFirstVisibleL(firstVisibleIndex, 0, getWidth());
                lastVisibleIndex = findLastVisibleL(lastVisibleIndex, 0, getWidth());
            }
        }

        // clear dirty flags
        contentsWidthChanged = false;
        oldBoundsWidth = newBoundsWidth;
        oldScaleX = newScaleX;
        oldOffsetX = newOffsetX;
        visibleIndexesDirty = false;
    }
    
    private int[][] getVisibleBounds(Rectangle viewRect) {
        updateVisibleIndexes();

        if (fitsWidth() || viewRect.x == 0 && viewRect.width == getWidth())
            return new int[][] { firstVisibleIndex, lastVisibleIndex };

        Rectangle rect = new Rectangle(viewRect.x, 0, viewRect.width, 0);
        int[][] bounds = indexesCache.get(rect);

        if (bounds == null) {
            // TODO: OPTIMIZE - DETERMINE OPTIMAL R/L DIRECTION !!!
            int[] firstIndex = findFirstVisibleR(firstVisibleIndex, viewRect.x,
                                                 viewRect.x + viewRect.width);
            int[] lastIndex = findLastVisibleL(lastVisibleIndex, viewRect.x,
                                               viewRect.x + viewRect.width);
            bounds = new int[][] { firstIndex, lastIndex };
            indexesCache.put(rect, bounds);
        }

        return bounds;
    }


    public int getNearestTimestampIndex(int x, int y) {
        int timestampsCount = timeline.getTimestampsCount();

        if (timestampsCount == 0) return -1;
        if (timestampsCount == 1) return 0;

        long dataX = (long)getDataX(x);

        if (firstVisibleIndex == VISIBLE_NONE) return -1;
        int nearestIndex = firstVisibleIndex[0];
        if (nearestIndex == -1) nearestIndex = firstVisibleIndex[1];
        
        long itemDataX = timeline.getTimestamp(nearestIndex);
        long nearestDistance = Math.abs(dataX - itemDataX);

        int lastIndex = lastVisibleIndex[0];
        if (lastIndex == -1) lastIndex = lastVisibleIndex[1];
        while(nearestIndex + 1 <= lastIndex) {
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
        public void offsetChanged(long oldOffsetX, long oldOffsetY,
                                  long newOffsetX, long newOffsetY) {
            if (!fitsWidth() && oldOffsetX != newOffsetX) {
                visibleIndexesDirty = true;
                ProfilerXYChart.this.oldOffsetX = oldOffsetX;
                ProfilerXYChart.this.newOffsetX = newOffsetX;
            }
        }

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();

            if (getContentsWidth() <= getWidth()) {
                visibleIndexesDirty = true;
                contentsWidthChanged = true;
            }
        }

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();

            visibleIndexesDirty = true;
            if (!fitsWidth() && oldScaleX != newScaleX) {
                ProfilerXYChart.this.oldScaleX = oldScaleX;
                ProfilerXYChart.this.newScaleX = newScaleX;
            }
        }
    }


    // --- ChartContext implementation -----------------------------------------

    public static final class Context extends ChartComponent.DefaultContext {

        Context(ProfilerXYChart chart) {
            super(chart);
        }

        protected ProfilerXYChart getChartComponent() {
            return (ProfilerXYChart)super.getChartComponent();
        }


        public int[][] getVisibleBounds(Rectangle viewRect) {
            return getChartComponent().getVisibleBounds(viewRect);
        }

        public int getNearestTimestampIndex(int x, int y) {
            return getChartComponent().getNearestTimestampIndex(x, y);
        }

    }

}

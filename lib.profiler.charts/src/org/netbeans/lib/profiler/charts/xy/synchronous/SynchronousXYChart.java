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

package org.netbeans.lib.profiler.charts.xy.synchronous;

import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.PaintersModel;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
public class SynchronousXYChart extends ChartComponent {

    private static final int[] VISIBLE_NONE = new int[] { -1, -1 };

    private final Timeline timeline;

    private int firstVisibleIndex[];
    private int lastVisibleIndex[];
    private Map<Rectangle, int[][]> indexesCache;

    private boolean visibleIndexesDirty;
    private boolean contentsWidthChanged;
    private int oldBoundsWidth, newBoundsWidth;
    private long oldOffsetX, newOffsetX;
    private double oldScaleX, newScaleX;


    // --- Constructors --------------------------------------------------------

    private SynchronousXYChart() {
        throw new UnsupportedOperationException(
                "new SynchronousXYChart() not supported"); // NOI18N
    }

    public SynchronousXYChart(final SynchronousXYItemsModel itemsModel,
                              final PaintersModel paintersModel) {
        super();

        timeline = itemsModel.getTimeline();

        indexesCache = new HashMap();

        firstVisibleIndex = VISIBLE_NONE;
        lastVisibleIndex  = VISIBLE_NONE;
        visibleIndexesDirty = true;

        setItemsModel(itemsModel);
        setPaintersModel(paintersModel);

        addConfigurationListener(new VisibleBoundsListener());
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

        if (timeline.getTimestamp(index) < dataStart)
            return findLastVisibleR(startIndex, viewStart, viewEnd);

        while (index >= 0) {
            long data = timeline.getTimestamp(index);
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

        if (timeline.getTimestamp(index) > dataEnd)
            return findLastVisibleL(startIndex, viewStart, viewEnd);

        int maxIndex = timestampsCount - 1;
        while (index <= maxIndex) {
            long data = timeline.getTimestamp(index);
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


    // --- ChartConfigurationListener implementation ---------------------------

    private class VisibleBoundsListener extends ChartConfigurationListener.Adapter {
        public void offsetChanged(long oldOffsetX, long oldOffsetY,
                                  long newOffsetX, long newOffsetY) {
            if (!fitsWidth() && oldOffsetX != newOffsetX) {
                visibleIndexesDirty = true;
                SynchronousXYChart.this.oldOffsetX = oldOffsetX;
                SynchronousXYChart.this.newOffsetX = newOffsetX;
            }
        }

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {

            if (getContentsWidth() <= getWidth()) {
                visibleIndexesDirty = true;
                contentsWidthChanged = true;
            }
        }

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {

            visibleIndexesDirty = true;
            if (!fitsWidth() && oldScaleX != newScaleX) {
                SynchronousXYChart.this.oldScaleX = oldScaleX;
                SynchronousXYChart.this.newScaleX = newScaleX;
            }
        }
    }


    // --- ChartContext implementation -----------------------------------------

    protected static class Context extends ChartComponent.Context
                                   implements SynchronousXYChartContext {

        protected Context(SynchronousXYChart chart) {
            super(chart);
        }

        protected SynchronousXYChart getChartComponent() {
            return (SynchronousXYChart)super.getChartComponent();
        }


        public int[][] getVisibleBounds(Rectangle viewRect) {
            return getChartComponent().getVisibleBounds(viewRect);
        }

        public int getNearestTimestampIndex(int x, int y) {
            return getChartComponent().getNearestTimestampIndex(x, y);
        }

    }

}

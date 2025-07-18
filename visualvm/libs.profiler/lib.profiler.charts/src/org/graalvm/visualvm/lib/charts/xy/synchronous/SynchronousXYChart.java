/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.charts.xy.synchronous;

import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.ChartComponent;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.PaintersModel;
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

    private int firstVisibleIndex[]; // first item visible, second item invisible
    private int lastVisibleIndex[]; // first item visible, second item invisible
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

        indexesCache = new HashMap<>();

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

    // startIndex: first visible or invisible
    private int[] findFirstVisibleL(int[] startIndex, int viewStart, int viewEnd) {
        if (startIndex == VISIBLE_NONE) return VISIBLE_NONE;
        
        if (timeline.getTimestampsCount() == 0) return VISIBLE_NONE;
        
        int index = startIndex[0];
        if (index == -1) index = startIndex[1];
        
        double dataStart = getDataX(viewStart);
        
        while (index > 0 && timeline.getTimestamp(index - 1) >= dataStart) index--;
        
        long timestamp = timeline.getTimestamp(index);
        
        if (timestamp > getDataX(viewEnd)) {
            if (index == 0) {
                return VISIBLE_NONE;
            } else {
                return new int[] { -1, index - 1 };
            }
        } else {
            if (timestamp >= dataStart) {
                return new int[] { index, -1 };
            } else {
                return new int[] { -1, index };
            }
        }
    }

    // startIndex: last visible or invisible
    private int[] findLastVisibleL(int[] startIndex, int viewStart, int viewEnd) {
        if (startIndex == VISIBLE_NONE) return VISIBLE_NONE;
        
        if (timeline.getTimestampsCount() == 0) return VISIBLE_NONE;

        int index = startIndex[0];
        if (index == -1) index = startIndex[1];
        
        double dataEnd = getDataX(viewEnd);
        
        while (index > 0 && timeline.getTimestamp(index - 1) > dataEnd) index--;
        
        long timestamp = timeline.getTimestamp(index);
        
        if (timestamp > dataEnd) {
            if (index == 0 || timeline.getTimestamp(index - 1) < getDataX(viewStart)) {
                return new int[] { -1, index };
            } else {
                return new int[] { index - 1, -1 };
            }
        } else {
            if (timestamp >= getDataX(viewStart)) {
                return new int[] { index, -1 };
            } else {
                return VISIBLE_NONE;
            }
        }
    }

    // startIndex: first visible or invisible
    private int[] findFirstVisibleR(int[] startIndex, int viewStart, int viewEnd) {
        if (startIndex == VISIBLE_NONE) return VISIBLE_NONE;
        
        int timestampsCount = timeline.getTimestampsCount();
        if (timestampsCount == 0) return VISIBLE_NONE;
        
        int index = startIndex[0];
        if (index == -1) index = startIndex[1];
        
        double dataStart = getDataX(viewStart);
        
        while (index < timestampsCount - 1 && timeline.getTimestamp(index + 1) < dataStart) index++;
        long timestamp = timeline.getTimestamp(index);
        
        if (timestamp >= dataStart) {
            if (timestamp > getDataX(viewEnd)) {
                return VISIBLE_NONE;
            } else {
                return new int[] { index, -1 };
            }
        } else {
            if (index == timestampsCount - 1 || timeline.getTimestamp(index + 1) > getDataX(viewEnd)) {
                return new int[] { -1, index };
            } else {
                return new int[] { index + 1, -1 };
            }
        }
    }

    // startIndex: last visible or invisible
    private int[] findLastVisibleR(int[] startIndex, int viewStart, int viewEnd) {
        if (startIndex == VISIBLE_NONE) return VISIBLE_NONE;
        
        int timestampsCount = timeline.getTimestampsCount();
        if (timestampsCount == 0) return VISIBLE_NONE;
        
        int index = startIndex[0];
        if (index == -1) index = startIndex[1];
        
        double dataEnd = getDataX(viewEnd);
        
        while (index < timestampsCount - 1 && timeline.getTimestamp(index + 1) <= dataEnd) index++;
        
        long timestamp = timeline.getTimestamp(index);
        
        if (timestamp < getDataX(viewStart)) {
            if (index == timestampsCount - 1) {
                return VISIBLE_NONE;
            } else {
                return new int[] { -1, index + 1 };
            }
        } else {
            if (timestamp > dataEnd) {
                return new int[] { -1, index };
            } else {
                return new int[] { index, -1 };
            }
        }
    }

    // Use in case of absolute panic, will always work
    // Note: doesn't clear cache, indexesCache.clear() must be invoked explicitly
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
                    lastVisibleIndex = findLastVisibleR(lastVisibleIndex, 0, getWidth());
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
                int[] firstVisibleI = findFirstVisibleR(firstVisibleIndex, 0, getWidth());
                if (firstVisibleI == VISIBLE_NONE) firstVisibleI = findFirstVisibleL(firstVisibleIndex, 0, getWidth());
                firstVisibleIndex = firstVisibleI;
                int[] lastVisibleI = findLastVisibleL(lastVisibleIndex, 0, getWidth());
                if (lastVisibleI == VISIBLE_NONE) lastVisibleI = findLastVisibleR(lastVisibleIndex, 0, getWidth());
                lastVisibleIndex = lastVisibleI;
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
            int firstI = firstVisibleIndex[0];
            if (firstI == -1) firstI = firstVisibleIndex[1];
            int[] firstIndex = firstI == -1 ? VISIBLE_NONE : getDataX(viewRect.x) > timeline.getTimestamp(firstI) ?
                    findFirstVisibleR(firstVisibleIndex, viewRect.x, viewRect.x + viewRect.width) :
                    findFirstVisibleL(firstVisibleIndex, viewRect.x, viewRect.x + viewRect.width);

            int lastI = lastVisibleIndex[0];
            if (lastI == -1) lastI = lastVisibleIndex[1];
            int[] lastIndex = lastI == -1 ? VISIBLE_NONE : getDataX(viewRect.x + viewRect.width) < timeline.getTimestamp(lastI) ?
                    findLastVisibleL(lastVisibleIndex, viewRect.x, viewRect.x + viewRect.width) :
                    findLastVisibleR(lastVisibleIndex, viewRect.x, viewRect.x + viewRect.width);
            
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
        else if (currentlyFollowingDataWidth()) lastIndex = timestampsCount - 1;
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

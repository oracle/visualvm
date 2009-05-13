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

import org.netbeans.lib.profiler.charts.ChartItemListener;
import org.netbeans.lib.profiler.charts.LongRect;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;
import org.netbeans.lib.profiler.charts.Timeline;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerXYItem implements XYItem {

    private final String name;

    private int itemIndex;
    private Timeline timeline;

    private int lastIndex;
    
    private final LongRect bounds;
    private long initialMinY;
    private long initialMaxY;

    private LongRect initialBounds;

    private long minY;
    private long maxY;


    // --- Constructor ---------------------------------------------------------

    public ProfilerXYItem(String name) {
        this(name, Long.MAX_VALUE);
    }

    public ProfilerXYItem(String name, long initialMinY) {
        this(name, initialMinY, Long.MIN_VALUE);
    }

    public ProfilerXYItem(String name, long initialMinY, long initialMaxY) {
        this.name = name;
        this.initialMinY = initialMinY;
        this.initialMaxY = initialMaxY;
        minY = Long.MAX_VALUE;
        maxY = Long.MIN_VALUE;
        bounds = new LongRect();
        initialBounds = new LongRect();
        lastIndex = -1;
    }


    // --- Item telemetry ------------------------------------------------------

    public String getName() { return name; }

    public void setInitialBounds(LongRect initialBounds) { this.initialBounds = initialBounds; }

    public LongRect getInitialBounds() { return initialBounds; }

    public XYItemChange valuesAdded() {

        int index = timeline.getTimestampsCount() - 1;
        XYItemChange change = null;

        if (lastIndex == index) { // No change

            LongRect b = new LongRect(bounds);
            change = new XYItemChange.Default(this, new int[] { -1 }, b, b, b);

        } else if (index > -1) { // New item(s)

            // Save oldBounds, setup dirtyBounds
            LongRect oldBounds = new LongRect(bounds);
            LongRect dirtyBounds = new LongRect();

            boolean initBounds = lastIndex == -1;
            int dirtyIndex = lastIndex == -1 ? 0 : lastIndex;

            // Process other values
            for (int i = dirtyIndex; i <= index; i++) {

                long timestamp = timeline.getTimestamp(i);
                long value = getYValue(i);

                // Update item minY/maxY
                minY = Math.min(value, minY);
                maxY = Math.max(value, maxY);

                // Process item bounds
                if (initBounds) {
                    // Initialize item bounds
                    bounds.x = timestamp;
                    bounds.y = Math.min(value, initialMinY);
                    bounds.width = 0;
                    bounds.height = Math.max(value, initialMaxY) - bounds.y;
                    initBounds = false;
                } else {
                    // Update item bounds
                    LongRect.add(bounds, timestamp, value);
                }

                // Process dirty bounds
                if (i == dirtyIndex) {
                    // Setup dirty bounds
                    dirtyBounds.x = timestamp;
                    dirtyBounds.y = value;
                    dirtyBounds.width = timeline.getTimestamp(index) - dirtyBounds.x;
                } else {
                    // Update dirty y/height
                    long dirtyY = dirtyBounds.y;
                    dirtyBounds.y = Math.min(dirtyY, value);
                    dirtyBounds.height = Math.max(dirtyY, value) - dirtyBounds.y;
                }

            }

            // Return ItemChange
            int indexesCount = index - lastIndex;
            int[] indexes = new int[indexesCount];
            for (int i = 0; i < indexesCount; i++) indexes[i] = lastIndex + 1 + i;
            change = new XYItemChange.Default(this, indexes, oldBounds,
                                              new LongRect(bounds), dirtyBounds);

        } else { // Reset

            minY = Long.MAX_VALUE;
            maxY = Long.MIN_VALUE;

            // Save oldBounds
            LongRect oldBounds = new LongRect(bounds);
            LongRect.set(bounds, 0, 0, 0, 0);

            // Return ItemChange
            change = new XYItemChange.Default(this, new int[] { -1 }, oldBounds,
                                            new LongRect(bounds), oldBounds);

        }

        lastIndex = index;
        return change;
        
    }

    public int getValuesCount() { return timeline.getTimestampsCount(); }

    public long getXValue(int index) { return timeline.getTimestamp(index); }

    public abstract long getYValue(int index);

    public long getMinYValue() { return minY; }

    public long getMaxYValue() { return maxY; }
    
    public LongRect getBounds() {
        if (getValuesCount() == 0) return initialBounds;
        else return bounds;
    }


    // --- ChartItem implementation (ChartItemListener not supported) ----------

    public void addItemListener(ChartItemListener listener) {}

    public void removeItemListener(ChartItemListener listener) {}


    // --- Internal interface --------------------------------------------------

    void setTimeline(Timeline timeline) { this.timeline = timeline; }

    void setItemIndex(int itemIndex) { this.itemIndex = itemIndex; }

    int getItemIndex() { return itemIndex; }

}

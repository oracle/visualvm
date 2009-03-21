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
import org.netbeans.lib.profiler.charts.xy.XYTimeline;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerXYItem implements XYItem {

    private final String name;

    private int itemIndex;
    private XYTimeline timeline;
    
    private final LongRect bounds;
    private long initialMinY;
    private long initialMaxY;


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
        bounds = new LongRect();
    }


    // --- Item telemetry ------------------------------------------------------

    public String getName() { return name; }

    public XYItemChange getChange() {
        
        int valuesCount = timeline.getTimestampsCount();

        if (valuesCount > 0) {

            // Resolve current timestamp
            int lastIndex = valuesCount - 1;
            long timestamp = timeline.getTimestamp(lastIndex);
            long value = getYValue(lastIndex);

            // Save oldBounds, setup dirtyBounds
            LongRect oldBounds = new LongRect(bounds);
            LongRect dirtyBounds = new LongRect();

            // Update bounds and dirtyBounds
            if (lastIndex == 0) {
                bounds.x = timestamp;
                bounds.y = Math.min(value, initialMinY);
                bounds.width = 0;
                bounds.height = Math.max(value, initialMaxY) - bounds.y;

                LongRect.set(dirtyBounds, timestamp, value, 0, 0);
            } else {
                LongRect.add(bounds, timestamp, value);
                long previousValue = getYValue(lastIndex - 1);
                dirtyBounds.x = timeline.getTimestamp(timeline.getTimestampsCount() - 2);
                dirtyBounds.width = timestamp - dirtyBounds.x;
                dirtyBounds.y = Math.min(previousValue, value);
                dirtyBounds.height = Math.max(previousValue, value) - dirtyBounds.y;
            }

            // Return ItemChange
            return new XYItemChange.Default(this, new int[] { lastIndex }, oldBounds,
                                            new LongRect(bounds), dirtyBounds);

        } else {

            // Save oldBounds
            LongRect oldBounds = new LongRect(bounds);
            LongRect.set(bounds, 0, 0, 0, 0);

            // Return ItemChange
            return new XYItemChange.Default(this, new int[] { -1 }, oldBounds,
                                            new LongRect(bounds), oldBounds);

        }
    }

    public int getValuesCount() { return timeline.getTimestampsCount(); }

    public long getXValue(int index) { return timeline.getTimestamp(index); }

    public abstract long getYValue(int index);
    
    public LongRect getBounds() { return bounds; }


    // --- ChartItem implementation (ChartItemListener not supported) ----------

    public void addItemListener(ChartItemListener listener) {}

    public void removeItemListener(ChartItemListener listener) {}


    // --- Internal interface --------------------------------------------------

    void setTimeline(XYTimeline timeline) { this.timeline = timeline; }

    void setItemIndex(int itemIndex) { this.itemIndex = itemIndex; }

    int getItemIndex() { return itemIndex; }

}

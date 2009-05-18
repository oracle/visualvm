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
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemsModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.netbeans.lib.profiler.charts.ChartItem;

/**
 *
 * @author Jiri Sedlacek
 */
public class SynchronousXYItemsModel extends ItemsModel.Abstract {

    private final ArrayList<SynchronousXYItem> items = new ArrayList();
    private final Timeline timeline;


    // --- Constructor ---------------------------------------------------------

    public SynchronousXYItemsModel(Timeline timeline) {
        this.timeline = timeline;
    }

    public SynchronousXYItemsModel(Timeline timeline, SynchronousXYItem[] items) {
        this(timeline);

        if (items == null)
            throw new IllegalArgumentException("Items cannot be null"); // NOI18N
        if (items.length == 0)
            throw new IllegalArgumentException("Items cannot be empty"); // NOI18N

        addItems(items);
    }


    // --- Public interface ----------------------------------------------------

    public void addItems(SynchronousXYItem[] addedItems) {
        for (int i = 0; i < addedItems.length; i++) {
            addedItems[i].setTimeline(timeline);
            items.add(addedItems[i]);
        }
        
        fireItemsAdded(Arrays.asList((ChartItem[])addedItems));

        if (timeline.getTimestampsCount() > 0) valuesAdded();
    }

    public void removeItems(SynchronousXYItem[] removedItems) {
        for (SynchronousXYItem item : removedItems) items.remove(item);
        fireItemsRemoved(Arrays.asList((ChartItem[])removedItems));
    }


    public final void valuesAdded() {
        // Update values
        List<ChartItemChange> itemChanges = new ArrayList(items.size());
        for (SynchronousXYItem item : items) itemChanges.add(item.valuesChanged());
        fireItemsChanged(itemChanges);

        // Check timestamp
        int valueIndex = timeline.getTimestampsCount() - 1;
        long timestamp = timeline.getTimestamp(valueIndex);
        long previousTimestamp = valueIndex == 0 ? -1 :
                                 timeline.getTimestamp(valueIndex - 1);
        
        if (previousTimestamp != -1 && previousTimestamp >= timestamp)
            throw new IllegalArgumentException(
                           "ProfilerXYItemsModel: new timestamp " + timestamp + // NOI18N
                           " not greater than previous " + previousTimestamp + // NOI18N
                           ", skipping the values."); // NOI18N
    }

    public final void valuesReset() {
        // Update values
        List<ChartItemChange> itemChanges = new ArrayList(items.size());
        for (SynchronousXYItem item : items) itemChanges.add(item.valuesChanged());
        fireItemsChanged(itemChanges);
    }


    public final Timeline getTimeline() {
        return timeline;
    }


    // --- AbstractItemsModel implementation -----------------------------------

    public final int getItemsCount() { return items.size(); }

    public final SynchronousXYItem getItem(int index) { return items.get(index); }

}

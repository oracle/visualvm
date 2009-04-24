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

import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemsModel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYItemsModel extends ItemsModel.Abstract {

    private final ProfilerXYItem[] items;
    private final Timeline timeline;


    // --- Constructor ---------------------------------------------------------

    public ProfilerXYItemsModel(Timeline timeline, ProfilerXYItem[] items) {
        if (items == null)
            throw new IllegalArgumentException("Items cannot be null"); // NOI18N
        if (items.length == 0)
            throw new IllegalArgumentException("Items cannot be empty"); // NOI18N

        this.items = items;
        this.timeline = timeline;

        for (int i = 0; i < items.length; i++) {
            items[i].setItemIndex(i);
            items[i].setTimeline(timeline);
        }

        if (timeline.getTimestampsCount() > 0) valuesAdded();
    }


    // --- Public interface ----------------------------------------------------

    public void valuesAdded() {
        // Update values
        List<ChartItemChange> itemChanges = new ArrayList(items.length);
        for (ProfilerXYItem item : items) itemChanges.add(item.valuesAdded());
        fireItemsChanged(itemChanges);

        // Check timestamp
        int valueIndex = timeline.getTimestampsCount() - 1;
        long timestamp = timeline.getTimestamp(valueIndex);
        long previousTimestamp = valueIndex == 0 ? -1 :
                                 timeline.getTimestamp(valueIndex - 1);
//
        if (previousTimestamp != -1 && previousTimestamp >= timestamp)
            throw new IllegalArgumentException(
                           "ProfilerXYItemsModel: new timestamp " + timestamp + // NOI18N
                           " not greater than previous " + previousTimestamp + // NOI18N
                           ", skipping the values."); // NOI18N
    }

    public void valuesReset() {
        // Update values
        List<ChartItemChange> itemChanges = new ArrayList(items.length);
        for (ProfilerXYItem item : items) itemChanges.add(item.valuesAdded());
        fireItemsChanged(itemChanges);
    }


    public Timeline getTimeline() {
        return timeline;
    }


    // --- AbstractItemsModel implementation -----------------------------------

    public int getItemsCount() { return items.length; }

    public ProfilerXYItem getItem(int index) { return items[index]; }

}

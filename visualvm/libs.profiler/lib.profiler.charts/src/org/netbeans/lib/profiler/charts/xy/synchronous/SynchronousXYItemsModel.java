/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
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
// See #168544
//            throw new IllegalArgumentException(
//                           "ProfilerXYItemsModel: new timestamp " + timestamp + // NOI18N
//                           " not greater than previous " + previousTimestamp + // NOI18N
//                           ", skipping the values."); // NOI18N
            System.err.println("WARNING [" + SynchronousXYItemsModel.class.getName() + // NOI18N
                               "]: ProfilerXYItemsModel: new timestamp " + // NOI18N
                               timestamp + " not greater than previous " + // NOI18N
                               previousTimestamp + ", skipping the values."); // NOI18N
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

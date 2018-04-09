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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.ValueItemDescriptor;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.modules.profiler.snaptracer.ProbeItemDescriptor;

/**
 * All methods must be invoked from the EDT.
 *
 * @author Jiri Sedlacek
 */
final class TimelineModel implements Timeline {

    private static final int STORAGE_BUFFER_STEP = 50;

    private int bufferStep;

    private int valuesCount;
    private long[] timestamps;
    private long[][] values;

    private final List<TimelineXYItem> items = new ArrayList();


    TimelineModel() {
        this(STORAGE_BUFFER_STEP);
    }
    
    private TimelineModel(int bufferStep) {
        this.bufferStep = bufferStep;
        reset();
    }


    int getItemsCount() {
        return items.size();
    }


    TimelineXYItem[] createItems(ProbeItemDescriptor[] itemDescriptors) {
        int itemsCount = values == null ? 0 : values.length;
        int addedItemsCount = itemDescriptors.length;
        TimelineXYItem[] itemsArr = new TimelineXYItem[addedItemsCount];

        for (int i = 0; i < addedItemsCount; i++) {
            if (itemDescriptors[i] instanceof ValueItemDescriptor) {
                ValueItemDescriptor d = (ValueItemDescriptor)itemDescriptors[i];
                itemsArr[i] = new TimelineXYItem(d.getName(), d.getMinValue(),
                                                 d.getMaxValue(), itemsCount + i) {
                    public long getYValue(int valueIndex) {
                        return values[getIndex()][valueIndex];
                    }
                };
            } else {
                // Reserved for non-value items
            }
            items.add(itemsArr[i]);
        }

        addItemsImpl(addedItemsCount);

        return itemsArr;
    }

    void removeItems(SynchronousXYItem[] removed) {
        removeItemsImpl(removed.length);

        int firstRemovedIndex = ((TimelineXYItem)removed[0]).getIndex();
        for (SynchronousXYItem item : removed)
            items.remove(firstRemovedIndex);
        for (int i = firstRemovedIndex; i < items.size(); i++)
            items.get(i).setIndex(i);
    }


    void addValues(long timestamp, long[] newValues) {
        updateStorage();
        
        // Check last timestamp whether greater than the new one
        long lastTimestamp = valuesCount == 0 ? -1 : timestamps[valuesCount - 1];
        // Silently increase timestamp, JVM was busy - timer out of sync
        if (lastTimestamp >= timestamp) timestamp = lastTimestamp + 1;

        timestamps[valuesCount] = timestamp;
        for (int i = 0; i < values.length; i++)
            values[i][valuesCount] = newValues[i];

        valuesCount++;
    }
    

    void reset() {
        valuesCount = 0;

        timestamps = null;
        if (values != null) {
            if (values.length == 0) {
                values = null;
            } else {
                for (int i = 0; i < values.length; i++)
                    values[i] = new long[0];
            }
        }
    }


    private void addItemsImpl(int addedItemsCount) {
        int newItemsCount = (values == null ? 0 : values.length) + addedItemsCount;
        values = new long[newItemsCount][];
        reset();
    }

    private void removeItemsImpl(int removedItemsCount) {
        values = new long[values.length - removedItemsCount][];
        reset();
    }


    public int getTimestampsCount() {
        return valuesCount;
    }

    public long getTimestamp(int index) {
        return timestamps[index];
    }


    private void updateStorage() {
        if (timestamps == null) {
            timestamps = new long[bufferStep];
            for (int i = 0; i < values.length; i++)
                values[i] = new long[bufferStep];
        } else if (valuesCount == timestamps.length) {
            timestamps = extendArray(timestamps, bufferStep);
            for (int i = 0; i < values.length; i++)
                values[i] = extendArray(values[i], bufferStep);
        }
    }

    private static long[] extendArray(long[] array, int extraLength) {
        int originalLength = array.length;
        long[] newArray = new long[originalLength + extraLength];
        System.arraycopy(array, 0, newArray, 0, originalLength);
        return newArray;
    }

}

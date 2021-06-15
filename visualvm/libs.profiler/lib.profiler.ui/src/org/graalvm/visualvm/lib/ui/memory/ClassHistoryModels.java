/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.memory;

import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;
import org.graalvm.visualvm.lib.jfluid.results.DataManagerListener;
import org.graalvm.visualvm.lib.jfluid.results.memory.ClassHistoryDataManager;
import org.graalvm.visualvm.lib.ui.graphs.GraphsUI;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ClassHistoryModels {

    // --- Instance variables --------------------------------------------------

    private final ClassHistoryDataManager dataManager;

    private final Timeline timeline;
    private final SynchronousXYItemsModel allocationsItemsModel;
    private final SynchronousXYItemsModel livenessItemsModel;


    // --- Constructor ---------------------------------------------------------

    public ClassHistoryModels(ClassHistoryDataManager dataManager) {
        this.dataManager = dataManager;

        timeline = createTimeline();
        allocationsItemsModel = createAllocationsItemsModel(timeline);
        livenessItemsModel = createLivenessItemsModel(timeline);

        dataManager.addDataListener(new DataManagerListener() {
            public void dataChanged() { dataChangedImpl(); }
            public void dataReset() { dataResetImpl(); }
        });
    }


    // --- Public interface ----------------------------------------------------

    public ClassHistoryDataManager getDataManager() {
        return dataManager;
    }

    public SynchronousXYItemsModel allocationsItemsModel() {
        return allocationsItemsModel;
    }

    public SynchronousXYItemsModel livenessItemsModel() {
        return livenessItemsModel;
    }


    // --- DataManagerListener implementation ----------------------------------

    private void dataChangedImpl() {
        allocationsItemsModel.valuesAdded();
        livenessItemsModel.valuesAdded();
    }

    private void dataResetImpl() {
        allocationsItemsModel.valuesReset();
        livenessItemsModel.valuesReset();
    }


    // --- Private implementation ----------------------------------------------

    private Timeline createTimeline() {
        return new Timeline() {
            public int getTimestampsCount() { return dataManager.getItemCount(); }
            public long getTimestamp(int index) { return dataManager.timeStamps[index]; }
        };
    }

    private SynchronousXYItemsModel createAllocationsItemsModel(Timeline timeline) {
        // Objects Allocated
        SynchronousXYItem allocObjectsItem = new SynchronousXYItem(GraphsUI.A_ALLOC_OBJECTS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalAllocObjects[index];
            }
        };
        allocObjectsItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.A_ALLOC_OBJECTS_INITIAL_VALUE));

        // Bytes Allocated
        SynchronousXYItem allocBytesItem = new SynchronousXYItem(GraphsUI.A_ALLOC_BYTES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.totalAllocObjectsSize[index];
            }
        };
        allocBytesItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.A_ALLOC_BYTES_INITIAL_VALUE));

        // Model
        SynchronousXYItemsModel model = new SynchronousXYItemsModel(timeline,
                           new SynchronousXYItem[] { allocObjectsItem, allocBytesItem });

        return model;
    }

    private SynchronousXYItemsModel createLivenessItemsModel(Timeline timeline) {
        // Live Objects
        SynchronousXYItem liveObjectsItem = new SynchronousXYItem(GraphsUI.L_LIVE_OBJECTS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTrackedLiveObjects[index];
            }
        };
        liveObjectsItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.L_LIVE_OBJECTS_INITIAL_VALUE));

        // Live Bytes
        SynchronousXYItem liveBytesItem = new SynchronousXYItem(GraphsUI.L_LIVE_BYTES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.trackedLiveObjectsSize[index];
            }
        };
        liveBytesItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.L_LIVE_BYTES_INITIAL_VALUE));

        // Objects Allocated
        SynchronousXYItem allocObjectsItem = new SynchronousXYItem(GraphsUI.A_ALLOC_OBJECTS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalAllocObjects[index];
            }
        };
        allocObjectsItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.A_ALLOC_OBJECTS_INITIAL_VALUE));

        // Model
        SynchronousXYItemsModel model = new SynchronousXYItemsModel(timeline,
                 new SynchronousXYItem[] { liveObjectsItem,
                                        liveBytesItem,
                                        allocObjectsItem});

        return model;
    }

}

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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.memory.ClassHistoryDataManager;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItem;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemsModel;
import org.netbeans.lib.profiler.ui.graphs.GraphsUI;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ClassHistoryModels {

    // --- Instance variables --------------------------------------------------

    private final ClassHistoryDataManager dataManager;

    private final Timeline timeline;
    private final ProfilerXYItemsModel allocationsItemsModel;
    private final ProfilerXYItemsModel livenessItemsModel;


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

    public ProfilerXYItemsModel allocationsItemsModel() {
        return allocationsItemsModel;
    }

    public ProfilerXYItemsModel livenessItemsModel() {
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

    private ProfilerXYItemsModel createAllocationsItemsModel(Timeline timeline) {
        // Objects Allocated
        ProfilerXYItem allocObjectsItem = new ProfilerXYItem(GraphsUI.A_ALLOC_OBJECTS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalAllocObjects[index];
            }
        };

        // Bytes Allocated
        ProfilerXYItem allocBytesItem = new ProfilerXYItem(GraphsUI.A_ALLOC_BYTES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.totalAllocObjectsSize[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                           new ProfilerXYItem[] { allocObjectsItem, allocBytesItem });

        return model;
    }

    private ProfilerXYItemsModel createLivenessItemsModel(Timeline timeline) {
        // Live Objects
        ProfilerXYItem liveObjectsItem = new ProfilerXYItem(GraphsUI.L_LIVE_OBJECTS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTrackedLiveObjects[index];
            }
        };

        // Live Bytes
        ProfilerXYItem liveBytesItem = new ProfilerXYItem(GraphsUI.L_LIVE_BYTES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.trackedLiveObjectsSize[index];
            }
        };

        // Objects Allocated
        ProfilerXYItem allocObjectsItem = new ProfilerXYItem(GraphsUI.A_ALLOC_OBJECTS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalAllocObjects[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                 new ProfilerXYItem[] { liveObjectsItem,
                                        liveBytesItem,
                                        allocObjectsItem});

        return model;
    }

}

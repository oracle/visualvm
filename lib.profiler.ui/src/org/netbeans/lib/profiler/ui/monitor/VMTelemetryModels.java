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

package org.netbeans.lib.profiler.ui.monitor;

import org.netbeans.lib.profiler.charts.xy.XYTimeline;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerGCXYItem;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItem;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemsModel;
import org.netbeans.lib.profiler.ui.graphs.GraphsUI;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VMTelemetryModels {

    // --- Instance variables --------------------------------------------------

    private final VMTelemetryDataManager dataManager;

    private final XYTimeline timeline;
    private final ProfilerXYItemsModel memoryItemsModel;
    private final ProfilerXYItemsModel generationsItemsModel;
    private final ProfilerXYItemsModel threadsItemsModel;


    // --- Constructor ---------------------------------------------------------

    public VMTelemetryModels(VMTelemetryDataManager dataManager) {
        this.dataManager = dataManager;

        timeline = createTimeline();
        memoryItemsModel = createMemoryItemsModel(timeline);
        generationsItemsModel = createGenerationsItemsModel(timeline);
        threadsItemsModel = createThreadsItemsModel(timeline);

        dataManager.addDataListener(new DataManagerListener() {
            public void dataChanged() { dataChangedImpl(); }
            public void dataReset() { dataResetImpl(); }
        });
    }


    // --- Public interface ----------------------------------------------------

    public VMTelemetryDataManager getDataManager() {
        return dataManager;
    }

    public ProfilerXYItemsModel memoryItemsModel() {
        return memoryItemsModel;
    }

    public ProfilerXYItemsModel generationsItemsModel() {
        return generationsItemsModel;
    }

    public ProfilerXYItemsModel threadsItemsModel() {
        return threadsItemsModel;
    }


    // --- DataManagerListener implementation ----------------------------------

    private void dataChangedImpl() {
        memoryItemsModel.valuesAdded();
        generationsItemsModel.valuesAdded();
        threadsItemsModel.valuesAdded();
    }

    private void dataResetImpl() {
        memoryItemsModel.valuesReset();
        generationsItemsModel.valuesReset();
        threadsItemsModel.valuesReset();
    }


    // --- Private implementation ----------------------------------------------

    private XYTimeline createTimeline() {
        return new XYTimeline() {
            public int getTimestampsCount() { return dataManager.getItemCount(); }
            public long getTimestamp(int index) { return dataManager.timeStamps[index]; }
        };
    }

    private ProfilerXYItemsModel createMemoryItemsModel(XYTimeline timeline) {
        // Heap size
        ProfilerXYItem heapSizeItem = new ProfilerXYItem(GraphsUI.HEAP_SIZE_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.totalMemory[index];
            }
        };

        // Used heap
        ProfilerXYItem usedHeapItem = new ProfilerXYItem(GraphsUI.USED_HEAP_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.usedMemory[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                           new ProfilerXYItem[] { heapSizeItem, usedHeapItem });

        return model;
    }

    private ProfilerXYItemsModel createGenerationsItemsModel(XYTimeline timeline) {
        // Surviving generations
        ProfilerXYItem survivingGenerationsItem = new ProfilerXYItem(GraphsUI.SURVGEN_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nSurvivingGenerations[index];
            }
        };

        // Relative time spent in GC
        ProfilerXYItem gcTimeItem = new ProfilerXYItem(GraphsUI.GC_TIME_NAME, 0, 1000) {
            public long getYValue(int index) {
                return dataManager.relativeGCTimeInPerMil[index];
            }
        };

        // GC intervals
        ProfilerGCXYItem gcIntervalsItem = new ProfilerGCXYItem("") { // NOI18N

            public long[] getGCStarts(int index) {
                return dataManager.gcStarts[index];
            }

            public long[] getGCEnds(int index) {
                return dataManager.gcFinishs[index];
            }

        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                 new ProfilerXYItem[] { gcIntervalsItem,
                                        survivingGenerationsItem,
                                        gcTimeItem });

        return model;
    }

    private ProfilerXYItemsModel createThreadsItemsModel(XYTimeline timeline) {
        // Threads
        ProfilerXYItem threadsItem = new ProfilerXYItem(GraphsUI.THREADS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalThreads[index];
            }
        };

        // Loaded classes
        ProfilerXYItem loadedClassesItem = new ProfilerXYItem(GraphsUI.LOADED_CLASSES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.loadedClassesCount[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                       new ProfilerXYItem[] { threadsItem, loadedClassesItem });

        return model;
    }

}

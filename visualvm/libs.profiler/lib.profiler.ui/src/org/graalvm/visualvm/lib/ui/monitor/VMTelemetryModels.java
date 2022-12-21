/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.monitor;

import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;
import org.graalvm.visualvm.lib.jfluid.results.DataManagerListener;
import org.graalvm.visualvm.lib.jfluid.results.monitor.VMTelemetryDataManager;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerGCXYItem;
import org.graalvm.visualvm.lib.ui.graphs.GraphsUI;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VMTelemetryModels {

    // --- Instance variables --------------------------------------------------

    private final VMTelemetryDataManager dataManager;
    private final DataManagerListener listener;

    private final Timeline timeline;
    private final SynchronousXYItemsModel cpuItemsModel;
    private final SynchronousXYItemsModel memoryItemsModel;
    private final SynchronousXYItemsModel generationsItemsModel;
    private final SynchronousXYItemsModel threadsItemsModel;


    // --- Constructor ---------------------------------------------------------

    public VMTelemetryModels(VMTelemetryDataManager dataManager) {
        this.dataManager = dataManager;

        timeline = createTimeline();
        cpuItemsModel = createCPUItemsModel(timeline);
        memoryItemsModel = createMemoryItemsModel(timeline);
        generationsItemsModel = createGenerationsItemsModel(timeline);
        threadsItemsModel = createThreadsItemsModel(timeline);

        listener = new DataManagerListener() {
            public void dataChanged() { dataChangedImpl(); }
            public void dataReset() { dataResetImpl(); }
        };
        dataManager.addDataListener(listener);
    }


    // --- Public interface ----------------------------------------------------

    public VMTelemetryDataManager getDataManager() {
        return dataManager;
    }
    
    public SynchronousXYItemsModel cpuItemsModel() {
        return cpuItemsModel;
    }

    public SynchronousXYItemsModel memoryItemsModel() {
        return memoryItemsModel;
    }

    public SynchronousXYItemsModel generationsItemsModel() {
        return generationsItemsModel;
    }

    public SynchronousXYItemsModel threadsItemsModel() {
        return threadsItemsModel;
    }
    
    
    public void cleanup() {
        dataManager.removeDataListener(listener);
    }


    // --- DataManagerListener implementation ----------------------------------

    private void dataChangedImpl() {
        cpuItemsModel.valuesAdded();
        memoryItemsModel.valuesAdded();
        generationsItemsModel.valuesAdded();
        threadsItemsModel.valuesAdded();
    }

    private void dataResetImpl() {
        cpuItemsModel.valuesReset();
        memoryItemsModel.valuesReset();
        generationsItemsModel.valuesReset();
        threadsItemsModel.valuesReset();
    }


    // --- Private implementation ----------------------------------------------

    private Timeline createTimeline() {
        return new Timeline() {
            public int getTimestampsCount() { return dataManager.getItemCount(); }
            public long getTimestamp(int index) { return dataManager.timeStamps[index]; }
        };
    }
    
    private SynchronousXYItemsModel createCPUItemsModel(Timeline timeline) {
        // CPU
        SynchronousXYItem cpuTimeItem = new SynchronousXYItem("CPU Time", 0, 1000) {
            public long getYValue(int index) {
                return dataManager.processCPUTimeInPromile[index];
            }
        };
        cpuTimeItem.setInitialBounds(new LongRect(0, 0, 0, 1000));
        
        // Relative time spent in GC
        SynchronousXYItem gcTimeItem = new SynchronousXYItem(GraphsUI.GC_TIME_NAME, 0, 1000) {
            public long getYValue(int index) {
                return dataManager.relativeGCTimeInPerMil[index];
            }
        };
        gcTimeItem.setInitialBounds(new LongRect(0, 0, 0, 1000));

        // Model
        SynchronousXYItemsModel model = new SynchronousXYItemsModel(timeline,
                           new SynchronousXYItem[] { cpuTimeItem, gcTimeItem });

        return model;
    }

    private SynchronousXYItemsModel createMemoryItemsModel(Timeline timeline) {
        // Heap size
        SynchronousXYItem heapSizeItem = new SynchronousXYItem(GraphsUI.HEAP_SIZE_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.totalMemory[index];
            }
        };
        heapSizeItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.HEAP_SIZE_INITIAL_VALUE));

        // Used heap
        SynchronousXYItem usedHeapItem = new SynchronousXYItem(GraphsUI.USED_HEAP_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.usedMemory[index];
            }
        };
        usedHeapItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.USED_HEAP_INITIAL_VALUE));

        // Model
        SynchronousXYItemsModel model = new SynchronousXYItemsModel(timeline,
                           new SynchronousXYItem[] { heapSizeItem, usedHeapItem });

        return model;
    }

    private SynchronousXYItemsModel createGenerationsItemsModel(Timeline timeline) {
        // Surviving generations
        SynchronousXYItem survivingGenerationsItem = new SynchronousXYItem(GraphsUI.SURVGEN_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nSurvivingGenerations[index];
            }
        };
        survivingGenerationsItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.SURVGEN_INITIAL_VALUE));

//        // Relative time spent in GC
//        SynchronousXYItem gcTimeItem = new SynchronousXYItem(GraphsUI.GC_TIME_NAME, 0, 1000) {
//            public long getYValue(int index) {
//                return dataManager.relativeGCTimeInPerMil[index];
//            }
//        };
//        gcTimeItem.setInitialBounds(new LongRect(0, 0, 0, 1000));

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
        SynchronousXYItemsModel model = new SynchronousXYItemsModel(timeline,
                 new SynchronousXYItem[] { gcIntervalsItem,
                                        survivingGenerationsItem });

        return model;
    }

    private SynchronousXYItemsModel createThreadsItemsModel(Timeline timeline) {
        // Threads
        SynchronousXYItem threadsItem = new SynchronousXYItem(GraphsUI.THREADS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalThreads[index];
            }
        };
        threadsItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.THREADS_INITIAL_VALUE));

        // Loaded classes
        SynchronousXYItem loadedClassesItem = new SynchronousXYItem(GraphsUI.LOADED_CLASSES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.loadedClassesCount[index];
            }
        };
        loadedClassesItem.setInitialBounds(new LongRect(0, 0, 0, GraphsUI.LOADED_CLASSES_INITIAL_VALUE));

        // Model
        SynchronousXYItemsModel model = new SynchronousXYItemsModel(timeline,
                       new SynchronousXYItem[] { threadsItem, loadedClassesItem });

        return model;
    }

}

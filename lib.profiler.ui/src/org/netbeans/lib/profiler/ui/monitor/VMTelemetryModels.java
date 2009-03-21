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

import java.awt.Color;
import org.netbeans.lib.profiler.ui.charts.xy.CompoundProfilerXYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYTimeline;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItem;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemMarker;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemPainter;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemsModel;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYPaintersModel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VMTelemetryModels {

    // --- General colors definition -------------------------------------------

    private static final Color PROFILER_BLUE = new Color(127, 63, 191);
    private static final Color PROFILER_RED = new Color(255, 127, 127);


    // --- Items colors definition ---------------------------------------------

    public static final String HEAP_SIZE_NAME = "Heap Size";
    public static final float HEAP_SIZE_PAINTER_LINE_WIDTH = 0f;
    public static final Color HEAP_SIZE_PAINTER_LINE_COLOR = null;
    public static final Color HEAP_SIZE_PAINTER_FILL_COLOR = PROFILER_RED;
    public static final float HEAP_SIZE_MARKER_LINE_WIDTH = 4f;
    public static final Color HEAP_SIZE_MARKER_LINE_COLOR = Color.WHITE;
    public static final Color HEAP_SIZE_MARKER_FILL_COLOR = PROFILER_RED;

    public static final String USED_HEAP_NAME = "Used Heap";
    public static final float USED_HEAP_PAINTER_LINE_WIDTH = 0f;
    public static final Color USED_HEAP_PAINTER_LINE_COLOR = null;
    public static final Color USED_HEAP_PAINTER_FILL_COLOR = PROFILER_BLUE;
    public static final float USED_HEAP_MARKER_LINE_WIDTH = 4f;
    public static final Color USED_HEAP_MARKER_LINE_COLOR = Color.WHITE;
    public static final Color USED_HEAP_MARKER_FILL_COLOR = PROFILER_BLUE;

    public static final String SURVGEN_NAME = "Surviving Generations";
    public static final float SURVGEN_PAINTER_LINE_WIDTH = 3f;
    public static final Color SURVGEN_PAINTER_LINE_COLOR = PROFILER_RED;
    public static final Color SURVGEN_PAINTER_FILL_COLOR = null;
    public static final float SURVGEN_MARKER_LINE_WIDTH = 4f;
    public static final Color SURVGEN_MARKER_LINE_COLOR = Color.WHITE;
    public static final Color SURVGEN_MARKER_FILL_COLOR = PROFILER_RED;

    public static final String GC_TIME_NAME = "Relative Time Spent in GC";
    public static final float GC_TIME_PAINTER_LINE_WIDTH = 3f;
    public static final Color GC_TIME_PAINTER_LINE_COLOR = PROFILER_BLUE;
    public static final Color GC_TIME_PAINTER_FILL_COLOR = null;
    public static final float GC_TIME_MARKER_LINE_WIDTH = 4f;
    public static final Color GC_TIME_MARKER_LINE_COLOR = Color.WHITE;
    public static final Color GC_TIME_MARKER_FILL_COLOR = PROFILER_BLUE;

    public static final String THREADS_NAME = "Threads";
    public static final float THREADS_PAINTER_LINE_WIDTH = 3f;
    public static final Color THREADS_PAINTER_LINE_COLOR = PROFILER_RED;
    public static final Color THREADS_PAINTER_FILL_COLOR = null;
    public static final float THREADS_MARKER_LINE_WIDTH = 4f;
    public static final Color THREADS_MARKER_LINE_COLOR = Color.WHITE;
    public static final Color THREADS_MARKER_FILL_COLOR = PROFILER_RED;

    public static final String LOADED_CLASSES_NAME = "Loaded Classes";
    public static final float LOADED_CLASSES_PAINTER_LINE_WIDTH = 3f;
    public static final Color LOADED_CLASSES_PAINTER_LINE_COLOR = PROFILER_BLUE;
    public static final Color LOADED_CLASSES_PAINTER_FILL_COLOR = null;
    public static final float LOADED_CLASSES_MARKER_LINE_WIDTH = 4f;
    public static final Color LOADED_CLASSES_MARKER_LINE_COLOR = Color.WHITE;
    public static final Color LOADED_CLASSES_MARKER_FILL_COLOR = PROFILER_BLUE;


    // --- Instance variables --------------------------------------------------

    private final VMTelemetryDataManager dataManager;

    private int valuesCount;
    private int currentIndex;

    private final XYTimeline timeline;
    private final ProfilerXYItemsModel memoryItemsModel;
    private final ProfilerXYPaintersModel memoryPaintersModel;
    private final ProfilerXYItemsModel generationsItemsModel;
    private final ProfilerXYPaintersModel generationsPaintersModel;
    private final ProfilerXYItemsModel threadsItemsModel;
    private final ProfilerXYPaintersModel threadsPaintersModel;


    // --- Constructor ---------------------------------------------------------

    public VMTelemetryModels(VMTelemetryDataManager dataManager) {
        this.dataManager = dataManager;

        valuesCount = 0;
        currentIndex = -1;

        timeline = createTimeline();
        memoryItemsModel = createMemoryItemsModel(timeline);
        memoryPaintersModel = createMemoryPaintersModel();
        generationsItemsModel = createGenerationsItemsModel(timeline);
        generationsPaintersModel = createGenerationsPaintersModel();
        threadsItemsModel = createThreadsItemsModel(timeline);
        threadsPaintersModel = createThreadsPaintersModel();

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
    
    public ProfilerXYPaintersModel memoryPaintersModel() {
        return memoryPaintersModel;
    }

    public ProfilerXYItemsModel generationsItemsModel() {
        return generationsItemsModel;
    }
    
    public ProfilerXYPaintersModel generationsPaintersModel() {
        return generationsPaintersModel;
    }

    public ProfilerXYItemsModel threadsItemsModel() {
        return threadsItemsModel;
    }

    public ProfilerXYPaintersModel threadsPaintersModel() {
        return threadsPaintersModel;
    }


    // --- DataManagerListener implementation ----------------------------------

    private void dataChangedImpl() {
        valuesCount++;
        currentIndex++;

        memoryItemsModel.valueAdded();
        generationsItemsModel.valueAdded();
        threadsItemsModel.valueAdded();
    }

    private void dataResetImpl() {
        valuesCount = 0;
        currentIndex = -1;

        memoryItemsModel.valuesReset();
        generationsItemsModel.valuesReset();
        threadsItemsModel.valuesReset();
    }


    // --- Private implementation ----------------------------------------------

    private XYTimeline createTimeline() {
        return new XYTimeline() {
            public int getTimestampsCount() { return valuesCount; }
            public long getTimestamp(int index) { return dataManager.timeStamps[index]; }
        };
    }

    private ProfilerXYItemsModel createMemoryItemsModel(XYTimeline timeline) {
        // Heap size
        ProfilerXYItem heapSizeItem = new ProfilerXYItem(HEAP_SIZE_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.totalMemory[index];
            }
        };

        // Used heap
        ProfilerXYItem usedHeapItem = new ProfilerXYItem(USED_HEAP_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.usedMemory[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                           new ProfilerXYItem[] { heapSizeItem, usedHeapItem });

        return model;
    }

    private ProfilerXYPaintersModel createMemoryPaintersModel() {
        // Heap size
        ProfilerXYItemPainter heapSizePainter =
                ProfilerXYItemPainter.absolutePainter(HEAP_SIZE_PAINTER_LINE_WIDTH,
                                                      HEAP_SIZE_PAINTER_LINE_COLOR,
                                                      HEAP_SIZE_PAINTER_FILL_COLOR);
        ProfilerXYItemMarker heapSizeMarker =
                 ProfilerXYItemMarker.absolutePainter(HEAP_SIZE_MARKER_LINE_WIDTH,
                                                      HEAP_SIZE_MARKER_LINE_COLOR,
                                                      HEAP_SIZE_MARKER_FILL_COLOR);
        XYItemPainter hsp = new CompoundProfilerXYItemPainter(heapSizePainter,
                                                      heapSizeMarker);

        // Used heap
        ProfilerXYItemPainter usedHeapPainter =
                ProfilerXYItemPainter.absolutePainter(USED_HEAP_PAINTER_LINE_WIDTH,
                                                      USED_HEAP_PAINTER_LINE_COLOR,
                                                      USED_HEAP_PAINTER_FILL_COLOR);
        ProfilerXYItemMarker usedHeapMarker =
                 ProfilerXYItemMarker.absolutePainter(USED_HEAP_MARKER_LINE_WIDTH,
                                                      USED_HEAP_MARKER_LINE_COLOR,
                                                      USED_HEAP_MARKER_FILL_COLOR);
        XYItemPainter uhp = new CompoundProfilerXYItemPainter(usedHeapPainter,
                                                      usedHeapMarker);

        // Model
        ProfilerXYPaintersModel model = new ProfilerXYPaintersModel(
                                            new XYItemPainter[] { hsp, uhp });

        return model;
    }

    private ProfilerXYItemsModel createGenerationsItemsModel(XYTimeline timeline) {
        // Surviving generations
        ProfilerXYItem survivingGenerationsItem = new ProfilerXYItem(SURVGEN_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nSurvivingGenerations[index];
            }
        };

        // Relative time spent in GC
        ProfilerXYItem gcTimeItem = new ProfilerXYItem(GC_TIME_NAME, 0, 100) {
            public long getYValue(int index) {
                return dataManager.relativeGCTimeInPerMil[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                 new ProfilerXYItem[] { survivingGenerationsItem, gcTimeItem });

        return model;
    }

    private ProfilerXYPaintersModel createGenerationsPaintersModel() {
        // Surviving generations
        ProfilerXYItemPainter survgenPainter =
                ProfilerXYItemPainter.absolutePainter(SURVGEN_PAINTER_LINE_WIDTH,
                                                      SURVGEN_PAINTER_LINE_COLOR,
                                                      SURVGEN_PAINTER_FILL_COLOR);
        ProfilerXYItemMarker survgenMarker =
                 ProfilerXYItemMarker.absolutePainter(SURVGEN_MARKER_LINE_WIDTH,
                                                      SURVGEN_MARKER_LINE_COLOR,
                                                      SURVGEN_MARKER_FILL_COLOR);
        XYItemPainter sgp = new CompoundProfilerXYItemPainter(survgenPainter,
                                                      survgenMarker);

        // Relative time spent in GC
        ProfilerXYItemPainter gcTimePainter =
                ProfilerXYItemPainter.relativePainter(GC_TIME_PAINTER_LINE_WIDTH,
                                                      GC_TIME_PAINTER_LINE_COLOR,
                                                      GC_TIME_PAINTER_FILL_COLOR, 0);
        ProfilerXYItemMarker gcTimeMarker =
                 ProfilerXYItemMarker.relativePainter(GC_TIME_MARKER_LINE_WIDTH,
                                                      GC_TIME_MARKER_LINE_COLOR,
                                                      GC_TIME_MARKER_FILL_COLOR, 0);
        XYItemPainter gtp = new CompoundProfilerXYItemPainter(gcTimePainter,
                                                      gcTimeMarker);

        // Model
        ProfilerXYPaintersModel model = new ProfilerXYPaintersModel(
                 new XYItemPainter[] { sgp, gtp });

        return model;
    }

    private ProfilerXYItemsModel createThreadsItemsModel(XYTimeline timeline) {
        // Threads
        ProfilerXYItem threadsItem = new ProfilerXYItem(THREADS_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.nTotalThreads[index];
            }
        };

        // Loaded classes
        ProfilerXYItem loadedClassesItem = new ProfilerXYItem(LOADED_CLASSES_NAME, 0) {
            public long getYValue(int index) {
                return dataManager.loadedClassesCount[index];
            }
        };

        // Model
        ProfilerXYItemsModel model = new ProfilerXYItemsModel(timeline,
                       new ProfilerXYItem[] { threadsItem, loadedClassesItem });

        return model;
    }

    private ProfilerXYPaintersModel createThreadsPaintersModel() {
        // Threads
        ProfilerXYItemPainter threadsPainter =
                ProfilerXYItemPainter.absolutePainter(THREADS_PAINTER_LINE_WIDTH,
                                                      THREADS_PAINTER_LINE_COLOR,
                                                      THREADS_PAINTER_FILL_COLOR);
        ProfilerXYItemMarker threadsMarker =
                 ProfilerXYItemMarker.absolutePainter(THREADS_MARKER_LINE_WIDTH,
                                                      THREADS_MARKER_LINE_COLOR,
                                                      THREADS_MARKER_FILL_COLOR);
        XYItemPainter thp = new CompoundProfilerXYItemPainter(threadsPainter,
                                                      threadsMarker);

        // Loaded classes
        ProfilerXYItemPainter loadedClassesPainter =
                ProfilerXYItemPainter.relativePainter(LOADED_CLASSES_PAINTER_LINE_WIDTH,
                                                      LOADED_CLASSES_PAINTER_LINE_COLOR,
                                                      LOADED_CLASSES_PAINTER_FILL_COLOR,
                                                      0);
        ProfilerXYItemMarker loadedClassesMarker =
                 ProfilerXYItemMarker.relativePainter(LOADED_CLASSES_MARKER_LINE_WIDTH,
                                                      LOADED_CLASSES_MARKER_LINE_COLOR,
                                                      LOADED_CLASSES_MARKER_FILL_COLOR,
                                                      0);
        XYItemPainter lcp = new CompoundProfilerXYItemPainter(loadedClassesPainter,
                                                      loadedClassesMarker);

        // Model
        ProfilerXYPaintersModel model = new ProfilerXYPaintersModel(
           new XYItemPainter[] { thp, lcp });

        return model;
    }

}

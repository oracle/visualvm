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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.memory.ClassHistoryDataManager;
import org.netbeans.lib.profiler.ui.graphs.GraphsUI;

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

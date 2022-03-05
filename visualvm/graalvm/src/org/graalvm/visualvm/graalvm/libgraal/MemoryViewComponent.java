/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.libgraal;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
class MemoryViewComponent extends JPanel {

    private static final String UNKNOWN = NbBundle.getMessage(MemoryViewComponent.class, "LBL_Unknown"); // NOI18N

    private boolean liveModel;
    private boolean memoryMonitoringSupported;
    private String heapName;

    private SimpleXYChartSupport chartSupport;

    MemoryViewComponent(MemoryModel model) {
        initModels(model);
        initComponents();
        refresh(model);
    }

    DataViewComponent.DetailsView getDetailsView() {
        return new DataViewComponent.DetailsView(heapName, null, 10, this, null);
    }

    private void refresh(MemoryModel model) {
        if (memoryMonitoringSupported) {
            long heapCapacity = model.getHeapCapacity();
            long heapUsed = model.getHeapUsed();
            long maxHeap = model.getMaxHeap();

            if (liveModel) {
                chartSupport.addValues(model.getTimestamp(), new long[]{heapCapacity, heapUsed});
            }
            chartSupport.updateDetails(new String[]{chartSupport.formatBytes(heapCapacity),
                chartSupport.formatBytes(heapUsed),
                chartSupport.formatBytes(maxHeap)});
        }
    }

    private void initModels(final MemoryModel model) {
        liveModel = model.isLive();
        memoryMonitoringSupported = true;
        heapName = memoryMonitoringSupported ? model.getHeapName() : NbBundle.getMessage(MemoryViewComponent.class, "LBL_Memory"); // NOI18N

        if (memoryMonitoringSupported) {
            String HEAP_SIZE = NbBundle.getMessage(MemoryViewComponent.class, "LBL_Heap_size"); // NOI18N
            String HEAP_SIZE_LEG = NbBundle.getMessage(MemoryViewComponent.class, "LBL_Heap_size_leg", heapName); // NOI18N
            String USED_HEAP = NbBundle.getMessage(MemoryViewComponent.class, "LBL_Used_heap"); // NOI18N
            String USED_HEAP_LEG = NbBundle.getMessage(MemoryViewComponent.class, "LBL_Used_heap_leg", heapName.toLowerCase()); // NOI18N
            String MAX_HEAP = NbBundle.getMessage(MemoryViewComponent.class, "LBL_Max_Heap");   // NOI18N

            SimpleXYChartDescriptor chartDescriptor
                    = SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, model.getChartCache());

            chartDescriptor.addLineFillItems(HEAP_SIZE_LEG, USED_HEAP_LEG);
            chartDescriptor.setDetailsItems(new String[]{HEAP_SIZE, USED_HEAP, MAX_HEAP});
            chartDescriptor.setLimitYValue(model.getMaxHeap());

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
            model.registerHeapChartSupport(chartSupport);

            chartSupport.setZoomingEnabled(!liveModel);

            model.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    refresh(model);
                }
            });
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);

        if (memoryMonitoringSupported) {
            add(chartSupport.getChart(), BorderLayout.CENTER);
            chartSupport.updateDetails(new String[]{UNKNOWN, UNKNOWN, UNKNOWN});
        } else {
            add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
        }
    }
}

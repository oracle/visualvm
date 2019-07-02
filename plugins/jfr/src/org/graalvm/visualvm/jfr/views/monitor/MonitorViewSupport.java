/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.monitor;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class MonitorViewSupport {
    
    private static final String UNKNOWN = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Unknown"); // NOI18N
    
    
    static abstract class MasterViewSupport extends JPanel {
        
        private final JFRModel model;
        
        private HTMLTextArea area;
        
        
        MasterViewSupport(JFRModel model) {
            this.model = model;
            initComponents();
        }
        
        
        abstract void firstShown();
        
        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(MonitorViewSupport.class, "LBL_Monitor"), null, this);
        }
        
        void dataComputed() {
            final String telemetry = getBasicTelemetry(model);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { area.setText(telemetry); }
            });
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (model == null) {
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else if (!model.containsEvent(JFRSnapshotMonitorViewProvider.EventChecker.class)) {
                // Remove the Java 7 only event from the list of required events (http://www.oracle.com/hotspot/jvm/vm/gc/heap/perm_gen_summary)
                List<String> eventTypes = new ArrayList();
                eventTypes.addAll(Arrays.asList(JFRSnapshotMonitorViewProvider.EventChecker.checkedTypes()));
                eventTypes.remove(JFRSnapshotMonitorViewProvider.EVENT_PERMGEN_SUMMARY);
                
                add(MessageComponent.noData(NbBundle.getMessage(MonitorViewSupport.class, "LBL_Monitor"), eventTypes.toArray(new String[0])), BorderLayout.CENTER);
            } else {
                area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
                area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

                add(area, BorderLayout.CENTER);

                addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                            if (isShowing()) {
                                removeHierarchyListener(this);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() { firstShown(); }
                                });
                            }
                        }
                    }
                });
            }
        }
        
        private String getBasicTelemetry(JFRModel model) {
            long startTime = model.getJvmStartTime();
            long endTime = model.getJvmShutdownTime();
            boolean terminated = endTime != -1;
            if (!terminated) endTime = model.getLastEventTime();
            
            String ret = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Uptime", (startTime == -1 ? "&lt;unknown&gt;" : getTime(endTime - startTime))); // NOI18N
            if (terminated) {
                String reason = model.getJvmShutdownReason();
                ret += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Terminated:</b> " + (reason != null ? reason : "&lt;unknown reason&gt;");
            }
            return ret;
        }
        
        private static String getTime(long millis) {
            // Hours
            long hours = millis / 3600000;
            String sHours = hours == 0 ? null : new DecimalFormat("#0").format(hours); // NOI18N
            millis %= 3600000;
            
            // Minutes
            long minutes = millis / 60000;
            String sMinutes = new DecimalFormat(hours > 0 ? "00" : "#0").format(minutes); // NOI18N
            millis %= 60000;
            
            // Seconds
            String sSeconds = new DecimalFormat("#0.000").format(millis / 1000d); // NOI18N
            
            if (sHours == null) {
                 return NbBundle.getMessage(MonitorViewSupport.class, "FORMAT_ms", // NOI18N
                                            new Object[] { sMinutes, sSeconds });
            } else {
                return NbBundle.getMessage(MonitorViewSupport.class, "FORMAT_hms", // NOI18N
                                            new Object[] { sHours, sMinutes, sSeconds });
            }
        }
        
    }
    
    
    static class CPUViewSupport extends JPanel implements JFREventVisitor {
        
        private static final String CPU = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Cpu"); // NOI18N
        private static final String CPU_USAGE = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Cpu_Usage"); // NOI18N
//        private static final String GC_USAGE = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Gc_Usage"); // NOI18N

        private final boolean liveModel = false;
//        private int processorsCount;
        private boolean cpuMonitoringSupported;
        private boolean gcMonitoringSupported;

        private SimpleXYChartSupport chartSupport;
        
        
        CPUViewSupport() {
            initModels();
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(CPU, null, 10, this, null);
        }
        
        
        // --- Visitor ---
        
        private static final class CPU extends Record {
            final long value;
            CPU(JFREvent event) throws JFRPropertyNotAvailableException {
                super(event);
                value = Math.round(event.getFloat("jvmUser") * 1000) + Math.round(event.getFloat("jvmSystem") * 1000); // TODO: ??? // NOI18N
            }
        }
        
        private List<CPU> records;
        
        @Override
        public void init() {
            records = new ArrayList();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotMonitorViewProvider.EVENT_CPU_LOAD.equals(typeName)) {
                try {
                    records.add(new CPU(event));
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            Collections.sort(records, Record.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final CPU record : records)
                        chartSupport.addValues(record.time, new long[] { record.value/*, 0*/ });

                    if (!records.isEmpty()) {
                        CPU last = records.get(records.size() - 1);
                        records.clear();
                        
                        String cpuDetail = chartSupport.formatPercent(last.value);
                        chartSupport.updateDetails(new String[] { cpuDetail/*, UNKNOWN*/ });
                    }
                    
                    records = null;
                }
            });
        }
        
        // ---

        private void initModels() {
            cpuMonitoringSupported = true;
            gcMonitoringSupported = false;

            if (cpuMonitoringSupported || gcMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.percent(false, 0.1d, Integer.MAX_VALUE);

                chartDescriptor.addLineItems(CPU_USAGE/*, GC_USAGE*/);
                chartDescriptor.setDetailsItems(new String[] { CPU_USAGE/*, GC_USAGE*/ });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (cpuMonitoringSupported || gcMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer("JFR snapshot"), BorderLayout.CENTER);
            }
        }
        
    }
    
    
    static class HeapViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean liveModel = false;
        private boolean memoryMonitoringSupported;
        private final String heapName = "Heap";

        private SimpleXYChartSupport chartSupport;

        public HeapViewSupport() {
            initModels();
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(heapName, null, 10, this, null);
        }
        
        
        // --- Visitor ---
        
        private static final class Heap extends Record {
            final long used;
            final long commited;
            Heap(JFREvent event) throws JFRPropertyNotAvailableException {
                super(event);
                used = event.getLong("heapUsed"); // NOI18N
                commited = event.getLong("heapSpace.committedSize"); // NOI18N
            }
        }
        
        private List<Heap> records;
        private JFREvent lastEvent;
        private Instant lastEventTime;
        
        @Override
        public void init() {
            records = new ArrayList();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotMonitorViewProvider.EVENT_HEAP_SUMMARY.equals(typeName))
                try {
                    records.add(new Heap(event)); // NOI18N

                    Instant eventTime = event.getInstant("eventTime"); // NOI18N
                    if (lastEventTime == null || lastEventTime.isBefore(eventTime)) {
                        lastEvent = event;
                        lastEventTime = eventTime;
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            return false;
        }
        
        @Override
        public void done() {
            Collections.sort(records, Record.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final Heap record : records)
                        chartSupport.addValues(record.time, new long[] { record.commited, record.used });

                    if (!records.isEmpty()) {
                        records.clear();
                        
                        try {
                            long heapUsed = lastEvent.getLong("heapUsed"); // NOI18N
                            long heapSpace_committedSize = lastEvent.getLong("heapSpace.committedSize"); // NOI18N
                            long heapSpace_reservedSize = lastEvent.getLong("heapSpace.reservedSize"); // NOI18N
                            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(heapUsed),
                                                                      chartSupport.formatBytes(heapSpace_committedSize),
                                                                      chartSupport.formatBytes(heapSpace_reservedSize) });
                        } catch (JFRPropertyNotAvailableException e) {}
                    }
                    
                    records = null;
                    lastEvent = null;
                    lastEventTime = null;
                }
            });
        }

        private void initModels() {
//            liveModel = model.isLive();
            memoryMonitoringSupported = true;
//            heapName = memoryMonitoringSupported ? model.getHeapName() : NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Memory"); // NOI18N

            if (memoryMonitoringSupported) {
                String HEAP_SIZE = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Heap_size"); // NOI18N
                String HEAP_SIZE_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Heap_size_leg",heapName); // NOI18N
                String USED_HEAP = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Used_heap"); // NOI18N
                String USED_HEAP_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Used_heap_leg",heapName.toLowerCase()); // NOI18N
                String MAX_HEAP = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Max_Heap");   // NOI18N

                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, Integer.MAX_VALUE);

                chartDescriptor.addLineFillItems(HEAP_SIZE_LEG, USED_HEAP_LEG);
                chartDescriptor.setDetailsItems(new String[] { HEAP_SIZE, USED_HEAP, MAX_HEAP });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
//                model.registerHeapChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer("JFR snapshot"), BorderLayout.CENTER);
            }
        }

    }
    
    
    static class PermGenViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean liveModel = false;
        private boolean memoryMonitoringSupported;
        private final String heapName = "PermGen";

        private SimpleXYChartSupport chartSupport;

        public PermGenViewSupport() {
            initModels();
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(heapName, null, 20, this, null);
        }
        
        
        // --- Visitor ---
        
        private static final class NonHeap extends Record {
            final long used;
            final long commited;
            NonHeap(JFREvent event) throws JFRPropertyNotAvailableException {
                super(event);
                used = event.getLong("objectSpace.used"); // NOI18N
                commited = event.getLong("permSpace.committedSize"); // NOI18N
            }
        }
        
        private List<NonHeap> records;
        private JFREvent lastEvent;
        private Instant lastEventTime;
        
        @Override
        public void init() {
            records = new ArrayList();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotMonitorViewProvider.EVENT_PERMGEN_SUMMARY.equals(typeName)) {
                try {
                    records.add(new NonHeap(event));

                    Instant eventTime = event.getInstant("eventTime"); // NOI18N
                    if (lastEventTime == null || lastEventTime.isBefore(eventTime)) {
                        lastEvent = event;
                        lastEventTime = eventTime;
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            Collections.sort(records, Record.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final NonHeap record : records)
                        chartSupport.addValues(record.time, new long[] { record.commited, record.used });

                    if (!records.isEmpty()) {
                        records.clear();
                        
                        try {
                            long permspace_used = lastEvent.getLong("objectSpace.used"); // NOI18N
                            long permspace_committed = lastEvent.getLong("permSpace.committedSize"); // NOI18N
                            long permspace_reserved = lastEvent.getLong("permSpace.reservedSize"); // NOI18N
                            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(permspace_used),
                                                                      chartSupport.formatBytes(permspace_committed),
                                                                      chartSupport.formatBytes(permspace_reserved) });
                        } catch (JFRPropertyNotAvailableException e) {}
                    }
                    
                    records = null;
                    lastEvent = null;
                    lastEventTime = null;
                }
            });
        }

        private void initModels() {
//            liveModel = model.isLive();
            memoryMonitoringSupported = true;
//            heapName = memoryMonitoringSupported ? model.getHeapName() : NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Memory"); // NOI18N

            if (memoryMonitoringSupported) {
                String HEAP_SIZE = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Heap_size"); // NOI18N
                String HEAP_SIZE_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Heap_size_leg",heapName); // NOI18N
                String USED_HEAP = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Used_heap"); // NOI18N
                String USED_HEAP_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Used_heap_leg",heapName.toLowerCase()); // NOI18N
                String MAX_HEAP = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Max_Heap");   // NOI18N

                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, Integer.MAX_VALUE);

                chartDescriptor.addLineFillItems(HEAP_SIZE_LEG, USED_HEAP_LEG);
                chartDescriptor.setDetailsItems(new String[] { HEAP_SIZE, USED_HEAP, MAX_HEAP });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
//                model.registerHeapChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer("JFR snapshot"), BorderLayout.CENTER);
            }
        }

    }
    
    
    static class MetaspaceViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean liveModel = false;
        private boolean memoryMonitoringSupported;
        private final String heapName = "Metaspace";

        private SimpleXYChartSupport chartSupport;

        public MetaspaceViewSupport() {
            initModels();
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(heapName, null, 20, this, null);
        }
        
        
        // --- Visitor ---
        
        private static final class NonHeap extends Record {
            final long used;
            final long commited;
            NonHeap(JFREvent event) throws JFRPropertyNotAvailableException {
                super(event);
                used = event.getLong("metaspace.used"); // NOI18N
                commited = event.getLong("metaspace.committed"); // NOI18N
            }
        }
        
        private List<NonHeap> records;
        private JFREvent lastEvent;
        private Instant lastEventTime;
        
        @Override
        public void init() {
            records = new ArrayList();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotMonitorViewProvider.EVENT_METASPACE_SUMMARY.equals(typeName)) {
                try {
                    records.add(new NonHeap(event));

                    Instant eventTime = event.getInstant("eventTime"); // NOI18N
                    if (lastEventTime == null || lastEventTime.isBefore(eventTime)) {
                        lastEvent = event;
                        lastEventTime = eventTime;
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            Collections.sort(records, Record.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final NonHeap record : records)
                        chartSupport.addValues(record.time, new long[] { record.commited, record.used });

                    if (!records.isEmpty()) {
                        records.clear();
                        
                        try {
                            long metaspace_used = lastEvent.getLong("metaspace.used"); // NOI18N
                            long metaspace_committed = lastEvent.getLong("metaspace.committed"); // NOI18N
                            long metaspace_reserved = lastEvent.getLong("metaspace.reserved"); // NOI18N
                            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(metaspace_used),
                                                                      chartSupport.formatBytes(metaspace_committed),
                                                                      chartSupport.formatBytes(metaspace_reserved) });
                        } catch (JFRPropertyNotAvailableException e) {}
                    }
                    
                    records = null;
                    lastEvent = null;
                    lastEventTime = null;
                }
            });
        }

        private void initModels() {
//            liveModel = model.isLive();
            memoryMonitoringSupported = true;
//            heapName = memoryMonitoringSupported ? model.getHeapName() : NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Memory"); // NOI18N

            if (memoryMonitoringSupported) {
                String HEAP_SIZE = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Heap_size"); // NOI18N
                String HEAP_SIZE_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Heap_size_leg",heapName); // NOI18N
                String USED_HEAP = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Used_heap"); // NOI18N
                String USED_HEAP_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Used_heap_leg",heapName.toLowerCase()); // NOI18N
                String MAX_HEAP = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Max_Heap");   // NOI18N

                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, Integer.MAX_VALUE);

                chartDescriptor.addLineFillItems(HEAP_SIZE_LEG, USED_HEAP_LEG);
                chartDescriptor.setDetailsItems(new String[] { HEAP_SIZE, USED_HEAP, MAX_HEAP });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
//                model.registerHeapChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer("JFR snapshot"), BorderLayout.CENTER);
            }
        }

    }
    
    
    static class ClassesViewSupport extends JPanel implements JFREventVisitor {

        private static final String TOTAL_LOADED = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Total_loaded_classes");   // NOI18N
        private static final String TOTAL_LOADED_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Total_loaded_classes_leg");   // NOI18N
//        private static final String SHARED_LOADED = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Shared_loaded_classes"); // NOI18N
//        private static final String SHARED_LOADED_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Shared_loaded_classes_leg"); // NOI18N
        private static final String TOTAL_UNLOADED = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Total_unloaded_classes");   // NOI18N
//        private static final String SHARED_UNLOADED = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Shared_unloaded_classes"); // NOI18N

        private final boolean liveModel = false;
        private boolean classMonitoringSupported;

        private SimpleXYChartSupport chartSupport;

        ClassesViewSupport() {
            initModels();
            initComponents();
        }

        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(MonitorViewSupport.class, "LBL_Classes"), null, 10, this, null);   // NOI18N
        }
        
        
        // --- Visitor ---
        
        private static final class Classes extends Record {
            final long loaded;
            Classes(JFREvent event) throws JFRPropertyNotAvailableException {
                super(event);
                loaded = event.getLong("loadedClassCount"); // NOI18N
            }
        }
        
        private List<Classes> records;
        private JFREvent lastEvent;
        private Instant lastEventTime;
        
        @Override
        public void init() {
            records = new ArrayList();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotMonitorViewProvider.EVENT_CLASS_LOADING.equals(typeName)) {
                try {
                    records.add(new Classes(event));
                    
                    Instant eventTime = event.getInstant("eventTime"); // NOI18N
                    if (lastEventTime == null || lastEventTime.isBefore(eventTime)) {
                        lastEvent = event;
                        lastEventTime = eventTime;
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            Collections.sort(records, Record.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final Classes record : records)
                        chartSupport.addValues(record.time, new long[] { record.loaded/*, 0*/ });

                    if (!records.isEmpty()) {
                        records.clear();
                        
                        try {
                            long loadedClassCount = lastEvent.getLong("loadedClassCount"); // NOI18N
                            long unloadedClassCount = lastEvent.getLong("unloadedClassCount"); // NOI18N
                            chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(loadedClassCount),
                                                                      chartSupport.formatDecimal(unloadedClassCount)/*,
                                                                      chartSupport.formatDecimal(totalUnloaded),
                                                                      chartSupport.formatDecimal(sharedUnloaded)*/ });
                        } catch (JFRPropertyNotAvailableException e) {}
                    }
                    
                    records = null;
                    lastEvent = null;
                    lastEventTime = null;
                }
            });
        }

        private void initModels() {
//            liveModel = model.isLive();
            classMonitoringSupported = true;

            if (classMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.decimal(100, false, Integer.MAX_VALUE);

                chartDescriptor.addLineItems(TOTAL_LOADED_LEG/*, SHARED_LOADED_LEG*/);
                chartDescriptor.setDetailsItems(new String[] { TOTAL_LOADED, /*SHARED_LOADED,*/
                                                               TOTAL_UNLOADED/*, SHARED_UNLOADED*/ });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
//                model.registerClassesChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (classMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN/*, UNKNOWN, UNKNOWN*/ });
            } else {
                add(new NotSupportedDisplayer("JFR snapshot"),
                    BorderLayout.CENTER);
            }
        }

    }
    
    
    static class ThreadsViewSupport extends JPanel implements JFREventVisitor {

        private static final String LIVE = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Live_threads");   // NOI18N
        private static final String LIVE_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Live_threads_leg");   // NOI18N
        private static final String DAEMON = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Daemon_threads");// NOI18N
        private static final String DAEMON_LEG = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Daemon_threads_leg");// NOI18N
        private static final String PEAK = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Live_threads_peak");  // NOI18N
        private static final String STARTED = NbBundle.getMessage(MonitorViewSupport.class, "LBL_Started_threads_total");   // NOI18N

        private final boolean liveModel = false;
        private boolean threadsMonitoringSupported;

        private SimpleXYChartSupport chartSupport;

        ThreadsViewSupport() {
            initModels();
            initComponents();
        }

        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(MonitorViewSupport.class, "LBL_Threads"), null, 10, this, null);   // NOI18N
        }
        
        
        // --- Visitor ---
        
        private static final class Threads extends Record {
            final long active;
            final long daemon;
            Threads(JFREvent event) throws JFRPropertyNotAvailableException {
                super(event);
                active = event.getLong("activeCount"); // NOI18N
                daemon = event.getLong("daemonCount"); // NOI18N
            }
        }
        
        private List<Threads> records;
        private JFREvent lastEvent;
        private Instant lastEventTime;
        
        @Override
        public void init() {
            records = new ArrayList();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotMonitorViewProvider.EVENT_JAVA_THREAD.equals(typeName)) {
                try {
                    records.add(new Threads(event));
                    
                    Instant eventTime = event.getInstant("eventTime"); // NOI18N
                    if (lastEventTime == null || lastEventTime.isBefore(eventTime)) {
                        lastEvent = event;
                        lastEventTime = eventTime;
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            Collections.sort(records, Record.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final Threads record : records)
                        chartSupport.addValues(record.time, new long[] { record.active, record.daemon });

                    if (!records.isEmpty()) {
                        records.clear();
                        
                        try {
                            // TODO: lastEvent may not have the last timestamp!
                            long activeCount = lastEvent.getLong("activeCount"); // NOI18N
                            long daemonCount = lastEvent.getLong("daemonCount"); // NOI18N
                            long peakCount = lastEvent.getLong("peakCount"); // NOI18N
                            long accumulatedCount = lastEvent.getLong("accumulatedCount"); // NOI18N
                            chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(activeCount),
                                                                      chartSupport.formatDecimal(daemonCount),
                                                                      chartSupport.formatDecimal(peakCount),
                                                                      chartSupport.formatDecimal(accumulatedCount) });
                        } catch (JFRPropertyNotAvailableException e) {}
                    }
                    
                    records = null;
                    lastEvent = null;
                    lastEventTime = null;
                }
            });
        }

        private void initModels() {
//            liveModel = model.isLive();
            threadsMonitoringSupported = true;

            if (threadsMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.decimal(3, false, Integer.MAX_VALUE);

                chartDescriptor.addLineItems(LIVE_LEG, DAEMON_LEG);
                chartDescriptor.setDetailsItems(new String[] { LIVE, DAEMON,
                                                               PEAK, STARTED });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
//                model.registerThreadsChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (threadsMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer("JFR snapshot"), BorderLayout.CENTER);
            }
        }

    }
    
    
    private static abstract class Record {
        final long time;
        Record(JFREvent event) throws JFRPropertyNotAvailableException { time = event.getInstant("eventTime").toEpochMilli(); } // NOI18N
        @Override public int hashCode() { return Long.hashCode(time); }
        @Override public boolean equals(Object o) { return o instanceof Record ? ((Record)o).time == time : false; }
        
        static final Comparator<Record> COMPARATOR = new Comparator<Record>() {
            @Override public int compare(Record r1, Record r2) { return Long.compare(r1.time, r2.time); }
        };
    }
    
}

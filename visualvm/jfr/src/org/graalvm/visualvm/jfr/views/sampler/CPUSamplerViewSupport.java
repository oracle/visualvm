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
package org.graalvm.visualvm.jfr.views.sampler;

import java.awt.BorderLayout;
import java.awt.Font;
import java.text.Format;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.StackTraceSnapshotBuilder;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.cpu.SnapshotCPUView;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.PercentRenderer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class CPUSamplerViewSupport {
    
    static final class CPUViewSupport extends JPanel implements JFREventVisitor {
        
        private static final Set<String> IGNORED_EVENTS = new HashSet(Arrays.asList(
                "jdk.ThreadStart", "jdk.ThreadEnd" // NOI18N
        ));
        
        
        private final JFRModel model;
        
        private final boolean hasData;
        
        private List<JFREventWithStack> data;
        
        
        CPUViewSupport(JFRModel model) {
            this.model = model;
            
            hasData = true; // all events used, let's assume some of them contain stack traces
//            hasData = model.containsEvent(JFRSnapshotSamplerViewProvider.CPUSampleChecker.class);
            
            initComponents();
        }
        
        
        @Override
        public void init() {
            if (hasData) data = new ArrayList();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (!hasData) return true;
            
            try {
                if (JFRSnapshotSamplerViewProvider.EVENT_EXECUTION_SAMPLE.equals(typeName) ||
                    JFRSnapshotSamplerViewProvider.EVENT_NATIVE_SAMPLE.equals(typeName)) {
    //                System.err.println(">>> visiting " + typeName);

                    data.add(new JFREventWithStack(typeName, event, model));
                } else if ("jdk.ThreadEnd".equals(typeName)) { // NOI18N
                    data.add(new JFREventWithStack(typeName, event, model));
                } else if (!IGNORED_EVENTS.contains(typeName)) {
                    JFRThread thread = event.getThread("eventThread"); // NOI18N
                    if (thread != null) {
                        JFRStackTrace stack = event.getStackTrace("eventStackTrace"); // NOI18N
                        if (stack != null) {
                            Instant time = event.getInstant("eventTime"); // NOI18N
                            if (time != null) {
                                data.add(new JFREventWithStack(typeName, event, model));
                            }
                        }
                    }
                }
            } catch (JFRPropertyNotAvailableException e) {} // NOTE: valid state, the event doesn't contain thread information
            
            return false;
        }

        @Override
        public void done() {
            if (hasData) {
                StackTraceSnapshotBuilder builder = new StackTraceSnapshotBuilder();
                Map<Long, Map<String, Object>> threads = new HashMap();

                Collections.sort(data);
                
                long now = System.nanoTime();
                
                for (JFREventWithStack ev : data) {
                    try {
                        if ("jdk.ThreadEnd".equals(ev.typeName)) { // NOI18N
                            threads.remove(ev.event.getThread("eventThread").getId()); // NOI18N
                        } else {
                            Map<String, Object> threadInfo = ev.getThreadInfo();

                            threads.put((Long)threadInfo.get("tid"), threadInfo); // NOI18N
                            builder.addStacktrace(getAllThreads(threads), now + ev.getEventTime());
                        }
                    } catch (JFRPropertyNotAvailableException e) {
                        System.err.println(">>> " + e + " -- " + ev.event);
                    }
                }

                data = null;

                try {
                    final CPUResultsSnapshot snapshot = builder.createSnapshot(System.currentTimeMillis());
                    builder = null;
                    threads = null;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            removeAll();
                            add(createView(snapshot), BorderLayout.CENTER);
                        }
                    });
                } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
                    // TODO: notify no data snapshot
                }
            }
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_Cpu_samples"), null, 10, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
//            if (!hasData) {
//                setLayout(new BorderLayout());
//                add(MessageComponent.noData("CPU samples", JFRSnapshotSamplerViewProvider.CPUSampleChecker.checkedTypes()), BorderLayout.CENTER);
//            }
        }
        
        private SnapshotCPUView createView(CPUResultsSnapshot snapshot) {
            return new SnapshotCPUView(snapshot, true, null, null, null, null) {
                @Override protected boolean profileMethodEnabled() { return false; }
                @Override protected boolean profileMethodSupported() { return false; }
                @Override protected boolean profileClassSupported() { return false; }
                @Override protected boolean showSourceSupported() { return false; }
                @Override protected void showSource(ClientUtils.SourceCodeSelection value) {}
                @Override protected void selectForProfiling(ClientUtils.SourceCodeSelection value) {}
            };
        }

        private Map<String, Object>[] getAllThreads(Map<Long, Map<String, Object>> threads) {
            Collection<Map<String, Object>> allThreds = threads.values();

            return allThreds.toArray(new Map[0]);
        }
        
    }
    
    private static class JFREventWithStack implements Comparable<JFREventWithStack> {

        private final String typeName;
        private final JFREvent event;
        private final long eventTime;

        private JFREventWithStack(String typeName, JFREvent event, JFRModel model) throws JFRPropertyNotAvailableException {
            this.typeName = typeName;
            this.event = event;
            eventTime = ValuesConverter.instantToRelativeNanos(event.getInstant("eventTime"), model);
//            eventTime = ValuesConverter.durationToNanos(Duration.between(model.getFirstEventTime(), event.getInstant("eventTime"))) + 1000000000000l;
//            eventTime = ValuesConverter.instantToRelativeNanos(event.getInstant("eventTime"), model);
        }

        private Thread.State getState() {
            if ("jdk.JavaMonitorWait".equals(typeName)) { // NOI18N
                return Thread.State.WAITING;
            }
            if ("jdk.JavaMonitorEnter".equals(typeName)) { // NOI18N
                return Thread.State.BLOCKED;
            }
            if ("jdk.ThreadPark".equals(typeName)) { // NOI18N
                return Thread.State.WAITING;
            }
            if ("jdk.ThreadSleep".equals(typeName)) { // NOI18N
                return Thread.State.TIMED_WAITING;
            }
            return Thread.State.RUNNABLE;
        }

//        private long getNanoTime() throws JFRPropertyNotAvailableException {
//            Instant inst = getEventTime();
//            long nanoSec = TimeUnit.SECONDS.toNanos(inst.getEpochSecond());
//
//            return nanoSec + inst.getNano();
//        }

        long getEventTime() {
            return eventTime;
        }

        private boolean isProfilingEvent() {
            return JFRSnapshotSamplerViewProvider.EVENT_EXECUTION_SAMPLE.equals(typeName)
                  || JFRSnapshotSamplerViewProvider.EVENT_NATIVE_SAMPLE.equals(typeName);
        }

        private Map<String,Object> getThreadInfo() throws JFRPropertyNotAvailableException {
            JFRStackTrace stack = event.getStackTrace("eventStackTrace"); // NOI18N

            if (isProfilingEvent()) {
                String state = event.getString("state"); // NOI18N
                JFRThread thread = event.getThread("sampledThread"); // NOI18N

                return JFRThreadInfoSupport.getThreadInfo(thread, stack, state);
            }

            JFRThread thread = event.getThread("eventThread"); // NOI18N
            return JFRThreadInfoSupport.getThreadInfo(thread, stack, getState());
        }

        @Override
        public int hashCode() {
            return event.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof JFREventWithStack) {
                JFREventWithStack ev = (JFREventWithStack) obj;

                return event.equals(ev.event);
            }
            return false;
        }

        @Override
        public int compareTo(JFREventWithStack o) {
            return Long.compare(getEventTime(), o.getEventTime());
        }
    }
    
    static final class ThreadsCPUViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean hasData;
        
        private Map<String, Double> eventData;
        
        private String[] names;
        private double[] values;
        
        private TreadsAllocTableModel tableModel;
        private ProfilerTable table;
        
        
        ThreadsCPUViewSupport(JFRModel model) {
            hasData = model.containsEvent(JFRSnapshotSamplerViewProvider.ThreadCPUChecker.class);
            
            initComponents();
        }
        
        
        @Override
        public void init() {
            if (hasData) eventData = new HashMap();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (!hasData) return true;
            
            if (JFRSnapshotSamplerViewProvider.EVENT_THREAD_CPU.equals(typeName)) { // NOI18N
                try {
                    String threadName = event.getThread("eventThread").getName(); // NOI18N
                    double utilization = 100d * (event.getFloat("user") + event.getFloat("system")); // NOI18N
                    Double _utilization = eventData.get(threadName);
                    if (_utilization == null || _utilization < utilization)
                        eventData.put(threadName, utilization);
                } catch (JFRPropertyNotAvailableException e) { System.err.println(">>> " + e); }
            }
            return false;
        }

        @Override
        public void done() {
            if (hasData) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    names = new String[eventData.size()];
                    values = new double[eventData.size()];

                    int i = 0;
                    for (Map.Entry<String, Double> entry : eventData.entrySet()) {
                        names[i] = entry.getKey();
                        values[i++] = entry.getValue();
                    }
                    
                    tableModel.fireTableDataChanged();

                    eventData.clear();
                    eventData = null;
                }
            });
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_ThreadAlloc"), null, 20, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (!hasData) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Thread CPU load", JFRSnapshotSamplerViewProvider.ThreadCPUChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                tableModel = new TreadsAllocTableModel();
                table = new ProfilerTable(tableModel, true, true, null);

                table.setMainColumn(0);
                table.setFitWidthColumn(0);

                table.setSortColumn(1);
                table.setDefaultSortOrder(SortOrder.DESCENDING);
                table.setDefaultSortOrder(0, SortOrder.ASCENDING);

                HideableBarRenderer percentRenderer = new HideableBarRenderer(new PercentRenderer() {
                    @Override
                    protected String getValueString(Object value, int row, Format format) {
                        String s = super.getValueString(value, row, format);
                        if (s.startsWith("(")) s = s.substring(1, s.length() - 1); // NOI18N
                        return s;
                    }
                });

                LabelRenderer threadRenderer = new LabelRenderer();
                threadRenderer.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
                threadRenderer.setFont(threadRenderer.getFont().deriveFont(Font.BOLD));

                table.setColumnRenderer(0, threadRenderer);
                table.setColumnRenderer(1, percentRenderer);

                percentRenderer.setMaxValue(9999999);
                table.setDefaultColumnWidth(1, percentRenderer.getOptimalWidth());
                percentRenderer.setMaxValue(100);

                add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
            }
        }
        
        
        private class TreadsAllocTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Thread";
                } else if (columnIndex == 1) {
                    return "Top Utilization";
                }

                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return String.class;
                } else {
                    return Double.class;
                }
            }

            public int getRowCount() {
                return names == null ? 0 : names.length;
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return names[rowIndex];
                } else if (columnIndex == 1) {
                    return values[rowIndex];
                }

                return null;
            }

        }
        
    }
    
}

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.StackTraceSnapshotBuilder;
import org.graalvm.visualvm.lib.jfluid.results.cpu.StackTraceSnapshotBuilder.SampledThreadInfo;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.cpu.SnapshotCPUView;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.PercentRenderer;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class CPUSamplerViewSupport {
    
    static final class CPUViewSupport extends JPanel implements JFREventVisitor {
        
        private static final Set<String> IGNORED_EVENTS = new HashSet(Arrays.asList(
                "jdk.ThreadStart", "jdk.ThreadEnd", "jdk.JavaMonitorWait",  // NOI18N
                "jdk.JavaMonitorEnter", "jdk.ThreadPark", "jdk.ThreadSleep" // NOI18N
        ));
        
//        private static final Set<String> IGNORED_EVENTS = Set.of(
//                "jdk.ThreadStart", "jdk.ThreadEnd", "jdk.JavaMonitorWait",  // NOI18N
//                "jdk.JavaMonitorEnter", "jdk.ThreadPark", "jdk.ThreadSleep" // NOI18N
//        );
        
        private Map<Long, SampledThreadInfo> data;
        
        
        CPUViewSupport() {
            initComponents();
        }
        
        
        @Override
        public void init() {
            data = new TreeMap();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.ExecutionSample".equals(typeName) || "jdk.NativeMethodSample".equals(typeName)) { // NOI18N
//                System.err.println(">>> visiting " + typeName);
                try {
                    long time = event.getInstant("eventTime").toEpochMilli();
                    String state = event.getString("state"); // NOI18N
                    JFRThread thread = event.getThread("sampledThread"); // NOI18N
                    JFRStackTrace stack = event.getStackTrace("eventStackTrace");

                    SampledThreadInfo info = JFRThreadInfoSupport.getInfo(thread, stack, state, null);
                    data.put(time, info);
//                    System.err.println(">>>   DONE " + event);
                } catch (JFRPropertyNotAvailableException e) {System.err.println(">>> 111 " + e + " -- " + event);}
            } else if (!IGNORED_EVENTS.contains(typeName)) {
                try {
                    JFRThread thread = event.getThread("eventThread");
                    if (thread != null) {
                        JFRStackTrace stack = event.getStackTrace("eventStackTrace");
                        if (stack != null) {
                            Instant time = event.getInstant("eventTime");
                            if (time != null) {
                                SampledThreadInfo info = JFRThreadInfoSupport.getInfo(thread, stack, Thread.State.RUNNABLE, null);
                                data.put(time.toEpochMilli(), info);
                            }
                        }
                    }
                } catch (JFRPropertyNotAvailableException e) {} // NOTE: valid state, the event doesn't contain thread information
            }
            
            return false;
        }

        @Override
        public void done() {
            new RequestProcessor().post(new Runnable() {
                public void run() {
                    StackTraceSnapshotBuilder builder = new StackTraceSnapshotBuilder();
                    builder.reset();
                    
                    for (Map.Entry<Long, SampledThreadInfo> entry : data.entrySet())
                        builder.addStacktrace(new SampledThreadInfo[] { entry.getValue() }, entry.getKey());
                    
                    data.clear();
                    data = null;
                    
                    try {
                        final CPUResultsSnapshot snapshot = builder.createSnapshot(System.currentTimeMillis());
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
            });
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_Cpu_samples"), null, 10, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
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
        
    }
    
    
    static final class ThreadsCPUViewSupport extends JPanel implements JFREventVisitor {
        
        private Map<String, Double> eventData;
        
        private String[] names;
        private double[] values;
        
        private TreadsAllocTableModel tableModel;
        private ProfilerTable table;
        
        
        ThreadsCPUViewSupport() {
            initComponents();
        }
        
        
        @Override
        public void init() {
            eventData = new HashMap();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.ThreadCPULoad".equals(typeName)) { // NOI18N
                try {
                    eventData.put(event.getThread("eventThread").getName(), 100d * (event.getFloat("user") + event.getFloat("system"))); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {System.err.println(">>> %% " + e);}
            }
            return false;
        }

        @Override
        public void done() {
            SwingUtilities.invokeLater(new Runnable() {
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
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private class TreadsAllocTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Thread";
                } else if (columnIndex == 1) {
                    return "Utilization";
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

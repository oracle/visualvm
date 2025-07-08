/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.environment;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.utils.TimeRecord;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class EnvironmentViewSupport {
    
    static abstract class MasterViewSupport extends JPanel implements JFREventVisitor {

        private String osInfo = "<nobr><b>OS:</b> &lt;unknown&gt;</nbsp>";
        private String cpuInfo = "<nobr><b>CPU:</b> &lt;unknown&gt;</nbsp>";
        private String memInfo = "<nobr><b>Memory:</b> &lt;unknown&gt;</nbsp>";
        
        private HTMLTextArea area;

        
        MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        abstract void firstShown();

        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Environment", null, this);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotEnvironmentViewProvider.EVENT_OS_INFO.equals(typeName)) { // NOI18N
                try {
                    osInfo = formatOSInfo(event.getString("osVersion")); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    osInfo = "<not available>";
                }
            } else if (JFRSnapshotEnvironmentViewProvider.EVENT_CPU_INFO.equals(typeName)) { // NOI18N
                try {
                    cpuInfo = formatCPUInfo(event.getString("description")); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    osInfo = "<nobr><b>OS:</b> &lt;not available&gt;</nbsp>";
                }
            } else if (JFRSnapshotEnvironmentViewProvider.EVENT_PHYSICAL_MEMORY.equals(typeName)) { // NOI18N
                try {
                    memInfo = formatMemInfo(event.getLong("totalSize")); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    memInfo = "<nobr><b>Memory:</b> &lt;not available&gt;</nbsp>";
                }
            }
            return false;
        }
        
        @Override
        public void done() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    area.setText(cpuInfo + "<br>" + memInfo + "<br>" + osInfo);
                    
                    osInfo = null;
                    cpuInfo = null;
                    memInfo = null;
                }
            });
        }
        
        
        private static String formatOSInfo(String os) {
            String[] s = os.split("\\r?\\n"); // NOI18N
            
//            // TODO: String.lines() only available in Java 11+, change to support 8!
//            os.lines().forEach(new Consumer<String>() {
//                @Override
//                public void accept(String t) {
//                    if (s[0] == null && !t.isBlank()) s[0] = t.trim();
//                }
//            });

            s[0] = s[0].trim();
            
            if (s[0].startsWith("OS:")) s[0] = s[0].substring("OS:".length()).trim(); // NOI18N
            if (s[0].startsWith("uname:")) s[0] = s[0].substring("uname:".length()).trim(); // NOI18N
            if (s[0].startsWith("Bsduname:")) s[0] = s[0].substring("Bsduname:".length()).trim(); // NOI18N
            if (s[0].startsWith("DISTRIB_ID=")) s[0] = s[0].substring("DISTRIB_ID=".length()).trim(); // NOI18N
            
            if (s[0].isEmpty() && s.length >= 2) s[0] = s[1];

            int i = s[0].indexOf(';'); // NOI18N
            if (i > 0) s[0] = s[0].substring(0, i).trim();
            
            return "<nobr><b>OS:</b> " + s[0] + "</nbsp>";
        }
        
        private static String formatCPUInfo(String cpu) {
            String[] s = cpu.split("\\r?\\n"); // NOI18N
            
//            cpu.lines().forEach(new Consumer<String>() {
//                @Override
//                public void accept(String t) {
//                    if (s[0] == null && !t.isBlank()) s[0] = t.trim();
//                }
//            });

            s[0] = s[0].trim();
            
            if (s[0].startsWith("Brand:")) s[0] = s[0].substring("Brand:".length()).trim(); // NOI18N
            else return "<nobr><b>CPU:</b> " + s[0] + "</nbsp>";
            
            int i = s[0].indexOf(','); // NOI18N
            if (i > 0) s[0] = s[0].substring(0, i).trim();
            
            return "<nobr><b>CPU:</b> " + s[0] + "</nbsp>";
        }
        
        private static String formatMemInfo(Long mem) {
            String s = NumberFormat.getInstance().format(Math.ceil(mem / 1024d / 1024 / 1024)) + " GB";
            return "<nobr><b>Memory:</b> " + s + " (" + Formatters.bytesFormat().format(new Object[] { mem }) + ")</nbsp>";
        }
        

        private void initComponents(JFRModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (model == null) {
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else if (!model.containsEvent(JFRSnapshotEnvironmentViewProvider.EventChecker.class)) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Environment", JFRSnapshotEnvironmentViewProvider.EventChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr><br><br><br>");
                area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

                add(area, BorderLayout.CENTER);

                addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                            if (isShowing()) {
                                removeHierarchyListener(this);
                                SwingUtilities.invokeLater(MasterViewSupport.this::firstShown);
                            }
                        }
                    }
                });
            }
        }
    }
    
    
    static class CPUUtilizationSupport extends JPanel implements JFREventVisitor {
        
        private final JFRModel jfrModel;
        
        private SimpleXYChartSupport chartSupport;
        
        
        CPUUtilizationSupport(JFRModel jfrModel) {
            this.jfrModel = jfrModel;
            
            initModels();
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("CPU utilization", null, 10, this, null);
        }
        
        
        private static final class CPU extends TimeRecord {
            final long value;
            CPU(JFREvent event, JFRModel jfrModel) throws JFRPropertyNotAvailableException {
                super(event, jfrModel);
                value = Math.round(event.getFloat("machineTotal") * 1000); // NOI18N
            }
        }
        
        private List<CPU> records;
        
        @Override
        public void init() {
            records = new ArrayList<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotEnvironmentViewProvider.EVENT_CPU_LOAD.equals(typeName)) // NOI18N
                 try { records.add(new CPU(event, jfrModel)); }
                 catch (JFRPropertyNotAvailableException e) {}
            return false;
        }
        
        @Override
        public void done() {
            records.sort(TimeRecord.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long lastTime = Long.MIN_VALUE + 1;
                    for (final CPU record : records) {
                        long time = jfrModel.nsToAbsoluteMillis(record.time);
                        if (time <= lastTime) time = lastTime + 1;
                        chartSupport.addValues(time, new long[] { record.value });
                        lastTime = time;
                    }

                    if (!records.isEmpty()) {
                        CPU last = records.get(records.size() - 1);
                        records.clear();
                        
                        String cpuDetail = chartSupport.formatPercent(last.value);
                        chartSupport.updateDetails(new String[] { cpuDetail });
                    }
                    
                    records = null;
                }
            });
        }

        private void initModels() {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.percent(false, 0.1d, Integer.MAX_VALUE);

            chartDescriptor.addLineItems("Machine");
            chartDescriptor.setDetailsItems(new String[] { "Machine" });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);

            chartSupport.setZoomingEnabled(true);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            add(chartSupport.getChart(), BorderLayout.CENTER);
            chartSupport.updateDetails(new String[] { "&lt;unknown&gt;" });
        }
        
    }
    
    
    static class NetworkUtilizationSupport extends JPanel implements JFREventVisitor {
        
        private final JFRModel jfrModel;
        
        private SimpleXYChartSupport chartSupport;
        
        
        NetworkUtilizationSupport(JFRModel jfrModel) {
            this.jfrModel = jfrModel;
            
            initModels();
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Network utilization", null, 20, this, null);
        }
        
        
        private static final class Network extends TimeRecord {
            long read;
            long write;
            Network(JFREvent event, JFRModel jfrModel) throws JFRPropertyNotAvailableException {
                super(event, jfrModel);
                read = event.getLong("readRate"); // NOI18N
                write = event.getLong("writeRate"); // NOI18N
            }
            void add(JFREvent event) {
                try {
                    long readRate = event.getLong("readRate"); // NOI18N
                    long writeRate = event.getLong("writeRate"); // NOI18N
                    read += readRate;
                    write += writeRate;
                } catch (JFRPropertyNotAvailableException e) {}
            }
        }
        
        private Map<Long, Network> records;
        
        @Override
        public void init() {
            records = new TreeMap<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotEnvironmentViewProvider.EVENT_NETWORK_UTILIZATION.equals(typeName)) { // NOI18N
                try {
                    long time = TimeRecord.getTime(event, jfrModel);
                    Network network = records.get(time);
                    if (network == null) records.put(time, new Network(event, jfrModel));
                    else network.add(event);
                } catch (JFRPropertyNotAvailableException e) {}
                
            }
            return false;
        }
        
        @Override
        public void done() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Network last = null;
                    long lastTime = Long.MIN_VALUE + 1;
                    
                    for (final Network record : records.values()) {
                        last = record;
                        long time = jfrModel.nsToAbsoluteMillis(record.time);
                        if (time <= lastTime) time = lastTime + 1;
                        chartSupport.addValues(time, new long[] { record.read, record.write });
                        lastTime = time;
                    }

                    if (last != null) {
                        records.clear();
                        
                        String readDetail = chartSupport.formatBytes(last.read);
                        String writeDetail = chartSupport.formatBytes(last.write);
                        chartSupport.updateDetails(new String[] { readDetail, writeDetail });
                    }
                    
                    records = null;
                }
            });
        }

        private void initModels() {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, Integer.MAX_VALUE);

            chartDescriptor.addLineItems("Read rate", "Write rate");
            chartDescriptor.setDetailsItems(new String[] { "Read rate", "Write rate" });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);

            chartSupport.setZoomingEnabled(true);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            add(chartSupport.getChart(), BorderLayout.CENTER);
            chartSupport.updateDetails(new String[] { "&lt;unknown&gt;", "&lt;unknown&gt;" });
        }
        
    }
    
    
    static class MemoryUsageSupport extends JPanel implements JFREventVisitor {
        
        private final JFRModel jfrModel;
        
        private SimpleXYChartSupport chartSupport;
        
        
        MemoryUsageSupport(JFRModel jfrModel) {
            this.jfrModel = jfrModel;
            
            initModels();
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Memory usage", null, 10, this, null);
        }
        
        
        private static final class Memory extends TimeRecord {
            final long value;
            Memory(JFREvent event, JFRModel jfrModel) throws JFRPropertyNotAvailableException {
                super(event, jfrModel);
                value = event.getLong("usedSize"); // NOI18N
            }
        }
        
        private List<Memory> records;
        private JFREvent lastEvent;
        private long lastEventTime = Long.MIN_VALUE;
        
        @Override
        public void init() {
            records = new ArrayList<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {            
            if (JFRSnapshotEnvironmentViewProvider.EVENT_PHYSICAL_MEMORY.equals(typeName)) { // NOI18N
                try {
                    Memory record = new Memory(event, jfrModel);
                    records.add(record);
                    
                    if (lastEventTime < record.time) {
                        lastEvent = event;
                        lastEventTime = record.time;
                    }
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            records.sort(TimeRecord.COMPARATOR);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long lastTime = Long.MIN_VALUE + 1;
                    for (final Memory record : records) {
                        long time = jfrModel.nsToAbsoluteMillis(record.time);
                        if (time <= lastTime) time = lastTime + 1;
                        chartSupport.addValues(time, new long[] { record.value });
                        lastTime = time;
                    }

                    if (!records.isEmpty()) {
                        records.clear();
                        
                        try {
                            long usedSize = lastEvent.getLong("usedSize"); // NOI18N
                            long totalSize = lastEvent.getLong("totalSize"); // NOI18N
                            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(usedSize),    
                                                                      chartSupport.formatBytes(totalSize) }); 
                        } catch (JFRPropertyNotAvailableException e) {}
                    }
                    
                    records = null;
                    lastEvent = null;
                    
                    records = null;
                }
            });
        }

        private void initModels() {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, Integer.MAX_VALUE);

            chartDescriptor.addLineFillItems("Used");
            chartDescriptor.setDetailsItems(new String[] { "Used", "Total" });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);

            chartSupport.setZoomingEnabled(true);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            add(chartSupport.getChart(), BorderLayout.CENTER);
            chartSupport.updateDetails(new String[] { "&lt;unknown&gt;" });
        }
        
    }
    
    
    static class CPUDetailsSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        CPUDetailsSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("CPU details", null, 10, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotEnvironmentViewProvider.EVENT_CPU_INFO.equals(typeName)) { // NOI18N
                try {
                    final String type = event.getString("cpu"); // NOI18N
                    final int sockets = event.getInt("sockets"); // NOI18N
                    final int cores = event.getInt("cores"); // NOI18N
                    final int threads = event.getInt("hwThreads"); // NOI18N
                    final String description = formatDescription(event.getString("description")); // NOI18N
                    
                    initialized = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(
                                    "<b>Chips:</b>&nbsp;" + sockets + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                    "<b>Cores:</b>&nbsp;" + cores + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                    "<b>Threads:</b>&nbsp;" + threads + "<br><br>" +
                                    "<b>Type:</b>&nbsp;" + type + "<br><br>" +
                                    /*"<b>Details:</b>&nbsp;" +*/ description
                            );
                            area.setCaretPosition(0);
                        }
                    });
                } catch (JFRPropertyNotAvailableException e) {}
                
                return true;
            }
            return false;
        }
        
        @Override
        public void done() {
            if (!initialized) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    area.setText("&lt;unknown&gt;");
                    area.setCaretPosition(0);
                    initialized = true;
                }
            });
        }
        
        
        private static String formatDescription(String description) {
            if (description.startsWith("Brand: ")) { // NOI18N
                String[] parts = description.split("Supports: "); // NOI18N
                if (parts != null && parts.length == 2) {
                    return parts[0].replaceAll("((((?m)^)|(, ))(\\p{Upper}[^:,]+): )", "$2<b>$5:</b> ") + // NOI18N
                           "<b>Supports:</b> " + parts[1]; // NOI18N
                }
            }
            
            return description;
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
    
    
    static class OSDetailsSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        OSDetailsSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("OS details", null, 20, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotEnvironmentViewProvider.EVENT_OS_INFO.equals(typeName)) { // NOI18N
                try {
                    final String version = event.getString("osVersion"); // NOI18N
                    
                    initialized = true;
                
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(version);
                            area.setCaretPosition(0);
                        }
                    });
                } catch (JFRPropertyNotAvailableException e) {}
                
                return true;
            }
            return false;
        }
        
        @Override
        public void done() {
            if (!initialized) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    area.setText("&lt;unknown&gt;");
                    area.setCaretPosition(0);
                    initialized = true;
                }
            });
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
    
    
    static class NetworkDetailsSupport extends JPanel implements JFREventVisitor {
        
        private Set<String> data;
        
        private HTMLTextArea area;
        
        
        NetworkDetailsSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Network details", null, 30, this, null);
        }
        
        
        @Override
        public void init() {
            data = new TreeSet<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotEnvironmentViewProvider.EVENT_NETWORK_UTILIZATION.equals(typeName)) { // NOI18N
                try {
                    data.add(event.getString("networkInterface")); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            final StringBuilder s = new StringBuilder();
            
            Iterator<String> it = data.iterator();
            if (!it.hasNext()) {
                s.append("&lt;unknown&gt;");
            } else {
                s.append("<nobr><b>Network interfaces:</b></nobr><br>");
                while (it.hasNext()) {
                    s.append(it.next());
                    if (it.hasNext()) s.append("<br>"); // NOI18N
                }
            }
            
            data.clear();
            data = null;
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    area.setText(s.toString());
                    area.setCaretPosition(0);
                }
            });
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
    
    
    static class EnvVarSupport extends JPanel implements JFREventVisitor {
        
        private Map<String, String> data;
        
        private HTMLTextArea area;
        
        
        EnvVarSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Environment variables", null, 40, this, null);
        }
        
        
        @Override
        public void init() {
            data = new TreeMap<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotEnvironmentViewProvider.EVENT_ENVIRONMENT_VARIABLE.equals(typeName)) { // NOI18N
                try {
                    data.put(event.getString("key"), event.getString("value")); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            final StringBuilder s = new StringBuilder();
            
            Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
            if (!it.hasNext()) {
                s.append("&lt;unknown&gt;");
            } else {
                while (it.hasNext()) {
                    Map.Entry<String, String> entry = it.next();
                    s.append("<nobr><b>" + entry.getKey() + "</b>=" + entry.getValue() + "</nobr>"); // NOI18N
                    if (it.hasNext()) s.append("<br>"); // NOI18N
                }
            }
            
            data.clear();
            data = null;
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    area.setText(s.toString());
                    area.setCaretPosition(0);
                }
            });
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
    
    
    static class ProcessesSupport extends JPanel implements JFREventVisitor {
        
        private Map<Long, String> data;
        
        private HTMLTextArea area;
        
        
        ProcessesSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("System processes", null, 50, this, null);
        }
        
        
        @Override
        public void init() {
            data = new TreeMap<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotEnvironmentViewProvider.EVENT_SYSTEM_PROCESS.equals(typeName)) { // NOI18N
                try {
                    data.put(Long.parseLong(event.getString("pid")), event.getString("commandLine")); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }
        
        @Override
        public void done() {
            final StringBuilder s = new StringBuilder();
            
            
            
            if (data.isEmpty()) {
                s.append("&lt;unknown&gt;");
            } else {
                s.append("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
                s.append("<tr><th align='right' style='margin-bottom:5px;'>PID&nbsp;&nbsp;&nbsp;</th><th align='left' style='margin-bottom:5px;'>Command Line</th></tr>"); // NOI18N
                
                for (Map.Entry<Long, String> entry : data.entrySet()) {
                    s.append("<tr><td align='right'>"); // NOI18N
                    s.append("<b>" + entry.getKey() + "</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>" + entry.getValue()); // NOI18N
                    s.append("</td></tr>"); // NOI18N
                }
                
                s.append("</table>"); // NOI18N
            }
            
            data.clear();
            data = null;
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    area.setText(s.toString());
                    area.setCaretPosition(0);
                }
            });
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
    
}

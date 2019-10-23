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
package org.graalvm.visualvm.jfr.views.recording;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeNode;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class RecordingViewSupport {
    
    static DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    
    static abstract class MasterViewSupport extends JPanel implements JFREventVisitor {
        
        private HTMLTextArea area;
        
        
        MasterViewSupport(JFRSnapshot snapshot, JFRModel model) {
            initComponents(model);
        }
        
        
        abstract void firstShown();

        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Recording", null, this);  // NOI18N
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.DumpReason".equals(typeName)) { // NOI18N
                try {
                    final String reason = event.getString("reason"); // NOI18N

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            String summary = area.getText();
                            summary += "<br><b>Dump reason:</b>&nbsp;";
                            summary += reason;
                            area.setText(summary);
                        }
                    });
                } catch (JFRPropertyNotAvailableException e) {}
                
                return true;
            }
            return false;
        }
        

        private void initComponents(JFRModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (model == null) {
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else {
                area = new HTMLTextArea(createSummary(model));
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
        
        
        private static String createSummary(JFRModel model) {
            final StringBuilder s = new StringBuilder("<table border='0' cellpadding='0' cellspacing='0'>"); // NOI18N
            
            long firstTime = ValuesConverter.nanosToMillis(model.getFirstEventTime());
            long lastTime = ValuesConverter.nanosToMillis(model.getLastEventTime());
            String firstEventTime = TIME_FORMAT.format(new Date(firstTime));
            String lastEventTime = TIME_FORMAT.format(new Date(lastTime));
            String totalTime = getTime(lastTime - firstTime);
            String eventsCount = NumberFormat.getIntegerInstance().format(model.getEventsCount());
            
            s.append("<tr>");
            s.append("<td><b>First event time:</b>&nbsp;</td><td>").append(firstEventTime).append("</td>");
            s.append("<td style='padding-left: 50px;'><b>Events count:</b>&nbsp;</td><td>").append(eventsCount).append("</td>");
            s.append("</tr>");
            
            s.append("<tr>");
            s.append("<td><b>Last event time:</b>&nbsp;</td><td>").append(lastEventTime.toString()).append("</td>");
            s.append("<td style='padding-left: 50px;'><b>Events time:</b>&nbsp;</td><td>").append(totalTime).append("</td>");
            s.append("</tr>");
            
            s.append("</table>"); // NOI18N
            
            return s.toString();
        }
        
        private static String getTime(long millis) {
            // Hours
            long hours = millis / 3600000;
            String sHours = hours == 0 ? null : new DecimalFormat("#0").format(hours); // NOI18N
            millis %= 3600000;
            
            // Minutes
            long minutes = millis / 60000;
            String sMinutes = hours == 0 && minutes == 0 ? null : new DecimalFormat(hours > 0 ? "00" : "#0").format(minutes); // NOI18N
            millis %= 60000;
            
            // Seconds
            String sSeconds = new DecimalFormat("#0.000").format(millis / 1000d); // NOI18N
            
            if (sMinutes == null) {
                 return NbBundle.getMessage(RecordingViewSupport.class, "FORMAT_s", // NOI18N
                                            new Object[] { sSeconds });
            } else if (sHours == null) {
                 return NbBundle.getMessage(RecordingViewSupport.class, "FORMAT_ms", // NOI18N
                                            new Object[] { sMinutes, sSeconds });
            } else {
                return NbBundle.getMessage(RecordingViewSupport.class, "FORMAT_hms", // NOI18N
                                            new Object[] { sHours, sMinutes, sSeconds });
            }
        }

    }
    
    
    static class SettingsSupport extends JPanel {
        
        private DataModel tableModel;
        private ProfilerTreeTable table;
        
        
        SettingsSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Settings", null, 10, this, null);
        }
        
        
        void setData(RecordingNode root) {
            tableModel.setRoot(root);
        }
        
        
        private void initComponents() {
            tableModel = new DataModel();
            table = new ProfilerTreeTable(tableModel, true, true, new int[] { 0 });
            
            table.setRootVisible(false);
            table.setShowsRootHandles(true);

            table.setMainColumn(0);
            table.setFitWidthColumn(0);

            table.setDefaultSortOrder(SortOrder.ASCENDING);
            table.setSortColumn(0);
            
            RecordingRenderers.NameRenderer nameRenderer = new RecordingRenderers.NameRenderer();
            table.setTreeCellRenderer(nameRenderer);
            
            RecordingRenderers.ValueRenderer valueRenderer = new RecordingRenderers.ValueRenderer();
            RecordingRenderers.TimeRenderer timeRenderer = new RecordingRenderers.TimeRenderer();
            int commonWidth = Math.max(valueRenderer.getPreferredWidth(), timeRenderer.getPreferredWidth());
            
            table.setColumnRenderer(1, valueRenderer);
            table.setDefaultColumnWidth(1, commonWidth);
            table.setColumnVisibility(1, RecordingRenderers.ValueRenderer.isInitiallyVisible());
            
            table.setColumnRenderer(2, timeRenderer);
            table.setDefaultColumnWidth(2, commonWidth);
            table.setColumnVisibility(2, RecordingRenderers.TimeRenderer.isInitiallyVisible());
            
            RecordingRenderers.ThreadRenderer threadRenderer = new RecordingRenderers.ThreadRenderer();
            table.setColumnRenderer(3, threadRenderer);
            table.setDefaultColumnWidth(3, threadRenderer.getPreferredWidth());
            table.setColumnVisibility(3, RecordingRenderers.ThreadRenderer.isInitiallyVisible());
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private static class DataModel extends ProfilerTreeTableModel.Abstract {
            
            DataModel() {
                super(new RecordingNode.Root("reading data...") {});
            }
            
            
            @Override
            public int getColumnCount() {
                return 4;
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0: return JTree.class;
                    case 1: return String.class;
                    case 2: return String.class;
                    case 3: return Long.class;
                    default: return null;
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0: return RecordingRenderers.NameRenderer.getDisplayName();
                    case 1: return RecordingRenderers.ValueRenderer.getDisplayName();
                    case 2: return RecordingRenderers.TimeRenderer.getDisplayName();
                    case 3: return RecordingRenderers.ThreadRenderer.getDisplayName();
                    default: return null;
                }
            }

            @Override
            public Object getValueAt(TreeNode node, int column) {
                if (node == null) return null;
                RecordingNode rnode = (RecordingNode)node;
                
                switch (column) {
                    case 0: return rnode;
                    case 1: return rnode.value;
                    case 2: return rnode.time;
                    case 3: return rnode.thread;
                    default: return null;
                }
            }
            
            @Override
            public void setValueAt(Object o, TreeNode node, int column) {}

            @Override
            public boolean isCellEditable(TreeNode node, int column) { return false; }
            
        }
        
    }
    
    
    static class RecordingsSupport extends JPanel implements JFREventVisitor {
        
        private Record[] records;
        private Set<Record> cache;
        
        private DataModel model;
        private ProfilerTable table;
        
        
        RecordingsSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Concurrent recordings", null, 10, this, null);
        }
        
        
        @Override
        public void init() {
            Record progress = new Record();
            progress.name = "reading data...";
            records = new Record[] { progress };
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { model.fireTableDataChanged(); }
            });
            
            cache = new HashSet();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.ActiveRecording".equals(typeName)) { // NOI18N
                try {
                    Record record = new Record();
                    record.name = event.getString("name"); // NOI18N
                    record.id = event.getLong("id"); // NOI18N
                    record.start = event.getLong("recordingStart"); // NOI18N
                    record.duration = event.getLong("recordingDuration"); // NOI18N
                    record.maxAge = event.getLong("maxAge"); // NOI18N
                    record.maxSize = event.getLong("maxSize"); // NOI18N
                    record.destination = event.getString("destination"); // NOI18N
                    if (record.destination == null) record.destination = "-";
    //                record.thread = event.getThread().getJavaName();

                    cache.add(record);
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }

        @Override
        public void done() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (cache.isEmpty())  {
                        Record noData = new Record();
                        noData.name = "<no recordings>";
                        cache.add(noData);
                    }
                    
                    records = cache.toArray(new Record[0]);
                    
                    model.fireTableDataChanged();
                    
                    cache.clear();
                    cache = null;
                }
            });
        }
        
        
        private void initComponents() {
            model = new DataModel();
            
            table = new ProfilerTable(model, true, true, null);
            
            table.setMainColumn(0);
            table.setFitWidthColumn(0);

            table.setDefaultSortOrder(SortOrder.ASCENDING);
            table.setSortColumn(1);
            
            RecordingRenderers.NameRenderer nameRenderer = new RecordingRenderers.NameRenderer();
            table.setColumnRenderer(0, nameRenderer);
            
            RecordingRenderers.IdRenderer idRenderer = new RecordingRenderers.IdRenderer();
            table.setColumnRenderer(1, idRenderer);
            table.setDefaultColumnWidth(1, idRenderer.getPreferredWidth());
            table.setColumnVisibility(1, RecordingRenderers.IdRenderer.isInitiallyVisible());
            
            RecordingRenderers.StartRenderer startRenderer = new RecordingRenderers.StartRenderer();
            table.setColumnRenderer(2, startRenderer);
            table.setDefaultColumnWidth(2, startRenderer.getPreferredWidth());
            table.setColumnVisibility(2, RecordingRenderers.TimeRenderer.isInitiallyVisible());
            
            RecordingRenderers.DurationRenderer durationRenderer = new RecordingRenderers.DurationRenderer();
            RecordingRenderers.SizeRenderer sizeRenderer = new RecordingRenderers.SizeRenderer();
            RecordingRenderers.AgeRenderer ageRenderer = new RecordingRenderers.AgeRenderer();
            int commonWidth = Math.max(durationRenderer.getPreferredWidth(), sizeRenderer.getPreferredWidth());
            commonWidth = Math.max(commonWidth, ageRenderer.getPreferredWidth());
            table.setColumnRenderer(3, durationRenderer);
            table.setDefaultColumnWidth(3, commonWidth);
            table.setColumnVisibility(3, RecordingRenderers.DurationRenderer.isInitiallyVisible());
            
            table.setColumnRenderer(4, sizeRenderer);
            table.setDefaultColumnWidth(4, commonWidth);
            table.setColumnVisibility(4, RecordingRenderers.SizeRenderer.isInitiallyVisible());
            
            table.setColumnRenderer(5, ageRenderer);
            table.setDefaultColumnWidth(5, commonWidth);
            table.setColumnVisibility(5, RecordingRenderers.AgeRenderer.isInitiallyVisible());
            
            RecordingRenderers.DestinationRenderer destinationRenderer = new RecordingRenderers.DestinationRenderer();
            table.setColumnRenderer(6, destinationRenderer);
            table.setDefaultColumnWidth(6, destinationRenderer.getPreferredWidth());
            table.setColumnVisibility(6, RecordingRenderers.DestinationRenderer.isInitiallyVisible());
            
//            RecordingRenderers.ThreadRenderer threadRenderer = new RecordingRenderers.ThreadRenderer();
//            table.setColumnRenderer(7, threadRenderer);
//            table.setDefaultColumnWidth(7, threadRenderer.getPreferredWidth());
//            table.setColumnVisibility(7, RecordingRenderers.ThreadRenderer.isInitiallyVisible());
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private static class Record {
            
            String name;
            long id = -1;
            long start = -1;
            long duration = -1;
            long maxSize = -1;
            long maxAge = -1;
            String destination;
//            String thread;
            
            
            @Override
            public int hashCode() {
                return Objects.hash(name, id, start, duration, maxSize, maxAge, destination /*, thread*/);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null) return false;
                if (!(o instanceof Record)) return false;
                
                final Record r = (Record) o;
                if (id != r.id) return false;
                if (start != r.start) return false;
                if (duration != r.duration) return false;
                if (maxSize != r.maxSize) return false;
                if (maxAge != r.maxAge) return false;
                if (!Objects.equals(name, r.name)) return false;
                if (!Objects.equals(destination, r.destination)) return false;
//                if (!Objects.equals(thread, r.thread)) return false;
                
                return true;
            }
            
        }
        
        
        private class DataModel extends AbstractTableModel {
            
            @Override
            public int getColumnCount() {
                return 7;
            }
            
            @Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return RecordingRenderers.NameRenderer.getDisplayName();
                    case 1: return RecordingRenderers.IdRenderer.getDisplayName();
                    case 2: return RecordingRenderers.StartRenderer.getDisplayName();
                    case 3: return RecordingRenderers.DurationRenderer.getDisplayName();
                    case 4: return RecordingRenderers.SizeRenderer.getDisplayName();
                    case 5: return RecordingRenderers.AgeRenderer.getDisplayName();
                    case 6: return RecordingRenderers.DestinationRenderer.getDisplayName();
//                    case 7: return RecordingRenderers.ThreadRenderer.getDisplayName();
                    default: return null;
                }
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return String.class;
                    case 1: return Long.class;
                    case 2: return Long.class;
                    case 3: return Long.class;
                    case 4: return Long.class;
                    case 5: return Long.class;
                    case 6: return String.class;
//                    case 7: return String.class;
                    default: return null;
                }
            }

            @Override
            public int getRowCount() {
                return records == null ? 0 : records.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0: return records[rowIndex].name;
                    case 1: return records[rowIndex].id;
                    case 2: return records[rowIndex].start;
                    case 3: return records[rowIndex].duration;
                    case 4: return records[rowIndex].maxSize;
                    case 5: return records[rowIndex].maxAge;
                    case 6: return records[rowIndex].destination;
//                    case 7: return records[rowIndex].thread;
                    default: return null;
                }
            }
            
        }
        
    }
    
}

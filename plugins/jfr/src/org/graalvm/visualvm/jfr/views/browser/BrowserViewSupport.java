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
package org.graalvm.visualvm.jfr.views.browser;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
final class BrowserViewSupport {
    
    static DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    static NumberFormat LONG_FORMAT = NumberFormat.getNumberInstance();
    
    
    static abstract class MasterViewSupport extends JPanel implements JFREventVisitor {

        MasterViewSupport() {
            initComponents();
        }
        
        
        abstract void firstShown();
        
        abstract void reloadEvents();
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            return false;
        }

        @Override
        public void done() {
        }
        

        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Browser", null, this);  // NOI18N
        }

        private void initComponents() {
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
    
    
    static abstract class EventsTreeViewSupport extends JPanel implements JFREventTypeVisitor {
        
        private Map<String, JFREventType> types;
        
        private DataModel tableModel;
        private ProfilerTreeTable table;
        

        EventsTreeViewSupport(long eventsCount) {
            initComponents(eventsCount);
        }
        
        
        abstract void reloadEvents(JFREventVisitor visitor);
        
        abstract void eventsSelected(String eventType, long eventsCount, List<String> valueNames);
        
        
        public void initTypes() {
            types = new HashMap();
        }
    
        public boolean visitType(String typeName, JFREventType eventType) {
//            System.err.println(">>> TYPE " + typeName + " - values " + eventType.getValueNames());
            types.put(typeName, eventType);
            return false;
        }
        
        
        JFREventVisitor getVisitor() {
            return new BrowserNode.Root() {
                @Override
                public void done() {
                    super.done();
                    tableModel.setRoot(this);
                    initialExpand(table, this);
                }
                @Override
                void reloadEvents(JFREventVisitor visitor) {
                    EventsTreeViewSupport.this.reloadEvents(visitor);
                }
                @Override
                JFREventType type(String typeName) {
                    return types.get(typeName);
                }
            };
        }
        

        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Types", null, 10, this, null);  // NOI18N
        }

        
        private void initComponents(long eventsCount) {
            tableModel = new DataModel();
            table = new ProfilerTreeTable(tableModel, true, true, new int[] { 0 });
            
            table.setRootVisible(false);
            table.setShowsRootHandles(true);

            table.setMainColumn(0);
            table.setFitWidthColumn(0);

            table.setDefaultSortOrder(SortOrder.ASCENDING);
            table.setDefaultSortOrder(2, SortOrder.DESCENDING);
            table.setSortColumn(0);
            
            final BrowserRenderers.NameRenderer nameRenderer = new BrowserRenderers.NameRenderer();
            nameRenderer.setShowsCount(!BrowserRenderers.EventsCountRenderer.isInitiallyVisible());
            table.setTreeCellRenderer(nameRenderer);
            
            BrowserRenderers.TypeIDRenderer typeIDRenderer = new BrowserRenderers.TypeIDRenderer();
            table.setColumnRenderer(1, typeIDRenderer);
            table.setDefaultColumnWidth(1, typeIDRenderer.getPreferredWidth());
            table.setColumnVisibility(1, BrowserRenderers.TypeIDRenderer.isInitiallyVisible());
            
            BrowserRenderers.EventsCountRenderer eventsCountRenderer = new BrowserRenderers.EventsCountRenderer();
            HideableBarRenderer eventsCountRendererH = new HideableBarRenderer(eventsCountRenderer, eventsCountRenderer.getPreferredWidth());
            eventsCountRendererH.setMaxValue(eventsCount);
            table.setColumnRenderer(2, eventsCountRendererH);
            table.setDefaultColumnWidth(2, eventsCountRendererH.getOptimalWidth());
            table.setColumnVisibility(2, BrowserRenderers.EventsCountRenderer.isInitiallyVisible());
            
            table.getColumnModel().getColumn(2).addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("maxWidth".equals(evt.getPropertyName())) { // NOI18N
                        nameRenderer.setShowsCount(Integer.valueOf(0).equals(evt.getNewValue()));
                        table.repaint();
                    }
                }
            });

            table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        BrowserNode node = getSelectedNode();
                        BrowserNode.EventType typeNode = node instanceof BrowserNode.EventType ? (BrowserNode.EventType)node : null;
                        if (typeNode == null) eventsSelected(null, -1, null);
                        else eventsSelected(typeNode.typeName, typeNode.eventsCount, typeNode.type.getDisplayableValueNames());
                    }
                }
                private BrowserNode getSelectedNode() {
                    int row = table.getSelectedRow();
                    return row == -1 ? null : (BrowserNode)table.getValueForRow(row);
                }
            });
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private static void initialExpand(ProfilerTreeTable table, BrowserNode root) {
            TreePath path = new TreePath(root);
            for (BrowserNode node : root.getChildren())
                table.expandPath(path.pathByAddingChild(node));
        }
        
        
        private static class DataModel extends ProfilerTreeTableModel.Abstract {
            
            DataModel() {
                super(new BrowserNode.Root("reading data...") {});
            }
            
            
            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0: return JTree.class;
                    case 1: return String.class;
                    case 2: return Long.class;
                    default: return null;
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0: return BrowserRenderers.NameRenderer.getDisplayName();
                    case 1: return BrowserRenderers.TypeIDRenderer.getDisplayName();
                    case 2: return BrowserRenderers.EventsCountRenderer.getDisplayName();
                    default: return null;
                }
            }

            @Override
            public Object getValueAt(TreeNode node, int column) {
                if (node == null) return null;
                BrowserNode rnode = (BrowserNode)node;
                
                switch (column) {
                    case 0: return rnode;
                    case 1: return rnode instanceof BrowserNode.EventType && ((BrowserNode.EventType)rnode).type != null ? ((BrowserNode.EventType)rnode).type.getName() : null;
                    case 2: return rnode.eventsCount == 0 ? null : rnode.eventsCount;
                    default: return null;
                }
            }
            
            @Override
            public void setValueAt(Object o, TreeNode node, int column) {}

            @Override
            public boolean isCellEditable(TreeNode node, int column) { return false; }
            
        }

    }
    
    
    static abstract class EventsTableViewSupport extends JPanel {
        
        private final List<String> names;
        private final List<List<String>> values;
//        private final List<JFREvent> events;
        
        private EventsTableModel tableModel;
        private ProfilerTable table;
        
        
        EventsTableViewSupport() {
            names = new ArrayList();
//            events = new ArrayList();
            values = new ArrayList();
            
            initComponents();
        }
        
        
        JFREventVisitor getVisitor(final String eventType, final long eventsCount, final List<String> valueNames) {
            final List<List<String>> newValues = new ArrayList();
            
            return new JFREventVisitor() {
                @Override
                public void init() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            values.clear();
                            values.add(null);
                            tableModel.fireTableStructureChanged();
                        }
                    });
                }
                
                @Override
                public boolean visit(String typeName, JFREvent event) {
                    if (eventType == null) return true;
                    
                    if (eventType.equals(typeName)) newValues.add(event.getDisplayableValues());
                    
                    return newValues.size() == eventsCount;
                }
                
                @Override
                public void done() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            names.clear();
                            if (valueNames != null) names.addAll(valueNames);
                            values.clear();
                            values.addAll(newValues);
                            newValues.clear();
                            tableModel.fireTableStructureChanged();
                        }
                    });
                }
            };
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Events", null, 10, this, null);  // NOI18N
        }
        
        
        private void initComponents() {
            tableModel = new EventsTableModel();
            table = new ProfilerTable(tableModel, true, true, null);

//            table.setMainColumn(0);
            table.setFitWidthColumn(-1);
//
//            table.setSortColumn(1);
//            table.setDefaultSortOrder(SortOrder.DESCENDING);
//            table.setDefaultSortOrder(0, SortOrder.ASCENDING);

//            renderers = new HideableBarRenderer[2];
//            renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
//            renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
//            
//            JavaNameRenderer classRenderer = new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS));
//            
            table.setDefaultRenderer(String.class, new LabelRenderer());
//            table.setColumnRenderer(0, new LabelRenderer());
//            table.setColumnRenderer(1, renderers[0]);
//            table.setColumnRenderer(2, renderers[1]);
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private class EventsTableModel extends AbstractTableModel {
            
            private DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        
            public String getColumnName(int columnIndex) {
                return names.isEmpty() ? "" : names.get(columnIndex);
//                if (columnIndex == 0) {
//                    return "Time";
//                } else if (columnIndex == 1) {
//                    return "Duration";
////                } else if (columnIndex == 2) {
////                    return "Objects";
//                }
//
//                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
//                if (columnIndex == 0) {
//                    return String.class;
//                } else {
//                    return String.class;
//                }
            }

            public int getRowCount() {
                return values.size();
//                return names == null ? 0 : names.length;
            }

            public int getColumnCount() {
                return names.isEmpty() ? 1 : names.size();
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                List<String> row = values.get(rowIndex);
                if (row == null) return columnIndex == 0 ? "loading events..." : "";
                else return row.get(columnIndex);
//                if (columnIndex == 0) {
//                    JFREvent event = events.get(rowIndex);
//                    try {
//                        return event == null ? "loading events..." : TIME_FORMAT.format(event.getInstant("eventTime").toEpochMilli());
//                    } catch (JFRPropertyNotAvailableException ex) {
//                        return null;
//                    }
//                } else if (columnIndex == 1) {
//                    JFREvent event = events.get(rowIndex);
//                    try {
//                        return event == null ? "loading events..." : event.getDuration("eventDuration").toMillis();
//                    } catch (JFRPropertyNotAvailableException ex) {
//                        return null;
//                    }
//                }

//                return null;
            }

        }
        
    }
    
}

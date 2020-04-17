/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.jfr.model.JFRDataDescriptor;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRMethod;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackFrame;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.jfluid.utils.formatting.DefaultMethodNameFormatter;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.graalvm.visualvm.lib.ui.swing.renderer.FormattedLabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class BrowserViewSupport {
    
    private static final int ITEMS_LIMIT = Integer.getInteger("jfrviewer.browserItemsLimit", 100); // NOI18N
    private static final String ITEMS_LIMIT_STR = NumberFormat.getInstance().format(ITEMS_LIMIT);
    
    static enum EventsFilter {
        ALL { @Override public String toString() { return "All Events"; } },
        FIRST_N { @Override public String toString() { return "First " + ITEMS_LIMIT_STR + " Events"; } },
        MIDDLE_N { @Override public String toString() { return "Middle " + ITEMS_LIMIT_STR + " Events"; } },
        LAST_N { @Override public String toString() { return "Last " + ITEMS_LIMIT_STR + " Events"; } },
        SAMPLE_N { @Override public String toString() { return "Sample " + ITEMS_LIMIT_STR + " Events"; } },
    };
    
    static abstract class MasterViewSupport extends JPanel implements JFREventVisitor {
        
        private EventsFilter lastPrimary;
        private boolean lastExperimental;
        

        MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        abstract void firstShown();
        
        abstract void eventsFilterChanged(EventsFilter newFilter);
        
        abstract void includeExperimentalChanged(boolean newExperimental);
        
        
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
        
        
        private void updateFilterHint() {
            thirdLabel.setVisible(!EventsFilter.ALL.equals(lastPrimary) && !EventsFilter.SAMPLE_N.equals(lastPrimary));
        }
        
        private void updateUpdateButton() {
            if (updateButton != null) updateButton.setEnabled(lastExperimental != (secondChoice == null ? false : secondChoice.isSelected()));
        }
        
        
        private int prefHeight = -1;
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            if (prefHeight == -1) prefHeight = pref.height;
            else pref.height = prefHeight;
            return pref;
        }
        

        private void initComponents(JFRModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (model == null) {
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(11, 5, 20, 5));

                GridBagConstraints constraints;

                // firstLabel
                firstLabel = new JLabel();
                firstLabel.setText("Display:");
                firstLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(firstLabel, constraints);

                // firstCombo
                firstCombo = new JComboBox(new Object[] { EventsFilter.ALL, EventsFilter.FIRST_N, EventsFilter.MIDDLE_N, EventsFilter.LAST_N, EventsFilter.SAMPLE_N });
                firstCombo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (lastPrimary != firstCombo.getSelectedItem()) {
                            lastPrimary = (EventsFilter)firstCombo.getSelectedItem();
                            updateFilterHint();
                            eventsFilterChanged(lastPrimary);
                        }
                    }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 1;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(firstCombo, constraints);
                
                // secondLabel
                secondLabel = new JLabel();
                secondLabel.setText("of the selected type");
                secondLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 2;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(secondLabel, constraints);
                
                // thirdLabel
                thirdLabel = new JLabel();
                thirdLabel.setText("(by position in the file)");
                thirdLabel.setToolTipText("<html>Events might not be ordered in the file by their creation time.<br>First/Middle/Last means position in the file, not the creation time.</html>");
                thirdLabel.setOpaque(false);
                thirdLabel.setEnabled(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 3;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 5, 0, 0);
                add(thirdLabel, constraints);
                
                lastPrimary = (EventsFilter)firstCombo.getSelectedItem();
                updateFilterHint();
                eventsFilterChanged(lastPrimary);

                if (model.getExperimentalEventsCount() > 0) {
                    // updateSeparator
                    JSeparator updateSeparator = new JSeparator(JSeparator.VERTICAL);
                    updateSeparator.setOpaque(false);
                    constraints = new GridBagConstraints();
                    constraints.gridx = 4;
                    constraints.gridy = 2;
                    constraints.gridwidth = 1;
                    constraints.fill = GridBagConstraints.NONE;
                    constraints.anchor = GridBagConstraints.WEST;
                    constraints.insets = new Insets(4, secondChoice == null ? 16 : 12, 0, 0);
                    add(updateSeparator, constraints);
                    
                    secondChoice = new JCheckBox("Display experimental items");
                    secondChoice.setOpaque(false);
                    secondChoice.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) { updateUpdateButton(); }
                    });
                    constraints = new GridBagConstraints();
                    constraints.gridx = 5;
                    constraints.gridy = 2;
                    constraints.gridwidth = 1;
                    constraints.fill = GridBagConstraints.NONE;
                    constraints.anchor = GridBagConstraints.WEST;
                    constraints.insets = new Insets(4, 10, 0, 0);
                    add(secondChoice, constraints);
                    
                    // updateButton
                    updateButton = new JButton("Update Data");
                    updateButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            updateButton.setEnabled(false);
                            
                            if (lastExperimental != secondChoice.isSelected()) {
                                lastExperimental = secondChoice.isSelected();
                                includeExperimentalChanged(lastExperimental);
                            }
                        }
                    });
                    constraints = new GridBagConstraints();
                    constraints.gridx = 6;
                    constraints.gridy = 2;
                    constraints.gridwidth = 1;
                    constraints.fill = GridBagConstraints.NONE;
                    constraints.anchor = GridBagConstraints.WEST;
                    constraints.insets = new Insets(4, 12, 0, 0);
                    add(updateButton, constraints);
                    
                    Dimension cpuD = firstCombo.getPreferredSize();
                    Dimension sepD = updateSeparator.getPreferredSize();
                    sepD.height = cpuD.height - 2;
                    sepD.width = 5;
                    updateSeparator.setPreferredSize(sepD);
                    updateSeparator.setMinimumSize(sepD);
                    
                    lastExperimental = secondChoice == null ? false : secondChoice.isSelected();
                    updateUpdateButton();
                }

                // filler1
                constraints = new GridBagConstraints();
                constraints.gridx = 7;
                constraints.gridy = 2;
                constraints.weightx = 1;
                constraints.weighty = 1;
                constraints.gridwidth = GridBagConstraints.REMAINDER;
                constraints.fill = GridBagConstraints.BOTH;
                constraints.anchor = GridBagConstraints.NORTHWEST;
                constraints.insets = new Insets(0, 0, 0, 0);
                add(Spacer.create(), constraints);
                
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
        
        private JLabel firstLabel;
        private JLabel secondLabel;
        private JLabel thirdLabel;
        private JComboBox firstCombo;
        private JCheckBox secondChoice;
        private JButton updateButton;

    }
    
    
    static abstract class EventsTreeViewSupport extends JPanel implements JFREventTypeVisitor {
        
        private boolean includeExperimental;
        
        private Map<String, JFREventType> types;
        
        private DataModel tableModel;
        private ProfilerTreeTable table;
        
        private boolean selectionPaused = false;
        

        EventsTreeViewSupport(long eventsCount) {
            initComponents(eventsCount);
        }
        
        
        abstract void eventsSelected(String eventType, long eventsCount, List<JFRDataDescriptor> dataDescriptors);
        
        
        void pauseSelection() {
            selectionPaused = true;
        }
        
        void refreshSelection() {
            if (selectionPaused) return;
            
            BrowserNode node = getSelectedNode();
            BrowserNode.EventType typeNode = node instanceof BrowserNode.EventType ? (BrowserNode.EventType)node : null;
            if (typeNode == null) eventsSelected(null, -1, null);
            else eventsSelected(typeNode.typeName, typeNode.eventsCount, typeNode.type.getDisplayableDataDescriptors(includeExperimental));
        }
        
        
        void setIncludeExperimental(boolean experimental) {
            includeExperimental = experimental;
        }
        
        
        public void initTypes() {
            types = new HashMap();
        }
    
        public boolean visitType(String typeName, JFREventType eventType) {
            if (includeExperimental || !eventType.isExperimental()) types.put(typeName, eventType);
            return false;
        }
        
        
        JFREventVisitor getVisitor() {
            return new BrowserNode.Root() {
                @Override
                public void done() {
                    super.done();
                    
                    tableModel.setRoot(this);
                    initialExpand(table, this);
                    
                    selectionPaused = false;
                    refreshSelection();
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
                    if (!e.getValueIsAdjusting()) refreshSelection();
                }
            });
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        private BrowserNode getSelectedNode() {
            int row = table.getSelectedRow();
            return row == -1 ? null : (BrowserNode)table.getValueForRow(row);
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
        
        private EventsFilter eventsFilter;
        private boolean includeExperimental;
        
        private String[] names;
        private Comparable[][] values;
        private long[] ids;
        
        private EventsTableModel tableModel;
        private ProfilerTable table;
        
        
        EventsTableViewSupport() {
            initComponents();
        }
        
        
        abstract void idSelected(long id);
        
        
        void setEventsFilter(EventsFilter filter) {
            eventsFilter = filter;
        }
        
        void setIncludeExperimental(boolean experimental) {
            includeExperimental = experimental;
        }
        
        
        JFREventVisitor getVisitor(final String eventType, final long eventsCount, final List<JFRDataDescriptor> dataDescriptors) {
            return new JFREventVisitor() {
                private final EventsFilter filter;
                
                private final int totalEvents;
                private final int displayedEvents;
                
                private final Comparable[][] newValues;
                private final long[] newIds;

                private final Comparable[] COMPARABLE_ARR = new Comparable[0];
                
                private int dataIndex;
                private int eventIndex;
                private int startIndex;
                
                private double nextIndex;
                private double step;
                
                {
                    totalEvents = eventsCount == -1 ? 0 : (int)Math.min(eventsCount, Integer.MAX_VALUE); // NOTE: won't display more than Integer.MAX_VALUE events!
                    
                    filter = totalEvents <= ITEMS_LIMIT ? EventsFilter.ALL : eventsFilter;
                    displayedEvents = EventsFilter.ALL.equals(filter) ? totalEvents : Math.min(totalEvents, ITEMS_LIMIT);
                
                    newValues = dataDescriptors == null ? null : new Comparable[displayedEvents][dataDescriptors.size()];
                    newIds = totalEvents == 0 ? null : new long[displayedEvents];
                }
            
                @Override
                public void init() {
                    dataIndex = 0;
                    eventIndex = -1; // incremented before first access
                    
                    switch (filter) {
                        case ALL: // no filtering
                        case FIRST_N: // covered by the return condition
                            startIndex = 0;
                            break;
                        case MIDDLE_N:
                            if (totalEvents <= ITEMS_LIMIT + 1) startIndex = 0;
                            else startIndex = (totalEvents - ITEMS_LIMIT) / 2;
                            break;
                        case LAST_N:
                            if (totalEvents <= ITEMS_LIMIT) startIndex = 0;
                            else startIndex = totalEvents - ITEMS_LIMIT;
                            break;
                        case SAMPLE_N:
                            nextIndex = 0;
                            step = (totalEvents - 1) / (double)(ITEMS_LIMIT - 1);
                            break;
                    }
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            names = null;
                            values = dataDescriptors == null ? null : new Comparable[][] { null };
                            tableModel.fireTableStructureChanged();
                        }
                    });
                }
                
                @Override
                public boolean visit(String typeName, JFREvent event) {
                    if (eventType == null) return true;
                    
                    if (eventType.equals(typeName)) {
                        eventIndex++;
                        
                        switch (filter) {
                            case ALL: // no filtering
                            case FIRST_N: // covered by the return condition
                                break;
                            case MIDDLE_N:
                            case LAST_N:
                                if (eventIndex < startIndex) return false;
                                break;
                            case SAMPLE_N:
                                if (eventIndex == Math.round(nextIndex) || eventIndex == totalEvents - 1) nextIndex += step; // extra check for last item (might miss last event due to rounding bias)
                                else return false;
                                break;
                        }
                        
                        newIds[dataIndex] = event.getID();
                        newValues[dataIndex++] = event.getDisplayableValues(includeExperimental).toArray(COMPARABLE_ARR);
                    }
                    
                    return dataIndex == displayedEvents;
                }
                
                @Override
                public void done() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            List<String> tooltips = new ArrayList();
                            List<ProfilerRenderer> renderers = new ArrayList();
                            
                            if (dataDescriptors != null) {
                                int namesIndex = 0;
                                names = new String[dataDescriptors.size()];
                                for (JFRDataDescriptor descriptor : dataDescriptors) {
                                    String dataName = descriptor.getDataName();
                                    names[namesIndex++] = dataName;
                                    
                                    String dataDescription = descriptor.getDataDescription();
                                    tooltips.add(dataDescription != null && !dataDescription.isEmpty() ? dataDescription : dataName);
                                    
                                    Format format = descriptor.getDataFormat();
                                    LabelRenderer renderer = format == null ? new LabelRenderer() : new FormattedLabelRenderer(format);
                                    if (descriptor.isNumericData()) renderer.setHorizontalAlignment(LabelRenderer.TRAILING);
                                    renderers.add(renderer);
                                }
                            }
                            
                            values = newValues;
                            ids = newIds;
                            tableModel.fireTableStructureChanged();
                            
                            table.setSortColumn(0);
                            
                            table.setColumnToolTips(tooltips.toArray(new String[0]));
                            
                            for (int column = 0; column < renderers.size(); column++)
                                table.setColumnRenderer(column, renderers.get(column));
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

            table.setFitWidthColumn(-1);
            table.setSorting(0, SortOrder.UNSORTED);
            table.setDefaultSortOrder(SortOrder.ASCENDING);
            table.setDefaultRenderer(Comparable.class, new LabelRenderer());
            
            table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int selected = table.getSelectedRow();
                        if (selected == 0 && values[0] == null) selected = -1; // "loading events..."
                        idSelected(selected == -1 ? -1 : ids[table.convertRowIndexToModel(selected)]);
                    }
                }
            });
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private class EventsTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                return names == null ? " " : names[columnIndex];
            }

            public Class<?> getColumnClass(int columnIndex) {
                return Comparable.class;
            }

            public int getRowCount() {
                return values == null ? 0 : values.length;
            }

            public int getColumnCount() {
                return names == null ? 1 : names.length;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                Comparable[] row = values[rowIndex];
                if (row == null) return columnIndex == 0 ? "loading events..." : "";
                else return row[columnIndex];
            }

        }
        
    }
    
    
    static abstract class StackTraceViewSupport extends JPanel {
        
        private static final RequestProcessor PROCESSOR = new RequestProcessor("JFR StackTrace Processor"); // NOI18N
        
        private static final DefaultMethodNameFormatter METHOD_FORMAT = new DefaultMethodNameFormatter();
        
        private HTMLTextArea area;
        
        private boolean showing; // accessed in EDT only
        private long pendingID = -1; // accessed in EDT only
        
        private long currentID = Long.MIN_VALUE; // accessed in EDT only
        private RequestProcessor.Task currentTask; // accessed in EDT only
        
        
        StackTraceViewSupport() {
            initComponents();
            
            addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        showing = StackTraceViewSupport.this.isShowing();
                        if (showing && pendingID != Long.MIN_VALUE) {
                            idSelected(pendingID);
                            pendingID = Long.MIN_VALUE;
                        }
                    }
                }
            });
        }
        
        abstract JFREvent getEvent(long id);
        
        // invoked in EDT
        void idSelected(final long id) {
            if (!showing) {
                pendingID = id;
                return;
            }
            
            if (id == currentID) return;
            
            if (id == -1) {
                currentID = -1;
                setText("<nobr>&lt;no event selected&gt;</nobr>");
            } else {
                currentID = id;
                setText("<nobr>&lt;reading stack trace...&gt;</nobr>");
                
                if (currentTask != null) currentTask.cancel();
                
                currentTask = PROCESSOR.post(new Runnable() {
                    public void run() {
                        JFREvent event = getEvent(id);
                        
                        JFRStackTrace stack;
                        try {
                            stack = event.getStackTrace("eventStackTrace"); // NOI18N
                        } catch (JFRPropertyNotAvailableException e) {
                            stack = null;
                        }
                        
                        final String stackTrace = formatStackTrace(stack);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (currentID == id) {
                                    setText(stackTrace);
                                    currentTask = null;
                                }
                            }
                        });
                    }
                });
            }
        }
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Stack Trace", null, 10, this, null);  // NOI18N
        }
        
        private static String formatStackTrace(JFRStackTrace stack) {
            if (stack == null) return "<nobr>&lt;no stack trace&gt;</nobr>";
            
            StringBuilder sb = new StringBuilder();
            
            String header = "<nobr><code>"; // NOI18N
            sb.append(header);
            
            for (JFRStackFrame frame : stack.getFrames())
                sb.append(formatStackFrame(frame)).append("<br>"); // NOI18N
            
            if (sb.length() == header.length()) return "<nobr>&lt;empty stack trace&gt;</nobr>";
            
            if (stack.isTruncated()) sb.append("&lt;...truncated...&gt;").append("<br>");
            
            sb.append("</code></nobr>"); // NOI18N
            
            return sb.toString();
        }
        
        private static String formatStackFrame(JFRStackFrame frame) {
            JFRMethod method = frame.getMethod();
            int line = frame.getLine();
            int bci = frame.getBCI();
            String type = frame.getType();
            
            String fullName = METHOD_FORMAT.formatMethodName(method.getType().getName(), method.getName(), method.getDescriptor()).toFormatted();
            int idx = fullName.indexOf(" : "); // NOI18N
            String methodName = idx == -1 ? fullName : fullName.substring(0, idx);
//            String returnName = fullName.substring(idx);

            String ret = methodName;
            if (line != -1) ret += ":" + line; // NOI18N
            ret += "; "; // NOI18N
            if (bci > 0) ret += "bci=" + bci + ", "; // NOI18N
            ret += type;
            
            return ret.replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
        }
        
        private void setText(String text) {
            area.setText(text);
            area.setCaretPosition(0);
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea();
            area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
    
}

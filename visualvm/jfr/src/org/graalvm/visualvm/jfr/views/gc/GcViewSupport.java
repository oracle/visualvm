/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.gc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.time.Duration;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.components.HTMLLabel;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;

/**
 *
 * @author Jiri Sedlacek
 */
final class GcViewSupport {
    
    static enum Aggregation {
        NONE { @Override public String toString() { return "None"; } },
        NAME { @Override public String toString() { return "Name"; } },
        CAUSE { @Override public String toString() { return "Cause"; } },
        PHASE { @Override public String toString() { return "GC Phase"; } },
    }

    static abstract class MasterViewSupport extends JPanel {
        
        private Aggregation lastPrimary/*, lastSecondary*/;
        private boolean lastPhase;
        
        
        MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("GC", null, this);
        }
        
        
        abstract void firstShown();
        
        abstract void changeAggregation(Aggregation primary, Aggregation secondary);
        
        
        void showProgress() {
            updateButton.setEnabled(false);
            updateButton.setVisible(false);
            statusValueLabel.setVisible(true);
        }
        
        void hideProgress() {
            statusValueLabel.setVisible(false);
            updateButton.setVisible(true);
        }
        
        
        private void handleAggregationChanged(boolean updateSecondary) {
            if (updateSecondary) {
                DefaultComboBoxModel<Aggregation> model = (DefaultComboBoxModel<Aggregation>)secondCombo.getModel();
                while (model.getSize() > 1) model.removeElementAt(1);
                
//                if (!Aggregation.CLASS.equals(firstCombo.getSelectedItem()) &&
//                    !Aggregation.CLASS_MESSAGE.equals(firstCombo.getSelectedItem()))
//                        model.addElement(Aggregation.CLASS);
//                
//                if (!Aggregation.MESSAGE.equals(firstCombo.getSelectedItem()) &&
//                    !Aggregation.CLASS_MESSAGE.equals(firstCombo.getSelectedItem()))
//                        model.addElement(Aggregation.MESSAGE);
//                
//                if (!Aggregation.CLASS.equals(firstCombo.getSelectedItem()) &&
//                    !Aggregation.MESSAGE.equals(firstCombo.getSelectedItem()) &&
//                    !Aggregation.CLASS_MESSAGE.equals(firstCombo.getSelectedItem()))
//                        model.addElement(Aggregation.CLASS_MESSAGE);
//                
//                if (!Aggregation.THREAD.equals(firstCombo.getSelectedItem()))
//                    model.addElement(Aggregation.THREAD);
            }
            
            updateButton.setEnabled(lastPrimary != firstCombo.getSelectedItem() ||
                                    lastPhase != secondChoice.isSelected());
            
        }
        
        
        private int prefHeight = -1;
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            if (prefHeight == -1) prefHeight = pref.height;
            else pref.height = prefHeight;
            return pref;
        }
        
        
        private void initComponents(JFRModel model) {
            setOpaque(false);
            
            if (model == null) {
                setLayout(new BorderLayout());
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else if (!model.containsEvent(JFRSnapshotGcViewProvider.EventChecker.class)) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("GC", JFRSnapshotGcViewProvider.EventChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(11, 5, 20, 5));

                GridBagConstraints constraints;

                // modeLabel
                firstLabel = new JLabel();
                firstLabel.setText("Aggregation:");
                firstLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(firstLabel, constraints);

                // cpuButton
                firstCombo = new JComboBox<>(new Aggregation[] { Aggregation.NONE, Aggregation.NAME, Aggregation.CAUSE });
                firstCombo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(true); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 1;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(firstCombo, constraints);

                // modeLabel
//                secondLabel = new JLabel();
//                secondLabel.setText("secondary:");
//                secondLabel.setOpaque(false);
//                constraints = new GridBagConstraints();
//                constraints.gridx = 2;
//                constraints.gridy = 2;
//                constraints.gridwidth = 1;
//                constraints.fill = GridBagConstraints.NONE;
//                constraints.anchor = GridBagConstraints.WEST;
//                constraints.insets = new Insets(4, 12, 0, 0);
//                add(secondLabel, constraints);
//
//                // memoryButton
                secondCombo = new JComboBox<>(new Aggregation[] { Aggregation.NONE, Aggregation.NAME, Aggregation.CAUSE });
//                secondCombo.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(false); }
//                });
//                constraints = new GridBagConstraints();
//                constraints.gridx = 3;
//                constraints.gridy = 2;
//                constraints.gridwidth = 1;
//                constraints.fill = GridBagConstraints.NONE;
//                constraints.anchor = GridBagConstraints.WEST;
//                constraints.insets = new Insets(4, 8, 0, 0);
//                add(secondCombo, constraints);

                // pause levels choice
                boolean hasPhases = model.containsEvent(JFRSnapshotGcViewProvider.EventChecker_Phases.class);
                secondChoice = new JCheckBox(hasPhases ? "Show GC Phases" : "Show GC Phases (no phases recorded)");
                secondChoice.setOpaque(false);
                secondChoice.setEnabled(hasPhases);
                secondChoice.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(false); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 3;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 12, 0, 0);
                add(secondChoice, constraints);
                
                lastPrimary = (Aggregation)firstCombo.getSelectedItem();
                lastPhase = secondChoice.isSelected();
//                lastSecondary = (Aggregation)secondCombo.getSelectedItem();

                // updateSeparator
                JSeparator updateSeparator = new JSeparator(JSeparator.VERTICAL);
                updateSeparator.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 4;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 12, 0, 0);
                add(updateSeparator, constraints);

                // updateButton
                updateButton = new JButton("Update Data");
                updateButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        updateButton.setEnabled(false);
                        lastPrimary = (Aggregation)firstCombo.getSelectedItem();
                        lastPhase = secondChoice.isSelected();
//                        lastSecondary = (Aggregation)secondCombo.getSelectedItem();
//                        changeAggregation((Aggregation)firstCombo.getSelectedItem(), (Aggregation)secondCombo.getSelectedItem());
                        changeAggregation((Aggregation)firstCombo.getSelectedItem(), secondChoice.isSelected() ? Aggregation.PHASE : Aggregation.NONE);
                    }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 5;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 12, 0, 0);
                add(updateButton, constraints);

                // statusValueLabel
                statusValueLabel = new HTMLLabel("<nobr><b>Progress:</b> reading data...</nobr>");
                constraints = new GridBagConstraints();
                constraints.gridx = 6;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 20, 0, 0);
                add(statusValueLabel, constraints);
                statusValueLabel.setVisible(false);

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

                Dimension cpuD     = firstCombo.getPreferredSize();
                Dimension memoryD  = secondCombo.getPreferredSize();
    //            Dimension stopD    = stopButton.getPreferredSize();

                Dimension maxD = new Dimension(Math.max(cpuD.width, memoryD.width), Math.max(cpuD.height, memoryD.height));
    //            maxD = new Dimension(Math.max(maxD.width, stopD.width), Math.max(maxD.height, stopD.height));

                firstCombo.setPreferredSize(maxD);
                firstCombo.setMinimumSize(maxD);
                secondCombo.setPreferredSize(maxD);
                secondCombo.setMinimumSize(maxD);
    //            stopButton.setPreferredSize(maxD);
    //            stopButton.setMinimumSize(maxD);
    
                Dimension sepD = updateSeparator.getPreferredSize();
                sepD.height = maxD.height - 2;
                sepD.width = 5;
                updateSeparator.setPreferredSize(sepD);
                updateSeparator.setMinimumSize(sepD);

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

        private JLabel firstLabel;
//        private JLabel secondLabel;
        private JComboBox<Aggregation> firstCombo;
        private JComboBox<Aggregation> secondCombo;
        private JCheckBox secondChoice;
        private JButton updateButton;
        private HTMLLabel statusValueLabel;
        
    }
    
    
    static final class DataViewSupport extends JPanel {
        
        private DataModel tableModel;
        private ProfilerTreeTable table;
        
        
        DataViewSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Data", null, 10, this, null); // NOI18N
        }
        
        
        void setData(GcNode root, boolean aggregated, boolean phases) {
            tableModel.setRoot(root);
             
            if (root.getChildCount() > 0) {
                table.setShowsRootHandles(aggregated || phases);
                
                if (aggregated) {
                    if (!table.isColumnVisible(4)) table.setColumnVisibility(4, true);
//                    if (table.getSortColumn() == 1) table.setSortColumn(3);
                } else {
//                    if (table.getSortColumn() == 4) table.setSortColumn(2);
                    if (table.isColumnVisible(4)) table.setColumnVisibility(4, false);
                }
            }
        }
        
        
        private void initComponents() {
            tableModel = new DataModel();
            table = new ProfilerTreeTable(tableModel, true, true, new int[] { 0 });
            
            table.setRootVisible(false);
            table.setShowsRootHandles(true);

            table.setMainColumn(0);
            table.setFitWidthColumn(0);

            table.setSortColumn(1);
            table.setDefaultSortOrder(SortOrder.DESCENDING);
            table.setDefaultSortOrder(0, SortOrder.ASCENDING);
            
            GcRenderers.NameRenderer nameRenderer = new GcRenderers.NameRenderer();
            table.setTreeCellRenderer(nameRenderer);
            
            GcRenderers.GcIdRenderer gcidRenderer = new GcRenderers.GcIdRenderer();
            table.setColumnRenderer(1, gcidRenderer);
            table.setDefaultSortOrder(1, SortOrder.ASCENDING);
            table.setDefaultColumnWidth(1, gcidRenderer.getPreferredWidth());
            table.setColumnVisibility(1, GcRenderers.GcIdRenderer.isInitiallyVisible());
            table.setSortColumn(1);
            
            GcRenderers.LongestPauseRenderer longestPauseRenderer = new GcRenderers.LongestPauseRenderer();
            table.setColumnRenderer(2, longestPauseRenderer);
            table.setDefaultColumnWidth(2, longestPauseRenderer.getPreferredWidth());
            table.setColumnVisibility(2, GcRenderers.LongestPauseRenderer.isInitiallyVisible());
            
            GcRenderers.SumOfPausesRenderer sumOfPausesRenderer = new GcRenderers.SumOfPausesRenderer();
            table.setColumnRenderer(3, sumOfPausesRenderer);
            table.setDefaultColumnWidth(3, sumOfPausesRenderer.getPreferredWidth());
            table.setColumnVisibility(3, GcRenderers.SumOfPausesRenderer.isInitiallyVisible());
            
            GcRenderers.CountRenderer countRenderer = new GcRenderers.CountRenderer();
            table.setColumnRenderer(4, countRenderer);
            table.setDefaultColumnWidth(4, countRenderer.getPreferredWidth());
            table.setColumnVisibility(4, GcRenderers.CountRenderer.isInitiallyVisible());
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private static class DataModel extends ProfilerTreeTableModel.Abstract {
            
            DataModel() {
                super(new GcNode.Root());
            }
            
            
            @Override
            public int getColumnCount() {
                return 5;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return JTree.class;
                    case 1: return Long.class;
                    case 2: return Duration.class;
                    case 3: return Duration.class;
                    case 4: return Long.class;
                    default: return null;
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0: return GcRenderers.NameRenderer.getDisplayName();
                    case 1: return GcRenderers.GcIdRenderer.getDisplayName();
                    case 2: return GcRenderers.LongestPauseRenderer.getDisplayName();
                    case 3: return GcRenderers.SumOfPausesRenderer.getDisplayName();
                    case 4: return GcRenderers.CountRenderer.getDisplayName();
                    default: return null;
                }
            }

            @Override
            public Object getValueAt(TreeNode node, int column) {
                if (node == null) return null;
                GcNode fnode = (GcNode)node;
                
                switch (column) {
                    case 0: return fnode;
                    case 1: return toLong(fnode.gcid, -1);
                    case 2: return fnode.longestPause;
                    case 3: return fnode.sumOfPauses;
                    case 4: return toLong(fnode.count, 0);
                    default: return null;
                }
            }
            
            @Override
            public void setValueAt(Object o, TreeNode node, int column) {}

            @Override
            public boolean isCellEditable(TreeNode node, int column) { return false; }
            
            
            private Long toLong(long value, long noValue) {
                return value == noValue ? null : Long.valueOf(value);
            }
            
        }
        
    }
    
    
    static class GcConfigurationSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        GcConfigurationSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("GC configuration", null, 10, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotGcViewProvider.EVENT_GC_CONFIGURATION.equals(typeName)) { // NOI18N
                try {
                    final StringBuilder s = new StringBuilder();
                    
                    s.append("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
                
                    s.append("<tr><td nowrap><b>Young GC:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getString("youngCollector")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Old GC:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getString("oldCollector")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Concurrent Threads:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("concurrentGCThreads"))).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Parallel Threads:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("parallelGCThreads"))).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>System.gc() Disabled:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getBoolean("isExplicitGCDisabled")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>System.gc() Concurrent:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getBoolean("isExplicitGCConcurrent")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Uses Dynamic Threads:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getBoolean("usesDynamicGCThreads")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>GC Time Ratio:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("gcTimeRatio"))).append("</td></tr>"); // NOI18N
                    
                
                    s.append("</table>"); // NOI18N
                    
                    initialized = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(s.toString());
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
    
    static class GcHeapConfigurationSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        GcHeapConfigurationSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Heap configuration", null, 20, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotGcViewProvider.EVENT_HEAP_CONFIGURATION.equals(typeName)) { // NOI18N
                try {
                    final StringBuilder s = new StringBuilder();
                    
                    s.append("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
                
                    s.append("<tr><td nowrap><b>Initial Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("initialSize") })).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Minimum Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("minSize") })).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Maximum Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("maxSize") })).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Compressed Oops:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getBoolean("usesCompressedOops")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Compressed Oops Mode:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getString("compressedOopsMode")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Address Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("heapAddressBits"))).append(" bits</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Object Alignment:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("objectAlignment") })).append("</td></tr>"); // NOI18N
                    
                
                    s.append("</table>"); // NOI18N
                    
                    initialized = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(s.toString());
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
    
    static class GcYoungGenConfigurationSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        GcYoungGenConfigurationSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Young Generation configuration", null, 30, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotGcViewProvider.EVENT_YOUNG_GEN_CONFIGURATION.equals(typeName)) { // NOI18N
                try {
                    final StringBuilder s = new StringBuilder();
                    
                    s.append("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
                
                    s.append("<tr><td nowrap><b>Minimum Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("minSize") })).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Maximum Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("maxSize") })).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Yount Generation Ratio:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("newRatio"))).append("</td></tr>"); // NOI18N
                    
                
                    s.append("</table>"); // NOI18N
                    
                    initialized = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(s.toString());
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
    
    static class GcSurvivorConfigurationSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        GcSurvivorConfigurationSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Survivor configuration", null, 40, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotGcViewProvider.EVENT_SURVIVOR_CONFIGURATION.equals(typeName)) { // NOI18N
                try {
                    final StringBuilder s = new StringBuilder();
                    
                    s.append("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
                
                    s.append("<tr><td nowrap><b>Initial Tenuring Threshold:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("initialTenuringThreshold"))).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Maximum Tenuring Threshold:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.numberFormat().format(event.getLong("maxTenuringThreshold"))).append("</td></tr>"); // NOI18N
                    
                
                    s.append("</table>"); // NOI18N
                    
                    initialized = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(s.toString());
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
    
    static class GcTlabConfigurationSupport extends JPanel implements JFREventVisitor {
        
        private volatile boolean initialized = false;
        
        private HTMLTextArea area;
        
        
        GcTlabConfigurationSupport() {
            initComponents();
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("TLAB configuration", null, 50, this, null);
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotGcViewProvider.EVENT_TLAB_CONFIGURATION.equals(typeName)) { // NOI18N
                try {
                    final StringBuilder s = new StringBuilder();
                    
                    s.append("<table border='0' cellpadding='0' cellspacing='0' width='100%'>");
                
                    s.append("<tr><td nowrap><b>TLABs Used:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(event.getBoolean("usesTLABs")).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>Minimum TLAB Size:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("minTLABSize") })).append("</td></tr>"); // NOI18N
                    s.append("<tr><td nowrap><b>TLAB Refill Waste Limit:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td><td width='100%'>").append(Formatters.bytesFormat().format(new Object[] { event.getLong("tlabRefillWasteLimit") })).append("</td></tr>"); // NOI18N
                    
                
                    s.append("</table>"); // NOI18N
                    
                    initialized = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            area.setText(s.toString());
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
    
}

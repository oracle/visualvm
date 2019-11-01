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
package org.graalvm.visualvm.jfr.views.fileio;

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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.ui.components.HTMLLabel;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;

/**
 *
 * @author Jiri Sedlacek
 */
final class FileIOViewSupport {
    
    static enum Aggregation {
        NONE { @Override public String toString() { return "None"; } },
        FILE { @Override public String toString() { return "File"; } },
        THREAD { @Override public String toString() { return "Thread"; } }
    };
    
    static abstract class MasterViewSupport extends JPanel {
        
        private Aggregation lastPrimary, lastSecondary;
        
        
        MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("File IO", null, this);
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
                if (Aggregation.FILE.equals(firstCombo.getSelectedItem())) {
                    if (Aggregation.FILE.equals(secondCombo.getItemAt(1))) {
                        int sel = secondCombo.getSelectedIndex();
                        DefaultComboBoxModel model = (DefaultComboBoxModel)secondCombo.getModel();
                        model.removeElementAt(1);
                        model.addElement(Aggregation.THREAD);
                        secondCombo.setSelectedIndex(sel);
                    }
                } else if (Aggregation.THREAD.equals(firstCombo.getSelectedItem())) {
                    if (Aggregation.THREAD.equals(secondCombo.getItemAt(1))) {
                        int sel = secondCombo.getSelectedIndex();
                        DefaultComboBoxModel model = (DefaultComboBoxModel)secondCombo.getModel();
                        model.removeElementAt(1);
                        model.addElement(Aggregation.FILE);
                        secondCombo.setSelectedIndex(sel);
                    }
                }
            }
            
            updateButton.setEnabled(lastPrimary != firstCombo.getSelectedItem() ||
                                    lastSecondary != secondCombo.getSelectedItem());
            
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
            } else if (!model.containsEvent(JFRSnapshotFileIOViewProvider.EventChecker.class)) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("File IO", JFRSnapshotFileIOViewProvider.EventChecker.checkedTypes()), BorderLayout.CENTER);
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
                firstCombo = new JComboBox(new Object[] { Aggregation.FILE, Aggregation.THREAD });
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
                secondLabel = new JLabel();
                secondLabel.setText("secondary:");
                secondLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 2;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 12, 0, 0);
                add(secondLabel, constraints);

                // memoryButton
                secondCombo = new JComboBox(new Object[] { Aggregation.NONE, Aggregation.THREAD });
                secondCombo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(false); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 3;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(secondCombo, constraints);
                
                lastPrimary = (Aggregation)firstCombo.getSelectedItem();
                lastSecondary = (Aggregation)secondCombo.getSelectedItem();
                
                // updateSeparator
                JSeparator updateSeparator = new JSeparator(JSeparator.VERTICAL);
                updateSeparator.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 4;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 16, 0, 0);
                add(updateSeparator, constraints);

                // updateButton
                updateButton = new JButton("Update Data");
                updateButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        updateButton.setEnabled(false);
                        lastPrimary = (Aggregation)firstCombo.getSelectedItem();
                        lastSecondary = (Aggregation)secondCombo.getSelectedItem();
                        changeAggregation((Aggregation)firstCombo.getSelectedItem(), (Aggregation)secondCombo.getSelectedItem());
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
        private JComboBox firstCombo;
        private JComboBox secondCombo;
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
        
        
        void setData(FileIONode root, boolean twoAggregations) {
            tableModel.setRoot(root);
            table.setShowsRootHandles(twoAggregations);
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
            
            FileIORenderers.NameRenderer nameRenderer = new FileIORenderers.NameRenderer();
            table.setTreeCellRenderer(nameRenderer);
            
            FileIORenderers.TotalTimeRenderer totalTimeRenderer = new FileIORenderers.TotalTimeRenderer();
            table.setColumnRenderer(1, totalTimeRenderer);
            table.setDefaultColumnWidth(1, totalTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(1, FileIORenderers.TotalTimeRenderer.isInitiallyVisible());
            
            FileIORenderers.ReadTimeRenderer readTimeRenderer = new FileIORenderers.ReadTimeRenderer();
            table.setColumnRenderer(2, readTimeRenderer);
            table.setDefaultColumnWidth(2, readTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(2, FileIORenderers.ReadTimeRenderer.isInitiallyVisible());
            
            FileIORenderers.MaxReadTimeRenderer maxReadTimeRenderer = new FileIORenderers.MaxReadTimeRenderer();
            table.setColumnRenderer(3, maxReadTimeRenderer);
            table.setDefaultColumnWidth(3, maxReadTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(3, FileIORenderers.MaxReadTimeRenderer.isInitiallyVisible());
            
            FileIORenderers.WriteTimeRenderer writeTimeRenderer = new FileIORenderers.WriteTimeRenderer();
            table.setColumnRenderer(4, writeTimeRenderer);
            table.setDefaultColumnWidth(4, writeTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(4, FileIORenderers.WriteTimeRenderer.isInitiallyVisible());
            
            FileIORenderers.MaxWriteTimeRenderer maxWriteTimeRenderer = new FileIORenderers.MaxWriteTimeRenderer();
            table.setColumnRenderer(5, maxWriteTimeRenderer);
            table.setDefaultColumnWidth(5, maxWriteTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(5, FileIORenderers.MaxWriteTimeRenderer.isInitiallyVisible());
            
            FileIORenderers.TotalCountRenderer totalCountRenderer = new FileIORenderers.TotalCountRenderer();
            table.setColumnRenderer(6, totalCountRenderer);
            table.setDefaultColumnWidth(6, totalCountRenderer.getPreferredWidth());
            table.setColumnVisibility(6, FileIORenderers.TotalCountRenderer.isInitiallyVisible());
            
            FileIORenderers.ReadCountRenderer readCountRenderer = new FileIORenderers.ReadCountRenderer();
            table.setColumnRenderer(7, readCountRenderer);
            table.setDefaultColumnWidth(7, readCountRenderer.getPreferredWidth());
            table.setColumnVisibility(7, FileIORenderers.ReadCountRenderer.isInitiallyVisible());
            
            FileIORenderers.WriteCountRenderer writeCountRenderer = new FileIORenderers.WriteCountRenderer();
            table.setColumnRenderer(8, writeCountRenderer);
            table.setDefaultColumnWidth(8, writeCountRenderer.getPreferredWidth());
            table.setColumnVisibility(8, FileIORenderers.WriteCountRenderer.isInitiallyVisible());
            
            FileIORenderers.ReadBytesRenderer readBytesRenderer = new FileIORenderers.ReadBytesRenderer();
            table.setColumnRenderer(9, readBytesRenderer);
            table.setDefaultColumnWidth(9, readBytesRenderer.getPreferredWidth());
            table.setColumnVisibility(9, FileIORenderers.ReadBytesRenderer.isInitiallyVisible());
            
            FileIORenderers.WriteBytesRenderer writeBytesRenderer = new FileIORenderers.WriteBytesRenderer();
            table.setColumnRenderer(10, writeBytesRenderer);
            table.setDefaultColumnWidth(10, writeBytesRenderer.getPreferredWidth());
            table.setColumnVisibility(10, FileIORenderers.WriteBytesRenderer.isInitiallyVisible());
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private static class DataModel extends ProfilerTreeTableModel.Abstract {
            
            DataModel() {
                super(new FileIONode.Root());
            }
            
            
            @Override
            public int getColumnCount() {
                return 11;
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0: return JTree.class;
                    case 1: return Duration.class;
                    case 2: return Duration.class;
                    case 3: return Duration.class;
                    case 4: return Duration.class;
                    case 5: return Duration.class;
                    case 6: return Long.class;
                    case 7: return Long.class;
                    case 8: return Long.class;
                    case 9: return Long.class;
                    case 10: return Long.class;
                    default: return null;
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0: return FileIORenderers.NameRenderer.getDisplayName();
                    case 1: return FileIORenderers.TotalTimeRenderer.getDisplayName();
                    case 2: return FileIORenderers.ReadTimeRenderer.getDisplayName();
                    case 3: return FileIORenderers.MaxReadTimeRenderer.getDisplayName();
                    case 4: return FileIORenderers.WriteTimeRenderer.getDisplayName();
                    case 5: return FileIORenderers.MaxWriteTimeRenderer.getDisplayName();
                    case 6: return FileIORenderers.TotalCountRenderer.getDisplayName();
                    case 7: return FileIORenderers.ReadCountRenderer.getDisplayName();
                    case 8: return FileIORenderers.WriteCountRenderer.getDisplayName();
                    case 9: return FileIORenderers.ReadBytesRenderer.getDisplayName();
                    case 10: return FileIORenderers.WriteBytesRenderer.getDisplayName();
                    default: return null;
                }
            }

            @Override
            public Object getValueAt(TreeNode node, int column) {
                if (node == null) return null;
                FileIONode fnode = (FileIONode)node;
                
                switch (column) {
                    case 0: return fnode;
                    case 1: return toDuration(fnode.durationR, fnode.durationW);
                    case 2: return fnode.durationR;
                    case 3: return fnode.durationRMax;
                    case 4: return fnode.durationW;
                    case 5: return fnode.durationWMax;
                    case 6: return toLong(fnode.countR + fnode.countW);
                    case 7: return toLong(fnode.countR);
                    case 8: return toLong(fnode.countW);
                    case 9: return toLong(fnode.bytesR);
                    case 10: return toLong(fnode.bytesW);
                    default: return null;
                }
            }
            
            @Override
            public void setValueAt(Object o, TreeNode node, int column) {}

            @Override
            public boolean isCellEditable(TreeNode node, int column) { return false; }
            
            
            private Long toLong(long value) {
                return value == 0 ? null : Long.valueOf(value);
            }
            
            private Duration toDuration(Duration duration1, Duration duration2) {
                if (duration1 == null && duration2 == null) return null;
                if (duration2 == null) return duration1;
                if (duration1 == null) return duration2;
                return duration1.plus(duration2);
            }
            
        }
        
    }
    
}

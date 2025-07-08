/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.locks;

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
final class LocksViewSupport {
    
    static enum Aggregation {
        NONE { @Override public String toString() { return "None"; } },
        CLASS { @Override public String toString() { return "Monitor Class"; } },
        OBJECT { @Override public String toString() { return "Monitor Object"; } },
        THREAD_BLOCKED { @Override public String toString() { return "Blocked Thread"; } },
        THREAD_BLOCKING { @Override public String toString() { return "Blocking Thread"; } }
    }

    static abstract class MasterViewSupport extends JPanel {
        
        private int lastMode;
        private Aggregation lastPrimary, lastSecondary;
        
        
        MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Locks", null, this);
        }
        
        
        abstract void firstShown();
        
        abstract void changeAggregation(int mode, Aggregation primary, Aggregation secondary);
        
        
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
                int sel = secondCombo.getSelectedIndex();
                
                DefaultComboBoxModel<Aggregation> model = (DefaultComboBoxModel<Aggregation>)secondCombo.getModel();
                while (model.getSize() > 1) model.removeElementAt(1);
                
                if (!Aggregation.CLASS.equals(firstCombo.getSelectedItem()) &&
                    !Aggregation.OBJECT.equals(firstCombo.getSelectedItem()))
                        model.addElement(Aggregation.CLASS);
                
                if (!Aggregation.CLASS.equals(firstCombo.getSelectedItem()) &&
                    !Aggregation.OBJECT.equals(firstCombo.getSelectedItem()))
                        model.addElement(Aggregation.OBJECT);
                
                if (!Aggregation.THREAD_BLOCKED.equals(firstCombo.getSelectedItem()))
                    model.addElement(Aggregation.THREAD_BLOCKED);
                
                if (!Aggregation.THREAD_BLOCKING.equals(firstCombo.getSelectedItem()))
                    model.addElement(Aggregation.THREAD_BLOCKING);
                
                secondCombo.setSelectedIndex(sel < secondCombo.getItemCount() ? sel : 0);
            }
            
            updateButton.setEnabled(lastMode != modeCombo.getSelectedIndex() ||
                                    lastPrimary != firstCombo.getSelectedItem() ||
                                    lastSecondary != secondCombo.getSelectedItem());
            
        }
        
        
        private int prefHeight = -1;
        public Dimension getPreferredSize() {
            if (modeCombo == null) return super.getPreferredSize();
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
            } else if (!model.containsEvent(JFRSnapshotLocksViewProvider.EventChecker.class)) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Locks", JFRSnapshotLocksViewProvider.EventChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(11, 5, 20, 5));

                GridBagConstraints constraints;

                // modeLabel
                JLabel modeLabel = new JLabel();
                modeLabel.setText("Display:");
                modeLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(modeLabel, constraints);

                // modeCombo
                modeCombo = new JComboBox<>(new String[] { "Locks & Object.wait()", "Locks", "Object.wait()" });
                modeCombo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(false); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 1;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(modeCombo, constraints);

                // modeSeparator
                JSeparator modeSeparator = new JSeparator(JSeparator.VERTICAL);
                modeSeparator.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 2;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 16, 0, 0);
                add(modeSeparator, constraints);

                // firstLabel
                firstLabel = new JLabel();
                firstLabel.setText("Aggregation:");
                firstLabel.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 3;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(firstLabel, constraints);

                // firstCombo
                firstCombo = new JComboBox<>(new Aggregation[] { Aggregation.CLASS, Aggregation.OBJECT, Aggregation.THREAD_BLOCKED, Aggregation.THREAD_BLOCKING });
                firstCombo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(true); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 4;
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
                constraints.gridx = 5;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 12, 0, 0);
                add(secondLabel, constraints);

                // memoryButton
                secondCombo = new JComboBox<>(new Aggregation[] { Aggregation.NONE, Aggregation.THREAD_BLOCKED, Aggregation.THREAD_BLOCKING });
                secondCombo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleAggregationChanged(false); }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 6;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 8, 0, 0);
                add(secondCombo, constraints);
                
                lastMode = modeCombo.getSelectedIndex();
                lastPrimary = (Aggregation)firstCombo.getSelectedItem();
                lastSecondary = (Aggregation)secondCombo.getSelectedItem();

                // updateSeparator
                JSeparator updateSeparator = new JSeparator(JSeparator.VERTICAL);
                updateSeparator.setOpaque(false);
                constraints = new GridBagConstraints();
                constraints.gridx = 7;
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
                        lastMode = modeCombo.getSelectedIndex();
                        lastPrimary = (Aggregation)firstCombo.getSelectedItem();
                        lastSecondary = (Aggregation)secondCombo.getSelectedItem();
                        changeAggregation(lastMode, lastPrimary, lastSecondary);
                    }
                });
                constraints = new GridBagConstraints();
                constraints.gridx = 8;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 12, 0, 0);
                add(updateButton, constraints);

                // statusValueLabel
                statusValueLabel = new HTMLLabel("<nobr><b>Progress:</b> reading data...</nobr>");
                constraints = new GridBagConstraints();
                constraints.gridx = 9;
                constraints.gridy = 2;
                constraints.gridwidth = 1;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(4, 20, 0, 0);
                add(statusValueLabel, constraints);
                statusValueLabel.setVisible(false);

                // filler1
                constraints = new GridBagConstraints();
                constraints.gridx = 10;
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
                Dimension stopD    = modeCombo.getPreferredSize();

                Dimension maxD = new Dimension(Math.max(cpuD.width, memoryD.width), Math.max(cpuD.height, memoryD.height));
                maxD = new Dimension(Math.max(maxD.width, 0), Math.max(maxD.height, stopD.height));

                firstCombo.setPreferredSize(maxD);
                firstCombo.setMinimumSize(maxD);
                secondCombo.setPreferredSize(maxD);
                secondCombo.setMinimumSize(maxD);

                stopD.height = maxD.height;
                modeCombo.setPreferredSize(stopD);
                modeCombo.setMinimumSize(stopD);

                Dimension sepD = modeSeparator.getPreferredSize();
                sepD.height = maxD.height - 2;
                sepD.width = 5;
                modeSeparator.setPreferredSize(sepD);
                modeSeparator.setMinimumSize(sepD);

                Dimension sepD2 = updateSeparator.getPreferredSize();
                sepD2.height = maxD.height - 2;
                sepD2.width = 5;
                updateSeparator.setPreferredSize(sepD2);
                updateSeparator.setMinimumSize(sepD2);

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
        private JLabel secondLabel;
        private JComboBox<String> modeCombo;
        private JComboBox<Aggregation> firstCombo;
        private JComboBox<Aggregation> secondCombo;
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
        
        
        void setData(LocksNode root, boolean twoAggregations) {
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
            
            LocksRenderers.LocksNameRenderer nameRenderer = new LocksRenderers.LocksNameRenderer();
            table.setTreeCellRenderer(nameRenderer);
            
            LocksRenderers.TotalTimeRenderer totalTimeRenderer = new LocksRenderers.TotalTimeRenderer();
            table.setColumnRenderer(1, totalTimeRenderer);
            table.setDefaultColumnWidth(1, totalTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(1, LocksRenderers.TotalTimeRenderer.isInitiallyVisible());
            
            LocksRenderers.MaxTimeRenderer maxReadTimeRenderer = new LocksRenderers.MaxTimeRenderer();
            table.setColumnRenderer(2, maxReadTimeRenderer);
            table.setDefaultColumnWidth(2, maxReadTimeRenderer.getPreferredWidth());
            table.setColumnVisibility(2, LocksRenderers.MaxTimeRenderer.isInitiallyVisible());
            
            LocksRenderers.TotalCountRenderer totalCountRenderer = new LocksRenderers.TotalCountRenderer();
            table.setColumnRenderer(3, totalCountRenderer);
            table.setDefaultColumnWidth(3, totalCountRenderer.getPreferredWidth());
            table.setColumnVisibility(3, LocksRenderers.TotalCountRenderer.isInitiallyVisible());
            
            setLayout(new BorderLayout());
            add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
        }
        
        
        private static class DataModel extends ProfilerTreeTableModel.Abstract {
            
            DataModel() {
                super(new LocksNode.Root());
            }
            
            
            @Override
            public int getColumnCount() {
                return 4;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return JTree.class;
                    case 1: return Duration.class;
                    case 2: return Duration.class;
                    case 3: return Long.class;
                    default: return null;
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0: return LocksRenderers.LocksNameRenderer.getDisplayName();
                    case 1: return LocksRenderers.TotalTimeRenderer.getDisplayName();
                    case 2: return LocksRenderers.MaxTimeRenderer.getDisplayName();
                    case 3: return LocksRenderers.TotalCountRenderer.getDisplayName();
                    default: return null;
                }
            }

            @Override
            public Object getValueAt(TreeNode node, int column) {
                if (node == null) return null;
                LocksNode fnode = (LocksNode)node;
                
                switch (column) {
                    case 0: return fnode;
                    case 1: return fnode.duration;
                    case 2: return fnode.durationMax;
                    case 3: return toLong(fnode.count);
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
            
        }
        
    }
    
}

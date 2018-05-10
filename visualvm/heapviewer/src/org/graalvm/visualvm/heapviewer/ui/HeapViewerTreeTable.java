/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import java.util.Enumeration;
import java.util.List;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import java.util.Objects;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
class HeapViewerTreeTable extends ProfilerTreeTable {   
    
    private volatile boolean initializing = true;
    
    private boolean sorting;
    
    
    HeapViewerTreeTable(ProfilerTreeTableModel model, List<? extends RowSorter.SortKey> sortKeys) {
        super(model, true, true, new int[] { 0 });
        
        setRootVisible(false);
        setShowsRootHandles(true);
        
        setShadeUnfocusedSelection(true);
        
        setForgetPreviouslyExpanded(true);
        
        setAllowsThreeStateColumns(true);
        getRowSorter().setSortKeys(sortKeys);
        
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            private HeapViewerNode currentSelected;
            private boolean currentAdjusting;
            private boolean adjustingNull;
            public void valueChanged(ListSelectionEvent e) {
//                System.err.println(">>> selected " + getSelectedNode() + " adjusting " + e.getValueIsAdjusting());
                
                // Ignore changes during sorting, will be handled separately in willBeSorted()
                if (sorting) return;
                
                HeapViewerNode node = getSelectedNode();
                boolean adjusting = e.getValueIsAdjusting();
                
                // workaround for noise created by restoring selection on internal model updates
                // leading nulls which are adjusting mean that following non-adjusting null should be skipped
                if (node == null) {
                    if (adjusting) {
                        adjustingNull = true;
                        return;
                    } else if (adjustingNull) {
                        adjustingNull = false;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                // No node selected after leading adjusting nulls, selection not restored
                                if (getSelectedNode() == null) nodeSelected(null, false);
                            }
                        });
                        return;
                    }
                } else {
                    adjustingNull = false;
                }
                
                // workaround for noise created by restoring selection on internal model updates
                // ignore the same selection which is a result of clearing noise nulls
                if (Objects.equals(currentSelected, node) && currentAdjusting == adjusting) return;
                
                currentSelected = node;
                currentAdjusting = adjusting;
                
                nodeSelected(node, adjusting);
            }
        });
        
        initializing = false;
    }
    
    
    protected final boolean isInitializing() { return initializing; }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {}
    
    protected void forgetChildren(HeapViewerNode node) {}
    
    
    protected void nodeCollapsed(final TreeNode node) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { forgetChildren((HeapViewerNode)node); }
        });
    }
    
    
    protected void willBeSorted(List<? extends RowSorter.SortKey> sortKeys) {
        sorting = true;
        final HeapViewerNode beforeSortingSelected = getSelectedNode();
        
        final UIState uiState = getUIState();
        
        try {
        
            Enumeration<TreePath> expanded = getExpandedNodes();
            clearSelection();
//            resetExpandedNodes();
            if (expanded != null) while (expanded.hasMoreElements()) {
                HeapViewerNode node = (HeapViewerNode)expanded.nextElement().getLastPathComponent();
//                System.err.println(">>> willBeSorted - " + node.toString());
                node.willBeSorted();
            }

        } catch (Throwable t) {
            Exceptions.printStackTrace(t);
        }
        
//        if (getTreeModel() != null) getTreeModel().reload();
//        
//        resetExpandedNodes();
        
        resetPath(null);

        if (uiState != null) {
            restoreSelectedNodes(uiState);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                        resetPath(null);
//                        resetExpandedNodes();
                    restoreExpandedNodes(uiState);
//                        restoreSelectedNodes(uiState);
                    sorting = false;
                    HeapViewerNode afterSortingSelected = getSelectedNode();
                    if (!Objects.equals(beforeSortingSelected, afterSortingSelected))
                        nodeSelected(afterSortingSelected, false);
                }
            });
        } else {
            sorting = false;
            HeapViewerNode afterSortingSelected = getSelectedNode();
            if (!Objects.equals(beforeSortingSelected, afterSortingSelected))
                nodeSelected(afterSortingSelected, false);
        }
    }
    
    
    private HeapViewerNode getSelectedNode() {
        int row = getSelectedRow();
        return row == -1 ? null : (HeapViewerNode)getValueForRow(row);
    }
    
}

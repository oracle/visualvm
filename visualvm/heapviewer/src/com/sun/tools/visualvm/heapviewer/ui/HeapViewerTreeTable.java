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

package com.sun.tools.visualvm.heapviewer.ui;

import java.util.Enumeration;
import java.util.List;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTableModel;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
class HeapViewerTreeTable extends ProfilerTreeTable {   
    
    private volatile boolean initializing = true;
    
    HeapViewerTreeTable(ProfilerTreeTableModel model, List<? extends RowSorter.SortKey> sortKeys) {
        super(model, true, true, new int[] { 0 });
        
        setRootVisible(false);
        setShowsRootHandles(true);
        
        setShadeUnfocusedSelection(true);
        
        setForgetPreviouslyExpanded(true);
        
        setAllowsThreeStateColumns(true);
        getRowSorter().setSortKeys(sortKeys);
        
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int row = getSelectedRow();
                HeapViewerNode sel = row == -1 ? null : (HeapViewerNode)getValueForRow(row);
                nodeSelected(sel, e.getValueIsAdjusting());
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
                }
            });
        }
    }
    
}

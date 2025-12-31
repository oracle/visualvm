/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components.treetable;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.JTreeTable;


public class TreeTableModelAdapter extends AbstractTableModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected AbstractTreeTableModel treeTableModel;
    protected JTree tree;
    protected JTreeTable treeTable;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructs a TreeTableModelAdapter, bridging the given treeTable and the model
     *
     * @param treeTableModel the model to use
     * @param treeTable      the table which is going to use this model
     */
    public TreeTableModelAdapter(AbstractTreeTableModel treeTableModel, JTreeTable treeTable) {
        this.treeTable = treeTable;
        this.tree = treeTable.getTree();
        this.treeTableModel = treeTableModel;

        tree.addTreeExpansionListener(new TreeExpansionListener() {
                // Don't use fireTableRowsInserted() here; the selection model
                // would get updated twice.
                public void treeExpanded(TreeExpansionEvent event) {
                    TreePath[] selectedPaths = tree.getSelectionPaths();
                    fireTableDataChanged();
                    tree.setSelectionPaths(selectedPaths);
                }

                public void treeCollapsed(TreeExpansionEvent event) {
                    TreePath[] selectedPaths = tree.getSelectionPaths();
                    fireTableDataChanged();
                    tree.setSelectionPaths(selectedPaths);
                }
            });

        // Install a TreeModelListener that can updateState the table when
        // tree changes. We use delayedFireTableDataChanged as we can
        // not be guaranteed the tree will have finished processing
        // the event before us.
        treeTableModel.addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    delayedFireTableDataChanged();
                }

                public void treeNodesInserted(TreeModelEvent e) {
                    delayedFireTableDataChanged();
                }

                public void treeNodesRemoved(TreeModelEvent e) {
                    delayedFireTableDataChanged();
                }

                public void treeStructureChanged(TreeModelEvent e) {
                    delayedFireTableDataChanged();
                }
            });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * TableModel wrapper, passes it through to the model after
     * fetching the right TreeTableNode for the given row.
     */
    public boolean isCellEditable(int row, int column) {
        return treeTableModel.isCellEditable(nodeForRow(row), column);
    }

    /**
     * TableModel wrapper, passes it through to the model.
     */
    public Class<?> getColumnClass(int column) {
        return treeTableModel.getColumnClass(column);
    }

    /**
     * TableModel wrapper, passes it through to the model.
     */
    public int getColumnCount() {
        return treeTableModel.getColumnCount();
    }

    /**
     * TableModel wrapper, passes it through to the model.
     */
    public String getColumnName(int column) {
        return treeTableModel.getColumnName(column);
    }

    /**
     * Returns a list of open paths in the tree, can be used to
     * re-open the paths in a tree after a call to 'treeStructureChanged'
     * (which causes all open paths to collapse)
     */
    public List<TreePath> getExpandedPaths() {
        Enumeration<TreePath> expanded = tree.getExpandedDescendants(getRootPath());
        List<TreePath> paths = new ArrayList<>();

        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                paths.add(expanded.nextElement());
            }
        }
    
        return paths;
    }

    /**
     * Returns the (tree)path to the root of the model.
     *
     * @return
     */
    public TreePath getRootPath() {
        return new TreePath(treeTableModel.getPathToRoot((CCTNode) treeTableModel.getRoot()));
    }

    /**
     * TableModel wrapper, passes it through to the model.
     */
    public int getRowCount() {
        return tree.getRowCount();
    }

    /**
     * TableModel wrapper, passes it through to the model after
     * fetching the right TreeTableNode for the given row.
     */
    public void setValueAt(Object value, int row, int column) {
        treeTableModel.setValueAt(value, nodeForRow(row), column);
    }

    /**
     * Returns the object on the given row and column.
     */
    public Object getValueAt(int row, int column) {
        Object j = treeTableModel.getValueAt(nodeForRow(row), column);

        return j;
    }

    /**
     * Opens the root node.
     */
    public void expandRoot() {
        tree.expandPath(getRootPath());
    }

    /**
     * Restores the given open paths on the treeModel.
     *
     * @param paths a List of TreePaths which are going to be opened.
     */
    public void restoreExpandedPaths(List<TreePath> paths) {
        tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, Boolean.TRUE); // NOI18N
        for (TreePath p : paths) {
            tree.expandPath(p);
        }
        tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, Boolean.FALSE); // NOI18N
    }
    
    private TreePath getCurrentPath(TreePath oldPath) {
        if (oldPath == null || oldPath.getPathCount() < 1) return null;
        if (!treeTableModel.getRoot().equals(oldPath.getPathComponent(0))) return null;
        
        TreePath p = getRootPath();
        Object[] op = oldPath.getPath();
        CCTNode n = (CCTNode)treeTableModel.getRoot();
        
        for (int i = 1; i < op.length; i++) {
            // #241115
            CCTNode[] children = n.getChildren();
            if (children == null) return null;
            
            CCTNode nn = null;
            
            for (CCTNode c : children)
                if (c.equals(op[i])) {
                    nn = c;
                    break;
                }
            
            if (nn == null) return null;
            
            n = nn;
            p = p.pathByAddingChild(n);
        }
        
        return p;
    }

    public void updateTreeTable() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                List<TreePath> pathState = getExpandedPaths();

                TreePath[] selectedPaths = tree.getSelectionPaths();
                tree.getSelectionModel().clearSelection();
                treeTableModel.fireTreeStructureChanged(this,
                        treeTableModel.getPathToRoot((CCTNode) treeTableModel.getRoot()),
                        null, null);
                
                if (selectedPaths != null)
                    for (int i = 0; i < selectedPaths.length; i++)
                        selectedPaths[i] = getCurrentPath(selectedPaths[i]);
                tree.setSelectionPaths(selectedPaths);

                restoreExpandedPaths(pathState);

                treeTable.getTableHeader().repaint();

                delayedFireTableDataChanged();
            }
        });
    }
    
    public void changeRoot(final CCTNode newRoot) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                List<TreePath> pathState = getExpandedPaths();
                TreePath[] selectedPaths = tree.getSelectionPaths();

                treeTableModel.setRoot(newRoot);
                tree.getSelectionModel().clearSelection();
                treeTableModel.fireTreeStructureChanged(this,
                                                        treeTableModel.getPathToRoot((CCTNode) treeTableModel.getRoot()),
                                                        null, null);

                if (selectedPaths != null)
                    for (int i = 0; i < selectedPaths.length; i++)
                        selectedPaths[i] = getCurrentPath(selectedPaths[i]);
                List<TreePath> expandedPaths = new ArrayList<>();
                for (TreePath tp : pathState)
                    expandedPaths.add(getCurrentPath(tp));

                tree.setSelectionPaths(selectedPaths);
                restoreExpandedPaths(expandedPaths);

                treeTable.getTableHeader().repaint();

                delayedFireTableDataChanged();
            }
        });
    }
    
    public void setup(List<TreePath> expanded, final TreePath selected) {
        tree.getSelectionModel().clearSelection();
        treeTableModel.fireTreeStructureChanged(this,
                                                treeTableModel.getPathToRoot((CCTNode) treeTableModel.getRoot()),
                                                null, null);
        treeTable.getTableHeader().repaint();
        fireTableDataChanged();
        
        if (expanded != null) restoreExpandedPaths(expanded);
        if (selected != null) {
            tree.setSelectionPath(selected);
            final Rectangle rect = tree.getPathBounds(selected);
            if (rect != null) {
                // scroll immediately
                treeTable.scrollRectToVisible(tree.getPathBounds(selected));
                // make sure the rect is still visible after eventually showing the horizontal scrollbar
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { treeTable.scrollRectToVisible(rect); }
                });
            }
        }
    }
    
    private boolean firingChange;
    public final boolean isFiringChange() {
        return firingChange;
    }

    /**
     * Invokes fireTableDataChanged after all the pending events have been
     * processed. SwingUtilities.invokeLater is used to handle this.
     */
    protected void delayedFireTableDataChanged() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    TreePath[] selectedPaths = tree.getSelectionPaths();
                    firingChange = true;
                    try {
                        fireTableDataChanged();
                    } finally {
                        firingChange = false;
                    }
                    tree.setSelectionPaths(selectedPaths);
                }
            });
    }
    
    /**
     * Returns the object (TreeTableNode) on the given row in the tree.
     */
    protected Object nodeForRow(int row) {
        TreePath treePath = tree.getPathForRow(row);

        if (treePath != null) {
            return treePath.getLastPathComponent();
        } else {
            return null;
        }
    }
}

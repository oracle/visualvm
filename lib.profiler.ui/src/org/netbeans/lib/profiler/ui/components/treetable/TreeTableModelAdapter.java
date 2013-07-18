/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.ui.components.treetable;

import java.awt.Rectangle;
import java.util.ArrayList;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.ui.UIUtils;


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
    public Class getColumnClass(int column) {
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
    public List getExpandedPaths() {
        Enumeration expanded = tree.getExpandedDescendants(getRootPath());
        List paths = new ArrayList();

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
    public void restoreExpandedPaths(List paths) {
        tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, Boolean.TRUE); // NOI18N
        for (Object p : paths) {
            tree.expandPath((TreePath)p);
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
            CCTNode nn = null;
            
            for (CCTNode c : n.getChildren())
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
                List pathState = getExpandedPaths();

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
                List pathState = getExpandedPaths();
                TreePath[] selectedPaths = tree.getSelectionPaths();

                treeTableModel.setRoot(newRoot);
                tree.getSelectionModel().clearSelection();
                treeTableModel.fireTreeStructureChanged(this,
                                                        treeTableModel.getPathToRoot((CCTNode) treeTableModel.getRoot()),
                                                        null, null);

                if (selectedPaths != null)
                    for (int i = 0; i < selectedPaths.length; i++)
                        selectedPaths[i] = getCurrentPath(selectedPaths[i]);
                List expandedPaths = new ArrayList();
                for (Object tp : pathState)
                    expandedPaths.add(getCurrentPath((TreePath)tp));

                tree.setSelectionPaths(selectedPaths);
                restoreExpandedPaths(expandedPaths);

                treeTable.getTableHeader().repaint();

                delayedFireTableDataChanged();
            }
        });
    }
    
    public void setup(List expanded, final TreePath selected) {
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

    /**
     * Invokes fireTableDataChanged after all the pending events have been
     * processed. SwingUtilities.invokeLater is used to handle this.
     */
    protected void delayedFireTableDataChanged() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    TreePath[] selectedPaths = tree.getSelectionPaths();
                    fireTableDataChanged();
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

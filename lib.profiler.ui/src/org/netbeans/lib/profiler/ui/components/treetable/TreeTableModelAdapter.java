/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import java.util.Enumeration;
import java.util.Vector;
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
     * Returns a vector of open paths in the tree, can be used to
     * re-open the paths in a tree after a call to 'treeStructureChanged'
     * (which causes all open paths to collapse)
     */
    public Vector getExpandedPaths() {
        Enumeration expanded = tree.getExpandedDescendants(getRootPath());
        Vector paths = new Vector();

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
     * @param paths a Vector of TreePaths which are going to be opened.
     */
    public void restoreExpandedPaths(Vector paths) {
        Enumeration e = paths.elements();

        tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, Boolean.TRUE); // NOI18N
        while (e.hasMoreElements()) {
            TreePath path = (TreePath) e.nextElement();
            tree.expandPath(path);
        }
        tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, Boolean.FALSE); // NOI18N
    }

    public void updateTreeTable() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Vector pathState = getExpandedPaths();

                    TreePath[] selectedPaths = tree.getSelectionPaths();
                    treeTableModel.fireTreeStructureChanged(this,
                                                            treeTableModel.getPathToRoot((CCTNode) treeTableModel.getRoot()),
                                                            null, null);
                    tree.setSelectionPaths(selectedPaths);

                    restoreExpandedPaths(pathState);

                    treeTable.getTableHeader().repaint();

                    delayedFireTableDataChanged();
                }
            });
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

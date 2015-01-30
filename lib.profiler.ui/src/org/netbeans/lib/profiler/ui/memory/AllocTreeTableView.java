/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.memory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryCCTManager;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTableModel;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class AllocTreeTableView extends MemoryView {
    
    private AllocTreeTableModel treeTableModel;
    private ProfilerTreeTable treeTable;
    
    public AllocTreeTableView() {
        initUI();
    }
    
    
    protected ProfilerTable getResultsComponent() { return treeTable; }
    
    
    void setData(final AllocMemoryResultsSnapshot snapshot, Collection filter, int aggregation) {
        final boolean includeEmpty = filter != null;
        
        int _nTrackedItems = snapshot.getNProfiledClasses();
        String[] _classNames = snapshot.getClassNames();
        int[] _nTotalAllocObjects = snapshot.getObjectsCounts();
        long[] _totalAllocObjectsSize = snapshot.getObjectsSizePerClass();
        
        List<PresoObjAllocCCTNode> nodes = new ArrayList();
        
        long totalObjects = 0;
        long totalBytes = 0;
        
        for (int i = 0; i < _nTrackedItems; i++) {
            totalObjects += _nTotalAllocObjects[i];
            totalBytes += _totalAllocObjectsSize[i];
            
            if ((!includeEmpty && _nTotalAllocObjects[i] > 0) || (includeEmpty && filter.contains(_classNames[i]))) {
                final int _i = i;
                PresoObjAllocCCTNode node = new PresoObjAllocCCTNode(_classNames[i], _nTotalAllocObjects[i], _totalAllocObjectsSize[i]) {
                    public CCTNode[] getChildren() {
                        if (children == null) {
                            MemoryCCTManager callGraphManager = new MemoryCCTManager(snapshot, _i, true);
                            PresoObjAllocCCTNode root = callGraphManager.getRootNode();
                            setChildren(root == null ? new PresoObjAllocCCTNode[0] :
                                        (PresoObjAllocCCTNode[])root.getChildren());
                        }
                        return children;
                    }
                    public boolean isLeaf() {
                        if (children == null) return includeEmpty ? nCalls == 0 : false;
                        else return super.isLeaf();
                    }   
                    public int getChildCount() {
                        if (children == null) getChildren();
                        return super.getChildCount();
                    }
                };
                nodes.add(node);
            }
        }
        
        renderers[0].setMaxValue(totalBytes);
        renderers[1].setMaxValue(totalObjects);
        
        final PresoObjAllocCCTNode root = PresoObjAllocCCTNode.rootNode(nodes.toArray(new PresoObjAllocCCTNode[nodes.size()]));
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                treeTableModel.setRoot(root);
            }
        });
    }
    
    
    ExportUtils.ExportProvider[] getExportProviders() {
        return treeTable.getRowCount() == 0 ? null : new ExportUtils.ExportProvider[] {
            new ExportUtils.CSVExportProvider(treeTable),
            new ExportUtils.HTMLExportProvider(treeTable, EXPORT_ALLOCATED),
            new ExportUtils.XMLExportProvider(treeTable, EXPORT_ALLOCATED),
            new ExportUtils.PNGExportProvider(treeTable.getParent())
        };
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection userValue);
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        treeTableModel = new AllocTreeTableModel(PrestimeCPUCCTNode.EMPTY);
        
        treeTable = new ProfilerTreeTable(treeTableModel, true, true, new int[] { 0 }) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return AllocTreeTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                AllocTreeTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
        };
        
        treeTable.providePopupMenu(true);
        treeTable.setDefaultAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int row = treeTable.getSelectedRow();
                ClientUtils.SourceCodeSelection userValue = getUserValueForRow(row);
                if (userValue != null) performDefaultAction(userValue);
            }
        });
        
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);
        treeTable.makeTreeAutoExpandable(2);
        
        treeTable.setMainColumn(0);
        treeTable.setFitWidthColumn(0);
        
        treeTable.setSortColumn(1);
        treeTable.setDefaultSortOrder(0, SortOrder.ASCENDING);
        
        renderers = new HideableBarRenderer[2];
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        
        renderers[0].setMaxValue(123456789);
        renderers[1].setMaxValue(12345678);
        
        treeTable.setTreeCellRenderer(new MemoryJavaNameRenderer());
        treeTable.setColumnRenderer(1, renderers[0]);
        treeTable.setColumnRenderer(2, renderers[1]);
        
        treeTable.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
        treeTable.setDefaultColumnWidth(2, renderers[1].getMaxNoBarWidth());
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(treeTable, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)treeTable.getValueForRow(row);
        String[] name = node.getMethodClassNameAndSig();
        return new ClientUtils.SourceCodeSelection(name[0], name[1], name[2]);
    }
    
    
    private class AllocTreeTableModel extends ProfilerTreeTableModel.Abstract {
        
        AllocTreeTableModel(TreeNode root) {
            super(root);
        }
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return COLUMN_NAME;
            } else if (columnIndex == 1) {
                return COLUMN_ALLOCATED_BYTES;
            } else if (columnIndex == 2) {
                return COLUMN_ALLOCATED_OBJECTS;
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return JTree.class;
            } else if (columnIndex == 1) {
                return Long.class;
            } else if (columnIndex == 2) {
                return Integer.class;
            }
            return null;
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(TreeNode node, int columnIndex) {
            PresoObjAllocCCTNode allocNode = (PresoObjAllocCCTNode)node;
            
            if (columnIndex == 0) {
                return allocNode.getNodeName();
            } else if (columnIndex == 1) {
                return allocNode.totalObjSize;
            } else if (columnIndex == 2) {
                return allocNode.nCalls;
            }

            return null;
        }
        
        public void setValueAt(Object aValue, TreeNode node, int columnIndex) {
        }

        public boolean isCellEditable(TreeNode node, int columnIndex) {
            return false;
        }
        
    }
    
}

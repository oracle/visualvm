/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryCCTManager;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.results.memory.PresoObjLivenessCCTNode;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTableModel;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.utils.StringUtils;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class LivenessTreeTableView extends MemoryView {
    
    private LivenessTreeTableModel treeTableModel;
    private ProfilerTreeTable treeTable;
    
    public LivenessTreeTableView() {
        initUI();
    }
    
    
    protected ProfilerTable getResultsComponent() { return treeTable; }
    
    
    void setData(final LivenessMemoryResultsSnapshot snapshot, Collection filter, int aggregation) {
//        final boolean includeEmpty = filter != null;
        final boolean includeEmpty = false;
        
        int _nTrackedItems = snapshot.getNProfiledClasses();
        String[] _classNames = snapshot.getClassNames();
        long[] _nTrackedAllocObjects = snapshot.getNTrackedAllocObjects();
        long[] _objectsSizePerClass = snapshot.getObjectsSizePerClass();
        int[] _nTrackedLiveObjects = snapshot.getNTrackedLiveObjects();
        int[] _nTotalAllocObjects = snapshot.getnTotalAllocObjects();
        float[] _avgObjectAge = snapshot.getAvgObjectAge();
        int[] _maxSurvGen = snapshot.getMaxSurvGen();
        
        List<PresoObjLivenessCCTNode> nodes = new ArrayList();
        
        long totalLiveBytes = 0;
        long totalLiveObjects = 0;
        long totalTrackedAlloc = 0;
        long totalTotalAlloc = 0;
        
        for (int i = 0; i < _nTrackedItems; i++) {
            totalLiveBytes += _objectsSizePerClass[i];
            totalLiveObjects += _nTrackedLiveObjects[i];
            totalTrackedAlloc += _nTrackedAllocObjects[i];
            totalTotalAlloc += _nTotalAllocObjects[i];
            
            if ((!includeEmpty && _nTotalAllocObjects[i] > 0) || (includeEmpty && filter.contains(_classNames[i]))) {
                final int _i = i;
                PresoObjLivenessCCTNode node = new PresoObjLivenessCCTNode(_classNames[i], _nTrackedAllocObjects[i], _objectsSizePerClass[i], _nTrackedLiveObjects[i], _nTotalAllocObjects[i], _avgObjectAge[i], _maxSurvGen[i]) {
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
        
        renderers[0].setMaxValue(totalLiveBytes);
        renderers[1].setMaxValue(totalLiveObjects);
        renderers[2].setMaxValue(totalTrackedAlloc);
        renderers[3].setMaxValue(totalTotalAlloc);
        
        final PresoObjLivenessCCTNode root = PresoObjLivenessCCTNode.rootNode(nodes.toArray(new PresoObjLivenessCCTNode[nodes.size()]));
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                treeTableModel.setRoot(root);
            }
        });
    }
    
    
    ExportUtils.ExportProvider[] getExportProviders() {
        return treeTable.getRowCount() == 0 ? null : new ExportUtils.ExportProvider[] {
            new ExportUtils.CSVExportProvider(treeTable),
            new ExportUtils.HTMLExportProvider(treeTable, EXPORT_ALLOCATED_LIVE),
            new ExportUtils.XMLExportProvider(treeTable, EXPORT_ALLOCATED_LIVE),
            new ExportUtils.PNGExportProvider(treeTable.getParent())
        };
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection userValue);
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        treeTableModel = new LivenessTreeTableModel(PrestimeCPUCCTNode.EMPTY);
        
        treeTable = new ProfilerTreeTable(treeTableModel, true, true, new int[] { 0 }) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return LivenessTreeTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                LivenessTreeTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
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
        
        treeTable.setColumnVisibility(4, false);
        treeTable.setColumnVisibility(5, false);
        
        renderers = new HideableBarRenderer[4];
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[2] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[3] = new HideableBarRenderer(new NumberPercentRenderer() {
            public void setValue(Object value, int row) {
                if (((Number)value).longValue() == -1) {
                    super.setValue(null, row);
                } else {
                    super.setValue(value, row);
                }
            }
        });
        
        renderers[0].setMaxValue(123456789);
        renderers[1].setMaxValue(12345678);
        renderers[2].setMaxValue(12345678);
        renderers[3].setMaxValue(12345678);
        
        treeTable.setTreeCellRenderer(new MemoryJavaNameRenderer());
        treeTable.setColumnRenderer(1, renderers[0]);
        treeTable.setColumnRenderer(2, renderers[1]);
        treeTable.setColumnRenderer(3, renderers[2]);
        treeTable.setColumnRenderer(4, renderers[3]);
        treeTable.setColumnRenderer(5, new LabelRenderer() {
            public void setValue(Object value, int row) {
                super.setValue(StringUtils.floatPerCentToString(((Float)value).floatValue()), row);
            }
            public int getHorizontalAlignment() {
                return LabelRenderer.TRAILING;
            }
        });

        treeTable.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
        treeTable.setDefaultColumnWidth(2, renderers[1].getMaxNoBarWidth());
        treeTable.setDefaultColumnWidth(3, renderers[2].getMaxNoBarWidth());
        treeTable.setDefaultColumnWidth(4, renderers[3].getMaxNoBarWidth());
        treeTable.setDefaultColumnWidth(5, renderers[3].getNoBarWidth() - 25);
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(treeTable, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
        
        treeTable.setFiltersMode(false); // OR filter for results treetable
        treeTable.addRowFilter(new RowFilter() { // Do not filter first level nodes
            public boolean include(RowFilter.Entry entry) {
                PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)entry.getIdentifier();
                return node.getParent() != null && node.getParent().getParent() == null;
            }
        });
        FilterUtils.filterContains(treeTable, null); // Installs filter accepting all nodes by default
    }
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
        PresoObjLivenessCCTNode node = (PresoObjLivenessCCTNode)treeTable.getValueForRow(row);
        String[] name = node.getMethodClassNameAndSig();
        return new ClientUtils.SourceCodeSelection(name[0], name[1], name[2]);
    }
    
    
    private class LivenessTreeTableModel extends ProfilerTreeTableModel.Abstract {
        
        LivenessTreeTableModel(TreeNode root) {
            super(root);
        }
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return COLUMN_NAME;
            } else if (columnIndex == 1) {
                return COLUMN_LIVE_BYTES;
            } else if (columnIndex == 2) {
                return COLUMN_LIVE_OBJECTS;
            } else if (columnIndex == 3) {
                return COLUMN_ALLOCATED_OBJECTS;
            } else if (columnIndex == 4) {
                return COLUMN_TOTAL_ALLOCATED_OBJECTS;
            } else if (columnIndex == 5) {
                return COLUMN_AVG_AGE;
            } else if (columnIndex == 6) {
                return COLUMN_GENERATIONS;
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
            } else if (columnIndex == 3) {
                return Long.class;
            } else if (columnIndex == 4) {
                return Integer.class;
            } else if (columnIndex == 5) {
                return Float.class;
            } else if (columnIndex == 6) {
                return Integer.class;
            }
            return null;
        }

        public int getColumnCount() {
            return 7;
        }

        public Object getValueAt(TreeNode node, int columnIndex) {
            PresoObjLivenessCCTNode livenessNode = (PresoObjLivenessCCTNode)node;
            
            if (columnIndex == 0) {
                return livenessNode.getNodeName();
            } else if (columnIndex == 1) {
                return livenessNode.totalObjSize;
            } else if (columnIndex == 2) {
                return livenessNode.nLiveObjects;
            } else if (columnIndex == 3) {
                return livenessNode.nCalls;
            } else if (columnIndex == 4) {
                return livenessNode.nTotalAllocObjects;
            } else if (columnIndex == 5) {
                return livenessNode.avgObjectAge;
            } else if (columnIndex == 6) {
                return livenessNode.survGen;
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

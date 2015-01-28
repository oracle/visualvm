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

package org.netbeans.lib.profiler.ui.cpu;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTableModel;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.McsTimeRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class CPUTreeTableView extends DataView {
    
    private CPUTreeTableModel treeTableModel;
    private ProfilerTreeTable treeTable;
    
    private Map<Integer, ClientUtils.SourceCodeSelection> idMap;
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    private boolean sampled = true;
    private boolean twoTimeStamps;
    
    
    public CPUTreeTableView(Set<ClientUtils.SourceCodeSelection> selection) {
        this.selection = selection;
        
        initUI();
    }
    
    
    void setData(final CPUResultsSnapshot newData, final Map<Integer, ClientUtils.SourceCodeSelection> newIdMap, final int aggregation, final boolean _sampled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean structureChange = sampled != _sampled;
                sampled = _sampled;
                twoTimeStamps = newData == null ? false : newData.isCollectingTwoTimeStamps();
                idMap = newIdMap;
                if (treeTableModel != null) {
                    treeTableModel.setRoot(newData == null ? PrestimeCPUCCTNode.EMPTY :
                                           newData.getRootNode(aggregation));
                }
                if (structureChange) {
                    int col = treeTable.convertColumnIndexToView(selection == null ? 3 : 4);
                    String colN = treeTableModel.getColumnName(selection == null ? 3 : 4);
                    treeTable.getColumnModel().getColumn(col).setHeaderValue(colN);
                    repaint();
                }
            }
        });
    }
    
    public void resetData() {
        setData(null, null, -1, sampled);
    }
    
    
    public void showSelectionColumn() {
        treeTable.setColumnVisibility(0, true);
    }
    
    public void refreshSelection() {
        treeTableModel.dataChanged();
    }
    
    
    ExportUtils.ExportProvider[] getExportProviders() {
        return treeTable.getRowCount() == 0 ? null : new ExportUtils.ExportProvider[] {
            new ExportUtils.CSVExportProvider(treeTable),
            new ExportUtils.HTMLExportProvider(treeTable, "Methods - Call Tree"),
            new ExportUtils.XMLExportProvider(treeTable, "Methods - Call Tree"),
            new ExportUtils.PNGExportProvider(treeTable.getParent())
        };
    }
    
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    protected void popupShowing() {};
    
    protected void popupHidden()  {};
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        treeTableModel = new CPUTreeTableModel(PrestimeCPUCCTNode.EMPTY);
        
        int offset = selection == null ? -1 : 0;
        
        treeTable = new ProfilerTreeTable(treeTableModel, true, true, new int[] { 1 + offset }) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return CPUTreeTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                CPUTreeTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
            protected void popupShowing() {
                CPUTreeTableView.this.popupShowing();
            }
            protected void popupHidden() {
                CPUTreeTableView.this.popupHidden();
            }
        };
        
        treeTable.providePopupMenu(true);
        installDefaultAction();
        
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);
        treeTable.makeTreeAutoExpandable(2);
        
        treeTable.setMainColumn(1 + offset);
        treeTable.setFitWidthColumn(1 + offset);
        
        treeTable.setSortColumn(2 + offset);
        treeTable.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) treeTable.setColumnVisibility(0, false);
        treeTable.setColumnVisibility(4 + offset, false);
        
        renderers = new HideableBarRenderer[3];
        
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer())) {
            public void setValue(Object value, int row) {
                super.setMaxValue(getMaxValue(row, false));
                super.setValue(value, row);
            }
        };
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer())) {
            public void setValue(Object value, int row) {
                super.setMaxValue(getMaxValue(row, true));
                super.setValue(value, row);
            }
        };
        renderers[2] = new HideableBarRenderer(new NumberRenderer());
        
        long refTime = 123456;
        renderers[0].setMaxValue(refTime);
        renderers[1].setMaxValue(refTime);
        renderers[2].setMaxValue(refTime);
        
        if (selection != null) treeTable.setColumnRenderer(0, new CheckBoxRenderer() {
            private boolean visible;
            public void setValue(Object value, int row) {
                TreePath path = treeTable.getPathForRow(row);
                visible = isSelectable((PrestimeCPUCCTNode)path.getLastPathComponent());
                if (visible) super.setValue(value, row);
            }
            public void paint(Graphics g) {
                if (visible) {
                    super.paint(g);
                } else {
                    g.setColor(getBackground());
                    g.fillRect(0, 0, size.width, size.height);
                }
            }
        });
        treeTable.setTreeCellRenderer(new CPUJavaNameRenderer());
        treeTable.setColumnRenderer(2 + offset, renderers[0]);
        treeTable.setColumnRenderer(3 + offset, renderers[1]);
        treeTable.setColumnRenderer(4 + offset, renderers[2]);
        
        int w;
        if (selection != null) {
            w = new JLabel(treeTable.getColumnName(0)).getPreferredSize().width;
            treeTable.setDefaultColumnWidth(0, w + 15);
        }
        treeTable.setDefaultColumnWidth(2 + offset, renderers[0].getOptimalWidth());
        treeTable.setDefaultColumnWidth(3 + offset, renderers[1].getMaxNoBarWidth());
        
        sampled = !sampled;
        w = new JLabel(treeTable.getColumnName(4 + offset)).getPreferredSize().width;
        sampled = !sampled;
        w = Math.max(w, new JLabel(treeTable.getColumnName(4 + offset)).getPreferredSize().width);
        treeTable.setDefaultColumnWidth(4 + offset, Math.max(renderers[2].getNoBarWidth(), w + 15));
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(treeTable, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    
    protected ProfilerTable getResultsComponent() {
        return treeTable;
    }
    
    
    private long getMaxValue(int row, boolean secondary) {
        TreePath path = treeTable.getPathForRow(row);
        if (path.getPathCount() < 2) return 1;
        
        PrestimeCPUCCTNode node = (PrestimeCPUCCTNode)path.getPathComponent(1);
        return secondary ? node.getTotalTime1() : node.getTotalTime0();
    }
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
        PrestimeCPUCCTNode node = (PrestimeCPUCCTNode)treeTable.getValueForRow(row);
        if (node == null) return null;
        else if (node.isThreadNode() || node.isFilteredNode() || node.isSelfTimeNode()) return null;
//        else return selectionForId(node.getMethodId());
        else return idMap.get(node.getMethodId());
    }
    
//    private ClientUtils.SourceCodeSelection selectionForId(int methodId) {
//        ProfilingSessionStatus sessionStatus = client.getStatus();
//        sessionStatus.beginTrans(false);
//        try {
//            String className = sessionStatus.getInstrMethodClasses()[methodId];
//            String methodName = sessionStatus.getInstrMethodNames()[methodId];
//            String methodSig = sessionStatus.getInstrMethodSignatures()[methodId];
//            return new ClientUtils.SourceCodeSelection(className, methodName, methodSig);
//        } finally {
//            sessionStatus.endTrans();
//        }
//    }
    
    private static boolean isSelectable(PrestimeCPUCCTNode node) {
        if (node.isThreadNode() || node.isFilteredNode() || node.isSelfTimeNode()) return false;
        if (node.getMethodClassNameAndSig()[1].endsWith("[native]")) return false; // NOI18N
        return true;
    }
    
    
    private class CPUTreeTableModel extends ProfilerTreeTableModel.Abstract {
        
        CPUTreeTableModel(TreeNode root) {
            super(root);
        }
        
        public String getColumnName(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return "Name";
            } else if (columnIndex == 2) {
                return "Total Time";
            } else if (columnIndex == 3) {
                return "Total Time (CPU)";
            } else if (columnIndex == 4) {
                return sampled ? "Hits" : "Invocations";
            } else if (columnIndex == 0) {
                return "Selected";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return JTree.class;
            } else if (columnIndex == 4) {
                return Integer.class;
            } else if (columnIndex == 0) {
                return Boolean.class;
            } else {
                return Long.class;
            }
        }

        public int getColumnCount() {
            return selection == null ? 4 : 5;
        }

        public Object getValueAt(TreeNode node, int columnIndex) {
            PrestimeCPUCCTNode cpuNode = (PrestimeCPUCCTNode)node;
            
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return cpuNode.getNodeName();
            } else if (columnIndex == 2) {
                return cpuNode.getTotalTime0();
            } else if (columnIndex == 3) {
                return twoTimeStamps ? cpuNode.getTotalTime1() : 0;
            } else if (columnIndex == 4) {
                return cpuNode.getNCalls();
            } else if (columnIndex == 0) {
                if (selection.isEmpty()) return Boolean.FALSE;
                return selection.contains(idMap.get(cpuNode.getMethodId()));
            }

            return null;
        }
        
        public void setValueAt(Object aValue, TreeNode node, int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 0) {
                PrestimeCPUCCTNode cpuNode = (PrestimeCPUCCTNode)node;
                int methodId = cpuNode.getMethodId();
                if (Boolean.TRUE.equals(aValue)) selection.add(idMap.get(methodId));
                else selection.remove(idMap.get(methodId));
            }
        }

        public boolean isCellEditable(TreeNode node, int columnIndex) {
            if (selection == null) columnIndex++;
            if (columnIndex != 0) return false;
            return (isSelectable((PrestimeCPUCCTNode)node));
        }
        
    }
    
}

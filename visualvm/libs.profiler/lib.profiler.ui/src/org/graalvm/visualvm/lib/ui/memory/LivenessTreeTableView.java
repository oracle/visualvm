/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.memory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.PrestimeCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsDiff;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCCTManager;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjLivenessCCTNode;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.ExportUtils;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.CheckBoxRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class LivenessTreeTableView extends MemoryView {
    
    private LivenessTreeTableModel treeTableModel;
    private ProfilerTreeTable treeTable;
    
    private Map<TreeNode, ClientUtils.SourceCodeSelection> nodesMap;
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    private final boolean includeTotalAllocs;
    
    private boolean filterObjects = true;
    private boolean filterAllocations = false;
    private boolean searchObjects = true;
    private boolean searchAllocations = false;
    
    
    LivenessTreeTableView(Set<ClientUtils.SourceCodeSelection> selection, boolean includeTotalAllocs) {
        this.selection = selection;
        
        this.includeTotalAllocs = includeTotalAllocs;
        
        initUI();
    }
    
    
    protected RowFilter getExcludesFilter() {
        return new RowFilter() { // Do not filter first level nodes
            public boolean include(RowFilter.Entry entry) {
                PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)entry.getIdentifier();
                CCTNode parent = node.getParent();
                if (parent == null) return true;
                if (parent.getParent() == null) return !filterObjects;
                return !filterAllocations;
            }
        };
    }
    
    protected Component[] getFilterOptions() {
        PopupButton pb = new PopupButton (Icons.getIcon(ProfilerIcons.TAB_CALL_TREE)) {
            protected void populatePopup(JPopupMenu popup) {
                popup.add(new JCheckBoxMenuItem(SEARCH_CLASSES_SCOPE, filterObjects) {
                    {
                        if (!filterAllocations) setEnabled(false);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterObjects = !filterObjects;
                        enableFilter();
                    }
                });
                popup.add(new JCheckBoxMenuItem(FILTER_ALLOCATIONS_SCOPE, filterAllocations) {
                    {
                        if (!filterObjects) setEnabled(false);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        filterAllocations = !filterAllocations;
                        enableFilter();
                    }
                });
            }
        };
        pb.setToolTipText(FILTER_SCOPE_TOOLTIP);
        return new Component[] { Box.createHorizontalStrut(5), pb };
    }
    
    protected SearchUtils.TreeHelper getSearchHelper() {
        return new SearchUtils.TreeHelper() {
            public int getNodeType(TreeNode tnode) {
                PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)tnode;
                CCTNode parent = node.getParent();
                if (parent == null) return SearchUtils.TreeHelper.NODE_SKIP_DOWN; // invisible root
                
                if (parent.getParent() == null) {
                    if (searchObjects) {
                        return searchAllocations ? SearchUtils.TreeHelper.NODE_SEARCH_DOWN :
                                                   SearchUtils.TreeHelper.NODE_SEARCH_NEXT;
                    } else {
                        return searchAllocations ? SearchUtils.TreeHelper.NODE_SKIP_DOWN :
                                                   SearchUtils.TreeHelper.NODE_SKIP_NEXT;
                    }
                }
                
                return searchAllocations ? SearchUtils.TreeHelper.NODE_SEARCH_DOWN :
                                           SearchUtils.TreeHelper.NODE_SKIP_NEXT;
            }
        };
    }
    
    protected Component[] getSearchOptions() {
        PopupButton pb = new PopupButton (Icons.getIcon(ProfilerIcons.TAB_CALL_TREE)) {
            protected void populatePopup(JPopupMenu popup) {
                popup.add(new JCheckBoxMenuItem(SEARCH_CLASSES_SCOPE, searchObjects) {
                    {
                        if (!searchAllocations) setEnabled(false);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        searchObjects = !searchObjects;
                    }
                });
                popup.add(new JCheckBoxMenuItem(SEARCH_ALLOCATIONS_SCOPE, searchAllocations) {
                    {
                        if (!searchObjects) setEnabled(false);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        searchAllocations = !searchAllocations;
                    }
                });
            }
        };
        pb.setToolTipText(SEARCH_SCOPE_TOOLTIP);
        return new Component[] { Box.createHorizontalStrut(5), pb };
    }
    
    protected ProfilerTable getResultsComponent() { return treeTable; }
    
    
    public void setData(MemoryResultsSnapshot snapshot, GenericFilter filter, int aggregation) {
        final boolean includeEmpty = filter != null;
//        final boolean includeEmpty = false;
        final boolean diff = snapshot instanceof LivenessMemoryResultsDiff;
        final LivenessMemoryResultsSnapshot _snapshot = (LivenessMemoryResultsSnapshot)snapshot;
        
        String[] _classNames = _snapshot.getClassNames();
        long[] _nTrackedAllocObjects = _snapshot.getNTrackedAllocObjects();
        long[] _objectsSizePerClass = _snapshot.getObjectsSizePerClass();
        int[] _nTrackedLiveObjects = _snapshot.getNTrackedLiveObjects();
        int[] _nTotalAllocObjects = _snapshot.getnTotalAllocObjects();
        float[] _avgObjectAge = _snapshot.getAvgObjectAge();
        int[] _maxSurvGen = _snapshot.getMaxSurvGen();
        
        int _nTrackedItems = Math.min(_snapshot.getNProfiledClasses(), _classNames.length);
        _nTrackedItems = Math.min(_nTrackedItems, _nTotalAllocObjects.length);
        
        List<PresoObjLivenessCCTNode> nodes = new ArrayList();
        final Map<TreeNode, ClientUtils.SourceCodeSelection> _nodesMap = new HashMap();
        
        long totalLiveBytes = 0;
        long _totalLiveBytes = 0;
        long totalLiveObjects = 0;
        long _totalLiveObjects = 0;
        long totalTrackedAlloc = 0;
        long _totalTrackedAlloc = 0;
        long totalTotalAlloc = 0;
        long _totalTotalAlloc = 0;
        
        for (int i = 0; i < _nTrackedItems; i++) {
            if (diff) {
                totalLiveBytes = Math.max(totalLiveBytes, _objectsSizePerClass[i]);
                _totalLiveBytes = Math.min(_totalLiveBytes, _objectsSizePerClass[i]);
                totalLiveObjects = Math.max(totalLiveObjects, _nTrackedLiveObjects[i]);
                _totalLiveObjects = Math.min(_totalLiveObjects, _nTrackedLiveObjects[i]);
                totalTrackedAlloc = Math.max(totalTrackedAlloc, _nTrackedAllocObjects[i]);
                _totalTrackedAlloc = Math.min(_totalTrackedAlloc, _nTrackedAllocObjects[i]);
                if (includeTotalAllocs) {
                    totalTotalAlloc = Math.max(totalTotalAlloc, _nTotalAllocObjects[i]);
                    _totalTotalAlloc = Math.min(_totalTotalAlloc, _nTotalAllocObjects[i]);
                }
            } else {
                totalLiveBytes += _objectsSizePerClass[i];
                totalLiveObjects += _nTrackedLiveObjects[i];
                totalTrackedAlloc += _nTrackedAllocObjects[i];
                if (includeTotalAllocs) totalTotalAlloc += _nTotalAllocObjects[i];
            }
            
            final int _i = i;
            
            class Node extends PresoObjLivenessCCTNode {
                Node(String className, long nTrackedAllocObjects, long objectsSizePerClass, int nTrackedLiveObjects, int nTotalAllocObjects, float avgObjectAge, int maxSurvGen) {
                    super(className, nTrackedAllocObjects, objectsSizePerClass, nTrackedLiveObjects, nTotalAllocObjects, avgObjectAge, maxSurvGen);
                }
                public CCTNode[] getChildren() {
                    if (children == null) {
                        MemoryCCTManager callGraphManager = new MemoryCCTManager(_snapshot, _i, true);
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
            }
            
            if (!includeEmpty) { // old snapshot
                if (_nTrackedLiveObjects[i] > 0) {
                    PresoObjLivenessCCTNode node = new Node(_classNames[i], _nTrackedAllocObjects[i], _objectsSizePerClass[i], _nTrackedLiveObjects[i], _nTotalAllocObjects[i], _avgObjectAge[i], _maxSurvGen[i]);
                    nodes.add(node);
                    _nodesMap.put(node, new ClientUtils.SourceCodeSelection(_classNames[i], Wildcards.ALLWILDCARD, null));
                }
            } else if (filter.passes(_classNames[i].replace('.', '/'))) { // NOI18N
                PresoObjLivenessCCTNode node = new Node(_classNames[i], _nTrackedAllocObjects[i], _objectsSizePerClass[i], _nTrackedLiveObjects[i], _nTotalAllocObjects[i], _avgObjectAge[i], _maxSurvGen[i]);
                nodes.add(node);
                _nodesMap.put(node, new ClientUtils.SourceCodeSelection(_classNames[i], Wildcards.ALLWILDCARD, null));
            }
        }
        
        final long __totalLiveBytes = !diff ? totalLiveBytes :
                Math.max(Math.abs(totalLiveBytes), Math.abs(_totalLiveBytes));
        final long __totalLiveObjects = !diff ? totalLiveObjects :
                Math.max(Math.abs(totalLiveObjects), Math.abs(_totalLiveObjects));
        final long __totalTrackedAlloc = !diff ? totalTrackedAlloc :
                Math.max(Math.abs(totalTrackedAlloc), Math.abs(_totalTrackedAlloc));
        final long __totalTotalAlloc = !diff ? totalTotalAlloc :
                Math.max(Math.abs(totalTotalAlloc), Math.abs(_totalTotalAlloc));
        final PresoObjLivenessCCTNode root = PresoObjLivenessCCTNode.rootNode(nodes.toArray(new PresoObjLivenessCCTNode[0]));
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nodesMap = _nodesMap;
                renderers[0].setMaxValue(__totalLiveBytes);
                renderers[1].setMaxValue(__totalLiveObjects);
                renderers[2].setMaxValue(__totalTrackedAlloc);
                if (includeTotalAllocs) renderers[3].setMaxValue(__totalTotalAlloc);
                
                renderers[0].setDiffMode(diff);
                renderers[1].setDiffMode(diff);
                renderers[2].setDiffMode(diff);
                if (includeTotalAllocs) renderers[3].setDiffMode(diff);
                
                renderersEx[0].setDiffMode(diff);
                renderersEx[1].setDiffMode(diff);
                    
                treeTableModel.setRoot(root);
            }
        });
    }
    
    
    public void resetData() {
        final PresoObjLivenessCCTNode root = PresoObjLivenessCCTNode.rootNode(new PresoObjLivenessCCTNode[0]);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nodesMap = null;
                
                renderers[0].setMaxValue(0);
                renderers[1].setMaxValue(0);
                renderers[2].setMaxValue(0);
                if (includeTotalAllocs) renderers[3].setMaxValue(0);

                renderers[0].setDiffMode(false);
                renderers[1].setDiffMode(false);
                renderers[2].setDiffMode(false);
                if (includeTotalAllocs) renderers[3].setDiffMode(false);

                renderersEx[0].setDiffMode(false);
                renderersEx[1].setDiffMode(false);
                
                treeTableModel.setRoot(root);
            }
        });
    }
    
    
    public void showSelectionColumn() {
        treeTable.setColumnVisibility(0, true);
    }
    
    public void refreshSelection() {
        treeTableModel.dataChanged();
    }
    
    
    public ExportUtils.ExportProvider[] getExportProviders() {
        return treeTable.getRowCount() == 0 ? null : new ExportUtils.ExportProvider[] {
            new ExportUtils.CSVExportProvider(treeTable),
            new ExportUtils.HTMLExportProvider(treeTable, EXPORT_ALLOCATED_LIVE),
            new ExportUtils.XMLExportProvider(treeTable, EXPORT_ALLOCATED_LIVE),
            new ExportUtils.PNGExportProvider(treeTable)
        };
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection userValue);
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    protected void popupShowing() {};
    
    protected void popupHidden()  {};
    
    
    private HideableBarRenderer[] renderers;
    private NumberRenderer[] renderersEx;
    
    private void initUI() {
        final int offset = selection == null ? -1 : 0;
        
        treeTableModel = new LivenessTreeTableModel(PrestimeCPUCCTNode.EMPTY);
        
        treeTable = new ProfilerTreeTable(treeTableModel, true, true, new int[] { 1 + offset }) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return LivenessTreeTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                LivenessTreeTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
            protected void popupShowing() {
                LivenessTreeTableView.this.popupShowing();
            }
            protected void popupHidden() {
                LivenessTreeTableView.this.popupHidden();
            }
        };
        
        treeTable.setColumnToolTips(selection == null ? new String[] {
                                  NAME_COLUMN_TOOLTIP,
                                  LIVE_SIZE_COLUMN_TOOLTIP,
                                  LIVE_COUNT_COLUMN_TOOLTIP,
                                  ALLOC_COUNT_COLUMN_TOOLTIP,
                                  AVG_AGE_COLUMN_TOOLTIP,
                                  GENERATIONS_COLUMN_TOOLTIP,
                                  TOTAL_ALLOC_COUNT_COLUMN_TOOLTIP,
                                } : new String[] {
                                  SELECTED_COLUMN_TOOLTIP,
                                  NAME_COLUMN_TOOLTIP,
                                  LIVE_SIZE_COLUMN_TOOLTIP,
                                  LIVE_COUNT_COLUMN_TOOLTIP,
                                  ALLOC_COUNT_COLUMN_TOOLTIP,
                                  AVG_AGE_COLUMN_TOOLTIP,
                                  GENERATIONS_COLUMN_TOOLTIP,
                                  TOTAL_ALLOC_COUNT_COLUMN_TOOLTIP,
                                });
        
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
        
        treeTable.setMainColumn(1 + offset);
        treeTable.setFitWidthColumn(1 + offset);
        
        treeTable.setSortColumn(2 + offset);
        treeTable.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) treeTable.setColumnVisibility(0, false);
        treeTable.setColumnVisibility(5 + offset, false);
        if (includeTotalAllocs) treeTable.setColumnVisibility(7 + offset, false);
        
        renderers = new HideableBarRenderer[4];
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[2] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[3] = new HideableBarRenderer(new NumberPercentRenderer() {
            public void setValue(Object value, int row) {
                if (value == null || ((Number)value).longValue() == -1) {
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
        
        renderersEx = new NumberRenderer[2];
        renderersEx[0] = new NumberRenderer() {
            protected String getValueString(Object value, int row, Format format) {
                if (value == null) return "-"; // NOI18N
                float _value = ((Float)value).floatValue();
                String s = StringUtils.floatPerCentToString(_value);
                if (renderingDiff && _value >= 0) s = '+' + s; // NOI18N
                return s;
            }
        };
        renderersEx[1] = new NumberRenderer();
        
        if (selection != null) treeTable.setColumnRenderer(0, new CheckBoxRenderer() {
            private boolean visible;
            public void setValue(Object value, int row) {
                TreePath path = treeTable.getPathForRow(row);
                visible = nodesMap.containsKey((TreeNode)path.getLastPathComponent());
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
        treeTable.setTreeCellRenderer(new MemoryJavaNameRenderer());
        treeTable.setColumnRenderer(2 + offset, renderers[0]);
        treeTable.setColumnRenderer(3 + offset, renderers[1]);
        treeTable.setColumnRenderer(4 + offset, renderers[2]);
        treeTable.setColumnRenderer(5 + offset, renderersEx[0]);
        treeTable.setColumnRenderer(6 + offset, renderersEx[1]);
        if (includeTotalAllocs) treeTable.setColumnRenderer(7 + offset, renderers[3]);

        if (selection != null) {
            int w = new JLabel(treeTable.getColumnName(0)).getPreferredSize().width;
            treeTable.setDefaultColumnWidth(0, w + 15);
        }
        treeTable.setDefaultColumnWidth(2 + offset, renderers[0].getOptimalWidth());
        treeTable.setDefaultColumnWidth(3 + offset, renderers[1].getMaxNoBarWidth());
        treeTable.setDefaultColumnWidth(4 + offset, renderers[2].getMaxNoBarWidth());
        treeTable.setDefaultColumnWidth(5 + offset, renderers[2].getNoBarWidth() - 25);
        if (includeTotalAllocs) treeTable.setDefaultColumnWidth(7 + offset, renderers[3].getMaxNoBarWidth());
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(treeTable, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
        PresoObjLivenessCCTNode node = (PresoObjLivenessCCTNode)treeTable.getValueForRow(row);
        if (node == null || node.isFiltered()) return null;
        String[] name = node.getMethodClassNameAndSig();
        return new ClientUtils.SourceCodeSelection(name[0], name[1], name[2]);
    }
    
    
    private class LivenessTreeTableModel extends ProfilerTreeTableModel.Abstract {
        
        private final int columns = 6 +
                (selection == null ? 0 : 1) +
                (includeTotalAllocs ? 1 : 0);
        
        LivenessTreeTableModel(TreeNode root) {
            super(root);
        }
        
        public String getColumnName(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return COLUMN_NAME;
            } else if (columnIndex == 2) {
                return COLUMN_LIVE_BYTES;
            } else if (columnIndex == 3) {
                return COLUMN_LIVE_OBJECTS;
            } else if (columnIndex == 4) {
                return COLUMN_ALLOCATED_OBJECTS;
            } else if (columnIndex == 5) {
                return COLUMN_AVG_AGE;
            } else if (columnIndex == 6) {
                return COLUMN_GENERATIONS;
            } else if (columnIndex == 7) {
                return COLUMN_TOTAL_ALLOCATED_OBJECTS;
            } else if (columnIndex == 0) {
                return COLUMN_SELECTED;
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return JTree.class;
            } else if (columnIndex == 2) {
                return Long.class;
            } else if (columnIndex == 3) {
                return Integer.class;
            } else if (columnIndex == 4) {
                return Long.class;
            } else if (columnIndex == 5) {
                return Float.class;
            } else if (columnIndex == 6) {
                return Integer.class;
            } else if (columnIndex == 7) {
                return Integer.class;
            } else if (columnIndex == 0) {
                return Boolean.class;
            }
            return null;
        }

        public int getColumnCount() {
            return columns;
        }

        public Object getValueAt(TreeNode node, int columnIndex) {
            PresoObjLivenessCCTNode livenessNode = (PresoObjLivenessCCTNode)node;
            
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return livenessNode.getNodeName();
            } else if (columnIndex == 2) {
                return livenessNode.totalObjSize;
            } else if (columnIndex == 3) {
                return livenessNode.nLiveObjects;
            } else if (columnIndex == 4) {
                return livenessNode.nCalls;
            } else if (columnIndex == 5) {
                return livenessNode.avgObjectAge;
            } else if (columnIndex == 6) {
                return livenessNode.survGen;
            } else if (columnIndex == 7) {
                return livenessNode.nTotalAllocObjects;
            } else if (columnIndex == 0) {
                if (selection.isEmpty()) return Boolean.FALSE;
                return selection.contains(nodesMap.get(node));
            }

            return null;
        }
        
        public void setValueAt(Object aValue, TreeNode node, int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 0) {
                if (Boolean.TRUE.equals(aValue)) selection.add(nodesMap.get(node));
                else selection.remove(nodesMap.get(node));
            }
        }

        public boolean isCellEditable(TreeNode node, int columnIndex) {
            if (selection == null) columnIndex++;
            if (columnIndex != 0) return false;
            return (nodesMap.containsKey(node));
        }
        
    }
    
}

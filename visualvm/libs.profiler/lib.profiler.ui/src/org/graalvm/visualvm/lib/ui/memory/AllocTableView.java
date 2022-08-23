/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.results.memory.AllocMemoryResultsDiff;
import org.graalvm.visualvm.lib.jfluid.results.memory.AllocMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.ExportUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.renderer.CheckBoxRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class AllocTableView extends MemoryView {

    private MemoryTableModel tableModel;
    private ProfilerTable table;

    private int nTrackedItems;
    private ClientUtils.SourceCodeSelection[] classNames;
    private int[] nTotalAllocObjects;
    private long[] totalAllocObjectsSize;

    private final Set<ClientUtils.SourceCodeSelection> selection;

    private boolean filterZeroItems = true;
    
    
    AllocTableView(Set<ClientUtils.SourceCodeSelection> selection) {
        this.selection = selection;
        
        initUI();
    }
    
    
    protected ProfilerTable getResultsComponent() { return table; }
    
    
    private void setData(final int _nTrackedItems, final String[] _classNames,
                 final int[] _nTotalAllocObjects, final long[] _totalAllocObjectsSize, final boolean diff) {
        
        // TODO: show classes with zero instances in live results!
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (tableModel != null) {
                    nTrackedItems = _nTrackedItems;
                    classNames = new ClientUtils.SourceCodeSelection[_classNames.length];
                    for (int i = 0; i < classNames.length; i++)
                        classNames[i] = new ClientUtils.SourceCodeSelection(_classNames[i], Wildcards.ALLWILDCARD, null);
                    nTotalAllocObjects = _nTotalAllocObjects;
                    totalAllocObjectsSize = _totalAllocObjectsSize;
                    
                    long totalObjects = 0;
                    long _totalObjects = 0;
                    long totalBytes = 0;
                    long _totalBytes = 0;
                    
                    for (int i = 0; i < nTrackedItems; i++) {
                        if (diff) {
                            totalObjects = Math.max(totalObjects, nTotalAllocObjects[i]);
                            _totalObjects = Math.min(_totalObjects, nTotalAllocObjects[i]);
                            totalBytes = Math.max(totalBytes, totalAllocObjectsSize[i]);
                            _totalBytes = Math.min(_totalBytes, totalAllocObjectsSize[i]);
                        } else {
                            totalObjects += nTotalAllocObjects[i];
                            totalBytes += totalAllocObjectsSize[i];
                        }
                    }
                    if (diff) {
                        renderers[0].setMaxValue(Math.max(Math.abs(totalBytes), Math.abs(_totalBytes)));
                        renderers[1].setMaxValue(Math.max(Math.abs(totalObjects), Math.abs(_totalObjects)));
                    } else {
                        renderers[0].setMaxValue(totalBytes);
                        renderers[1].setMaxValue(totalObjects);
                    }
                    
                    renderers[0].setDiffMode(diff);
                    renderers[1].setDiffMode(diff);
                    
                    tableModel.fireTableDataChanged();
                }
            }
        });
    }
    
    public void setData(MemoryResultsSnapshot snapshot, GenericFilter filter, int aggregation) {
        AllocMemoryResultsSnapshot _snapshot = (AllocMemoryResultsSnapshot)snapshot;
        boolean diff = _snapshot instanceof AllocMemoryResultsDiff;
        
        String[] _classNames = _snapshot.getClassNames();
        int[] _nTotalAllocObjects = _snapshot.getObjectsCounts();
        long[] _totalAllocObjectsSize = _snapshot.getObjectsSizePerClass();
        
        int _nTrackedItems = Math.min(_snapshot.getNProfiledClasses(), _classNames.length);
        _nTrackedItems = Math.min(_nTrackedItems, _nTotalAllocObjects.length);
        
        if (filter == null) { // old snapshot
            filterZeroItems = !diff;
            
            setData(_nTrackedItems, _classNames, _nTotalAllocObjects, _totalAllocObjectsSize, diff);
        } else { // new snapshot or live results
            filterZeroItems = false;
            
            List<String> fClassNames = new ArrayList();
            List<Integer> fTotalAllocObjects = new ArrayList();
            List<Long> fTotalAllocObjectsSize = new ArrayList();
            
            for (int i = 0; i < _nTrackedItems; i++) {
                if (filter.passes(_classNames[i].replace('.', '/'))) { // NOI18N
                    fClassNames.add(_classNames[i]);
                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                    fTotalAllocObjectsSize.add(_totalAllocObjectsSize[i]);
                }
            }
            
            int trackedItems = fClassNames.size();
            String[] aClassNames = fClassNames.toArray(new String[0]);
            
            int[] aTotalAllocObjects = new int[trackedItems];
            for (int i = 0; i < trackedItems; i++) aTotalAllocObjects[i] = fTotalAllocObjects.get(i);
            
            long[] aTotalAllocObjectsSize = new long[trackedItems];
            for (int i = 0; i < trackedItems; i++) aTotalAllocObjectsSize[i] = fTotalAllocObjectsSize.get(i);
            
            setData(trackedItems, aClassNames, aTotalAllocObjects, aTotalAllocObjectsSize, diff);
        }
    }
    
    public void resetData() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nTrackedItems = 0;
                classNames = null;
                nTotalAllocObjects = null;
                totalAllocObjectsSize = null;
                
                renderers[0].setMaxValue(0);
                renderers[1].setMaxValue(0);
                renderers[0].setDiffMode(false);
                renderers[1].setDiffMode(false);
                
                tableModel.fireTableDataChanged();
            }
        });
    }
    
    
    public void showSelectionColumn() {
        table.setColumnVisibility(0, true);
    }
    
    public void refreshSelection() {
        tableModel.fireTableDataChanged();
    }
    
    
    public ExportUtils.ExportProvider[] getExportProviders() {
        return table.getRowCount() == 0 ? null : new ExportUtils.ExportProvider[] {
            new ExportUtils.CSVExportProvider(table),
            new ExportUtils.HTMLExportProvider(table, EXPORT_ALLOCATED),
            new ExportUtils.XMLExportProvider(table, EXPORT_ALLOCATED),
            new ExportUtils.PNGExportProvider(table)
        };
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection userValue);
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    protected void popupShowing() {};
    
    protected void popupHidden()  {};
    
    
    private HideableBarRenderer[] renderers;
    
    HideableBarRenderer.BarDiffMode barDiffMode() {
        return HideableBarRenderer.BarDiffMode.MODE_BAR_DIFF;
    }
    
    private void initUI() {
        final int offset = selection == null ? -1 : 0;
        
        tableModel = new MemoryTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return AllocTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                AllocTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
            protected void popupShowing() {
                AllocTableView.this.popupShowing();
            }
            protected void popupHidden() {
                AllocTableView.this.popupHidden();
            }
        };
        
        table.setColumnToolTips(selection == null ? new String[] {
                                  NAME_COLUMN_TOOLTIP,
                                  ALLOC_SIZE_COLUMN_TOOLTIP,
                                  ALLOC_COUNT_COLUMN_TOOLTIP
                                } : new String[] {
                                  SELECTED_COLUMN_TOOLTIP,
                                  NAME_COLUMN_TOOLTIP,
                                  ALLOC_SIZE_COLUMN_TOOLTIP,
                                  ALLOC_COUNT_COLUMN_TOOLTIP
                                });
        
        table.providePopupMenu(true);
        installDefaultAction();
        
        table.setMainColumn(1 + offset);
        table.setFitWidthColumn(1 + offset);
        
        table.setSortColumn(2 + offset);
        table.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) table.setColumnVisibility(0, false);
        
        // Filter out classes with no instances
        table.addRowFilter(new RowFilter() {
            public boolean include(RowFilter.Entry entry) {
                return !filterZeroItems || ((Number)entry.getValue(3 + offset)).intValue() > 0;
            }
        });
        
        renderers = new HideableBarRenderer[2];
        
        HideableBarRenderer.BarDiffMode barDiffMode = barDiffMode();
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[0].setBarDiffMode(barDiffMode);
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[1].setBarDiffMode(barDiffMode);
        
        renderers[0].setMaxValue(123456789);
        renderers[1].setMaxValue(12345678);
        
        if (selection != null) table.setColumnRenderer(0, new CheckBoxRenderer());
        table.setColumnRenderer(1 + offset, new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS)));
        table.setColumnRenderer(2 + offset, renderers[0]);
        table.setColumnRenderer(3 + offset, renderers[1]);
        
        if (selection != null) {
            int w = new JLabel(table.getColumnName(0)).getPreferredSize().width;
            table.setDefaultColumnWidth(0, w + 15);
        }
        table.setDefaultColumnWidth(2 + offset, renderers[0].getOptimalWidth());
        table.setDefaultColumnWidth(3 + offset, renderers[1].getMaxNoBarWidth());
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
        if (nTrackedItems == 0 || row == -1) return null;
        if (row >= tableModel.getRowCount()) return null; // #239936
        return classNames[table.convertRowIndexToModel(row)];
    }
    
    
    private class MemoryTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return COLUMN_NAME;
            } else if (columnIndex == 2) {
                return COLUMN_ALLOCATED_BYTES;
            } else if (columnIndex == 3) {
                return COLUMN_ALLOCATED_OBJECTS;
            } else if (columnIndex == 0) {
                return COLUMN_SELECTED;
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return String.class;
            } else if (columnIndex == 2) {
                return Long.class;
            } else if (columnIndex == 3) {
                return Integer.class;
            } else if (columnIndex == 0) {
                return Boolean.class;
            }
            return null;
        }

        public int getRowCount() {
            return nTrackedItems;
        }

        public int getColumnCount() {
            return selection == null ? 3 : 4;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (nTrackedItems == 0) return null;
            
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return classNames[rowIndex].getClassName();
            } else if (columnIndex == 2) {
                return totalAllocObjectsSize[rowIndex];
            } else if (columnIndex == 3) {
                return nTotalAllocObjects[rowIndex];
            } else if (columnIndex == 0) {
                if (selection.isEmpty()) return Boolean.FALSE;
                return selection.contains(classNames[rowIndex]);
            }

            return null;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 0) {
                if (Boolean.FALSE.equals(aValue)) selection.remove(classNames[rowIndex]);
                else selection.add(classNames[rowIndex]);
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (selection == null) columnIndex++;
            
            return columnIndex == 0;
        }
        
    }
    
}

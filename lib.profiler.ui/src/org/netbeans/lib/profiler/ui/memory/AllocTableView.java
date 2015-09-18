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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;

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
    
    
    public AllocTableView(Set<ClientUtils.SourceCodeSelection> selection) {
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
    
    public void setData(MemoryResultsSnapshot snapshot, Collection<String> filter, int aggregation) {
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
            
            if (isAll(filter)) {
                for (int i = 0; i < _nTrackedItems; i++) {
                    fClassNames.add(_classNames[i]);
                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                    fTotalAllocObjectsSize.add(_totalAllocObjectsSize[i]);
                }
            } else if (isExact(filter)) {
                for (int i = 0; i < _nTrackedItems; i++) {
                    if (filter.contains(_classNames[i])) {
                        fClassNames.add(_classNames[i]);
                        fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                        fTotalAllocObjectsSize.add(_totalAllocObjectsSize[i]);
                    }
                }
            } else {
                for (String f : filter) {
                    if (f.endsWith("**")) { // NOI18N
                        f = f.substring(0, f.length() - 2);
                        for (int i = 0; i < _nTrackedItems; i++) {
                            if (_classNames[i].startsWith(f)) {
                                fClassNames.add(_classNames[i]);
                                fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                                fTotalAllocObjectsSize.add(_totalAllocObjectsSize[i]);
                            }
                        }
                    } else if (f.endsWith("*")) { // NOI18N
                        f = f.substring(0, f.length() - 1);
                        for (int i = 0; i < _nTrackedItems; i++) {
                            if (!_classNames[i].startsWith(f)) continue;
                            
                            boolean subpackage = false;
                            for (int ii = f.length(); ii < _classNames[i].length(); ii++)
                                if (_classNames[i].charAt(ii) == '.') { // NOI18N
                                    subpackage = true;
                                    break;
                                }
                            
                            if (!subpackage) {
                                fClassNames.add(_classNames[i]);
                                fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                                fTotalAllocObjectsSize.add(_totalAllocObjectsSize[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < _nTrackedItems; i++) {
                            if (_classNames[i].equals(f)) {
                                fClassNames.add(_classNames[i]);
                                fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                                fTotalAllocObjectsSize.add(_totalAllocObjectsSize[i]);
                            }
                        }
                    }
                }
            }
            
            int trackedItems = fClassNames.size();
            String[] aClassNames = fClassNames.toArray(new String[trackedItems]);
            
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
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        
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

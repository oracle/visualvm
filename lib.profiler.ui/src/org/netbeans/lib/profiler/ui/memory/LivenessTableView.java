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
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.lib.profiler.utils.Wildcards;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class LivenessTableView extends JPanel {
    
    private MemoryTableModel tableModel;
    private ProfilerTable table;
    
    private int nTrackedItems;
    private ClientUtils.SourceCodeSelection[] classNames;
    private int[] nTrackedLiveObjects;
    private long[] trackedLiveObjectsSize;
    private long[] nTrackedAllocObjects;
    private float[] avgObjectAge;
    private int[] maxSurvGen;
    private int[] nTotalAllocObjects;
    
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    private boolean filterZeroItems = true;
    
    
    public LivenessTableView(Set<ClientUtils.SourceCodeSelection> selection) {
        this.selection = selection;
        
        initUI();
    }
    
    
    void setData(final int _nTrackedItems, final String[] _classNames,
                 final int[] _nTrackedLiveObjects, final long[] _trackedLiveObjectsSize,
                 final long[] _nTrackedAllocObjects, final float[] _avgObjectAge,
                 final int[] _maxSurvGen, final int[] _nTotalAllocObjects) {
        
        // TODO: show classes with zero instances in live results!
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (tableModel != null) {
                    nTrackedItems = _nTrackedItems;
                    classNames = new ClientUtils.SourceCodeSelection[_classNames.length];
                    for (int i = 0; i < classNames.length; i++)
                        classNames[i] = new ClientUtils.SourceCodeSelection(_classNames[i], Wildcards.ALLWILDCARD, null);
                    nTrackedLiveObjects = _nTrackedLiveObjects;
                    trackedLiveObjectsSize = _trackedLiveObjectsSize;
                    nTrackedAllocObjects = _nTrackedAllocObjects;
                    avgObjectAge = _avgObjectAge;
                    maxSurvGen = _maxSurvGen;
                    nTotalAllocObjects = _nTotalAllocObjects;
                    
                    long liveBytes = 0;
                    long liveObjects = 0;
                    long allocObjects = 0;
                    long totalAllocObjects = 0;
                    for (int i = 0; i < nTrackedItems; i++) {
                        liveBytes += trackedLiveObjectsSize[i];
                        liveObjects += nTrackedLiveObjects[i];
                        allocObjects += nTrackedAllocObjects[i];
                        totalAllocObjects += nTotalAllocObjects[i];
                    }
                    renderers[0].setMaxValue(liveBytes);
                    renderers[1].setMaxValue(liveObjects);
                    renderers[2].setMaxValue(allocObjects);
                    renderers[3].setMaxValue(totalAllocObjects);
                    
                    tableModel.fireTableDataChanged();
                }
            }
        });
    }
    
    void setData(LivenessMemoryResultsSnapshot snapshot, Collection filter, int aggregation) {
        int _nTrackedItems = snapshot.getNTrackedItems();
        String[] _classNames = snapshot.getClassNames();
        int[] _nTrackedLiveObjects = snapshot.getNTrackedLiveObjects();
        long[] _trackedLiveObjectsSize = snapshot.getTrackedLiveObjectsSize();
        long[] _nTrackedAllocObjects = snapshot.getTrackedLiveObjectsSize();
        float[] _avgObjectAge = snapshot.getAvgObjectAge();
        int[] _maxSurvGen = snapshot.getMaxSurvGen();
        int[] _nTotalAllocObjects = snapshot.getnTotalAllocObjects();
        
        if (filter == null) { // old snapshot
            filterZeroItems = true;
            
            // class names in VM format
            for (int i = 0; i < _nTrackedItems; i++)
                _classNames[i] = StringUtils.userFormClassName(_classNames[i]);
            
            setData(_nTrackedItems, _classNames, _nTrackedLiveObjects, _trackedLiveObjectsSize,
                _nTrackedAllocObjects, _avgObjectAge, _maxSurvGen, _nTotalAllocObjects);
        } else { // new snapshot
            filterZeroItems = false;
            
            List<String> fClassNames = new ArrayList();
            List<Integer> fTrackedLiveObjects = new ArrayList();
            List<Long> fTrackedLiveObjectsSize = new ArrayList();
            List<Long> fTrackedAllocObjects = new ArrayList();
            List<Float> fAvgObjectAge = new ArrayList();
            List<Integer> fMaxSurvGen = new ArrayList();
            List<Integer> fTotalAllocObjects = new ArrayList();
            
            for (int i = 0; i < _nTrackedItems; i++) {
                if (filter.contains(_classNames[i])) {
                    fClassNames.add(_classNames[i]);
                    fTrackedLiveObjects.add(_nTrackedLiveObjects[i]);
                    fTrackedLiveObjectsSize.add(_trackedLiveObjectsSize[i]);
                    fTrackedAllocObjects.add(_nTrackedAllocObjects[i]);
                    fAvgObjectAge.add(_avgObjectAge[i]);
                    fMaxSurvGen.add(_maxSurvGen[i]);
                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                }
            }
            
            int trackedItems = fClassNames.size();
            String[] aClassNames = fClassNames.toArray(new String[trackedItems]);
            
            int[] aTrackedLiveObjects = new int[trackedItems];
            for (int i = 0; i < trackedItems; i++) aTrackedLiveObjects[i] = fTrackedLiveObjects.get(i);
            
            long[] aTrackedLiveObjectsSize = new long[trackedItems];
            for (int i = 0; i < trackedItems; i++) aTrackedLiveObjectsSize[i] = fTrackedLiveObjectsSize.get(i);
            
            long[] aTrackedAllocObjects = new long[trackedItems];
            for (int i = 0; i < trackedItems; i++) aTrackedAllocObjects[i] = fTrackedAllocObjects.get(i);
            
            float[] aAvgObjectAge = new float[trackedItems];
            for (int i = 0; i < trackedItems; i++) aAvgObjectAge[i] = fAvgObjectAge.get(i);
            
            int[] aTotalAllocObjectsSize = new int[trackedItems];
            for (int i = 0; i < trackedItems; i++) aTotalAllocObjectsSize[i] = fTotalAllocObjects.get(i);
            
            int[] aMaxSurvGen = new int[trackedItems];
            for (int i = 0; i < trackedItems; i++) aMaxSurvGen[i] = fMaxSurvGen.get(i);
            
            setData(trackedItems, aClassNames, aTrackedLiveObjects, aTrackedLiveObjectsSize,
                aTrackedAllocObjects, aAvgObjectAge, aMaxSurvGen, aTotalAllocObjectsSize);
        }
    }
    
    void resetData() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nTrackedItems = 0;
                classNames = null;
                nTrackedLiveObjects = null;
                trackedLiveObjectsSize = null;
                nTrackedAllocObjects = null;
                avgObjectAge = null;
                maxSurvGen = null;
                nTotalAllocObjects = null;
                
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
    
    
    ExportUtils.ExportProvider[] getExportProviders() {
        return table.getRowCount() == 0 ? null : new ExportUtils.ExportProvider[] {
            new ExportUtils.CSVExportProvider(table),
            new ExportUtils.HTMLExportProvider(table, "Allocated and Live Objects"),
            new ExportUtils.XMLExportProvider(table, "Allocated and Live Objects"),
            new ExportUtils.PNGExportProvider(table)
        };
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection value);
    
    protected abstract void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value);
    
    protected void popupShowing() {};
    
    protected void popupHidden()  {};
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        tableModel = new MemoryTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
            protected ClientUtils.SourceCodeSelection getValueForPopup(int row) {
                return valueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value) {
                LivenessTableView.this.populatePopup(popup, (ClientUtils.SourceCodeSelection)value);
            }
            protected void popupShowing() {
                LivenessTableView.this.popupShowing();
            }
            protected void popupHidden() {
                LivenessTableView.this.popupHidden();
            }
        };
        
        table.providePopupMenu(true);
        table.setDefaultAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                ClientUtils.SourceCodeSelection value = valueForRow(row);
                if (value != null) performDefaultAction(value);
            }
        });
        
        final int offset = selection == null ? -1 : 0;
        
        table.setMainColumn(1 + offset);
        table.setFitWidthColumn(1 + offset);
        
        table.setSortColumn(2 + offset);
        table.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) table.setColumnVisibility(0, false);
        table.setColumnVisibility(5 + offset, false);
        table.setColumnVisibility(6 + offset, false);
        
        // Filter out classes with no instances
        table.setRowFilter(new RowFilter() {
            public boolean include(RowFilter.Entry entry) {
                return !filterZeroItems || ((Number)entry.getValue(5 + offset)).intValue() > 0;
            }
        });
        
        renderers = new HideableBarRenderer[4];
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[2] = new HideableBarRenderer(new NumberPercentRenderer());
        renderers[3] = new HideableBarRenderer(new NumberPercentRenderer());
        
        renderers[0].setMaxValue(123456789);
        renderers[1].setMaxValue(12345678);
        renderers[2].setMaxValue(12345678);
        renderers[3].setMaxValue(12345678);
        
        if (selection != null) table.setColumnRenderer(0, new CheckBoxRenderer());
        table.setColumnRenderer(1 + offset, new JavaNameRenderer());
        table.setColumnRenderer(2 + offset, renderers[0]);
        table.setColumnRenderer(3 + offset, renderers[1]);
        table.setColumnRenderer(4 + offset, renderers[2]);
        table.setColumnRenderer(5 + offset, renderers[3]);
        table.setColumnRenderer(6 + offset, new NumberRenderer());
        table.setColumnRenderer(7 + offset, new NumberRenderer());
        
        if (selection != null) {
            int w = new JLabel(table.getColumnName(0)).getPreferredSize().width;
            table.setDefaultColumnWidth(0, w + 15);
        }
        table.setDefaultColumnWidth(2 + offset, renderers[0].getOptimalWidth());
        table.setDefaultColumnWidth(3 + offset, renderers[1].getMaxNoBarWidth());
        table.setDefaultColumnWidth(4 + offset, renderers[2].getMaxNoBarWidth());
        table.setDefaultColumnWidth(5 + offset, renderers[3].getMaxNoBarWidth());
        table.setDefaultColumnWidth(6 + offset, renderers[3].getNoBarWidth() - 25);
        table.setDefaultColumnWidth(7 + offset, renderers[3].getNoBarWidth() - 25);
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    
    private ClientUtils.SourceCodeSelection valueForRow(int row) {
        if (nTrackedItems == 0 || row == -1) return null;
        if (row >= tableModel.getRowCount()) return null; // #239936
        return classNames[table.convertRowIndexToModel(row)];
    }
    
    
    private class MemoryTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return "Name";
            } else if (columnIndex == 2) {
                return "Live Bytes";
            } else if (columnIndex == 3) {
                return "Live Objects";
            } else if (columnIndex == 4) {
                return "Allocated Objects";
            } else if (columnIndex == 5) {
                return "Total Alloc. Obj.";
            } else if (columnIndex == 6) {
                return "Avg. Age";
            } else if (columnIndex == 7) {
                return "Generations";
            } else if (columnIndex == 0) {
                return "Selected";
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
            } else if (columnIndex == 4) {
                return Long.class;
            } else if (columnIndex == 5) {
                return Integer.class;
            } else if (columnIndex == 6) {
                return Float.class;
            } else if (columnIndex == 7) {
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
            return selection == null ? 7 : 8;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (nTrackedItems == 0) return null;
            
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return classNames[rowIndex].getClassName();
            } else if (columnIndex == 2) {
                return trackedLiveObjectsSize[rowIndex];
            } else if (columnIndex == 3) {
                return nTrackedLiveObjects[rowIndex];
            } else if (columnIndex == 4) {
                return nTrackedAllocObjects[rowIndex];
            } else if (columnIndex == 5) {
                return nTotalAllocObjects[rowIndex];
            } else if (columnIndex == 6) {
                return avgObjectAge[rowIndex];
            } else if (columnIndex == 7) {
                return maxSurvGen[rowIndex];
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

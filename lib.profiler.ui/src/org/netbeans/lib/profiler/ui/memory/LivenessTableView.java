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
import java.text.Format;
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
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
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
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class LivenessTableView extends MemoryView {
    
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
    
    private final boolean includeTotalAllocs;
    
    private boolean filterZeroItems = true;
    
    
    LivenessTableView(Set<ClientUtils.SourceCodeSelection> selection, boolean includeTotalAllocs) {
        this.selection = selection;
        
        this.includeTotalAllocs = includeTotalAllocs;
        
        initUI();
    }
    
    
    protected ProfilerTable getResultsComponent() { return table; }
    
    
    private void setData(final int _nTrackedItems, final String[] _classNames,
                 final int[] _nTrackedLiveObjects, final long[] _trackedLiveObjectsSize,
                 final long[] _nTrackedAllocObjects, final float[] _avgObjectAge,
                 final int[] _maxSurvGen, final int[] _nTotalAllocObjects, final boolean diff) {
        
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
                    if (includeTotalAllocs) nTotalAllocObjects = _nTotalAllocObjects;
                    
                    long liveBytes = 0;
                    long _liveBytes = 0;
                    long liveObjects = 0;
                    long _liveObjects = 0;
                    long allocObjects = 0;
                    long _allocObjects = 0;
                    long totalAllocObjects = 0;
                    long _totalAllocObjects = 0;
                    for (int i = 0; i < nTrackedItems; i++) {
                        if (diff) {
                            liveBytes = Math.max(liveBytes, trackedLiveObjectsSize[i]);
                            _liveBytes = Math.min(_liveBytes, trackedLiveObjectsSize[i]);
                            liveObjects = Math.max(liveObjects, nTrackedLiveObjects[i]);
                            _liveObjects = Math.min(_liveObjects, nTrackedLiveObjects[i]);
                            allocObjects = Math.max(allocObjects, nTrackedAllocObjects[i]);
                            _allocObjects = Math.min(_allocObjects, nTrackedAllocObjects[i]);
                            if (includeTotalAllocs) {
                                totalAllocObjects = Math.max(totalAllocObjects, nTotalAllocObjects[i]);
                                _totalAllocObjects = Math.min(_totalAllocObjects, nTotalAllocObjects[i]);
                            }
                        } else {
                            liveBytes += trackedLiveObjectsSize[i];
                            liveObjects += nTrackedLiveObjects[i];
                            allocObjects += nTrackedAllocObjects[i];
                            if (includeTotalAllocs) totalAllocObjects += nTotalAllocObjects[i];
                        }
                    }
                    if (diff) {
                        renderers[0].setMaxValue(Math.max(Math.abs(liveBytes), Math.abs(_liveBytes)));
                        renderers[1].setMaxValue(Math.max(Math.abs(liveObjects), Math.abs(_liveObjects)));
                        renderers[2].setMaxValue(Math.max(Math.abs(allocObjects), Math.abs(_allocObjects)));
                        if (includeTotalAllocs) renderers[3].setMaxValue(Math.max(Math.abs(totalAllocObjects), Math.abs(_totalAllocObjects)));
                    } else {
                        renderers[0].setMaxValue(liveBytes);
                        renderers[1].setMaxValue(liveObjects);
                        renderers[2].setMaxValue(allocObjects);
                        if (includeTotalAllocs) renderers[3].setMaxValue(totalAllocObjects);
                    }
                    
                    renderers[0].setDiffMode(diff);
                    renderers[1].setDiffMode(diff);
                    renderers[2].setDiffMode(diff);
                    if (includeTotalAllocs) renderers[3].setDiffMode(diff);
                    
                    renderersEx[0].setDiffMode(diff);
                    renderersEx[1].setDiffMode(diff);
                    
                    tableModel.fireTableDataChanged();
                }
            }
        });
    }
    
    public void setData(MemoryResultsSnapshot snapshot, Collection<String> filter, int aggregation) {
        LivenessMemoryResultsSnapshot _snapshot = (LivenessMemoryResultsSnapshot)snapshot;
        boolean diff = _snapshot instanceof LivenessMemoryResultsDiff;
        
        String[] _classNames = _snapshot.getClassNames();
        int[] _nTrackedLiveObjects = _snapshot.getNTrackedLiveObjects();
        long[] _trackedLiveObjectsSize = _snapshot.getTrackedLiveObjectsSize();
        long[] _nTrackedAllocObjects = _snapshot.getNTrackedAllocObjects();
        float[] _avgObjectAge = _snapshot.getAvgObjectAge();
        int[] _maxSurvGen = _snapshot.getMaxSurvGen();
        int[] _nTotalAllocObjects = _snapshot.getnTotalAllocObjects();
        
        int _nTrackedItems = Math.min(_snapshot.getNProfiledClasses(), _classNames.length);
        _nTrackedItems = Math.min(_nTrackedItems, _nTotalAllocObjects.length);
            
        if (filter == null) { // old snapshot
            filterZeroItems = !diff;
            
            setData(_nTrackedItems, _classNames, _nTrackedLiveObjects, _trackedLiveObjectsSize,
                _nTrackedAllocObjects, _avgObjectAge, _maxSurvGen, _nTotalAllocObjects, diff);
        } else { // new snapshot or live results
            filterZeroItems = false;
            
            List<String> fClassNames = new ArrayList();
            List<Integer> fTrackedLiveObjects = new ArrayList();
            List<Long> fTrackedLiveObjectsSize = new ArrayList();
            List<Long> fTrackedAllocObjects = new ArrayList();
            List<Float> fAvgObjectAge = new ArrayList();
            List<Integer> fMaxSurvGen = new ArrayList();
//            List<Integer> fTotalAllocObjects = new ArrayList();
            
            if (isAll(filter)) {
                for (int i = 0; i < _nTrackedItems; i++) {
                    fClassNames.add(_classNames[i]);
                    fTrackedLiveObjects.add(_nTrackedLiveObjects[i]);
                    fTrackedLiveObjectsSize.add(_trackedLiveObjectsSize[i]);
                    fTrackedAllocObjects.add(_nTrackedAllocObjects[i]);
                    fAvgObjectAge.add(_avgObjectAge[i]);
                    fMaxSurvGen.add(_maxSurvGen[i]);
//                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                }
            } else if (isExact(filter)) {
                for (int i = 0; i < _nTrackedItems; i++) {
                    if (filter.contains(_classNames[i])) {
                        fClassNames.add(_classNames[i]);
                        fTrackedLiveObjects.add(_nTrackedLiveObjects[i]);
                        fTrackedLiveObjectsSize.add(_trackedLiveObjectsSize[i]);
                        fTrackedAllocObjects.add(_nTrackedAllocObjects[i]);
                        fAvgObjectAge.add(_avgObjectAge[i]);
                        fMaxSurvGen.add(_maxSurvGen[i]);
    //                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                    }
                }
            } else {
                for (String f : filter) {
                    if (f.endsWith("**")) { // NOI18N
                        f = f.substring(0, f.length() - 2);
                        for (int i = 0; i < _nTrackedItems; i++) {
                            if (_classNames[i].startsWith(f)) {
                                fClassNames.add(_classNames[i]);
                                fTrackedLiveObjects.add(_nTrackedLiveObjects[i]);
                                fTrackedLiveObjectsSize.add(_trackedLiveObjectsSize[i]);
                                fTrackedAllocObjects.add(_nTrackedAllocObjects[i]);
                                fAvgObjectAge.add(_avgObjectAge[i]);
                                fMaxSurvGen.add(_maxSurvGen[i]);
            //                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
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
                                fTrackedLiveObjects.add(_nTrackedLiveObjects[i]);
                                fTrackedLiveObjectsSize.add(_trackedLiveObjectsSize[i]);
                                fTrackedAllocObjects.add(_nTrackedAllocObjects[i]);
                                fAvgObjectAge.add(_avgObjectAge[i]);
                                fMaxSurvGen.add(_maxSurvGen[i]);
            //                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < _nTrackedItems; i++) {
                            if (_classNames[i].equals(f)) {
                                fClassNames.add(_classNames[i]);
                                fTrackedLiveObjects.add(_nTrackedLiveObjects[i]);
                                fTrackedLiveObjectsSize.add(_trackedLiveObjectsSize[i]);
                                fTrackedAllocObjects.add(_nTrackedAllocObjects[i]);
                                fAvgObjectAge.add(_avgObjectAge[i]);
                                fMaxSurvGen.add(_maxSurvGen[i]);
            //                    fTotalAllocObjects.add(_nTotalAllocObjects[i]);
                            }
                        }
                    }
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
            
//            int[] aTotalAllocObjectsSize = new int[trackedItems];
//            for (int i = 0; i < trackedItems; i++) aTotalAllocObjectsSize[i] = fTotalAllocObjects.get(i);
            
            int[] aMaxSurvGen = new int[trackedItems];
            for (int i = 0; i < trackedItems; i++) aMaxSurvGen[i] = fMaxSurvGen.get(i);
            
            setData(trackedItems, aClassNames, aTrackedLiveObjects, aTrackedLiveObjectsSize,
                aTrackedAllocObjects, aAvgObjectAge, aMaxSurvGen, /*aTotalAllocObjectsSize*/_nTotalAllocObjects, diff);
        }
    }
    
    public void resetData() {
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
            new ExportUtils.HTMLExportProvider(table, EXPORT_ALLOCATED_LIVE),
            new ExportUtils.XMLExportProvider(table, EXPORT_ALLOCATED_LIVE),
            new ExportUtils.PNGExportProvider(table)
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
        
        tableModel = new MemoryTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return LivenessTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                LivenessTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
            protected void popupShowing() {
                LivenessTableView.this.popupShowing();
            }
            protected void popupHidden() {
                LivenessTableView.this.popupHidden();
            }
        };
        
        table.setColumnToolTips(selection == null ? new String[] {
                                  NAME_COLUMN_TOOLTIP,
                                  LIVE_SIZE_COLUMN_TOOLTIP,
                                  LIVE_COUNT_COLUMN_TOOLTIP,
                                  ALLOC_COUNT_COLUMN_TOOLTIP,
                                  AVG_AGE_COLUMN_TOOLTIP,
                                  GENERATIONS_COLUMN_TOOLTIP,
                                  TOTAL_ALLOC_COUNT_COLUMN_TOOLTIP
                                } : new String[] {
                                  SELECTED_COLUMN_TOOLTIP,
                                  NAME_COLUMN_TOOLTIP,
                                  LIVE_SIZE_COLUMN_TOOLTIP,
                                  LIVE_COUNT_COLUMN_TOOLTIP,
                                  ALLOC_COUNT_COLUMN_TOOLTIP,
                                  AVG_AGE_COLUMN_TOOLTIP,
                                  GENERATIONS_COLUMN_TOOLTIP,
                                  TOTAL_ALLOC_COUNT_COLUMN_TOOLTIP
                                });
        
        table.providePopupMenu(true);
        installDefaultAction();
        
        table.setMainColumn(1 + offset);
        table.setFitWidthColumn(1 + offset);
        
        table.setSortColumn(2 + offset);
        table.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) table.setColumnVisibility(0, false);
        table.setColumnVisibility(5 + offset, false);
        if (includeTotalAllocs) table.setColumnVisibility(7 + offset, false);
        
        // Filter out classes with no instances
        table.addRowFilter(new RowFilter() {
            public boolean include(RowFilter.Entry entry) {
                return !filterZeroItems || ((Number)entry.getValue(/*5*/4 + offset)).intValue() > 0;
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
        
        if (selection != null) table.setColumnRenderer(0, new CheckBoxRenderer());
        table.setColumnRenderer(1 + offset, new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS)));
        table.setColumnRenderer(2 + offset, renderers[0]);
        table.setColumnRenderer(3 + offset, renderers[1]);
        table.setColumnRenderer(4 + offset, renderers[2]);
        table.setColumnRenderer(5 + offset, renderersEx[0]);
        table.setColumnRenderer(6 + offset, renderersEx[1]);
        if (includeTotalAllocs) table.setColumnRenderer(7 + offset, renderers[3]);
        
        if (selection != null) {
            int w = new JLabel(table.getColumnName(0)).getPreferredSize().width;
            table.setDefaultColumnWidth(0, w + 15);
        }
        table.setDefaultColumnWidth(2 + offset, renderers[0].getOptimalWidth());
        table.setDefaultColumnWidth(3 + offset, renderers[1].getMaxNoBarWidth());
        table.setDefaultColumnWidth(4 + offset, renderers[2].getMaxNoBarWidth());
        table.setDefaultColumnWidth(5 + offset, renderers[2].getNoBarWidth() - 25);
        table.setDefaultColumnWidth(6 + offset, renderers[2].getNoBarWidth() - 25);
        if (includeTotalAllocs) table.setDefaultColumnWidth(7 + offset, renderers[3].getMaxNoBarWidth());
        
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
        
        private final int columns = 6 +
                (selection == null ? 0 : 1) +
                (includeTotalAllocs ? 1 : 0);
        
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
                return String.class;
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
        
        public int getRowCount() {
            return nTrackedItems;
        }

        public int getColumnCount() {
            return columns;
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
                return avgObjectAge[rowIndex];
            } else if (columnIndex == 6) {
                return maxSurvGen[rowIndex];
            } else if (columnIndex == 7) {
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

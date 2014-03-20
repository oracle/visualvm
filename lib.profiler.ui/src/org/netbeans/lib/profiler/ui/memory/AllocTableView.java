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
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
class AllocTableView extends JPanel {
    
    private static final String[] EMPTY_SELECTION = new String[0];
    
    private MemoryTableModel tableModel;
    private ProfilerTable table;
    
    private int nTrackedItems;
    private String[] classNames;
    private int[] nTotalAllocObjects;
    private long[] totalAllocObjectsSize;
    
    private Map<Integer, String> selections;
    
    
    public AllocTableView() {
        initUI();
    }
    
    
    void setData(final int _nTrackedItems, final String[] _classNames,
                 final int[] _nTotalAllocObjects, final long[] _totalAllocObjectsSize) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (tableModel != null) {
                    nTrackedItems = _nTrackedItems;
                    classNames = _classNames;
                    nTotalAllocObjects = _nTotalAllocObjects;
                    totalAllocObjectsSize = _totalAllocObjectsSize;
                    
                    long totalObjects = 0;
                    long totalBytes = 0;
                    for (int i = 0; i < nTrackedItems; i++) {
                        totalObjects += nTotalAllocObjects[i];
                        totalBytes += totalAllocObjectsSize[i];
                    }
                    renderers[0].setMaxValue(totalBytes);
                    renderers[1].setMaxValue(totalObjects);
                    
                    if (selections == null) selections = new HashMap();
                    
                    tableModel.fireTableDataChanged();
                }
            }
        });
    }
    
    void resetData() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nTrackedItems = 0;
                classNames = null;
                nTotalAllocObjects = null;
                totalAllocObjectsSize = null;
                selections = null;
                
                tableModel.fireTableDataChanged();
            }
        });
    }
    
    boolean hasSelection() {
        return selections != null && !selections.isEmpty();
    }
    
    String[] getSelections() {
        return !hasSelection() ? EMPTY_SELECTION :
                selections.values().toArray(EMPTY_SELECTION);
    }
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        tableModel = new MemoryTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null);
        table.setMainColumn(1);
        table.setFitWidthColumn(1);
        
        table.setSortColumn(2);
        table.setDefaultSortOrder(1, SortOrder.ASCENDING);
        
        table.setColumnVisibility(0, false);
        
        // Filter out classes with no instances
        table.setRowFilter(new RowFilter() {
            public boolean include(RowFilter.Entry entry) {
                return ((Number)entry.getValue(3)).intValue() > 0;
            }
        });
        
        renderers = new HideableBarRenderer[2];
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());
        
        renderers[0].setMaxValue(123456789);
        renderers[1].setMaxValue(12345678);
        
        table.setColumnRenderer(0, new CheckBoxRenderer());
        table.setColumnRenderer(1, new JavaNameRenderer());
        table.setColumnRenderer(2, renderers[0]);
        table.setColumnRenderer(3, renderers[1]);
        
        int w = new JLabel(table.getColumnName(0)).getPreferredSize().width;
        table.setDefaultColumnWidth(0, w + 15);
        table.setDefaultColumnWidth(2, renderers[0].getOptimalWidth());
        table.setDefaultColumnWidth(3, renderers[1].getMaxNoBarWidth());
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    
    private class MemoryTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 1) {
                return "Name";
            } else if (columnIndex == 2) {
                return "Allocated Bytes";
            } else if (columnIndex == 3) {
                return "Allocated Objects";
            } else if (columnIndex == 0) {
                return "Selected";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
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
            return 4;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (nTrackedItems == 0) return null;
            
            if (columnIndex == 1) {
                return classNames[rowIndex];
            } else if (columnIndex == 2) {
                return totalAllocObjectsSize[rowIndex];
            } else if (columnIndex == 3) {
                return nTotalAllocObjects[rowIndex];
            } else if (columnIndex == 0) {
                return selections.containsKey(rowIndex);
            }

            return null;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                if (Boolean.FALSE.equals(aValue)) selections.remove(rowIndex);
                else selections.put(rowIndex, getValueAt(rowIndex, 1).toString());
                tableModel.fireTableDataChanged();
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }
        
    }
    
}

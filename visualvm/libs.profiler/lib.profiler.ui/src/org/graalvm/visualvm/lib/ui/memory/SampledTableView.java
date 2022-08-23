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
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsDiff;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
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
abstract class SampledTableView extends MemoryView {

    private MemoryTableModel tableModel;
    private ProfilerTable table;

    private String[] names;
    private int[] instances;
    private long[] bytes;
    private ClientUtils.SourceCodeSelection[] classNames;

    private final Set<ClientUtils.SourceCodeSelection> selection;


    SampledTableView(Set<ClientUtils.SourceCodeSelection> selection) {
        this.selection = selection;

        initUI();
    }
    
    
    protected ProfilerTable getResultsComponent() { return table; }
    
    
    public void setData(MemoryResultsSnapshot snapshot, GenericFilter filter, final int aggregation) {
        final SampledMemoryResultsSnapshot _snapshot = (SampledMemoryResultsSnapshot)snapshot;
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean diff = _snapshot instanceof SampledMemoryResultsDiff;
                
                long totalSize = 0;
                long _totalSize = 0;
                long totalInstances = 0;
                long _totalInstances = 0;
                
                names = _snapshot.getClassNames();
                instances = _snapshot.getObjectsCounts();
                bytes = _snapshot.getObjectsSizePerClass();

                classNames = new ClientUtils.SourceCodeSelection[names.length];

                for (int i = 0; i < names.length; i++) {
                    if (diff) {
                        totalInstances = Math.max(totalInstances, instances[i]);
                        _totalInstances = Math.min(_totalInstances, instances[i]);
                        totalSize = Math.max(totalSize, bytes[i]);
                        _totalSize = Math.min(_totalSize, bytes[i]);
                    } else {
                        totalInstances += instances[i];
                        totalSize += bytes[i];
                    }
                    classNames[i] = new ClientUtils.SourceCodeSelection(names[i], Wildcards.ALLWILDCARD, null);
                }
                
                if (diff) {
                    renderers[0].setMaxValue(Math.max(Math.abs(totalSize), Math.abs(_totalSize)));
                    renderers[1].setMaxValue(Math.max(Math.abs(totalInstances), Math.abs(_totalInstances)));
                } else {
                    renderers[0].setMaxValue(totalSize);
                    renderers[1].setMaxValue(totalInstances);
                }
                
                renderers[0].setDiffMode(diff);
                renderers[1].setDiffMode(diff);

                tableModel.fireTableDataChanged();
            }
        });
    }
    
    public void resetData() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                names = null;
                instances = null;
                bytes = null;
                classNames = null;
                
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
            new ExportUtils.HTMLExportProvider(table, EXPORT_LIVE),
            new ExportUtils.XMLExportProvider(table, EXPORT_LIVE),
            new ExportUtils.PNGExportProvider(table)
        };
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection userValue);
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    protected void popupShowing() {};
    
    protected void popupHidden()  {};
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        int offset = selection == null ? -1 : 0;
        
        tableModel = new MemoryTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return SampledTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                SampledTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
            protected void popupShowing() {
                SampledTableView.this.popupShowing();
            }
            protected void popupHidden() {
                SampledTableView.this.popupHidden();
            }
        };
        
        table.setColumnToolTips(selection == null ? new String[] {
                                  NAME_COLUMN_TOOLTIP,
                                  LIVE_SIZE_COLUMN_TOOLTIP,
                                  LIVE_COUNT_COLUMN_TOOLTIP
                                } : new String[] {
                                  SELECTED_COLUMN_TOOLTIP,
                                  NAME_COLUMN_TOOLTIP,
                                  LIVE_SIZE_COLUMN_TOOLTIP,
                                  LIVE_COUNT_COLUMN_TOOLTIP
                                });
        
        table.providePopupMenu(true);
        installDefaultAction();
        
        table.setMainColumn(1 + offset);
        table.setFitWidthColumn(1 + offset);
        
        table.setSortColumn(2 + offset);
        table.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) table.setColumnVisibility(0, false);
        
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
        if (names == null || row == -1) return null;
        if (row >= tableModel.getRowCount()) return null; // #239936
        return classNames[table.convertRowIndexToModel(row)];
    }
    
    
    private class MemoryTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return COLUMN_NAME;
            } else if (columnIndex == 2) {
                return COLUMN_LIVE_BYTES;
            } else if (columnIndex == 3) {
                return COLUMN_LIVE_OBJECTS;
            } else if (columnIndex == 0) {
                return COLUMN_SELECTED;
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return String.class;
            } else if (columnIndex == 0) {
                return Boolean.class;
            } else {
                return Long.class;
            }
        }

        public int getRowCount() {
            return names == null ? 0 : names.length;
        }

        public int getColumnCount() {
            return selection == null ? 3 : 4;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (names == null) return null;
            
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return names[rowIndex];
            } else if (columnIndex == 2) {
                return bytes[rowIndex];
            } else if (columnIndex == 3) {
                return instances[rowIndex];
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

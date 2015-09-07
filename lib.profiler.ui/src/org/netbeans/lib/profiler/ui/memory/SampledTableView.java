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
import java.util.Collection;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsSnapshot;
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
abstract class SampledTableView extends MemoryView {
    
    private MemoryTableModel tableModel;
    private ProfilerTable table;
    
    private String[] names;
    private int[] instances;
    private long[] bytes;
    private ClientUtils.SourceCodeSelection[] classNames;
    
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    
    public SampledTableView(Set<ClientUtils.SourceCodeSelection> selection) {
        this.selection = selection;
        
        initUI();
    }
    
    
    protected ProfilerTable getResultsComponent() { return table; }
    
    
    public void setData(MemoryResultsSnapshot snapshot, Collection<String> filter, final int aggregation) {
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

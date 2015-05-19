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
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.McsTimeRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class CPUTableView extends CPUView {
    
    private CPUTableModel tableModel;
    private ProfilerTable table;
    
    private FlatProfileContainer data;
    
    private Map<Integer, ClientUtils.SourceCodeSelection> idMap;
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    private boolean sampled = true;
    private boolean twoTimeStamps;
    
    private boolean hitsVisible = false;
    private boolean invocationsVisible = true;
    
    
    public CPUTableView(Set<ClientUtils.SourceCodeSelection> selection) {
        this.selection = selection;
        
        initUI();
    }
    
    
    void setData(final FlatProfileContainer newData, final Map<Integer, ClientUtils.SourceCodeSelection> newIdMap, final boolean _sampled, final boolean _diff) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean structureChange = sampled != _sampled;
                sampled = _sampled;
                twoTimeStamps = newData == null ? false : newData.isCollectingTwoTimeStamps();
                idMap = newIdMap;
                if (tableModel != null) {
                    data = newData;
                    
                    if (_diff) {
                        long[] maxTimes = new long[4];
                        long[] minTimes = new long[4];
                        int maxInvocations = 0;
                        int minInvocations = 0;
                        for (int row = 0; row < tableModel.getRowCount(); row++) {
                            maxTimes[0] = Math.max(maxTimes[0], data.getTimeInMcs0AtRow(row));
                            minTimes[0] = Math.min(minTimes[0], data.getTimeInMcs0AtRow(row));
                            if (twoTimeStamps) {
                                maxTimes[1] = Math.max(maxTimes[1], data.getTimeInMcs1AtRow(row));
                                minTimes[1] = Math.min(minTimes[1], data.getTimeInMcs1AtRow(row));
                            }
                            maxTimes[2] = Math.max(maxTimes[2], data.getTotalTimeInMcs0AtRow(row));
                            minTimes[2] = Math.min(minTimes[2], data.getTotalTimeInMcs0AtRow(row));
                            if (twoTimeStamps) {
                                maxTimes[3] = Math.max(maxTimes[3], data.getTotalTimeInMcs1AtRow(row));
                                minTimes[3] = Math.min(minTimes[3], data.getTotalTimeInMcs1AtRow(row));
                            }
                            maxInvocations = Math.max(maxInvocations, data.getNInvocationsAtRow(row));
                            minInvocations = Math.min(minInvocations, data.getNInvocationsAtRow(row));
                        }
                        
                        renderers[0].setMaxValue(Math.max(Math.abs(maxTimes[0]), Math.abs(minTimes[0])));
                        renderers[1].setMaxValue(Math.max(Math.abs(maxTimes[1]), Math.abs(minTimes[1])));
                        renderers[2].setMaxValue(Math.max(Math.abs(maxTimes[2]), Math.abs(minTimes[2])));
                        renderers[3].setMaxValue(Math.max(Math.abs(maxTimes[3]), Math.abs(minTimes[3])));
                        renderers[4].setMaxValue(Math.max(Math.abs(maxInvocations), Math.abs(minInvocations)));
                    } else {
                        long[] maxTimes = new long[4];
                        int maxInvocations = 0;
                        for (int row = 0; row < tableModel.getRowCount(); row++) {
                            maxTimes[0] += data.getTimeInMcs0AtRow(row);
                            if (twoTimeStamps) maxTimes[1] += data.getTimeInMcs1AtRow(row);
                            maxTimes[2] += data.getTotalTimeInMcs0AtRow(row);
                            if (twoTimeStamps) maxTimes[3] += data.getTotalTimeInMcs1AtRow(row);
                            maxInvocations += data.getNInvocationsAtRow(row);
                        }
                        
                        renderers[0].setMaxValue(maxTimes[0]);
                        renderers[1].setMaxValue(maxTimes[1]);
                        renderers[2].setMaxValue(maxTimes[2]);
                        renderers[3].setMaxValue(maxTimes[3]);
                        renderers[4].setMaxValue(maxInvocations);
                    }
                    
                    renderers[0].setDiffMode(_diff);
                    renderers[1].setDiffMode(_diff);
                    renderers[2].setDiffMode(_diff);
                    renderers[3].setDiffMode(_diff);
                    renderers[4].setDiffMode(_diff);
                    
                    tableModel.fireTableDataChanged();
                }
                if (structureChange) {
                    // Resolve Hits/Invocations column
                    int col = table.convertColumnIndexToView(selection == null ? 5 : 6);
                    String colN = tableModel.getColumnName(selection == null ? 5 : 6);
                    
                    // Persist current Hits/Invocations column visibility
                    if (sampled) invocationsVisible = table.isColumnVisible(col);
                    else hitsVisible = table.isColumnVisible(col);
                    
                    // Update Hits/Invocations column name
                    table.getColumnModel().getColumn(col).setHeaderValue(colN);
                    
                    // Set new Hits/Invocations column visibility
                    table.setColumnVisibility(col, sampled ? hitsVisible : invocationsVisible);
                    
                    setToolTips();
                    
                    repaint();
                }
            }
        });
    }
    
    public void resetData() {
        setData(null, null, sampled, false);
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
            new ExportUtils.HTMLExportProvider(table, EXPORT_HOTSPOTS),
            new ExportUtils.XMLExportProvider(table, EXPORT_HOTSPOTS),
            new ExportUtils.PNGExportProvider(table)
        };
    }
    
    
    protected abstract void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue);
    
    protected void popupShowing() {};
    
    protected void popupHidden()  {};
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        tableModel = new CPUTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
            public ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
                return CPUTableView.this.getUserValueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                CPUTableView.this.populatePopup(popup, value, (ClientUtils.SourceCodeSelection)userValue);
            }
            protected void popupShowing() {
                CPUTableView.this.popupShowing();
            }
            protected void popupHidden() {
                CPUTableView.this.popupHidden();
            }
        };
        
        setToolTips();
        
        table.providePopupMenu(true);
        installDefaultAction();
        
        int offset = selection == null ? -1 : 0;
        
        table.setMainColumn(1 + offset);
        table.setFitWidthColumn(1 + offset);
        
        table.setSortColumn(3 + offset);
        table.setDefaultSortOrder(1 + offset, SortOrder.ASCENDING);
        
        if (selection != null) table.setColumnVisibility(0, false);
        table.setColumnVisibility(2 + offset, false);
        table.setColumnVisibility(4 + offset, false);
        table.setColumnVisibility(6 + offset, false);
        
        renderers = new HideableBarRenderer[5];
        
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer()));
        renderers[2] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer()));
        renderers[3] = new HideableBarRenderer(new NumberPercentRenderer(new McsTimeRenderer()));
        renderers[4] = new HideableBarRenderer(new NumberRenderer());
        
        long refTime = 123456;
        renderers[0].setMaxValue(refTime);
        renderers[1].setMaxValue(refTime);
        renderers[2].setMaxValue(refTime);
        renderers[3].setMaxValue(refTime);
        renderers[4].setMaxValue(refTime);
        
        if (selection != null) table.setColumnRenderer(0, new CheckBoxRenderer() {
            private boolean visible;
            public void setValue(Object value, int row) {
                visible = isSelectable(idMap.get(data.getMethodIdAtRow(row)));
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
        table.setColumnRenderer(1 + offset, new JavaNameRenderer(Icons.getIcon(ProfilerIcons.NODE_LEAF)));
        table.setColumnRenderer(2 + offset, renderers[0]);
        table.setColumnRenderer(3 + offset, renderers[1]);
        table.setColumnRenderer(4 + offset, renderers[2]);
        table.setColumnRenderer(5 + offset, renderers[3]);
        table.setColumnRenderer(6 + offset, renderers[4]);
        
        int w;
        if (selection != null) {
            w = new JLabel(table.getColumnName(0)).getPreferredSize().width;
            table.setDefaultColumnWidth(0, w + 15);
        }
        table.setDefaultColumnWidth(2 + offset, renderers[0].getMaxNoBarWidth());
        table.setDefaultColumnWidth(3 + offset, renderers[1].getOptimalWidth());
        table.setDefaultColumnWidth(4 + offset, renderers[2].getMaxNoBarWidth());
        table.setDefaultColumnWidth(5 + offset, renderers[3].getMaxNoBarWidth());
        
        sampled = !sampled;
        w = new JLabel(table.getColumnName(6 + offset)).getPreferredSize().width;
        sampled = !sampled;
        w = Math.max(w, new JLabel(table.getColumnName(6 + offset)).getPreferredSize().width);
        table.setDefaultColumnWidth(6 + offset, Math.max(renderers[4].getNoBarWidth(), w + 15));
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    private void setToolTips() {
        table.setColumnToolTips(selection == null ? new String[] {
                                        NAME_COLUMN_TOOLTIP,
                                        SELF_TIME_COLUMN_TOOLTIP,
                                        SELF_TIME_CPU_COLUMN_TOOLTIP,
                                        TOTAL_TIME_COLUMN_TOOLTIP,
                                        TOTAL_TIME_CPU_COLUMN_TOOLTIP,
                                        sampled ? HITS_COLUMN_TOOLTIP :
                                                  INVOCATIONS_COLUMN_TOOLTIP
                                      } : new String[] {
                                        SELECTED_COLUMN_TOOLTIP,
                                        NAME_COLUMN_TOOLTIP,
                                        SELF_TIME_COLUMN_TOOLTIP,
                                        SELF_TIME_CPU_COLUMN_TOOLTIP,
                                        TOTAL_TIME_COLUMN_TOOLTIP,
                                        TOTAL_TIME_CPU_COLUMN_TOOLTIP,
                                        sampled ? HITS_COLUMN_TOOLTIP :
                                                  INVOCATIONS_COLUMN_TOOLTIP
                                      });
    }
    
    protected ProfilerTable getResultsComponent() {
        return table;
    }
    
    
    protected ClientUtils.SourceCodeSelection getUserValueForRow(int row) {
        if (data == null || row == -1) return null;
        if (row >= tableModel.getRowCount()) return null; // #239936
        row = table.convertRowIndexToModel(row);
        return idMap.get(data.getMethodIdAtRow(row));
//        return selectionForId(data.getMethodIdAtRow(row));
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
    
    static boolean isSelectable(ClientUtils.SourceCodeSelection method) {
        return !method.getMethodName().endsWith("[native]"); // NOI18N
    }
    
    
    private class CPUTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return COLUMN_NAME;
            } else if (columnIndex == 2) {
                return COLUMN_SELFTIME;
            } else if (columnIndex == 3) {
                return COLUMN_SELFTIME_CPU;
            } else if (columnIndex == 4) {
                return COLUMN_TOTALTIME;
            } else if (columnIndex == 5) {
                return COLUMN_TOTALTIME_CPU;
            } else if (columnIndex == 6) {
                return sampled ? COLUMN_HITS : COLUMN_INVOCATIONS;
            } else if (columnIndex == 0) {
                return COLUMN_SELECTED;
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return String.class;
            } else if (columnIndex == 6) {
                return Integer.class;
            } else if (columnIndex == 0) {
                return Boolean.class;
            } else {
                return Long.class;
            }
        }

        public int getRowCount() {
            return data == null ? 0 : data.getNRows();
        }

        public int getColumnCount() {
            return selection == null ? 6 : 7;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null) return null;
            
            if (selection == null) columnIndex++;
            
            if (columnIndex == 1) {
                return data.getMethodNameAtRow(rowIndex);
            } else if (columnIndex == 2) {
                return data.getTimeInMcs0AtRow(rowIndex);
            } else if (columnIndex == 3) {
                return twoTimeStamps ? data.getTimeInMcs1AtRow(rowIndex) : 0;
            } else if (columnIndex == 4) {
                return data.getTotalTimeInMcs0AtRow(rowIndex);
            } else if (columnIndex == 5) {
                return twoTimeStamps ? data.getTotalTimeInMcs1AtRow(rowIndex) : 0;
            } else if (columnIndex == 6) {
                return data.getNInvocationsAtRow(rowIndex);
            } else if (columnIndex == 0) {
                if (selection.isEmpty()) return Boolean.FALSE;
                return selection.contains(idMap.get(data.getMethodIdAtRow(rowIndex)));
            }

            return null;
        }
        
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex == 0) {
                int methodId = data.getMethodIdAtRow(rowIndex);
                if (Boolean.TRUE.equals(aValue)) selection.add(idMap.get(methodId));
                else selection.remove(idMap.get(methodId));
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (selection == null) columnIndex++;
            
            if (columnIndex != 0) return false;
            return isSelectable(idMap.get(data.getMethodIdAtRow(rowIndex)));
        }
        
    }
    
}

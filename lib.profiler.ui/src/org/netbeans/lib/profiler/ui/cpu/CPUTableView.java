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
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.CheckBoxRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.McsTimeRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class CPUTableView extends JPanel {
    
    private final ProfilerClient client;
    
    private CPUTableModel tableModel;
    private ProfilerTable table;
    
    private FlatProfileContainer data;
    
    private Map<Integer, ClientUtils.SourceCodeSelection> idMap;
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    private boolean sampled = true;
    private boolean twoTimeStamps;
    
    
    public CPUTableView(ProfilerClient client, Set<ClientUtils.SourceCodeSelection> selection) {
        this.client = client;
        this.selection = selection;
        
        initUI();
    }
    
    
    void setData(final FlatProfileContainer newData, final Map<Integer, ClientUtils.SourceCodeSelection> newIdMap, final boolean _sampled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean structureChange = sampled != _sampled;
                sampled = _sampled;
                twoTimeStamps = newData == null ? false : newData.isCollectingTwoTimeStamps();
                idMap = newIdMap;
                if (tableModel != null) {
                    data = newData;
                    
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
                    
                    tableModel.fireTableDataChanged();
                }
                if (structureChange) {
                    int col = table.convertColumnIndexToView(6);
                    String colN = tableModel.getColumnName(6);
                    table.getColumnModel().getColumn(col).setHeaderValue(colN);
                    repaint();
                }
            }
        });
    }
    
    public void resetData() {
        setData(null, null, sampled);
    }
    
    
    public void showSelectionColumn() {
        table.setColumnVisibility(0, true);
    }
    
    public void refreshSelection() {
        tableModel.fireTableDataChanged();
    }
    
    
    protected abstract void performDefaultAction(ClientUtils.SourceCodeSelection value);
    
    protected abstract void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value);
    
    protected abstract void popupShowing();
    
    protected abstract void popupHidden();
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        tableModel = new CPUTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null) {
            protected ClientUtils.SourceCodeSelection getValueForPopup(int row) {
                return valueForRow(row);
            }
            protected void populatePopup(JPopupMenu popup, Object value) {
                CPUTableView.this.populatePopup(popup, (ClientUtils.SourceCodeSelection)value);
            }
            protected void popupShowing() {
                CPUTableView.this.popupShowing();
            }
            protected void popupHidden() {
                CPUTableView.this.popupHidden();
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
        
        table.setMainColumn(1);
        table.setFitWidthColumn(1);
        
        table.setSortColumn(3);
        table.setDefaultSortOrder(1, SortOrder.ASCENDING);
        
        table.setColumnVisibility(0, false);
        table.setColumnVisibility(2, false);
        table.setColumnVisibility(4, false);
        table.setColumnVisibility(6, false);
        
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
        
        table.setColumnRenderer(0, new CheckBoxRenderer());
        table.setColumnRenderer(1, new JavaNameRenderer());
        table.setColumnRenderer(2, renderers[0]);
        table.setColumnRenderer(3, renderers[1]);
        table.setColumnRenderer(4, renderers[2]);
        table.setColumnRenderer(5, renderers[3]);
        table.setColumnRenderer(6, renderers[4]);
        
        int w = new JLabel(table.getColumnName(0)).getPreferredSize().width;
        table.setDefaultColumnWidth(0, w + 15);
        table.setDefaultColumnWidth(2, renderers[0].getMaxNoBarWidth());
        table.setDefaultColumnWidth(3, renderers[1].getOptimalWidth());
        table.setDefaultColumnWidth(4, renderers[2].getMaxNoBarWidth());
        table.setDefaultColumnWidth(5, renderers[3].getMaxNoBarWidth());
        
        sampled = !sampled;
        w = new JLabel(table.getColumnName(6)).getPreferredSize().width;
        sampled = !sampled;
        w = Math.max(w, new JLabel(table.getColumnName(6)).getPreferredSize().width);
        table.setDefaultColumnWidth(6, Math.max(renderers[4].getNoBarWidth(), w + 15));
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    
    private ClientUtils.SourceCodeSelection valueForRow(int row) {
        if (data == null || row == -1) return null;
        if (row >= tableModel.getRowCount()) return null; // #239936
        row = table.convertRowIndexToModel(row);
        return selectionForId(data.getMethodIdAtRow(row));
    }
    
    private ClientUtils.SourceCodeSelection selectionForId(int methodId) {
        ProfilingSessionStatus sessionStatus = client.getStatus();
        sessionStatus.beginTrans(false);
        try {
            String className = sessionStatus.getInstrMethodClasses()[methodId];
            String methodName = sessionStatus.getInstrMethodNames()[methodId];
            String methodSig = sessionStatus.getInstrMethodSignatures()[methodId];
            return new ClientUtils.SourceCodeSelection(className, methodName, methodSig);
        } finally {
            sessionStatus.endTrans();
        }
    }
    
    
    private class CPUTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 1) {
                return "Name";
            } else if (columnIndex == 2) {
                return "Self Time";
            } else if (columnIndex == 3) {
                return "Self Time (CPU)";
            } else if (columnIndex == 4) {
                return "Total Time";
            } else if (columnIndex == 5) {
                return "Total Time (CPU)";
            } else if (columnIndex == 6) {
                return sampled ? "Hits" : "Invocations";
            } else if (columnIndex == 0) {
                return "Selected";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
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
            return 7;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null) return null;
            
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
            if (columnIndex == 0) {
                int methodId = data.getMethodIdAtRow(rowIndex);
                if (Boolean.TRUE.equals(aValue)) selection.add(idMap.get(methodId));
                else selection.remove(idMap.get(methodId));
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }
        
    }
    
}

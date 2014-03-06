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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class CPUTableView extends JPanel {
    
    private CPUTableModel tableModel;
    private ProfilerTable table;
    
    private FlatProfileContainer data;
    
    
    public CPUTableView() {
        initUI();
    }
    
    
    public void setData(final FlatProfileContainer d) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (tableModel != null) {
                    data = d;
                    
                    long[] maxTimes = new long[4];
                    int maxInvocations = 0;
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        maxTimes[0] += data.getTimeInMcs0AtRow(row);
                        maxTimes[1] += data.getTimeInMcs1AtRow(row);
                        maxTimes[2] += data.getTotalTimeInMcs0AtRow(row);
                        maxTimes[3] += data.getTotalTimeInMcs1AtRow(row);
                        maxInvocations += data.getNInvocationsAtRow(row);
                    }
                    
                    renderers[0].setMaxValue(maxTimes[0]);
                    renderers[1].setMaxValue(maxTimes[1]);
                    renderers[2].setMaxValue(maxTimes[2]);
                    renderers[3].setMaxValue(maxTimes[3]);
                    renderers[4].setMaxValue(maxInvocations);
                    
                    tableModel.fireTableDataChanged();
                }
            }
        });
    }
    
    
    private HideableBarRenderer[] renderers;
    
    private void initUI() {
        tableModel = new CPUTableModel();
        
        table = new ProfilerTable(tableModel, true, true, null);
        table.setColumnVisibility(1, false);
        table.setColumnVisibility(3, false);
        table.setColumnVisibility(5, false);
        table.setSortColumn(2);
        
        renderers = new HideableBarRenderer[5];
        
        renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.millisecondsFormat()));
        renderers[1] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.millisecondsFormat()));
        renderers[2] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.millisecondsFormat()));
        renderers[3] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.millisecondsFormat()));
        renderers[4] = new HideableBarRenderer(new NumberRenderer());
        
        long refTime = 123456;
        renderers[0].setMaxValue(refTime);
        renderers[1].setMaxValue(refTime);
        renderers[2].setMaxValue(refTime);
        renderers[3].setMaxValue(refTime);
        renderers[4].setMaxValue(refTime);
        
        table.setColumnRenderer(0, new JavaNameRenderer());
        table.setColumnRenderer(1, renderers[0]);
        table.setColumnRenderer(2, renderers[1]);
        table.setColumnRenderer(3, renderers[2]);
        table.setColumnRenderer(4, renderers[3]);
        table.setColumnRenderer(5, renderers[4]);
        
        table.setDefaultColumnWidth(1, renderers[0].getNoBarWidth());
        table.setDefaultColumnWidth(2, renderers[1].getOptimalWidth());
        table.setDefaultColumnWidth(3, renderers[2].getNoBarWidth());
        table.setDefaultColumnWidth(4, renderers[3].getNoBarWidth());
        table.setDefaultColumnWidth(5, renderers[4].getNoBarWidth());
        
        ProfilerTableContainer tableContainer = new ProfilerTableContainer(table, false, null);
        
        setLayout(new BorderLayout());
        add(tableContainer, BorderLayout.CENTER);
    }
    
    
    private class CPUTableModel extends AbstractTableModel {
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Name";
            } else if (columnIndex == 1) {
                return "Self Time";
            } else if (columnIndex == 2) {
                return "Self Time (CPU)";
            } else if (columnIndex == 3) {
                return "Total Time";
            } else if (columnIndex == 4) {
                return "Total Time (CPU)";
            } else if (columnIndex == 5) {
                return "Samples";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return String.class;
            } else if (columnIndex == 5) {
                return Integer.class;
            } else {
                return Long.class;
            }
        }

        public int getRowCount() {
            return data == null ? 0 : data.getNRows();
        }

        public int getColumnCount() {
            return 6;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null) return null;
            
            if (columnIndex == 0) {
                return data.getMethodNameAtRow(rowIndex);
            } else if (columnIndex == 1) {
                return data.getTimeInMcs0AtRow(rowIndex);
            } else if (columnIndex == 2) {
                return data.getTimeInMcs1AtRow(rowIndex);
            } else if (columnIndex == 3) {
                return data.getTotalTimeInMcs0AtRow(rowIndex);
            } else if (columnIndex == 4) {
                return data.getTotalTimeInMcs1AtRow(rowIndex);
            } else if (columnIndex == 5) {
                return data.getNInvocationsAtRow(rowIndex);
            }

            return null;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
        
    }
    
}

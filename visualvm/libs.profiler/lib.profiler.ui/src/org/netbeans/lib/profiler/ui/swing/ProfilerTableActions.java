/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;

/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerTableActions {
    
    static void install(ProfilerTable table) {
        new ProfilerTableActions(table).install();
    }
    
    
    private final ProfilerTable table;
    
    private ProfilerTableActions(ProfilerTable table) { this.table = table; }
    
    private void install() {
        ActionMap map = table.getActionMap();
        
        map.put("selectNextColumn", selectNextColumnAction()); // NOI18N
        map.put("selectPreviousColumn", selectPreviousColumnAction()); // NOI18N
        map.put("selectNextColumnCell", createNextCellAction()); // NOI18N
        map.put("selectPreviousColumnCell", createPreviousCellAction()); // NOI18N
        map.put("selectFirstColumn", selectFirstColumnAction()); // NOI18N
        map.put("selectLastColumn", selectLastColumnAction()); // NOI18N
        map.put("selectNextRowCell", selectNextRowAction()); // NOI18N
        map.put("selectPreviousRowCell", selectPreviousRowAction()); // NOI18N
        
        map.put("selectNextRowExtendSelection", map.get("selectNextRow")); // NOI18N
        map.put("selectPreviousRowExtendSelection", map.get("selectPreviousRow")); // NOI18N
        map.put("selectNextColumnExtendSelection", map.get("selectNextColumn")); // NOI18N
        map.put("selectPreviousColumnExtendSelection", map.get("selectPreviousColumn")); // NOI18N
        map.put("selectLastColumnExtendSelection", map.get("selectLastColumn")); // NOI18N
        map.put("selectFirstColumnExtendSelection", map.get("selectFirstColumn")); // NOI18N
    }
    
    private Action selectNextRowAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;
                
                int row = table.getSelectedRow();
                if (row == -1) {
                    table.selectColumn(cModel.getFirstVisibleColumn(), false);
                    table.selectRow(0, true);
                } else {
                    if (++row == table.getRowCount()) {
                        row = 0;
                        int column = table.getSelectedColumn();
                        if (column == -1) column = cModel.getFirstVisibleColumn();
                        column = cModel.getNextVisibleColumn(column);
                        table.selectColumn(column, false);
                    }
                    table.selectRow(row, true);
                }
            }
        };
    }
    
    private Action selectPreviousRowAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;
                
                int row = table.getSelectedRow();
                if (row == -1) {
                    table.selectColumn(cModel.getLastVisibleColumn(), false);
                    table.selectRow(table.getRowCount() - 1, true);
                } else {
                    if (--row == -1) {
                        row = table.getRowCount() - 1;
                        int column = table.getSelectedColumn();
                        if (column == -1) column = cModel.getLastVisibleColumn();
                        column = cModel.getPreviousVisibleColumn(column);
                        table.selectColumn(column, false);
                    }
                    table.selectRow(row, true);
                }
            }
        };
    }
    
    private Action selectFirstColumnAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;
                
                int row = table.getSelectedRow();
                table.selectColumn(cModel.getFirstVisibleColumn(), row != -1);
                if (row == -1) table.selectRow(0, true);
            }
        };
    }
    
    private Action selectLastColumnAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;
                
                int row = table.getSelectedRow();
                table.selectColumn(cModel.getLastVisibleColumn(), row != -1);
                if (row == -1) table.selectRow(0, true);
            }
        };
    }
    
    private Action selectNextColumnAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;
                
                int column = table.getSelectedColumn();
                if (column == -1) {
                    table.selectColumn(cModel.getFirstVisibleColumn(), false);
                    table.selectRow(0, true);
                } else {
                    int nextColumn = cModel.getNextVisibleColumn(column);
                    if (nextColumn > column) table.selectColumn(nextColumn, true);
                }
            }
        };
    }
    
    private Action selectPreviousColumnAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;
                
                int column = table.getSelectedColumn();
                if (column == -1) {
                    table.selectColumn(cModel.getFirstVisibleColumn(), false);
                    table.selectRow(0, true);
                } else {
                    int previousColumn = cModel.getPreviousVisibleColumn(column);
                    if (previousColumn < column) table.selectColumn(previousColumn, true);
                }
            }
        };
    }
    
    private Action createNextCellAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;

                int column = table.getSelectedColumn();
                if (column == -1) {
                    table.selectColumn(cModel.getFirstVisibleColumn(), false);
                    table.selectRow(0, true);
                } else {
                    int nextColumn = cModel.getNextVisibleColumn(column);
                    boolean differentRow = nextColumn <= column && table.getRowCount() > 1;
                    if (nextColumn != column) table.selectColumn(nextColumn, !differentRow);
                    if (differentRow) {
                        int row = table.getSelectedRow();
                        int newRow = getNextRow(row);
                        if (row != newRow) table.selectRow(newRow, true);
                    }
                }
            }
        };
    }
    
    private Action createPreviousCellAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerColumnModel cModel = table._getColumnModel();
                if (table.getRowCount() == 0 || cModel.getVisibleColumnCount() == 0) return;

                int column = table.getSelectedColumn();
                if (column == -1) {
                    table.selectColumn(cModel.getFirstVisibleColumn(), false);
                    table.selectRow(0, true);
                } else {
                    int previousColumn = cModel.getPreviousVisibleColumn(column);
                    boolean differentRow = previousColumn >= column && table.getRowCount() > 1;
                    if (previousColumn != column) table.selectColumn(previousColumn, !differentRow);
                    if (differentRow) {
                        int row = table.getSelectedRow();
                        int newRow = getPreviousRow(row);
                        if (row != newRow) table.selectRow(newRow, true);
                    }
                }
            }
        };
    }
    
    private int getNextRow(int row) {
        return ++row == table.getRowCount() ? 0 : row;
    }
    
    private int getPreviousRow(int row) {
        return --row == -1 ? table.getRowCount() - 1 : row;
    }
    
}

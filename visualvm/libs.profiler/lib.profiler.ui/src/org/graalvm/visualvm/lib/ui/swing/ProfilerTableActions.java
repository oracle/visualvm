/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.swing;

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

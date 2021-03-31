/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jvmstat.application;

import java.awt.Component;
import java.util.EventObject;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 *
 * @author Jiri Sedlacek
 */
class ConnectionsTable extends JTable {

    ConnectionsTable(TableModel model, TableCellRenderer cellRenderer,
                     TableCellEditor cellEditor) {

        super(model);

        setDefaultRenderer(Object.class, cellRenderer);
        setDefaultEditor(Object.class, new CellEditor(cellEditor));

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getSelectionModel().addListSelectionListener(new SelectionListener(this));

        setTableHeader(null);
        
    }


    public void columnMarginChanged(ChangeEvent e) {
	resizeAndRepaint();
    }

    public void editingStopped(ChangeEvent e) {}
    private void doEditingStopped(ChangeEvent e) {
        super.editingStopped(e);
        super.removeEditor();
    }

    public void editingCanceled(ChangeEvent e) {}
//    private void doEditingCanceled(ChangeEvent e) {
//        super.editingCanceled(e);
//    }

    public void removeEditor() {}
//    void doRemoveEditor() {
//        super.removeEditor();
//    }


    private static class SelectionListener implements ListSelectionListener {

        private final ConnectionsTable table;
        private int lastRow = -1;


        SelectionListener(ConnectionsTable table) {
            this.table = table;
        }

        public void valueChanged(ListSelectionEvent e) {
            int row = table.getSelectedRow();
            if (lastRow == row) return;

            if (lastRow != -1) table.setRowHeight(lastRow, table.getRowHeight());
            if (row != -1) {
                table.editCellAt(row, 0, null);
                if (table.isShowing()) table.requestFocusInWindow();
            } else {
                table.doEditingStopped(null);
            }
            
            lastRow = row;
        }
        
    }


    private static class CellEditor extends JPanel implements TableCellEditor {

        private final TableCellEditor impl;


        CellEditor(TableCellEditor impl) {
            this.impl = impl;
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row,
                                                     int column) {
            Component editor = impl.getTableCellEditorComponent(table, value,
                                                     isSelected, row, column);
            table.setRowHeight(row, editor.getPreferredSize().height);
            return editor;
        }


        public Object getCellEditorValue() {
            return impl.getCellEditorValue();
        }

        public boolean stopCellEditing() {
            return impl.stopCellEditing();
        }

        public boolean isCellEditable(EventObject anEvent) {
            return impl.isCellEditable(anEvent);
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return impl.shouldSelectCell(anEvent);
        }

        public void cancelCellEditing() {
            impl.cancelCellEditing();
        }

        public void addCellEditorListener(CellEditorListener l) {
            impl.addCellEditorListener(l);
        }

        public void removeCellEditorListener(CellEditorListener l) {
            impl.removeCellEditorListener(l);
        }

    }

}

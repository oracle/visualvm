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

package org.graalvm.visualvm.modules.mbeans;

import java.awt.Component;
import java.util.EventObject;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

@SuppressWarnings("serial")
class XTextFieldEditor extends XTextField implements TableCellEditor {

    protected EventListenerList evtListenerList = new EventListenerList();
    protected ChangeEvent changeEvent = new ChangeEvent(this);

    private FocusListener editorFocusListener = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            // https://java.net/jira/browse/VISUALVM-167
            // must not call fireEditingStopped() here!
        }
    };

    public XTextFieldEditor() {
        super();
        textField.addFocusListener(editorFocusListener);
    }

    //edition stopped on JMenuItem selection & JTextField selection
    @Override
    public void  actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if ((e.getSource() instanceof JMenuItem) ||
            (e.getSource() instanceof JTextField)) {
            fireEditingStopped();
        }
    }

    //edition stopped on drag & drop success
    protected void dropSuccess() {
        fireEditingStopped();
    }

    //TableCellEditor implementation

    public void addCellEditorListener(CellEditorListener listener) {
        evtListenerList.add(CellEditorListener.class,listener);
    }

    public void removeCellEditorListener(CellEditorListener listener) {
        evtListenerList.remove(CellEditorListener.class, listener);
    }

    protected void fireEditingStopped() {
        CellEditorListener listener;
        Object[] listeners = evtListenerList.getListenerList();
        for (int i=0;i< listeners.length;i++) {
            if (listeners[i] == CellEditorListener.class) {
                listener = (CellEditorListener) listeners[i+1];
                listener.editingStopped(changeEvent);
            }
        }
    }

    protected void fireEditingCanceled() {
        CellEditorListener listener;
        Object[] listeners = evtListenerList.getListenerList();
        for (int i=0;i< listeners.length;i++) {
            if (listeners[i] == CellEditorListener.class) {
                listener = (CellEditorListener) listeners[i+1];
                listener.editingCanceled(changeEvent);
            }
        }
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    public boolean isCellEditable(EventObject event) {
        return true;
    }

    public boolean shouldSelectCell(EventObject event) {
        return true;
    }

    public Object getCellEditorValue() {
        Object object = getValue();

        if (object instanceof XObject) {
            return ((XObject) object).getObject();
        }
        else {
            return object;
        }
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        String className;
        if (table instanceof XTable) {
            XTable mytable = (XTable) table;
            className = mytable.getClassName(row);
        } else {
            className = String.class.getName();
        }
        Class<?> clazz;
        try {
            clazz = Utils.getClass(className);
        } catch (ClassNotFoundException e) {
            clazz = null;
        }
        init(value, clazz);

        return this;
    }

}

/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.truffle.ui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
class EditableHistoryCombo extends JComboBox {
    
    private Runnable onTextChange;
    
    EditableHistoryCombo() {
        super(new ComboHistoryModel(10));
        
        setEditable(true);
//        putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE); // NOI18N
        
        setPrototypeDisplayValue("org.netbeans.lib.profiler.ui.swing.XXXXXXXXXXXXXXXXXXXX"); // NOI18N
        Dimension dim = getPreferredSize();
        dim.height = !UIUtils.isNimbusLookAndFeel() ? getMinimumSize().height :
                     new JTextField("X").getPreferredSize().height; // NOI18N
        
        setMinimumSize(dim);
        setPreferredSize(dim);
        setMaximumSize(dim);
        
        JTextComponent comp = getTextComponent();
        if (comp != null) comp.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onChange(); }
            public void removeUpdate(DocumentEvent e)  { onChange(); }
            public void changedUpdate(DocumentEvent e) { onChange(); }
            private void onChange() { if (onTextChange != null) onTextChange.run(); }
        });
    }
    
    
    JTextComponent getTextComponent() {
        Component comp = getEditor().getEditorComponent();
        return comp instanceof JTextComponent ? (JTextComponent)comp : null;
    }
    
    String getText() {
        JTextComponent textC = getTextComponent();
        return textC != null ? textC.getText() : getSelectedItem().toString();
    }
    
    void setOnTextChangeHandler(Runnable handler) {
        onTextChange = handler;
    }
    
    
    private static class ComboHistoryModel extends DefaultComboBoxModel {
        
        private final int historySize;
        
        
        ComboHistoryModel(int historySize) {
            this.historySize = historySize;
        }
        
        
        public void addElement(Object item) {
            insertElementAt(item, 0);
        }
        
        public void insertElementAt(Object item, int index) {
            int current = getIndexOf(item);
            if (current == index) return;
            
            if (current != -1) removeElementAt(current);
            super.insertElementAt(item, index);
            
            if (getSize() > historySize) removeElementAt(historySize);
            
            setSelectedItem(item);
        }
        
    }
    
}

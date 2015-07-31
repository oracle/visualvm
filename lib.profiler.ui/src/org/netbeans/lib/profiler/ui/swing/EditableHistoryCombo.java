/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
        
        setPrototypeDisplayValue("java.lang.String.equals(Object)"); // NOI18N
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

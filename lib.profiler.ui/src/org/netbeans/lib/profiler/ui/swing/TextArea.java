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

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

/**
 *
 * @author Jiri Sedlacek
 */
public class TextArea extends JTextArea {
    
    private static ResourceBundle BUNDLE() {
        return ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.swing.Bundle"); // NOI18N
    }
    
    private String hint;
    private Color hintFg;
    private boolean showsHint;
    
    
    public TextArea() {
        super();
    }
    
    public TextArea(String text) {
        super(text);
    }
    
    public TextArea(int rows, int columns) {
        super(rows, columns);
    }
    
    public TextArea(String text, int rows, int columns) {
        super(text, rows, columns);
    }
    
    public TextArea(Document doc) {
        super(doc);
    }
    
    public TextArea(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
    }
    
    
    // --- Change support ------------------------------------------------------
    
    private boolean changeListener;
    
    public void setText(String t) {
        if (!changeListener) {
            changeListener = true;
            getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updated(); }
                public void removeUpdate(DocumentEvent e) { updated(); }
                public void changedUpdate(DocumentEvent e) { updated(); }
                private void updated() { if (!showsHint()) changed(); }
            });
        }
        super.setText(t);
    }
    
    protected void changed() {}
    
    
    // --- Hint support --------------------------------------------------------
    
    public void setHint(String hint) {
        hideHint();
        this.hint = hint;
        showHint();
    }
    
    public String getHint() {
        return hint;
    }
    
    public boolean showsHint() {
        return showsHint;
    }
    
    
    protected void processFocusEvent(FocusEvent e) {
        if (isFocusOwner()) hideHint(); else showHint();
        super.processFocusEvent(e);
    }
    
    
    private void showHint() {
        if (hint != null && getText().isEmpty()) {
            showsHint = true;
            setText(hint);
            hintFg = getForeground();
            setForeground(getDisabledTextColor());
        }
    }
    
    private void hideHint() {
        if (showsHint) {
            showsHint = false;
            setForeground(hintFg);
            setText(null);
        }
    }
    
    
    // --- Popup menu support --------------------------------------------------
    
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.isPopupTrigger()) showPopup(e);
    }
    
    
    private void showPopup(MouseEvent e) {
        boolean editable = isEditable();
        boolean selectedText = getSelectedText() != null;
        
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem miCut = new JMenuItem(BUNDLE().getString("TextArea_Cut")) { // NOI18N
            protected void fireActionPerformed(ActionEvent ae) {
                super.fireActionPerformed(ae);
                cut();
                requestFocusInWindow();
            }
        };
        miCut.setEnabled(editable && selectedText);
        popup.add(miCut);
        
        JMenuItem miCopy = new JMenuItem(BUNDLE().getString("TextArea_Copy")) { // NOI18N
            protected void fireActionPerformed(ActionEvent ae) {
                super.fireActionPerformed(ae);
                copy();
                requestFocusInWindow();
            }
        };
        miCopy.setEnabled(selectedText);
        popup.add(miCopy);
        
        JMenuItem miPaste = new JMenuItem(BUNDLE().getString("TextArea_Paste")) { // NOI18N
            protected void fireActionPerformed(ActionEvent ae) {
                super.fireActionPerformed(ae);
                hideHint();
                try {
                    replaceSelection(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).
                                     getTransferData(DataFlavor.stringFlavor).toString());
                    requestFocusInWindow();
                } catch (Exception ex) {}
                showHint();
            }
        };
        try {
            Transferable clipboardContent = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
            miPaste.setEnabled(editable && clipboardContent != null && clipboardContent.isDataFlavorSupported(
                                                                       DataFlavor.stringFlavor));
            requestFocusInWindow();
        } catch (Exception ex) {
            miPaste.setEnabled(false);
        }
        popup.add(miPaste);
        
        if (editable) {
            JMenuItem miDelete = new JMenuItem(BUNDLE().getString("TextArea_Delete")) { // NOI18N
                protected void fireActionPerformed(ActionEvent ae) {
                    super.fireActionPerformed(ae);
                    try {
                        int selStart = getSelectionStart();
                        getDocument().remove(selStart, getSelectionEnd() - selStart);
                        requestFocusInWindow();
                    } catch (Exception ex) {}
                }
            };
            miDelete.setEnabled(selectedText);
            popup.add(miDelete);
        }
        
        popup.addSeparator();
        
        JMenuItem miSelect = new JMenuItem(BUNDLE().getString("TextArea_SelectAll")) { // NOI18N
            protected void fireActionPerformed(ActionEvent ae) {
                super.fireActionPerformed(ae);
                selectAll();
                requestFocusInWindow();
            }
        };
        miSelect.setEnabled(!showsHint() && !getText().isEmpty());
        popup.add(miSelect);
        
        customizePopup(popup);
        
        popup.show(this, e.getX(), e.getY());
    }
    
    protected void customizePopup(JPopupMenu popup) {}
    
    
    // --- Resize support ------------------------------------------------------
    
    protected void processKeyEvent(KeyEvent e) {
        if (e.isControlDown() && e.getID() == KeyEvent.KEY_RELEASED) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_EQUALS || keyCode == KeyEvent.VK_PLUS) {
                if (changeSize(e.isShiftDown(), true)) e.consume();
            } else if (keyCode == KeyEvent.VK_MINUS) {
                if (changeSize(e.isShiftDown(), false)) e.consume();
            } else if (keyCode == KeyEvent.VK_0) {
                if (resetSize()) e.consume();
            }
        }
        
        if (!e.isConsumed()) super.processKeyEvent(e);
    }
    
    protected boolean changeSize(boolean vertical, boolean direction) { return false; }
    
    protected boolean resetSize() { return false; }
    
    protected final JMenu createResizeMenu() {
        JMenu menu = new JMenu(BUNDLE().getString("TextArea_Resize")); // NOI18N
                        
        JMenuItem horizPlus = new JMenuItem(BUNDLE().getString("TextArea_HorizPlus")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) { changeSize(false, true); }
        };
        horizPlus.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_MASK));
        menu.add(horizPlus);
        JMenuItem horizMinus = new JMenuItem(BUNDLE().getString("TextArea_HorizMinus")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) { changeSize(false, false); }
        };
        horizMinus.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_MASK));
        menu.add(horizMinus);
        JMenuItem vertPlus = new JMenuItem(BUNDLE().getString("TextArea_VertPlus")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) { changeSize(true, true); }
        };
        vertPlus.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        menu.add(vertPlus);
        JMenuItem vertMinus = new JMenuItem(BUNDLE().getString("TextArea_VertMinus")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) { changeSize(true, false); }
        };
        vertMinus.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        menu.add(vertMinus);

        menu.addSeparator();

        JMenuItem reset = new JMenuItem(BUNDLE().getString("TextArea_DefaultSize")) { // NOI18N
            protected void fireActionPerformed(ActionEvent e) { resetSize(); }
        };
        reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_MASK));
        menu.add(reset);
        
        return menu;
    }
    
}

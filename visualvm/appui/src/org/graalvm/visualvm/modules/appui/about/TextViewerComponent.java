/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.appui.about;

import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ResourceBundle;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.UIManager;


/**
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class TextViewerComponent extends JTextArea implements MouseListener {

    /** Private Writer that extracts correctly formatted string from HTMLDocument */
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.modules.appui.about.Bundle"); // NOI18N
    private static final String CUT_STRING = messages.getString("TextViewerComponent_CutString"); // NOI18N
    private static final String COPY_STRING = messages.getString("TextViewerComponent_CopyString"); // NOI18N
    private static final String PASTE_STRING = messages.getString("TextViewerComponent_PasteString"); // NOI18N
    private static final String DELETE_STRING = messages.getString("TextViewerComponent_DeleteString"); // NOI18N
    private static final String SELECT_ALL_STRING = messages.getString("TextViewerComponent_SelectAllString"); // NOI18N
                                                                                                        // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ActionListener popupListener;
    private JMenuItem itemCopy;
    private JMenuItem itemCut;
    private JMenuItem itemDelete;
    private JMenuItem itemPaste;
    private JMenuItem itemSelectAll;

    // --- Popup menu support ----------------------------------------------------
    private JPopupMenu popupMenu;
    private boolean showPopup = true;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TextViewerComponent() {
        setEditable(false);
        setOpaque(true);
        setAutoscrolls(true);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Label.font").getSize())); // NOI18N
        setBackground(UISupport.getDefaultBackground());
        addMouseListener(this);
    }

    public TextViewerComponent(String text) {
        this();
        setText(text);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setShowPopup(boolean showPopup) {
        this.showPopup = showPopup;
    }

    public boolean getShowPopup() {
        return showPopup;
    }

    public void deleteSelection() {
        try {
            getDocument().remove(getSelectionStart(), getSelectionEnd() - getSelectionStart());
        } catch (Exception ex) {
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            if (isEnabled() && isFocusable() && showPopup) {
                JPopupMenu popup = getPopupMenu();

                if (popup != null) {
                    updatePopupMenu();

                    if (!hasFocus()) {
                        requestFocus(); // required for Select All functionality
                    }

                    popup.show(this, e.getX(), e.getY());
                }
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void paste() {
        try {
            replaceSelection(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this)
                                    .getTransferData(DataFlavor.stringFlavor).toString());
        } catch (Exception ex) {
        }
    }

    protected JPopupMenu getPopupMenu() {
        if (popupMenu == null) {
            popupMenu = createPopupMenu();
        }

        return popupMenu;
    }

    protected JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        popupListener = createPopupListener();

        itemCut = new JMenuItem(CUT_STRING);
        itemCopy = new JMenuItem(COPY_STRING);
        itemPaste = new JMenuItem(PASTE_STRING);
        itemDelete = new JMenuItem(DELETE_STRING);
        itemSelectAll = new JMenuItem(SELECT_ALL_STRING);

        itemCut.addActionListener(popupListener);
        itemCopy.addActionListener(popupListener);
        itemPaste.addActionListener(popupListener);
        itemDelete.addActionListener(popupListener);
        itemSelectAll.addActionListener(popupListener);

        popup.add(itemCut);
        popup.add(itemCopy);
        popup.add(itemPaste);
        popup.add(itemDelete);
        popup.addSeparator();
        popup.add(itemSelectAll);

        return popup;
    }

    protected void updatePopupMenu() {
        // Cut
        itemCut.setEnabled(isEditable() && (getSelectedText() != null));

        // Copy
        itemCopy.setEnabled(getSelectedText() != null);

        // Paste
        try {
            Transferable clipboardContent = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
            itemPaste.setEnabled(isEditable() && (clipboardContent != null)
                                 && clipboardContent.isDataFlavorSupported(DataFlavor.stringFlavor));
        } catch (Exception e) {
            itemPaste.setEnabled(false);
        }

        // Delete
        if (isEditable()) {
            itemDelete.setVisible(true);
            itemDelete.setEnabled(getSelectedText() != null);
        } else {
            itemDelete.setVisible(false);
        }

        // Select All
        // always visible and enabled...
    }

    private ActionListener createPopupListener() {
        return new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == itemCut) {
                        cut();
                    } else if (e.getSource() == itemCopy) {
                        copy();
                    } else if (e.getSource() == itemPaste) {
                        paste();
                    } else if (e.getSource() == itemDelete) {
                        deleteSelection();
                    } else if (e.getSource() == itemSelectAll) {
                        selectAll();
                    }
                }
            };
    }
}

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

package com.sun.tools.visualvm.heapviewer.swing;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class LinkButton extends JButton {
    
    private boolean mouseOver;
    private boolean focused;
    private String text;
    
    public LinkButton() {
        super();
        tweak();
    }
    
    public LinkButton(String text) {
        super(text);
        tweak();
    }
    
    public LinkButton(String text, Icon icon) {
        super(text, icon);
        tweak();
    }
    
    
    public void setText(String text) {
        this.text = text.replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;"); // NOI18N
        if (!mouseOver && !focused) super.setText("<html>" + this.text + "</html>"); // NOI18N
        else super.setText("<html><a href='#'>" + this.text + "</a></html>"); // NOI18N
    }
    
    protected void fireActionPerformed(ActionEvent e) {
        super.fireActionPerformed(e);
        clicked();
    }
    
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    protected void clicked() {}
    
    protected void middleClicked(MouseEvent e) {}
    
    protected void populatePopup(JPopupMenu popup) {}
    
    
    protected void processKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_CONTEXT_MENU ||
           (code == KeyEvent.VK_F10 && e.getModifiers() == InputEvent.SHIFT_MASK)) {
            e.consume();
            showPopupMenu(null);
        }
        
        super.processKeyEvent(e);
    }
    
    
    private void tweak() {
        setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter mouse = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                mouseOver = true;
                setText(text);
            }
            public void mouseExited(MouseEvent e) {
                mouseOver = false;
                setText(text);
            }
            public void mouseMoved(MouseEvent e) {
                if (!mouseOver) {
                    mouseOver = true;
                    setText(text);
                }
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopupMenu(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopupMenu(e);
            }
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) middleClicked(e);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                focused = true;
                setText(text);
            }
            public void focusLost(FocusEvent e) {
                focused = false;
                mouseOver = false;
                setText(text);
            }
        });
    }
    
    private void showPopupMenu(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        populatePopup(popup);
        
        if (popup.getComponentCount() > 0) {
            Dimension pref = popup.getPreferredSize();
            if (e == null) {
                popup.show(this, getWidth() / 2, -pref.height);
            } else {
                popup.show(this, e.getX(), e.getY() - pref.height);
            }
        }
    }
    
}

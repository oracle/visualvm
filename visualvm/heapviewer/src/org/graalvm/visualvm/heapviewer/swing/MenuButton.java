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

package org.graalvm.visualvm.heapviewer.swing;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Jiri Sedlacek
 */
public class MenuButton extends JMenuItem implements ChangeListener {
    
    private static final String CONSUME_EVENT_KEY = "PopupMenu.consumeEventOnClose"; // NOI18N
    private Object originalConsumeEvent;
    
    private final Color foreground;
    private final Color background;
    private final boolean opaque;
    
    private boolean listening;
    private boolean hovered;
    
    private boolean skipGainedFocus;
    
    
    public MenuButton(String text) {
        this(text, false);
    }
    
    public MenuButton(String text, final boolean selectable) {
        super(text);
        
        
        foreground = getForeground();
        background = getBackground();
        opaque = isOpaque();
        
        setModel(new JToggleButton.ToggleButtonModel() {
//            private boolean fromGroup;
            public void setSelected(boolean b) {
                if (!selectable) return;
//                System.err.println(">>> Selecting " + getText() + " from " + isSelected() + " to " + b);
                boolean selected = isSelected();
                if (selected == b) return;
                super.setSelected(b);
                if (selected != isSelected())
                    if (selected) {
                        setForeground(foreground);
                        setBackground(background);
                        setOpaque(opaque);
                        repaint();
                        deselected();
                    } else {
//                        setForeground(UIManager.getColor("List.selectionForeground"));
                        setForeground(Color.WHITE);
                        setBackground(UIManager.getColor("List.selectionBackground")); // NOI18N
                        setOpaque(true);
                        repaint();
                        selected();
                    }
            }
        });
        
        setFocusable(true);
        setRolloverEnabled(true);
        setBorder(BorderFactory.createEmptyBorder(0, -11, 0, 0));
        
        MouseAdapter ml = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
//                System.err.println(">>> mouseEntered");
                if (!canSelect()) return;
                hovered = true;
                showHover();
            }
            public void mouseMoved(MouseEvent e) {
//                System.err.println(">>> mouseMoved");
                if (!canSelect()) return;
                hovered = true;
                showHover();
            }
            public void mouseExited(MouseEvent e) {
//                System.err.println(">>> mouseExited");
                hovered = false;
                if (!canSelect()) return;
                hideHover();
            }
            public void mousePressed(MouseEvent e) {
                if (!canSelect()) return;
                requestFocusInWindow();
                showHover();
            }
            public void mouseReleased(MouseEvent e) {
                if (!canSelect()) return;
                requestFocusInWindow();
                showHover();
            }
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    clicked();
                    if (selectable) setSelected(true);
                }
            }
        };
        addMouseListener(ml);
        addMouseMotionListener(ml);
        
        
        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ' || e.getKeyChar() == '\n') { // NOI18N
                    clicked();
                    if (selectable) setSelected(true);
                }
            }
        });
        
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (skipGainedFocus) {
                    skipGainedFocus = false;
                } else {
                    if (!canSelect()) return;
                    showHover();
                    scrollRectToVisible(getBounds());
                }
            }
            public void focusLost(FocusEvent e) {
//                System.err.println(">>> focusLost");
                hovered = false;
                if (!canSelect()) return;
                hideHover();
            }
        });
    }
    
    
    public void requestFocus() {
//        skipGainedFocus = true;
        super.requestFocus();
    }
    
    
//    public void addNotify() {
//        super.addNotify();
//        MenuSelectionManager.defaultManager().addChangeListener(this);
//    }
//    
//    public void removeNotify() {
//        MenuSelectionManager.defaultManager().removeChangeListener(this);
//        super.removeNotify();
//    }
    
    public void stateChanged(ChangeEvent e) {
//                System.err.println(">>> change " + hovered + " - " + Arrays.toString(MenuSelectionManager.defaultManager().getSelectedPath()));
//        System.err.println(">>> State changed at " + getText());
        if (MenuSelectionManager.defaultManager().getSelectedPath().length == 0 && hovered) {
            if (isFocusOwner()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { showHover(); }
                });
            } else {
                showHover();
            }
        }
    }
    
    
    protected void processMouseEvent(MouseEvent e) {
        if (canSelect()) super.processMouseEvent(e);
    }
    
    
    protected void clicked() { /*System.err.println(">>> clicked " + getText());*/ }
    
    protected void selected() { /*System.err.println(">>> selected " + getText());*/ }
    
    protected void deselected() { /*System.err.println(">>> deselected " + getText());*/ }
    
    
    private void showHover() {
        originalConsumeEvent = UIManager.get(CONSUME_EVENT_KEY);
        UIManager.put(CONSUME_EVENT_KEY, Boolean.FALSE);
        MenuSelectionManager msm = MenuSelectionManager.defaultManager();
        msm.setSelectedPath(new MenuElement[] { MenuButton.this });
        if (!listening) {
            msm.addChangeListener(this);
            listening = true;
        }
        repaint();
    }
    
    private void hideHover() {
        MenuSelectionManager msm = MenuSelectionManager.defaultManager();
        if (listening) {
            msm.removeChangeListener(this);
            listening = false;
        }
        msm.clearSelectedPath();
        UIManager.put(CONSUME_EVENT_KEY, originalConsumeEvent);
        repaint();
    }
    
    private boolean canSelect() {
        MenuElement[] sel = MenuSelectionManager.defaultManager().getSelectedPath();
//        System.err.println(">>> Window " + SwingUtilities.getWindowAncestor(this).getName() + " sel " + Arrays.toString(sel));
//        return false;
        if (sel.length == 0) return true;
        return sel[0] instanceof MenuButton;
    }
    
}

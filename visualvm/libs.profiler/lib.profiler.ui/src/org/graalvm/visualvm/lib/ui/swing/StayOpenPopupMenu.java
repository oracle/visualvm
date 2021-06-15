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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * A JPopupMenu implementation optionally allowing to interact with JMenuItems
 * without immediately closing the menu.
 *
 * Optionally supports to set a custom background color (some LaF implementations
 * may not support it) to visually present the difference from a standard popup.
 *
 * @author Jiri Sedlacek
 */
public class StayOpenPopupMenu extends ProfilerPopupMenu {


    public StayOpenPopupMenu() {
        super();
    }

    public StayOpenPopupMenu(String label) {
        super(label);
    }


    // --- Handling keyboard events --------------------------------------------

    private static boolean isReturnAction(KeyEvent e) {
        int keyCode = e.getKeyCode();
        return keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE;
    }

    private static KeyStroke mnemonic(JMenuItem item) {
        return KeyStroke.getKeyStroke(item.getMnemonic(), KeyEvent.ALT_DOWN_MASK);
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path, MenuSelectionManager manager) {
        if (isReturnAction(e)) { // Handle SPACE and ENTER
            MenuElement[] p = manager.getSelectedPath();
            MenuElement m = p != null && p.length > 0 ? p[p.length - 1] : null;
            if (m instanceof StayOpen) {
                e.consume();
                if (e.getID() == KeyEvent.KEY_PRESSED)
                    performAction((StayOpen)m, e.getModifiers());
                return;
            }
        } else for (Component component : getComponents()) { // Handle mnemonics and accelerators
            if (component instanceof StayOpen) {
                StayOpen item = (StayOpen)component;
                JMenuItem i = item.getItem();
                KeyStroke k = KeyStroke.getKeyStrokeForEvent(e);
                if (k.equals(mnemonic(i)) || k.equals(i.getAccelerator())) {
                    e.consume();
                    manager.setSelectedPath(new MenuElement[] { this, i });
                    performAction(item, e.getModifiers());
                    return;
                }
            }
        }
        
        super.processKeyEvent(e, path, manager);
    }
    
    
    // --- Actions implementation ----------------------------------------------
    
    private static void performAction(StayOpen item, int modifiers) {
        JMenuItem i = item.getItem();
        
        // Skip disabled items
        if (!item.getItem().isEnabled()) return;
        
        // Handle toggle items
        if (i.getModel() instanceof JToggleButton.ToggleButtonModel)
            i.setSelected(!i.isSelected());
        
        // Invoke item action
        item.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED,
                             item.getItem().getActionCommand(),
                             EventQueue.getMostRecentEventTime(), modifiers));
    }
    
    private static void performAction(StayOpen item, MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            if (item.getItem().contains(e.getPoint()))
                performAction(item, e.getModifiers());
            e.consume();
        }
    }
    
    
    // --- StayOpen items ------------------------------------------------------
    
    private static interface StayOpen extends ActionListener {
        
        JMenuItem getItem();
        
    }
    
    /**
     * JMenuItem implementation not closing the owner StayOpenPopupMenu when invoked.
     */
    public static class Item extends JMenuItem implements StayOpen {
        
        public Item() { super(); }

        public Item(Icon icon) { super(icon); }

        public Item(String text) { super(text); }

        public Item(Action a) { super(a); }

        public Item(String text, Icon icon) { super(text, icon); }

        public Item(String text, int mnemonic) { super(text, mnemonic); }
        
        
        public JMenuItem getItem() {
            return this;
        }
        
        public void actionPerformed(ActionEvent event) {
            fireActionPerformed(event);
        }
        
        
        protected void processMouseEvent(MouseEvent e) {
            performAction(this, e);
            if (!e.isConsumed()) super.processMouseEvent(e);
        }
        
    }
    
    /**
     * JCheckBoxMenuItem implementation not closing the owner StayOpenPopupMenu when invoked.
     */
    public static class CheckBoxItem extends JCheckBoxMenuItem implements StayOpen {
        
        public CheckBoxItem() { super(); }

        public CheckBoxItem(Icon icon) { super(icon); }

        public CheckBoxItem(String text) { super(text); }

        public CheckBoxItem(Action a) { super(a); }

        public CheckBoxItem(String text, Icon icon) { super(text, icon); }

        public CheckBoxItem(String text, boolean b) { super(text, b); }

        public CheckBoxItem(String text, Icon icon, boolean b) { super(text, icon, b); }
        
        
        public JMenuItem getItem() {
            return this;
        }
        
        public void actionPerformed(ActionEvent event) {
            super.fireActionPerformed(event);
        }
        
        
        protected void processMouseEvent(MouseEvent e) {
            performAction(this, e);
            if (!e.isConsumed()) super.processMouseEvent(e);
        }
        
    }
    
    /**
     * JRadioButtonMenuItem implementation not closing the owner StayOpenPopupMenu when invoked.
     */
    public static class RadioButtonItem extends JRadioButtonMenuItem implements StayOpen {
        
        public RadioButtonItem() { super(); }

        public RadioButtonItem(Icon icon) { super(icon); }

        public RadioButtonItem(String text) { super(text); }

        public RadioButtonItem(Action a) { super(a); }

        public RadioButtonItem(String text, Icon icon) { super(text, icon); }

        public RadioButtonItem(String text, boolean b) { super(text, b); }

        public RadioButtonItem(String text, Icon icon, boolean b) { super(text, icon, b); }
        
        
        public JMenuItem getItem() {
            return this;
        }
        
        public void actionPerformed(ActionEvent event) {
            super.fireActionPerformed(event);
        }
        
        
        protected void processMouseEvent(MouseEvent e) {
            performAction(this, e);
            if (!e.isConsumed()) super.processMouseEvent(e);
        }
        
    }
    
}

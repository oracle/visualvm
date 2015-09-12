/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.ui;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 * A JPopupMenu implementation optionally allowing to interact with JMenuItems
 * without immediately closing the menu.
 * 
 * Optionally supports to set a custom background color (some LaF implementations
 * may not support it) to visually present the difference from a standard popup.
 *
 * @author Jiri Sedlacek
 */
public class StayOpenPopupMenu extends JPopupMenu {
    
    private boolean forceBackground;
    
    
    public StayOpenPopupMenu() {
        super();
    }

    public StayOpenPopupMenu(String label) {
        super(label);
    }
    
    
    // --- Tweaking UI ---------------------------------------------------------
    
    public JMenuItem add(JMenuItem menuItem) {
        if (forceBackground && !UIUtils.isOracleLookAndFeel()) menuItem.setOpaque(false);
        return super.add(menuItem);
    }
    
    public void add(Component comp, Object constraints) {
        if (forceBackground && !UIUtils.isOracleLookAndFeel() && comp instanceof JComponent)
            ((JComponent)comp).setOpaque(false);
        comp.setMinimumSize(comp.getPreferredSize());
        super.add(comp, constraints);
    }
    
    
    public void setForceBackground(boolean force) {
        if (!UIUtils.isNimbus() || !Boolean.TRUE.equals(UIManager.getBoolean("nb.dark.theme"))) // NOI18N
            this.forceBackground = force;
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (forceBackground) {
            g.setColor(getBackground());
            g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
        }
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

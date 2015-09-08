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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.GenericToolbar;
import org.netbeans.lib.profiler.ui.swing.SmallButton;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public class DropdownButton extends JPanel {
    
    private static final Icon DROPDOWN_ICON = Icons.getIcon(GeneralIcons.POPUP_ARROW);
    private static final int DROPDOWN_ICON_WIDTH = DROPDOWN_ICON.getIconWidth();
    private static final int DROPDOWN_ICON_HEIGHT = DROPDOWN_ICON.getIconHeight();
    
    private static final String NO_ACTION = "none"; // NOI18N
    private static final String POPUP_ACTION = "displayPopup"; // NOI18N
    
    private static final int POPUP_EXTENT;
    private static final int POPUP_OFFSET;
    private static final int POPUP_XWIDTH;
    private static final int POPUP_MARGIN;
    
    static {
        if (UIUtils.isWindowsLookAndFeel()) {
            POPUP_EXTENT = 15;
            POPUP_OFFSET = 4;
            POPUP_XWIDTH = -1;
            POPUP_MARGIN = 6;
        } else if (UIUtils.isNimbus()) {
            POPUP_EXTENT = 17;
            POPUP_OFFSET = 6;
            POPUP_XWIDTH = -1;
            POPUP_MARGIN = 6;
        } else if (UIUtils.isMetalLookAndFeel()) {
            POPUP_EXTENT = 16;
            POPUP_OFFSET = 5;
            POPUP_XWIDTH = -2;
            POPUP_MARGIN = 6;
        } else if (UIUtils.isAquaLookAndFeel()) {
            POPUP_EXTENT = 19;
            POPUP_OFFSET = 7;
            POPUP_XWIDTH = -8;
            POPUP_MARGIN = 6;
        } else {
            POPUP_EXTENT = 16;
            POPUP_OFFSET = 5;
            POPUP_XWIDTH = -2;
            POPUP_MARGIN = 6;
        }
    }
    
    
    private final JComponent container;
    private final Button button;
    private final Popup popup;
    
    private boolean pushed;
    
    
    public DropdownButton(String text, Icon icon, boolean toolbar) {
        setOpaque(false);
        
        if (toolbar) {
            JToolBar tb = new GenericToolbar() {
                public void doLayout() {
                    for (Component c : getComponents())
                        c.setBounds(0, 0, getWidth(), getHeight());
                }
                public void paint(Graphics g) {
                    paintChildren(g);
                }
            };
            tb.setFloatable(false);
            tb.setFocusable(false);
            container = tb;
            add(container);
        } else {
            container = this;
        }
        
        button = new Button(text, icon);
        container.add(button);
        
        popup = new Popup();
        container.add(popup);
        
        KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        container.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(down, POPUP_ACTION);
        container.getActionMap().put(POPUP_ACTION, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { displayPopup(); }
        });
    }
    
    
    public void setEnabled(boolean enabled) {
        if (button != null) {
            button.setEnabled(enabled);
            if (enabled) exposeButton();
            else exposePopup();
        }
    }
    
    public boolean isEnabled() {
        return button == null ? false : button.isEnabled();
    }
    
    public void setPopupEnabled(boolean enabled) {
        if (popup != null) popup.setEnabled(enabled);
    }
    
    public boolean isPopupEnabled() {
        return popup == null ? false : popup.isEnabled();
    }
    
    public void setPushed(boolean p) {
        pushed = p;
        repaint();
    }
    
    public boolean isPushed() {
        return pushed;
    }
    
    
    public void setToolTipText(String text) {
        button.setToolTipText(text);
    }
    
    public void setPushedToolTipText(String text) {
        button.putClientProperty("PUSHED_TOOLTIP", text); // NOI18N
    }
    
    public void setPopupToolTipText(String text) {
        popup.setToolTipText(text);
    }
    
    
    public void setText(String text) {
        if (button != null) {
            String _text = button.getText();
            button.setText(text);
            
            Component parent = getParent();
            if (!Objects.equals(text, _text) && parent != null) {
                parent.invalidate();
                parent.revalidate();
                parent.repaint();
            }
        }
    }
    
    public String getText() {
        return button == null ? null : button.getText();
    }
    
    public void setIcon(Icon icon) {
        if (button != null) {
            Icon _icon = button.getIcon();
            button.setIcon(icon);
            
            Component parent = getParent();
            if (!Objects.equals(icon, _icon) && parent != null) {
                parent.invalidate();
                parent.revalidate();
                parent.repaint();
            }
        }
    }
    
    public Icon getIcon() {
        return button == null ? null : button.getIcon();
    }
    
    
    public void clickPopup() {
        if (popup != null) popup.doClick();
    }
    
    public void displayPopup() {
        JPopupMenu menu = new JPopupMenu();
        populatePopup(menu);
        if (menu.getComponentCount() > 0) {
            Dimension size = menu.getPreferredSize();
            size.width = Math.max(size.width, getWidth());
            menu.setPreferredSize(size);
            menu.show(this, 0, getHeight());
        }
    }
    
    protected void populatePopup(JPopupMenu menu) {
        // Implementation here
    }
    
    protected void performAction() {
        // Implementation here
    }
    
    
    public void paint(Graphics g) {
        paintChildren(g);
    }
    
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        
        DROPDOWN_ICON.paintIcon(this, g, getWidth() - DROPDOWN_ICON_WIDTH - POPUP_OFFSET,
                                        (getHeight() - DROPDOWN_ICON_HEIGHT) / 2);
        
        if (pushed || !button.isEnabled() || container.getComponent(0) == popup || button.getModel().isRollover() || button.isFocusOwner()) {
            g.setColor(Color.GRAY);
            g.drawLine(getWidth() - POPUP_EXTENT, POPUP_MARGIN,
                       getWidth() - POPUP_EXTENT, getHeight() - POPUP_MARGIN);
        }
    }
    
    private void processChildMouseEvent(MouseEvent e) {
        if (e.getX() >= getWidth() - POPUP_EXTENT && contains(e.getX(), e.getY())) {
            if (exposePopup()) {
                button.processEventImpl(fromEvent((MouseEvent)e, button, MouseEvent.MOUSE_EXITED));
                popup.processEventImpl(fromEvent((MouseEvent)e, popup, MouseEvent.MOUSE_ENTERED));
            } else {
                popup.processEventImpl(e);
            }
        } else {
            if (exposeButton()) {
                popup.processEventImpl(fromEvent((MouseEvent)e, popup, MouseEvent.MOUSE_EXITED));
                if (contains(e.getX(), e.getY())) button.processEventImpl(fromEvent((MouseEvent)e, button, MouseEvent.MOUSE_ENTERED));
            } else {
                button.processEventImpl(e);
            }
        }
    }
    
    private static MouseEvent fromEvent(MouseEvent e, Component source, int id) {
        return new MouseEvent(source, id, e.getWhen(), e.getModifiers(), e.getX(),
                              e.getY(), e.getClickCount(), e.isPopupTrigger());
    }
    
    private boolean exposeButton() {
        if (container.getComponent(0) == button) return false;
        Component c = button.isEnabled() ? button : popup;
        boolean focus = c.isFocusOwner();
        if (focus) {
            setFocusable(true);
            requestFocusInWindow();
        }
        container.add(popup);
        if (focus) {
            c.requestFocusInWindow();
            setFocusable(false);
        }
        repaint();
        return true;
    }
    
    private boolean exposePopup() {
        if (container.getComponent(0) == popup) return false;
        Component c = button.isEnabled() ? button : popup;
        boolean focus = c.isFocusOwner();
        if (focus) {
            setFocusable(true);
            requestFocusInWindow();
        }
        container.add(button);
        if (focus) {
            c.requestFocusInWindow();
            setFocusable(false);
        }
        repaint();
        return true;
    }
    
    
    public Dimension getPreferredSize() {
        Dimension d = button.getPreferredSize();
        d.width += POPUP_EXTENT + POPUP_XWIDTH;
        return d;
    }
    
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    public void doLayout() {
        for (Component c : getComponents())
            c.setBounds(0, 0, getWidth(), getHeight());
    }
    
    
    private class Button extends SmallButton {
        
        Button(String text, Icon icon) {
            super(text, icon);
            
            // See GenericToolbar.addImpl()
            putClientProperty("MetalListener", new Object()); // NOI18N
            
            if (UIUtils.isAquaLookAndFeel())
                putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N
            
            setModel(new DefaultButtonModel() {
                public boolean isRollover() {
                    return super.isRollover() || (isEnabled() && (popup != null && popup.getModel().isRollover()));
                }
                public boolean isPressed() {
                    return pushed || super.isPressed();
                }
                public boolean isArmed() {
                    return pushed || super.isArmed();
                }
            });
            
            setHorizontalAlignment(LEADING);
            setDefaultCapable(false);
        }
        
        public String getToolTipText() {
            if (pushed) {
                Object pushedTT = getClientProperty("PUSHED_TOOLTIP"); // NOI18N
                if (pushedTT != null) return pushedTT.toString();
            }
            return super.getToolTipText();
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            super.fireActionPerformed(e);
            performAction();
        }
        
        protected void processEvent(AWTEvent e) {
            if (!(e instanceof MouseEvent)) processEventImpl(e);
            else processChildMouseEvent((MouseEvent)e);
        }
        
        private void processEventImpl(AWTEvent e) {
            super.processEvent(e);
        }
        
        public boolean hasFocus() {
            return isEnabled() ? super.hasFocus() : popup.hasFocus();
        }
        
        public void paint(Graphics g) {
            Rectangle c = g.getClipBounds();
            if (pushed || !isEnabled() || container.getComponent(0) != this)
                g.setClip(0, 0, getWidth() - POPUP_EXTENT, getHeight());
            super.paint(g);
            g.setClip(c);
        }
        
        public void repaint() {
            DropdownButton.this.repaint();
        }
        
        public Insets getMargin() {
            Insets i = super.getMargin();
            if (UIUtils.isNimbusLookAndFeel()) {
                if (i == null) {
                    i = new Insets(0, 2, 0, 2);
                } else {
                    i.left = 2;
                    i.right = 2;
                }
            } else if (UIUtils.isAquaLookAndFeel()) {
                if (i == null) {
                    i = new Insets(0, -6, 0, 0);
                } else {
                    i.left = -6;
                    i.right = 0;
                }
            }
            return i;
        }
        
    }
    
    private class Popup extends JButton {
        
        Popup() {
            super(" "); // NOI18N
            
            // See GenericToolbar.addImpl()
            putClientProperty("MetalListener", new Object()); // NOI18N
            
            setModel(new DefaultButtonModel() {
                public boolean isRollover() {
                    return super.isRollover() || pushed;
                }
            });
            
            setHorizontalAlignment(LEADING);
            setDefaultCapable(false);
            
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), NO_ACTION);
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), NO_ACTION);
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            super.fireActionPerformed(e);
            displayPopup();
        }
        
        protected void processEvent(AWTEvent e) {
            if (!(e instanceof MouseEvent)) processEventImpl(e);
            else processChildMouseEvent((MouseEvent)e);
        }
        
        private void processEventImpl(AWTEvent e) {
            super.processEvent(e);
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                if (isFocusable()) requestFocus();
                else button.requestFocus();
            }
        }
        
        public boolean hasFocus() {
            return isFocusable() ? super.hasFocus() : button.hasFocus();
            
        }
        
        public boolean isFocusable() {
            return !button.isEnabled();
        }
        
        public void paint(Graphics g) {
            if (pushed || !button.isEnabled() || container.getComponent(0) == this) {
                Rectangle c = g.getClipBounds();
                g.setClip(getWidth() - POPUP_EXTENT, 0, POPUP_EXTENT, getHeight());
                super.paint(g);
                g.setClip(c);
            }
        }
        
        public void repaint() {
            DropdownButton.this.repaint();
        }
        
    }
    
}

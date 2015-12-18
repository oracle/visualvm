/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerPopupFactory {
    
    public static Popup getPopup(Component invoker, Component content, int x, int y) {
        PopupPane pane = new PopupPane(content);
        
        Point loc = new Point(x, y);
        SwingUtilities.convertPointToScreen(loc, invoker);
        
        Popup popup = PopupFactory.getSharedInstance().getPopup(invoker, pane, loc.x, loc.y);
        pane.setPopup(popup);
        
        return popup;
    }
    
     
    private static class PopupPane extends JPanel implements AWTEventListener,
                                           ComponentListener, WindowListener {
        
        private Popup popup;
        private Reference<Component> focus;
        
        
        PopupPane(Component content) {
            setFocusCycleRoot(true);
            setFocusTraversalPolicyProvider(true);
            setFocusTraversalPolicy(new PopupFocusTraversalPolicy());
            
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            
            JPopupMenu ref = new JPopupMenu();
            if (!UIUtils.isNimbus()) setBorder(ref.getBorder());
            else setBorder(BorderFactory.createLineBorder(UIUtils.getDisabledLineColor()));
            setBackground(ref.getBackground());
        }
        
        public boolean isDisplayable() {
        return true;
    }
        void setPopup(Popup popup) {
            this.popup = popup;
        }
        
        
        public void addNotify() {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != null) focus = new WeakReference(focusOwner);
            
            super.addNotify();
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                    System.err.println(">>> POLICY: " + ((JComponent)getComponent(0)).getFocusTraversalPolicy());
                    getFocusTraversalPolicy().getDefaultComponent(PopupPane.this)
                                             .requestFocusInWindow();
                }
            });
            
            installListeners();
        }

        public void removeNotify() {
            uninstallListeners();
            
            if (focus != null) {
                Component focusOwner = focus.get();
                focus.clear();
                focus = null;
                if (focusOwner != null) focusOwner.requestFocusInWindow();
            }
            
            super.removeNotify();
        }
        
        private void installListeners() {
            Window window = SwingUtilities.getWindowAncestor(this);
            
            if (window != null) {
                window.addWindowListener(this);
                window.addComponentListener(this);
            }
            
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK +
                                                            AWTEvent.MOUSE_WHEEL_EVENT_MASK +
                                                            AWTEvent.KEY_EVENT_MASK);
        }
        
        private void uninstallListeners() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
            
            Window window = SwingUtilities.getWindowAncestor(this);
            
            if (window != null) {
                window.removeComponentListener(this);
                window.removeWindowListener(this);
            }
            
            popup = null;
        }
        
        private void closePopup() {
            if (popup != null) popup.hide();
        }
        
        
        private boolean internal = false;
        
        public void eventDispatched(AWTEvent e) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent)e;
                if (internal || me.isConsumed()) return;

                Component src = me.getComponent();
                if (src == null) return;
                src = SwingUtilities.getDeepestComponentAt(src, me.getX(), me.getY());
                if (src == null) return;

                switch (me.getID()) {
                    case MouseEvent.MOUSE_PRESSED:
    //                    if (!isInPopup(src)) closePopup();
                    case MouseEvent.MOUSE_WHEEL:
                        if (!isInPopup(src)) {
                            closePopup();
                        } else {
                            internal = true;
                            try { src.dispatchEvent(me); }
                            finally { internal = false; }
                        }
                        me.consume();
                        break;
                }
            } else if (e instanceof KeyEvent) {
                KeyEvent me = (KeyEvent)e;
                if (internal || me.isConsumed()) return;
                
                if (me.getKeyCode() != KeyEvent.VK_ESCAPE ||
                    me.getID() != KeyEvent.KEY_PRESSED) return;
                
                Component src = me.getComponent();
                    
                internal = true;
                try { src.dispatchEvent(me); }
                finally { internal = false; }

                if (!me.isConsumed()) closePopup();
                me.consume();
            }
        }
        
        private boolean isInPopup(Component comp) {
            while (comp != null) {
                if (comp == this || comp instanceof JPopupMenu) return true;
                comp = comp.getParent();
            }
            return false;
        }
        

        public void componentResized(ComponentEvent e) { /*closePopup();*/ }

        public void componentMoved(ComponentEvent e)   { /*closePopup();*/ }

        public void componentShown(ComponentEvent e)   { closePopup(); }

        public void componentHidden(ComponentEvent e)  { closePopup(); }

        public void windowClosing(WindowEvent e)       { closePopup(); }

        public void windowClosed(WindowEvent e)        { closePopup(); }

        public void windowIconified(WindowEvent e)     { closePopup(); }
        
        public void windowDeactivated(WindowEvent e)   { closePopup(); }
        
        public void windowOpened(WindowEvent e)        {}

        public void windowDeiconified(WindowEvent e)   {}

        public void windowActivated(WindowEvent e)     {}
        
    }
    
    
    private static class PopupFocusTraversalPolicy extends FocusTraversalPolicy {
        
        public Component getComponentAfter(Container aContainer, Component aComponent) {
            List<Component> l = components(aContainer);
            int i = l.indexOf(aComponent);
            return i == -1 || i == l.size() - 1 ? null : l.get(i + 1);
        }

        public Component getComponentBefore(Container aContainer, Component aComponent) {
            List<Component> l = components(aContainer);
            int i = l.indexOf(aComponent);
            return i == -1 || i == 0 ? null : l.get(i - 1);
        }

        public Component getFirstComponent(Container aContainer) {
            List<Component> l = components(aContainer);
            return l.isEmpty() ? null : l.get(0);
        }

        public Component getLastComponent(Container aContainer) {
            List<Component> l = components(aContainer);
            return l.isEmpty() ? null : l.get(l.size() - 1);
        }

        public Component getDefaultComponent(Container aContainer) {
            Component c = getFirstComponent(aContainer);
            
            if (c instanceof AbstractButton) {
                ButtonModel bm = ((AbstractButton)c).getModel();
                if (bm instanceof DefaultButtonModel) {
                    ButtonGroup bg = ((DefaultButtonModel)bm).getGroup();
                    Enumeration<AbstractButton> en = bg == null ? null : bg.getElements();
                    while (en != null && en.hasMoreElements()) {
                        AbstractButton ab = en.nextElement();
                        if (ab.isSelected()) return ab;
                    }
                }
            }
            
            return c;
        }

        protected List<Component> components(Container aContainer) {
            List<Component> l = new ArrayList();

            for (int i = 0; i < aContainer.getComponentCount(); i++) {
                Component c = aContainer.getComponent(i);
                if (c instanceof JPanel || c instanceof JToolBar)
                    l.addAll(components((Container)c));
                else if (c instanceof JScrollPane)
                    l.addAll(components((Container)((JScrollPane)c).getViewport()));
                else if (focusable(c)) l.add(c);
            }

            return l;
        }
        
        protected boolean focusable(Component c) {
            if (c instanceof JLabel) return false;
            return c.isVisible() && c.isEnabled() && c.isFocusable();
        }
        
    }
    
}

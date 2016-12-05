/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerPopup {
    
    private static final boolean DEBUG = Boolean.getBoolean("ProfilerPopup.DebugWindows"); // NOI18N
    private static final int IGNORE_OWNER_TIMEOUT = Integer.getInteger("ProfilerPopup.OwnerTimeout", 40); // NOI18N
    
//    private Reference<Component> focusRef;
    private final Reference<Window> ownerRef;
    
    private final PopupPane content;
    private final Point location;
    
    private JWindow window;
    private Window owner;
    
    private final Listener listener;
    
    
    public static ProfilerPopup create(Component invoker, Component content, int x, int y) {
        return create(invoker, content, x, y, null);
    }
    
    public static ProfilerPopup create(Component invoker, Component content, int x, int y, Listener listener) {
        Point location = new Point(x, y);
        Window owner = null;
        
        if (invoker != null) {
            SwingUtilities.convertPointToScreen(location, invoker);
            owner = SwingUtilities.getWindowAncestor(invoker);
        }
        
        return new ProfilerPopup(content, location, owner, listener);
    }
    
    
    public void show() {
//        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
//        if (focusOwner != null) focusRef = new WeakReference(focusOwner);
            
        owner = ownerRef == null ? null : ownerRef.get();
        
        window = new JWindow(owner);
        window.setType(Window.Type.POPUP);
        window.setAlwaysOnTop(false);
//        window.setFocusable(true);
//        window.setFocusableWindowState(true);
//        window.setAutoRequestFocus(true);
        
        window.getContentPane().add(content);
        window.pack();
        window.setLocation(location);
        
        window.setVisible(true);
        
        Component defaultFocus = content.getFocusTraversalPolicy().getDefaultComponent(content);
        if (defaultFocus != null) defaultFocus.requestFocusInWindow();
        
        content.installListeners();
        
        if (listener != null) listener.popupShown();
    }
    
    public void hide() {
        content.uninstallListeners();
        owner = null;

        window.setVisible(false);
        window.dispose();
        window = null;
        
//        if (focusRef != null) {
//            Component focusOwner = focusRef.get();
//            focusRef.clear();
//            focusRef = null;
//            if (focusOwner != null) focusOwner.requestFocusInWindow();
//        }
        
        if (listener != null) listener.popupHidden();
    }
    
    
    public static boolean isInPopup(Component component) {
        Container parent = component.getParent();
        while (parent != null)
            if (parent instanceof PopupPane) return true;
            else parent = parent.getParent();
        return false;
    }
    
    
    public static abstract class Listener {
        
        protected void popupShown() {}
        
        protected void popupHidden() {}
        
    }
    
    
    private ProfilerPopup(Component component, Point location, Window owner, Listener listener) {
        this.content = new PopupPane(component);
        this.location = location;
        this.ownerRef = owner == null ? null : new WeakReference(owner);
        this.listener = listener;
    }
    
    
    private class PopupPane extends JPanel implements WindowFocusListener, ComponentListener, KeyEventDispatcher {
        
        private boolean skippingEvents;
        private long gainedFocusTime;
        
        
        PopupPane(Component content) {
            super(new BorderLayout());
            add(content, BorderLayout.CENTER);
            
            setFocusCycleRoot(true);
            setFocusTraversalPolicyProvider(true);
            setFocusTraversalPolicy(new PopupFocusTraversalPolicy());
            
            JPopupMenu ref = new JPopupMenu();
            if (!UIUtils.isNimbus()) setBorder(ref.getBorder());
            else setBorder(BorderFactory.createLineBorder(UIUtils.getDisabledLineColor()));
            setBackground(ref.getBackground());
        }
        
        
        void installListeners() {
            window.addWindowFocusListener(this);
            
            if (owner != null) owner.addComponentListener(this);

            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        }
        
        void uninstallListeners() {
            
            window.removeWindowFocusListener(this);
            
            if (owner != null) owner.removeComponentListener(this);
            
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
        }
        
        
        // --- WindowFocusListener ---------------------------------------------
        
        public void windowGainedFocus(WindowEvent e) {
            skippingEvents = false;
            gainedFocusTime = System.currentTimeMillis();
            if (DEBUG) System.err.println(">>> Focus to popup gained"); // NOI18N
        }        

        public void windowLostFocus(WindowEvent e) {
            if (skippingEvents) return;
            
            if (e.getOppositeWindow() instanceof Dialog) {
                final Dialog d = (Dialog)e.getOppositeWindow();
                if (d != owner && d.isModal()) {
                    // Do not close popup when a modal dialog is opened,
                    // except of the owner
                    skippingEvents = true;
                    if (DEBUG) System.err.println(">>> LOST TO DIALOG " + getString(d) + " owned by " + getString(d.getOwner())); // NOI18N
                    return;
                }
            } else if (e.getOppositeWindow() == owner) {
                // NOTE: workaround for bug on Linux,
                //       closing the dialog opened from ProfilerPopup
                //       sometimes also closes the ProfilerPopup,
                //       passing the focus back to NB main window
                long lostFocusTime = System.currentTimeMillis();
                if (DEBUG) System.err.println(">>> ### HIDDEN BY OWNER, gained focus before " + (lostFocusTime - gainedFocusTime)); // NOI18N
                if (lostFocusTime - gainedFocusTime < IGNORE_OWNER_TIMEOUT) {
                    gainedFocusTime = 0;
                    final Window win = window;
                    final Component comp = window.getMostRecentFocusOwner();
                    if (DEBUG) System.err.println(">>>   Requesting focus again to " + getString(comp)); // NOI18N
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            win.requestFocus();
                            if (comp != null) comp.requestFocus();
                            if (DEBUG) System.err.println(">>>       REQUESTED lazily"); // NOI18N
                        }
                    });
                    return;
                }
            }
            
            ProfilerPopup.this.hide();
            if (DEBUG) System.err.println(">>> Focus to popup lost to " + getString(e.getOppositeWindow()));
        }
        
        private String getString(Component c) {
            if (c instanceof Dialog) return "[dialog] " + ((Dialog)c).getTitle(); // NOI18N
            else if (c instanceof Frame) return "[frame] " + ((Frame)c).getTitle(); // NOI18N
            else return c.getClass().getName();
        }
        
        
        // --- ComponentListener -----------------------------------------------
        
        public void componentResized(ComponentEvent e) { if (DEBUG) System.err.println(">>> Closed by componentResized"); ProfilerPopup.this.hide(); }

        public void componentMoved(ComponentEvent e)   { if (DEBUG) System.err.println(">>> Closed by componentMoved"); ProfilerPopup.this.hide(); }

        public void componentShown(ComponentEvent e)   { }

        public void componentHidden(ComponentEvent e)  { if (DEBUG) System.err.println(">>> Closed by componentHidden"); ProfilerPopup.this.hide(); }
        
        
        // --- KeyEventDispatcher ----------------------------------------------
        
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (skippingEvents || e.isConsumed()) return false;
            
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE)
                if (SwingUtilities.getRootPane(this) != e.getSource()) { // Closing JPopupMenu using the ESC key
                    e.consume();
                    if (DEBUG) System.err.println(">>> Closed by ESC"); // NOI18N
                    ProfilerPopup.this.hide();
                    return true;
                }
            
            return false;
        }        
        
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

        private static List<Component> components(Container aContainer) {
            List<Component> l = new ArrayList();

            for (int i = 0; i < aContainer.getComponentCount(); i++) {
                Component c = aContainer.getComponent(i);
                if (c instanceof JPanel || c instanceof JToolBar)
                    l.addAll(components((Container)c));
                else if (c instanceof JScrollPane)
                    l.addAll(components((Container)((JScrollPane)c).getViewport()));
//                else if (c instanceof JRootPane)
//                    l.addAll(components((Container)((JRootPane)c).getContentPane()));
                else if (focusable(c)) l.add(c);
            }

            return l;
        }
        
        private static boolean focusable(Component c) {
            if (c instanceof JLabel || c instanceof Box.Filler) return false;
            return c.isVisible() && c.isEnabled() && c.isFocusable();
        }
        
    }
    
}

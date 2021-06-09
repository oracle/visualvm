/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
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
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerPopup {
    
    private static final boolean DEBUG = Boolean.getBoolean("ProfilerPopup.DebugWindows"); // NOI18N
    
    private static final int IGNORE_OWNER_TIMEOUT = Integer.getInteger("ProfilerPopup.OwnerTimeout", 40); // NOI18N
    private static final int RESIZE_STRIPE = Integer.getInteger("ProfilerPopup.ResizeStripe", 10); // NOI18N
    
    public static final int RESIZE_NONE    = 0;
    public static final int RESIZE_TOP     = 1;
    public static final int RESIZE_LEFT    = 2;
    public static final int RESIZE_BOTTOM  = 4;
    public static final int RESIZE_RIGHT   = 8;
    
//    private Reference<Component> focusRef;
    private final Reference<Window> ownerRef;
    
    private final PopupPane content;
    private final Rectangle location;
    private final int popupAlign;
    
    private JWindow window;
    private Window owner;
    
    private Point ownerLocation;
    
    private final Listener listener;
    
    private final int resizeMode;
    
    
    public static ProfilerPopup create(Component invoker, Component content, int x, int y) {
        return create(invoker, content, x, y, RESIZE_NONE);
    }
    
    public static ProfilerPopup createRelative(Component invoker, Component content, int popupAlign) {
        if (invoker == null) throw new IllegalArgumentException("Invoker cannot be null for relative popups"); // NOI18N
        return createRelative(invoker, content, popupAlign, RESIZE_NONE);
    }
    
    public static ProfilerPopup create(Component invoker, Component content, int x, int y, int resizeMode) {
        return create(invoker, content, x, y, resizeMode, null);
    }
    
    public static ProfilerPopup createRelative(Component invoker, Component content, int popupAlign, int resizeMode) {
        if (invoker == null) throw new IllegalArgumentException("Invoker cannot be null for relative popups"); // NOI18N
        return createRelative(invoker, content, popupAlign, resizeMode, null);
    }
    
    public static ProfilerPopup create(Component invoker, Component content, int x, int y, int resizeMode, Listener listener) {
        return create(invoker, content, x, y, -1, resizeMode, listener);
    }
    
    public static ProfilerPopup createRelative(Component invoker, Component content, int popupAlign, int resizeMode, Listener listener) {
        if (invoker == null) throw new IllegalArgumentException("Invoker cannot be null for relative popups"); // NOI18N
        return create(invoker, content, -1, -1, popupAlign, resizeMode, listener);
    }
    
    private static ProfilerPopup create(Component invoker, Component content, int x, int y, int popupAlign, int resizeMode, Listener listener) {
        Point location = new Point(x, y);
        Dimension size = new Dimension();
        Window owner = null;
        
        if (invoker != null) {
            SwingUtilities.convertPointToScreen(location, invoker);
            size.setSize(invoker.getSize());
            owner = SwingUtilities.getWindowAncestor(invoker);
        }
        
        return new ProfilerPopup(content, new Rectangle(location, size), popupAlign, owner, resizeMode, listener);
    }
    
    
    public void show() {
//        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
//        if (focusOwner != null) focusRef = new WeakReference(focusOwner);
            
        owner = ownerRef == null ? null : ownerRef.get();
        ownerLocation = owner == null ? null : owner.getLocationOnScreen();
        
        window = new JWindow(owner);
        window.setType(Window.Type.POPUP);
        window.setAlwaysOnTop(false);
        window.setFocusable(true);
        window.setFocusableWindowState(true);
        window.setAutoRequestFocus(true);
        
        window.getContentPane().add(content);
        window.pack();
        
        if (popupAlign == -1) {
            window.setLocation(location.getLocation());
        } else {
            Dimension size = content.getSize();
            
            int x;
            switch (popupAlign) {
                case SwingConstants.EAST:
                case SwingConstants.NORTH_EAST:
                case SwingConstants.SOUTH_EAST:
                    x = location.x + location.width - size.width + 1;
                    break;
                default:
                    x = location.x + 1;
                    break;
            }
            
            int y;
            switch (popupAlign) {
                case SwingConstants.NORTH:
                case SwingConstants.NORTH_EAST:
                case SwingConstants.NORTH_WEST:
                    y = location.y - size.height + 1;
                    break;
                default:
                    y = location.y + location.height + 1;
                    break;
            }
            
            window.setLocation(x, y);
        }
        
        window.setVisible(true);
        
        Component defaultFocus = content.getFocusTraversalPolicy().getDefaultComponent(content);
        if (defaultFocus != null) defaultFocus.requestFocusInWindow();
        
        content.installListeners();
        
        if (listener != null) listener.popupShown();
    }
    
    public void hide() {
        content.uninstallListeners();
        
        owner = null;
        ownerLocation = null;

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
    
    
    private ProfilerPopup(Component component, Rectangle location, int popupAlign, Window owner, int resizeMode, Listener listener) {
        this.content = new PopupPane(component, resizeMode != RESIZE_NONE);
        this.location = location;
        this.popupAlign = popupAlign;
        this.ownerRef = owner == null ? null : new WeakReference(owner);
        this.resizeMode = resizeMode;
        this.listener = listener;
    }
    
    
    public static abstract class Listener {
        
        protected void popupShown() {}
        
        protected void popupHidden() {}
        
    }
    
    
    private class PopupPane extends JPanel implements WindowFocusListener, ComponentListener, KeyEventDispatcher,
                                                      MouseListener, MouseMotionListener {
        
        private boolean skippingEvents;
        private long gainedFocusTime;
        
        
        PopupPane(Component content, boolean resize) {
            super(new BorderLayout());
            add(content, BorderLayout.CENTER);
            
            setFocusCycleRoot(true);
            setFocusTraversalPolicyProvider(true);
            setFocusTraversalPolicy(new PopupFocusTraversalPolicy());
            
            if (UIUtils.isAquaLookAndFeel()) {
                if (resize) {
                    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                    addMouseListener(this);
                    addMouseMotionListener(this);
                }
            } else {
                Border border = BorderFactory.createLineBorder(UIUtils.getDisabledLineColor());
                
                if (resize) {
                    setBorder(BorderFactory.createCompoundBorder(border,
                              BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                    addMouseListener(this);
                    addMouseMotionListener(this);
                } else {
                    setBorder(border);
                }
            }
        }
        
        
        // --- Resizing --------------------------------------------------------
        
        private boolean dragging;
        private int currentResizing;
        private int dragX, dragY = -1;
        
        public void mouseClicked(MouseEvent e) { }
        
        public void mousePressed(MouseEvent e) {
            dragging = true;
            dragX = e.getXOnScreen();
            dragY = e.getYOnScreen();
        }

        public void mouseReleased(MouseEvent e) {
            dragging = false;
            dragX = -1;
            dragY = -1;
            updateResizing(e);
        }

        public void mouseEntered(MouseEvent e) {
            if (!dragging) updateResizing(e);
        }

        public void mouseExited(MouseEvent e)  {
            if (!dragging) {
                currentResizing = RESIZE_NONE;
                setCursor(Cursor.getDefaultCursor());
            }
        }
        
        public void mouseMoved(MouseEvent e) {
            if (!dragging) updateResizing(e);
        }
        
        public void mouseDragged(MouseEvent e) {
            if (dragX >= 0 && dragY >= 0) {
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();
                
                int dx = x - dragX;
                int dy = y - dragY;
                
                int newX = window.getX();
                int newY = window.getY();
                int newW = window.getWidth();
                int newH = window.getHeight();
                
                int xx = 0;
                int yy = 0;
                Dimension min = window.getMinimumSize();
                
                if (isResizeLeft(currentResizing)) {
                    newX += dx;
                    newW -= dx;
                    if (newW < min.width) {
                        xx = newW - min.width;
                        newX += xx;
                        newW = min.width;
                    }
                } else if (isResizeRight(currentResizing)) {
                    newW += dx;
                    if (newW < min.width) {
                        xx = min.width - newW;
                        newW = min.width;
                    }
                }
                if (isResizeTop(currentResizing)) {
                    newY += dy;
                    newH -= dy;
                    if (newH < min.height) {
                        yy = newH - min.height;
                        newY += yy;
                        newH = min.height;
                    }
                } else if (isResizeBottom(currentResizing)) {
                    newH += dy;
                    if (newH < min.height) {
                        yy = min.height - newH;
                        newH = min.height;
                    }
                }
                
                window.setBounds(newX, newY, newW, newH);
                content.setSize(newW, newH);
                
                dragX = x + xx;
                dragY = y + yy;
            }
        }

        private void updateResizing(MouseEvent e) {
            int newResizing = RESIZE_NONE;
            
            int x = e.getX();
            int y = e.getY();
            
            if (isResizeLeft(resizeMode) && x < 8 && x >= 0) {
                newResizing |= RESIZE_LEFT;
            } else if (isResizeRight(resizeMode) && x > getWidth() - RESIZE_STRIPE && x < getWidth()) {
                newResizing |= RESIZE_RIGHT;
            }
            
            if (isResizeTop(resizeMode)&& y < 8 && y >= 0) {
                newResizing |= RESIZE_TOP;
            } else if (isResizeBottom(resizeMode)&& y > getHeight() - RESIZE_STRIPE && y < getHeight()) {
                newResizing |= RESIZE_BOTTOM;
            }
            
            currentResizing = newResizing;
            
            switch (currentResizing) {
                case RESIZE_NONE:
                    setCursor(Cursor.getDefaultCursor());
                    break;
                case RESIZE_TOP:
                    setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    break;
                case RESIZE_LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    break;
                case RESIZE_BOTTOM:
                    setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    break;
                case RESIZE_RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    break;
                case RESIZE_TOP | RESIZE_LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    break;
                case RESIZE_LEFT | RESIZE_BOTTOM:
                    setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    break;
                case RESIZE_BOTTOM | RESIZE_RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    break;
                case RESIZE_RIGHT | RESIZE_TOP:
                    setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    break;
            }
        }
        
        public void paint(Graphics g) {
            super.paint(g);
            
            if (resizeMode > 0) {
                g.setColor(UIUtils.getDisabledLineColor());
                
                switch (resizeMode) {
                    case RESIZE_TOP | RESIZE_LEFT:
                        g.drawLine(0, 5, 5, 0);
                        g.drawLine(0, 9, 9, 0);
                        break;
                    case RESIZE_TOP | RESIZE_RIGHT:
                        int w = getWidth();
                        g.drawLine(w - 6, 0, w, 6);
                        g.drawLine(w - 10, 0, w, 10);
                        break;
                    case RESIZE_BOTTOM | RESIZE_LEFT:
                        int h = getHeight();
                        g.drawLine(0, h - 6, 6, h);
                        g.drawLine(0, h - 10, 10, h);
                        break;
                    default:
                        w = getWidth();
                        h = getHeight();
                        g.drawLine(w, h - 7, w - 7, h);
                        g.drawLine(w, h - 11, w - 11, h);
                }
            }
        }
        
        boolean isResizeTop(int mode)    { return (mode & RESIZE_TOP) != 0; }
        boolean isResizeLeft(int mode)   { return (mode & RESIZE_LEFT) != 0; }
        boolean isResizeBottom(int mode) { return (mode & RESIZE_BOTTOM) != 0; }
        boolean isResizeRight(int mode)  { return (mode & RESIZE_RIGHT) != 0; }
        
        
        // --- Closing ---------------------------------------------------------
        
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
                    // NOTE: workaround for problem on macOS,
                    //       closing the dialog opened from ProfilerPopup
                    //       doesn't bring the focus back to the ProfilerPopup
                    final Window win = window;
                    final Component comp = window.getMostRecentFocusOwner();
                    d.addWindowListener(new WindowAdapter() {
                        public void windowClosed(WindowEvent e) {
                            if (DEBUG) System.err.println(">>> BLOCKING DIALOG CLOSED " + getString(d)); // NOI18N
                            win.requestFocus();
                            if (comp != null) comp.requestFocus();
                        }
                    });
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
            else return c == null ? "null" : c.getClass().getName(); // NOI18N
        }
        
        
        // --- ComponentListener -----------------------------------------------
        
        public void componentResized(ComponentEvent e) { if (DEBUG) System.err.println(">>> Closed by componentResized"); ProfilerPopup.this.hide(); }

        public void componentMoved(ComponentEvent e)   {
            Point newLocation = owner.getLocationOnScreen();
            window.setLocation(window.getX() + (newLocation.x - ownerLocation.x),
                               window.getY() + (newLocation.y - ownerLocation.y));
            ownerLocation = newLocation;
        }

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

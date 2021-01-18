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
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
final class ProfilerTableHovers {
    
    private static final int MAX_RENDERER_WIDTH = 5000;
    
    private static final int POPUP_LEFT = 0;
    private static final int POPUP_RIGHT = 1;
    
    
    private final ProfilerTable table;
    
    private Opener opener;
    private Closer closer;
    
    private CellRendererPane crp;
    
    private int currentRow = -1;
    private int currentColumn = -1;
    private Point currentScreenPoint;
    
    private final JWindow[] windows = new JWindow[2];
    
    
    // --- Internal API --------------------------------------------------------
    
    static void install(ProfilerTable table) {
        new ProfilerTableHovers(table).install();
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private ProfilerTableHovers(ProfilerTable table) {
        this.table = table;
    }
    
    private void install() {
        opener = new Opener();
        opener.install();
    }
    
    
    private void updatePopups(Point p, boolean repaint) {
        if (currentScreenPoint == null) {
            hidePopups();
        } else {
            if (p == null) {
                p = new Point(currentScreenPoint);
                SwingUtilities.convertPointFromScreen(p, table);
            }
            checkPopup(table.rowAtPoint(p), table.columnAtPoint(p), p, repaint);
        }
    }
    
    private void checkPopup(int row, int column, Point point, boolean repaint) {
        if (!table.isShowing()) {
            // Prevent "IllegalComponentStateException: component must be showing on the screen..."
            hidePopups();
            currentScreenPoint = null;
            return;
        }
        
        if (row < 0 || column < 0 || row >= table.getRowCount() ||
            column >= table.getColumnCount()) { hidePopups(); return; }
        
        Component renderer = getRenderer(row, column);
        Rectangle[] popups = computePopups(row, column, point, renderer);
        
        if (popups == null) {
            hidePopups();
        } else if (repaint || currentRow != row || currentColumn != column) {
//            // If reusing the popup for a new cell hide the current popup
//            // to honor window transitions (Linux) 
//            // !!! ACTUALLY NOT WORKING UNTIL Window.dispose() !!!
//            if (!repaint) {
//                if (windows[POPUP_LEFT] != null) windows[POPUP_LEFT].setVisible(false);
//                if (windows[POPUP_RIGHT] != null) windows[POPUP_RIGHT].setVisible(false);
//            }

            if (!isInFocusedWindow(table) || isPopupOpen()) {
                // Do not show value hovers when a lightweight popup is showing,
                // might be drawn on top of it - overlap it.
                hidePopups();
            } else {
                currentRow = row;
                currentColumn = column;

                showPopups(renderer, popups);
            }
        }
    }
    
    private static boolean isInFocusedWindow(Component c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        return w != null && w.isFocused();
    }
    
    private static boolean isPopupOpen() {
        MenuElement[] menuSel = MenuSelectionManager.defaultManager().getSelectedPath();
        return menuSel != null && menuSel.length > 0;
//        // Doesn't work reliably, hovering to a sliding window (Palette) changes the focus owner
//        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JRootPane;
    }
    
//    private static boolean isLwPopupOpen(Component c) {
//        Container cc = c.getParent();
//        
//        for (Container p = cc; p != null; p = p.getParent()) {
//            if (p instanceof JRootPane) {
//                if (p.getParent() instanceof JInternalFrame) continue;
//                cc = ((JRootPane)p).getLayeredPane();
//                if (cc instanceof JLayeredPane)
//                    return ((JLayeredPane)cc).getComponentsInLayer(
//                             JLayeredPane.POPUP_LAYER).length > 0;
//            } else if (p instanceof Window) {
//                break;
//            } else if (p instanceof JApplet) {
//                break;
//            }
//        }
//        
//        return false;
//    }
    
    private void showPopups(Component renderer, Rectangle[] popups) {
        Image img = createPopupImage(renderer);
        Color border = table.getGridColor();
        
        if (popups[POPUP_LEFT] != null) openWindow(popups[POPUP_LEFT], img, POPUP_LEFT, border);
        else if (windows[POPUP_LEFT] != null) closeWindow(POPUP_LEFT);
        
        if (popups[POPUP_RIGHT] != null) openWindow(popups[POPUP_RIGHT], img, POPUP_RIGHT, border);
        else if (windows[POPUP_RIGHT] != null) closeWindow(POPUP_RIGHT);
        
        if (closer == null) {
            closer = new Closer();
            closer.install();
        }
    }
    
    private void hidePopups() {
        if (windows[POPUP_LEFT] == null && windows[POPUP_RIGHT] == null) return;
        
        currentRow = -1;
        currentColumn = -1;
        
        if (windows[POPUP_LEFT] != null) closeWindow(POPUP_LEFT);
        if (windows[POPUP_RIGHT] != null) closeWindow(POPUP_RIGHT);
        
        if (closer != null) {
            closer.deinstall();
            closer = null;
        }
    }
    
    
    private Image createPopupImage(Component renderer) {
        int width = renderer.getWidth();
        int height = renderer.getHeight();
        
        Image i = !Platform.isMac() ? table.createVolatileImage(width, height) :
                  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = i.getGraphics();
        
        try {
            g.setColor(table.getBackground());
            g.fillRect(0, 0, width, height);
            
            // org.graalvm.visualvm.lib.ui.swing.renderer.Movable.move()
            renderer.move(0, 0);
            
            if (crp == null) crp = new CellRendererPane() {
                public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h, boolean v) {
                    super.paintComponent(g, c, p, x, y, w, h, v);
                    remove(c); // Prevent leaking ProfilerTreeTable.ProfilerTreeTableTree and transitively all the UI/models
                }
            };
            
            crp.paintComponent(g, renderer, null, 0, 0, width, height, false);
        } finally {
            g.dispose();
        }
        
        return i;
    }
    
    private void openWindow(Rectangle popup, final Image img, int popupId, Color border) {
        final boolean left = popupId == POPUP_LEFT;
        
        final int popupW = popup.width;
        final int popupH = popup.height;
        final int imageW = img.getWidth(null);
        final int imageH = img.getHeight(null);
        
        JPanel l = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                if (left) g.drawImage(img, 1, 1, 1 + popupW, 1 + popupH, 0, 0, popupW, popupH, null);
                else      g.drawImage(img, 0, 1, 0 + popupW, 1 + popupH, imageW - popupW, 0, imageW, imageH, null);
            }
        };
        if (left) l.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, border));
        else      l.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, border));
        l.setSize(popupW + 1, popupH + 2);
        
        JWindow win = windows[popupId];
        if (win == null) {
            win = new JWindow(SwingUtilities.getWindowAncestor(table));
            win.setType(Window.Type.POPUP);
            win.setFocusable(false);
            win.setAutoRequestFocus(false);
            win.setFocusableWindowState(false);
            win.getContentPane().add(l);
            
            // Make sure there's no shadow behind the native window
            safeSetBackground(win, new Color(255, 255, 255, 0)); // Linux // #269737
            win.getRootPane().putClientProperty("Window.shadow", Boolean.FALSE.toString()); // macOS // NOI18N
            
            win.setVisible(true);
        } else {
            win.getContentPane().removeAll();
            win.getContentPane().add(l);
            
            if (win.isVisible()) win.repaint();
            else win.setVisible(true);
        }
        
        Point p = table.getLocationOnScreen();
        win.setBounds(popup.x + p.x - (left ? 1 : 0), popup.y + p.y - 1, popupW + 1, popupH + 2);
        
        windows[popupId] = win;
    }
    
    private void closeWindow(int index) {
        windows[index].setVisible(false);
        windows[index].dispose();
        windows[index] = null;
    }
    
    // See Window.setBackground() documentation
    private static void safeSetBackground(JWindow window, Color background) {
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        
//        if (!gc.isTranslucencyCapable()) return; // PERPIXEL_TRANSLUCENT not supported // doesn't work well with Ubuntu@VirtualBox, handled in catch below
        if (gc.getDevice().getFullScreenWindow() == window) return; // fullscreen windows not supported
        
        // PERPIXEL_TRANSLUCENT not supported, safely ignore
        try { window.setBackground(background); }
        catch (UnsupportedOperationException e) {}
    }
    
    
    private Rectangle[] computePopups(int row, int column, Point point, Component renderer) {
        Rectangle rendererRect = getRendererRect(column, renderer);
        if (rendererRect == null) return null;
        
        Rectangle cellRect = table.getCellRect(row, column, true);
        rendererRect.translate(cellRect.x, cellRect.y);
        cellRect.width -= 1;
        if (cellRect.contains(rendererRect)) return null; // Value fully visible
        
        Rectangle visibleRect = cellRect.intersection(rendererRect);
        if (!visibleRect.contains(point)) return null; // Value fully invisible
        
        // Mouse over partially visible value
        Rectangle[] ret = new Rectangle[2];
        
        if (rendererRect.x < visibleRect.x) {
            Rectangle left = new Rectangle(rendererRect);
            left.width = visibleRect.x - left.x;
            ret[POPUP_LEFT] = left;
        }

        // rendererRect.x + rendererRect.width *- 1*: workaround for extra space for correctly right-aligned values
        if (rendererRect.x + rendererRect.width - 1 > visibleRect.x + visibleRect.width) {
            Rectangle right = new Rectangle(rendererRect);
            right.x = visibleRect.x + visibleRect.width;
            right.width = rendererRect.x + rendererRect.width - right.x;
            ret[POPUP_RIGHT] = right;
        }
        
        return ret;
    }
    
    
    private Rectangle getRendererRect(int column, Component renderer) {
        int _column = table.convertColumnIndexToModel(column);
        
        // Do not show value hovers for standard renderers shortening values using '...'
        if (!(renderer instanceof ProfilerRenderer) &&
            !table.isScrollableColumn(_column))
                return null;
        
        // Do not show value hovers when explicitly disabled
        if (renderer instanceof JComponent)
            if (((JComponent)renderer).getClientProperty(ProfilerTable.PROP_NO_HOVER) != null)
                return null;
        
        Rectangle bounds = renderer.getBounds();
        bounds.x -= table.getColumnOffset(_column);
        
        return bounds;
    }
    
    private Component getRenderer(int row, int column) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        Component _renderer = table.getRenderer(renderer, row, column, true);
        _renderer.setSize(Math.min(_renderer.getWidth(), MAX_RENDERER_WIDTH), _renderer.getHeight());
        return _renderer;
    }
    
    
    private class Opener extends MouseAdapter implements ComponentListener, TableModelListener {
        
        void install() {
            table.addMouseListener(this);
            table.addMouseMotionListener(this);
            table.addComponentListener(this);
            table.getModel().addTableModelListener(this);
        }
        void deinstall() {
            hidePopups();
            
            table.removeMouseListener(this);
            table.removeMouseMotionListener(this);
            table.removeComponentListener(this);
            table.getModel().removeTableModelListener(this);
            
            currentScreenPoint = null;
        }
        
        // MouseAdapter
        public void mouseMoved(MouseEvent e) {
            // Do not display popup when a modifier is pressed (can't read all keys)
//            if (e.getModifiers() != 0) return;
            
            currentScreenPoint = e.getLocationOnScreen();
            
            updatePopups(e.getPoint(), false);
        }
        
        public void mouseDragged(MouseEvent e) {
//            if (e.getModifiers() != 0) return;
            
            currentScreenPoint = e.getLocationOnScreen();
            
            updatePopups(e.getPoint(), false);
        }
        
        public void mouseExited(MouseEvent e) {
            hidePopups();
            currentScreenPoint = null;
        }
        
        // ComponentListener
        public void componentResized(ComponentEvent e) {} // Lines added/removed to/from table
        public void componentMoved(ComponentEvent e) { updatePopups(null, false); } // Table scrolled (mouse wheel, gesture)
        public void componentShown(ComponentEvent e) {}
        public void componentHidden(ComponentEvent e) {}
        
        // TableModelListener
        public void tableChanged(TableModelEvent e) {
            // Must invoke later, column widths not ready yet
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updatePopups(null, true); }
            });
        }
        
    }
    
    private class Closer extends MouseAdapter implements /*TableModelListener,*/ KeyListener,
                                                         ComponentListener, HierarchyListener,
                                                         HierarchyBoundsListener, FocusListener,
                                                         ListSelectionListener {
        
//        private Component focusOwner;
                
        void install() {
            table.addMouseListener(this);
            table.addMouseMotionListener(this);
            
//            focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
//            if (focusOwner != null) {
//                focusOwner.addKeyListener(this);
//                focusOwner.addFocusListener(this);
//            }
            
//            table.getModel().addTableModelListener(this);
            table.getSelectionModel().addListSelectionListener(this);
            table.addComponentListener(this);
            table.addHierarchyListener(this);
            table.addHierarchyBoundsListener(this);
        }
        
        void deinstall() {
            table.removeMouseListener(this);
            table.removeMouseMotionListener(this);
            
//            if (focusOwner != null) {
//                focusOwner.removeKeyListener(this);
//                focusOwner.removeFocusListener(this);
//                focusOwner = null;
//            }
            
//            table.getModel().removeTableModelListener(this);
            table.getSelectionModel().removeListSelectionListener(this);
            table.removeComponentListener(this);
            table.removeHierarchyListener(this);
            table.removeHierarchyBoundsListener(this);
        }
        
        // MouseAdapter
//        public void mouseExited(MouseEvent e) { hidePopups(); currentScreenPoint = null; }
//        public void mouseDragged(MouseEvent e) { updatePopups(e.getPoint(), false); }
//        public void mouseDragged(MouseEvent e) { hidePopups(); }
//        public void mousePressed(MouseEvent e) { hidePopups(); }
//        public void mouseReleased(MouseEvent e) { hidePopups(); }
        public void mousePressed(MouseEvent e) { updatePopups(e.getPoint(), true); }
        public void mouseReleased(MouseEvent e) { updatePopups(e.getPoint(), true); }
        
//        // TableModelListener
//        public void tableChanged(TableModelEvent e) { updatePopups(null, true); }
        
        // ListSelectionListener
        public void valueChanged(ListSelectionEvent e) { updatePopups(null, true); }

        // KeyListener
        public void keyTyped(KeyEvent e) { hidePopups(); }
        public void keyPressed(KeyEvent e) { hidePopups(); }
        public void keyReleased(KeyEvent e) { hidePopups(); }
        
        // ComponentListener
        public void componentResized(ComponentEvent e) { /*hidePopups();*/ } // Lines added/removed to/from table
        public void componentMoved(ComponentEvent e) { }  // Table scrolled (mouse wheel, gesture
        public void componentShown(ComponentEvent e) { hidePopups(); }
        public void componentHidden(ComponentEvent e) { hidePopups(); currentScreenPoint = null; }

        // HierarchyListener
        public void hierarchyChanged(HierarchyEvent e) { hidePopups(); }
        
        // HierarchyBoundsListener
        public void ancestorMoved(HierarchyEvent e) { hidePopups(); }
        public void ancestorResized(HierarchyEvent e) { hidePopups(); }
        
        // FocusListener
        public void focusGained(FocusEvent e) {}
        public void focusLost(FocusEvent e) { hidePopups(); currentScreenPoint = null; }

        // PropertyChangeListener
        public void propertyChange(PropertyChangeEvent evt) { hidePopups(); }
        
    }
    
}

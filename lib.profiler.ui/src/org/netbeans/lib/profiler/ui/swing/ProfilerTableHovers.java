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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
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
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 * 
 * 
 * TODO:
 * 
 *  - limit image size by screen bounds
 *  - limit window size(s) by screen bounds
 * 
 *  - handle value changes when value hover is displayed
 *  - keep the value hover displayed when (left?)clicking the table?
 * 
 *  - do not show value hovers over other popups (menus)
 * 
 */
final class ProfilerTableHovers {
    
    static void install(ProfilerTable table) {
        new ProfilerTableHovers(table).install();
    }
    
    
    private final ProfilerTable table;
    
    private Opener opener;
    private Closer closer;
    
    private CellRendererPane crp;
    
    private int currentRow = -1;
    private int currentColumn = -1;
    
    
    private ProfilerTableHovers(ProfilerTable table) {
        this.table = table;
    }
    
    private void install() {
        opener = new Opener();
        opener.install();
        
        closer = new Closer();
        
        crp = new CellRendererPane() {
            public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h, boolean v) {
                super.paintComponent(g, c, p, x, y, w, h, v);
                remove(c); // Prevent leaking ProfilerTreeTable.ProfilerTreeTableTree and transitively all the UI/models
            }
        };
    }
    
    private final List<Window> windows = new ArrayList(2);
    
    private void showPopups(Component renderer, Rectangle[] popups) {
        Image img = createPopupImage(renderer);
        Color border = table.getGridColor();
        
        if (popups[0] != null) {
            Window left = createWindow(popups[0], img, true, border);
            left.setVisible(true);
            windows.add(left);
        }
        
        if (popups[1] != null) {
            Window right = createWindow(popups[1], img, false, border);
            right.setVisible(true);
            windows.add(right);
        }
        
        closer.install();
    }
    
    private void hidePopups() {
        if (windows.isEmpty()) return;
        
        currentRow = -1;
        currentColumn = -1;
        
        for (Window w : windows) {
            w.setVisible(false);
            w.dispose();
        }
        
        windows.clear();
        
        closer.deinstall();
    }
    
    
    private Image createPopupImage(Component renderer) {
        int width = renderer.getWidth();
        int height = renderer.getHeight();
        
        // org.netbeans.lib.profiler.ui.swing.renderer.Movable.move()
        renderer.move(0, 0);
        
        Image i = !Platform.isMac() ? table.createVolatileImage(width, height) :
                  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = i.getGraphics();
        try {
            g.setColor(table.getBackground());
            g.fillRect(0, 0, width, height);
            crp.paintComponent(g, renderer, null, 0, 0, width, height, false);
        } finally {
            g.dispose();
        }
        
        return i;
    }
    
    private Window createWindow(Rectangle popup, final Image img, final boolean leading, final Color border) {
        final int popupW = popup.width;
        final int popupH = popup.height;
        final int imageW = img.getWidth(null);
        final int imageH = img.getHeight(null);
        
        JPanel l = new JPanel(null) {
            {
                if (leading) setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, border));
                else         setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, border));
            }
            protected void paintComponent(Graphics g) {
                if (leading) g.drawImage(img, 1, 1, 1 + popupW, 1 + popupH, 0, 0, popupW, popupH, null);
                else         g.drawImage(img, 0, 1, 0 + popupW, 1 + popupH, imageW - popupW, 0, imageW, imageH, null);
            }
        };
        l.setSize(popupW + 1, popupH + 2);
        
        JWindow win = new JWindow(SwingUtilities.getWindowAncestor(table));
        win.setType(Window.Type.POPUP);
        win.setFocusable(false);
        win.setAutoRequestFocus(false);
        win.setFocusableWindowState(false);
        win.getContentPane().add(l);

        // Make sure there's no shadow behind the native window
        win.setBackground(new Color(255, 255, 255, 0)); // Linux
        win.getRootPane().putClientProperty("Window.shadow", Boolean.FALSE.toString()); // Mac OS X // NOI18N
        
        Point p = table.getLocationOnScreen();
        win.setBounds(popup.x + p.x - (leading ? 1 : 0), popup.y + p.y - 1, popupW + 1, popupH + 2);
        
        return win;
    }
    
    
    private void checkPopup(int row, int column, Point point) {
        if (row < 0 || column < 0 || row >= table.getRowCount() ||
            column >= table.getColumnCount()) hidePopups();
        
        Component renderer = getRenderer(row, column);
        Rectangle[] popups = computePopups(row, column, point, renderer);
        
        if (popups == null) {
            hidePopups();
        } else if (currentRow != row || currentColumn != column) {
            hidePopups();
            currentRow = row;
            currentColumn = column;
            showPopups(renderer, popups);
        }
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
            ret[0] = left;
        }

        // rendererRect.x + rendererRect.width *- 1*: workaround for extra space for correctly right-aligned values
        if (rendererRect.x + rendererRect.width - 1 > visibleRect.x + visibleRect.width) {
            Rectangle right = new Rectangle(rendererRect);
            right.x = visibleRect.x + visibleRect.width;
            right.width = rendererRect.x + rendererRect.width - right.x;
            ret[1] = right;
        }
        
        return ret;
    }
    
    
    private Rectangle getRendererRect(int column, Component renderer) {
        int _column = table.convertColumnIndexToModel(column);
        
        // Do not show value hovers for standard renderers shortening values using '...'
        if (!(renderer instanceof ProfilerRenderer) &&
            !table.isScrollableColumn(_column))
                return null;
        
        // Do not show value hovers when explicitely disabled
        if (renderer instanceof JComponent)
            if (((JComponent)renderer).getClientProperty(ProfilerTable.PROP_NO_HOVER) != null)
                return null;
        
        Rectangle bounds = renderer.getBounds();
        bounds.x -= table.getColumnOffset(_column);
        
        return bounds;
    }
    
    private Component getRenderer(int row, int column) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        return table.getRenderer(renderer, row, column, true);
    }
    
    
    private class Opener extends MouseAdapter implements ComponentListener {
        private Point currentScreenPoint;
        
        void install() {
            table.addMouseListener(this);
            table.addMouseMotionListener(this);
            table.addComponentListener(this);
        }
        void deinstall() {
            hidePopups();
            
            table.removeMouseListener(this);
            table.removeMouseMotionListener(this);
            table.removeComponentListener(this);
            
            currentScreenPoint = null;
        }
        
        // MouseAdapter
        public void mouseMoved(MouseEvent e) {
            // Do not display popup when a modifier is pressed (can't read all keys)
            if (e.getModifiers() != 0) return;
            
            Point p = e.getPoint();
            
            currentScreenPoint = new Point(p);
            SwingUtilities.convertPointToScreen(currentScreenPoint, table);
            
            checkPopup(table.rowAtPoint(p), table.columnAtPoint(p), p);
        }
        
        // ComponentListener
        public void componentResized(ComponentEvent e) {}
        public void componentMoved(ComponentEvent e) { updatePopups(); }
        public void componentShown(ComponentEvent e) {}
        public void componentHidden(ComponentEvent e) {}
        
        void updatePopups() {
            hidePopups();
            
            if (currentScreenPoint != null) {
                Point p = new Point(currentScreenPoint);
                SwingUtilities.convertPointFromScreen(p, table);
                checkPopup(table.rowAtPoint(p), table.columnAtPoint(p), p);
            }
        }
    }
    
    private class Closer extends MouseAdapter implements TableModelListener, KeyListener,
                                                         ComponentListener, HierarchyListener,
                                                         HierarchyBoundsListener, FocusListener {
        
        private Component focusOwner;
                
        void install() {
            table.addMouseListener(this);
            table.addMouseMotionListener(this);
            
            focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != null) {
                focusOwner.addKeyListener(this);
                focusOwner.addFocusListener(this);
            }
            
            table.getModel().addTableModelListener(this);
            table.addComponentListener(this);
            table.addHierarchyListener(this);
            table.addHierarchyBoundsListener(this);
        }
        
        void deinstall() {
            table.removeMouseListener(this);
            table.removeMouseMotionListener(this);
            
            if (focusOwner != null) {
                focusOwner.removeKeyListener(this);
                focusOwner.removeFocusListener(this);
                focusOwner = null;
            }
            
            table.getModel().removeTableModelListener(this);
            table.removeComponentListener(this);
            table.removeHierarchyListener(this);
            table.removeHierarchyBoundsListener(this);
        }
        
        // MouseAdapter
        public void mouseExited(MouseEvent e) { hidePopups(); }
        public void mouseDragged(MouseEvent e) { hidePopups(); }
        public void mousePressed(MouseEvent e) { hidePopups(); }
        public void mouseReleased(MouseEvent e) { hidePopups(); }
        
        // TableModelListener
        public void tableChanged(TableModelEvent e) {
//            if (painter.valueChanged()) {
//                final int row = painter.getRow();
//                final int column = painter.getColumn();
//                hidePopup();
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() { checkPopup(row, column, null); }
//                });
//            }
        }

        // KeyListener
        public void keyTyped(KeyEvent e) { hidePopups(); }
        public void keyPressed(KeyEvent e) { hidePopups(); }
        public void keyReleased(KeyEvent e) { hidePopups(); }
        
        // ComponentListener
        public void componentResized(ComponentEvent e) { hidePopups(); }
        public void componentMoved(ComponentEvent e) { }
        public void componentShown(ComponentEvent e) { hidePopups(); }
        public void componentHidden(ComponentEvent e) { hidePopups(); }

        // HierarchyListener
        public void hierarchyChanged(HierarchyEvent e) { hidePopups(); }
        
        // HierarchyBoundsListener
        public void ancestorMoved(HierarchyEvent e) { hidePopups(); }
        public void ancestorResized(HierarchyEvent e) { hidePopups(); }
        
        // FocusListener
        public void focusGained(FocusEvent e) {}
        public void focusLost(FocusEvent e) { hidePopups(); }

        // PropertyChangeListener
        public void propertyChange(PropertyChangeEvent evt) { hidePopups(); }
        
    }
    
}

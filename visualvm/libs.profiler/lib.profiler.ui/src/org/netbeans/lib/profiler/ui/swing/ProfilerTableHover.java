/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
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
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerTableHover {
    
    private static final boolean REPAINT_ON_HIDE =
            !Boolean.getBoolean("ProfilerTableHover.noRepaintOnHide"); // NOI18N
    
    
    static void install(ProfilerTable table) {
        new ProfilerTableHover(table).install();
    }
    
    
    private final ProfilerTable table;
    
    private Popup popup;
    private Point popupLocation; // topleft popup corner, table coords
    private Rectangle popupRect; // visible cell rect, screeen coords
    
    private AWT awt;
    private Mouse mouse;
    private Paranoid paranoid;
    
    private Point currentPoint;
    private Point forwardPoint;
    
    
    private ProfilerTableHover(ProfilerTable table) {
        this.table = table;
    }
    
    private void install() {
        mouse = new Mouse();
        mouse.install();
    }
    
    private void showPopup(Painter p, Rectangle rect) {
        mouse.deinstall();
        
        Point l = table.getLocationOnScreen();
        
        rect.translate(l.x, l.y);
        popupRect = rect;
        popupLocation = new Point(l.x + p.getX(), l.y + p.getY());
        
        PopupFactory popupFactory = PopupFactory.getSharedInstance();
        popup = popupFactory.getPopup(table, p, popupLocation.x, popupLocation.y);
        popup.show();
        
        paranoid = new Paranoid(p);
        paranoid.install();
        
        awt = new AWT();
        awt.install();
    }
    
    private void hidePopup() {
        if (awt != null) {
            awt.deinstall();
            awt = null;
        }
        
        if (paranoid != null) {
            paranoid.deinstall();
            paranoid = null;
        }
        
        popup.hide();
        popupRect = null;
        popupLocation = null;
        popup = null;
        
        // Make sure lightweight popups are cleared correctly when mouse-wheeling
        if (REPAINT_ON_HIDE) table.repaint();
        
        // Skip AWT noise after closing popup
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { mouse.install(); }
        });
    }
    
    private void checkPopup(int row, int column, Point point) {
        if (row < 0 || row >= table.getRowCount()) return;
        if (column < 0 || column >= table.getColumnCount()) return;
        
        if (point == null) point = currentPoint; else currentPoint = point;
        if (point == null) return;
        
        Rectangle cellRect = table.getCellRect(row, column, true);
        Rectangle rendererRect = getRendererRect(row, column);
        if (rendererRect == null) return;
        
        rendererRect.translate(cellRect.x, cellRect.y);
        if (cellRect.contains(rendererRect)) return; // Value fully visible
        
        Rectangle visibleRect = cellRect.intersection(rendererRect);
        if (visibleRect.contains(point)) // Mouse over partially visible value
            showPopup(new Painter(row, column, rendererRect), visibleRect);

    }
    
    
    private class AWT implements AWTEventListener {
        
        void install() {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_MOTION_EVENT_MASK);
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }
        
        void deinstall() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        }

        public void eventDispatched(AWTEvent e) {
            if (popup == null) return;
            
            // Not a mouse event
            if (!(e instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent)e;
            
            // Event not relevant
            if (isIgnoreEvent(me)) return;
            
            // Mouse moved over popup
            if (me.getID() == MouseEvent.MOUSE_MOVED && overPopup(me)) return;
            
            if (!overPopup(me)) {
                // Mouse event outside of popup
                hidePopup();
            } else if (isForwardEvent(me)) {
                // Mouse event on popup, to be forwarded to table
                Point popupPoint = popupLocation;
                hidePopup();
                forwardEvent(me, popupPoint);
            }
        }
        
        private boolean overPopup(MouseEvent e) {
            if (popupRect == null) return false;
            // NOTE: e.getLocationOnScreen() doesn't work for MOUSE_WHEEL events
            Point p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());
            return popupRect.contains(p);
        }
        
        private boolean isIgnoreEvent(MouseEvent e) {
            int eventID = e.getID();
            return eventID == MouseEvent.MOUSE_ENTERED ||
                   eventID == MouseEvent.MOUSE_EXITED;
        }
        
        private boolean isForwardEvent(MouseEvent e) {
            int eventID = e.getID();
            return eventID == MouseEvent.MOUSE_PRESSED ||
                   eventID == MouseEvent.MOUSE_RELEASED ||
                   eventID == MouseEvent.MOUSE_WHEEL;
        }
        
        private void forwardEvent(MouseEvent e, Point popupPoint) {
            Point p = e.getPoint();
            p.translate(popupPoint.x, popupPoint.y);
            SwingUtilities.convertPointFromScreen(p, table);
            forwardPoint = new Point(p.x - 1, p.y - 1);
            
            MouseWheelEvent we = e instanceof MouseWheelEvent ? (MouseWheelEvent)e : null;
            MouseEvent ee = we != null ? new MouseWheelEvent(table, e.getID(), e.getWhen(),
                                             e.getModifiers(), p.x, p.y, e.getClickCount(),
                                             e.isPopupTrigger(), we.getScrollType(),
                                             we.getScrollAmount(), we.getWheelRotation()) :
                                         new MouseEvent(table, e.getID(), e.getWhen(),
                                             e.getModifiers(), p.x, p.y, e.getClickCount(),
                                             e.isPopupTrigger(), e.getButton());
            
            table.dispatchEvent(ee);
            
            // #241878 dispatch MOUSE_RELEASED after forwarding MOUSE_PRESSED
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                ee = new MouseEvent(table, MouseEvent.MOUSE_RELEASED, e.getWhen() + 1,
                                    e.getModifiers(), p.x, p.y, e.getClickCount(),
                                    e.isPopupTrigger(), e.getButton());
                table.dispatchEvent(ee);
            }
        }
        
    }
    
    private class Paranoid implements TableModelListener, KeyListener, ComponentListener,
                                      HierarchyListener, HierarchyBoundsListener,
                                      FocusListener, PropertyChangeListener {
        private final Painter painter;
        private Component focusOwner;
        
        Paranoid(Painter painter) {
            this.painter = painter;
        }
        
        void install() {
            focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != null) {
                focusOwner.addKeyListener(this);
                if (table.equals(focusOwner)) table.addFocusListener(this);
            }
            
            table.getModel().addTableModelListener(this);
            table.addComponentListener(this);
            table.addHierarchyListener(this);
            table.addHierarchyBoundsListener(this);
        }
        void deinstall() {
            if (focusOwner != null) {
                focusOwner.removeKeyListener(this);
                if (table.equals(focusOwner)) table.removeFocusListener(this);
                focusOwner = null;
            }
            
            table.getModel().removeTableModelListener(this);
            table.removeComponentListener(this);
            table.removeHierarchyListener(this);
            table.removeHierarchyBoundsListener(this);
        }
        
        // TableModelListener
        public void tableChanged(TableModelEvent e) {
            if (painter.valueChanged()) {
                final int row = painter.getRow();
                final int column = painter.getColumn();
                hidePopup();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { checkPopup(row, column, null); }
                });
            }
        }

        // KeyListener
        public void keyTyped(KeyEvent e) { hidePopup(); }
        public void keyPressed(KeyEvent e) { hidePopup(); }
        public void keyReleased(KeyEvent e) { hidePopup(); }
        
        // ComponentListener
        public void componentResized(ComponentEvent e) { hidePopup(); }
        public void componentMoved(ComponentEvent e) { hidePopup(); }
        public void componentShown(ComponentEvent e) { hidePopup(); }
        public void componentHidden(ComponentEvent e) { hidePopup(); }

        // HierarchyListener
        public void hierarchyChanged(HierarchyEvent e) { hidePopup(); }
        
        // HierarchyBoundsListener
        public void ancestorMoved(HierarchyEvent e) { hidePopup(); }
        public void ancestorResized(HierarchyEvent e) { hidePopup(); }
        
        // FocusListener
        public void focusGained(FocusEvent e) {}
        public void focusLost(FocusEvent e) { hidePopup(); }

        // PropertyChangeListener
        public void propertyChange(PropertyChangeEvent evt) { hidePopup(); }
    }
    
    private class Mouse extends MouseAdapter {
        void install() {
            table.addMouseMotionListener(this);
        }
        void deinstall() {
            table.removeMouseMotionListener(this);
        }
        public void mouseMoved(MouseEvent e) {
            // Do not display popup when a modifier is pressed (can't read all keys)
            if (e.getModifiers() != 0) return;
                    
            Point point = e.getPoint();
            // Skip AWT noise after closing popup
            if (point.equals(forwardPoint)) return;
            
            checkPopup(table.rowAtPoint(point), table.columnAtPoint(point), point);
        }
    }
    
    private class Painter extends JPanel {
        
        private final int row;
        private final int column;
        
        private final Object value;
        private final TableCellRenderer renderer;
        
        Painter(int row, int column, Rectangle bounds) {
            super(null);
            
            this.row = row;
            this.column = column;
            
            value = table.getValueAt(row, column);
            renderer = table.getCellRenderer(row, column);
            
            setBorder(BorderFactory.createLineBorder(table.getGridColor()));
            setBounds(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);
            
            setPreferredSize(getSize());
        }
        
        protected void paintComponent(Graphics g) {
            Component x = table.getRenderer(renderer, row, column, false);
            getPainter().paintComponent(g, x, null, 1, 1, getWidth() - 2, getHeight() - 2, false);
        }
        
        int getRow() {
            return row;
        }
        
        int getColumn() {
            return column;
        }
        
        boolean valueChanged() {
            if (table.getRowCount() <= row) return true;
            if (table.getColumnCount() <= column) return true;
            Object v = table.getValueAt(row, column);
            if (v == null && value == null) return false;
            if (v != null && value != null) return !v.equals(value);
            return true;
        }
        
    }
    
    private Rectangle getRendererRect(int row, int column) {
        Component component = getRenderer(row, column);
        
        if (component instanceof JComponent)
            if (((JComponent)component).getClientProperty(ProfilerTable.PROP_NO_HOVER) != null)
                return null;
        
        Rectangle bounds = component.getBounds();
        bounds.x -= table.getColumnOffset(table.convertColumnIndexToModel(column));
        
        return bounds;
    }
    
    private Component getRenderer(int row, int column) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        return table.getRenderer(renderer, row, column, true);
    }
    
    private static CellRendererPane PAINTER;
    private static CellRendererPane getPainter() {
        if (PAINTER == null) PAINTER = new CellRendererPane() {
            public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h, boolean shouldValidate) {
                super.paintComponent(g, c, p, x, y, w, h, shouldValidate);
                remove(c); // Prevent leaking ProfilerTreeTable.ProfilerTreeTableTree and transitively all the UI/models
            }
        };
        return PAINTER;
    }
    
}

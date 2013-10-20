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
import java.awt.Dimension;
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
    
    static void install(ProfilerTable table) {
        new ProfilerTableHover(table).install();
    }
    
    
    private final ProfilerTable table;
    
    private Popup popup;
    private Rectangle popupRect;
    
    private AWT awt;
    private Mouse mouse;
    private Paranoid paranoid;
    
    private Point forwardPoint;
    
    
    private ProfilerTableHover(ProfilerTable table) {
        this.table = table;
    }
    
    private void install() {
        mouse = new Mouse();
        mouse.install();
    }
    
    
    private void showPopup(int row, int column, Painter p) {
        mouse.deinstall();
        
        Point l = table.getLocationOnScreen();
        Rectangle r = table.getCellRect(row, column, true);
        popupRect = new Rectangle(l.x + r.x, l.y + r.y, r.width, r.height);
        PopupFactory popupFactory = PopupFactory.getSharedInstance();
        popup = popupFactory.getPopup(table, p, popupRect.x - 1, popupRect.y - 1);
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
        popup = null;
        
        // Skip AWT noise after closing popup
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { mouse.install(); }
        });
    }
    
    private void checkPopup(int row, int column) {
        if (row < 0 || row >= table.getRowCount()) return;
        if (column < 0 || column >= table.getColumnCount()) return;

        Painter p = new Painter(row, column);
        Rectangle r = table.getCellRect(row, column, true);

        if (p.getWidth() > r.width) showPopup(row, column, p);
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
                Rectangle lastPopupRect = popupRect;
                hidePopup();
                forwardEvent(me, lastPopupRect);
            }
        }
        
        private boolean overPopup(MouseEvent e) {
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
        
        private void forwardEvent(MouseEvent e, Rectangle lastPopupRect) {
            Point p = e.getPoint();
            p.translate(lastPopupRect.x, lastPopupRect.y);
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
                    public void run() { checkPopup(row, column); }
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
            Point point = e.getPoint();
            // Skip AWT noise after closing popup
            if (point.equals(forwardPoint)) return;
            
            checkPopup(table.rowAtPoint(point), table.columnAtPoint(point));
        }
    }
    
    private class Painter extends JPanel {
        
        private final int row;
        private final int column;
        private final Object value;
        private final TableCellRenderer renderer;
        
        Painter(int row, int column) {
            super(null);
            
            this.row = row;
            this.column = column;
            
            value = table.getValueAt(row, column);
            renderer = table.getCellRenderer(row, column);
            Dimension size = table.getRenderer(renderer, row, column).getPreferredSize();
            size.width += 2;
            size.height += 4;
            setSize(size);
            setPreferredSize(size);
            setBorder(BorderFactory.createLineBorder(table.getGridColor()));
        }
        
        protected void paintComponent(Graphics g) {
            Component x = table.getRenderer(renderer, row, column);
            x.setBounds(getBounds());
            getPainter().paintComponent(g, x, null, 1, 0, getWidth(), getHeight(), false);
        }
        
        int getRow() {
            return row;
        }
        
        int getColumn() {
            return column;
        }
        
        boolean valueChanged() {
            Object v = table.getValueAt(row, column);
            if (v == null && value == null) return false;
            if (v != null && value != null) return !v.equals(value);
            return true;
        }
        
    }
    
    private static CellRendererPane PAINTER;
    private static CellRendererPane getPainter() {
        if (PAINTER == null) PAINTER = new CellRendererPane();
        return PAINTER;
    }
    
}

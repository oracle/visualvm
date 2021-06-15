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

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * JComponent with TableHeader appearance.
 *
 * @author Jiri Sedlacek
 */
class HeaderComponent extends JComponent {

    private boolean isPressed;
    private boolean isSelected;

    HeaderComponent(final ActionListener listener) {
        if (listener != null) addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                isSelected = true;
                repaint();
            }
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                if (isSelected) {
                    repaint();
                    fireActionEvent(e);
                }
            }
            public void mouseEntered(MouseEvent e) {
                if (isPressed || !isButtonDown(e)) {
                    isSelected = true;
                    repaint();
                }
            }
            public void mouseExited(MouseEvent e) {
                if (isSelected) {
                    isSelected = false;
                    repaint();
                }
            }
            private void fireActionEvent(MouseEvent e) {
                ActionEvent ae = new ActionEvent(e.getSource(), e.getID(),
                                                 e.paramString(), e.getWhen(),
                                                 e.getModifiers());
                listener.actionPerformed(ae);
            }
            private boolean isButtonDown(MouseEvent e) {
                return SwingUtilities.isLeftMouseButton(e) ||
                       SwingUtilities.isMiddleMouseButton(e) ||
                       SwingUtilities.isRightMouseButton(e);
            }
        });
    }
    
    protected void paintComponent(Graphics g) {
        JTableHeader header = getHeader();
        setupHeader(header);
        TableCellRenderer renderer = header.getDefaultRenderer();
        JComponent component = (JComponent)renderer.getTableCellRendererComponent(
                               getTable(), "", isSelected && isPressed, isFocusOwner(), -1, 0); // NOI18N
        
        int height = header.getPreferredSize().height;
        component.setBounds(0, 0, getWidth(), height);
        component.setOpaque(false);
        getPainter().paintComponent(g, component, null, 0, 0, getWidth(), height, false);
    }
    
    private void setupHeader(JTableHeader h) {
        h.setDraggedColumn(isSelected && isPressed ? getColumn() : null);
        
        MouseEvent e = isSelected && !isPressed ?
                new MouseEvent(h, MouseEvent.MOUSE_ENTERED, 1, 0, 1, 1, 0, false) :
                new MouseEvent(h, MouseEvent.MOUSE_EXITED, 1, 0, 0, 0, 0, false);
        h.dispatchEvent(e);
    }
    
    
    private static CellRendererPane PAINTER;
    private static CellRendererPane getPainter() {
        if (PAINTER == null) PAINTER = new CellRendererPane();
        return PAINTER;
    }
    
    private static JTable REF_TABLE;
    private static JTable getTable() {
        if (REF_TABLE == null) REF_TABLE = new JTable(new Object[][] {{}},
                                                      new Object[] { " " }) { // NOI18N
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public boolean contains(int x, int y) { return x == 1 && y == 1; }
                };
            }
        };
        return REF_TABLE;
    }
    
    private static JTableHeader getHeader() {
        return getTable().getTableHeader();
    }
    
    private static TableColumn getColumn() {
        return getHeader().getColumnModel().getColumn(0);
    }
    
}

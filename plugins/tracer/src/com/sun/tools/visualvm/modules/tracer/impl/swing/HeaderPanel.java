/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.OverlayLayout;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeaderPanel extends JPanel {

    private JPanel clientContainer;
    private Header header;

    public HeaderPanel() {
        initComponents();
    }


    protected Object getRendererValue() { return null; }

    protected void setupRenderer(Component renderer) {}

    protected boolean isSelected() { return false; }

    protected boolean processMouseEvents() { return false; }


    public JPanel getClientContainer() {
        if (clientContainer == null) {
            clientContainer = new JPanel(null);
            clientContainer.setOpaque(false);
            add(clientContainer, 0);
        }
        return clientContainer;
    }

    public boolean isOptimizedDrawingEnabled() {
        return clientContainer == null;
    }
    
    protected void processMouseEvent(MouseEvent e) {
        if (processMouseEvents()) header.processMouseEvent(e);
        if (!e.isConsumed()) super.processMouseEvent(e);
    }

    private void initComponents() {
        JTable impl = new JTable(new DefaultTableModel(new Object[] { "" }, 0)); // NOI18N
        impl.setFocusable(false);
        header = new Header(impl.getColumnModel());
        impl.setTableHeader(header);
        header.setResizingAllowed(false);
        header.setReorderingAllowed(false);

        final TableCellRenderer renderer = header.getDefaultRenderer();
        header.setDefaultRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component component = renderer.getTableCellRendererComponent(
                        table, getRendererValue(), isSelected(),
                        isSelected(), row, processMouseEvents() ? 0 : 1);

                setupRenderer(component);

                table.getColumnModel().getColumn(0).setWidth(table.getWidth());
                return component;
            }
        });

        JScrollPane scroll = new JScrollPane(impl, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            public Dimension getPreferredSize() { return header.getPreferredSize(); }
            public void reshape(int x, int y, int width, int height) {
                header.setPreferredSize(new Dimension(width, height));
                super.reshape(x, y, width, height);
            }
        };
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());

        setLayout(new OverlayLayout(this));
        add(scroll);
    }

    private static class Header extends JTableHeader {
        Header(TableColumnModel model) { super(model); };
        public void processMouseEvent(MouseEvent e) { super.processMouseEvent(e); }
    }

}

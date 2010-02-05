/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeaderPanel extends JPanel {

    public HeaderPanel() {
        initComponents();
    }


    protected Object getRendererValue() { return null; }

    protected void setupRenderer(Component renderer) {}


    private void initComponents() {
        JTable impl = new JTable(new DefaultTableModel(new Object[] { "" }, 0)); // NOI18N

        final JTableHeader header = impl.getTableHeader();
        header.setResizingAllowed(false);
        header.setReorderingAllowed(false);

        final TableCellRenderer renderer = header.getDefaultRenderer();
        header.setDefaultRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component component = renderer.getTableCellRendererComponent(
                        table, getRendererValue(), false, false, row, 1);

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

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
    }

}

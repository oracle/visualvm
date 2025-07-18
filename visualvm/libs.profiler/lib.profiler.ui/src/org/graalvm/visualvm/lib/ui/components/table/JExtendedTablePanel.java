/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components.table;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import org.graalvm.visualvm.lib.ui.UIConstants;
import org.graalvm.visualvm.lib.ui.components.JExtendedTable;


/**
 * A subclass of JPanel that provides additional functionality for displaying JExtendedTable.
 * JExtendedTablePanel provides JScrollPane for displaying JExtendedTable and customized Viewport.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class JExtendedTablePanel extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //-----------------------------------------------------------------------
    // Custom ExtendedTable Viewport
    private class CustomExtendedTableViewport extends JViewport {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JTableHeader tableHeader;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        CustomExtendedTableViewport(JExtendedTable extendedTable) {
            super();
            setView(extendedTable);
            updateBackgroundColor();
            this.tableHeader = extendedTable.getTableHeader();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (UIConstants.SHOW_TABLE_VERTICAL_GRID) {
                paintVerticalLines(g);
            }
        }

        private int getEmptySpaceY() {
            if (getView() == null) {
                return 0;
            }

            return getView().getHeight();
        }

        private void paintVerticalLines(Graphics g) {
            int emptySpaceY = getEmptySpaceY();

            if (emptySpaceY > 0) {
                g.setColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);

                int cellX = 0;
                int cellWidth;

                for (int i = 0; i < extendedTable.getColumnModel().getColumnCount(); i++) {
                    cellWidth = extendedTable.getColumnModel().getColumn(i).getWidth();
                    g.drawLine((cellX + cellWidth) - 1, emptySpaceY, (cellX + cellWidth) - 1, getHeight() - 1);
                    cellX += cellWidth;
                }
            }
        }

        private void updateBackgroundColor() {
            setBackground(extendedTable.isEnabled() ? extendedTable.getBackground()
                                                    : UIManager.getColor("TextField.inactiveBackground")); // NOI18N
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    CustomExtendedTableViewport extendedTableViewport;
    protected JExtendedTable extendedTable;
    protected JScrollPane extendedTableScrollPane;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of JExtendedTablePanel */
    public JExtendedTablePanel(JExtendedTable extendedTable) {
        super(new BorderLayout());
        this.extendedTable = extendedTable;

        initComponents();
        hookHeaderColumnResize();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void clearBorders() {
        extendedTableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        extendedTableScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
    }
    
    public void setCorner(String key, java.awt.Component corner) {
        extendedTableScrollPane.setCorner(key, corner);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateVerticalScrollbar();
        extendedTable.setEnabled(enabled);
        extendedTableViewport.updateBackgroundColor();
    }

    public JScrollPane getScrollPane() {
        return extendedTableScrollPane;
    }

    private void hookHeaderColumnResize() {
        if (extendedTable.getTableHeader() != null) {
            extendedTable.getTableHeader().getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                    public void columnAdded(TableColumnModelEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnMoved(TableColumnModelEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnRemoved(TableColumnModelEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnMarginChanged(ChangeEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnSelectionChanged(ListSelectionEvent e) {
                    } // Ignored
                });

        }
    }

    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder());

        extendedTableScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        extendedTableViewport = new CustomExtendedTableViewport(extendedTable);
        extendedTableScrollPane.setViewport(extendedTableViewport);
        extendedTableScrollPane.addMouseWheelListener(extendedTable);
        // Enable vertical scrollbar only if needed
        JScrollBar vScrollbar = extendedTableScrollPane.getVerticalScrollBar();
        vScrollbar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateVerticalScrollbar();
            }
        });

        add(extendedTableScrollPane, BorderLayout.CENTER);
    }

    private void updateVerticalScrollbar() {
        JScrollBar vScrollbar = extendedTableScrollPane.getVerticalScrollBar();
        vScrollbar.setEnabled(JExtendedTablePanel.this.isEnabled() &&
                              vScrollbar.getVisibleAmount() < vScrollbar.getMaximum());
    }
    
}

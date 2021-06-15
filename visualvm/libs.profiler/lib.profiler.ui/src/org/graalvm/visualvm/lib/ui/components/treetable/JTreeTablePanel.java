/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components.treetable;

import java.awt.event.MouseWheelEvent;
import org.graalvm.visualvm.lib.ui.UIConstants;
import org.graalvm.visualvm.lib.ui.components.JTreeTable;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreePath;


/**
 * A subclass of JPanel that provides additional functionality for displaying JTreeTable.
 * JTreeTablePanel provides JScrollPane for displaying JTreeTable and JScrollBar for JTree
 * column of JTreeTable if necessary.
 *
 * @author Jiri Sedlacek
 */
public class JTreeTablePanel extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //-----------------------------------------------------------------------
    // Custom TreeTable Viewport
    private static class CustomTreeTableViewport extends JViewport {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JTableHeader tableHeader;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        CustomTreeTableViewport(JTreeTable treeTable) {
            super();
            setView(treeTable);
            setBackground(treeTable.getBackground());
            this.tableHeader = treeTable.getTableHeader();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (UIConstants.SHOW_TABLE_VERTICAL_GRID) {
                paintVerticalLines(g);
            }
        }

        private void paintVerticalLines(Graphics g) {
            Component view = getView();
            int linesTop = view == null ? 0 : view.getHeight();
            int linesBottom = getHeight() - 1;
            if (linesTop > 0 && linesTop <= linesBottom) {
                g.setColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
                int columnCount = tableHeader.getColumnModel().getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    Rectangle cellRect = tableHeader.getHeaderRect(i);
                    int cellX = cellRect.x + cellRect.width - 1;
                    g.drawLine(cellX, linesTop, cellX, linesBottom);
                }
            }
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    CustomTreeTableViewport treeTableViewport;
    protected JPanel scrollBarPanel;
    protected JScrollBar scrollBar;
    protected JScrollPane treeTableScrollPane;
    protected JTreeTable treeTable;

    private int invisibleRowsCount = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of JTreeTablePanel */
    public JTreeTablePanel(JTreeTable treeTable) {
        super(new BorderLayout());
        this.treeTable = treeTable;

        initComponents();
        hookHeaderColumnResize();
        hookScrollBarValueChange();
        hookTreeCollapsedExpanded();

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        updateScrollBar(true);
                    }
                }
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void clearBorders() {
        treeTableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        treeTableScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
    }
    
    public void setCorner(String key, java.awt.Component corner) {
        treeTableScrollPane.setCorner(key, corner);
    }

    public JScrollPane getScrollPane() {
        return treeTableScrollPane;
    }

    private void hookHeaderColumnResize() {
        treeTable.getTableHeader().getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) {
                treeTableViewport.repaint();
            }

            public void columnMoved(TableColumnModelEvent e) {
                treeTableViewport.repaint();
            }

            public void columnRemoved(TableColumnModelEvent e) {
                treeTableViewport.repaint();
            }

            public void columnMarginChanged(ChangeEvent e) {
                treeTableViewport.repaint();
                updateScrollBar(true);
            }

            public void columnSelectionChanged(ListSelectionEvent e) {}
        });
    }

    private void hookScrollBarValueChange() {
        scrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(final AdjustmentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        treeTable.setTreeCellOffsetX(e.getValue());
                        if (!e.getValueIsAdjusting()) updateScrollBar(false);
                    }
                });
            }
        });
    }

    private void hookTreeCollapsedExpanded() {
        treeTable.getTree().addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent event) {
                updateSB();
            }
            public void treeExpanded(TreeExpansionEvent event) {
                updateSB();
            }
            private void updateSB() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { updateScrollBar(false); }
                });
            }
        });
    }

    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder());

        treeTableScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        treeTableViewport = new CustomTreeTableViewport(treeTable);
        treeTableScrollPane.setViewport(treeTableViewport);
        // Enable vertical scrollbar only if needed
        final JScrollBar vScrollBar = treeTableScrollPane.getVerticalScrollBar();
        vScrollBar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Component c = treeTableViewport.getView();
                vScrollBar.setEnabled(JTreeTablePanel.this.isEnabled() &&
                vScrollBar.getVisibleAmount() < vScrollBar.getMaximum());
            }
        });
        vScrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!e.getValueIsAdjusting()) updateScrollBar(false);
            }
        });
        scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
        scrollBar.setUnitIncrement(10);
        scrollBarPanel = new JPanel(new BorderLayout());
        scrollBarPanel.add(scrollBar, BorderLayout.WEST);
        treeTable.setTreeCellOffsetX(0);
        scrollBarPanel.setVisible(false);
        scrollBar.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(scrollBar, e);
            }
        });
        
        MouseWheelListener[] listeners = treeTableScrollPane.getMouseWheelListeners();
        if (listeners != null && listeners.length == 1) {
            final MouseWheelListener listener = listeners[0];
            treeTableScrollPane.removeMouseWheelListener(listener);
            treeTableScrollPane.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (onlyShift(e) && treeTable.columnAtPoint(e.getPoint()) == 0) {
                        scroll(scrollBar, e);
                    } else {
                        listener.mouseWheelMoved(e);
                    }
                    treeTable.mouseWheelMoved(e);
                }
            });
        }

        add(treeTableScrollPane, BorderLayout.CENTER);
        add(scrollBarPanel, BorderLayout.SOUTH);
    }
    
    private static void scroll(final JScrollBar scroller, final MouseWheelEvent event) {
        if (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int unitsToScroll = event.getUnitsToScroll();
            if (unitsToScroll != 0) {
                int direction = unitsToScroll < 0 ? -1 : 1;
                int increment = scroller.getUnitIncrement(direction);
                int oldValue = scroller.getValue();
                int newValue = oldValue + increment * unitsToScroll;
                newValue = Math.max(Math.min(newValue, scroller.getMaximum() -
                           scroller.getVisibleAmount()), scroller.getMinimum());
                if (oldValue != newValue) scroller.setValue(newValue);
            }
            event.consume();
        }
    }
    
    private static boolean onlyShift(MouseEvent e) {
        return e.isShiftDown() && !(e.isAltDown() || e.isAltGraphDown() ||
                                    e.isControlDown() || e.isMetaDown());
    }

    private void updateScrollBar(boolean updateWidth) {
        if (!isShowing()) return;

        boolean refreshScrollBar = false;

        JTree tree = treeTable.getTree();
        Point viewPos = treeTableViewport.getViewPosition();
        int viewHeight = treeTableViewport.getHeight();
        TreePath firstVisiblePath = tree.getClosestPathForLocation(viewPos.x, viewPos.y);
        TreePath lastVisiblePath =  tree.getClosestPathForLocation(viewPos.x, viewPos.y + viewHeight - 1);
        int firstVisibleRow = tree.getRowForPath(firstVisiblePath);
        int lastVisibleRow = tree.getRowForPath(lastVisiblePath);

        if (firstVisibleRow < 0) return;

        Rectangle size = new Rectangle();
        for (int row = firstVisibleRow; row <= lastVisibleRow; row++)
            size.add(tree.getRowBounds(row));

        int treeWidth = size.width + 3; // +3 means extra right margin
        int columnWidth = treeTable.getColumnModel().getColumn(0).getWidth();
        int treeOffset = treeTable.getTreeCellOffsetX();
        int maximum = Math.max(treeWidth - columnWidth, treeOffset);

        if (scrollBarPanel.isVisible() && maximum <= 0) {
            int firstInvisibleRow = lastVisibleRow + 1;
            int lastInvisibleRow = Math.min(lastVisibleRow + getInvisibleRowsCount(),
                                            treeTable.getRowCount() - 1);
            if (firstInvisibleRow <= lastInvisibleRow) {
                size = new Rectangle();
                for (int row = firstInvisibleRow; row <= lastInvisibleRow; row++)
                    size.add(tree.getRowBounds(row));
                size.width += 3;
                int maximum2 = Math.max(size.width - columnWidth, treeOffset);
                if (maximum2 > 0) {
                    treeWidth = size.width;
                    maximum = maximum2;
                }
            }
        }

        if (maximum <= 0) {
            if (scrollBarPanel.isVisible()) {
                treeTable.setTreeCellOffsetX(0);
                scrollBarPanel.setVisible(false);
                refreshScrollBar = true;
            }
        } else {
            int value = treeOffset;
            int extent = treeWidth;
            if (!scrollBarPanel.isVisible()) {
                scrollBarPanel.setVisible(true);
                refreshScrollBar = true;
            }
            scrollBar.setValues(value, extent, 0, maximum + extent);
        }

        if (updateWidth) {
            Dimension dim = scrollBar.getPreferredSize();
            dim.width = treeTable.getColumnModel().getColumn(0).getWidth();
            scrollBar.setPreferredSize(dim);
            scrollBar.setBlockIncrement((int)((float)scrollBar.getModel().getExtent() * 0.95f));
            refreshScrollBar = true;
        }

        if (refreshScrollBar) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    scrollBar.invalidate();
                    validate();
                    repaint();
                }
            });
        }
    }

    private int getInvisibleRowsCount() {
        if (invisibleRowsCount == -1)
            invisibleRowsCount =
                    (int)Math.ceil((float)scrollBar.getPreferredSize().height /
                                   (float)treeTable.getRowHeight());

        return invisibleRowsCount;
    }
    
}

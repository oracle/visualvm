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

package com.sun.tools.visualvm.modules.tracer.impl.details;

import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderButton;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderPanel;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.TimelineSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class DetailsPanel extends JPanel {

    private JPanel noDataContainer;
    private JComponent dataContainer;

    private JScrollBar scrollBar;

    private DetailsTable table;
    private final TimelineSupport support;

    private boolean selectionAdjusting;
    private KeyStroke tableKeyStroke;


    public DetailsPanel(TimelineSupport support) {
        this.support = support;
        initComponents();
        initListeners();
    }


    public void setTableModel(TableModel model) {
        if (model == null) {
            table.clearSelection();
            table.setModel(new DefaultTableModel());
            removeAll();
            add(noDataContainer, BorderLayout.CENTER);
        } else {
            int selectedRow = getSelectedRow();
            table.setModel(model);
            if (selectedRow != -1)
                table.getSelectionModel().setSelectionInterval(selectedRow,
                                                               selectedRow);
            removeAll();
            add(dataContainer, BorderLayout.CENTER);
        }
        
        validate();
        repaint();
    }


    private void initListeners() {
        TableListener tableListener = new TableListener();
        table.getSelectionModel().addListSelectionListener(tableListener);
        table.addKeyListener(tableListener);
    }

    private int getSelectedRow() {
        int selectedRow = table.getSelectedRow();
        return selectedRow == -1 ? -1 : table.convertRowIndexToModel(selectedRow);
    }

    private boolean isTrackingEnd() {
        if (scrollBar == null) return false;
        return scrollBar.getValue() + scrollBar.getVisibleAmount() >= scrollBar.getMaximum();
    }

    private boolean isSelectionChanging() {
        if (selectionAdjusting) return true;
        if (tableKeyStroke == null) return false;
        return table.getActionForKeyStroke(tableKeyStroke) != null;
    }

    private void initComponents() {        
        table = new DetailsTable();
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JCheckBox checkBox = new JCheckBox();
        checkBox.setBorder(BorderFactory.createEmptyBorder());
        checkBox.setSize(checkBox.getMinimumSize());
        BufferedImage img = new BufferedImage(checkBox.getWidth(), checkBox.getHeight(), BufferedImage.TYPE_INT_ARGB);
        checkBox.print(img.getGraphics());
        final HeaderButton hb = new HeaderButton(null, new ImageIcon(img)) {
            protected void performAction(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        support.resetSelectedTimestamps();
                    }
                });
            }
        };
        hb.setToolTipText("Clear marked timestamps");
        
        final HeaderPanel corner = new HeaderPanel();

        JViewport viewport = new Viewport(table);

        final JScrollPane tableScroll = new JScrollPane(
                                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setViewport(viewport);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        tableScroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        
        support.addSelectionListener(new TimelineSupport.SelectionListener() {
            
            private boolean lastSelected = false;

            public void rowSelectionChanged(boolean rowsSelected) {}

            public void timeSelectionChanged(boolean timestampsSelected) {
                if (lastSelected == timestampsSelected) return;
                
                tableScroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER,
                                      timestampsSelected ? hb : corner);
                hb.reset();

                lastSelected = timestampsSelected;
            }
        });
        
        scrollBar = new JScrollBar(JScrollBar.VERTICAL) {
            public int getUnitIncrement(int direction) {
                JViewport vp = tableScroll.getViewport();
                Scrollable view = (Scrollable)(vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableUnitIncrement(vr, getOrientation(), direction);
            }
            public int getBlockIncrement(int direction) {
                JViewport vp = tableScroll.getViewport();
                Scrollable view = (Scrollable)(vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableBlockIncrement(vr, getOrientation(), direction);
            }
            public void setValues(int newValue, int newExtent, int newMin, int newMax) {
                setEnabled(newExtent < newMax);
                if (isEnabled() && !isSelectionChanging() && isTrackingEnd())
                    newValue = newMax - newExtent;
                super.setValues(newValue, newExtent, newMin, newMax);
            }
        };
        tableScroll.setVerticalScrollBar(scrollBar);
        dataContainer = tableScroll;

        JLabel noDataLabel = new JLabel("<No probe selected>", JLabel.CENTER);
        noDataLabel.setEnabled(false);
        noDataContainer = new JPanel(new BorderLayout());
        noDataContainer.setOpaque(false);
        noDataContainer.add(noDataLabel, BorderLayout.CENTER);

        setOpaque(false);
        setLayout(new BorderLayout());
        add(noDataContainer, BorderLayout.CENTER);
    }


    private class TableListener implements ListSelectionListener, KeyListener {
        public void valueChanged(ListSelectionEvent e) {
            selectionAdjusting = e.getValueIsAdjusting();
//            support.highlightTimestamp(getSelectedRow());
        }
        public void keyPressed(KeyEvent e) {
            tableKeyStroke = KeyStroke.getKeyStrokeForEvent(e);
        }
        public void keyReleased(KeyEvent e) {
            tableKeyStroke = null;
        }
        public void keyTyped(KeyEvent e) {}
    }


    private static class Viewport extends JViewport {
        
        private final DetailsTable view;
        private final Color background;

        Viewport(DetailsTable view) {
            super();

            setView(view);
            this.view = view;
            
            setOpaque(true);
            background = view.getBackground();
            setBackground(background);

            view.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                public void columnAdded(TableColumnModelEvent e) { repaint(); }
                public void columnMoved(TableColumnModelEvent e) { repaint(); }
                public void columnRemoved(TableColumnModelEvent e) { repaint(); }
                public void columnMarginChanged(ChangeEvent e) { repaint(); }
                public void columnSelectionChanged(ListSelectionEvent e) {}
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintVerticalLines(g);
        }

        private void paintVerticalLines(Graphics g) {
            int height = getHeight();
            int viewHeight = view.getHeight();
            if (viewHeight >= height) return;

            g.setColor(background);
            g.fillRect(0, viewHeight, getWidth(), getHeight() - viewHeight);

            int cellX = 0;
            int cellWidth;
            TableColumnModel model = view.getColumnModel();
            int columnCount = model.getColumnCount();
            
            g.setColor(DetailsTable.DEFAULT_GRID_COLOR);
            for (int i = 0; i < columnCount; i++) {
                cellWidth = model.getColumn(i).getWidth();
                cellX += cellWidth;
                g.drawLine(cellX - 1, viewHeight, cellX - 1, height);
            }
        }

    }

}

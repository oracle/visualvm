/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TimelinePanel extends JPanel {

    private final ChartPanel chartPanel;
    private final RowMouseHandler mouseHandler;


    // --- Constructor ---------------------------------------------------------
    
    public TimelinePanel(TimelineSupport support) {
        super(new BorderLayout());
        setOpaque(false);

        ProbesPanel probesPanel = new ProbesPanel(support);
        chartPanel = new ChartPanel(support.getChart(), support);

        add(probesPanel, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);

        new ProbesWheelHandler(chartPanel, probesPanel).register();
        mouseHandler = new RowMouseHandler(support, probesPanel);
        mouseHandler.register();
    }


    // --- Public interface ----------------------------------------------------

    public void reset() {
        chartPanel.resetChart();
        resetSelection();
    }

    public void resetSelection() {
        if (chartPanel.resetSelection()) mouseHandler.updateSelection();
    }

    public Action zoomInAction() {
        return chartPanel.zoomInAction();
    }

    public Action zoomOutAction() {
        return chartPanel.zoomOutAction();
    }

    public Action toggleViewAction() {
        return chartPanel.toggleViewAction();
    }

    public AbstractButton mouseZoom() {
        return chartPanel.mouseZoom();
    }

    public AbstractButton mouseHScroll() {
        return chartPanel.mouseHScroll();
    }

    public AbstractButton mouseVScroll() {
        return chartPanel.mouseVScroll();
    }


    // --- Private implementation ----------------------------------------------

    private static class ProbesWheelHandler implements MouseWheelListener {

        private final ChartPanel chartPanel;
        private final ProbesPanel probesPanel;

        ProbesWheelHandler(ChartPanel chartPanel, ProbesPanel probesPanel) {
            this.chartPanel = chartPanel;
            this.probesPanel = probesPanel;
        }

        void register() {
            probesPanel.addMouseWheelListener(this);
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            chartPanel.vScroll(e);
        }

    }

    private static class RowMouseHandler extends MouseAdapter {

        private static final int RESIZE_RANGE = 3;

        private final TimelineSupport support;
        private final TimelineChart chart;
        private final ProbesPanel probesPanel;

        private int baseY;
        private int baseHeight;
        private TimelineChart.Row draggingRow;


        RowMouseHandler(TimelineSupport support, ProbesPanel probesPanel) {
            this.support = support;
            this.chart = support.getChart();
            this.probesPanel = probesPanel;
        }


        void register() {
            chart.addMouseListener(this);
            chart.addMouseMotionListener(this);
            probesPanel.getMouseTarget().addMouseListener(this);
            probesPanel.getMouseTarget().addMouseMotionListener(this);
        }


        public void mousePressed(MouseEvent e) {
            updateRowState(e, SwingUtilities.isLeftMouseButton(e));
            if (draggingRow != null) chart.updateSelection(false, this);
            updateCursor();
        }

        public void mouseReleased(MouseEvent e) {
            if (draggingRow != null) chart.updateSelection(true, this);
            updateRowState(e, false);
            updateCursor();
        }

//        public void mouseClicked(MouseEvent e) {
//            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
//                if (e.getSource() == chart) ; // TODO: select row in Details
//        }

        public void mouseMoved(MouseEvent e) {
            updateRowState(e, false);
            updateCursor();
        }

        public void mouseDragged(MouseEvent e){
            if (draggingRow != null) {
                boolean checkStep = (e.getModifiers() & Toolkit.getDefaultToolkit().
                                     getMenuShortcutKeyMask()) == 0;
                chart.setRowHeight(draggingRow.getIndex(), baseHeight + e.getY() - baseY, checkStep);
            }
        }


        private void updateRowState(MouseEvent e, boolean updateSelection) {
            baseY = e.getY();
            draggingRow = chart.getNearestRow(baseY, RESIZE_RANGE, true);
            if (draggingRow != null) {
                baseHeight = draggingRow.getHeight();
            } else if (updateSelection) {
                TimelineChart.Row row = chart.getRowAt(baseY);
                if ((e.getModifiers() & Toolkit.getDefaultToolkit().
                        getMenuShortcutKeyMask()) == 0) {
                    if (chart.setSelectedRow(row)) updateSelection();
                } else {
                    if (row != null && chart.toggleRowSelection(row)) updateSelection();
                }
            }
        }

        void updateSelection() {
            probesPanel.updateSelection();
            support.rowSelectionChanged();
        }

        private void updateCursor() {
            if (draggingRow != null) {
                Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                chart.setCursor(resizeCursor);
                probesPanel.setCursor(resizeCursor);
            } else {
                Cursor defaultCursor = Cursor.getDefaultCursor();
                chart.setCursor(defaultCursor);
                probesPanel.setCursor(defaultCursor);
            }
        }

    }

}

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

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TimelinePanel extends JPanel {

    private final ChartPanel chartPanel;


    // --- Constructor ---------------------------------------------------------
    
    public TimelinePanel(TimelineSupport support) {
        super(new BorderLayout());
        setOpaque(false);

        ProbesPanel probesPanel = new ProbesPanel(support);
        chartPanel = new ChartPanel(support.getChart());

        add(probesPanel, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);

        new RowMouseHandler(support.getChart(), probesPanel.getMouseTarget(),
                            this).register();
    }


    // --- Public interface ----------------------------------------------------

    public void reset() {
        chartPanel.reset();
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

    private static class RowMouseHandler extends MouseAdapter {

        private static final int RESIZE_RANGE = 3;

        private final TimelineChart chart;
        private final Component mouseTarget;
        private final Component repaintTarget;

        private int baseY;
        private int baseHeight;
        private TimelineChart.Row draggingRow;


        RowMouseHandler(TimelineChart chart, Component mouseTarget,
                               Component repaintTarget) {
            this.chart = chart;
            this.mouseTarget = mouseTarget;
            this.repaintTarget = repaintTarget;
        }


        void register() {
            chart.addMouseListener(this);
            chart.addMouseMotionListener(this);
            mouseTarget.addMouseListener(this);
            mouseTarget.addMouseMotionListener(this);
        }


        public void mousePressed(MouseEvent e) {
            updateRowState(e, true);
            if (draggingRow != null) ChartPanel.disableSelection(chart);
            updateCursor();
        }

        public void mouseReleased(MouseEvent e) {
            if (draggingRow != null) ChartPanel.enableSelection(chart);
            updateRowState(e, false);
            updateCursor();
        }

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
//                TimelineChart.Row row = chart.getRowAt(baseY);
//                chart.setSelectedRow(row == null ? -1 : row.getIndex());
//                repaintTarget.repaint();
            }
        }

        private void updateCursor() {
            if (draggingRow != null) {
                Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                chart.setCursor(resizeCursor);
                mouseTarget.setCursor(resizeCursor);
            } else {
                Cursor defaultCursor = Cursor.getDefaultCursor();
                chart.setCursor(defaultCursor);
                mouseTarget.setCursor(defaultCursor);
            }
        }

    }

}

/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JLabel;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineUnitsOverlay extends ChartOverlay {

    private static final Color UNITS_LEGEND_FOREGROUND = new Color(100, 100, 100);
    private static final Color UNITS_LEGEND_BACKGROUND = new Color(255, 255, 255);

    private final TimelineChart chart;
    private final JLabel painter;

    private Model model;


    TimelineUnitsOverlay(TimelineChart chart) {
        this.chart = chart;
        painter = new JLabel();
        painter.setFont(smallerFont(painter.getFont()));
    }


    void setupModel(Model model) {
        this.model = model;
    }


    private boolean hasValues() {
        return ((SynchronousXYItemsModel)chart.getItemsModel()).getTimeline().
                getTimestampsCount() > 0;
    }

    private void setupPainter(String text) {
        painter.setText(text);
        painter.setSize(painter.getPreferredSize());
    }


    public void paint(Graphics g) {
        if (model == null || !hasValues()) return;

        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineChart.Row row = chart.getRow(i);
            ChartContext rowContext = row.getContext();
            
            int x = Utils.checkedInt(rowContext.getViewportOffsetX());
            int y = Utils.checkedInt(rowContext.getViewportOffsetY());
            int w = Utils.checkedInt(rowContext.getViewportWidth());
            int h = Utils.checkedInt(rowContext.getViewportHeight());

            setupPainter(model.getMaxUnits(row));
            int xx = w - painter.getWidth() - 2;
            int yy = y + 1;
            paint(g, xx, yy, UNITS_LEGEND_BACKGROUND);
            paint(g, xx, --yy, UNITS_LEGEND_FOREGROUND);

            setupPainter(model.getMinUnits(row));
            xx = w - painter.getWidth() - 2;
            yy = y + h - painter.getHeight();
            paint(g, xx, yy, UNITS_LEGEND_BACKGROUND);
            paint(g, xx, --yy, UNITS_LEGEND_FOREGROUND);
        }
    }

    private void paint(Graphics g, int x, int y, Color color) {
        painter.setForeground(color);
        g.translate(x, y);
        painter.paint(g);
        g.translate(-x, -y);
    }


    private static Font smallerFont(Font font) {
        return new Font(font.getName(), font.getStyle(), font.getSize() - 2);
    }


    static interface Model {

        public String getMinUnits(TimelineChart.Row row);
        public String getMaxUnits(TimelineChart.Row row);

    }

}

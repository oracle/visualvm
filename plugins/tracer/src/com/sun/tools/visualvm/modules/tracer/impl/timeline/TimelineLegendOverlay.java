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

import com.sun.tools.visualvm.modules.tracer.impl.swing.ColorIcon;
import com.sun.tools.visualvm.modules.tracer.impl.swing.LegendFont;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JLabel;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineLegendOverlay extends ChartOverlay {

    private final TimelineChart chart;
    private final JLabel painter;


    TimelineLegendOverlay(TimelineChart chart) {
        this.chart = chart;

        painter = new JLabel();
        painter.setFont(new LegendFont());

        int size = painter.getFont().getSize() - 3;
        ColorIcon.setup(size, size,
                LegendFont.FOREGROUND_COLOR, LegendFont.BACKGROUND_COLOR);
    }


    private void setupPainter(String text, Color color) {
        painter.setText(text);
        painter.setIcon(ColorIcon.fromColor(color));
        painter.setSize(painter.getPreferredSize());
    }


    public void paint(Graphics g) {
        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineChart.Row row = chart.getRow(i);
            ChartContext rowContext = row.getContext();
            SynchronousXYItem[] rowItems = row.getItems();

            int x = 3;
            int y = -1;

            for (SynchronousXYItem rowItem : rowItems) {
                TimelineXYPainter itemPainter =
                        (TimelineXYPainter)chart.getPaintersModel().getPainter(rowItem);
                if (itemPainter.isPainting()) {
                    setupPainter(rowItem.getName(), itemColor(itemPainter));
                    if (y == -1)
                        y = Utils.checkedInt(rowContext.getViewportOffsetY()) +
                            Utils.checkedInt(rowContext.getViewportHeight()) -
                            painter.getHeight() - 1;
                    paint(g, x, y);
                    x += painter.getWidth() + 10;
                }
            }
        }
    }

    private void paint(Graphics g, int x, int y) {
        g.translate(x, y + 1);
        painter.setForeground(LegendFont.BACKGROUND_COLOR);
        painter.paint(g);
        g.translate(0, -1);
        painter.setForeground(LegendFont.FOREGROUND_COLOR);
        painter.setIcon(ColorIcon.BOTTOM_SHADOW);
        painter.paint(g);
        g.translate(-x, -y);
    }


    private static Color itemColor(TimelineXYPainter painter) {
        Color color = painter.lineColor;
        if (color == null) color = painter.fillColor;
        return color;
    }

}

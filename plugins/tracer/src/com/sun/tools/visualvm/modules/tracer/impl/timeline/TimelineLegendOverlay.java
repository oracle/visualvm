/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.impl.swing.ColorIcon;
import com.sun.tools.visualvm.modules.tracer.impl.swing.LabelRenderer;
import com.sun.tools.visualvm.modules.tracer.impl.swing.LegendFont;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
    private final LabelRenderer painter;


    TimelineLegendOverlay(TimelineChart chart) {
        this.chart = chart;

        painter = new LabelRenderer();
        painter.setFont(new LegendFont());

        int size = painter.getFont().getSize() - 3;
        ColorIcon.setup(size, size,
                LegendFont.FOREGROUND_COLOR, LegendFont.BACKGROUND_COLOR);
    }


    private void setupPainter(String text, Color color) {
        painter.setText(text);
        painter.setIcon(ColorIcon.fromColor(color));
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
                    setupPainter(rowItem.getName(), itemPainter.getDefiningColor());
                    Dimension pd = painter.getPreferredSize();
                    if (y == -1)
                        y = Utils.checkedInt(rowContext.getViewportOffsetY()) +
                            rowContext.getViewportHeight() - pd.height - 1;
                    paint(g, x, y);
                    x += pd.width + 10;
                }
            }
        }
    }

    private void paint(Graphics g, int x, int y) {
        painter.setLocation(x, y + 1);
        painter.setForeground(LegendFont.BACKGROUND_COLOR);
        painter.paint(g);

        painter.setLocation(x, y);
        painter.setForeground(LegendFont.FOREGROUND_COLOR);
        painter.setIcon(ColorIcon.BOTTOM_SHADOW);
        painter.paint(g);
    }

    // --- Peformance tweaks ---------------------------------------------------

    public void invalidate() {}

    public void update(Graphics g) {}

}

/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartDecorator;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class RowForegroundDecorator implements ChartDecorator {

    private static final Color SELECTED_FILTER = new Color(0, 0, 200, 25);
    private static final float[] FRACTIONS = new float[] { 0.0f, 0.49f, 0.51f, 1.0f };
    private static final Color[] COLORS = !UIUtils.isDarkResultsBackground() ?
        new Color[] { new Color(250, 251, 252, 120),
                      new Color(237, 240, 242, 120),
                      new Color(229, 233, 236, 125),
                      new Color(215, 221, 226, 130) } :
        new Color[] { new Color(050, 051, 052, 110),
                      new Color(037, 040, 042, 110),
                      new Color(29, 033, 036, 115),
                      new Color(015, 021, 026, 120) };

    private final TimelineChart chart;
    private final boolean gradient;
    private final boolean selection;


    RowForegroundDecorator(TimelineChart chart, boolean gradient, boolean selection) {
        this.chart = chart;
        this.gradient = gradient;
        this.selection = selection;
    }


    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        if (gradient || selection) {
            int rowsCount = chart.getRowsCount();
            for (int i = 0; i < rowsCount; i++) {
                TimelineChart.Row row = chart.getRow(i);
                ChartContext rowContext = row.getContext();

                int y = Utils.checkedInt(rowContext.getViewportOffsetY());
                int h = Utils.checkedInt(rowContext.getViewportHeight() - 1);

                if (gradient) {
                    g.setPaint(new LinearGradientPaint(0, y, 0, y + h, FRACTIONS, COLORS));
                    g.fillRect(0, y, chart.getWidth(), h);
                }

                if (selection && chart.isRowSelected(row)) {
                    g.setColor(SELECTED_FILTER);
                    g.fillRect(0, y, chart.getWidth(), h);
                }
            }
        }
    }

}

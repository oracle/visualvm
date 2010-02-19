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
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartDecorator;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
final class RowGradientDecorator implements ChartDecorator {

    private static final Color SELECTED_FILTER = new Color(0, 0, 200, 50);
    private static final float[] FRACTIONS = new float[] { 0.1f, 0.5f, 0.55f, 0.8f };
    private static final Color[] COLORS = new Color[] { new Color(250, 250, 250, 110),
                                                        new Color(205, 205, 220, 30),
                                                        new Color(180, 180, 195, 30),
                                                        new Color(200, 200, 210, 110) };

    private final TimelineChart chart;


    RowGradientDecorator(TimelineChart chart) {
        this.chart = chart;
    }


    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineChart.Row row = chart.getRow(i);
            ChartContext rowContext = row.getContext();

            int y = Utils.checkedInt(rowContext.getViewportOffsetY());
            int h = Utils.checkedInt(rowContext.getViewportHeight() - 1);

            g.setPaint(new LinearGradientPaint(0, y, 0, y + h, FRACTIONS, COLORS));
            g.fillRect(0, y, chart.getWidth(), h);

            if (chart.getSelectedRow() == i) {
                g.setColor(SELECTED_FILTER);
                g.fillRect(0, y, chart.getWidth(), h);
            }
        }
    }

}

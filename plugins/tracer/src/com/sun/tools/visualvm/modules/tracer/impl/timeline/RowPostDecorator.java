/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
import java.awt.Point;
import java.awt.Rectangle;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartDecorator;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class RowPostDecorator implements ChartDecorator {

    private final TimelineChart chart;


    public RowPostDecorator(TimelineChart chart) {
        this.chart = chart;
    }


    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineChart.Row row = chart.getRow(i);
            ChartContext rowContext = row.getContext();

            Point start = new Point(0, Utils.checkedInt(rowContext.getViewportOffsetY()) + 1);
            Point end = new Point(0, Utils.checkedInt(rowContext.getViewportOffsetY()) + rowContext.getViewportHeight() - 2);
            float[] fractions = new float[] { 0.1f, 0.5f, 0.55f, 0.8f };
            Color[] colors = new Color[] { new Color(250, 250, 250, 110), new Color(205, 205, 220, 30), new Color(180, 180, 195, 30), new Color(200, 200, 210, 110) };
            
            Rectangle rowBounds = new Rectangle(0, Utils.checkedInt(rowContext.getViewportOffsetY()) + 1,
                                                chart.getWidth(), rowContext.getViewportHeight() - 2);
            g.setPaint(new LinearGradientPaint(start, end, fractions, colors));
            g.fill(rowBounds);

            if (chart.getSelectedRow() == i) {
                g.setPaint(new Color(0, 0, 200, 50));
                g.fill(rowBounds);
            }
        }
    }

}

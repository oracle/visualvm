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

package org.graalvm.visualvm.modules.tracer.impl.timeline;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartDecorator;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
final class RowBackgroundDecorator implements ChartDecorator {

    private static final Color BACKGROUND = new Color(228, 228, 248);

    private final TimelineChart chart;


    RowBackgroundDecorator(TimelineChart chart) {
        this.chart = chart;
    }


    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineChart.Row row = chart.getRow(i);
            ChartContext rowContext = row.getContext();

            int y = Utils.checkedInt(rowContext.getViewportOffsetY());
            int h = Utils.checkedInt(rowContext.getViewportHeight() - 1);

            if (chart.isRowSelected(row)) {
                    g.setColor(BACKGROUND);
                    g.fillRect(0, y, chart.getWidth(), h);
                }
        }
    }

}

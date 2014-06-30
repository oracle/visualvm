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

package com.sun.tools.visualvm.charts.xy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Iterator;
import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.axis.AxisComponent;
import org.netbeans.lib.profiler.charts.axis.AxisMark;
import org.netbeans.lib.profiler.charts.axis.AxisMarksComputer;
import org.netbeans.lib.profiler.charts.axis.AxisMarksPainter;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class XYAxisComponent extends AxisComponent {
    
    private static final Color VERTICAL_MESH_COLOR = Utils.checkedColor(
                                                        new Color(80, 80, 80, 50));
    private static final Stroke VERTICAL_MESH_STROKE = new BasicStroke(1,
                                                        BasicStroke.CAP_SQUARE,
                                                        BasicStroke.JOIN_BEVEL, 0,
                                                        new float[] {0, 2}, 0);
    private static final Stroke VERTICAL_MESH_STROKE_PERF = new BasicStroke(1,
                                                        BasicStroke.CAP_SQUARE,
                                                        BasicStroke.JOIN_BEVEL);

    private static final int AXIS_BASIS_EXTENT = 2;
    private static final Color AXIS_LINE_COLOR = new Color(90, 90, 90);

    private static boolean WORKAROUND_OPENJDK_BUG = false;


    private final ChartComponent chart;
    private final AxisMarksComputer marksComputer;


    public XYAxisComponent(ChartComponent chart, AxisMarksComputer marksComputer,
                         AxisMarksPainter marksPainter, int location, int mesh) {
        
        super(chart, marksComputer, marksPainter, location, mesh);

        this.chart = chart;
        this.marksComputer = marksComputer;

        setForeground(AXIS_LINE_COLOR);
    }


    protected int getAxisBasisExtent() { return AXIS_BASIS_EXTENT; }

    protected void paintVerticalMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
        Iterator<AxisMark> marks = marksComputer.marksIterator(
                                 chartMask.y, chartMask.y + chartMask.height);

        if (WORKAROUND_OPENJDK_BUG) return;

        g.setPaint(VERTICAL_MESH_COLOR);
        g.setStroke(Utils.forceSpeed() ? VERTICAL_MESH_STROKE_PERF :
                                         VERTICAL_MESH_STROKE);
        
        int x1 = chartMask.x;
        int x2 = x1 + chartMask.width;
        
        // #VISUALVM-595 correctly align the origin to have stable offset for dotted stroke
        if (x1 % 2 != chart.getOffsetX() % 2) {
            x1 -= 1;
            x2 += 2;
        }

        while (marks.hasNext()) {
            AxisMark mark = marks.next();
            int y = mark.getPosition();

            try {
                // Workaround for a bug on OpenJDK - when tooltip is displayed
                // over the chart on mouseMove 'java.lang.ArithmeticException: / by zero'
                // at 'sun.java2d.pisces.Dasher.lineTo' exception is thrown.
                g.drawLine(x1, y, x2, y);
            } catch (ArithmeticException e) {
                WORKAROUND_OPENJDK_BUG = true;
                System.err.println("'java.lang.ArithmeticException: / by zero' detected in XYAxisComponent.paintVerticalMesh, applying workaround"); // NOI18N
                break;
            }
        }
    }
    
}

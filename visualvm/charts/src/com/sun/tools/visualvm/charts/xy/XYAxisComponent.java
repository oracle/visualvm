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

    private static final int AXIS_BASIS_EXTENT = 2;
    private static final Color AXIS_LINE_COLOR = new Color(90, 90, 90);


    private final AxisMarksComputer marksComputer;


    public XYAxisComponent(ChartComponent chart, AxisMarksComputer marksComputer,
                         AxisMarksPainter marksPainter, int location, int mesh) {
        
        super(chart, marksComputer, marksPainter, location, mesh);

        this.marksComputer = marksComputer;

        setForeground(AXIS_LINE_COLOR);
    }


    protected int getAxisBasisExtent() { return AXIS_BASIS_EXTENT; }

    protected void paintVerticalMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
        Iterator<AxisMark> marks = marksComputer.marksIterator(
                                 chartMask.y, chartMask.y + chartMask.height);

        while (marks.hasNext()) {
            AxisMark mark = marks.next();
            int y = mark.getPosition();

            g.setPaint(VERTICAL_MESH_COLOR);
            g.setStroke(VERTICAL_MESH_STROKE);
            g.drawLine(chartMask.x, y, chartMask.x + chartMask.width, y);
        }
    }

}

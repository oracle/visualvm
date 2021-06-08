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

package org.graalvm.visualvm.modules.tracer.impl.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYChartContext;

/**
 *
 * @author Jiri Sedlacek
 */
final class ContinuousXYPainter extends TimelineXYPainter {

    private static final Polygon POLYGON = new Polygon();
    
    protected final int lineWidth;
    protected final Color lineColor;
    protected final Color fillColor;
    protected final Color definingColor;

    protected final Stroke lineStroke;

    private final PointsComputer computer;


    ContinuousXYPainter(float lineWidth, Color lineColor, Color fillColor,
                        double dataFactor, PointsComputer computer) {

        super((int)Math.ceil(lineWidth), fillColor != null, dataFactor);

        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("lineColor or fillColor must not be null"); // NOI18N

        this.lineWidth = (int)Math.ceil(lineWidth);
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);

        definingColor = lineColor != null ? lineColor : fillColor;

        this.lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);

        this.computer = computer;
    }


    protected Color getDefiningColor() {
        return definingColor;
    }

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                         List<ItemSelection> selected, Graphics2D g,
                         Rectangle dirtyArea, SynchronousXYChartContext context) {

        int valuesCount = item.getValuesCount();
        int extraTrailing = fillColor != null ? 2 : 0;

        Rectangle dirtyExt = new Rectangle(dirtyArea);
        dirtyExt.x -= lineWidth;
        dirtyExt.width += lineWidth * 2;

        int[][] idxs = computer.getVisible(dirtyExt, valuesCount, context, 1,
                                           extraTrailing);
        if (idxs == null) return;
        int[] visibleIndexes = idxs[0];
        int npoints = idxs[1][0];
        int[][] points = computer.createPoints(visibleIndexes, npoints, item,
                                               dataFactor, context);
        
        if (fillColor != null) {
            points[0][npoints - 2] = points[0][npoints - 3];
            points[1][npoints - 2] = computer.getZeroY(context);
            points[0][npoints - 1] = points[0][0];
            points[1][npoints - 1] = points[1][npoints - 2];

            POLYGON.xpoints = points[0];
            POLYGON.ypoints = points[1];
            POLYGON.npoints = npoints;

            g.setPaint(fillColor);
            g.fill(POLYGON);
        }

        if (lineColor != null) {
            g.setPaint(lineColor);
            g.setStroke(lineStroke);
            g.drawPolyline(points[0], points[1], npoints - extraTrailing);
        }
    }

}

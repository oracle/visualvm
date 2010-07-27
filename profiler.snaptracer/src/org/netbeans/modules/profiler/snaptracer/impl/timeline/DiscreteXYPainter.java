/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;

/**
 *
 * @author Jiri Sedlacek
 */
final class DiscreteXYPainter extends TimelineXYPainter {

    private static final Polygon POLYGON = new Polygon();

    private static final int[] x3arr = new int[3];
    private static final int[] y3arr = new int[3];
    private static final int[] x4arr = new int[4];
    private static final int[] y4arr = new int[4];

    protected final int lineWidth;
    protected final Color lineColor;
    protected final Color fillColor;
    protected final Color definingColor;

    protected final Stroke lineStroke;

    protected final int width;
    protected final boolean fixedWidth;
    protected final boolean topLineOnly;
    protected final boolean outlineOnly;

    private final PointsComputer computer;


    DiscreteXYPainter(float lineWidth, Color lineColor, Color fillColor,
                      int width, boolean fixedWidth, boolean topLineOnly,
                      boolean outlineOnly, double dataFactor, PointsComputer computer) {

        super((int)Math.ceil(lineWidth), fillColor != null ||
              (!topLineOnly && !outlineOnly), dataFactor);
        
        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("lineColor or fillColor must not be null"); // NOI18N

        this.lineWidth = (int)Math.ceil(lineWidth);
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);

        definingColor = lineColor != null ? lineColor : fillColor;

        this.lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);

        this.width = width;
        this.fixedWidth = fixedWidth;
        this.topLineOnly = topLineOnly;
        this.outlineOnly = outlineOnly;

        this.computer = computer;
    }

    
    protected Color getDefiningColor() {
        return definingColor;
    }

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                         List<ItemSelection> selected, Graphics2D g,
                         Rectangle dirtyArea, SynchronousXYChartContext context) {

        int zeroY = 0;
        int zeroYLimit = 0;
        if (fillColor != null || !topLineOnly) {
            zeroY = Utils.checkedInt(context.getViewY(context.getDataOffsetY()));
            zeroY = Math.max(Utils.checkedInt(context.getViewportOffsetY()), zeroY);
            zeroY = Math.min(Utils.checkedInt(context.getViewportOffsetY() +
                                              context.getViewportHeight()), zeroY);
            zeroYLimit = zeroY - lineWidth + 1;
        }

        int outlineWidth = lineColor == null || topLineOnly || outlineOnly ? 0 :
                           Math.max(1, (width > 0 ? 2 : 1) * lineWidth - 1);
        int valuesCount = item.getValuesCount();

        if (width > 0 || lineColor == null || topLineOnly) {

            int[][] idxs = computer.getVisible(dirtyArea, valuesCount, context,
                                               1, 0);
            if (idxs == null) return;
            int[] visibleIndexes = idxs[0];
            int npoints = idxs[1][0];
            int[][] points = computer.createPoints(visibleIndexes, npoints, item,
                                                   dataFactor, context);
            int[] xpoints = points[0];
            int[] ypoints = points[1];
            

            int startX = xpoints[0];
            int stopX;
            int currentX = startX;
            int nextX = 0;
            int itemsOffset = fixedWidth ? 0 : width;
            
            for (int i = 0; i < npoints; i++) {
                int height = zeroY - ypoints[i];

                if (fixedWidth) {
                    startX = Math.max(startX, currentX - width / 2);
                    nextX = i == npoints - 1 ? xpoints[i] : xpoints[i + 1];
                    stopX = currentX + Math.min(width / 2, (nextX - currentX) / 2);
                } else {
                    nextX = i == npoints - 1 ? xpoints[i] : xpoints[i + 1];
                    int diff = nextX - currentX;
                    itemsOffset = Math.min(diff, width + 1);
                    stopX = currentX + (diff - itemsOffset) / 2;
                }

                int segmentWidth = stopX - startX;

                if (fillColor != null && segmentWidth >= outlineWidth) {
                    g.setColor(fillColor);
                    g.fillRect(startX, zeroY - height, segmentWidth + 1, height);
                }
                if (lineColor != null) {
                    g.setColor(lineColor);
                    g.setStroke(lineStroke);

                    if (topLineOnly) {
                        g.drawLine(startX, zeroY - height, Math.max(startX + 1, stopX), zeroY - height);
                    } else if (segmentWidth + 1 <= lineWidth) {
                        g.drawLine(startX, zeroY - height, startX, zeroYLimit);
                    } else {
                        int[] xx;
                        int[] yy;

                        if (i == 0) {
                            xx = xArr(startX, stopX, stopX);
                            yy = yArr(zeroY - height, zeroY - height, zeroYLimit);
                        } else if (i == npoints - 1) {
                            xx = xArr(startX, startX, stopX);
                            yy = yArr(zeroYLimit, zeroY - height, zeroY - height);
                        } else {
                            xx = xArr(startX, startX, stopX, stopX);
                            yy = yArr(zeroYLimit, zeroY - height, zeroY - height, zeroYLimit);
                        }

                        g.drawPolyline(xx, yy, xx.length);
                    }
                }
                
                currentX = nextX;
                startX = stopX + itemsOffset;
            }

        } else {

            int extraPoints = fillColor != null ? 2 : 0;
            int[][] idxs = computer.getVisible(dirtyArea, valuesCount, context,
                                               2, extraPoints);
            if (idxs == null) return;
            int[] visibleIndexes = idxs[0];
            int npoints = idxs[1][0];
            int[][] points = computer.createPoints(visibleIndexes, npoints, item,
                                                   dataFactor, context);
            int[] xpoints = points[0];
            int[] ypoints = points[1];
            int npointse = npoints;
            npoints -= extraPoints;
            
            int index = 1;
            int lastX = xpoints[0];

            while (index < npoints - 2) {
                int currentX = xpoints[index + 1];
                currentX -= (currentX - lastX) / 2;
                xpoints[index] = currentX;
                lastX = xpoints[index + 1];
                xpoints[index + 1] = currentX;
                index += 2;
            }

            if (fillColor != null) {
                xpoints[npointse - 2] = xpoints[npointse - 3];
                ypoints[npointse - 2] = zeroY;
                xpoints[npointse - 1] = xpoints[0];
                ypoints[npointse - 1] = ypoints[npointse - 2];

                POLYGON.xpoints = xpoints;
                POLYGON.ypoints = ypoints;
                POLYGON.npoints = npointse;

                g.setPaint(fillColor);
                g.fill(POLYGON);
            }

            g.setColor(lineColor);
            g.setStroke(lineStroke);
            g.drawPolyline(xpoints, ypoints, npoints);

            if (!outlineOnly) {
                g.setColor(lineColor);
                g.setStroke(lineStroke);

                int i = 1;
                while (i < npoints - 1) {
                    int y = ypoints[i] + lineWidth / 2;
                    if (y < zeroYLimit) g.drawLine(xpoints[i], y, xpoints[i], zeroYLimit);
                    i += 3;
                    if (i >= npoints - 1) break;
                    y = ypoints[i] + lineWidth / 2;
                    if (y < zeroYLimit) g.drawLine(xpoints[i], y, xpoints[i], zeroYLimit);
                    i++;
                }
            }

        }
    }
    
    private static int[] xArr(int... vals) {
        if (vals.length == 3) {
            x3arr[0] = vals[0];
            x3arr[1] = vals[1];
            x3arr[2] = vals[2];
            return x3arr;
        } else {
            x4arr[0] = vals[0];
            x4arr[1] = vals[1];
            x4arr[2] = vals[2];
            x4arr[3] = vals[3];
            return x4arr;
        }
    }

    private static int[] yArr(int... vals) {
        if (vals.length == 3) {
            y3arr[0] = vals[0];
            y3arr[1] = vals[1];
            y3arr[2] = vals[2];
            return y3arr;
        } else {
            y4arr[0] = vals[0];
            y4arr[1] = vals[1];
            y4arr[2] = vals[2];
            y4arr[3] = vals[3];
            return y4arr;
        }
    }


}

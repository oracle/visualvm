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

import org.netbeans.lib.profiler.charts.ItemSelection;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineXYPainter extends SynchronousXYItemPainter {

    private static final int HOVER_RADIUS = 2; // Not used

    private final Color fillColor2;
    private boolean painting;


    // --- Constructor ---------------------------------------------------------

    public static TimelineXYPainter absolutePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor1,
                                                       Color fillColor2) {

        return new TimelineXYPainter(lineWidth, lineColor, fillColor1, fillColor2,
                                         TYPE_ABSOLUTE, 0);
    }

    public static TimelineXYPainter relativePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor1,
                                                       Color fillColor2,
                                                       int maxOffset) {

        return new TimelineXYPainter(lineWidth, lineColor, fillColor1, fillColor2,
                                         TYPE_RELATIVE, maxOffset);
    }


    public TimelineXYPainter(float lineWidth, Color lineColor, Color fillColor1,
                     Color fillColor2, int type, int maxValueOffset) {

        super(lineWidth, lineColor, fillColor1, type, maxValueOffset);
        this.fillColor2 = Utils.checkedColor(fillColor2);
        painting = true;
    }


    // --- Public interface ----------------------------------------------------

    public void setPainting(boolean painting) {
        this.painting = painting;
    }

    public boolean isPainting() {
        return painting;
    }


    // --- ItemPainter implementation ------------------------------------------

    public boolean supportsHovering(ChartItem item) {
        return true;
    }

    public boolean supportsSelecting(ChartItem item) {
        return true;
    }

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {

        XYItemSelection sel = (XYItemSelection)selection;
        XYItem item  = sel.getItem();
        int selectedValueIndex = sel.getValueIndex();

        if (selectedValueIndex == -1 ||
            selectedValueIndex >= item.getValuesCount())
            // This happens on reset - bounds of the selection are unknown, let's clear whole area
            return new LongRect(0, 0, context.getViewportWidth(),
                                context.getViewportHeight());
        else
            return getViewBounds(item, new int[] { sel.getValueIndex() }, context);
    }

    public XYItemSelection getClosestSelection(ChartItem item, int viewX,
                                               int viewY, ChartContext context) {

        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return null; // item not visible

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                                           ItemSelection.DISTANCE_UNKNOWN);
    }

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, SynchronousXYChartContext context) {

        if (!isPainting()) return;
        if (item.getValuesCount() < 2) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        int[][] points = createPoints(item, dirtyArea, context, type, maxValueOffset);
        if (points == null) return;

        int[] xPoints  = points[0];
        int[] yPoints  = points[1];
        int npoints = xPoints.length;

        if (fillColor != null) {
            int zeroY = Utils.checkedInt(context.getViewY(context.getDataOffsetY()));
            zeroY = Math.max(Utils.checkedInt(context.getViewportOffsetY()), zeroY);
            zeroY = Math.min(Utils.checkedInt(context.getViewportOffsetY() +
                                                      context.getViewportHeight()), zeroY);

            Polygon polygon = new Polygon();
            polygon.xpoints = xPoints;
            polygon.ypoints = yPoints;
            polygon.npoints = npoints;
            polygon.xpoints[npoints - 2] = xPoints[npoints - 3];
            polygon.ypoints[npoints - 2] = zeroY;
            polygon.xpoints[npoints - 1] = xPoints[0];
            polygon.ypoints[npoints - 1] = zeroY;
            
            if (fillColor2 == null || Utils.forceSpeed()) g.setPaint(fillColor);
            else g.setPaint(new GradientPaint(0, context.getViewportOffsetY(),
                           fillColor, 0, context.getViewportOffsetY() +
                           context.getViewportHeight(), fillColor2));
            g.fill(polygon);
        }

        if (lineColor != null) {
            g.setPaint(lineColor);
            g.setStroke(lineStroke);
            g.drawPolyline(xPoints, yPoints, npoints - 2);
        }

    }

    private static int[][] createPoints(XYItem item, Rectangle dirtyArea,
                                 SynchronousXYChartContext context,
                                 int type, int maxValueOffset) {

        int valuesCount = item.getValuesCount();
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);

        int firstFirst = visibleBounds[0][0];
        int firstIndex = firstFirst;
        if (firstIndex == -1) firstIndex = visibleBounds[0][1];
        if (firstIndex == -1) return null;
        if (firstFirst != -1 && firstIndex > 0) firstIndex -= 1;

        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = valuesCount - 1;
        if (lastFirst != -1 && lastIndex < valuesCount - 1) lastIndex += 1;

        int itemsStep = (int)(valuesCount / context.getViewWidth());
        if (itemsStep == 0) itemsStep = 1;

        int visibleCount = lastIndex - firstIndex + 1;

        if (itemsStep > 1) {
            int firstMod = firstIndex % itemsStep;
            firstIndex -= firstMod;
            int lastMod = lastIndex % itemsStep;
            lastIndex = lastIndex - lastMod + itemsStep;
            visibleCount = (lastIndex - firstIndex) / itemsStep + 1;
            lastIndex = Math.min(lastIndex, valuesCount - 1);
        }

        int[] xPoints = new int[visibleCount + 2];
        int[] yPoints = new int[visibleCount + 2];


        double itemValueFactor = type == TYPE_RELATIVE ? getItemValueFactor(context,
                                 maxValueOffset, item.getBounds().height) : 0;

        for (int i = 0; i < visibleCount; i++) {
            int dataIndex = i == visibleCount - 1 ? lastIndex :
                                 firstIndex + i * itemsStep;
            xPoints[i] = Utils.checkedInt(Math.ceil(
                         context.getViewX(item.getXValue(dataIndex))));
            yPoints[i] = Utils.checkedInt(Math.ceil(
                         getYValue(item, dataIndex,
                         type, context, itemValueFactor)));
        }

        return new int[][] { xPoints, yPoints };
    }

    private LongRect getViewBoundsRelative(LongRect dataBounds, XYItem item,
                                           ChartContext context) {
        LongRect itemBounds = item.getBounds();

        double itemValueFactor = getItemValueFactor(context, maxValueOffset,
                                                    itemBounds.height);

        // TODO: fix the math!!!
        double value1 = context.getDataOffsetY() + itemValueFactor *
                      (double)(dataBounds.y - itemBounds.y);
        double value2 = context.getDataOffsetY() + itemValueFactor *
                      (double)(dataBounds.y + dataBounds.height - itemBounds.y);

        long viewX = (long)Math.ceil(context.getViewX(dataBounds.x));
        long viewWidth = (long)Math.ceil(context.getViewWidth(dataBounds.width));
        if (context.isRightBased()) viewX -= viewWidth;

        long viewY1 = (long)Math.ceil(context.getViewY(value1));
        long viewY2 = (long)Math.ceil(context.getViewY(value2));
        long viewHeight = context.isBottomBased() ? viewY1 - viewY2 :
                                                    viewY2 - viewY1;
        if (!context.isBottomBased()) viewY2 -= viewHeight;

        LongRect viewBounds =  new LongRect(viewX, viewY2, viewWidth, viewHeight);
        LongRect.addBorder(viewBounds, HOVER_RADIUS);

        return viewBounds;
    }

    private LongRect getViewBounds(XYItem item, int[] valuesIndexes, ChartContext context) {

        LongRect dataBounds = new LongRect();

        if (valuesIndexes == null) {
            LongRect.set(dataBounds, item.getBounds());
        } else {
            boolean firstPoint = true;
            for (int valueIndex : valuesIndexes) {
                if (valueIndex == -1) continue;
                long xValue = item.getXValue(valueIndex);
                long yValue = item.getYValue(valueIndex);
                if (firstPoint) {
                    LongRect.set(dataBounds, xValue, yValue, 0, 0);
                    firstPoint = false;
                } else {
                    LongRect.add(dataBounds, xValue, yValue);
                }
            }
        }

        if (type == TYPE_RELATIVE) {

            return getViewBoundsRelative(dataBounds, item, context);

        } else {

            LongRect viewBounds = context.getViewRect(dataBounds);
            LongRect.addBorder(viewBounds, HOVER_RADIUS);
            return viewBounds;

        }
    }

    private static double getItemValueFactor(ChartContext context,
                                             double maxValueOffset,
                                             double itemHeight) {
        return ((double)context.getDataHeight() -
               context.getDataHeight(maxValueOffset)) / itemHeight;
    }

    private static double getYValue(XYItem item, int valueIndex,
                                  int type, ChartContext context, double itemValueFactor) {
        if (type == TYPE_ABSOLUTE) {
            return context.getViewY(item.getYValue(valueIndex));
        } else {
            return context.getViewY(context.getDataOffsetY() + (itemValueFactor *
                        (item.getYValue(valueIndex) - item.getBounds().y)));
        }
    }

}

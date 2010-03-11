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

import java.awt.BasicStroke;
import org.netbeans.lib.profiler.charts.ItemSelection;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineXYPainter extends XYItemPainter.Abstract {

    private static final int HOVER_RADIUS = 2; // Not used

    private final Color fillColor2;
    private boolean painting;

    protected final int lineWidth;
    protected final Color lineColor;
    protected final Color fillColor;

    protected final Stroke lineStroke;

    protected final double dataFactor;


    // --- Constructor ---------------------------------------------------------

    public TimelineXYPainter(float lineWidth, Color lineColor, Color fillColor,
                             Color fillColor2, double dataFactor) {

        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("No parameters defined"); // NOI18N

        this.lineWidth = (int)Math.ceil(lineWidth);
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);
        this.fillColor2 = Utils.forceSpeed() ? null : Utils.checkedColor(fillColor2);

        this.lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);

        this.dataFactor = dataFactor;

        painting = true;
    }


    // --- Public interface ----------------------------------------------------

    void setPainting(boolean painting) {
        this.painting = painting;
    }

    boolean isPainting() {
        return painting;
    }


    // --- ItemPainter implementation ------------------------------------------

    public LongRect getItemBounds(ChartItem item) {
        XYItem xyItem = (XYItem)item;
        return getDataBounds(xyItem.getBounds());
    }

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
        XYItem xyItem = (XYItem)item;
        return getViewBounds(xyItem.getBounds(), context);
    }


    public boolean isBoundsChange(ChartItemChange itemChange) {
        XYItemChange change = (XYItemChange)itemChange;
        return !LongRect.equals(change.getOldValuesBounds(),
                                change.getNewValuesBounds());
    }

    public boolean isAppearanceChange(ChartItemChange itemChange) {
        XYItemChange change = (XYItemChange)itemChange;
        LongRect dirtyBounds = change.getDirtyValuesBounds();
        return dirtyBounds.width != 0 || dirtyBounds.height != 0;
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
        XYItemChange change = (XYItemChange)itemChange;
        return getViewBounds(change.getDirtyValuesBounds(), context);
    }


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

    public void paintItem(ChartItem item, List<ItemSelection> highlighted,
                          List<ItemSelection> selected, Graphics2D g,
                          Rectangle dirtyArea, ChartContext context) {

        paint((XYItem)item, highlighted, selected, g, dirtyArea,
              (SynchronousXYChartContext)context);
    }


    // --- XYItemPainter implementation ----------------------------------------

    public double getItemView(double dataY, XYItem item, ChartContext context) {
        return context.getViewY(dataY * dataFactor);
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        return context.getDataY(viewY / dataFactor);
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        double scale = context.getViewHeight(dataFactor);
        if (scale <= 0) scale = -1;
        return scale;
    }


    // --- Private implementation ----------------------------------------------

    private LongRect getDataBounds(LongRect itemBounds) {
        LongRect bounds = new LongRect(itemBounds);
        bounds.y *= dataFactor;
        bounds.height *= dataFactor;

        if (fillColor != null) {
            bounds.height += bounds.y;
            bounds.y = 0;
        }

        return bounds;
    }

    private LongRect getViewBounds(LongRect itemBounds, ChartContext context) {
        LongRect dataBounds = getDataBounds(itemBounds);

        LongRect viewBounds = context.getViewRect(dataBounds);
        LongRect.addBorder(viewBounds, lineWidth);

        return viewBounds;
    }

    private LongRect getViewBounds(XYItem item, int[] valuesIndexes, ChartContext context) {

        LongRect dataBounds = new LongRect();

        if (valuesIndexes == null) {
            LongRect.set(dataBounds, item.getBounds());
            dataBounds.y *= dataFactor;
            dataBounds.height *= dataFactor;
        } else {
            boolean firstPoint = true;
            for (int valueIndex : valuesIndexes) {
                if (valueIndex == -1) continue;
                long xValue = item.getXValue(valueIndex);
                long yValue = (long)(item.getYValue(valueIndex) * dataFactor);
                if (firstPoint) {
                    LongRect.set(dataBounds, xValue, yValue, 0, 0);
                    firstPoint = false;
                } else {
                    LongRect.add(dataBounds, xValue, yValue);
                }
            }
        }

        LongRect viewBounds = context.getViewRect(dataBounds);
        LongRect.addBorder(viewBounds, HOVER_RADIUS);
        return viewBounds;
    }


    protected void paint(XYItem item, List<ItemSelection> highlighted,
                         List<ItemSelection> selected, Graphics2D g,
                         Rectangle dirtyArea, SynchronousXYChartContext context) {

        if (!painting) return;
        if (item.getValuesCount() < 2) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        int[][] points = createPoints(item, dirtyArea, context, dataFactor);
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
            g.setPaint(fillColor);
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
                                        double dataFactor) {

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

        int itemsStep = (int)Math.ceil(valuesCount / context.getViewWidth());
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


        for (int i = 0; i < visibleCount; i++) {
            int dataIndex = i == visibleCount - 1 ? lastIndex :
                                 firstIndex + i * itemsStep;
            xPoints[i] = Utils.checkedInt(Math.ceil(
                         context.getViewX(item.getXValue(dataIndex))));
            yPoints[i] = Utils.checkedInt(Math.ceil(
                         getYValue(item, dataIndex,
                         context, dataFactor)));
        }

        return new int[][] { xPoints, yPoints };
    }

    private static double getYValue(XYItem item, int valueIndex,
                                    ChartContext context, double dataFactor) {

        return context.getViewY(item.getYValue(valueIndex) * dataFactor);
    }

}

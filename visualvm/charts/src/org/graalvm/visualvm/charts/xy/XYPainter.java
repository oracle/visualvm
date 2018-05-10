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
public class XYPainter extends SynchronousXYItemPainter {
    
    private final int mode;

    private final Color fillColor2;
    private boolean painting;

    
    // --- Initializer ---------------------------------------------------------
    
    {
        String _mode = System.getProperty("visualvm.charts.defaultMode", "minmax").toLowerCase(); // NOI18N
        if ("fast".equals(_mode)) { // NOI18N
            mode = 0;
        } else {
            mode = 1;
        }
    }

    // --- Constructor ---------------------------------------------------------

    public static XYPainter absolutePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor1,
                                                       Color fillColor2) {

        return new XYPainter(lineWidth, lineColor, fillColor1, fillColor2,
                                         TYPE_ABSOLUTE, 0);
    }

    public static XYPainter relativePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor1,
                                                       Color fillColor2,
                                                       int maxOffset) {

        return new XYPainter(lineWidth, lineColor, fillColor1, fillColor2,
                                         TYPE_RELATIVE, maxOffset);
    }


    public XYPainter(float lineWidth, Color lineColor, Color fillColor1,
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

        if (mode == 1) return getMinMaxClosestSelection(item, viewX, viewY, context);
        else if (mode == 0) return getFastClosestSelection(item, viewX, viewY, context);
        else return null;
    }
    
    private int[][] getPoints(XYItem item, Rectangle dirtyArea,
                              SynchronousXYChartContext context,
                              int type, int maxValueOffset) {
        
        if (mode == 1) return getMinMaxPoints(item, dirtyArea, context, type, maxValueOffset);
        else if (mode == 0) return getFastPoints(item, dirtyArea, context, type, maxValueOffset);
        else return null;
    }

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, SynchronousXYChartContext context) {
        
        if (!isPainting()) return;
        if (item.getValuesCount() < 2) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        int[][] points = getPoints(item, dirtyArea, context, type, maxValueOffset);
        if (points == null) return;

        int[] xPoints  = points[0];
        int[] yPoints  = points[1];
        int npoints = points[2][0];
        
        if (fillColor != null) {
            int zeroY = Utils.checkedInt(context.getViewY(context.getDataOffsetY()));
            zeroY = Math.max(Utils.checkedInt(context.getViewportOffsetY()), zeroY);
            zeroY = Math.min(Utils.checkedInt(context.getViewportOffsetY() +
                                                      context.getViewportHeight()), zeroY);

            Polygon polygon = new Polygon();
            polygon.xpoints = xPoints;
            polygon.ypoints = yPoints;
            polygon.npoints = npoints + 2;
            polygon.xpoints[npoints] = xPoints[npoints - 1];
            polygon.ypoints[npoints] = zeroY;
            polygon.xpoints[npoints + 1] = xPoints[0];
            polygon.ypoints[npoints + 1] = zeroY;
            
            if (fillColor2 == null || Utils.forceSpeed()) g.setPaint(fillColor);
            else g.setPaint(new GradientPaint(0, context.getViewportOffsetY(),
                           fillColor, 0, context.getViewportOffsetY() +
                           context.getViewportHeight(), fillColor2));
            g.fill(polygon);
        }

        if (lineColor != null) {
            g.setPaint(lineColor);
            g.setStroke(lineStroke);
            g.drawPolyline(xPoints, yPoints, npoints);
        }

    }
    
    
    private XYItemSelection getFastClosestSelection(ChartItem item, int viewX,
                                                    int viewY, ChartContext context) {

        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return null; // item not visible

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                                           ItemSelection.DISTANCE_UNKNOWN);
    }
    
    private XYItemSelection getMinMaxClosestSelection(ChartItem item, int viewX,
                                                      int viewY, ChartContext context) {

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        if (xyItem.getValuesCount() == 0) return null;
        
        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;
        Rectangle bounds = new Rectangle(0, 0, contx.getViewportWidth(), contx.getViewportHeight());
        if (bounds.isEmpty()) return null;
        
        int[][] visibleBounds = contx.getVisibleBounds(bounds);

        int firstVisible = visibleBounds[0][0];
        if (firstVisible == -1) firstVisible = visibleBounds[0][1];
        if (firstVisible == -1) return null;

        int lastVisible = visibleBounds[1][0];
        if (lastVisible == -1) lastVisible = visibleBounds[1][1];
        if (lastVisible == -1) lastVisible = xyItem.getValuesCount() - 1;
        
        int idx = firstVisible;
        int x = getViewX(contx, xyItem, idx);
        int dist = Math.abs(viewX - x);
        
        while (++idx <= lastVisible) {
            int newX = getViewX(contx, xyItem, idx);
            int newDist = Math.abs(viewX - newX);
            if (newDist > dist) {
                idx--;
                break;
            } else {
                x = newX;
                dist = newDist;
            }
        }
        
        if (idx > lastVisible) idx = lastVisible;
        
        long maxVal = xyItem.getYValue(idx);
        int maxIdx = idx;
        
        while (--idx >= firstVisible && getViewX(contx, xyItem, idx) == x) {
            long y = xyItem.getYValue(idx);
            if (y > maxVal) {
                maxVal = y;
                maxIdx = idx;
            }
        }
        
        return new XYItemSelection.Default(xyItem, maxIdx, dist);
    }
    
    private int[][] getFastPoints(XYItem item, Rectangle dirtyArea,
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

        return new int[][] { xPoints, yPoints, { xPoints.length - 2 } };
    }
    
    private int[][] getMinMaxPoints(XYItem item, Rectangle dirtyArea,
                                    SynchronousXYChartContext context,
                                    int type, int maxValueOffset) {
        
        if (dirtyArea.isEmpty()) return null;
        
        dirtyArea.grow(lineWidth, lineWidth);
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);
        
        int firstFirst = visibleBounds[0][0];
        int firstIndex = firstFirst;
        if (firstIndex == -1) firstIndex = visibleBounds[0][1];
        if (firstIndex == -1) return null;
        
        int valuesCount = item.getValuesCount();
        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = valuesCount - 1;
        
        int firstX = getViewX(context, item, firstIndex);
        while (firstIndex > 0 && getViewX(context, item, firstIndex) >= firstX - lineWidth)
            firstIndex--;
        
        int lastX = getViewX(context, item, lastIndex);
        while (lastIndex < valuesCount - 1 && getViewX(context, item, lastIndex) <= lastX + lineWidth)
            lastIndex++;
        
        double itemValueFactor = type == TYPE_RELATIVE ? getItemValueFactor(context,
                                 maxValueOffset, item.getBounds().height) : 0;
        
        int maxPoints = Math.max(dirtyArea.width, (lastIndex - firstIndex + 1) * 3);
        
        int[] xPoints = new int[maxPoints + 2];
        int[] yPoints = new int[maxPoints + 2];
        
        int nPoints = 0;
        for (int index = firstIndex; index <= lastIndex; index++) {
            int x = getViewX(context, item, index);
            int y = Utils.checkedInt(Math.ceil(getYValue(item, index,
                                     type, context, itemValueFactor)));
            
            if (nPoints == 0) { // First point
                xPoints[nPoints] = x;
                yPoints[nPoints] = y;
                nPoints++;
            } else { // Other than first point
                int x_1 = xPoints[nPoints - 1];
                
                if (x_1 != x) { // New point
                    xPoints[nPoints] = x;
                    yPoints[nPoints] = y;
                    nPoints++;
                } else { // Existing point
                    int y_1 = yPoints[nPoints - 1];
                    
                    if (nPoints > 1 && xPoints[nPoints - 2] == x_1) { // Existing point with two values
                        int y_2 = yPoints[nPoints - 2];
                        
                        int minY = Math.min(y, y_1);
                        int maxY = Math.max(y, y_2);
                        
                        yPoints[nPoints - 3] = minY;
                        yPoints[nPoints - 2] = maxY;
                        yPoints[nPoints - 1] = minY;
                    } else { // Existing point with one value
                        if (y_1 != y) { // Creating second value
                            int minY = Math.min(y, y_1);
                            int maxY = Math.max(y, y_1);
                            
                            yPoints[nPoints - 1] = minY;
                            
                            xPoints[nPoints] = x;
                            yPoints[nPoints] = maxY;
                            nPoints++;
                            
                            xPoints[nPoints] = x;
                            yPoints[nPoints] = minY;
                            nPoints++;
                        }
                    }
                }
            }
        }
        
        return new int[][] { xPoints, yPoints, { nPoints } };
    }
    
    private static int getViewX(SynchronousXYChartContext context, XYItem item, int index) {
        return Utils.checkedInt(Math.ceil(context.getViewX(item.getXValue(index))));
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
        LongRect.addBorder(viewBounds, lineWidth);

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
            LongRect.addBorder(viewBounds, lineWidth);
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

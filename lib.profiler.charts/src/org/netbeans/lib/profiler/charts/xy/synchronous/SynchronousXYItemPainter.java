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

package org.netbeans.lib.profiler.charts.xy.synchronous;

import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import org.netbeans.lib.profiler.charts.xy.XYItem;

/**
 *
 * @author Jiri Sedlacek
 */
public class SynchronousXYItemPainter extends XYItemPainter.Abstract {

    protected final int lineWidth;
    protected final Color lineColor;
    protected final Color fillColor;

    protected final Stroke lineStroke;

    protected final int type;
    protected final int maxValueOffset;


    // --- Constructor ---------------------------------------------------------

    public static SynchronousXYItemPainter absolutePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor) {
        
        return new SynchronousXYItemPainter(lineWidth, lineColor, fillColor,
                                         TYPE_ABSOLUTE, 0);
    }

    public static SynchronousXYItemPainter relativePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor,
                                                       int maxOffset) {

        return new SynchronousXYItemPainter(lineWidth, lineColor, fillColor,
                                         TYPE_RELATIVE, maxOffset);
    }


    public SynchronousXYItemPainter(float lineWidth, Color lineColor, Color fillColor,
                          int type, int maxValueOffset) {

        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("No parameters defined"); // NOI18N

        this.lineWidth = (int)Math.ceil(lineWidth);
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);

        this.lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);

        this.type = type;
        this.maxValueOffset = maxValueOffset;
    }


    // --- ItemPainter implementation ------------------------------------------
    
    public LongRect getItemBounds(ChartItem item) {
//        if (!(item instanceof XYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        XYItem xyItem = (XYItem)item;
        if (type == TYPE_ABSOLUTE) {
            return getDataBounds(xyItem.getBounds());
        } else {
            LongRect itemBounds1 = new LongRect(xyItem.getBounds());
            itemBounds1.y = 0;
            itemBounds1.height = 0;
            return itemBounds1;
        }
    }

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
//        if (!(item instanceof XYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        XYItem xyItem = (XYItem)item;
        if (type == TYPE_ABSOLUTE) {
            return getViewBounds(xyItem.getBounds(), context);
        } else {
            return getViewBoundsRelative(xyItem.getBounds(), xyItem, context);
        }
    }


    public boolean isBoundsChange(ChartItemChange itemChange) {
//        if (!(itemChange instanceof XYItemChange))
//            throw new UnsupportedOperationException("Unsupported itemChange: " + itemChange);

        // Items can only be added => always bounds change
        XYItemChange change = (XYItemChange)itemChange;
        return !LongRect.equals(change.getOldValuesBounds(),
                                change.getNewValuesBounds());
    }

    public boolean isAppearanceChange(ChartItemChange itemChange) {
//        if (!(itemChange instanceof XYItemChange))
//            throw new UnsupportedOperationException("Unsupported itemChange: " + itemChange);
        
        // Items can only be added => always appearance change
        XYItemChange change = (XYItemChange)itemChange;
        LongRect dirtyBounds = change.getDirtyValuesBounds();
        return dirtyBounds.width != 0 || dirtyBounds.height != 0;
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
//        if (!(itemChange instanceof XYItemChange))
//            throw new UnsupportedOperationException("Unsupported itemChange: " + itemChange);
        
        // Items can only be added => always dirty bounds for last value
        XYItemChange change = (XYItemChange)itemChange;
        if (type == TYPE_ABSOLUTE) {

            return getViewBounds(change.getDirtyValuesBounds(), context);
        } else {
            LongRect oldValuesBounds = change.getOldValuesBounds();
            LongRect newValuesBounds = change.getNewValuesBounds();
            if (oldValuesBounds.y != newValuesBounds.y ||
                oldValuesBounds.height != newValuesBounds.height) {

                return getItemBounds(change.getItem(), context);
            } else {
                return getViewBoundsRelative(change.getDirtyValuesBounds(),
                                             change.getItem(), context);
            }
//            return new LongRect(0, 0, context.getViewportWidth(), context.getViewportHeight());
        }
//        return new LongRect(0, 0, context.getViewportWidth(), context.getViewportHeight());
    }


    public double getItemView(double dataY, XYItem item, ChartContext context) {
        if (type == TYPE_ABSOLUTE) {
            return super.getItemView(dataY, item, context);
        } else {
            double itemValueFactor = getItemValueFactor(context,
                                     maxValueOffset, item.getBounds().height);
            return context.getViewY(context.getDataOffsetY() + (itemValueFactor * dataY));
        }
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        if (type == TYPE_ABSOLUTE) {
            return super.getItemValue(viewY, item, context);
        } else {
            double itemValueFactor = getItemValueFactor(context,
                                     maxValueOffset, item.getBounds().height);
            return context.getDataY(viewY) / itemValueFactor;
        }
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        if (type == TYPE_ABSOLUTE) {
            return super.getItemValueScale(item, context);
        } else {
            long itemHeight = item.getBounds().height;
            if (itemHeight == 0) return 1;
            double itemValueFactor = getItemValueFactor(context,
                                     maxValueOffset, itemHeight);
            return itemValueFactor / context.getDataHeight(1d);
        }
    }


    public boolean supportsHovering(ChartItem item) {
        return false;
    }

    public boolean supportsSelecting(ChartItem item) {
        return false;
    }

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {
        throw new UnsupportedOperationException("getSelectionBounds() not supported"); // NOI18N
    }

    public ItemSelection getClosestSelection(ChartItem item, int viewX,
                                             int viewY, ChartContext context) {
        return null;
    }

    public void paintItem(ChartItem item, List<ItemSelection> highlighted,
                          List<ItemSelection> selected, Graphics2D g,
                          Rectangle dirtyArea, ChartContext context) {
//        if (!(item instanceof XYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N
//        if (!(context instanceof ProfilerXYChartComponent.Context))
//            throw new UnsupportedOperationException("Unsupported context: " + context);
        
        paint((XYItem)item, highlighted, selected, g, dirtyArea,
              (SynchronousXYChartContext)context);
    }


    // --- Private implementation ----------------------------------------------

    private LongRect getDataBounds(LongRect itemBounds) {
        LongRect bounds = new LongRect(itemBounds);

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

    private LongRect getViewBoundsRelative(LongRect dataBounds, XYItem item,
                                           ChartContext context) {
        LongRect itemBounds = item.getBounds();

        double itemValueFactor = getItemValueFactor(context,
                                 maxValueOffset, itemBounds.height);

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

    
    protected void paint(XYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, SynchronousXYChartContext context) {

        if (item.getValuesCount() < 2) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        int[][] points = createPoints(item, dirtyArea, context, type, maxValueOffset);
        if (points == null) return;

        int[] xPoints  = points[0];
        int[] yPoints  = points[1];
        int npoints = xPoints.length;

//long start = System.nanoTime();
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
//System.err.println(">>> Paint: " + (System.nanoTime() - start) / 1000 + " [ms], dirtyArea: " + dirtyArea);
//        if (type == TYPE_RELATIVE) {
//        g.setColor(Color.RED);
//        Rectangle bbox = new Rectangle(dirtyArea);
////        bbox.width -= 1;
////        bbox.height -= 1;
//            g.draw(bbox);
////            System.err.println(">>> Here");
//        }

//        if (type == TYPE_RELATIVE_BOUNDED) {
//            System.err.println(">>> paintItem, dirtyArea: " + dirtyArea);
//        }
        
    }

    private static int[][] createPoints(XYItem item, Rectangle dirtyArea,
                                 SynchronousXYChartContext context,
                                 int type, int maxValueOffset) {

        int valuesCount = item.getValuesCount();
//        long st = System.currentTimeMillis();
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);
//        System.err.println(">>> Create points: " + (System.currentTimeMillis() - st));

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

//        System.err.println(">>> First: " + firstIndex + ", last: " + lastIndex);
//        if (firstIndex > lastIndex) {
//            System.err.println(">>> First: " + firstIndex + ", last: " + lastIndex);
//            Thread.dumpStack();
//        }

        int itemsStep = (int)Math.ceil(valuesCount / context.getViewWidth());
        if (itemsStep == 0) itemsStep = 1;

        int visibleCount = lastIndex - firstIndex + 1;
//        if (visibleCount + 2 < 0) System.err.println(">>> Negative, first: " + Arrays.toString(visibleBounds[0]) + ", last: " + Arrays.toString(visibleBounds[1]));
//        System.err.println(">>> first: " + Arrays.toString(visibleBounds[0]) + ", last: " + Arrays.toString(visibleBounds[1]));

        if (itemsStep > 1) {
            int firstMod = firstIndex % itemsStep;
            firstIndex -= firstMod;
            int lastMod = lastIndex % itemsStep;
            lastIndex = lastIndex - lastMod + itemsStep;
            visibleCount = (lastIndex - firstIndex) / itemsStep + 1;
            lastIndex = Math.min(lastIndex, valuesCount - 1);
        }

//        if (visibleCount + 2 < 0) System.err.println(">>> Negative, first: " + firstIndex + ", last: " + lastIndex);

        int[] xPoints = new int[visibleCount + 2];
        int[] yPoints = new int[visibleCount + 2];


        double itemValueFactor = type == TYPE_RELATIVE ? getItemValueFactor(context,
                                 maxValueOffset, item.getBounds().height) : 0;
//        System.err.println(">>> Painting: " + visibleCount);
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

    private static double getYValue(XYItem item, int valueIndex,
                                  int type, ChartContext context, double itemValueFactor) {
        if (type == TYPE_ABSOLUTE) {
            return context.getViewY(item.getYValue(valueIndex));
        } else {
            return context.getViewY(context.getDataOffsetY() + (itemValueFactor *
                        (item.getYValue(valueIndex) - item.getBounds().y)));
        }
    }

    private static double getItemValueFactor(ChartContext context,
                                             double maxValueOffset,
                                             double itemHeight) {
        return ((double)context.getDataHeight() -
               context.getDataHeight(maxValueOffset)) / itemHeight;
    }

}

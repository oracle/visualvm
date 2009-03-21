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

package org.netbeans.lib.profiler.ui.charts.xy;

import org.netbeans.lib.profiler.charts.Utils;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.LongRect;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYItemMarker extends XYItemPainter.Abstract {

    private static final int TYPE_ABSOLUTE = 0;
    private static final int TYPE_RELATIVE = 1;

    private static final int ITEM_MARK_RADIUS = 7;

    private final int lineWidth;
    private final Color lineColor;
    private final Color fillColor;

    private final Stroke lineStroke;

    private final int decorationRadius;

    private final int type;
    private final int maxOffset;


    // --- Constructor ---------------------------------------------------------

    public static ProfilerXYItemMarker absolutePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor) {

        return new ProfilerXYItemMarker(lineWidth, lineColor, fillColor,
                                         TYPE_ABSOLUTE, 0);
    }

    public static ProfilerXYItemMarker relativePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor,
                                                       int maxOffset) {

        return new ProfilerXYItemMarker(lineWidth, lineColor, fillColor,
                                         TYPE_RELATIVE, maxOffset);
    }


    private ProfilerXYItemMarker(float lineWidth, Color lineColor, Color fillColor,
                                 int type, int maxOffset) {

        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("No parameters defined"); // NOI18N

        this.lineWidth = (int)Math.ceil(lineWidth);
        this.lineColor = Utils.checkedColor(lineColor);
        this.fillColor = Utils.checkedColor(fillColor);

        this.lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);

        decorationRadius = ITEM_MARK_RADIUS + this.lineWidth;

        this.type = type;
        this.maxOffset = maxOffset;
    }


    // --- ItemPainter implementation ------------------------------------------

    public Color getItemColor(XYItem item) {
        return lineColor != null ? lineColor : fillColor;
    }
    
    public LongRect getItemBounds(ChartItem item) {
//        if (!(item instanceof ProfilerXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        ProfilerXYItem xyItem = (ProfilerXYItem)item;
        if (type == TYPE_ABSOLUTE) {
            return xyItem.getBounds();
        } else {
            LongRect itemBounds1 = new LongRect(xyItem.getBounds());
            itemBounds1.y = 0;
            itemBounds1.height = 0;
            return itemBounds1;
        }
    }

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
//        if (!(item instanceof ProfilerXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        ProfilerXYItem xyItem = (ProfilerXYItem)item;
        return getViewBounds(xyItem, null, context);
    }


    public boolean isBoundsChange(ChartItemChange itemChange) {
//        if (!(itemChange instanceof XYItemChange))
//            throw new UnsupportedOperationException("Unsupported itemChange: " + itemChange);

        // Items can only be added => always bounds change
        XYItemChange change = (XYItemChange)itemChange;
        return !LongRect.equals(change.getOldValuesBounds(),
                                change.getNewValuesBounds());
    }

    public boolean isAppearanceChange(ChartItemChange itemChange, ChartContext context) {
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
        return getViewBounds(change.getItem(), change.getValuesIndexes(), context);
    }


    public boolean supportsHovering(ChartItem item) {
        return true;
    }

    public boolean supportsSelecting(ChartItem item) {
        return true;
    }

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {
//        if (!(selection instanceof XYItemSelection))
//            throw new UnsupportedOperationException("Unsupported selection: " + selection); // NOI18N

        XYItemSelection sel = (XYItemSelection)selection;
        return getViewBounds(sel.getItem(), new int[] { sel.getValueIndex() }, context);
    }

    public XYItemSelection getClosestSelection(ChartItem item, int viewX,
                                                       int viewY, ChartContext context) {
//        if (!(item instanceof ProfilerXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N
//        if (!(context instanceof ProfilerXYChartComponent.Context))
//            throw new UnsupportedOperationException("Unsupported context: " + context);

        ProfilerXYChart.Context contx = (ProfilerXYChart.Context)context;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return null; // item not visible

        ProfilerXYItem xyItem = (ProfilerXYItem)item;
        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                                           ItemSelection.DISTANCE_UNKNOWN);
    }

    public void paintItem(ChartItem item, List<ItemSelection> highlighted,
                          List<ItemSelection> selected, Graphics2D g,
                          Rectangle dirtyArea, ChartContext context) {
//        if (!(item instanceof ProfilerXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N
//        if (!(context instanceof ProfilerXYChartComponent.Context))
//            throw new UnsupportedOperationException("Unsupported context: " + context);
        
        paint((ProfilerXYItem)item, highlighted, selected, g, dirtyArea,
              (ProfilerXYChart.Context)context);
    }


    // --- Private implementation ----------------------------------------------

    private static LongRect getRelativeDataBounds(LongRect dataBounds, XYItem item,
                                                  ChartContext context, int maxOffset) {
        LongRect relativeBounds = new LongRect(dataBounds);
        LongRect itemBounds = item.getBounds();

        double itemValueFactor = (double)(context.getDataHeight() -
                                 context.getDataHeight(maxOffset)) /
                                 (double)(itemBounds.height);
        // TODO: fix the math, no need to compute the value2 - height is enough
        long value1 = context.getDataOffsetY() + (long)(itemValueFactor *
                      (double)(relativeBounds.y - itemBounds.y));
        long value2 = context.getDataOffsetY() + (long)(itemValueFactor *
                      (double)((relativeBounds.y + relativeBounds.height)
                      - itemBounds.y));

        relativeBounds.y = value1;
        relativeBounds.height = value2 - value1;

        return relativeBounds;
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

        if (type == TYPE_RELATIVE)
            LongRect.set(dataBounds, getRelativeDataBounds(dataBounds, item,
                                                           context, maxOffset));

        LongRect viewBounds = context.getViewRect(dataBounds);
        LongRect.addBorder(viewBounds, decorationRadius);

        return viewBounds;
    }

    
    private void paint(ProfilerXYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, ProfilerXYChart.Context context) {

        if (highlighted.isEmpty()) return;

        double itemValueFactor = type == TYPE_RELATIVE ?
                                         (double)(context.getDataHeight() -
                                          context.getDataHeight(maxOffset)) /
                                         (double)(item.getBounds().height) : 0;

        for (ItemSelection selection : highlighted) {

            XYItemSelection sel = (XYItemSelection)selection;
            int valueIndex = sel.getValueIndex();
            if (valueIndex == -1) continue;

            int itemX = ChartContext.getCheckedIntValue(context.getViewX(
                                                        item.getXValue(valueIndex)));
            int itemY = ChartContext.getCheckedIntValue(getYValue(item, valueIndex,
                                                        type, context, itemValueFactor));

            if (fillColor != null) {
                g.setPaint(fillColor);
                g.fillOval(itemX - ITEM_MARK_RADIUS, itemY - ITEM_MARK_RADIUS,
                           ITEM_MARK_RADIUS * 2, ITEM_MARK_RADIUS * 2);
            }

            if (lineColor != null) {
                g.setPaint(lineColor);
                g.setStroke(lineStroke);
                g.drawOval(itemX - ITEM_MARK_RADIUS, itemY - ITEM_MARK_RADIUS,
                           ITEM_MARK_RADIUS * 2, ITEM_MARK_RADIUS * 2);
            }

        }

//        System.err.println(">>> paintItem, dirtyArea: " + dirtyArea);
        
    }

    private static long getYValue(XYItem item, int valueIndex,
                                  int type, ChartContext context, double itemValueFactor) {
        if (type == TYPE_ABSOLUTE) {
            return context.getViewY(item.getYValue(valueIndex));
        } else {
            return context.getViewY(context.getDataOffsetY() + (long)(itemValueFactor *
                        (double)(item.getYValue(valueIndex) - item.getBounds().y)));
        }
    }

}

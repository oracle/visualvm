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
import org.graalvm.visualvm.lib.charts.ItemSelection;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartItem;
import org.graalvm.visualvm.lib.charts.ChartItemChange;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.XYItemChange;
import org.graalvm.visualvm.lib.charts.xy.XYItemPainter;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYChartContext;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class TimelineXYPainter extends XYItemPainter.Abstract {

    private final int viewExtent;
    private final boolean bottomBased;

    private boolean painting;

    protected final double dataFactor;


    // --- Constructor ---------------------------------------------------------

    TimelineXYPainter(int viewExtent, boolean bottomBased, double dataFactor) {
        this.viewExtent = viewExtent;
        this.bottomBased = bottomBased;
        this.dataFactor = dataFactor;
        painting = true;
    }


    // --- Abstract interface --------------------------------------------------

    protected abstract void paint(XYItem item, List<ItemSelection> highlighted,
                                  List<ItemSelection> selected, Graphics2D g,
                                  Rectangle dirtyArea, SynchronousXYChartContext
                                  context);

    protected abstract Color getDefiningColor();


    // --- Public interface ----------------------------------------------------

    void setPainting(boolean painting) {
        this.painting = painting;
    }

    boolean isPainting() {
        return painting;
    }


    // --- Protected interface -------------------------------------------------

    protected final int getViewExtent() {
        return viewExtent;
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
            return getViewBounds(item, selectedValueIndex, context);
//            return getViewBounds(item, new int[] { selectedValueIndex }, context);
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

    public final void paintItem(ChartItem item, List<ItemSelection> highlighted,
                          List<ItemSelection> selected, Graphics2D g,
                          Rectangle dirtyArea, ChartContext context) {

        if (!painting) return;
        
        XYItem it = (XYItem)item;
        if (it.getValuesCount() < 1) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        SynchronousXYChartContext ctx = (SynchronousXYChartContext)context;
        paint((XYItem)item, highlighted, selected, g, dirtyArea, ctx);
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

        if (bottomBased) {
            bounds.height += bounds.y;
            bounds.y = 0;
        }

        return bounds;
    }

    private LongRect getViewBounds(LongRect itemBounds, ChartContext context) {
        LongRect dataBounds = getDataBounds(itemBounds);

        LongRect viewBounds = context.getViewRect(dataBounds);
        LongRect.addBorder(viewBounds, viewExtent);

        return viewBounds;
    }

    private LongRect getViewBounds(XYItem item, int valueIndex, ChartContext context) {
        long xValue = item.getXValue(valueIndex);
        long yValue = (long)(item.getYValue(valueIndex) * dataFactor);
        return context.getViewRect(new LongRect(xValue, yValue, 0, 0));
    }

//    private LongRect getViewBounds(XYItem item, int[] valuesIndexes, ChartContext context) {
//
//        LongRect dataBounds = new LongRect();
//
//        if (valuesIndexes == null) {
//            LongRect.set(dataBounds, item.getBounds());
//            dataBounds.y *= dataFactor;
//            dataBounds.height *= dataFactor;
//        } else {
//            boolean firstPoint = true;
//            for (int valueIndex : valuesIndexes) {
//                if (valueIndex == -1) continue;
//                long xValue = item.getXValue(valueIndex);
//                long yValue = (long)(item.getYValue(valueIndex) * dataFactor);
//                if (firstPoint) {
//                    LongRect.set(dataBounds, xValue, yValue, 0, 0);
//                    firstPoint = false;
//                } else {
//                    LongRect.add(dataBounds, xValue, yValue);
//                }
//            }
//        }
//
//        LongRect viewBounds = context.getViewRect(dataBounds);
//        LongRect.addBorder(viewBounds, viewExtent);
//        return viewBounds;
//    }

}

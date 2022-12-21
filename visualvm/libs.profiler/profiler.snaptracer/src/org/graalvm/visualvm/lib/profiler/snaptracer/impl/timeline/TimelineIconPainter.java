/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartItem;
import org.graalvm.visualvm.lib.charts.ChartItemChange;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.XYItemChange;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYChartContext;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.IdeSnapshot;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.icons.TracerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
class TimelineIconPainter extends TimelineXYPainter {

    private static final Icon ICON = Icons.getIcon(TracerIcons.GENERIC_ACTION);

    private static final int ICON_EXTENT = 8;

    protected final Color color;
    protected final IdeSnapshot snapshot;


    // --- Constructor ---------------------------------------------------------

    TimelineIconPainter(Color color, IdeSnapshot snapshot) {
        super(ICON_EXTENT, true, 1);
        this.color = color;
        this.snapshot = snapshot;
    }


    // --- Abstract interface --------------------------------------------------

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                         List<ItemSelection> selected, Graphics2D g,
                         Rectangle dirtyArea, SynchronousXYChartContext
                         context) {

        if (context.getViewWidth() == 0) return;
        
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);

        int firstFirst = visibleBounds[0][0];
        int firstIndex = firstFirst;
        if (firstIndex == -1) firstIndex = visibleBounds[0][1];
        if (firstIndex == -1) return;

        int minX = dirtyArea.x - ICON_EXTENT;
        while (context.getViewX(item.getXValue(firstIndex)) > minX && firstIndex > 0) firstIndex--;

        int endIndex = item.getValuesCount() - 1;
        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = endIndex;

        int maxX = dirtyArea.x + dirtyArea.width + ICON_EXTENT;
        while (context.getViewX(item.getXValue(lastIndex)) < maxX && lastIndex < endIndex) lastIndex++;

        g.setColor(color);

        for (int index = firstIndex; index <= lastIndex; index++) {
            long dataY = item.getYValue(index);
            if (dataY == 0) continue;

            long dataX = item.getXValue(index);
            int  viewX = Utils.checkedInt(context.getViewX(dataX));
            Icon icon = snapshot.getLogInfoForValue(dataY).getIcon();
            if (icon == null) icon = ICON;
            int iconWidth = icon.getIconWidth();
            int iconHeight = icon.getIconHeight();
            icon.paintIcon(null, g, viewX - iconWidth / 2, (context.getViewportHeight() - iconHeight) / 2);
        }
    }

    protected Color getDefiningColor() { return color; }
    

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
        return dirtyBounds.width != 0 && dirtyBounds.height != 0;
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
    }

    public XYItemSelection getClosestSelection(ChartItem item, int viewX,
                                               int viewY, ChartContext context) {

        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;
        SynchronousXYItem xyItem = (SynchronousXYItem)item;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return new XYItemSelection.Default(xyItem,
                nearestTimestampIndex, ItemSelection.DISTANCE_UNKNOWN);

        int minX = viewX - ICON_EXTENT;
        int maxX = viewX + ICON_EXTENT;
        int itemX = Utils.checkedInt(contx.getViewX(xyItem.getXValue(nearestTimestampIndex)));
        if (itemX > maxX || itemX < minX) return new XYItemSelection.Default(xyItem,
                nearestTimestampIndex, ItemSelection.DISTANCE_UNKNOWN);

        int closest = -1;
        int index = nearestTimestampIndex;
        while (index < xyItem.getValuesCount()) {
            if (Utils.checkedInt(contx.getViewX(xyItem.getXValue(index))) > maxX) break;
            if (xyItem.getYValue(index) != 0) closest = index;
            index++;
        }

        if (closest != -1) return new XYItemSelection.Default(xyItem, closest,
                ItemSelection.DISTANCE_UNKNOWN);

        index = nearestTimestampIndex - 1;
        while (index >= 0) {
            if (Utils.checkedInt(contx.getViewX(xyItem.getXValue(index))) < minX) break;
            if (xyItem.getYValue(index) != 0) closest = index;
            index--;
        }

        if (closest != -1) return new XYItemSelection.Default(xyItem, closest,
                ItemSelection.DISTANCE_UNKNOWN);

        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                ItemSelection.DISTANCE_UNKNOWN);
    }


    // --- XYItemPainter implementation ----------------------------------------

    public double getItemView(double dataY, XYItem item, ChartContext context) {
        return -1;
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        return -1;
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        return -1;
    }


    // --- Private implementation ----------------------------------------------

    private LongRect getDataBounds(LongRect itemBounds) {

        LongRect bounds = new LongRect(itemBounds);
        bounds.y = 0;
        bounds.height = 1000;
        return bounds;
    }

    private LongRect getViewBounds(LongRect itemBounds, ChartContext context) {

        boolean isData = itemBounds.height != 0;

        LongRect viewBounds = context.getViewRect(itemBounds);

        if (isData) {
            viewBounds.y = Utils.checkedInt(context.getViewY(context.getDataHeight() / 2));
            viewBounds.height = 0;
            LongRect.addBorder(viewBounds, ICON_EXTENT);
        } else {
            LongRect.clear(viewBounds);
        }
        
        return viewBounds;
    }

    private LongRect getViewBounds(XYItem item, int valueIndex, ChartContext context) {
        long xValue = item.getXValue(valueIndex);
        LongRect viewBounds = new LongRect(Utils.checkedInt(context.getViewX(xValue)),
                                           Utils.checkedInt(context.getViewY(context.
                                           getDataHeight() / 2)), 0, 0);

        if (item.getYValue(valueIndex) != 0) LongRect.addBorder(viewBounds, ICON_EXTENT);

        return viewBounds;
    }

}

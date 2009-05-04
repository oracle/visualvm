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

import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.LongRect;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerGCXYItemPainter extends ProfilerXYItemPainter {

    // --- Constructor ---------------------------------------------------------

    public static ProfilerGCXYItemPainter painter(Color fillColor) {
        
        return new ProfilerGCXYItemPainter(fillColor);
    }


    ProfilerGCXYItemPainter(Color fillColor) {
        super(0, null, fillColor, TYPE_ABSOLUTE, 0);
    }


    // --- ItemPainter implementation ------------------------------------------

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
//        if (!(item instanceof ProfilerGCXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        // TODO: should return real bounds (== empty bounds for no gc)

        LongRect viewBounds = super.getItemBounds(item, context);
        viewBounds.y = 0;
        viewBounds.height = context.getViewportHeight();
        return viewBounds;
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
//        if (!(itemChange instanceof XYItemChange))
//            throw new UnsupportedOperationException("Unsupported itemChange: " + itemChange);
        
        XYItemChange change = (XYItemChange)itemChange;
        ProfilerGCXYItem item = (ProfilerGCXYItem)change.getItem();
        
        int[] indexes = change.getValuesIndexes();

        if (indexes.length == 1 && indexes[0] == -1) {
            // Data reset
            LongRect dirtyBounds = change.getDirtyValuesBounds();
            
            long x = (long)context.getViewX(dirtyBounds.x);
            long y = 0;
            long width = (long)context.getViewWidth(dirtyBounds.width);
            long height = context.getViewportHeight();

            return new LongRect(x, y, width, height);
        } else {
            // New data
            int index = indexes[0];
            int lastIndex = indexes[indexes.length - 1];

            long dataStart = -1;
            long dataEnd   = -1;

            while (index <= lastIndex) {
                long[] gcEnds = item.getGCEnds(index);
                if (gcEnds.length > 0) {
                    dataEnd = gcEnds[gcEnds.length - 1];
                    if (dataStart == -1) {
                        long[] gcStarts = item.getGCStarts(index);
                        dataStart = gcStarts[0];
                    }
                }
                index++;
            }

            if (dataStart == -1) return new LongRect();
            if (dataEnd == -1) dataEnd = item.getXValue(item.getValuesCount() - 1);

            long x = (long)context.getViewX(dataStart);
            long y = 0;
            long width = (long)context.getViewWidth(dataEnd - dataStart);
            width = Math.max(width, 1);
            long height = context.getViewportHeight();

            return new LongRect(x, y, width, height);

        }
    }


    public double getItemView(double dataY, XYItem item, ChartContext context) {
        return 0;
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        return 0;
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        return 0;
    }


    // --- Private implementation ----------------------------------------------

    
    protected void paint(XYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, ProfilerXYChart.Context context) {
//        if (!(item instanceof ProfilerGCXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        int valuesCount = item.getValuesCount();
        if (valuesCount < 2) return;

        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);

        int firstFirst = visibleBounds[0][0];
        int index = firstFirst;
        if (index == -1) index = visibleBounds[0][1];
        if (index == -1) return;

        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = valuesCount - 1;
        if (lastFirst != -1 && lastIndex < valuesCount - 1) lastIndex += 1;

        int itemsStep = (int)Math.ceil(valuesCount / context.getViewWidth());
        if (itemsStep == 0) itemsStep = 1;

        int visibleCount = lastIndex - index + 1;

        if (itemsStep > 1) {
            int firstMod = index % itemsStep;
            index -= firstMod;
            int lastMod = lastIndex % itemsStep;
            lastIndex = lastIndex - lastMod + itemsStep;
            visibleCount = (lastIndex - index) / itemsStep + 1;
            lastIndex = Math.min(lastIndex, valuesCount - 1);
        }

        ProfilerGCXYItem xyItem = (ProfilerGCXYItem)item;

        g.setColor(fillColor);

        for (int iter = 0; iter < visibleCount; iter++) {
            long[] gcStarts = xyItem.getGCStarts(index);
            if (gcStarts.length > 0) {
                long[] gcEnds = xyItem.getGCEnds(index);
                for (int i = 0; i < gcStarts.length; i++) {
                    int itemStart = ChartContext.getCheckedIntValue(
                                                 context.getViewX(gcStarts[i]));
                    long gcEnd = gcEnds[i];
                    if (gcEnd == -1)
                        gcEnd =  item.getXValue(item.getValuesCount() - 1);
                    int itemLength = ChartContext.getCheckedIntValue(
                                                 context.getViewWidth(
                                                 gcEnd - gcStarts[i]));
                    
                    g.fillRect(itemStart, 0, Math.max(itemLength, 1),
                               context.getViewportHeight());
                }
            }
            
            index = Math.min(index + itemsStep, lastIndex);
        }
        
    }

}

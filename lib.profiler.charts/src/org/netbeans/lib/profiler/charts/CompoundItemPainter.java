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

package org.netbeans.lib.profiler.charts;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public class CompoundItemPainter implements ItemPainter {
    
    private ItemPainter painter1;
    private ItemPainter painter2;


    public CompoundItemPainter(ItemPainter painter1, ItemPainter painter2) {
        this.painter1 = painter1;
        this.painter2 = painter2;
    }


    public LongRect getItemBounds(ChartItem item) {
        LongRect itemBounds = painter1.getItemBounds(item);
        LongRect.add(itemBounds, painter2.getItemBounds(item));
        return itemBounds;
    }

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
        LongRect itemBounds = painter1.getItemBounds(item, context);
        LongRect.add(itemBounds, painter2.getItemBounds(item, context));
        return itemBounds;
    }


    public boolean isBoundsChange(ChartItemChange itemChange) {
        return painter1.isBoundsChange(itemChange) ||
               painter2.isBoundsChange(itemChange);
    }

    public boolean isAppearanceChange(ChartItemChange itemChange, ChartContext context) {
        return painter1.isAppearanceChange(itemChange, context) ||
               painter2.isAppearanceChange(itemChange, context);
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
        LongRect dirtyBounds = painter1.getDirtyBounds(itemChange, context);
        LongRect.add(dirtyBounds, painter2.getDirtyBounds(itemChange, context));
        return dirtyBounds;
    }
    

    public boolean supportsHovering(ChartItem item) {
        return painter1.supportsHovering(item) || painter2.supportsHovering(item);
    }

    public boolean supportsSelecting(ChartItem item) {
        return painter1.supportsSelecting(item) || painter2.supportsSelecting(item);
    }

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {
        LongRect bounds1 = painter1.supportsHovering(selection.getItem()) ? painter1.getSelectionBounds(selection, context) : new LongRect();
        LongRect bounds2 = painter2.supportsHovering(selection.getItem()) ? painter2.getSelectionBounds(selection, context) : new LongRect();
        LongRect.add(bounds1, bounds2);
        return bounds1;
    }

    public ItemSelection getClosestSelection(ChartItem item, int viewX, int viewY, ChartContext context) {
        ItemSelection selection1 = painter1.supportsHovering(item) ? painter1.getClosestSelection(item, viewX, viewY, context) : null;
        ItemSelection selection2 = painter2.supportsHovering(item) ? painter2.getClosestSelection(item, viewX, viewY, context) : null;

        if (selection1 == null) return selection2;
        else if (selection2 == null) return selection1;
        else if (selection1.getDistance() < selection2.getDistance()) return selection1;
        else return selection2;
    }


    public void paintItem(ChartItem item, List<ItemSelection> highlighted, List<ItemSelection> selected, Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        painter1.paintItem(item, highlighted, selected, g, dirtyArea, context);
        painter2.paintItem(item, highlighted, selected, g, dirtyArea, context);
    }


    protected ItemPainter getPainter1() {
        return painter1;
    }

    protected ItemPainter getPainter2() {
        return painter2;
    }

}

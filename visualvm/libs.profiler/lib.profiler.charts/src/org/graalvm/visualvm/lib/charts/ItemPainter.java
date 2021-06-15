/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.charts;

import org.graalvm.visualvm.lib.charts.swing.LongRect;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ItemPainter {

    // --- Item bounds ---------------------------------------------------------

    // General item bounds without decorations, data space [0, 0]-based
    public LongRect getItemBounds(ChartItem item);

    // Concrete item bounds for given ChartContext, display space
    public LongRect getItemBounds(ChartItem item, ChartContext context);


    // --- Item change support -------------------------------------------------

    public boolean isBoundsChange(ChartItemChange itemChange);

    public boolean isAppearanceChange(ChartItemChange itemChange);

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context);


    // --- Item location -------------------------------------------------------

    public boolean supportsHovering(ChartItem item);

    public boolean supportsSelecting(ChartItem item);

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context);

    public ItemSelection getClosestSelection(ChartItem item, int viewX, int viewY, ChartContext context);


    // --- Item appearance -----------------------------------------------------

    public void paintItem(ChartItem item, List<ItemSelection> highlighted,
                          List<ItemSelection> selected, Graphics2D g,
                          Rectangle dirtyArea, ChartContext context);

}

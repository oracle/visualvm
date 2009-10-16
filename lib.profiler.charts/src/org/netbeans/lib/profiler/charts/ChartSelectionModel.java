/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.Rectangle;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ChartSelectionModel {

    public static final int SELECTION_NONE = 0;
    public static final int SELECTION_LINE_V = 1;
    public static final int SELECTION_LINE_H = 2;
    public static final int SELECTION_CROSS = 3;
    public static final int SELECTION_RECT = 4;

    public static final int HOVER_NONE = 100;
    public static final int HOVER_NEAREST = 101;
    public static final int HOVER_EACH_NEAREST = 102;

    public static final int HOVER_DISTANCE_LIMIT_NONE = -1;


    // --- Selection mode ------------------------------------------------------

    public void setMoveMode(int mode);

    public int getMoveMode();

    public void setDragMode(int mode);

    public int getDragMode();

    public int getSelectionMode();

    public void setHoverMode(int mode);

    public int getHoverMode();

    public void setHoverDistanceLimit(int limit);

    public int getHoverDistanceLimit();


    // --- Bounds selection ----------------------------------------------------

    public void setSelectionBounds(Rectangle selectionBounds);

    public Rectangle getSelectionBounds();

    
    // --- Items selection -----------------------------------------------------

    public void setHighlightedItems(List<ItemSelection> items);

    public List<ItemSelection> getHighlightedItems();

    public void setSelectedItems(List<ItemSelection> items);

    public List<ItemSelection> getSelectedItems();


    // --- Selection listeners -------------------------------------------------

    public void addSelectionListener(ChartSelectionListener listener);

    public void removeSelectionListener(ChartSelectionListener listener);

}

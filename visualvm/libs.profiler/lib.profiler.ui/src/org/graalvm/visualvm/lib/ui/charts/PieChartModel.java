/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.charts;

import java.awt.Color;


/**
 *
 * @author Jiri Sedlacek
 */
public interface PieChartModel {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Color getItemColor(int index); // color of item

    public int getItemCount(); // number of displayed (processed) items

    public String getItemName(int index); // name of item

    public double getItemValue(int index); // value of item

    public double getItemValueRel(int index); // relative item value (<0, 1>, E(items) = 1)

    public boolean isSelectable(int index); // is the given item ready to be selected?

    /**
     * Adds new ChartModel listener.
     * @param listener ChartModel listener to add
     */
    public void addChartModelListener(ChartModelListener listener);

    public boolean hasData(); // does the model contain some non-zero item?

    /**
     * Removes ChartModel listener.
     * @param listener ChartModel listener to remove
     */
    public void removeChartModelListener(ChartModelListener listener);
}

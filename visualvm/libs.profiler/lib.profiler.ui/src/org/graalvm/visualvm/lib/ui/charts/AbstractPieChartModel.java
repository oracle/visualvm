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
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractPieChartModel implements PieChartModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Collection<ChartModelListener> listeners = new CopyOnWriteArraySet<>(); // Data change listeners

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract Color getItemColor(int index); // color of item

    // --- Abstract PieChartModel ------------------------------------------------
    public abstract int getItemCount(); // number of displayed (processed) items

    public abstract String getItemName(int index); // name of item

    public abstract double getItemValue(int index); // value of item

    public abstract double getItemValueRel(int index); // relative item value (<0, 1>, E(items) = 1)

    public abstract boolean hasData(); // does the model contain some non-zero item?

    // --- Listeners -------------------------------------------------------------

    /**
     * Adds new ChartModel listener.
     * @param listener ChartModel listener to add
     */
    public synchronized void addChartModelListener(ChartModelListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes ChartModel listener.
     * @param listener ChartModel listener to remove
     */
    public synchronized void removeChartModelListener(ChartModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners about the data change.
     */
    protected void fireChartDataChanged() {
        for(ChartModelListener l : listeners) {
            l.chartDataChanged();
        }
    }
}

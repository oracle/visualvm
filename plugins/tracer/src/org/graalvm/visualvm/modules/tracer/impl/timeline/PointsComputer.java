/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.impl.timeline;

import java.awt.Rectangle;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYChartContext;

/**
 *
 * @author Jiri Sedlacek
 */
final class PointsComputer {

    private static final int INDEXES_STEP = 1000;

    private int[] arr1;
    private int[] arr2;
    private final int[] count = new int[1];
    private final int[][] ret = new int[2][];


    PointsComputer() {}


    void reset() {
        arr1 = null;
        arr2 = null;
    }

    private int[] arr1(int size) {
        if (arr1 == null || arr1.length < size)
            arr1 = newArr(size + INDEXES_STEP, true);
        return arr1;
    }
    
    private int[] arr2(int size) {
        if (arr2 == null || arr2.length < size)
            arr2 = newArr(size + INDEXES_STEP, false);
        return arr2;
    }

    private int[] newArr(int size, boolean arr1) {
        int[] arr = new int[size];
        if (arr1) ret[0] = arr;
        return arr;
    }


    int[][] getVisible(Rectangle dirtyArea, int valuesCount,
                       SynchronousXYChartContext context,
                       int extraFactor, int extraTrailing) {

        if (context.getViewWidth() == 0) return null;
        
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);

        int firstFirst = visibleBounds[0][0];
        int firstIndex = firstFirst;
        if (firstIndex == -1) firstIndex = visibleBounds[0][1];
        if (firstIndex == -1) return null;
        // firstIndex - 2: workaround for polyline joins
        if (firstFirst != -1) firstIndex = Math.max(firstIndex - 2, 0);

        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = valuesCount - 1;
        // lastIndex + 2: workaround for polyline joins
        if (lastFirst != -1) lastIndex = Math.min(lastIndex + 2, valuesCount - 1);

        int itemsStep = (int)Math.ceil(valuesCount / context.getViewWidth());
        if (itemsStep == 0) itemsStep = 1;

        int visibleCount = lastIndex - firstIndex + 1;

        if (itemsStep > 1) {
            int firstMod = firstIndex % itemsStep;
            firstIndex -= firstMod;
            int lastMod = lastIndex % itemsStep;
            lastIndex = lastIndex - lastMod + itemsStep;
            visibleCount = (lastIndex - firstIndex) / itemsStep + 1;
            lastIndex = Math.min(lastIndex, valuesCount - 1);
        }

        int visibleCountEx = extraFactor == 1 ? visibleCount :
                            (visibleCount - 1) * extraFactor + 2;

        count[0] = visibleCountEx + extraTrailing;
        int[] visibleIndexes = arr1(count[0]);

        for (int i = 0; i < visibleCountEx; i++) {
            int index = firstIndex + (i / extraFactor) * itemsStep;
            if (index > lastIndex) index = lastIndex;
            visibleIndexes[i] = index;
        }
        
        for (int i = visibleCountEx; i < visibleIndexes.length; i++)
            visibleIndexes[i] = -1;

        ret[1] = count;
        return ret;
    }

    int getZeroY(SynchronousXYChartContext context) {
        int zeroY = Utils.checkedInt(context.getViewY(context.getDataOffsetY()));
        zeroY = Math.max(Utils.checkedInt(context.getViewportOffsetY()), zeroY);
        zeroY = Math.min(Utils.checkedInt(context.getViewportOffsetY() +
                                          context.getViewportHeight()), zeroY);
        return zeroY;
    }

    int[][] createPoints(int[] indexes, int itemsCount, XYItem item,
                         double dataFactor, SynchronousXYChartContext context) {

        int[] xPoints = indexes;
        int[] yPoints = arr2(itemsCount);

        for (int i = 0; i < itemsCount; i++) {
            int dataIndex = xPoints[i];
            if (dataIndex != -1) {
                xPoints[i] = Utils.checkedInt(Math.ceil(
                             context.getViewX(item.getXValue(dataIndex))));
                yPoints[i] = Utils.checkedInt(Math.ceil(
                             context.getViewY(item.getYValue(dataIndex) *
                             dataFactor)));
            }
        }

        ret[1] = yPoints;
        return ret;
    }

}

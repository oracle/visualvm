/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
public class DynamicPieChartModel extends AbstractPieChartModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Color[] itemColors;
    protected String[] itemNames;
    protected double[] itemValues;
    protected double[] itemValuesRel;
    protected boolean hasData = false;
    protected int itemCount = 0;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Color getItemColor(int index) {
        return itemColors[index];
    }

    // --- Abstract PieChartModel ------------------------------------------------
    public int getItemCount() {
        return itemCount;
    }

    public String getItemName(int index) {
        return itemNames[index];
    }

    public double getItemValue(int index) {
        return itemValues[index];
    }

    public double getItemValueRel(int index) {
        return itemValuesRel[index];
    }

    public void setItemValues(double[] itemValues) {
        if (itemValues.length != itemCount) {
            hasData = false;
            throw new RuntimeException("Unexpected number of values."); // NOI18N
        } else {
            this.itemValues = itemValues;
            updateItemValuesRel();
        }

        fireChartDataChanged();
    }

    public boolean isSelectable(int index) {
        return true;
    }

    public boolean hasData() {
        return hasData;
    }

    public void setupModel(String[] itemNames, Color[] itemColors) {
        this.itemNames = itemNames;
        this.itemColors = itemColors;

        if (itemNames.length != itemColors.length) {
            itemCount = 0;
            throw new RuntimeException("Counts of item names and item colors don't match."); // NOI18N
        } else {
            itemCount = itemNames.length;
        }

        itemValues = null;
        itemValuesRel = new double[itemCount];
        hasData = false;
    }

    // --- Private Implementation ------------------------------------------------

    // computes relative item values
    // O(n) = 2n
    private void updateItemValuesRel() {
        double sum = 0d;

        // compute sum of all item values
        for (double itemValue : itemValues) {
            sum += itemValue;
        }

        // compute new relative item values
        if (sum == 0) {
            for (int i = 0; i < itemValues.length; i++) {
                itemValuesRel[i] = 0;
            }

            hasData = false;
        } else {
            for (int i = 0; i < itemValues.length; i++) {
                itemValuesRel[i] = itemValues[i] / sum;
            }

            hasData = true;
        }
    }
}

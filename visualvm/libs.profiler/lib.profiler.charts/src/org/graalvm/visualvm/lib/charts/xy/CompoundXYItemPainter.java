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

package org.graalvm.visualvm.lib.charts.xy;

import org.graalvm.visualvm.lib.charts.CompoundItemPainter;
import org.graalvm.visualvm.lib.charts.ChartContext;

/**
 *
 * @author Jiri Sedlacek
 */
public class CompoundXYItemPainter extends CompoundItemPainter implements XYItemPainter {

    public CompoundXYItemPainter(XYItemPainter painter1, XYItemPainter painter2) {
        super(painter1, painter2);
    }


    public double getItemView(double dataY, XYItem item, ChartContext context) {
        return getPainter1().getItemView(dataY, item, context);
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        return getPainter1().getItemValue(viewY, item, context);
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        return getPainter1().getItemValueScale(item, context);
    }


    protected XYItemPainter getPainter1() {
        return (XYItemPainter)super.getPainter1();
    }

    protected XYItemPainter getPainter2() {
        return (XYItemPainter)super.getPainter2();
    }

}

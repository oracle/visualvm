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

package org.netbeans.lib.profiler.charts.xy;

import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
public interface XYItemPainter extends ItemPainter {

    public static final int TYPE_ABSOLUTE = 0;
    public static final int TYPE_RELATIVE = 1;
    

    public double getItemView(double dataY, XYItem item, ChartContext context);

    public double getItemValue(double viewY, XYItem item, ChartContext context);

    public double getItemValueScale(XYItem item, ChartContext context);


    public static abstract class Abstract implements XYItemPainter {

        public double getItemView(double dataY, XYItem item, ChartContext context) {
            return context.getViewY(dataY);
        }

        public double getItemValue(double viewY, XYItem item, ChartContext context) {
            return context.getDataY(viewY);
        }

        public double getItemValueScale(XYItem item, ChartContext context) {
            double scale = context.getViewHeight(1d);
            if (scale <= 0) scale = -1;
            return scale;
        }

    }

}

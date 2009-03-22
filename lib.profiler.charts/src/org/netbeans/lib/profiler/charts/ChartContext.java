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

import java.awt.Rectangle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ChartContext {

    public static final int VALUE_OUT_OF_RANGE_NEG = Integer.MIN_VALUE;
    public static final int VALUE_OUT_OF_RANGE_POS = Integer.MAX_VALUE;


    // --- Chart orientation ---------------------------------------------------

    public abstract boolean isRightBased();

    public abstract boolean isBottomBased();


    // --- Fixed scale ---------------------------------------------------------

    public abstract boolean fitsWidth();

    public abstract boolean fitsHeight();


    // --- Chart bounds --------------------------------------------------------

    public abstract long getDataOffsetX();

    public abstract long getDataOffsetY();

    public abstract long getDataWidth();

    public abstract long getDataHeight();

    public abstract long getViewWidth();

    public abstract long getViewHeight();

    public abstract int getViewportWidth();

    public abstract int getViewportHeight();


    // --- Data to View --------------------------------------------------------

    public abstract long getViewX(long dataX);

    public abstract long getReversedViewX(long dataX);

    public abstract long getViewY(long dataY);

    public abstract long getReversedViewY(long dataY);

    public abstract long getViewWidth(long dataWidth);

    public abstract long getViewHeight(long dataHeight);

    public abstract LongRect getViewRect(LongRect dataRect);

//    public abstract LongRect getReversedViewRect(LongRect dataRect);


    // --- View to Data --------------------------------------------------------

    public abstract long getDataX(int viewX);

    public abstract long getReversedDataX(int viewX);

    public abstract long getDataY(int viewY);

    public abstract long getReversedDataY(int viewY);

    public abstract long getDataWidth(int viewWidth);

    public abstract long getDataHeight(int viewHeight);

//    public abstract LongRect getDataRect(LongRect viewRect);
//
//    public abstract LongRect getReversedDataRect(LongRect viewRect);


    // --- Utils ---------------------------------------------------------------

    public static final int getCheckedIntValue(long value) {
        if (value < Integer.MIN_VALUE) return VALUE_OUT_OF_RANGE_NEG;
        if (value > Integer.MAX_VALUE) return VALUE_OUT_OF_RANGE_POS;
        else return (int)value;
    }

    public static final Rectangle getCheckedRectangle(LongRect rect) {
        // TODO: this is incorrect, width/height don't reflect x/y truncation!
        return new Rectangle(getCheckedIntValue(rect.x),
                            getCheckedIntValue(rect.y),
                            getCheckedIntValue(rect.width),
                            getCheckedIntValue(rect.height));
    }

    protected final LongRect getViewRectImpl(LongRect dataRect) {
        LongRect viewRect = new LongRect();

        viewRect.x = getViewX(dataRect.x);
        viewRect.width = getViewWidth(dataRect.width);
        if (isRightBased()) viewRect.x -= viewRect.width;

        viewRect.y = getViewY(dataRect.y);
        viewRect.height = getViewHeight(dataRect.height);
        if (isBottomBased()) viewRect.y -= viewRect.height;

        return viewRect;
    }

}

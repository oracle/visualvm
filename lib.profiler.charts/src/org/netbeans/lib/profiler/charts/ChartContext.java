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

import org.netbeans.lib.profiler.charts.swing.LongRect;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ChartContext {

    // --- Chart orientation ---------------------------------------------------

    public boolean isRightBased();

    public boolean isBottomBased();


    // --- Fixed scale ---------------------------------------------------------

    public boolean fitsWidth();

    public boolean fitsHeight();


    // --- Chart bounds --------------------------------------------------------

    public long getDataOffsetX();

    public long getDataOffsetY();

    public long getDataWidth();

    public long getDataHeight();

    public long getViewWidth();

    public long getViewHeight();

    public int getViewportWidth();

    public int getViewportHeight();


    // --- Data to View --------------------------------------------------------

    public double getViewX(double dataX);

    public double getReversedViewX(double dataX);

    public double getViewY(double dataY);

    public double getReversedViewY(double dataY);

    public double getViewWidth(double dataWidth);

    public double getViewHeight(double dataHeight);

    public LongRect getViewRect(LongRect dataRect);

//    public LongRect getReversedViewRect(LongRect dataRect);


    // --- View to Data --------------------------------------------------------

    public double getDataX(double viewX);

    public double getReversedDataX(double viewX);

    public double getDataY(double viewY);

    public double getReversedDataY(double viewY);

    public double getDataWidth(double viewWidth);

    public double getDataHeight(double viewHeight);

//    public LongRect getDataRect(LongRect viewRect);
//
//    public LongRect getReversedDataRect(LongRect viewRect);

}

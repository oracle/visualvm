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

package org.graalvm.visualvm.lib.ui;

import java.awt.*;


/** Various UI Constants used in the JFluid UI
 *
 * @author Ian Formanek
 */
public interface UIConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /** Color used to draw vertical gridlines in JTables */
    public static final Color TABLE_VERTICAL_GRID_COLOR = !UIUtils.isDarkResultsBackground() ?
                              new Color(214, 223, 247) : new Color(84, 93, 117);

    /** if true, results tables display the horizontal grid lines */
    public static final boolean SHOW_TABLE_HORIZONTAL_GRID = false;

    /** if true, results tables display the vertical grid lines */
    public static final boolean SHOW_TABLE_VERTICAL_GRID = true;

    /** Color used for painting selected cell background in JTables */
    public static final Color TABLE_SELECTION_BACKGROUND_COLOR = new Color(193, 210, 238); //(253, 249, 237)

    /** Color used for painting selected cell foreground in JTables */
    public static final Color TABLE_SELECTION_FOREGROUND_COLOR = Color.BLACK;
    public static final int TABLE_ROW_MARGIN = 0;

    public static final String PROFILER_PANELS_BACKGROUND = "ProfilerPanels.background"; // NOI18N
}

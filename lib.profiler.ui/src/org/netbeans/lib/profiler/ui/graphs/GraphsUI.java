/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.ui.graphs;

import java.awt.Color;
import java.util.ResourceBundle;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public interface GraphsUI {

    // -----
    // I18N String constants
    static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.graphs.Bundle"); // NOI18N
    public static final String A_ALLOC_OBJECTS_NAME = messages.getString("GraphsUI_AllocObjects"); // NOI18N
    public static final String A_ALLOC_BYTES_NAME = messages.getString("GraphsUI_AllocBytes"); // NOI18N
    public static final String L_LIVE_OBJECTS_NAME = messages.getString("GraphsUI_LiveObjects"); // NOI18N
    public static final String L_LIVE_BYTES_NAME = messages.getString("GraphsUI_LiveBytes"); // NOI18N
    public static final String HEAP_SIZE_NAME = messages.getString("GraphsUI_HeapSize"); // NOI18N
    public static final String USED_HEAP_NAME = messages.getString("GraphsUI_UsedHeap"); // NOI18N
    public static final String SURVGEN_NAME = messages.getString("GraphsUI_SurvGen"); // NOI18N
    public static final String GC_TIME_NAME = messages.getString("GraphsUI_GcTime"); // NOI18N
    public static final String THREADS_NAME = messages.getString("GraphsUI_Threads"); // NOI18N
    public static final String LOADED_CLASSES_NAME = messages.getString("GraphsUI_LoadedClasses"); // NOI18N
    // -----

    // --- General colors definition -------------------------------------------

    public static final Color  PROFILER_BLUE = new Color(127, 63, 191);
    public static final Color  PROFILER_RED = new Color(255, 127, 127);
    public static final Color  PROFILER_GREEN = new Color(30, 157, 68);


    // --- Charts colors definition --------------------------------------------

    public static final Color  CHART_BACKGROUND_COLOR = UIUtils.getProfilerResultsBackground();
    public static final Color  SMALL_LEGEND_BACKGROUND_COLOR = UIUtils.getProfilerResultsBackground();
    public static final Color  SMALL_LEGEND_BORDER_COLOR = new Color(235, 235, 235);

    public static final float  TOOLTIP_OVERLAY_LINE_WIDTH = 2.1f;
    public static final Color  TOOLTIP_OVERLAY_LINE_COLOR = Color.DARK_GRAY;
    public static final Color  TOOLTIP_OVERLAY_FILL_COLOR = UIUtils.getProfilerResultsBackground();


    // --- Class History graphs ------------------------------------------------

    public static final float  A_ALLOC_OBJECTS_PAINTER_LINE_WIDTH = 3f;
    public static final Color  A_ALLOC_OBJECTS_PAINTER_LINE_COLOR = PROFILER_RED;
    public static final Color  A_ALLOC_OBJECTS_PAINTER_FILL_COLOR = null;
    public static final int    A_ALLOC_OBJECTS_MARKER_RADIUS = 5;
    public static final float  A_ALLOC_OBJECTS_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  A_ALLOC_OBJECTS_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  A_ALLOC_OBJECTS_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  A_ALLOC_OBJECTS_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  A_ALLOC_OBJECTS_MARKER_FILL_COLOR = PROFILER_RED;
    public static final long   A_ALLOC_OBJECTS_INITIAL_VALUE = 100l;

    public static final float  A_ALLOC_BYTES_PAINTER_LINE_WIDTH = 3f;
    public static final Color  A_ALLOC_BYTES_PAINTER_LINE_COLOR = PROFILER_BLUE;
    public static final Color  A_ALLOC_BYTES_PAINTER_FILL_COLOR = null;
    public static final int    A_ALLOC_BYTES_MARKER_RADIUS = 5;
    public static final float  A_ALLOC_BYTES_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  A_ALLOC_BYTES_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  A_ALLOC_BYTES_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  A_ALLOC_BYTES_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  A_ALLOC_BYTES_MARKER_FILL_COLOR = PROFILER_BLUE;
    public static final long   A_ALLOC_BYTES_INITIAL_VALUE = 102400l;

    public static final float  L_LIVE_OBJECTS_PAINTER_LINE_WIDTH = 3f;
    public static final Color  L_LIVE_OBJECTS_PAINTER_LINE_COLOR = PROFILER_RED;
    public static final Color  L_LIVE_OBJECTS_PAINTER_FILL_COLOR = null;
    public static final int    L_LIVE_OBJECTS_MARKER_RADIUS = 5;
    public static final float  L_LIVE_OBJECTS_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  L_LIVE_OBJECTS_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  L_LIVE_OBJECTS_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  L_LIVE_OBJECTS_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  L_LIVE_OBJECTS_MARKER_FILL_COLOR = PROFILER_RED;
    public static final long   L_LIVE_OBJECTS_INITIAL_VALUE = 100l;

    public static final float  L_LIVE_BYTES_PAINTER_LINE_WIDTH = 3f;
    public static final Color  L_LIVE_BYTES_PAINTER_LINE_COLOR = PROFILER_BLUE;
    public static final Color  L_LIVE_BYTES_PAINTER_FILL_COLOR = null;
    public static final int    L_LIVE_BYTES_MARKER_RADIUS = 5;
    public static final float  L_LIVE_BYTES_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  L_LIVE_BYTES_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  L_LIVE_BYTES_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  L_LIVE_BYTES_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  L_LIVE_BYTES_MARKER_FILL_COLOR = PROFILER_BLUE;
    public static final long   L_LIVE_BYTES_INITIAL_VALUE = 102400l;

    public static final String L_ALLOC_OBJECTS_NAME = A_ALLOC_OBJECTS_NAME;
    public static final float  L_ALLOC_OBJECTS_PAINTER_LINE_WIDTH = 3f;
    public static final Color  L_ALLOC_OBJECTS_PAINTER_LINE_COLOR = PROFILER_GREEN;
    public static final Color  L_ALLOC_OBJECTS_PAINTER_FILL_COLOR = null;
    public static final int    L_ALLOC_OBJECTS_MARKER_RADIUS = 5;
    public static final float  L_ALLOC_OBJECTS_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  L_ALLOC_OBJECTS_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  L_ALLOC_OBJECTS_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  L_ALLOC_OBJECTS_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  L_ALLOC_OBJECTS_MARKER_FILL_COLOR = PROFILER_GREEN;
    public static final long   L_ALLOC_OBJECTS_INITIAL_VALUE = 100l;


    // --- VM Telemetry graphs -------------------------------------------------

    public static final float  HEAP_SIZE_PAINTER_LINE_WIDTH = 0f;
    public static final Color  HEAP_SIZE_PAINTER_LINE_COLOR = null;
    public static final Color  HEAP_SIZE_PAINTER_FILL_COLOR = PROFILER_RED;
    public static final int    HEAP_SIZE_MARKER_RADIUS = 5;
    public static final float  HEAP_SIZE_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  HEAP_SIZE_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  HEAP_SIZE_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  HEAP_SIZE_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  HEAP_SIZE_MARKER_FILL_COLOR = PROFILER_RED;
    public static final long   HEAP_SIZE_INITIAL_VALUE = 5177344l;

    public static final float  USED_HEAP_PAINTER_LINE_WIDTH = 0f;
    public static final Color  USED_HEAP_PAINTER_LINE_COLOR = null;
    public static final Color  USED_HEAP_PAINTER_FILL_COLOR = PROFILER_BLUE;
    public static final int    USED_HEAP_MARKER_RADIUS = 5;
    public static final float  USED_HEAP_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  USED_HEAP_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  USED_HEAP_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  USED_HEAP_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  USED_HEAP_MARKER_FILL_COLOR = PROFILER_BLUE;
    public static final long   USED_HEAP_INITIAL_VALUE = 1048576l;

    public static final Color  HEAP_LIMIT_FILL_COLOR = new Color(220, 220, 220);

    public static final float  SURVGEN_PAINTER_LINE_WIDTH = 3f;
    public static final Color  SURVGEN_PAINTER_LINE_COLOR = PROFILER_RED;
    public static final Color  SURVGEN_PAINTER_FILL_COLOR = null;
    public static final int    SURVGEN_MARKER_RADIUS = 5;
    public static final float  SURVGEN_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  SURVGEN_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  SURVGEN_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  SURVGEN_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  SURVGEN_MARKER_FILL_COLOR = PROFILER_RED;
    public static final long   SURVGEN_INITIAL_VALUE = 2;

    public static final float  GC_TIME_PAINTER_LINE_WIDTH = 3f;
    public static final Color  GC_TIME_PAINTER_LINE_COLOR = PROFILER_BLUE;
    public static final Color  GC_TIME_PAINTER_FILL_COLOR = null;
    public static final int    GC_TIME_MARKER_RADIUS = 5;
    public static final float  GC_TIME_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  GC_TIME_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  GC_TIME_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  GC_TIME_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  GC_TIME_MARKER_FILL_COLOR = PROFILER_BLUE;

    public static final Color  GC_ACTIVITY_FILL_COLOR = new Color(250, 230, 230);

    public static final float  THREADS_PAINTER_LINE_WIDTH = 3f;
    public static final Color  THREADS_PAINTER_LINE_COLOR = PROFILER_RED;
    public static final Color  THREADS_PAINTER_FILL_COLOR = null;
    public static final int    THREADS_MARKER_RADIUS = 5;
    public static final float  THREADS_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  THREADS_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  THREADS_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  THREADS_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  THREADS_MARKER_FILL_COLOR = PROFILER_RED;
    public static final long   THREADS_INITIAL_VALUE = 3;

    public static final float  LOADED_CLASSES_PAINTER_LINE_WIDTH = 3f;
    public static final Color  LOADED_CLASSES_PAINTER_LINE_COLOR = PROFILER_BLUE;
    public static final Color  LOADED_CLASSES_PAINTER_FILL_COLOR = null;
    public static final int    LOADED_CLASSES_MARKER_RADIUS = 5;
    public static final float  LOADED_CLASSES_MARKER_LINE1_WIDTH = 0.75f;
    public static final Color  LOADED_CLASSES_MARKER_LINE1_COLOR = Color.BLACK;
    public static final float  LOADED_CLASSES_MARKER_LINE2_WIDTH = 3.5f;
    public static final Color  LOADED_CLASSES_MARKER_LINE2_COLOR = Color.WHITE;
    public static final Color  LOADED_CLASSES_MARKER_FILL_COLOR = PROFILER_BLUE;
    public static final long   LOADED_CLASSES_INITIAL_VALUE = 732;

}

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

package org.graalvm.visualvm.lib.ui.threads;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ResourceBundle;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.renderer.BaseRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineRenderer extends BaseRenderer {

    private static ResourceBundle BUNDLE() {
        return ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.threads.Bundle"); // NOI18N
    }

    private static final Color TICK_COLOR = new Color(200, 200, 200);

    private static final int BAR_MARGIN = 3;
    private static final int BAR_MARGIN_X2 = BAR_MARGIN * 2;

    private final ViewManager view;
    private ViewManager.RowView rowView;


    public TimelineRenderer(ViewManager view) {
        this.view = view;

        setOpaque(true);
        setHorizontalAlignment(SwingConstants.TRAILING); // #269252

        putClientProperty(ProfilerTable.PROP_NO_HOVER, this);
    }

    public void setValue(Object value, int row) {
        rowView = (ViewManager.RowView)value; // NOTE: rowView can be set to null here!
    }

    public String toString() {
        int lastIndex = rowView == null ? -1 : rowView.getLastIndex();
        return getStateName(lastIndex == -1 ? -1 : rowView.getState(lastIndex));
    }

    public void paint(Graphics g) {
        super.paint(g);

        int w = size.width;
        int h = size.height;

        long time = view.getFirstTimeMark(false);
        long step = view.getTimeMarksStep();

        g.setColor(TICK_COLOR);

        int x = view.getTimePosition(time, false);
        int oldX = x;
        while (x < w) {
            g.drawLine(x + location.x, location.y, x + location.x, h - 1 + location.y);
            time += step;
            x = view.getTimePosition(time, false);
            // Workaround to prevent endless loop until fixed
            if (x <= oldX) break;
            else oldX = x;
        }
        
        if (rowView == null) return;
        
        int i = rowView.getLastIndex();
        if (i == -1) return;
        
        int xx = (i == rowView.getMaxIndex() ? rowView.getMaxPosition() :
                  rowView.getPosition(rowView.getTime(i + 1))) + location.x;
        
        while (i >= 0 && xx >= 0) {
            x = Math.max(0, rowView.getPosition(rowView.getTime(i))) + location.x;
            int ww = xx - x;
            if (ww > 0) {
                Color c = ThreadData.getThreadStateColor(rowView.getState(i));
                if (c != null) {
                    g.setColor(c);
                    g.fillRect(x, BAR_MARGIN + location.y, ww, h - BAR_MARGIN_X2);
                }
                
                xx = x;
            }
            i--;
        }
    }
    
    private static String getStateName(int state) {
        switch (state) {
//            case 0: return "finished";
            case 1: return BUNDLE().getString("TimelineRenderer_ThreadStateRunning");
            case 2: return BUNDLE().getString("TimelineRenderer_ThreadStateSleeping");
            case 3: return BUNDLE().getString("TimelineRenderer_ThreadStateMonitor");
            case 4: return BUNDLE().getString("TimelineRenderer_ThreadStateWait");
            case 5: return BUNDLE().getString("TimelineRenderer_ThreadStatePark");
//            default: return "unknown";
            default: return BUNDLE().getString("TimelineRenderer_ThreadStateFinished");
        }
    }
    
}

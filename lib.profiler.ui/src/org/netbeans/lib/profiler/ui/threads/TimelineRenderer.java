/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.threads;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ResourceBundle;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.renderer.BaseRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineRenderer extends BaseRenderer {
    
    private static ResourceBundle BUNDLE() {
        return ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.threads.Bundle"); // NOI18N
    }
    
    private static final Color TICK_COLOR = new Color(200, 200, 200);
    
    private static final int BAR_MARGIN = 3;
    private static final int BAR_MARGIN_X2 = BAR_MARGIN * 2;
    
    private final ViewManager view;
    private ViewManager.RowView rowView;
    
    
    public TimelineRenderer(ViewManager view) {
        this.view = view;
        
        setOpaque(true);
        
        putClientProperty(ProfilerTable.PROP_NO_HOVER, this);
    }
    
    public void setValue(Object value, int row) {
        rowView = (ViewManager.RowView)value;
    }
    
    public String toString() {
        int lastIndex = rowView.getLastIndex();
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
        
        int i = rowView.getLastIndex();
        if (i != -1) {
            int xx = Math.min(rowView.getMaxPosition(), w);
            while (i >= 0 && xx >= 0) xx = paintState(g, i--, xx, h);
        }
    }
    
    private int paintState(Graphics g, int i, int xx, int h) {
        int x = Math.max(0, rowView.getPosition(rowView.getTime(i)));
        
        Color c = ThreadData.getThreadStateColor(rowView.getState(i));
        if (c != null) {
            g.setColor(c);
            g.fillRect(x + location.x, BAR_MARGIN + location.y, xx - x + 1, h - BAR_MARGIN_X2);
        }
        
        return x - 1;
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

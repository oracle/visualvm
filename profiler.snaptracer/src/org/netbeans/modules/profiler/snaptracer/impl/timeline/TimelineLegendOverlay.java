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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.modules.profiler.snaptracer.impl.swing.ColorIcon;
import org.netbeans.modules.profiler.snaptracer.impl.swing.LabelRenderer;
import org.netbeans.modules.profiler.snaptracer.impl.swing.LegendFont;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineLegendOverlay extends ChartOverlay {

    private final TimelineChart chart;
    private final LabelRenderer painter;


    TimelineLegendOverlay(TimelineChart chart) {
        this.chart = chart;

        painter = new LabelRenderer();
        painter.setFont(new LegendFont());

        int size = painter.getFont().getSize() - 3;
        ColorIcon.setup(size, size,
                LegendFont.FOREGROUND_COLOR, LegendFont.BACKGROUND_COLOR);
    }


    private void setupPainter(String text, Color color) {
        painter.setText(text);
        painter.setIcon(ColorIcon.fromColor(color));
    }


    public void paint(Graphics g) {
        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineChart.Row row = chart.getRow(i);
            ChartContext rowContext = row.getContext();
            SynchronousXYItem[] rowItems = row.getItems();

            int x = 3;
            int y = -1;

            for (SynchronousXYItem rowItem : rowItems) {
                TimelineXYPainter itemPainter =
                        (TimelineXYPainter)chart.getPaintersModel().getPainter(rowItem);
                if (itemPainter.isPainting()) {
                    setupPainter(rowItem.getName(), itemPainter.getDefiningColor());
                    Dimension pd = painter.getPreferredSize();
                    if (y == -1)
                        y = Utils.checkedInt(rowContext.getViewportOffsetY()) +
                            rowContext.getViewportHeight() - pd.height - 1;
                    paint(g, x, y);
                    x += pd.width + 10;
                }
            }
        }
    }

    private void paint(Graphics g, int x, int y) {
        painter.setLocation(x, y + 1);
        painter.setForeground(LegendFont.BACKGROUND_COLOR);
        painter.paint(g);

        painter.setLocation(x, y);
        painter.setForeground(LegendFont.FOREGROUND_COLOR);
        painter.setIcon(ColorIcon.BOTTOM_SHADOW);
        painter.paint(g);
    }

    // --- Peformance tweaks ---------------------------------------------------

    public void invalidate() {}

    public void update(Graphics g) {}

}

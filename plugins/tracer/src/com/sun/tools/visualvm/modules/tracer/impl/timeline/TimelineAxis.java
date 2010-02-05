/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderLabel;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Iterator;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.axis.AxisComponent;
import org.netbeans.lib.profiler.charts.axis.AxisMark;
import org.netbeans.lib.profiler.charts.axis.AxisMarksComputer;
import org.netbeans.lib.profiler.charts.axis.AxisMarksPainter;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineAxis extends JPanel {

    private HeaderPanel painter;
    private AxisComponent axis;

    private int preferredHeight;


    public TimelineAxis(ChartComponent chart, AxisMarksComputer marksComputer,
                        AxisMarksPainter marksPainter) {

        painter = new HeaderPanel();
        axis = new Axis(chart, marksComputer, marksPainter);

        preferredHeight = HeaderLabel.DEFAULT_HEIGHT;

        setLayout(null);

        add(axis);
        add(painter);

    }


    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = preferredHeight;
        return size;
    }


    public void validate() {}

    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        painter.reshape(0, 0, width, height);
        axis.reshape(1, 1, width - 2, height - 2);
    }


    private static class Axis extends AxisComponent {

        private final Paint meshPaint = Utils.checkedColor(new Color(180, 180, 180, 50));
        private final Stroke meshStroke = new BasicStroke(1);

        private final AxisMarksComputer marksComputer;


        public Axis(ChartComponent chart, AxisMarksComputer marksComputer,
                                          AxisMarksPainter marksPainter) {
            super(chart, marksComputer, marksPainter, SwingConstants.NORTH,
                  AxisComponent.MESH_FOREGROUND);
            this.marksComputer = marksComputer;
        }

        protected void paintHorizontalBasis(Graphics g, Rectangle clip,
                                                        Rectangle chartMask) {
        }

        protected int getAxisBasisExtent() {
            return 3;
        }

        protected void paintHorizontalTick(Graphics g, AxisMark mark, int x,
                                       Rectangle clip, Rectangle chartMask) {
            g.setColor(getForeground());
            g.drawLine(x, 1, x, 1 + getAxisBasisExtent());
        }

        protected void paintHorizontalMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
            Iterator<AxisMark> marks = marksComputer.marksIterator(
                                                     chartMask.x, chartMask.x + chartMask.width);

            while (marks.hasNext()) {
                AxisMark mark = marks.next();
                int x = mark.getPosition();

                g.setPaint(meshPaint);
                g.setStroke(meshStroke);
                g.drawLine(x, chartMask.y, x, chartMask.y + chartMask.height);
            }
        }

    }

}

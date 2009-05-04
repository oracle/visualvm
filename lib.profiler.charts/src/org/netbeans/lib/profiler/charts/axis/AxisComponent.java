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

package org.netbeans.lib.profiler.charts.axis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartDecorator;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class AxisComponent extends JComponent {

    public static final int NO_MESH         = 0;
    public static final int MESH_BACKGROUND = 1;
    public static final int MESH_FOREGROUND = 2;

    private final int location;
    private final boolean horizontal;

    private final ChartComponent chart;
    private final AxisMarksComputer marksComputer;
    private final AxisMarksPainter marksPainter;

    private int maxExtent = 0;

    private final Paint meshPaint = Utils.checkedColor(new Color(80, 80, 80, 50));
    private final Stroke meshStroke = new BasicStroke(1);


    // --- Constructors --------------------------------------------------------

    public AxisComponent(ChartComponent chart, AxisMarksComputer marksComputer,
                         AxisMarksPainter marksPainter, int location, int mesh) {

        this.location = location;
        horizontal = location == SwingConstants.NORTH ||
                     location == SwingConstants.SOUTH;

        this.chart = chart;
        this.marksComputer = marksComputer;
        this.marksPainter = marksPainter;

        setOpaque(false);

        chart.addConfigurationListener(new ChartListener());

        if (mesh == MESH_BACKGROUND) chart.addPreDecorator(createMeshPainter());
        else if (mesh == MESH_FOREGROUND) chart.addPostDecorator(createMeshPainter());
    }


    // --- Component paint -----------------------------------------------------

    public void paint(Graphics g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

        final Dimension dim = getPreferredSize();
        final int axisBasisExtent = getAxisBasisExtent();

        Rectangle chartBounds = SwingUtilities.convertRectangle(chart.getParent(),
                                                        chart.getBounds(), this);
        if (horizontal) {
            chartBounds.y = clip.y;
            chartBounds.height = clip.height;
        } else {
            chartBounds.x = clip.x;
            chartBounds.width = clip.width;
        }

        if (marksComputer instanceof AxisMarksComputer.Abstract)
            ((AxisMarksComputer.Abstract)marksComputer).refreshConfiguration();

        paintAxis(g, clip, chartBounds);

        if (horizontal) {
            if (dim.height < maxExtent + axisBasisExtent) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dim.height = maxExtent + axisBasisExtent;
                        setPreferredSize(dim);
                        invalidate();
                        ((JComponent)getParent()).revalidate();
                        getParent().repaint();
                    }
                });
            }
        } else {
            if (dim.width < maxExtent + axisBasisExtent) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dim.width = maxExtent + axisBasisExtent;
                        setPreferredSize(dim);
                        invalidate();
                        ((JComponent)getParent()).revalidate();
                        getParent().repaint();
                    }
                });
            }
        }
    }


    // --- Mesh painter --------------------------------------------------------

    protected void paintAxisMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
        if (horizontal) paintHorizontalMesh(g, clip, chartMask);
        else paintVerticalMesh(g, clip, chartMask);
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

    protected void paintVerticalMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
        Iterator<AxisMark> marks = marksComputer.marksIterator(
                                             chartMask.y, chartMask.y + chartMask.height);

        while (marks.hasNext()) {
            AxisMark mark = marks.next();
            int y = mark.getPosition();

            g.setPaint(meshPaint);
            g.setStroke(meshStroke);
            g.drawLine(chartMask.x, y, chartMask.x + chartMask.width, y);
        }
    }


    // --- Axis contents painter -----------------------------------------------

    protected void paintAxis(Graphics g, Rectangle clip, Rectangle chartMask) {
        if (horizontal) paintHorizontalAxis(g, clip, chartMask);
        else paintVerticalAxis(g, clip, chartMask);
    }

    protected int getAxisBasisExtent() {
        return 5;
    }

    protected void paintHorizontalBasis(Graphics g, Rectangle clip, Rectangle chartMask) {
        Rectangle dirty = clip.intersection(chartMask);
        g.setColor(getForeground());
        if (location == SwingConstants.NORTH) {
            g.drawLine(dirty.x - 1, getHeight() - 1, dirty.x + dirty.width, getHeight() - 1);
        } else {
            g.drawLine(dirty.x, 0, dirty.x + dirty.width, 0);
        }
    }

    protected void paintHorizontalTick(Graphics g, AxisMark mark, int x,
                                       Rectangle clip, Rectangle chartMask) {
        g.setColor(getForeground());
        if (location == SwingConstants.NORTH) {
            g.drawLine(x, getHeight() - 2 - getAxisBasisExtent(), x, getHeight() - 2);
        } else {
            g.drawLine(x, 1, x, 1 + getAxisBasisExtent());
        }
    }

    protected void paintHorizontalAxis(Graphics g, Rectangle clip, Rectangle chartMask) {
        paintHorizontalBasis(g, clip, chartMask);

        int viewStart = SwingUtilities.convertPoint(this, chartMask.x, 0, chart).x - 1; // -1: extra 1px for axis
        int viewEnd = viewStart + chartMask.width + 2; // +2 extra 1px + 1px for axis

        Iterator<AxisMark> marks = marksComputer.marksIterator(viewStart, viewEnd);

        int lZeroOffset = chart.isRightBased() ? 0 : 1;
        int rZeroOffset = chart.isRightBased() ? 1 : 0;

        while (marks.hasNext()) {
            AxisMark mark = marks.next();

            int x = SwingUtilities.convertPoint(chart, mark.getPosition(), 0, this).x;

            if (x < chartMask.x - lZeroOffset ||
                x >= chartMask.x + chartMask.width + rZeroOffset) continue;

            Component painter = marksPainter.getPainter(mark);
            painter.setSize(painter.getPreferredSize());
            int markHeight = painter.getHeight();
            int markOffsetX = painter.getWidth() / 2;

            if (x + markOffsetX < clip.x ||
                x - markOffsetX >= clip.x + clip.width) continue;

            maxExtent = Math.max(maxExtent, markHeight);

            paintHorizontalTick(g, mark, x, clip, chartMask);

            g.setColor(getForeground());
            if (location == SwingConstants.NORTH) {
                g.translate(x - markOffsetX, 0);
                painter.paint(g);
                g.translate(-x + markOffsetX, 0);
            } else {
                g.translate(x - markOffsetX, getAxisBasisExtent());
                painter.paint(g);
                g.translate(-x + markOffsetX, -getAxisBasisExtent());
            }
        }
    }

    protected void paintVerticalBasis(Graphics g, Rectangle clip, Rectangle chartMask) {
        g.setColor(getForeground());
        if (location == SwingConstants.WEST) {
            g.drawLine(getWidth() - 1, chartMask.y - 1, getWidth() - 1, chartMask.y + chartMask.height);
        } else {
            g.drawLine(0, chartMask.y, 0, chartMask.y + chartMask.height);
        }
    }

    protected void paintVerticalTick(Graphics g, AxisMark mark, int y,
                                       Rectangle clip, Rectangle chartMask) {
        g.setColor(getForeground());
        if (location == SwingConstants.WEST) {
            g.drawLine(getWidth() - 2 - getAxisBasisExtent(), y, getWidth() - 2, y);
        } else {
            g.drawLine(1, y, 1 + getAxisBasisExtent(), y);
        }
    }

    protected void paintVerticalAxis(Graphics g, Rectangle clip, Rectangle chartMask) {
        paintVerticalBasis(g, clip, chartMask);

        int viewStart = SwingUtilities.convertPoint(this, 0, chartMask.y, chart).y;
        int viewEnd = viewStart + chartMask.height;

        Iterator<AxisMark> marks = marksComputer.marksIterator(viewStart, viewEnd);

        int tZeroOffset = chart.isBottomBased() ? 0 : 1;
        int bZeroOffset = chart.isBottomBased() ? 1 : 0;

        int currentExtent = maxExtent;

        while (marks.hasNext()) {
            AxisMark mark = marks.next();

            int y = SwingUtilities.convertPoint(chart, 0, mark.getPosition(), this).y;

            if (y < chartMask.y - tZeroOffset ||
                y >= chartMask.y + chartMask.height + bZeroOffset) continue;

            Component painter = marksPainter.getPainter(mark);
            painter.setSize(painter.getPreferredSize());
            int markWidth = painter.getWidth();
            int markOffsetY = painter.getHeight() / 2;

            if (y + markOffsetY < clip.y ||
                y - markOffsetY >= clip.y + clip.height) continue;

            maxExtent = Math.max(maxExtent, markWidth);

            paintVerticalTick(g, mark, y, clip, chartMask);

            g.setColor(getForeground());
            if (location == SwingConstants.WEST) {
                g.translate(currentExtent - markWidth, y - markOffsetY);
                painter.paint(g);
                g.translate(-currentExtent + markWidth, -y + markOffsetY);
            } else {
                g.translate(getAxisBasisExtent(), y - markOffsetY);
                painter.paint(g);
                g.translate(-getAxisBasisExtent(), -y + markOffsetY);
            }
        }
    }


    private ChartDecorator createMeshPainter() {
        return new ChartDecorator() {
            public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
                paintAxisMesh(g, dirtyArea, dirtyArea);
            }
        };
    }


    private class ChartListener extends ChartConfigurationListener.Adapter {

        private boolean updateAxis;

        public void contentsWillBeUpdated(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY) {

            if (marksComputer instanceof AxisMarksComputer.Abstract) {
                AxisMarksComputer.Abstract computer =
                        (AxisMarksComputer.Abstract)marksComputer;
                updateAxis = computer.refreshConfiguration();
            } else {
                updateAxis = true;
            }
        }

        public void contentsUpdated(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY,
                                int shiftX, int shiftY) {

            if (!updateAxis) {
                if (horizontal) {
                    updateAxis = shiftX != 0 ||
                            lastOffsetX != offsetX ||
                             lastScaleX != scaleX;
                } else {
                    updateAxis = shiftY != 0 ||
                            lastOffsetY != offsetY ||
                             lastScaleY != scaleY;
                }
            }

            if (updateAxis) repaint();

        }

    }

}

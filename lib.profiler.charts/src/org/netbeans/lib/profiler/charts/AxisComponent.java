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

import org.netbeans.lib.profiler.charts.AxisMarksComputer.Mark;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.AxisMarksComputer.BytesMark;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
public class AxisComponent extends JComponent implements ChartDecorator {

    public static final int NO_MESH         = 0;
    public static final int MESH_BACKGROUND = 1;
    public static final int MESH_FOREGROUND = 2;

    private final int location;
    private boolean horizontal;

    private final ChartComponent chart;
    private final AxisMarksComputer marksComputer;
    private final MarkValuePainter marksPainter;

    private int maxExtent;

    private final Paint meshPaint = Utils.checkedColor(new Color(80, 80, 80, 50));
    private final Stroke meshStroke = new BasicStroke(1);


    public AxisComponent(ChartComponent chart, AxisMarksComputer marksComputer,
                         MarkValuePainter marksPainter, int location, int mesh) {

        this.location = location;
        horizontal = location == SwingConstants.NORTH ||
                     location == SwingConstants.SOUTH;

        this.chart = chart;
        this.marksComputer = marksComputer;
        this.marksPainter = marksPainter;

        setOpaque(false);

        if (horizontal) {
            setPreferredSize(new Dimension(1, 10));
        } else {
            setPreferredSize(new Dimension(10, 1));
        }

        chart.addConfigurationListener(new ChartListener());

        if (mesh == MESH_BACKGROUND) chart.addPreDecorator(this);
        else if (mesh == MESH_FOREGROUND) chart.addPostDecorator(this);
    }
    

    public void paint(Graphics g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

        Rectangle chartBounds = SwingUtilities.convertRectangle(chart.getParent(), chart.getBounds(), this);

        if (horizontal) {
            chartBounds.y = clip.y;
            chartBounds.height = clip.height;
            paintHorizontalAxis(g, clip, chartBounds);
            if (getPreferredSize().height < maxExtent + 20) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setPreferredSize(new Dimension(getPreferredSize().width, maxExtent + 20));
                        invalidate();
                        ((JComponent)getParent()).revalidate();
                        getParent().repaint();
                    }
                });
            }
        } else {
            chartBounds.x = clip.x;
            chartBounds.width = clip.width;
            paintVerticalAxis(g, clip, chartBounds);
            if (getPreferredSize().width < maxExtent + 20) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setPreferredSize(new Dimension(maxExtent + 20, getPreferredSize().height));
                        invalidate();
                        ((JComponent)getParent()).revalidate();
                        getParent().repaint();
                    }
                });
            }
        }
    }

    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        if (horizontal) {
            paintHorizontalAxisMesh(g, dirtyArea, dirtyArea);
        } else {
            paintVerticalAxisMesh(g, dirtyArea, dirtyArea);
        }
    }


    private void paintHorizontalAxis(Graphics g, Rectangle clip, Rectangle chartMask) {

        g.setColor(getForeground());

        if (location == SwingConstants.NORTH) {
            g.drawLine(chartMask.x - 1, getHeight() - 1, chartMask.x + chartMask.width, getHeight() - 1);
        } else {
            g.drawLine(chartMask.x, 0, chartMask.x + chartMask.width, 0);
        }

        int viewStart = SwingUtilities.convertPoint(this, chartMask.x, 0, chart).x - 1; // -1: extra 1px for axis
        int viewEnd = viewStart + chartMask.width + 2; // +2 extra 1px + 1px for axis

        Iterator<AxisMarksComputer.Mark> marks = marksComputer.marksIterator(
                                                 viewStart, viewEnd);

        int lZeroOffset = chart.isRightBased() ? 0 : 1;
        int rZeroOffset = chart.isRightBased() ? 1 : 0;

        while (marks.hasNext()) {
            AxisMarksComputer.Mark mark = marks.next();
//            int x = ChartContext.getCheckedIntValue(chart.getChartContext().getViewX(mark.getValue()));
            int x = SwingUtilities.convertPoint(chart, mark.getPosition(), 0, this).x;

            if (x < chartMask.x - lZeroOffset ||
                x >= chartMask.x + chartMask.width + rZeroOffset) continue;

            int height = 10;
            Component painter = marksPainter.getPainter(mark);
            painter.setSize(painter.getPreferredSize());
            int markHeight = painter.getHeight();
            int markOffsetX = painter.getWidth() / 2;
            maxExtent = Math.max(maxExtent, markHeight);

            g.setColor(getForeground());

            if (location == SwingConstants.NORTH) {
                g.drawLine(x, getHeight() - 2, x, getHeight() - 2 - height);

                g.translate(x - markOffsetX, getHeight() - 7 - height - markHeight);
                painter.paint(g);
                g.translate(-x + markOffsetX, -getHeight() + 7 + height + markHeight);
            } else {
                g.drawLine(x, 1, x, 1 + height - 4);

                g.translate(x - markOffsetX, 1 + height);
                painter.paint(g);
                g.translate(-x + markOffsetX, -1 - height);
            }
        }
    }

    private void paintHorizontalAxisMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
        Iterator<AxisMarksComputer.Mark> marks = marksComputer.marksIterator(
                                                 chartMask.x, chartMask.x + chartMask.width);

        while (marks.hasNext()) {
            AxisMarksComputer.Mark mark = marks.next();
            int x = ChartContext.getCheckedIntValue(chart.getChartContext().getViewX(mark.getValue()));

            g.setPaint(meshPaint);
            g.setStroke(meshStroke);
            g.drawLine(x, chartMask.y, x, chartMask.y + chartMask.height);
        }
    }

    private void paintVerticalAxis(Graphics g, Rectangle clip, Rectangle chartMask) {

        g.setColor(getForeground());
        
        if (location == SwingConstants.WEST) {
            g.drawLine(getWidth() - 1, chartMask.y - 1, getWidth() - 1, chartMask.y + chartMask.height);
        } else {
            g.drawLine(0, chartMask.y, 0, chartMask.y + chartMask.height);
        }

        int viewStart = SwingUtilities.convertPoint(this, 0, chartMask.y, chart).y;
        int viewEnd = viewStart + chartMask.height;

        Iterator<AxisMarksComputer.Mark> marks = marksComputer.marksIterator(
                                                 viewStart, viewEnd);

        int tZeroOffset = chart.isBottomBased() ? 0 : 1;
        int bZeroOffset = chart.isBottomBased() ? 1 : 0;

        while (marks.hasNext()) {
            AxisMarksComputer.Mark mark = marks.next();
//            System.err.println(">>> Mark value: " + mark.getValue() + ", mark position: " + mark.getPosition());
//            int y = ChartContext.getCheckedIntValue(chart.getChartContext().getViewY(mark.getValue()));
//            int y = ChartContext.getCheckedIntValue(chart.getChartContext().getViewY(mark.getValue()));
            int y = SwingUtilities.convertPoint(chart, 0, mark.getPosition(), this).y;

            if (y < chartMask.y - tZeroOffset ||
                y >= chartMask.y + chartMask.height + bZeroOffset) continue;

            int width = 6;
            Component painter = marksPainter.getPainter(mark);
            painter.setSize(painter.getPreferredSize());
            int markWidth = painter.getWidth();
            int markOffsetY = painter.getHeight() / 2;
            maxExtent = Math.max(maxExtent, markWidth);

            g.setColor(getForeground());

            if (location == SwingConstants.WEST) {
                g.drawLine(getWidth() - 2, y, getWidth() - 2 - width, y);

                g.translate(getWidth() - markWidth - 15, y - markOffsetY);
                painter.paint(g);
                g.translate(-getWidth() + markWidth + 15, -y + markOffsetY);
            } else {
                g.drawLine(1, y, 1 + width, y);

                g.translate(width + 5, y - markOffsetY);
                painter.paint(g);
                g.translate(-width - 5, -y + markOffsetY);
            }
        }
    }

    private void paintVerticalAxisMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {

        Iterator<AxisMarksComputer.Mark> marks = marksComputer.marksIterator(
                                                 chartMask.y, chartMask.y + chartMask.height);

        while (marks.hasNext()) {
            AxisMarksComputer.Mark mark = marks.next();
            int y = ChartContext.getCheckedIntValue(chart.getChartContext().getViewY(mark.getValue()));

            g.setPaint(meshPaint);
            g.setStroke(meshStroke);
            g.drawLine(chartMask.x, y, chartMask.x + chartMask.width, y);
        }
    }


    private class ChartListener implements ChartConfigurationListener {

        public void offsetChanged(long oldOffsetX, long oldOffsetY,
                                  long newOffsetX, long newOffsetY) {}

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {}

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {}

        public void viewChanged(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY,
                                int shiftX, int shiftY) {

            maxExtent = 0;
//            paintImmediately(0, 0, getWidth(), getHeight());
            repaint(); // TODO: update just the marks computer!!!
            
//            System.err.println("viewChanged");
//            if (scaleX != lastScaleX || scaleY != lastScaleY) {
//                repaint();
////                paintImmediately(0, 0, getWidth(), getHeight());
//            } else if (horizontal) {
//                if (offsetX != lastOffsetX) repaint();
////                if (offsetX != lastOffsetX) paintImmediately(0, 0, getWidth(), getHeight());
//            } else {
//                if (offsetY != lastOffsetY) repaint();
////                if (offsetY != lastOffsetY) paintImmediately(0, 0, getWidth(), getHeight());
//            }
        }

    }


    public static interface MarkValuePainter {

        public Component getPainter(AxisMarksComputer.Mark mark);

    }


    public static class SimplePainter extends JLabel implements MarkValuePainter {

        public Component getPainter(Mark mark) {
            setText(Long.toString(mark.getValue()));
            return this;
        }

    }

    public static class BytesPainter extends JLabel implements MarkValuePainter {

        public Component getPainter(Mark mark) {
            BytesMark bmark = (BytesMark)mark;

            long value = bmark.getValue();
            int radix = bmark.getRadix();

            value /= Math.pow(1024, radix);

            String units = "";
            switch (radix) {
                case 0:
                    units = "B";
                    break;
                case 1:
                    units = "KB";
                    break;
                case 2:
                    units = "MB";
                    break;
                case 3:
                    units = "GB";
                    break;
                case 4:
                    units = "TB";
                    break;
                case 5:
                    units = "PB";
                    break;
                default:
                    units = "";

            }

            setText(Long.toString(value) + " " + units);
            return this;
        }

    }

    public static class PercentPainter extends JLabel implements MarkValuePainter {

        private final long minValue;
        private final long maxValue;


        public PercentPainter(long minValue, long maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }


        public Component getPainter(Mark mark) {
            long value = mark.getValue();
            float relValue = (float)(value - minValue) / (float)maxValue * 100f;
            setText((int)relValue + "%"); // NOI18N
            return this;
        }

    }


    public static class TimestampPainter extends JLabel implements MarkValuePainter {

        private final SimpleDateFormat format;


        public TimestampPainter(String format) {
            this.format = new SimpleDateFormat(format);
        }


        public Component getPainter(Mark mark) {
            setText(format.format(new Date(mark.getValue())));
            return this;
        }

//        public void paint(Graphics g) {
//            Graphics2D g2 = (Graphics2D)g;
//            AffineTransform transform = g2.getTransform();
//            g2.rotate(Math.PI / 4d);
//            super.paint(g);
//            g2.setTransform(transform);
//        }

    }

}

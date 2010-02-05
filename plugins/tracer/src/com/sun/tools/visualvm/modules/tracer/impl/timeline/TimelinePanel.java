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
import com.sun.tools.visualvm.modules.tracer.impl.probes.ProbePresenter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.axis.TimeMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelinePanel extends JPanel {
    
    public TimelinePanel(TimelineSupport support) {
        super(new BorderLayout());
        setOpaque(false);

        ProbesPanel probesPanel = new ProbesPanel(support);
        ChartPanel chartPanel = new ChartPanel(support.getChart());

        add(probesPanel, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);
    }

    private static class ProbesPanel extends JPanel {

        public ProbesPanel(final TimelineSupport support) {
            final TimelineChart chart = support.getChart();

            final JPanel listPanel = new JPanel(new VerticalTimelineLayout(chart));
            listPanel.setOpaque(false);
        //        listPanel.setOpaque(true);
        //        listPanel.setBackground(new Color(245, 245, 245));

            final JViewport viewport = new JViewport();
            viewport.setOpaque(false);
        //        viewport.setOpaque(true);
        //        viewport.setBackground(new Color(245, 245, 245));
            viewport.setView(listPanel);
            viewport.setViewPosition(new Point(0, 0));

            chart.addConfigurationListener(new ChartConfigurationListener.Adapter() {
                public void contentsWillBeUpdated(long offsetX, final long offsetY,
                            double scaleX, double scaleY,
                            long lastOffsetX, final long lastOffsetY,
                            double lastScaleX, double lastScaleY) {

                    final int chartOffsetY = Utils.checkedInt(chart.getOffsetY());
                    final int chartHeight = Utils.checkedInt(chart.getChartContext().getViewHeight());

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Dimension size = listPanel.getSize();
                            if (size.height != chartHeight) {
                                size.height = chartHeight;
                                listPanel.setSize(size);
                            }

                            if (lastOffsetY != offsetY) {
                                viewport.setViewPosition(new Point(0, chartOffsetY));
                            }
                        }
                    });
                }
            });

            final JPanel bottomPanel = new JPanel(null);
            bottomPanel.setBackground(Color.WHITE);
            bottomPanel.setPreferredSize(new Dimension(1, new JScrollBar(JScrollBar.HORIZONTAL).getPreferredSize().height));
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            setOpaque(false);
            setLayout(new BorderLayout());
            add(new HeaderLabel("Probes"), BorderLayout.NORTH);
            add(viewport, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);

            setOpaque(true);
            setBackground(new Color(247, 247, 247));

            chart.addRowListener(new TimelineChart.RowListener() {
                public void rowAdded(TimelineChart.Row row) {
                    listPanel.add(new ProbePresenter(support.getProbe(row)), row.getIndex());
                }
                public void rowRemoved(TimelineChart.Row row) {
                    listPanel.remove(row.getIndex());
                }
            });
        }

    }

    private static class ChartPanel extends JPanel {

        public ChartPanel(TimelineChart chart) {
            setLayout(new BorderLayout());
            chart.setBackground(Color.WHITE);

            chart.setScale(0.02d, 1);
            chart.setOffset(0, 0);
            chart.addPreDecorator(new RowPreDecorator(chart));
            chart.addPostDecorator(new RowPostDecorator(chart));

            TimeMarksPainter marksPainter = new TimeMarksPainter() {
                public Dimension getPreferredSize() {
                    Dimension size = super.getPreferredSize();
                    size.height = HeaderLabel.DEFAULT_HEIGHT;
                    return size;
                }
            };
            Font font = marksPainter.getFont();
            marksPainter.setFont(font.deriveFont(Font.PLAIN, font.getSize2D() - 1));

            Timeline timeline = ((SynchronousXYItemsModel)chart.getItemsModel()).getTimeline();
            TimelineMarksComputer marksComputer = new TimelineMarksComputer(timeline,
                    chart.getChartContext(), SwingConstants.HORIZONTAL);
            TimelineAxis axis = new TimelineAxis(chart, marksComputer, marksPainter);

            JScrollBar hScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
            JScrollBar vScrollBar = new JScrollBar(JScrollBar.VERTICAL);

            chart.attachHorizontalScrollBar(hScrollBar);
            chart.attachVerticalScrollBar(vScrollBar);

            add(axis, BorderLayout.NORTH);
            add(chart, BorderLayout.CENTER);
            add(vScrollBar, BorderLayout.EAST);
            add(hScrollBar, BorderLayout.SOUTH);
        }

    }

}

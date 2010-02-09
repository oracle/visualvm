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
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

        new RowMouseHandler(support.getChart(), probesPanel.getMouseTarget(), this).register();
    }

    private static class ProbesPanel extends JPanel {

        private final JViewport viewport;
        private final HeaderButton increaseB;
        private final HeaderButton decreaseB;
        private final HeaderButton resetB;

        public ProbesPanel(final TimelineSupport support) {
            final TimelineChart chart = support.getChart();

            final JPanel listPanel = new JPanel(new VerticalTimelineLayout(chart));
            listPanel.setOpaque(false);

            viewport = new JViewport();
            viewport.setOpaque(false);
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

            final JPanel bottomPanel = new JPanel(new GridLayout(1, 3));
            bottomPanel.setPreferredSize(new Dimension(100, new JScrollBar(JScrollBar.HORIZONTAL).getPreferredSize().height));
            bottomPanel.setOpaque(false);

            increaseB = new HeaderButton("+") {
                protected void performAction(ActionEvent e) {
                    chart.increaseRowHeights((e.getModifiers() & Toolkit.
                            getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
                }
            };
            increaseB.setToolTipText("Increase rows height");
            bottomPanel.add(increaseB);

            decreaseB = new HeaderButton("-") {
                protected void performAction(ActionEvent e) {
                    chart.decreaseRowHeights((e.getModifiers() & Toolkit.
                            getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
                }
            };
            decreaseB.setToolTipText("Decrease rows height");
            bottomPanel.add(decreaseB);

            resetB = new HeaderButton("=") {
                protected void performAction(ActionEvent e) {
                    chart.resetRowHeights();
                }
            };
            resetB.setToolTipText("Reset rows height");
            bottomPanel.add(resetB);

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
                    refreshButtons(true);
                }
                public void rowRemoved(TimelineChart.Row row) {
                    listPanel.remove(row.getIndex());
                    refreshButtons(chart.hasRows());
                }
            });

            refreshButtons(chart.hasRows());
        }

        Component getMouseTarget() { return viewport; }

        private void refreshButtons(boolean enabled) {
            increaseB.setEnabled(enabled);
            decreaseB.setEnabled(enabled);
            resetB.setEnabled(enabled);
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


    private static class RowMouseHandler extends MouseAdapter {

        private static final int RESIZE_RANGE = 3;

        private final TimelineChart chart;
        private final Component mouseTarget;
        private final Component repaintTarget;

        private int baseY;
        private int baseHeight;
        private TimelineChart.Row draggingRow;


        public RowMouseHandler(TimelineChart chart, Component mouseTarget,
                               Component repaintTarget) {
            this.chart = chart;
            this.mouseTarget = mouseTarget;
            this.repaintTarget = repaintTarget;
        }


        public void register() {
            chart.addMouseListener(this);
            chart.addMouseMotionListener(this);
            mouseTarget.addMouseListener(this);
            mouseTarget.addMouseMotionListener(this);
        }


        public void mousePressed(MouseEvent e) {
            updateRowState(e, true);
            updateCursor();
        }

        public void mouseReleased(MouseEvent e) {
            updateRowState(e, false);
            updateCursor();
        }

        public void mouseMoved(MouseEvent e) {
            updateRowState(e, false);
            updateCursor();
        }

        public void mouseDragged(MouseEvent e){
            if (draggingRow != null) {
                boolean checkStep = (e.getModifiers() & Toolkit.getDefaultToolkit().
                                     getMenuShortcutKeyMask()) == 0;
                chart.setRowHeight(draggingRow.getIndex(), baseHeight + e.getY() - baseY, checkStep);
            }
        }


        private void updateRowState(MouseEvent e, boolean updateSelection) {
            baseY = e.getY();
            draggingRow = chart.getNearestRow(baseY, RESIZE_RANGE, true);
            if (draggingRow != null) {
                baseHeight = draggingRow.getHeight();
            } else if (updateSelection) {
//                TimelineChart.Row row = chart.getRowAt(baseY);
//                chart.setSelectedRow(row == null ? -1 : row.getIndex());
//                repaintTarget.repaint();
            }
        }

        private void updateCursor() {
            if (draggingRow != null) {
                Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                chart.setCursor(resizeCursor);
                mouseTarget.setCursor(resizeCursor);
            } else {
                Cursor defaultCursor = Cursor.getDefaultCursor();
                chart.setCursor(defaultCursor);
                mouseTarget.setCursor(defaultCursor);
            }
        }

    }

}

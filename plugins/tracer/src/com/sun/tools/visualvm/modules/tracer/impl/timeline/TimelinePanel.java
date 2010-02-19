/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.axis.TimeMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TimelinePanel extends JPanel {

    private final ChartPanel chartPanel;

    
    public TimelinePanel(TimelineSupport support) {
        super(new BorderLayout());
        setOpaque(false);

        ProbesPanel probesPanel = new ProbesPanel(support);
        chartPanel = new ChartPanel(support.getChart());

        add(probesPanel, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);

        new RowMouseHandler(support.getChart(), probesPanel.getMouseTarget(), this).register();
    }


    public void reset() {
        chartPanel.resetPanel();
    }


    private static class ProbesPanel extends JPanel {

        private final JPanel listPanel;
        private final JViewport viewport;
        private final HeaderButton increaseB;
        private final HeaderButton decreaseB;
        private final HeaderButton resetB;

        ProbesPanel(final TimelineSupport support) {
            final TimelineChart chart = support.getChart();

            listPanel = new JPanel(new VerticalTimelineLayout(chart)) {
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.height = Utils.checkedInt(chart.getChartContext().getViewHeight());
                    return d;
                }
            };
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

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (lastOffsetY != offsetY)
                                viewport.setViewPosition(new Point(0, Utils.
                                                         checkedInt(offsetY)));
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
                public void rowsAdded(List<TimelineChart.Row> rows) {
                    for (TimelineChart.Row row : rows)
                        listPanel.add(new ProbePresenter(support.getProbe(row)),
                                                         row.getIndex());
                    syncListPanel();
                    refreshButtons(true);
                }
                public void rowsRemoved(List<TimelineChart.Row> rows) {
                    for (TimelineChart.Row row : rows)
                        listPanel.remove(row.getIndex());
                    syncListPanel();
                    refreshButtons(chart.hasRows());
                }
                public void rowsResized(List<TimelineChart.Row> rows) {
                    syncListPanel();
                }
            });

            refreshButtons(chart.hasRows());
        }

        private void syncListPanel() {
            listPanel.doLayout();
            listPanel.repaint();
        }

        Component getMouseTarget() { return viewport; }

        private void refreshButtons(boolean enabled) {
            increaseB.setEnabled(enabled);
            decreaseB.setEnabled(enabled);
            resetB.setEnabled(enabled);
        }

    }

    private static class ChartPanel extends JPanel {

        private final TimelineChart chart;

        ChartPanel(TimelineChart chart) {
            this.chart = chart;

            chart.setBackground(Color.WHITE);
            chart.addPreDecorator(new RowBoundsDecorator(chart));
//            chart.addPreDecorator(new RowPostDecorator(chart));
            chart.addPostDecorator(new RowGradientDecorator(chart));
            
//            chart.addOverlayComponent(new RowUnitsOverlay(chart));

            TimelineSelectionOverlay selectionOverlay = new TimelineSelectionOverlay();
            chart.addOverlayComponent(selectionOverlay);
            selectionOverlay.registerChart(chart);
            enableSelection(chart);

            TimeMarksPainter marksPainter = new TimeMarksPainter() {
                public Dimension getPreferredSize() {
                    Dimension size = super.getPreferredSize();
                    size.height = HeaderLabel.DEFAULT_HEIGHT;
                    return size;
                }
            };
            Font font = marksPainter.getFont();
            marksPainter.setFont(font.deriveFont(Font.PLAIN, font.getSize() - 2));

            Timeline timeline = ((SynchronousXYItemsModel)chart.getItemsModel()).getTimeline();
            TimelineMarksComputer marksComputer = new TimelineMarksComputer(timeline,
                    chart.getChartContext(), SwingConstants.HORIZONTAL);
            TimelineAxis axis = new TimelineAxis(chart, marksComputer, marksPainter);

            JScrollBar hScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
            JScrollBar vScrollBar = new JScrollBar(JScrollBar.VERTICAL);

            chart.attachHorizontalScrollBar(hScrollBar);
            chart.attachVerticalScrollBar(vScrollBar);

            setLayout(new BorderLayout());
            add(axis, BorderLayout.NORTH);
            add(chart, BorderLayout.CENTER);
            add(vScrollBar, BorderLayout.EAST);
            add(hScrollBar, BorderLayout.SOUTH);

            resetPanel();
        }

        private void resetPanel() {
            chart.setScale(0.02d, 1);
            chart.setOffset(0, 0);
        }

    }


    private static void enableSelection(TimelineChart chart) {
        chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_EACH_NEAREST);
        chart.getSelectionModel().setMoveMode(ChartSelectionModel.SELECTION_LINE_V);
    }

    private static void disableSelection(TimelineChart chart) {
        chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_NONE);
        chart.getSelectionModel().setMoveMode(ChartSelectionModel.SELECTION_NONE);
    }


    private static class RowMouseHandler extends MouseAdapter {

        private static final int RESIZE_RANGE = 3;

        private final TimelineChart chart;
        private final Component mouseTarget;
        private final Component repaintTarget;

        private int baseY;
        private int baseHeight;
        private TimelineChart.Row draggingRow;


        RowMouseHandler(TimelineChart chart, Component mouseTarget,
                               Component repaintTarget) {
            this.chart = chart;
            this.mouseTarget = mouseTarget;
            this.repaintTarget = repaintTarget;
        }


        void register() {
            chart.addMouseListener(this);
            chart.addMouseMotionListener(this);
            mouseTarget.addMouseListener(this);
            mouseTarget.addMouseMotionListener(this);
        }


        public void mousePressed(MouseEvent e) {
            updateRowState(e, true);
            if (draggingRow != null) disableSelection(chart);
            updateCursor();
        }

        public void mouseReleased(MouseEvent e) {
            if (draggingRow != null) enableSelection(chart);
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

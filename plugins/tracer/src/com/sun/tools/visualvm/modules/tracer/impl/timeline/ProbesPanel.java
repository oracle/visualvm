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

import com.sun.tools.visualvm.modules.tracer.impl.probes.ProbePresenter;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderButton;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderLabel;
import com.sun.tools.visualvm.modules.tracer.impl.swing.ScrollBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
class ProbesPanel extends JPanel {

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
        viewport.setOpaque(true);
        viewport.setBackground(new Color(247, 247, 247));
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
        bottomPanel.setPreferredSize(new Dimension(100, new ScrollBar(JScrollBar.
                                     HORIZONTAL).getPreferredSize().height));
        bottomPanel.setOpaque(false);

        increaseB = new HeaderButton("+") {
            protected void performAction(ActionEvent e) {
                chart.increaseRowHeights((e.getModifiers() & Toolkit.getDefaultToolkit().
                                         getMenuShortcutKeyMask()) == 0);
            }
        };
        increaseB.setToolTipText("Increase rows height");
        bottomPanel.add(increaseB);

        decreaseB = new HeaderButton("-") {
            protected void performAction(ActionEvent e) {
                chart.decreaseRowHeights((e.getModifiers() & Toolkit.getDefaultToolkit().
                                         getMenuShortcutKeyMask()) == 0);
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

    Component getMouseTarget() {
        return viewport;
    }

    private void refreshButtons(boolean enabled) {
        increaseB.setEnabled(enabled);
        decreaseB.setEnabled(enabled);
        resetB.setEnabled(enabled);
    }
}

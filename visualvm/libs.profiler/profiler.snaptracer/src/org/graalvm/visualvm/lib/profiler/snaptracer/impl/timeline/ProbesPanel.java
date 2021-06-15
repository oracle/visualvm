/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbe;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.icons.TracerIcons;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.probes.ProbePresenter;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing.HeaderButton;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing.HeaderLabel;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing.ScrollBar;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class ProbesPanel extends JPanel {

    private final ListPanel listPanel;
    private final JViewport viewport;
    private final HeaderButton increaseB;
    private final HeaderButton decreaseB;
    private final HeaderButton resetB;

    @NbBundle.Messages({
        "TOOLTIP_IncreaseRowsHeight=Increase rows height",
        "TOOLTIP_DecreaseRowsHeight=Decrease rows height",
        "TOOLTIP_ResetRowsHeight=Reset rows height",
        "LBL_Probes=Probes"
    })
    ProbesPanel(final TimelineSupport support) {
        final TimelineChart chart = support.getChart();

        listPanel = new ListPanel(new VerticalTimelineLayout(chart)) {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = Utils.checkedInt(chart.getChartContext().getViewHeight());
                return d;
            }
            protected void updateSelection() {
                int count = getComponentCount();
                for (int i = 0; i < count; i++)
                    ((ProbePresenter)getComponent(i)).setSelected(
                            chart.isRowSelected(chart.getRow(i)));
            }
        };

        viewport = new JViewport() {
            public String getToolTipText(MouseEvent event) {
                Point p = event.getPoint();
                p.y += getViewPosition().y;
                return listPanel.getToolTipText(p);
            }
        };
        ToolTipManager.sharedInstance().registerComponent(viewport);
        viewport.setOpaque(true);
        viewport.setBackground(new Color(247, 247, 247));
        viewport.setView(listPanel);
        viewport.setViewPosition(new Point(0, 0));
        final ViewportUpdater updater = new ViewportUpdater(viewport);
        chart.addConfigurationListener(new ChartConfigurationListener.Adapter() {
            public void contentsWillBeUpdated(long offsetX, final long offsetY,
                                              double scaleX, double scaleY,
                                              long lastOffsetX, final long lastOffsetY,
                                              double lastScaleX, double lastScaleY) {
                if (lastOffsetY != offsetY)
                    SwingUtilities.invokeLater(updater.forPoint(new Point(
                            0, Utils.checkedInt(offsetY))));
            }
        });
        final JPanel bottomPanel = new JPanel(new GridLayout(1, 3));
        bottomPanel.setPreferredSize(new Dimension(100, new ScrollBar(JScrollBar.
                                     HORIZONTAL).getPreferredSize().height));
        bottomPanel.setOpaque(false);

        increaseB = new HeaderButton(null, Icons.getIcon(TracerIcons.INCREMENT)) {
            protected void performAction(ActionEvent e) {
                chart.increaseRowHeights((e.getModifiers() & Toolkit.getDefaultToolkit().
                                         getMenuShortcutKeyMask()) == 0);
            }
        };
        increaseB.setToolTipText(Bundle.TOOLTIP_IncreaseRowsHeight());
        bottomPanel.add(increaseB);

        decreaseB = new HeaderButton(null, Icons.getIcon(TracerIcons.DECREMENT)) {
            protected void performAction(ActionEvent e) {
                chart.decreaseRowHeights((e.getModifiers() & Toolkit.getDefaultToolkit().
                                         getMenuShortcutKeyMask()) == 0);
            }
        };
        decreaseB.setToolTipText(Bundle.TOOLTIP_DecreaseRowsHeight());
        bottomPanel.add(decreaseB);

        resetB = new HeaderButton(null, Icons.getIcon(TracerIcons.RESET)) {
            protected void performAction(ActionEvent e) {
                chart.resetRowHeights();
            }
        };
        resetB.setToolTipText(Bundle.TOOLTIP_ResetRowsHeight());
        bottomPanel.add(resetB);
        
        setOpaque(false);
        setLayout(new BorderLayout());
        add(new HeaderLabel(Bundle.LBL_Probes()), BorderLayout.NORTH);
        add(viewport, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        chart.addRowListener(new TimelineChart.RowListener() {
            public void rowsAdded(List<TimelineChart.Row> rows) {
                for (TimelineChart.Row row : rows) {
                    TracerProbe probe = support.getProbe(row);
                    listPanel.add(new ProbePresenter(probe, support.
                                  getDescriptor(probe)), row.getIndex());
                }
                listPanel.sync();
                revalidate();
                repaint();
                refreshButtons(true);
            }

            public void rowsRemoved(List<TimelineChart.Row> rows) {
                for (TimelineChart.Row row : rows)
                    listPanel.remove(row.getIndex());
                listPanel.sync();
                revalidate();
                repaint();
                refreshButtons(chart.hasRows());
            }

            public void rowsResized(List<TimelineChart.Row> rows) {
                listPanel.sync();
                revalidate();
                repaint();
            }
        });

        refreshButtons(chart.hasRows());
    }

    
    public void setCursor(Cursor cursor) {
        viewport.setCursor(cursor);
    }

    Component getMouseTarget() {
        return viewport;
    }

    void updateSelection() {
        listPanel.updateSelection();
    }


    private void refreshButtons(boolean enabled) {
        increaseB.setEnabled(enabled);
        decreaseB.setEnabled(enabled);
        resetB.setEnabled(enabled);
    }


    private static class ViewportUpdater implements Runnable {

        private final JViewport viewport;
        private Point point;

        ViewportUpdater(JViewport viewport) { this.viewport = viewport; }

        Runnable forPoint(Point point) { this.point = point; return this; }

        public void run() { viewport.setViewPosition(point); }

    }


    private static class ListPanel extends JPanel {

        ListPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        protected void updateSelection() {
        }
        
        String getToolTipText(Point p) {
            String tooltip = null;
            Component c = getComponentAt(p);
            if (c instanceof JComponent)
                tooltip = (String)((JComponent)c).getClientProperty("ToolTipHelper"); // NOI18N
            return tooltip;
        }

        private void sync() {
            doLayout();
            repaint();
        }

    }

}

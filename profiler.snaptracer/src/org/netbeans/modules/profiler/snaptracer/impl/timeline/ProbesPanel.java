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
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.snaptracer.TracerProbe;
import org.netbeans.modules.profiler.snaptracer.impl.icons.TracerIcons;
import org.netbeans.modules.profiler.snaptracer.impl.probes.ProbePresenter;
import org.netbeans.modules.profiler.snaptracer.impl.swing.HeaderButton;
import org.netbeans.modules.profiler.snaptracer.impl.swing.HeaderLabel;
import org.netbeans.modules.profiler.snaptracer.impl.swing.ScrollBar;
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
        increaseB.setToolTipText(NbBundle.getMessage(ProbesPanel.class,
                "TOOLTIP_IncreaseRowsHeight")); // NOI18N
        bottomPanel.add(increaseB);

        decreaseB = new HeaderButton(null, Icons.getIcon(TracerIcons.DECREMENT)) {
            protected void performAction(ActionEvent e) {
                chart.decreaseRowHeights((e.getModifiers() & Toolkit.getDefaultToolkit().
                                         getMenuShortcutKeyMask()) == 0);
            }
        };
        decreaseB.setToolTipText(NbBundle.getMessage(ProbesPanel.class,
                "TOOLTIP_DecreaseRowsHeight")); // NOI18N
        bottomPanel.add(decreaseB);

        resetB = new HeaderButton(null, Icons.getIcon(TracerIcons.RESET)) {
            protected void performAction(ActionEvent e) {
                chart.resetRowHeights();
            }
        };
        resetB.setToolTipText(NbBundle.getMessage(ProbesPanel.class,
                "TOOLTIP_ResetRowsHeight")); // NOI18N
        bottomPanel.add(resetB);
        
        setOpaque(false);
        setLayout(new BorderLayout());
        add(new HeaderLabel(NbBundle.getMessage(ProbesPanel.class,
                "LBL_Probes")), BorderLayout.NORTH); // NOI18N
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

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.graphs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import org.netbeans.lib.profiler.charts.CrossBorderLayout;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYChart;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipOverlay;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryModels;


/**
 *
 * @author Jiri Sedlacek
 */
public final class SurvivingGenerationsGraphPanel extends GraphPanel {

    // -----
    // I18N String constants
//    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.graphs.Bundle"); // NOI18N
//    private static final String TOTAL_MEMORY_CURRENT_STRING = messages.getString("MemoryGraphPanel_TotalMemoryCurrentString"); // NOI18N
//    private static final String USED_MEMORY_CURRENT_STRING = messages.getString("MemoryGraphPanel_UsedMemoryCurrentString"); // NOI18N
//    private static final String USED_MEMORY_MAXIMUM_STRING = messages.getString("MemoryGraphPanel_UsedMemoryMaximumString"); // NOI18N
//    private static final String TIME_AT_CURSOR_STRING = messages.getString("MemoryGraphPanel_TimeAtCursorString"); // NOI18N
//    private static final String TOTAL_MEMORY_AT_CURSOR_STRING = messages.getString("MemoryGraphPanel_TotalMemoryAtCursorString"); // NOI18N
//    private static final String USED_MEMORY_AT_CURSOR_STRING = messages.getString("MemoryGraphPanel_UsedMemoryAtCursorString"); // NOI18N
//    private static final String CHART_ACCESS_NAME = messages.getString("MemoryGraphPanel_ChartAccessName"); // NOI18N
                                                                                                            // -----

    private ProfilerXYChart chart;
    private Action[] chartActions;

    private final VMTelemetryModels models;

    private final boolean smallPanel;


    // --- Constructors --------------------------------------------------------

    public static SurvivingGenerationsGraphPanel createBigPanel(VMTelemetryModels models) {
        return new SurvivingGenerationsGraphPanel(models, false, null);
    }

    public static SurvivingGenerationsGraphPanel createSmallPanel(VMTelemetryModels models,
                                             Action chartAction) {
        return new SurvivingGenerationsGraphPanel(models, true, chartAction);
    }

    private SurvivingGenerationsGraphPanel(VMTelemetryModels models,
                             boolean smallPanel, Action chartAction) {

        // Save models and panel type
        this.models = models;
        this.smallPanel = smallPanel;

        // Create UI
        initComponents(chartAction);

        // Register listener
        models.getDataManager().addDataListener(new DataManagerListener() {
            public void dataChanged() { updateData(); }
            public void dataReset() { resetData(); }
        });

        // Initialize chart & legend
        resetData();
    }


    // --- GraphPanel implementation -------------------------------------------

    public Action[] getActions() {
        return chartActions;
    }


    // --- Private implementation ----------------------------------------------

    private void updateData() {
        if (smallPanel) {
            if (chart.fitsWidth()) {
                long[] timestamps = models.getDataManager().timeStamps;
                if (timestamps[timestamps.length - 1] - timestamps[0] >=
                    SMALL_CHART_FIT_TO_WINDOW_PERIOD)
                        chart.setFitsWidth(false);

            }
        } else {
        }
    }

    private void resetData() {
        if (smallPanel) {
            chart.setScale(INITIAL_CHART_SCALEX, 1);
            chart.setOffset(0, 0);
            chart.setFitsWidth(true);
        } else {
            chart.setScale(INITIAL_CHART_SCALEX, 1);
            chart.setOffset(0, 0);
            chart.setFitsWidth(false);
        }
    }


    private void initComponents(final Action chartAction) {
        // Chart
        chart = new ProfilerXYChart(models.generationsItemsModel(),
                                    models.generationsPaintersModel());
        chart.setBackground(CHART_BACKGROUND_COLOR);
        chart.setViewInsets(new Insets(10, 0, 0, 0));

        // Chart panel (chart & axes)
        JPanel chartPanel = new JPanel(new CrossBorderLayout());
        chartPanel.setBackground(CHART_BACKGROUND_COLOR);
        chartPanel.setBorder(BorderFactory.createMatteBorder(
                             10, 10, 10, 10, CHART_BACKGROUND_COLOR));
        chartPanel.add(chart, new Integer[] { SwingConstants.CENTER });

        // Small panel UI
        if (smallPanel) {

            // Customize chart
            chart.setMouseZoomingEnabled(false);
            chart.setSelectionModel(null);

            // Heap Size
            JLabel heapSizeSmall = new JLabel(VMTelemetryModels.SURVGEN_NAME,
                                              new ColorIcon(VMTelemetryModels.
                                              SURVGEN_PAINTER_LINE_COLOR, null,
                                              8, 8), SwingConstants.LEADING);
            heapSizeSmall.setFont(getFont().deriveFont((float)(getFont().getSize()) - 1));
            heapSizeSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

            // Used heap
            JLabel usedHeapSmall = new JLabel(VMTelemetryModels.GC_TIME_NAME,
                                              new ColorIcon(VMTelemetryModels.
                                              GC_TIME_PAINTER_LINE_COLOR, null,
                                              8, 8), SwingConstants.LEADING);
            usedHeapSmall.setFont(getFont().deriveFont((float) (getFont().getSize()) - 1));
            usedHeapSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

            // Legend container
            JPanel smallLegendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 1));
            smallLegendPanel.setBackground(SMALL_LEGEND_BACKGROUND_COLOR);
            smallLegendPanel.setBorder(new LineBorder(SMALL_LEGEND_BORDER_COLOR, 1));
            smallLegendPanel.add(heapSizeSmall);
            smallLegendPanel.add(usedHeapSmall);
            JPanel smallLegendContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
            smallLegendContainer.setBackground(SMALL_LEGEND_BACKGROUND_COLOR);
            smallLegendContainer.add(smallLegendPanel);

            // Master UI
            setLayout(new BorderLayout());
            add(chartPanel, BorderLayout.CENTER);
            add(smallLegendContainer, BorderLayout.SOUTH);


            // Doubleclick action
            chart.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) &&
                        e.getClickCount() == 2)
                            chartAction.actionPerformed(null);
                }
            });

            // Toolbar actions
            chartActions = new Action[] {};

        // Big panel UI
        } else {

            // Customize chart
            chart.addOverlayComponent(new ProfilerXYTooltipOverlay(chart));

            // Chart scrollbar
            JScrollBar hScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
            chart.attachHorizontalScrollBar(hScrollBar);

            // Chart container (chart panel & scrollbar)
            JPanel chartContainer = new JPanel(new BorderLayout());
            chartContainer.setBorder(new BevelBorder(BevelBorder.LOWERED));
            chartContainer.add(chartPanel, BorderLayout.CENTER);
            chartContainer.add(hScrollBar, BorderLayout.SOUTH);

            // Heap Size
            JLabel heapSizeBig = new JLabel(VMTelemetryModels.SURVGEN_NAME,
                                            new ColorIcon(VMTelemetryModels.
                                            SURVGEN_PAINTER_LINE_COLOR, Color.
                                            BLACK, 18, 9), SwingConstants.LEADING);
            heapSizeBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            // Used heap
            JLabel usedHeapBig = new JLabel(VMTelemetryModels.GC_TIME_NAME,
                                            new ColorIcon(VMTelemetryModels.
                                            GC_TIME_PAINTER_LINE_COLOR, Color.
                                            BLACK, 18, 9), SwingConstants.LEADING);
            usedHeapBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            // Legend container
            JPanel bigLegendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
            bigLegendPanel.add(heapSizeBig);
            bigLegendPanel.add(usedHeapBig);

            // Master UI
            setLayout(new BorderLayout());
            add(chartContainer, BorderLayout.CENTER);
            add(bigLegendPanel, BorderLayout.SOUTH);


            // Toolbar actions
            chartActions = new Action[] { chart.zoomInAction(),
                                          chart.zoomOutAction(),
                                          chart.toggleViewAction()};

        }

    }

}

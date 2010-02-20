/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
import java.util.Date;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.axis.AxisComponent;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.ItemsModel;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.swing.CrossBorderLayout;
import org.netbeans.lib.profiler.charts.PaintersModel;
import org.netbeans.lib.profiler.charts.xy.DecimalXYItemMarksComputer;
import org.netbeans.lib.profiler.charts.axis.PercentLongMarksPainter;
import org.netbeans.lib.profiler.charts.axis.SimpleLongMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimeMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.CompoundXYItemPainter;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemMarker;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemPainter;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerGCXYItemPainter;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYChart;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipOverlay;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipPainter;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipModel;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryModels;


/**
 *
 * @author Jiri Sedlacek
 */
public final class SurvivingGenerationsGraphPanel extends GraphPanel {

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
                VMTelemetryDataManager manager = models.getDataManager();
                long[] timestamps = manager.timeStamps;
                if (timestamps[manager.getItemCount() - 1] - timestamps[0] >=
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
        chart.setInitialDataBounds(new LongRect(System.currentTimeMillis(), 0,
                                       2500, GraphsUI.SURVGEN_INITIAL_VALUE));
    }


    private void initComponents(final Action chartAction) {
        // Painters model
        PaintersModel paintersModel = createGenerationsPaintersModel();

        // Chart
        chart = createChart(models.generationsItemsModel(),
                            paintersModel, smallPanel);
        chart.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chart.setViewInsets(new Insets(10, 0, 0, 0));

        // Horizontal axis
        AxisComponent hAxis =
                new AxisComponent(chart, new TimelineMarksComputer(
                         models.generationsItemsModel().getTimeline(),
                         chart.getChartContext(), SwingConstants.HORIZONTAL),
                         new TimeMarksPainter(),
                         SwingConstants.SOUTH, AxisComponent.MESH_FOREGROUND);

        // Surviving generations axis
        XYItem survgenItem = models.generationsItemsModel().getItem(1);
        XYItemPainter survgenPainter = (XYItemPainter)paintersModel.getPainter(survgenItem);
        SimpleLongMarksPainter survgenMarksPainter = new SimpleLongMarksPainter();
        survgenMarksPainter.setForeground(GraphsUI.SURVGEN_PAINTER_LINE_COLOR);
        AxisComponent sAxis =
                new AxisComponent(chart, new DecimalXYItemMarksComputer(
                         survgenItem, survgenPainter, chart.getChartContext(),
                         SwingConstants.VERTICAL),
                         survgenMarksPainter, SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);

        // GC time axis
        XYItem gcTimeItem = models.generationsItemsModel().getItem(2);
        XYItemPainter gcTimePainter = (XYItemPainter)paintersModel.getPainter(gcTimeItem);
        PercentLongMarksPainter gcTimeMarksPainter = new PercentLongMarksPainter(0, 1000);
        gcTimeMarksPainter.setForeground(GraphsUI.GC_TIME_PAINTER_LINE_COLOR);
        AxisComponent gAxis =
                new AxisComponent(chart, new DecimalXYItemMarksComputer(
                         gcTimeItem, gcTimePainter, chart.getChartContext(),
                         SwingConstants.VERTICAL),
                         gcTimeMarksPainter, SwingConstants.EAST,
                         AxisComponent.NO_MESH);

        // Chart panel (chart & axes)
        JPanel chartPanel = new JPanel(new CrossBorderLayout());
        chartPanel.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chartPanel.setBorder(BorderFactory.createMatteBorder(
                             10, 10, 10, 10, GraphsUI.CHART_BACKGROUND_COLOR));
        chartPanel.add(chart, new Integer[] { SwingConstants.CENTER });
        chartPanel.add(hAxis, new Integer[] { SwingConstants.SOUTH,
                                              SwingConstants.SOUTH_EAST,
                                              SwingConstants.SOUTH_WEST });
        chartPanel.add(sAxis, new Integer[] { SwingConstants.WEST,
                                              SwingConstants.SOUTH_WEST });
        chartPanel.add(gAxis, new Integer[] { SwingConstants.EAST,
                                              SwingConstants.SOUTH_EAST });

        // Small panel UI
        if (smallPanel) {

            // Customize chart
            chart.setMouseZoomingEnabled(false);
            chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_NONE);

            // Heap Size
            JLabel heapSizeSmall = new JLabel(GraphsUI.SURVGEN_NAME,
                                              new ColorIcon(GraphsUI.
                                              SURVGEN_PAINTER_LINE_COLOR, null,
                                              8, 8), SwingConstants.LEADING);
            heapSizeSmall.setFont(getFont().deriveFont((float)(getFont().getSize()) - 1));
            heapSizeSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

            // Used heap
            JLabel usedHeapSmall = new JLabel(GraphsUI.GC_TIME_NAME,
                                              new ColorIcon(GraphsUI.
                                              GC_TIME_PAINTER_LINE_COLOR, null,
                                              8, 8), SwingConstants.LEADING);
            usedHeapSmall.setFont(getFont().deriveFont((float) (getFont().getSize()) - 1));
            usedHeapSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

            // Legend container
            JPanel smallLegendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 1));
            smallLegendPanel.setBackground(GraphsUI.SMALL_LEGEND_BACKGROUND_COLOR);
            smallLegendPanel.setBorder(new LineBorder(GraphsUI.SMALL_LEGEND_BORDER_COLOR, 1));
            smallLegendPanel.add(heapSizeSmall);
            smallLegendPanel.add(usedHeapSmall);
            JPanel smallLegendContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
            smallLegendContainer.setBackground(GraphsUI.SMALL_LEGEND_BACKGROUND_COLOR);
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

            // Setup tooltip painter
            ProfilerXYTooltipPainter tooltipPainter = new ProfilerXYTooltipPainter(
                                                GraphsUI.TOOLTIP_OVERLAY_LINE_WIDTH,
                                                GraphsUI.TOOLTIP_OVERLAY_LINE_COLOR,
                                                GraphsUI.TOOLTIP_OVERLAY_FILL_COLOR,
                                                getTooltipModel());

            // Customize chart
            chart.addOverlayComponent(new ProfilerXYTooltipOverlay(chart,
                                                                   tooltipPainter));

            // Chart scrollbar
            JScrollBar hScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
            chart.attachHorizontalScrollBar(hScrollBar);

            // Chart container (chart panel & scrollbar)
            JPanel chartContainer = new JPanel(new BorderLayout());
            chartContainer.setBorder(new BevelBorder(BevelBorder.LOWERED));
            chartContainer.add(chartPanel, BorderLayout.CENTER);
            chartContainer.add(hScrollBar, BorderLayout.SOUTH);

            // Heap Size
            JLabel heapSizeBig = new JLabel(GraphsUI.SURVGEN_NAME,
                                            new ColorIcon(GraphsUI.
                                            SURVGEN_PAINTER_LINE_COLOR, Color.
                                            BLACK, 18, 9), SwingConstants.LEADING);
            heapSizeBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            // Used heap
            JLabel usedHeapBig = new JLabel(GraphsUI.GC_TIME_NAME,
                                            new ColorIcon(GraphsUI.
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

    protected ProfilerXYTooltipModel createTooltipModel() {
        return new ProfilerXYTooltipModel() {

            public String getTimeValue(long timestamp) {
                return DATE_FORMATTER.format(new Date(timestamp));
            }

            public int getRowsCount() {
                return 2;
            }

            public String getRowName(int index) {
                switch (index) {
                    case 0:
                        return GraphsUI.SURVGEN_NAME;
                    case 1:
                        return GraphsUI.GC_TIME_NAME;
                    default:
                        return null;
                }
            }

            public Color getRowColor(int index) {
                switch (index) {
                    case 0:
                        return GraphsUI.SURVGEN_PAINTER_LINE_COLOR;
                    case 1:
                        return GraphsUI.GC_TIME_PAINTER_LINE_COLOR;
                    default:
                        return null;
                }
            }

            public String getRowValue(int index, long itemValue) {
                switch (index) {
                    case 0:
                        return INT_FORMATTER.format(itemValue);
                    case 1:
                        String val = PERCENT_FORMATTER.format(itemValue / 1000f);
                        return trimPercents(val);
                    default:
                        return null;
                }
            }

            public String getRowUnits(int index, long itemValue) {
                switch (index) {
                    case 0:
                        return ""; // NOI18N
                    case 1:
                        return "%"; // NOI18N
                    default:
                        return null;
                }
            }

            public int getExtraRowsCount() {
                return getRowsCount();
            }

            public String getExtraRowName(int index) {
                return getMaxValueString(getRowName(index));
            }

            public Color getExtraRowColor(int index) {
                return getRowColor(index);
            }

            public String getExtraRowValue(int index) {
                SynchronousXYItem item = models.generationsItemsModel().getItem(index+1);
                switch (index) {
                    case 0:
                        return INT_FORMATTER.format(item.getMaxYValue());
                    case 1:
                        String val = PERCENT_FORMATTER.format(item.getMaxYValue() /
                                                              1000f);
                        return trimPercents(val);
                    default:
                        return null;
                }
            }

            public String getExtraRowUnits(int index) {
                return getRowUnits(index, -1);
            }

            private String trimPercents(String percents) {
                return !percents.endsWith("%") ? percents : // NOI18N
                        percents.substring(0, percents.length() - 1).trim();
            }

        };
    }

    private PaintersModel createGenerationsPaintersModel() {
        // Surviving generations
        SynchronousXYItemPainter survgenPainter =
                SynchronousXYItemPainter.absolutePainter(GraphsUI.SURVGEN_PAINTER_LINE_WIDTH,
                                                      GraphsUI.SURVGEN_PAINTER_LINE_COLOR,
                                                      GraphsUI.SURVGEN_PAINTER_FILL_COLOR);
        SynchronousXYItemMarker survgenMarker =
                 SynchronousXYItemMarker.absolutePainter(GraphsUI.SURVGEN_MARKER_RADIUS,
                                                      GraphsUI.SURVGEN_MARKER_LINE1_WIDTH,
                                                      GraphsUI.SURVGEN_MARKER_LINE1_COLOR,
                                                      GraphsUI.SURVGEN_MARKER_LINE2_WIDTH,
                                                      GraphsUI.SURVGEN_MARKER_LINE2_COLOR,
                                                      GraphsUI.SURVGEN_MARKER_FILL_COLOR);
        XYItemPainter sgp = new CompoundXYItemPainter(survgenPainter,
                                                      survgenMarker);

        // Relative time spent in GC
        SynchronousXYItemPainter gcTimePainter =
                SynchronousXYItemPainter.relativePainter(GraphsUI.GC_TIME_PAINTER_LINE_WIDTH,
                                                      GraphsUI.GC_TIME_PAINTER_LINE_COLOR,
                                                      GraphsUI.GC_TIME_PAINTER_FILL_COLOR,
                                                      10);
        SynchronousXYItemMarker gcTimeMarker =
                 SynchronousXYItemMarker.relativePainter(GraphsUI.GC_TIME_MARKER_RADIUS,
                                                      GraphsUI.GC_TIME_MARKER_LINE1_WIDTH,
                                                      GraphsUI.GC_TIME_MARKER_LINE1_COLOR,
                                                      GraphsUI.GC_TIME_MARKER_LINE2_WIDTH,
                                                      GraphsUI.GC_TIME_MARKER_LINE2_COLOR,
                                                      GraphsUI.GC_TIME_MARKER_FILL_COLOR,
                                                      10);
        XYItemPainter gtp = new CompoundXYItemPainter(gcTimePainter,
                                                      gcTimeMarker);

        // GC events painter
        XYItemPainter gep = ProfilerGCXYItemPainter.painter(GraphsUI.GC_ACTIVITY_FILL_COLOR);

        // Model
        ItemsModel items = models.generationsItemsModel();
        PaintersModel model = new PaintersModel.Default(
                                            new ChartItem[] { items.getItem(0),
                                                              items.getItem(1),
                                                              items.getItem(2) },
                                            new XYItemPainter[] { gep, sgp, gtp });

        return model;
    }

}

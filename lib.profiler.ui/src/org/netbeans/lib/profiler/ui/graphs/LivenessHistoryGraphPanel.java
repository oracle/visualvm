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
import java.util.Date;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import org.netbeans.lib.profiler.charts.LongRect;
import org.netbeans.lib.profiler.charts.PaintersModel;
import org.netbeans.lib.profiler.charts.axis.AxisComponent;
import org.netbeans.lib.profiler.charts.axis.BytesAxisUtils;
import org.netbeans.lib.profiler.charts.axis.BytesMarksPainter;
import org.netbeans.lib.profiler.charts.axis.SimpleLongMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimeMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.swing.CrossBorderLayout;
import org.netbeans.lib.profiler.charts.xy.BytesXYItemMarksComputer;
import org.netbeans.lib.profiler.charts.xy.DecimalXYItemMarksComputer;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.ui.charts.xy.CompoundProfilerXYItemPainter;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYChart;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemMarker;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItemPainter;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYPaintersModel;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipOverlay;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipPainter;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYItem;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipModel;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.memory.ClassHistoryModels;


/**
 *
 * @author Jiri Sedlacek
 */
public final class LivenessHistoryGraphPanel extends GraphPanel {

    private ProfilerXYChart chart;
    private Action[] chartActions;

    private final ClassHistoryModels models;


    // --- Constructors --------------------------------------------------------

    public static LivenessHistoryGraphPanel createPanel(ClassHistoryModels models) {
        return new LivenessHistoryGraphPanel(models);
    }

    private LivenessHistoryGraphPanel(ClassHistoryModels models) {

        // Save models and panel type
        this.models = models;

        // Create UI
        initComponents();

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
    }

    private void resetData() {
        chart.setScale(INITIAL_CHART_SCALEX, 1);
        chart.setOffset(0, 0);
        chart.setFitsWidth(false);
        chart.setInitialDataBounds(new LongRect(System.currentTimeMillis(), 0,
                                       2500, GraphsUI.L_LIVE_OBJECTS_INITIAL_VALUE));
    }


    private void initComponents() {
        // Painters model
        PaintersModel paintersModel = createLivenessPaintersModel();

        // Chart
        chart = createChart(models.livenessItemsModel(),
                            paintersModel, false);
        chart.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chart.setViewInsets(new Insets(10, 0, 0, 0));

        // Horizontal axis
        AxisComponent hAxis =
                new AxisComponent(chart, new TimelineMarksComputer(
                         models.livenessItemsModel().getTimeline(),
                         chart.getChartContext(), SwingConstants.HORIZONTAL),
                         new TimeMarksPainter(),
                         SwingConstants.SOUTH, AxisComponent.MESH_FOREGROUND);

        // Live objects axis
        XYItem liveObjectsItem = models.livenessItemsModel().getItem(0);
        XYItemPainter liveObjectsPainter = (XYItemPainter)paintersModel.getPainter(liveObjectsItem);
        SimpleLongMarksPainter liveObjectsMarksPainter = new SimpleLongMarksPainter();
        liveObjectsMarksPainter.setForeground(GraphsUI.L_LIVE_OBJECTS_PAINTER_LINE_COLOR);
        AxisComponent loAxis =
                new AxisComponent(chart, new DecimalXYItemMarksComputer(
                         liveObjectsItem, liveObjectsPainter, chart.getChartContext(),
                         SwingConstants.VERTICAL),
                         liveObjectsMarksPainter, SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);

        // Live bytes axis
        XYItem liveBytesItem = models.livenessItemsModel().getItem(1);
        XYItemPainter liveBytesPainter = (XYItemPainter)paintersModel.getPainter(liveBytesItem);
        BytesMarksPainter allocBytesMarksPainter = new BytesMarksPainter();
        allocBytesMarksPainter.setForeground(GraphsUI.L_LIVE_BYTES_PAINTER_LINE_COLOR);
        AxisComponent lbAxis =
                new AxisComponent(chart, new BytesXYItemMarksComputer(
                         liveBytesItem, liveBytesPainter, chart.getChartContext(),
                         SwingConstants.VERTICAL),
                         allocBytesMarksPainter, SwingConstants.EAST,
                         AxisComponent.NO_MESH);

        // Allocated objects axis
        XYItem allocObjectsItem = models.livenessItemsModel().getItem(2);
        XYItemPainter allocObjectsPainter = (XYItemPainter)paintersModel.getPainter(allocObjectsItem);
        SimpleLongMarksPainter allocObjectsMarksPainter = new SimpleLongMarksPainter();
        allocObjectsMarksPainter.setForeground(GraphsUI.L_ALLOC_OBJECTS_PAINTER_LINE_COLOR);
        AxisComponent aoAxis =
                new AxisComponent(chart, new DecimalXYItemMarksComputer(
                         allocObjectsItem, allocObjectsPainter, chart.getChartContext(),
                         SwingConstants.VERTICAL),
                         allocObjectsMarksPainter, SwingConstants.EAST,
                         AxisComponent.NO_MESH);

        JPanel multiAxisContainer = new JPanel(new BorderLayout());
        multiAxisContainer.add(lbAxis, BorderLayout.WEST);
        multiAxisContainer.add(aoAxis, BorderLayout.EAST);
        multiAxisContainer.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);

        // Chart panel (chart & axes)
        JPanel chartPanel = new JPanel(new CrossBorderLayout());
        chartPanel.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chartPanel.setBorder(BorderFactory.createMatteBorder(
                             10, 10, 10, 10, GraphsUI.CHART_BACKGROUND_COLOR));
        chartPanel.add(chart, new Integer[] { SwingConstants.CENTER });
        chartPanel.add(hAxis, new Integer[] { SwingConstants.SOUTH,
                                              SwingConstants.SOUTH_WEST,
                                              SwingConstants.SOUTH_EAST });
        chartPanel.add(loAxis, new Integer[] { SwingConstants.WEST,
                                              SwingConstants.SOUTH_WEST });
        chartPanel.add(multiAxisContainer, new Integer[] { SwingConstants.EAST,
                                              SwingConstants.SOUTH_EAST });

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

        // Live Objects
        JLabel liveObjectsBig = new JLabel(GraphsUI.L_LIVE_OBJECTS_NAME,
                                        new ColorIcon(GraphsUI.
                                        L_LIVE_OBJECTS_PAINTER_LINE_COLOR, Color.
                                        BLACK, 18, 9), SwingConstants.LEADING);
        liveObjectsBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Live Bytes
        JLabel liveBytesBig = new JLabel(GraphsUI.L_LIVE_BYTES_NAME,
                                        new ColorIcon(GraphsUI.
                                        L_LIVE_BYTES_PAINTER_LINE_COLOR, Color.
                                        BLACK, 18, 9), SwingConstants.LEADING);
        liveBytesBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Allocated Objects
        JLabel allocObjectsBig = new JLabel(GraphsUI.L_ALLOC_OBJECTS_NAME,
                                        new ColorIcon(GraphsUI.
                                        L_ALLOC_OBJECTS_PAINTER_LINE_COLOR, Color.
                                        BLACK, 18, 9), SwingConstants.LEADING);
        allocObjectsBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Legend container
        JPanel bigLegendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        bigLegendPanel.add(liveObjectsBig);
        bigLegendPanel.add(liveBytesBig);
        bigLegendPanel.add(allocObjectsBig);

        // Master UI
        setLayout(new BorderLayout());
        add(chartContainer, BorderLayout.CENTER);
        add(bigLegendPanel, BorderLayout.SOUTH);


        // Toolbar actions
        chartActions = new Action[] { chart.zoomInAction(),
                                      chart.zoomOutAction(),
                                      chart.toggleViewAction()};

    }

    protected ProfilerXYTooltipModel createTooltipModel() {
        return new ProfilerXYTooltipModel() {

            public String getTimeValue(long timestamp) {
                return DATE_FORMATTER.format(new Date(timestamp));
            }

            public int getRowsCount() {
                return 3;
            }

            public String getRowName(int index) {
                switch (index) {
                    case 0:
                        return GraphsUI.L_LIVE_OBJECTS_NAME;
                    case 1:
                        return GraphsUI.L_LIVE_BYTES_NAME;
                    case 2:
                        return GraphsUI.L_ALLOC_OBJECTS_NAME;
                    default:
                        return null;
                }
            }

            public Color getRowColor(int index) {
                switch (index) {
                    case 0:
                        return GraphsUI.L_LIVE_OBJECTS_PAINTER_LINE_COLOR;
                    case 1:
                        return GraphsUI.L_LIVE_BYTES_PAINTER_LINE_COLOR;
                    case 2:
                        return GraphsUI.L_ALLOC_OBJECTS_PAINTER_LINE_COLOR;
                    default:
                        return null;
                }
            }

            public String getRowValue(int index, long itemValue) {
                return INT_FORMATTER.format(itemValue);
            }

            public String getRowUnits(int index, long itemValue) {
                switch (index) {
                    case 0:
                        return ""; // NOI18N
                    case 1:
                        return BytesAxisUtils.UNITS_B;
                    case 2:
                        return ""; // NOI18N
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
                ProfilerXYItem item = models.livenessItemsModel().getItem(index);
                return INT_FORMATTER.format(item.getMaxYValue());
            }

            public String getExtraRowUnits(int index) {
                return getRowUnits(index, -1);
            }

        };
    }

    private PaintersModel createLivenessPaintersModel() {
        // Live Objects
        ProfilerXYItemPainter liveObjectsPainter =
                ProfilerXYItemPainter.absolutePainter(GraphsUI.L_LIVE_OBJECTS_PAINTER_LINE_WIDTH,
                                                      GraphsUI.L_LIVE_OBJECTS_PAINTER_LINE_COLOR,
                                                      GraphsUI.L_LIVE_OBJECTS_PAINTER_FILL_COLOR);
        ProfilerXYItemMarker liveObjectsMarker =
                 ProfilerXYItemMarker.absolutePainter(GraphsUI.L_LIVE_OBJECTS_MARKER_RADIUS,
                                                      GraphsUI.L_LIVE_OBJECTS_MARKER_LINE1_WIDTH,
                                                      GraphsUI.L_LIVE_OBJECTS_MARKER_LINE1_COLOR,
                                                      GraphsUI.L_LIVE_OBJECTS_MARKER_LINE2_WIDTH,
                                                      GraphsUI.L_LIVE_OBJECTS_MARKER_LINE2_COLOR,
                                                      GraphsUI.L_LIVE_OBJECTS_MARKER_FILL_COLOR);
        XYItemPainter lop = new CompoundProfilerXYItemPainter(liveObjectsPainter,
                                                      liveObjectsMarker);

        // Live Bytes
        ProfilerXYItemPainter liveBytesPainter =
                ProfilerXYItemPainter.relativePainter(GraphsUI.L_LIVE_BYTES_PAINTER_LINE_WIDTH,
                                                      GraphsUI.L_LIVE_BYTES_PAINTER_LINE_COLOR,
                                                      GraphsUI.L_LIVE_BYTES_PAINTER_FILL_COLOR,
                                                      10);
        ProfilerXYItemMarker liveBytesMarker =
                 ProfilerXYItemMarker.relativePainter(GraphsUI.L_LIVE_BYTES_MARKER_RADIUS,
                                                      GraphsUI.L_LIVE_BYTES_MARKER_LINE1_WIDTH,
                                                      GraphsUI.L_LIVE_BYTES_MARKER_LINE1_COLOR,
                                                      GraphsUI.L_LIVE_BYTES_MARKER_LINE2_WIDTH,
                                                      GraphsUI.L_LIVE_BYTES_MARKER_LINE2_COLOR,
                                                      GraphsUI.L_LIVE_BYTES_MARKER_FILL_COLOR,
                                                      10);
        XYItemPainter lbp = new CompoundProfilerXYItemPainter(liveBytesPainter,
                                                      liveBytesMarker);

        // Allocated Objects
        ProfilerXYItemPainter allocObjectsPainter =
                ProfilerXYItemPainter.relativePainter(GraphsUI.L_ALLOC_OBJECTS_PAINTER_LINE_WIDTH,
                                                      GraphsUI.L_ALLOC_OBJECTS_PAINTER_LINE_COLOR,
                                                      GraphsUI.L_ALLOC_OBJECTS_PAINTER_FILL_COLOR,
                                                      20);
        ProfilerXYItemMarker allocObjectsMarker =
                 ProfilerXYItemMarker.relativePainter(GraphsUI.L_ALLOC_OBJECTS_MARKER_RADIUS,
                                                      GraphsUI.L_ALLOC_OBJECTS_MARKER_LINE1_WIDTH,
                                                      GraphsUI.L_ALLOC_OBJECTS_MARKER_LINE1_COLOR,
                                                      GraphsUI.L_ALLOC_OBJECTS_MARKER_LINE2_WIDTH,
                                                      GraphsUI.L_ALLOC_OBJECTS_MARKER_LINE2_COLOR,
                                                      GraphsUI.L_ALLOC_OBJECTS_MARKER_FILL_COLOR,
                                                      20);
        XYItemPainter aop = new CompoundProfilerXYItemPainter(allocObjectsPainter,
                                                      allocObjectsMarker);

        // Model
        ProfilerXYPaintersModel model = new ProfilerXYPaintersModel(
           new XYItemPainter[] { lop, lbp, aop });

        return model;
    }

}

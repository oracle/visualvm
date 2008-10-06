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

import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.charts.ChartModelListener;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChart;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.monitor.SurvivingGenerationsXYChartModel;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryXYChartModel;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryXYChartModelDataResetListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.border.LineBorder;


/**
 * A panel that contains the graph for surviving generations and time spent in GC.
 *
 * @author Vladislav Nemec
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class SurvivingGenerationsGraphPanel extends GraphPanel implements ChartModelListener,
                                                                          VMTelemetryXYChartModelDataResetListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.graphs.Bundle"); // NOI18N
    private static final String SURVGEN_CURRENT_STRING = messages.getString("SurvivingGenerationsGraphPanel_SurvGenCurrentString"); // NOI18N
    private static final String SURVGEN_MAXIMUM_STRING = messages.getString("SurvivingGenerationsGraphPanel_SurvGenMaximumString"); // NOI18N
    private static final String GC_TIME_CURRENT_STRING = messages.getString("SurvivingGenerationsGraphPanel_GcTimeCurrentString"); // NOI18N
    private static final String GC_TIME_MAXIMUM_STRING = messages.getString("SurvivingGenerationsGraphPanel_GcTimeMaximumString"); // NOI18N
    private static final String TIME_AT_CURSOR_STRING = messages.getString("SurvivingGenerationsGraphPanel_TimeAtCursorString"); // NOI18N
    private static final String SURVGEN_AT_CURSOR_STRING = messages.getString("SurvivingGenerationsGraphPanel_SurvGenAtCursorString"); // NOI18N
    private static final String GC_TIME_AT_CURSOR_STRING = messages.getString("SurvivingGenerationsGraphPanel_GcTimeAtCursorString"); // NOI18N
    private static final String CHART_ACCESS_NAME = messages.getString("SurvivingGenerationsGraphPanel_ChartAccessName"); // NOI18N
                                                                                                                          // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JPanel bigLegendPanel;
    private JPanel smallLegendPanel;
    private SurvivingGenerationsXYChartModel survivingGenerationsXYChartModel;
    private SynchronousXYChart xyChart;
    private boolean completeFunctionality;
    private int chartTimeLength = 180000; // 3 minutes to switch from fitToWindow to trackingEnd
                                          //private int chartTimeLength = 10000; // 10 seconds for testing purposes

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new form SurvivingGenerationsGraphPanel with the default history size (3 minutes)
     * and no mouse zooming capabilities
     */
    public SurvivingGenerationsGraphPanel(final VMTelemetryXYChartModel survivingGenerationsXYChartModel,
                                          final Action detailsAction) {
        this(false, null, survivingGenerationsXYChartModel, null);
    }

    /** Creates new form SurvivingGenerationsGraphPanel with the given amount of history to keep
     *
     * @param completeFunctionality if true, the chart can be zoomed using mouse and will display all history, if false, it will only display last session and 3 minutes of data
     * @param backgroundPaint paint used for drawing graph background
     */
    public SurvivingGenerationsGraphPanel(final boolean completeFunctionality, final Color backgroundPaint,
                                          final VMTelemetryXYChartModel survivingGenerationsXYChartModel,
                                          final Action detailsAction) {
        this.completeFunctionality = completeFunctionality;
        this.survivingGenerationsXYChartModel = (SurvivingGenerationsXYChartModel) survivingGenerationsXYChartModel;

        survivingGenerationsXYChartModel.addDataResetListener(this);

        setLayout(new java.awt.BorderLayout());

        // --- Big legend panel ----------------------------------------------------
        JLabel survivingGenerationsLabelBig = new JLabel(survivingGenerationsXYChartModel.getSeriesName(0),
                                                         new ColorIcon(survivingGenerationsXYChartModel.getSeriesColor(0),
                                                                       Color.BLACK, 18, 9), SwingConstants.LEADING);
        survivingGenerationsLabelBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JLabel relativeTimeGCBig = new JLabel(survivingGenerationsXYChartModel.getSeriesName(1),
                                              new ColorIcon(survivingGenerationsXYChartModel.getSeriesColor(1), Color.BLACK, 18, 9),
                                              SwingConstants.LEADING);
        relativeTimeGCBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        bigLegendPanel = new JPanel();
        bigLegendPanel.add(survivingGenerationsLabelBig);
        bigLegendPanel.add(relativeTimeGCBig);

        // --- Small legend panel --------------------------------------------------
        JLabel survivingGenerationsLabelSmall = new JLabel(survivingGenerationsXYChartModel.getSeriesName(0),
                                                           new ColorIcon(survivingGenerationsXYChartModel.getSeriesColor(0),
                                                                         null, 8, 8), SwingConstants.LEADING);
        survivingGenerationsLabelSmall.setFont(getFont().deriveFont((float) (getFont().getSize()) - 1));
        survivingGenerationsLabelSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        JLabel relativeTimeGCSmall = new JLabel(survivingGenerationsXYChartModel.getSeriesName(1),
                                                new ColorIcon(survivingGenerationsXYChartModel.getSeriesColor(1), null, 8, 8),
                                                SwingConstants.LEADING);
        relativeTimeGCSmall.setFont(getFont().deriveFont((float) (getFont().getSize()) - 1));
        relativeTimeGCSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        smallLegendPanel = new JPanel();
        smallLegendPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 1));
        smallLegendPanel.setBackground(Color.WHITE);
        smallLegendPanel.setBorder(new LineBorder(new Color(235, 235, 235), 1));
        smallLegendPanel.add(survivingGenerationsLabelSmall);
        smallLegendPanel.add(relativeTimeGCSmall);

        // --- Chart panel ---------------------------------------------------------
        xyChart = new SynchronousXYChart(SynchronousXYChart.TYPE_LINE, SynchronousXYChart.VALUES_INTERPOLATED, 0.01) {
                public String getToolTipText(MouseEvent event) {
                    return getChartToolTipText(event);
                }
                public Point getToolTipLocation(MouseEvent event) {
                    return new Point(event.getX(), event.getY() + 20);
                }
            };

        xyChart.setUseSecondaryVerticalAxis(true);

        long time = System.currentTimeMillis();
        xyChart.setupInitialAppearance(time, time + 1200, 0, 2);
        getAccessibleContext().setAccessibleName(CHART_ACCESS_NAME);
        xyChart.setAccessibleContext(getAccessibleContext());

        if (completeFunctionality) {
            xyChart.setTopChartMargin(50);
            xyChart.allowSelection();
            xyChart.setMinimumVerticalMarksDistance(50);
        } else {
            xyChart.setTopChartMargin(20);
            xyChart.denySelection();
            xyChart.setMinimumVerticalMarksDistance(UIManager.getFont("Panel.font").getSize() + 8); // NOI18N
        }

        xyChart.setVerticalAxisValueDivider2(10);
        xyChart.setVerticalAxisValueString2("%"); // NOI18N

        chartDataReset();

        if (backgroundPaint != null) {
            setOpaque(true);
            setBackground(backgroundPaint);
            xyChart.setBackgroundPaint(backgroundPaint);
        }

        xyChart.setModel(survivingGenerationsXYChartModel);

        if (!completeFunctionality) {
            survivingGenerationsXYChartModel.addChartModelListener(this); // Needs to be AFTER xyChart.setModel() !!!
        }

        add(xyChart);

        xyChart.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                        if (detailsAction != null) {
                            detailsAction.actionPerformed(null);
                        }
                    }
                }
            });

        ToolTipManager.sharedInstance().registerComponent(xyChart);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JPanel getBigLegendPanel() {
        return bigLegendPanel;
    }

    // ----------------------------------------------------------------------------
    // Public API
    public SynchronousXYChart getChart() {
        return xyChart;
    }

    public String getChartToolTipText(MouseEvent event) {
        if (survivingGenerationsXYChartModel.getItemCount() < 2) {
            return null;
        }

        StringBuffer toolTipBuffer = new StringBuffer();

        toolTipBuffer.append("<html>"); // NOI18N

        if (!completeFunctionality || !xyChart.hasValidDataForPosition(event.getX(), event.getY())) {
            appendToolTipItem(toolTipBuffer, SURVGEN_CURRENT_STRING,
                              intFormat.format(survivingGenerationsXYChartModel.getYValue(survivingGenerationsXYChartModel
                                                                                                                                                                                                                                          .getItemCount()
                                                                                          - 1, 0)), false);
            appendToolTipItem(toolTipBuffer, SURVGEN_MAXIMUM_STRING,
                              intFormat.format(survivingGenerationsXYChartModel.getMaxYValue(0)), false);
            appendToolTipItem(toolTipBuffer, GC_TIME_CURRENT_STRING,
                              percentFormat.format(survivingGenerationsXYChartModel.getYValue(survivingGenerationsXYChartModel
                                                                                                                                                                                                                                                .getItemCount()
                                                                                              - 1, 1) / 1000f), false);
            appendToolTipItem(toolTipBuffer, GC_TIME_MAXIMUM_STRING,
                              percentFormat.format(survivingGenerationsXYChartModel.getMaxYValue(1) / 1000f), true);
        } else {
            appendToolTipItem(toolTipBuffer, SURVGEN_CURRENT_STRING,
                              intFormat.format(survivingGenerationsXYChartModel.getYValue(survivingGenerationsXYChartModel
                                                                                                                                                                                                                                                       .getItemCount()
                                                                                          - 1, 0)), false);
            appendToolTipItem(toolTipBuffer, SURVGEN_MAXIMUM_STRING,
                              intFormat.format(survivingGenerationsXYChartModel.getMaxYValue(0)), false);
            appendToolTipItem(toolTipBuffer, GC_TIME_CURRENT_STRING,
                              percentFormat.format(survivingGenerationsXYChartModel.getYValue(survivingGenerationsXYChartModel
                                                                                                                                                                                                                                                             .getItemCount()
                                                                                              - 1, 1) / 1000f), false);
            appendToolTipItem(toolTipBuffer, GC_TIME_MAXIMUM_STRING,
                              percentFormat.format(survivingGenerationsXYChartModel.getMaxYValue(1) / 1000f), false);

            toolTipBuffer.append("<br>"); // NOI18N

            appendToolTipItem(toolTipBuffer, TIME_AT_CURSOR_STRING, xyChart.getTimeAtPosition(event.getX()), false);
            appendToolTipItem(toolTipBuffer, SURVGEN_AT_CURSOR_STRING,
                              intFormat.format(xyChart.getYValueAtPosition(event.getX(), 0)), false);
            appendToolTipItem(toolTipBuffer, GC_TIME_AT_CURSOR_STRING,
                              percentFormat.format(xyChart.getYValueAtPosition(event.getX(), 1) / 1000f), true);
        }

        toolTipBuffer.append("</html>"); // NOI18N

        return toolTipBuffer.toString();
    }

    public JPanel getSmallLegendPanel() {
        return smallLegendPanel;
    }

    // --- ChartModelListener ----------------------------------------------------
    public void chartDataChanged() {
        if (!completeFunctionality) {
            if (xyChart.isFitToWindow()
                    && ((survivingGenerationsXYChartModel.getMaxXValue() - survivingGenerationsXYChartModel.getMinXValue()) >= chartTimeLength)) { // after 3 minutes switch from fitToWindow to trackingEnd
                UIUtils.runInEventDispatchThread(new Runnable() {
                        public void run() {
                            xyChart.setTrackingEnd();
                        }
                    });
            }
        }
    }

    // --- VMTelemetryXYChartModelDataResetListener ------------------------------
    public void chartDataReset() {
        UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    xyChart.resetChart();

                    if (completeFunctionality) {
                        //xyChart.setScale(0.01);
                        //xyChart.setViewOffsetX(0);
                        xyChart.resetTrackingEnd();
                        xyChart.resetFitToWindow();
                    } else {
                        xyChart.setFitToWindow();
                    }
                }
            });
    }

    // --- ToolTip stuff ---------------------------------------------------------
    private static void appendToolTipItem(StringBuffer toolTipBuffer, String itemName, String itemValue, boolean lastItem) {
        toolTipBuffer.append("&nbsp;<b>"); // NOI18N
        toolTipBuffer.append(itemName);
        toolTipBuffer.append("</b>: "); // NOI18N
        toolTipBuffer.append(itemValue);
        toolTipBuffer.append("&nbsp;"); // NOI18N

        if (!lastItem) {
            toolTipBuffer.append("<br>"); // NOI18N
        }
    }
}

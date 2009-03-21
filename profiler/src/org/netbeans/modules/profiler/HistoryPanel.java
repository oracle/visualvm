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

package org.netbeans.modules.profiler;

import org.netbeans.lib.profiler.ui.charts.ChartModelListener;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChart;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryXYChartModelDataResetListener;
import org.openide.util.NbBundle;
import java.awt.Color;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.ui.charts.xy.ProfilerXYTooltipModel;


/**
 *
 * @author Emanuel Hucka
 */
public class HistoryPanel extends GraphPanel implements ChartModelListener, VMTelemetryXYChartModelDataResetListener,
                                                        HistoryListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /*
     *HistoryPanel_ToolTipClass=Class
       HistoryPanel_ToolTipObjects=Objects
       HistoryPanel_ToolTipSize=Size
       HistoryPanel_ToolTipCursor=Cursor
       HistoryPanel_ToolTipTime=Time
       HistoryPanel_ToolTipLive=Live
       HistoryPanel_ToolTipAllocated=Allocated
     *
     */
    private static final String TOOLTIP_CLASS = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipClass"); //NOI18N
    private static final String TOOLTIP_OBJECTS = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipObjects"); //NOI18N
    private static final String TOOLTIP_SIZE = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipSize"); //NOI18N
    private static final String TOOLTIP_CURSOR = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipCursor"); //NOI18N
    private static final String TOOLTIP_TIME = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipTime"); //NOI18N
    private static final String TOOLTIP_LIVE = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipLive"); //NOI18N
    private static final String TOOLTIP_ALLOCATED = NbBundle.getMessage(HistoryPanel.class, "HistoryPanel_ToolTipAllocated"); //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    History history;
    private JPanel bigLegendPanel;

    //    private JPanel smallLegendPanel;
    private SynchronousXYChart xyChart;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //    private int chartTimeLength = 100000; // 3 minutes to switch from fitToWindow to trackingEnd
    public HistoryPanel(History source) {
        this.history = source;
        history.addHistoryListener(this);

        xyChart = new SynchronousXYChart(SynchronousXYChart.TYPE_LINE, SynchronousXYChart.VALUES_INTERPOLATED, 0.1) {
                public String getToolTipText(MouseEvent event) {
                    return getChartToolTipText(event);
                }
            };
        xyChart.setTopChartMargin(50);
        xyChart.allowSelection();

        setOpaque(true);
        setBackground(Color.WHITE);
        xyChart.setBackgroundPaint(Color.WHITE);

        setLayout(new java.awt.BorderLayout());
        add(xyChart, java.awt.BorderLayout.CENTER);

        xyChart.setupInitialAppearance(0, 50, 0, 1000);
        xyChart.setUseSecondaryVerticalAxis(true);
        xyChart.setVerticalAxisValueString2("B");
        xyChart.setVerticalAxisValueAdaptDivider(true);
        xyChart.setVerticalAxisValueAdaptDivider2(true);
        xyChart.setMinimumVerticalMarksDistance(UIManager.getFont("Panel.font").getSize() + 8); // NOI18N

        xyChart.setAccessibleContext(getAccessibleContext());

        chartDataReset();
        xyChart.setModel(history);
        //history.addChartModelListener(this); // Needs to be AFTER xyChart.setModel() !!!
        createBigLegend();
        //        createSmallLegend();
        ToolTipManager.sharedInstance().registerComponent(xyChart);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Action[] getActions() { return new Action[] {}; }

    protected ProfilerXYTooltipModel createTooltipModel() { return null; }

    public JPanel getBigLegendPanel() {
        return bigLegendPanel;
    }

    public SynchronousXYChart getChart() {
        return xyChart;
    }

    public String getChartToolTipText(MouseEvent event) {
        if (history.getItemCount() < 2) {
            return null;
        }

        StringBuilder toolTipBuffer = new StringBuilder();

        toolTipBuffer.append("<html><table cellspacing=\"1\" cellpadding=\"0\">"); // NOI18N
        appendToolTipItem(toolTipBuffer, TOOLTIP_CLASS, History.getInstance().getClassName(), false);

        if (!history.isLiveness()) {
            appendToolTipItem(toolTipBuffer, TOOLTIP_OBJECTS, INT_FORMATTER.format(history.getYValue(history.getItemCount() - 1, 0)),
                              false);
            appendToolTipItem(toolTipBuffer, TOOLTIP_SIZE,
                              INT_FORMATTER.format(history.getYValue(history.getItemCount() - 1, 1)) + " B", false);
        } else {
            appendToolTipItem(toolTipBuffer, TOOLTIP_LIVE, INT_FORMATTER.format(history.getYValue(history.getItemCount() - 1, 0)),
                              false);
            appendToolTipItem(toolTipBuffer, TOOLTIP_ALLOCATED,
                              INT_FORMATTER.format(history.getYValue(history.getItemCount() - 1, 1)), false);
        }

        toolTipBuffer.append("</table><br><table cellspacing=\"1\" cellpadding=\"0\">"); // NOI18N
        toolTipBuffer.append("<tr><td colspan=\"2\">" + TOOLTIP_CURSOR + "</td></tr>"); // NOI18N

        if (history.isLiveness()) {
            appendToolTipItem(toolTipBuffer, TOOLTIP_LIVE, INT_FORMATTER.format(xyChart.getYValueAtPosition(event.getX(), 0)), false);
            appendToolTipItem(toolTipBuffer, TOOLTIP_ALLOCATED, INT_FORMATTER.format(xyChart.getYValueAtPosition(event.getX(), 1)),
                              false);
        } else {
            appendToolTipItem(toolTipBuffer, TOOLTIP_OBJECTS, INT_FORMATTER.format(xyChart.getYValueAtPosition(event.getX(), 0)),
                              false);
            appendToolTipItem(toolTipBuffer, TOOLTIP_SIZE, INT_FORMATTER.format(xyChart.getYValueAtPosition(event.getX(), 1)) + " B",
                              false);
        }

        appendToolTipItem(toolTipBuffer, TOOLTIP_TIME, xyChart.getTimeAtPosition(event.getX()), true);

        toolTipBuffer.append("</table></html>"); // NOI18N

        return toolTipBuffer.toString();
    }

    public JPanel getSmallLegendPanel() {
        //        return smallLegendPanel;
        return null;
    }

    public void chartDataChanged() {
        /*if (xyChart.isFitToWindow() && history.getMaxXValue() - history.getMinXValue() >= chartTimeLength) { // after 3 minutes switch from fitToWindow to trackingEnd
           xyChart.setTrackingEnd();
           }*/
    }

    public void chartDataReset() {
        xyChart.resetChart();
        xyChart.resetTrackingEnd();
        xyChart.resetFitToWindow();
    }

    public void historyLogging() {
        if (history.isLiveness()) {
            xyChart.setVerticalAxisValueString2("");
        } else {
            xyChart.setVerticalAxisValueString2("B");
        }

        updateBigLegend();
    }

    //    private void createSmallLegend() {
    //        JLabel userThreadsSmall = new JLabel(history.getSeriesName(0), new ColorIcon(history.getSeriesColor(0), null, 8, 8), SwingConstants.LEADING);
    //        userThreadsSmall.setFont(getFont().deriveFont((float)(getFont().getSize()) - 1));
    //        userThreadsSmall.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
    //        
    //        smallLegendPanel = new JPanel();
    //        smallLegendPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 1));
    //        smallLegendPanel.setBackground(Color.WHITE);
    //        smallLegendPanel.setBorder(new LineBorder(Color.GRAY, 1));
    //        smallLegendPanel.add(userThreadsSmall);
    //    }
    protected void updateBigLegend() {
        ((JLabel) (bigLegendPanel.getComponent(0))).setText(history.getSeriesName(0));
        ((JLabel) (bigLegendPanel.getComponent(1))).setText(history.getSeriesName(1));
    }

    private static void appendToolTipItem(StringBuilder toolTipBuffer, String itemName, String itemValue, boolean lastItem) {
        toolTipBuffer.append("<tr><td><b>"); // NOI18N
        toolTipBuffer.append(itemName);
        toolTipBuffer.append("</b>:</td><td>"); // NOI18N
        toolTipBuffer.append(itemValue);
        toolTipBuffer.append("</td></tr>"); // NOI18N

        if (!lastItem) {
            toolTipBuffer.append("<br>"); // NOI18N
        }
    }

    private void createBigLegend() {
        bigLegendPanel = new JPanel();

        JLabel userThreadsBig = new JLabel(history.getSeriesName(0),
                                           new ColorIcon(history.getSeriesColor(0), Color.BLACK, 18, 9), SwingConstants.LEADING);
        userThreadsBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        bigLegendPanel.add(userThreadsBig);
        userThreadsBig = new JLabel(history.getSeriesName(1), new ColorIcon(history.getSeriesColor(1), Color.BLACK, 18, 9),
                                    SwingConstants.LEADING);
        userThreadsBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        bigLegendPanel.add(userThreadsBig);
    }
}

/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package net.java.visualvm.modules.glassfish.ui;

import org.netbeans.lib.profiler.ui.charts.ChartModelListener;
import org.netbeans.lib.profiler.ui.charts.DynamicSynchronousXYChartModel;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChart;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class Chart extends GraphPanel implements ChartModelListener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DynamicSynchronousXYChartModel chartModel;
    private JPanel bigLegendPanel;
    private JPanel smallLegendPanel;
    private SynchronousXYChart xyChart;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Chart() {
        long time = System.currentTimeMillis();

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);

        chartModel = new DynamicSynchronousXYChartModel() {
                public long getMaxDisplayYValue(int seriesIndex) {
                    return getMaxYValue(0);
                }
            };
        setupModel(chartModel);

        xyChart = createChart();
        xyChart.setBackgroundPaint(getBackground());
        xyChart.setModel(chartModel);
        xyChart.setFitToWindow();
        xyChart.setupInitialAppearance(time, time + 1200, 0, 2);
        add(xyChart, BorderLayout.CENTER);

        bigLegendPanel = createBigLegend();
        //            smallLegendPanel = createSmallLegend();
        ToolTipManager.sharedInstance().registerComponent(xyChart);

        //      xyChartModel.addChartModelListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JPanel getBigLegendPanel() {
        return bigLegendPanel;
    }

    public SynchronousXYChart getChart() {
        return xyChart;
    }

    public void setChartModel(DynamicSynchronousXYChartModel chartModel) {
        this.chartModel = chartModel;
    }

    public DynamicSynchronousXYChartModel getChartModel() {
        return chartModel;
    }

    public DynamicSynchronousXYChartModel getModel() {
        return chartModel;
    }

    public JPanel getSmallLegendPanel() {
        return smallLegendPanel;
    }

    public void setToolTipText(String toolTipText) {
        super.setToolTipText(toolTipText);
        xyChart.setToolTipText(toolTipText);
    }

    public void chartDataChanged() {
        //      if (xyChart.isFitToWindow() && xyChartModel.getMaxXValue() - xyChartModel.getMinXValue() >= chartTimeLength) { // after 3 minutes switch from fitToWindow to trackingEnd
        //        xyChart.setTrackingEnd();
        //      }
    }

    protected JPanel createBigLegend() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        legendPanel.setOpaque(false);

        for (int i = 0; i < getModel().getSeriesCount(); i++) {
            JLabel label = new JLabel(getModel().getSeriesName(i),
                                      new ColorIcon(getModel().getSeriesColor(i), Color.BLACK, 18, 9), SwingConstants.LEADING);
            label.setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            legendPanel.add(label);
        }

        legendPanel.setBackground(Color.WHITE);

        return legendPanel;
    }

    protected SynchronousXYChart createChart() {
        SynchronousXYChart xyChart = new SynchronousXYChart(SynchronousXYChart.TYPE_LINE, SynchronousXYChart.VALUES_INTERPOLATED,
                                                            0.01);
        xyChart.setTopChartMargin(20);
        xyChart.denySelection();
        xyChart.setMinimumVerticalMarksDistance(UIManager.getFont("Panel.font").getSize() + 8); // NOI18N

        return xyChart;
    }
    
    protected abstract void setupModel(DynamicSynchronousXYChartModel xyChartModel);
}

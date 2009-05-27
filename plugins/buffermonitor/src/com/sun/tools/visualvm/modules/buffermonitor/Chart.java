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

package com.sun.tools.visualvm.modules.buffermonitor;

import com.sun.tools.visualvm.core.options.GlobalPreferences;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import org.netbeans.lib.profiler.ui.charts.ChartModelListener;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChart;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;

public abstract class Chart extends GraphPanel implements ChartModelListener {
    
    private SynchronousXYChart xyChart;
    private BoundedDynamicXYChartModel xyChartModel;
    private JPanel bigLegendPanel;
    private JPanel smallLegendPanel;
    
    public Chart() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);
        
        xyChartModel = createModel();
        setupModel(xyChartModel);
        
        xyChart = createChart();
        setupChart(xyChart);
        add(xyChart, BorderLayout.CENTER);
        
        bigLegendPanel = createBigLegend();
        smallLegendPanel = createSmallLegend();
        
        ToolTipManager.sharedInstance().registerComponent(xyChart);
        
        //      xyChartModel.addChartModelListener(this);
    }
    
    protected BoundedDynamicXYChartModel createModel() {
        GlobalPreferences preferences = GlobalPreferences.sharedInstance();
        return new BoundedDynamicXYChartModel(preferences.getMonitoredDataCache() * 60 / preferences.getMonitoredDataPoll()) {
            public  long    getMaxDisplayYValue(int seriesIndex)      { return getMaxYValue(1); }
        };
    }
    
    protected void setupChart(SynchronousXYChart xyChart) {
        long time = System.currentTimeMillis();
        
        xyChart.setBackgroundPaint(getBackground());
        xyChart.setModel(xyChartModel);
        xyChart.setFitToWindow();
        xyChart.setupInitialAppearance(time, time + 1200, 0, 2);
    }
    
    protected abstract SynchronousXYChart createChart();
    protected abstract void setupModel(BoundedDynamicXYChartModel xyChartModel);
    protected abstract JPanel createBigLegend();
    protected abstract JPanel createSmallLegend();
    
    public SynchronousXYChart getChart() {
        return xyChart;
    };
    
    public BoundedDynamicXYChartModel getModel() {
        return xyChartModel;
    }
    
    public JPanel getBigLegendPanel() {
        return bigLegendPanel;
    };
    
    public JPanel getSmallLegendPanel() {
        return smallLegendPanel;
    }
    
    public void chartDataChanged() {
        //      if (xyChart.isFitToWindow() && xyChartModel.getMaxXValue() - xyChartModel.getMinXValue() >= chartTimeLength) { // after 3 minutes switch from fitToWindow to trackingEnd
        //        xyChart.setTrackingEnd();
        //      }
    }
    
    public void setToolTipText(String toolTipText) {
        super.setToolTipText(toolTipText);
        xyChart.setToolTipText(toolTipText);
    }
    
}




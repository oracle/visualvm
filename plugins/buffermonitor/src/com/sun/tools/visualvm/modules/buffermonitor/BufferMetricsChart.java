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
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChart;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.openide.util.NbBundle;

/**
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 * 
 */ 
public class BufferMetricsChart extends Chart {
    
    protected void setupModel(BoundedDynamicXYChartModel xyChartModel) {
        xyChartModel.setupModel(new String[] { NbBundle.getMessage(BufferMetricsChart.class, "LBL_Memory_Used"),   // NOI18N
        NbBundle.getMessage(BufferMetricsChart.class, "LBL_Total_Capacity") },   // NOI18N
                new Color[]  { new Color(255, 127, 127), new Color(127, 63, 191) } );
    }
    
    protected SynchronousXYChart createChart() {
        return new SynchronousXYChart(SynchronousXYChart.TYPE_LINE, SynchronousXYChart.VALUES_INTERPOLATED, 0.01);
    }
    
    protected void setupChart(SynchronousXYChart xyChart) {
        super.setupChart(xyChart);
        long time = System.currentTimeMillis();
        
        xyChart.setVerticalAxisValueDivider(1024*1024);
        xyChart.setupInitialAppearance(time, time + 1200, 0, 50*1024);
        xyChart.setVerticalAxisValueString("M"); // NOI18N
        xyChart.setTopChartMargin(5);
        xyChart.denySelection();
        xyChart.setMinimumVerticalMarksDistance(UIManager.getFont("Panel.font").getSize() + 8); // NOI18N
    }
    
    protected JPanel createBigLegend() {
        JLabel memoryUsed = new JLabel(getModel().getSeriesName(0), new ColorIcon(getModel().getSeriesColor(0), Color.BLACK, 18, 9), SwingConstants.LEADING);
        memoryUsed.setOpaque(false);
        memoryUsed.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        JLabel totalCapacity = new JLabel(getModel().getSeriesName(1), new ColorIcon(getModel().getSeriesColor(1), Color.BLACK, 18, 9), SwingConstants.LEADING);
        totalCapacity.setOpaque(false);
        totalCapacity.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        
        JPanel legendPanel = new JPanel();
        legendPanel.setOpaque(false);
        legendPanel.add(memoryUsed);
        legendPanel.add(totalCapacity);
        
        return legendPanel;
    }
    
    protected JPanel createSmallLegend() {
        return null;
    }
    
}

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
package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ServletMonitor;
import com.sun.appserv.management.monitor.statistics.AltServletStats;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.scheduler.Quantum;
import org.graalvm.visualvm.core.scheduler.ScheduledTask;
import org.graalvm.visualvm.core.scheduler.Scheduler;
import org.graalvm.visualvm.core.scheduler.SchedulerTask;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceViewProvider;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import java.lang.reflect.UndeclaredThrowableException;
import net.java.visualvm.modules.glassfish.datasource.GlassFishServlet;
import org.openide.util.ImageUtilities;
import java.awt.BorderLayout;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishServletViewProvider extends DataSourceViewProvider<GlassFishServlet> {
    private final static GlassFishServletViewProvider INSTANCE = new GlassFishServletViewProvider();
    private final Map<GlassFishServlet, GlassfishServletView> viewMap = new  HashMap<GlassFishServlet, GlassFishServletViewProvider.GlassfishServletView>();
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class GlassfishServletView extends DataSourceView {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final Image NODE_ICON = ImageUtilities.loadImage("net/java/visualvm/modules/glassfish/resources/servlet_icon.png",
                                                                   true);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private SimpleXYChartSupport reqsChart;
        private SimpleXYChartSupport timesChart;
        private DataViewComponent dvc;
        private GlassFishServlet servlet;

        private ScheduledTask refreshTask;
        
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GlassfishServletView(GlassFishServlet servlet) {
            super(servlet, servlet.getName(), NODE_ICON, 0, true);

            this.servlet = servlet;

            HTMLTextArea generalDataArea = new HTMLTextArea();

            JPanel chartTimesPanel = new JPanel(new BorderLayout());
            chartTimesPanel.setOpaque(false);
            SimpleXYChartDescriptor desc = SimpleXYChartDescriptor.decimal(10, false, 500);
            desc.addLineItems("Average Time","Maximum Time");
            timesChart = ChartFactory.createSimpleXYChart(desc);

            chartTimesPanel.add(timesChart.getChart(), BorderLayout.CENTER);

            JPanel chartReqsPanel = new JPanel(new BorderLayout());
            chartReqsPanel.setOpaque(false);
            desc = SimpleXYChartDescriptor.decimal(10, false, 500);
            desc.addLineItems("Request Count","Error Count");
            reqsChart = ChartFactory.createSimpleXYChart(desc);
            chartReqsPanel.add(reqsChart.getChart(), BorderLayout.CENTER);

            DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("Overview", null, generalDataArea);
            DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            dvc = new DataViewComponent(masterView, masterConfiguration);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Time Profile", true),
                                     DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Time Profile", null, 10, chartTimesPanel, null),
                               DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Requests", true),
                                     DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Requests", null, 10, chartReqsPanel, null),
                               DataViewComponent.BOTTOM_LEFT);

            refreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

                public void onSchedule(long timeStamp) {
                    try {
                    refreshData(timeStamp);
                    } catch (Exception e) {
                        if (!(e instanceof UndeclaredThrowableException)) {
                        System.out.println("Error: "+e.getMessage());
                        e.printStackTrace();
                        } else {
                        Scheduler.sharedInstance().unschedule(refreshTask);
                        refreshTask = null;
                        }
                    }
                }
            }, Quantum.seconds(3));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------
        @Override
        public DataViewComponent createComponent() {
            return dvc;
        }

        private void refreshData(long sampleTime) {
            ServletMonitor monitor = servlet.getMonitor();
            AltServletStats stats = monitor.getAltServletStats();
            timesChart.addValues(sampleTime,
                                     new long[] {
                                         Math.round((double) stats.getProcessingTime().getCount() / (double) stats.getRequestCount()
                                                                                                                  .getCount()),
                                         stats.getMaxTime().getCount()
                                     });
            reqsChart.addValues(sampleTime,
                                    new long[] { stats.getRequestCount().getCount(), stats.getErrorCount().getCount() });
        }
    }

    private GlassFishServletViewProvider() {}

    @Override
    protected DataSourceView createView(GlassFishServlet servlet) {
        return new GlassfishServletView(servlet);
    }

    @Override
    protected boolean supportsViewFor(GlassFishServlet servlet) {
        return true;
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(INSTANCE, GlassFishServlet.class);
    }
    
    public static void shutdown() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(INSTANCE);
        INSTANCE.viewMap.clear();
    }

    public boolean supportsViewsFor(GlassFishServlet dataSource) {
        return true;
    }
    
    public void saveViews(GlassFishServlet servlet, Snapshot snapshot) {
        // TODO implement later
    }

    public boolean supportsSaveViewsFor(GlassFishServlet servlet) {
        return false;
    }
    
}

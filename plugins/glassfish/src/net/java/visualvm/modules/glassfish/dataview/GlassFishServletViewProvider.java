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
package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ServletMonitor;
import com.sun.appserv.management.monitor.statistics.AltServletStats;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import org.netbeans.lib.profiler.ui.charts.DynamicSynchronousXYChartModel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import net.java.visualvm.modules.glassfish.datasource.GlassFishServlet;
import org.openide.util.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import net.java.visualvm.modules.glassfish.ui.Chart;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishServletViewProvider implements DataSourceViewsProvider<GlassFishServlet> {
    private final static GlassFishServletViewProvider INSTANCE = new GlassFishServletViewProvider();
    private final Map<GlassFishServlet, GlassfishServletView> viewMap = new  HashMap<GlassFishServlet, GlassFishServletViewProvider.GlassfishServletView>();
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class GlassfishServletView extends DataSourceView {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final Image NODE_ICON = Utilities.loadImage("net/java/visualvm/modules/glassfish/resources/servlet_icon.png",
                                                                   true);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Chart reqsChart;
        private Chart timesChart;
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
            timesChart = new Chart() {
                    @Override
                    protected void setupModel(DynamicSynchronousXYChartModel xyChartModel) {
                        xyChartModel.setupModel(new String[] { "Average Time", "Maximum Time" },
                                                new Color[] { Color.BLUE, Color.RED });
                    }
                };
            chartTimesPanel.add(timesChart, BorderLayout.CENTER);
            chartTimesPanel.add(timesChart.getBigLegendPanel(), BorderLayout.SOUTH);

            JPanel chartReqsPanel = new JPanel(new BorderLayout());
            chartReqsPanel.setOpaque(false);
            reqsChart = new Chart() {
                    @Override
                    protected void setupModel(DynamicSynchronousXYChartModel xyChartModel) {
                        xyChartModel.setupModel(new String[] { "Request Count", "Error Count" },
                                                new Color[] { Color.BLUE, Color.RED });
                    }
                };
            chartReqsPanel.add(reqsChart, BorderLayout.CENTER);
            chartReqsPanel.add(reqsChart.getBigLegendPanel(), BorderLayout.SOUTH);

            DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("Overview", null, generalDataArea);
            DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            dvc = new DataViewComponent(masterView, masterConfiguration);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Time Profile", true),
                                     DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Time Profile", null, chartTimesPanel, null),
                               DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Requests", true),
                                     DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Requests", null, chartReqsPanel, null),
                               DataViewComponent.BOTTOM_LEFT);

            refreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

                public void onSchedule(long timeStamp) {
                    try {
                    refreshData(timeStamp);
                    } catch (Exception e) {
                        Scheduler.sharedInstance().unschedule(refreshTask);
                        refreshTask = null;
                    }
                }
            }, Quantum.seconds(3));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------
        @Override
        public DataViewComponent getView() {
            return dvc;
        }

        private void refreshData(long sampleTime) {
            ServletMonitor monitor = servlet.getMonitor();
            AltServletStats stats = monitor.getAltServletStats();
            timesChart.getModel()
                      .addItemValues(sampleTime,
                                     new long[] {
                                         Math.round((double) stats.getProcessingTime().getCount() / (double) stats.getRequestCount()
                                                                                                                  .getCount()),
                                         stats.getMaxTime().getCount()
                                     });
            reqsChart.getModel()
                     .addItemValues(sampleTime,
                                    new long[] { stats.getRequestCount().getCount(), stats.getErrorCount().getCount() });
        }
    }

    private GlassFishServletViewProvider() {}
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Set<?extends DataSourceView> getViews(GlassFishServlet dataSource) {
        synchronized(viewMap) {
            if (viewMap.containsKey(dataSource)) {
                return Collections.singleton(viewMap.get(dataSource));
            } else {
                GlassfishServletView view = new GlassfishServletView(dataSource);
                viewMap.put(dataSource, view);
                return Collections.singleton(view);
            }
        }
    }

    public static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewsProvider(INSTANCE, GlassFishServlet.class);
    }
    
    public static void shutdown() {
        DataSourceViewsManager.sharedInstance().removeViewsProvider(INSTANCE);
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

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

import net.java.visualvm.modules.glassfish.ui.StatsTable;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.appserv.management.monitor.statistics.WebModuleVirtualServerStats;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import com.sun.tools.visualvm.core.ui.DataSourceUIFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import org.netbeans.lib.profiler.ui.charts.DynamicSynchronousXYChartModel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import net.java.visualvm.modules.glassfish.datasource.GlassFishWebModule;
import net.java.visualvm.modules.glassfish.explorer.GlassFishApplicationNode;
import net.java.visualvm.modules.glassfish.ui.Chart;
import org.openide.util.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishWebModuleViewProvider implements DataSourceViewProvider<GlassFishWebModule> {
    
    private static class GlassfishWebModuleView extends DataSourceView {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final Image NODE_ICON = Utilities.loadImage("net/java/visualvm/modules/glassfish/resources/application.png",
                                                                   true);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Chart activeSessionsChart;
        private Chart jspChart;
        private Chart totalSessionsChart;
        private DataViewComponent dvc;
        private TableModel servletsModel;
        private TableModel wsModel;
        private GlassFishWebModule module;
        private ScheduledTask refreshTask;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GlassfishWebModuleView(GlassFishWebModule webModule) {
            super("Overview", NODE_ICON, 0);

            module = webModule;

            HTMLTextArea generalDataArea = new HTMLTextArea();

            JPanel chartActiveSessionsPanel = new JPanel(new BorderLayout());
            chartActiveSessionsPanel.setOpaque(false);
            activeSessionsChart = new Chart() {
                    @Override
                    protected void setupModel(DynamicSynchronousXYChartModel xyChartModel) {
                        xyChartModel.setupModel(new String[] { "Current", "Maximum" }, new Color[] { Color.BLUE, Color.RED });
                    }
                };
            chartActiveSessionsPanel.add(activeSessionsChart, BorderLayout.CENTER);
            chartActiveSessionsPanel.add(activeSessionsChart.getBigLegendPanel(), BorderLayout.SOUTH);

            JPanel chartTotalSessionsPanel = new JPanel(new BorderLayout());
            chartTotalSessionsPanel.setOpaque(false);
            totalSessionsChart = new Chart() {
                    @Override
                    protected void setupModel(DynamicSynchronousXYChartModel xyChartModel) {
                        xyChartModel.setupModel(new String[] { "Created", "Expired", "Rejected" },
                                                new Color[] { Color.BLUE, Color.GREEN, Color.RED });
                    }
                };
            chartTotalSessionsPanel.add(totalSessionsChart, BorderLayout.CENTER);
            chartTotalSessionsPanel.add(totalSessionsChart.getBigLegendPanel(), BorderLayout.SOUTH);

            JPanel chartJspPanel = new JPanel(new BorderLayout());
            chartJspPanel.setOpaque(false);
            jspChart = new Chart() {
                    @Override
                    protected void setupModel(DynamicSynchronousXYChartModel xyChartModel) {
                        xyChartModel.setupModel(new String[] { "Count", "Reloads", "Errors" },
                                                new Color[] { Color.BLUE, Color.GREEN, Color.RED });
                    }
                };
            chartJspPanel.add(jspChart, BorderLayout.CENTER);
            chartJspPanel.add(jspChart.getBigLegendPanel(), BorderLayout.SOUTH);

            JPanel servletsPanel = new JPanel(new BorderLayout());
            servletsPanel.setOpaque(false);
            servletsModel = new ServletTableModel(webModule.getMonitor(), Quantum.seconds(5));

            RowSorter<TableModel> servletsRowSorter = new TableRowSorter<TableModel>(servletsModel);

            StatsTable servletsTable = new StatsTable(servletsModel);
            servletsTable.setRowSorter(servletsRowSorter);
            servletsTable.setOpaque(false);

            JScrollPane servletsScroller = new JScrollPane(servletsTable);
            servletsScroller.setOpaque(false);
            servletsPanel.add(servletsScroller, BorderLayout.CENTER);

            JPanel wsPanel = new JPanel(new BorderLayout());
            wsPanel.setOpaque(false);
            wsModel = new WSTableModel(webModule.getMonitor(), Quantum.seconds(5));

            RowSorter<TableModel> wsRowSorter = new TableRowSorter<TableModel>(wsModel);

            StatsTable wsTable = new StatsTable(wsModel);
            wsTable.setRowSorter(wsRowSorter);
            wsTable.setOpaque(false);

            JScrollPane wsScroller = new JScrollPane(wsTable);
            wsScroller.setOpaque(false);
            wsPanel.add(wsScroller, BorderLayout.CENTER);

            DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("Overview", null, generalDataArea);
            DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            dvc = new DataViewComponent(masterView, masterConfiguration);
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Sessions", true), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Sessions Active", null, chartActiveSessionsPanel, null),
                               DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Sessions Total", null, chartTotalSessionsPanel, null),
                               DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("JSPs", true), DataViewComponent.TOP_RIGHT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("JSPs", null, chartJspPanel, null), DataViewComponent.TOP_RIGHT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Runtime", true),
                                     DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Servlets", null, servletsPanel, null),
                               DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("WebServices", null, wsPanel, null),
                               DataViewComponent.BOTTOM_LEFT);

            refreshTask = Scheduler.getSharedInstance().schedule(new SchedulerTask() {
                public void onSchedule(long timeStamp) {
                    refreshData(timeStamp);
                }
            }, Quantum.seconds(5));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public DataViewComponent getView() {
            return dvc;
        }

        private void refreshData(long sampleTime) {
            WebModuleVirtualServerMonitor monitor = module.getMonitor();
            WebModuleVirtualServerStats stats = monitor.getWebModuleVirtualServerStats();

            activeSessionsChart.getModel()
                               .addItemValues(sampleTime,
                                              new long[] {
                                                  stats.getActiveSessionsCurrent().getCount(),
                                                  stats.getActiveSessionsHigh().getCount()
                                              });
            totalSessionsChart.getModel()
                              .addItemValues(sampleTime,
                                             new long[] {
                                                 stats.getSessionsTotal().getCount(), stats.getExpiredSessionsTotal().getCount(),
                                                 stats.getRejectedSessionsTotal().getCount()
                                             });
            jspChart.getModel()
                    .addItemValues(sampleTime,
                                   new long[] {
                                       stats.getJSPCount().getCount(), stats.getJSPReloadCount().getCount(),
                                       stats.getJSPErrorCount().getCount()
                                   });

        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static ExplorerActionsProvider<GlassFishApplicationNode> actionsProvider = new ExplorerActionsProvider<GlassFishApplicationNode>() {
        private List<ExplorerActionDescriptor> actions = new ArrayList<ExplorerActionDescriptor>() {

            {
                add(new ExplorerActionDescriptor(new AbstractAction("Start") {
                        public void actionPerformed(ActionEvent e) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    }, 0));
                add(new ExplorerActionDescriptor(new AbstractAction("Stop") {
                        public void actionPerformed(ActionEvent e) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    }, 1));
            }
        };

        public ExplorerActionDescriptor getDefaultAction(GlassFishApplicationNode node) {
            return null;

            //            return new ExplorerActionDescriptor(new AbstractAction("Open") {
            //
            //                public void actionPerformed(ActionEvent e) {
            //                    throw new UnsupportedOperationException("Not supported yet.");
            //                }
            //            }, 0);
        }

        public List<ExplorerActionDescriptor> getActions(GlassFishApplicationNode node) {
            return actions;
        }
    };


    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Set<?extends DataSourceView> getViews(GlassFishWebModule dataSource) {
        return Collections.singleton(new GlassfishWebModuleView(dataSource));
    }

    public void initialize() {
        DataSourceUIFactory.sharedInstance().addViewProvider(this, GlassFishWebModule.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(actionsProvider, GlassFishApplicationNode.class);
    }

    public boolean supportsViewFor(GlassFishWebModule dataSource) {
        return true;
    }
}

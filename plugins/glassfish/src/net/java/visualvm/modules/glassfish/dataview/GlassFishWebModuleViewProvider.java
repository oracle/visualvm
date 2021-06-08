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

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import net.java.visualvm.modules.glassfish.ui.StatsTable;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.appserv.management.monitor.statistics.WebModuleVirtualServerStats;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.scheduler.Quantum;
import org.graalvm.visualvm.core.scheduler.ScheduledTask;
import org.graalvm.visualvm.core.scheduler.Scheduler;
import org.graalvm.visualvm.core.scheduler.SchedulerTask;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceViewProvider;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.uisupport.HTMLLabel;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import net.java.visualvm.modules.glassfish.datasource.GlassFishWebModule;

import org.openide.util.ImageUtilities;
import java.awt.BorderLayout;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishWebModuleViewProvider extends DataSourceViewProvider<GlassFishWebModule> {
    private final static GlassFishWebModuleViewProvider INSTANCE = new GlassFishWebModuleViewProvider();
    private final static Logger LOGGER = Logger.getLogger(GlassFishWebModuleViewProvider.class.getName());
    
    private final Map<GlassFishWebModule, GlassfishWebModuleView> viewMap = new  HashMap<GlassFishWebModule, GlassFishWebModuleViewProvider.GlassfishWebModuleView>();
    
    private static class GlassfishWebModuleView extends DataSourceView {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final Image NODE_ICON = ImageUtilities.loadImage("net/java/visualvm/modules/glassfish/resources/application.png",
                                                                   true);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private SimpleXYChartSupport activeSessionsChart;
        private SimpleXYChartSupport jspChart;
        private SimpleXYChartSupport totalSessionsChart;
        private DataViewComponent dvc;
        private TableModel servletsModel;
        private TableModel wsModel;
        private GlassFishWebModule module;
        private ScheduledTask refreshTask;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GlassfishWebModuleView(GlassFishWebModule webModule) {
            super(webModule, "Overview", NODE_ICON, 0, true);

            module = webModule;
            JPanel masterPanel = new JPanel(new BorderLayout());
            masterPanel.setOpaque(false);
            
            HTMLTextArea generalDataArea = new HTMLTextArea();
            generalDataArea.setText(buildInfo());
            generalDataArea.setBorder(BorderFactory.createEmptyBorder());
//            generalDataArea.setOpaque(false);
            
            JScrollPane generalDataScroll = new JScrollPane(generalDataArea);
            generalDataScroll.setViewportBorder(BorderFactory.createEmptyBorder());
            generalDataScroll.setBorder(BorderFactory.createEmptyBorder());
            generalDataScroll.setOpaque(false);
            
            HTMLLabel appLink = new HTMLLabel("<html><body><h2>Application hosted by <a href=\"#\">" + DataSourceDescriptorFactory.getDescriptor(module.getGlassFishRoot().getApplication()).getName()+ "</a></h2></body></html>") {
                protected void showURL(URL url) {
                    DataSourceWindowManager.sharedInstance().openDataSource(module.getGlassFishRoot().getApplication());
                }
            };
            masterPanel.add(generalDataScroll, BorderLayout.CENTER);
            masterPanel.add(appLink, BorderLayout.NORTH);

            SimpleXYChartDescriptor desc = SimpleXYChartDescriptor.decimal(10, false, 500);
            desc.addLineItems("Current","Maximum");

            activeSessionsChart = ChartFactory.createSimpleXYChart(desc);

            desc = SimpleXYChartDescriptor.decimal(10, true, 500);
            desc.addLineItems("Created","Expired","Rejected");
            totalSessionsChart = ChartFactory.createSimpleXYChart(desc);

            desc = SimpleXYChartDescriptor.decimal(10, false, 500);
            desc.addLineItems("Count","Reloads","Errors");
            jspChart = ChartFactory.createSimpleXYChart(desc);

            JPanel servletsPanel = new JPanel(new BorderLayout());
            servletsPanel.setOpaque(false);
            servletsModel = new ServletTableModel(webModule.getMonitor(), Quantum.seconds(5));

            RowSorter<TableModel> servletsRowSorter = new TableRowSorter<TableModel>(servletsModel);

            StatsTable servletsTable = new StatsTable(servletsModel);
            servletsTable.setRowSorter(servletsRowSorter);

            JScrollPane servletsScroller = new JScrollPane(servletsTable);
            servletsScroller.getViewport().setBackground(generalDataArea.getBackground());
            servletsPanel.add(servletsScroller, BorderLayout.CENTER);

            JPanel wsPanel = new JPanel(new BorderLayout());
            wsPanel.setOpaque(false);
            wsModel = new WSTableModel(webModule.getMonitor(), Quantum.seconds(5));

            RowSorter<TableModel> wsRowSorter = new TableRowSorter<TableModel>(wsModel);

            StatsTable wsTable = new StatsTable(wsModel);
            wsTable.setRowSorter(wsRowSorter);

            JScrollPane wsScroller = new JScrollPane(wsTable);
            wsScroller.getViewport().setBackground(generalDataArea.getBackground());
            wsPanel.add(wsScroller, BorderLayout.CENTER);

            DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("Overview", null, masterPanel);
            DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            dvc = new DataViewComponent(masterView, masterConfiguration);
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Sessions", true), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Sessions Active", null, 10, activeSessionsChart.getChart(), null),
                               DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Sessions Total", null, 20, totalSessionsChart.getChart(), null),
                               DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("JSPs", true), DataViewComponent.TOP_RIGHT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("JSPs", null, 10, jspChart.getChart(), null), DataViewComponent.TOP_RIGHT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Runtime", true),
                                     DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Servlets", null, 10, servletsPanel, null),
                               DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("WebServices", null, 20, wsPanel, null),
                               DataViewComponent.BOTTOM_LEFT);
            
            refreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {
                public void onSchedule(long timeStamp) {
                    try {
                        refreshData(timeStamp);
                    } catch (Exception e) {
                        if (!(e instanceof UndeclaredThrowableException)) {
                            Logger.getLogger(GlassFishWebModuleViewProvider.class.getName()).log(Level.INFO,"onSchedule",e);
                        } else {
                            Scheduler.sharedInstance().unschedule(refreshTask);
                            refreshTask = null;
                        }
                    }
                }
            }, Quantum.seconds(5));
        }

        @Override
        protected DataViewComponent createComponent() {
            return dvc;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        private String buildInfo() {
                JmxModel jmx = JmxModelFactory.getJmxModelFor(module.getGlassFishRoot().getApplication());
                StringBuilder sb = new StringBuilder();
            try {
                ObjectName objName = new ObjectName(module.getObjectName());
                MBeanServerConnection connection = jmx.getMBeanServerConnection();
                sb.append("<br/>");
                sb.append("<b>Context: </b>").append(connection.getAttribute(objName, "path")).append("<br/>");
                sb.append("<b>Document Base: </b>").append(connection.getAttribute(objName, "docBase")).append("<br/>");
                sb.append("<b>Working Dir: </b>").append(connection.getAttribute(objName, "workDir")).append("<br/>");
                sb.append("<br/>");
                boolean cacheAllowed = (Boolean)connection.getAttribute(objName, "cachingAllowed");
                sb.append("<b>Caching: </b>").append(cacheAllowed ? "Allowed" : "Disallowed").append("<br/>");
                sb.append("<br/>");
            } catch (MBeanException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            } catch (AttributeNotFoundException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            } catch (InstanceNotFoundException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            } catch (ReflectionException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            } catch (IOException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            } catch (MalformedObjectNameException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            } catch (NullPointerException ex) {
                LOGGER.throwing(GlassFishWebModuleViewProvider.class.getName(), "buildInfo", ex);
            }
            return sb.toString();
        }
        
        private void refreshData(long sampleTime) {
            WebModuleVirtualServerMonitor monitor = module.getMonitor();
            WebModuleVirtualServerStats stats = monitor.getWebModuleVirtualServerStats();
            activeSessionsChart.addValues(sampleTime,
                                              new long[] {
                                                  stats.getActiveSessionsCurrent().getCount(),
                                                  stats.getActiveSessionsHigh().getCount()
                                              });
            totalSessionsChart.addValues(sampleTime,
                                             new long[] {
                                                 stats.getSessionsTotal().getCount(), stats.getExpiredSessionsTotal().getCount(),
                                                 stats.getRejectedSessionsTotal().getCount()
                                             });
            jspChart.addValues(sampleTime,
                                   new long[] {
                                       stats.getJSPCount().getCount(), stats.getJSPReloadCount().getCount(),
                                       stats.getJSPErrorCount().getCount()
                                   });

        }
    }

    private GlassFishWebModuleViewProvider() {}

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(INSTANCE, GlassFishWebModule.class);
    }
    
    public static void shutdown() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(INSTANCE);
        INSTANCE.viewMap.clear();
    }

    @Override
    protected DataSourceView createView(GlassFishWebModule webModule) {
        return new GlassfishWebModuleView(webModule);
    }

    @Override
    protected boolean supportsViewFor(GlassFishWebModule webModule) {
        return true;
    }
}

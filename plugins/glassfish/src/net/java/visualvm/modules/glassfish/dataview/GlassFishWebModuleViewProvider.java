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
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsFactory;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import net.java.visualvm.modules.glassfish.datasource.GlassFishWebModule;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.data.Range;
import org.jfree.data.general.DefaultValueDataset;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishWebModuleViewProvider implements DataSourceViewsProvider<GlassFishWebModule> {
    private final static GlassFishWebModuleViewProvider INSTANCE = new GlassFishWebModuleViewProvider();
    private final Map<GlassFishWebModule, GlassfishWebModuleView> viewMap = new  HashMap<GlassFishWebModule, GlassFishWebModuleViewProvider.GlassfishWebModuleView>();
    
    private static class GlassfishWebModuleView extends DataSourceView {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final Image NODE_ICON = Utilities.loadImage("net/java/visualvm/modules/glassfish/resources/application.png",
                                                                   true);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private MeterPlot trafficPlot;
        private DataViewComponent dvc;
        private GlassFishWebModule module;
        private ScheduledTask refreshTask;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GlassfishWebModuleView(GlassFishWebModule webModule) {
            super("Overview", NODE_ICON, 0);

            module = webModule;

            JPanel masterPanel = new JPanel(new BorderLayout());
            masterPanel.setOpaque(false);
            
            HTMLTextArea generalDataArea = new HTMLTextArea();
            generalDataArea.setText(buildInfo());
            generalDataArea.setBorder(BorderFactory.createEmptyBorder());
            generalDataArea.setOpaque(false);
            
            JScrollPane generalDataScroll = new JScrollPane(generalDataArea);
            generalDataScroll.setViewportBorder(BorderFactory.createEmptyBorder());
            generalDataScroll.setBorder(BorderFactory.createEmptyBorder());
            generalDataScroll.setOpaque(false);
            
            JLabel appLink = new JLabel("<html><body><h2>Application hosted by <a href=\"#\">" + DataSourceDescriptorFactory.getDescriptor(module.getGlassFishRoot().getApplication()).getName()+ "</a></h2></body></html>");
            appLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
            appLink.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    DataSourceWindowManager.sharedInstance().openDataSource(module.getGlassFishRoot().getApplication());
                }
            });
            masterPanel.add(generalDataScroll, BorderLayout.CENTER);
            masterPanel.add(appLink, BorderLayout.NORTH);
            
            trafficPlot = new MeterPlot();
            
            
            DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("Overview", null, masterPanel);
            DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            dvc = new DataViewComponent(masterView, masterConfiguration);
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Statistics", true), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(new DataViewComponent.DetailsView("Statistics", null, getTrafficChart(), null),
                               DataViewComponent.TOP_LEFT);
            
            refreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {
                public void onSchedule(long timeStamp) {
                    refreshData(timeStamp);
                }
            }, Quantum.seconds(5));
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
                Exceptions.printStackTrace(ex);
            } catch (AttributeNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InstanceNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ReflectionException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (MalformedObjectNameException ex) {
                Exceptions.printStackTrace(ex);
            } catch (NullPointerException ex) {
                Exceptions.printStackTrace(ex);
            }
            return sb.toString();
        }
        
        @Override
        public DataViewComponent getView() {
            return dvc;
        }

        private void refreshData(long sampleTime) {
            WebModuleVirtualServerMonitor monitor = module.getMonitor();
            long serviceTime = 0L;
            long requestCount = 0L;
            for(Map.Entry<String, ServletMonitor> entry : monitor.getServletMonitorMap().entrySet()) {
                serviceTime += entry.getValue().getAltServletStats().getProcessingTime().getCount();
                requestCount += entry.getValue().getAltServletStats().getRequestCount().getCount();
            }
            long throughput = (serviceTime == 0L || requestCount == 0L) ? 0L : Math.round(1000d / (serviceTime / requestCount));
            
            trafficPlot.setDataset(new DefaultValueDataset(throughput));
        }
        
        private JComponent getTrafficChart() {
            trafficPlot.setRange(new Range(0d, 300d));
            trafficPlot.addInterval(new MeterInterval("Low", new Range(0d, 20d), Color.BLACK, null, Color.RED));
            trafficPlot.addInterval(new MeterInterval("Medium", new Range(20d, 200d), Color.BLACK, null, Color.YELLOW));
            trafficPlot.addInterval(new MeterInterval("High", new Range(200d, 300d), Color.BLACK, null, Color.GREEN));
            
            trafficPlot.setDialShape(DialShape.CHORD);
            trafficPlot.setUnits("requests/sec");
            trafficPlot.setDialBackgroundPaint(Color.LIGHT_GRAY);
            trafficPlot.setDrawBorder(false);
            trafficPlot.setNeedlePaint(Color.BLACK);
            
            trafficPlot.setValuePaint(Color.BLUE);
            
            trafficPlot.setBackgroundPaint(Color.WHITE);

            JFreeChart chart = new JFreeChart(trafficPlot);
            chart.setAntiAlias(true);
            chart.setBackgroundPaint(Color.WHITE);
            chart.setBorderVisible(false);
            
            ChartPanel meterPanel = new ChartPanel(chart);
            meterPanel.setOpaque(false);
            meterPanel.setBackground(Color.WHITE);
            meterPanel.setBorder(BorderFactory.createTitledBorder("Traffic"));
            
            return meterPanel;
        }
    }

    private GlassFishWebModuleViewProvider() {}

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Set<?extends DataSourceView> getViews(GlassFishWebModule dataSource) {
        synchronized(viewMap) {
            if (viewMap.containsKey(dataSource)) {
                return Collections.singleton(viewMap.get(dataSource));
            } else {
                GlassfishWebModuleView view = new GlassfishWebModuleView(dataSource);
                viewMap.put(dataSource, view);
                return Collections.singleton(view);
            }
        }
    }

    public static void initialize() {
        DataSourceViewsFactory.sharedInstance().addViewProvider(INSTANCE, GlassFishWebModule.class);
    }
    
    public static void shutdown() {
        DataSourceViewsFactory.sharedInstance().removeViewProvider(INSTANCE);
        INSTANCE.viewMap.clear();
    }

    public boolean supportsViewsFor(GlassFishWebModule dataSource) {
        return true;
    }
}

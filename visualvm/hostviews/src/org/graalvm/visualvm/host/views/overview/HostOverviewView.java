/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.host.views.overview;

import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.host.model.HostOverview;
import org.graalvm.visualvm.host.model.HostOverviewFactory;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class HostOverviewView extends DataSourceView implements DataRemovedListener<Host> {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/host/views/resources/overview.png";    // NOI18N

    private Timer timer;
    private HostOverview hostOverview;
    

    HostOverviewView(Host host) {
        super(host, NbBundle.getMessage(HostOverviewView.class, "LBL_Overview"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 0, false);    // NOI18N
    }
    
    protected void willBeAdded() {
        hostOverview = HostOverviewFactory.getSystemOverviewFor((Host)getDataSource());
    }
        
    protected void removed() {
        timer.stop();
    }
    
    public void dataRemoved(Host dataSource) {
        timer.stop();
    }
    
    
    protected DataViewComponent createComponent() {
        GlobalPreferences preferences = GlobalPreferences.sharedInstance();
        int chartCache = preferences.getMonitoredHostCache() * 60 /
                         preferences.getMonitoredHostPoll();

        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport((Host)getDataSource()).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));

        boolean cpuSupported = hostOverview.getSystemLoadAverage() >= 0;
        final CpuLoadViewSupport cpuLoadViewSupport = new CpuLoadViewSupport(hostOverview, cpuSupported, chartCache);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(HostOverviewView.class, "LBL_CPU"), true), DataViewComponent.TOP_LEFT); // NOI18N
        dvc.addDetailsView(cpuLoadViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        if (!cpuSupported) dvc.hideDetailsArea(DataViewComponent.TOP_LEFT);

        final PhysicalMemoryViewSupport physicalMemoryViewSupport = new PhysicalMemoryViewSupport(chartCache);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(HostOverviewView.class, "LBL_Memory"), true), DataViewComponent.TOP_RIGHT); // NOI18N
        dvc.addDetailsView(physicalMemoryViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);

        final SwapMemoryViewSupport swapMemoryViewSupport = new SwapMemoryViewSupport(chartCache);
        dvc.addDetailsView(swapMemoryViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);

        timer = new Timer(2000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final long time = System.currentTimeMillis();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        cpuLoadViewSupport.refresh(hostOverview, time);
                        physicalMemoryViewSupport.refresh(hostOverview, time);
                        swapMemoryViewSupport.refresh(hostOverview, time);
                    }
                });
            }
        });
        timer.setInitialDelay(800);
        timer.start();
        ((Host)getDataSource()).notifyWhenRemoved(this);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel {
        
        MasterViewSupport(Host host) {
            initComponents(host);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(HostOverviewView.class, "LBL_Overview"), null, this);   // NOI18N
        }
        
        
        private void initComponents(Host host) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralInfo(host) + "</nobr>");  // NOI18N
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            
            add(area, BorderLayout.CENTER);
        }
        
        String getGeneralInfo(Host host) {
            HostOverview so = HostOverviewFactory.getSystemOverviewFor(host);
            StringBuilder data = new StringBuilder();
            String hostIp = NbBundle.getMessage(HostOverviewView.class, "LBL_Host_IP"); // NOI18N
            data.append("<b>"+ hostIp + ":</b> "+ so.getHostAddress() + "<br>"); // NOI18N
            
            String hostname = NbBundle.getMessage(HostOverviewView.class, "LBL_Hostname");  // NOI18N
            data.append("<b>"+ hostname + ":</b> " + so.getHostName() + "<br><br>"); // NOI18N

            String name = so.getName();
            String ver = so.getVersion();
            String patch = so.getPatchLevel();

            patch = "unknown".equals(patch) ? "" : patch;   // NOI18N
            String os = NbBundle.getMessage(HostOverviewView.class, "LBL_OS");  // NOI18N
            String arch = NbBundle.getMessage(HostOverviewView.class, "LBL_Architecture");  // NOI18N
            String proc = NbBundle.getMessage(HostOverviewView.class, "LBL_Processors");    // NOI18N
            String memory = NbBundle.getMessage(HostOverviewView.class, "LBL_Total_memory_size");    // NOI18N
            String swap = NbBundle.getMessage(HostOverviewView.class, "LBL_Swap_size"); // NOI18N
            String mb = NbBundle.getMessage(HostOverviewView.class, "LBL_MB");  // NOI18N
            data.append("<b>"+os+":</b> " + name + " (" + ver + ")" + " " + patch + "<br>");    // NOI18N
            data.append("<b>"+arch+":</b> " + so.getArch() + "<br>");   // NOI18N
            data.append("<b>"+proc+":</b> " + so.getAvailableProcessors() + "<br><br>");    // NOI18N
            data.append("<b>"+memory+":</b> " + formatBytes(so.getTotalPhysicalMemorySize()) + " "+mb+"<br>");  // NOI18N
            data.append("<b>"+swap+":</b> " + formatBytes(so.getTotalSwapSpaceSize()) + " "+mb+"<br>"); // NOI18N

            return data.toString();
        }
        
        private String formatBytes(long l) {
            return NumberFormat.getInstance().format(((10*l)/1024/1024)/10.0);
        }
        
    }
    
    
    // --- CPU load ------------------------------------------------------------

    private static class CpuLoadViewSupport extends JPanel {

        private boolean cpuMonitoringSupported;
        
        private static final String LOAD_AVERAGE = NbBundle.getMessage(HostOverviewView.class, "LBL_Load_average"); // NOI18N

        private SimpleXYChartSupport chartSupport;

        CpuLoadViewSupport(HostOverview hostOverview, boolean cpuSupported, int chartCache) {
            cpuMonitoringSupported = cpuSupported;
            initModels(chartCache);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(HostOverviewView.class, "LBL_CPU_load"), null, 10, this, null);    // NOI18N
        }

        public void refresh(HostOverview hostOverview, long time) {
            if (cpuMonitoringSupported) {
                long load = (long)(hostOverview.getSystemLoadAverage() * 1000);
                
                chartSupport.addValues(time, new long[] { load });
                chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(load) });
            }
        }

        private void initModels(int chartCache) {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.decimal(1, 0.001d, false, chartCache);

            chartDescriptor.addLineItems(LOAD_AVERAGE);
            chartDescriptor.setDetailsItems(new String[] { LOAD_AVERAGE });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (cpuMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.HOST),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Physical memory -----------------------------------------------------

    private static class PhysicalMemoryViewSupport extends JPanel {

        private static String USED_MEMORY = NbBundle.getMessage(HostOverviewView.class, "LBL_Used_memory"); // NOI18N
        private static String USED_MEMORY_LEG = NbBundle.getMessage(HostOverviewView.class, "LBL_Used_memory_leg"); // NOI18N
        private static String TOTAL_MEMORY = NbBundle.getMessage(HostOverviewView.class, "LBL_Total_memory");   // NOI18N

        private SimpleXYChartSupport chartSupport;

        PhysicalMemoryViewSupport(int chartCache) {
            initModels(chartCache);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(HostOverviewView.class, "LBL_Physical_memory"), null, 10, this, null); // NOI18N
        }

        public void refresh(HostOverview hostOverview, long time) {
            long memoryMax = hostOverview.getTotalPhysicalMemorySize();
            long memoryUsed = memoryMax - hostOverview.getFreePhysicalMemorySize();

            chartSupport.addValues(time, new long[] { memoryUsed });
            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(memoryUsed),
                                                      chartSupport.formatBytes(memoryMax) });
        }

        private void initModels(int chartCache) {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.bytes(128 * 1024 * 1024, false, chartCache);

            chartDescriptor.addLineFillItems(USED_MEMORY_LEG);
            chartDescriptor.setDetailsItems(new String[] { USED_MEMORY, TOTAL_MEMORY });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            add(chartSupport.getChart(), BorderLayout.CENTER);
        }

    }


    // --- Swap memory ---------------------------------------------------------

    private static class SwapMemoryViewSupport extends JPanel {

        private static final String USED_SWAP = NbBundle.getMessage(HostOverviewView.class, "LBL_Used_swap");   // NOI18N
        private static final String USED_SWAP_LEG = NbBundle.getMessage(HostOverviewView.class, "LBL_Used_swap_leg");   // NOI18N
        private static final String TOTAL_SWAP = NbBundle.getMessage(HostOverviewView.class, "LBL_Total_swap"); // NOI18N

        private SimpleXYChartSupport chartSupport;

        SwapMemoryViewSupport(int chartCache) {
            initModels(chartCache);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(HostOverviewView.class, "LBL_Swap_memory"), null, 20, this, null); // NOI18N
        }

        public void refresh(HostOverview hostOverview, long time) {
            long memorySwapMax = hostOverview.getTotalSwapSpaceSize();
            long memorySwapUsed = memorySwapMax - hostOverview.getFreeSwapSpaceSize();

            chartSupport.addValues(time, new long[] { memorySwapUsed });
            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(memorySwapUsed),
                                                      chartSupport.formatBytes(memorySwapMax) });
        }

        private void initModels(int chartCache) {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.bytes(128 * 1024 * 1024, false, chartCache);

            chartDescriptor.addLineFillItems(USED_SWAP_LEG);
            chartDescriptor.setDetailsItems(new String[] { USED_SWAP, TOTAL_SWAP });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            add(chartSupport.getChart(), BorderLayout.CENTER);
        }

    }

}

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

package org.graalvm.visualvm.modules.buffermonitor;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.swing.Timer;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * @author Tomas Hurka
 */
class BufferMonitorView extends DataSourceView implements DataRemovedListener<Application> {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/modules/buffermonitor/resources/monitor.png"; // NOI18N
    private static final Logger LOGGER = Logger.getLogger(BufferMonitorView.class.getName());
    
    private Timer timer;
    private Application application;
    private boolean refreshRunning;
    
    public BufferMonitorView(Application application) {
        super(application, NbBundle.getMessage(BufferMonitorView.class, "Buffer_Pools"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false); // NOI18N
        this.application = application;
    }
    
    @Override
    protected void removed() {
        timer.stop();
    }
    
    protected DataViewComponent createComponent() {
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(application).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        String title = NbBundle.getMessage(BufferMonitorView.class, "LBL_DIRECT");  // NOI18N
        final BufferMonitorViewSupport directBufferViewSupport = new BufferMonitorViewSupport(jmx, title, BufferMonitorViewProvider.DIRECT_BUFFER_NAME);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(title, true), DataViewComponent.TOP_LEFT); 
        dvc.addDetailsView(directBufferViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        title = NbBundle.getMessage(BufferMonitorView.class, "LBL_MAPPED"); // NOI18N
        final BufferMonitorViewSupport mappedBufferViewSupport = new BufferMonitorViewSupport(jmx, title, BufferMonitorViewProvider.MAPPED_BUFFER_NAME);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(title, true), DataViewComponent.TOP_RIGHT); 
        dvc.addDetailsView(mappedBufferViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        timer = new Timer(GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (refreshRunning) {
                    return;
                }
                refreshRunning = true;
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        try {
                            if (application.getState() == Application.STATE_AVAILABLE) {
                                final long time = System.currentTimeMillis();
                                directBufferViewSupport.refresh(time);
                                mappedBufferViewSupport.refresh(time);
                            }
                        } catch (Exception ex) {
                            LOGGER.throwing(BufferMonitorView.class.getName(), "refresh", ex); // NOI18N
                        } finally {
                            refreshRunning = false;
                        }
                    }
                });

            }
        });
        timer.setInitialDelay(800);
        timer.start();
        getDataSource().notifyWhenRemoved(this);
        
        return dvc;
    }
    
    public void dataRemoved(Application app) {
        timer.stop();
    }
    
    private static class MasterViewSupport extends JPanel  {
        
        public MasterViewSupport(Application app) {
            initComponents(app);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(BufferMonitorView.class, "Buffer_Pools"), null, this);   // NOI18N
        }
        
        
        private void initComponents(Application app) {
            setLayout(new BorderLayout());
            setOpaque(false);
        }
        
        String getGeneralInfo(Application app) {
            return "";
        }
        
    }
    private static class BufferMonitorViewSupport extends JPanel  {
        
        private static final String MEMORY_USED = NbBundle.getMessage(BufferMonitorView.class, "LBL_Memory_Used"); // NOI18N
        private static final String TOTAL_CAPACITY = NbBundle.getMessage(BufferMonitorView.class, "LBL_Total_Capacity"); // NOI18N
        private static final String COUNT = NbBundle.getMessage(BufferMonitorView.class, "LBL_Count"); // NOI18N
        private SimpleXYChartSupport chartSupport;
        private final String TITLE;
        private ObjectName bufferObjectName;
        private final MBeanServerConnection conn;
        private final String[] attributes = {"Count","MemoryUsed","TotalCapacity"}; // NOI18N
        
        public BufferMonitorViewSupport(JmxModel jmx, String title, String bufferName) {
            GlobalPreferences preferences = GlobalPreferences.sharedInstance();
            int chartCache = preferences.getMonitoredDataCache() * 60 /
                         preferences.getMonitoredDataPoll();
            conn = jmx.getMBeanServerConnection();
            try {
                bufferObjectName = new ObjectName(bufferName);
            } catch (MalformedObjectNameException ex) {
                ex.printStackTrace();
            }
            TITLE = title;
            initModels(chartCache);
            initComponents();
        }
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(TITLE, null, 10, this, null);
        }
        
        public void refresh(long time) {
            long count = 0;
            long memoryUsed = 0;
            long totalCapacity = 0;
            List attrs;
            
            try {
                attrs = conn.getAttributes(bufferObjectName, attributes);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            Iterator attrIt = attrs.iterator();
            
            while(attrIt.hasNext()) {
                Attribute attrib = (Attribute) attrIt.next();
                String name = attrib.getName();
                if (attributes[0].equals(name)) {
                    count = ((Long)attrib.getValue()).longValue();
                } else if (attributes[1].equals(name)) {
                    memoryUsed = ((Long)attrib.getValue()).longValue();
                } else if (attributes[2].equals(name)) {
                    totalCapacity = ((Long)attrib.getValue()).longValue();
               }
            }
            chartSupport.addValues(time, new long[] { memoryUsed, totalCapacity });
            chartSupport.updateDetails(new String[] { chartSupport.formatBytes(memoryUsed),
                                                      chartSupport.formatBytes(totalCapacity),
                                                      chartSupport.formatDecimal(count)});
        }
        
        private void initModels(int chartCache) {
            SimpleXYChartDescriptor chartDescriptor =
                    SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, chartCache);

            chartDescriptor.addLineFillItems(MEMORY_USED, TOTAL_CAPACITY);
            chartDescriptor.setDetailsItems(new String[] { MEMORY_USED, TOTAL_CAPACITY, COUNT });

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            add(chartSupport.getChart(), BorderLayout.CENTER);
        }
        
    }
    
}

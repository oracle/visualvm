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

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.Timer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * @author Tomas Hurka
 */
class BufferMonitorView extends DataSourceView implements DataRemovedListener<Application> {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/buffermonitor/resources/monitor.png"; // NOI18N
    
    private Timer timer;
    private Application application;
    
    public BufferMonitorView(Application application) {
        super(application, NbBundle.getMessage(BufferMonitorView.class, "Buffer_Monitor"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false); // NOI18N
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
        
        timer = new Timer(2000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final long time = System.currentTimeMillis();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        directBufferViewSupport.refresh(time);
                        mappedBufferViewSupport.refresh(time);
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
            return new DataViewComponent.MasterView(NbBundle.getMessage(BufferMonitorView.class, "LBL_Overview"), null, this);   // NOI18N
        }
        
        
        private void initComponents(Application app) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralInfo(app) + "</nobr>");  // NOI18N
            area.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
            
            add(area, BorderLayout.CENTER);
        }
        
        String getGeneralInfo(Application app) {
            return "";
        }
        
    }
    private static class BufferMonitorViewSupport extends JPanel  {
        
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        private static final String MEMORY_USED = NbBundle.getMessage(BufferMonitorView.class, "LBL_Memory_Used"); // NOI18N
        private static final String TOTAL_CAPACITY = NbBundle.getMessage(BufferMonitorView.class, "LBL_Total_Capacity"); // NOI18N
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private Chart bufferMetricsChart;
        private HTMLLabel memoryUsedLabel;
        private HTMLLabel totalCapacityLabel;
        private final String TITLE;
        private ObjectName bufferObjectName;
        private final MBeanServerConnection conn;
        private final String[] attributes = {"Count","MemoryUsed","TotalCapacity"}; // NOI18N
        
        public BufferMonitorViewSupport(JmxModel jmx, String title, String bufferName) {
            conn = jmx.getMBeanServerConnection();
            try {
                bufferObjectName = new ObjectName(bufferName);
            } catch (MalformedObjectNameException ex) {
                ex.printStackTrace();
            }
            TITLE = title;
            initComponents();
        }
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(TITLE, null, 10, this, null);
        }
        
        public void refresh(long time) {
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
                if ("Count".equals(name)) {
                } else if ("MemoryUsed".equals(name)) {
                    memoryUsed = ((Long)attrib.getValue()).longValue();
                } else if ("TotalCapacity".equals(name)) {
                    totalCapacity = ((Long)attrib.getValue()).longValue();
               }
            }
            
            String meoryUsedString =  formatter.format(memoryUsed); 
            String totalCapacityString =   formatter.format(totalCapacity);
            memoryUsedLabel.setText("<nobr><b>"+MEMORY_USED+":</b> " + meoryUsedString + "</nobr>");    // NOI18N
            
            totalCapacityLabel.setText("<nobr><b>"+TOTAL_CAPACITY+":</b> " + totalCapacityString + "</nobr>");    // NOI18N
            
            bufferMetricsChart.getModel().addItemValues(time, new long[] { memoryUsed, totalCapacity });
            
            
            bufferMetricsChart.setToolTipText(
                    "<html><nobr><b>"+MEMORY_USED+":</b> " + meoryUsedString + " </nobr>" + "<br>" +   // NOI18N
                    "<html><nobr><b>"+TOTAL_CAPACITY+":</b> " + totalCapacityString + " </nobr></html>"); // NOI18N
            
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            JComponent contents;
            
            // cpuMetricsPanel
            memoryUsedLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
            };
            memoryUsedLabel.setText("<nobr><b>"+MEMORY_USED+":</b> " + -1 + "</nobr>");  // NOI18N
            memoryUsedLabel.setOpaque(false);
            totalCapacityLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
            };
            totalCapacityLabel.setText("<nobr><b>"+TOTAL_CAPACITY+":</b> " + -1 + "</nobr>");  // NOI18N
            totalCapacityLabel.setOpaque(false);
            final JPanel heapMetricsDataPanel = new JPanel(new GridLayout(2, 2));
            heapMetricsDataPanel.setOpaque(false);
            heapMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            heapMetricsDataPanel.add(memoryUsedLabel);
            heapMetricsDataPanel.add(totalCapacityLabel);
            
            bufferMetricsChart = new BufferMetricsChart();
            bufferMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), bufferMetricsChart.getBorder()));
            JPanel heapMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            heapMetricsLegendContainer.setOpaque(false);
            heapMetricsLegendContainer.add(bufferMetricsChart.getBigLegendPanel());
            final JPanel heapMetricsPanel = new JPanel(new BorderLayout());
            heapMetricsPanel.setOpaque(true);
            heapMetricsPanel.setBackground(Color.WHITE);
            heapMetricsPanel.add(heapMetricsDataPanel, BorderLayout.NORTH);
            heapMetricsPanel.add(bufferMetricsChart, BorderLayout.CENTER);
            heapMetricsPanel.add(heapMetricsLegendContainer, BorderLayout.SOUTH);
            
            final boolean[] heapMetricsPanelResizing = new boolean[] { false };
            heapMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            heapMetricsPanel.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    if (heapMetricsPanelResizing[0] == true) {
                        heapMetricsPanelResizing[0] = false;
                        return;
                    }
                    
                    boolean shouldBeVisible = heapMetricsPanel.getSize().height > 275;
                    if (shouldBeVisible == heapMetricsDataPanel.isVisible()) return;
                    
                    heapMetricsPanelResizing[0] = true;
                    heapMetricsDataPanel.setVisible(shouldBeVisible);
                }
            });
            contents = heapMetricsPanel;
            
            add(contents, BorderLayout.CENTER);
        }
        
    }
    
}

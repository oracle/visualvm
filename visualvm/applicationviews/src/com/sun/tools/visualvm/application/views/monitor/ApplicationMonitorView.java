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

package com.sun.tools.visualvm.application.views.monitor;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.application.jvm.MonitoredDataListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.management.MemoryMXBean;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationMonitorView extends DataSourceView {
    private static final Logger LOGGER = Logger.getLogger(ApplicationMonitorView.class.getName());
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/application/views/resources/monitor.png";  // NOI18N

    private Jvm jvm;
    private MemoryMXBean memoryMXBean;
    private MonitoredDataListener monitoredDataListener;
    

    public ApplicationMonitorView(Application application) {
        super(application, NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Monitor"), new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 20, false);   // NOI18N
    }
    
    @Override
    protected void willBeAdded() {
        Application application = (Application) getDataSource();
        jvm = JvmFactory.getJVMFor(application);
        memoryMXBean = null;
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
            JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
            if (mxbeans != null) memoryMXBean = mxbeans.getMemoryMXBean();
        }
    }
        
    @Override
    protected void removed() {
        if (jvm != null) jvm.removeMonitoredDataListener(monitoredDataListener);
    }
    
    protected DataViewComponent createComponent() {
        Application application = (Application)getDataSource();
        final MasterViewSupport masterViewSupport = new MasterViewSupport(application, jvm, memoryMXBean);
        DataViewComponent dvc = new DataViewComponent(
                masterViewSupport.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        final CpuViewSupport cpuViewSupport = new CpuViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu"), true), DataViewComponent.TOP_LEFT);  // NOI18N
        dvc.addDetailsView(cpuViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        final HeapViewSupport heapViewSupport = new HeapViewSupport(jvm);
        final PermGenViewSupport permGenViewSupport = new PermGenViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Memory"), true), DataViewComponent.TOP_RIGHT);  // NOI18N
        dvc.addDetailsView(heapViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(permGenViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        final ClassesViewSupport classesViewSupport = new ClassesViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Classes"), true), DataViewComponent.BOTTOM_LEFT);    // NOI18N
        dvc.addDetailsView(classesViewSupport.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
        
        final ThreadsViewSupport threadsViewSupport = new ThreadsViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Threads"), true), DataViewComponent.BOTTOM_RIGHT);   // NOI18N
        dvc.addDetailsView(threadsViewSupport.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
        
        monitoredDataListener = new MonitoredDataListener() {
            public void monitoredDataEvent(final MonitoredData data) {
                final long time = System.currentTimeMillis();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            masterViewSupport.refresh(data);
                            cpuViewSupport.refresh(data, time);
                            heapViewSupport.refresh(data, time);
                            permGenViewSupport.refresh(data, time);
                            classesViewSupport.refresh(data, time);
                            threadsViewSupport.refresh(data, time);
                        } catch (Exception ex) {
                            LOGGER.log(Level.INFO,"monitoredDataEvent",ex); // NOI18N
                        }
                    }
                });
            }
        };
        jvm.addMonitoredDataListener(monitoredDataListener);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel implements DataRemovedListener<Application>, PropertyChangeListener {
        
        private Application application;
        private Jvm jvm;
        private MemoryMXBean memoryMXBean;
        private HTMLTextArea area;
        private JButton gcButton;
        private JButton heapDumpButton;
        
        public MasterViewSupport(Application application, Jvm jvm, MemoryMXBean memoryMXBean) {
            this.application = application;
            this.jvm = jvm;
            this.memoryMXBean = memoryMXBean;
            initComponents();
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Monitor"), null, this);  // NOI18N
        }
        
        public void refresh(MonitoredData data) {
            int selStart = area.getSelectionStart();
            int selEnd   = area.getSelectionEnd();
            area.setText(getBasicTelemetry(data));
            area.select(selStart, selEnd);
        }
        
        public void dataRemoved(Application dataSource) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    gcButton.setEnabled(false);
                    heapDumpButton.setEnabled(false);
                }
            });
        }

        public void propertyChange(PropertyChangeEvent evt) {
            dataRemoved(application);
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea(getBasicTelemetry(null));
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            
            // TODO: implement listener for Application.oomeHeapDumpEnabled
            
            add(area, BorderLayout.CENTER);

            gcButton = new JButton(new AbstractAction(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Perform_GC")) {    // NOI18N
                public void actionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            try { memoryMXBean.gc(); } catch (Exception e) { 
                                LOGGER.throwing(ApplicationMonitorView.class.getName(), "initComponents", e);   // NOI18N
                            }
                        };
                    });
                }
            });
            gcButton.setEnabled(memoryMXBean != null);
            
            heapDumpButton = new JButton(new AbstractAction(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_Dump")) {   // NOI18N
                public void actionPerformed(ActionEvent e) {
                    HeapDumpSupport.getInstance().takeHeapDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
                }
            });
            heapDumpButton.setEnabled(jvm.isTakeHeapDumpSupported());
            
            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setOpaque(false);
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setBackground(area.getBackground());
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            buttonsContainer.add(gcButton, BorderLayout.WEST);
            buttonsContainer.add(heapDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);
            
            add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);
            
            application.notifyWhenRemoved(this);            
            application.addPropertyChangeListener(Stateful.PROPERTY_STATE, WeakListeners.propertyChange(this,application));
        }

        private String getBasicTelemetry(MonitoredData data) {
            String uptimeLbl = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Uptime"); // NOI18N
            if (data == null) return "<nobr><b>"+uptimeLbl+":</b></nobr>";  // NOI18N
            else return "<nobr><b>"+uptimeLbl+":</b> " + getTime(data.getUpTime()) + "</nobr>"; // NOI18N
        }
        
        public static String getTime(long millis) {
            // Hours
            long hours = millis / 3600000;
            String sHours = (hours == 0 ? "" : "" + hours + " hrs "); // NOI18N
            millis = millis % 3600000;
            
            // Minutes
            long minutes = millis / 60000;
            String sMinutes = (((hours > 0) && (minutes < 10)) ? "0" + minutes : "" + minutes) + " min "; // NOI18N
            millis = millis % 60000;
            
            // Seconds
            long seconds = millis / 1000;
            String sSeconds = ((seconds < 10) ? "0" + seconds : "" + seconds) + " sec"; // NOI18N

            return sHours + sMinutes + sSeconds;
        }
        
    }

    
    // --- CPU -----------------------------------------------------------------
    
    private static class CpuViewSupport extends JPanel  {
        
        private boolean cpuMonitoringSupported;
        private ChartsSupport.Chart cpuUsageChart;
        private HTMLLabel cpuLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        private static final String CPU = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu"); // NOI18N
        private static final String CPU_USAGE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu_Usage"); // NOI18N
        
        private long lastProcessCpuTime = -1;
        private long lastUpTime = -1;

        public CpuViewSupport(Jvm jvm) {
            cpuMonitoringSupported = jvm.isCpuMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(CPU, null, 10, this, null);
        }
        
        public void refresh(MonitoredData data, long time) {
            if (cpuMonitoringSupported) {
                long upTime = data.getUpTime() * 1000000;
                long processCpuTime = data.getProcessCpuTime();
                
                if (lastProcessCpuTime != -1 && lastUpTime != -1) {
                    
                    long upTimeDiff = upTime - lastUpTime;
                    long processTimeDiff = processCpuTime - lastProcessCpuTime;
                    long cpuUsage = upTimeDiff > 0 ? Math.min((long)(100 * (float)processTimeDiff / (float)upTimeDiff), 100) : 0;
                
                    cpuUsageChart.getModel().addItemValues(time, new long[] { cpuUsage });
                    cpuLabel.setText("<nobr><b>"+CPU_USAGE+":</b> " + formatter.format(cpuUsage) + "% </nobr>");    // NOI18N

                    cpuUsageChart.setToolTipText(
                            "<html><nobr><b>"+CPU_USAGE+":</b> " + formatter.format(cpuUsage) + "% </nobr></html>"); // NOI18N

                }
                
                lastUpTime = upTime;
                lastProcessCpuTime = processCpuTime;
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (cpuMonitoringSupported) {
              // cpuMetricsPanel
              cpuLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              cpuLabel.setText("<nobr><b>"+CPU+":</b></nobr>");  // NOI18N
              cpuLabel.setOpaque(false);
              final JPanel heapMetricsDataPanel = new JPanel(new GridLayout(2, 2));
              heapMetricsDataPanel.setOpaque(false);
              heapMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
              heapMetricsDataPanel.add(cpuLabel);

              cpuUsageChart = new ChartsSupport.CpuMetricsChart();
              cpuUsageChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), cpuUsageChart.getBorder()));
              JPanel heapMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              heapMetricsLegendContainer.setOpaque(false);
              heapMetricsLegendContainer.add(cpuUsageChart.getBigLegendPanel());
              final JPanel heapMetricsPanel = new JPanel(new BorderLayout());
              heapMetricsPanel.setOpaque(true);
              heapMetricsPanel.setBackground(Color.WHITE);
              heapMetricsPanel.add(heapMetricsDataPanel, BorderLayout.NORTH);
              heapMetricsPanel.add(cpuUsageChart, BorderLayout.CENTER);
              heapMetricsPanel.add(heapMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] heapMetricsPanelResizing = new boolean[] { false };
              heapMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              heapMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (heapMetricsPanelResizing[0] == true) {
                          heapMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = heapMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == heapMetricsDataPanel.isVisible()) return;

                      heapMetricsPanelResizing[0] = true;
                      heapMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
              contents = heapMetricsPanel;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(contents, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- Heap ----------------------------------------------------------------
    
    private static class HeapViewSupport extends JPanel  {
        
        private boolean memoryMonitoringSupported;
        private ChartsSupport.Chart heapMetricsChart;
        private HTMLLabel heapSizeLabel;
        private HTMLLabel usedHeapLabel;
        private HTMLLabel maxHeapLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        private static final String HEAP_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_size"); // NOI18N
        private static final String USED_HEAP = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_heap"); // NOI18N
        private static final String MAX_HEAP = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Max_Heap");   // NOI18N

        public HeapViewSupport(Jvm jvm) {
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap"), null, 10, this, null);  // NOI18N
        }
        
        public void refresh(MonitoredData data, long time) {
            if (memoryMonitoringSupported) {
                long heapCapacity = data.getGenCapacity()[0];
                long heapUsed = data.getGenUsed()[0];
                long maxHeap = data.getGenMaxCapacity()[0];
                heapMetricsChart.getModel().addItemValues(time, new long[] { heapCapacity, heapUsed });
                heapSizeLabel.setText("<nobr><b>"+HEAP_SIZE+":</b> " + formatter.format(heapCapacity) + " </nobr>");    // NOI18N
                usedHeapLabel.setText("<nobr><b>"+USED_HEAP+":</b> " + formatter.format(heapUsed) + " </nobr>");    // NOI18N
                maxHeapLabel.setText("<nobr><b>"+MAX_HEAP+":</b> " + formatter.format(maxHeap) + " </nobr>");   // NOI18N
              
                heapMetricsChart.setToolTipText(
                        "<html><nobr><b>"+HEAP_SIZE+":</b> " + formatter.format(heapCapacity) + " </nobr>" + "<br>" +   // NOI18N
                        "<nobr><b>"+USED_HEAP+":</b> " + formatter.format(heapUsed) + " </nobr>" + "<br>" + // NOI18N
                        "<nobr><b>"+MAX_HEAP+":</b> " + formatter.format(maxHeap) + " </nobr></html>"); // NOI18N
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (memoryMonitoringSupported) {
              // heapMetricsPanel
              heapSizeLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              heapSizeLabel.setText("<nobr><b>"+HEAP_SIZE+":</b></nobr>");  // NOI18N
              heapSizeLabel.setOpaque(false);
              usedHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              usedHeapLabel.setText("<nobr><b>"+USED_HEAP+":</b></nobr>");  // NOI18N
              usedHeapLabel.setOpaque(false);
              maxHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              maxHeapLabel.setText("<nobr><b>"+MAX_HEAP+":</b></nobr>");    // NOI18N
              maxHeapLabel.setOpaque(false);
              final JPanel heapMetricsDataPanel = new JPanel(new GridLayout(2, 2));
              heapMetricsDataPanel.setOpaque(false);
              heapMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
              heapMetricsDataPanel.add(heapSizeLabel);
              heapMetricsDataPanel.add(maxHeapLabel);
              heapMetricsDataPanel.add(usedHeapLabel);

              heapMetricsChart = new ChartsSupport.HeapMetricsChart();
              heapMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), heapMetricsChart.getBorder()));
              JPanel heapMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              heapMetricsLegendContainer.setOpaque(false);
              heapMetricsLegendContainer.add(heapMetricsChart.getBigLegendPanel());
              final JPanel heapMetricsPanel = new JPanel(new BorderLayout());
              heapMetricsPanel.setOpaque(true);
              heapMetricsPanel.setBackground(Color.WHITE);
              heapMetricsPanel.add(heapMetricsDataPanel, BorderLayout.NORTH);
              heapMetricsPanel.add(heapMetricsChart, BorderLayout.CENTER);
              heapMetricsPanel.add(heapMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] heapMetricsPanelResizing = new boolean[] { false };
              heapMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              heapMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (heapMetricsPanelResizing[0] == true) {
                          heapMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = heapMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == heapMetricsDataPanel.isVisible()) return;

                      heapMetricsPanelResizing[0] = true;
                      heapMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
              contents = heapMetricsPanel;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(contents, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- PermGen -------------------------------------------------------------
    
    private static class PermGenViewSupport extends JPanel  {
        
        private boolean memoryMonitoringSupported;
        private ChartsSupport.Chart permgenMetricsChart;
        private HTMLLabel permHeapSizeLabel;
        private HTMLLabel permUsedHeapLabel;
        private HTMLLabel permMaxHeapLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        private static final String PERM_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_PermGen_size");  // NOI18N
        private static final String USED_PERM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_PermGen");  // NOI18N
        private static final String MAX_PERM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Max_PermGen_size");   // NOI18N
        
        public PermGenViewSupport(Jvm jvm) {
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_PermGen"), null, 10, this, null);   // NOI18N
        }
        
        public void refresh(MonitoredData data, long time) {
            if (memoryMonitoringSupported) {
                long permgenCapacity = data.getGenCapacity()[1];
                long permgenUsed = data.getGenUsed()[1];
                long permgenMax = data.getGenMaxCapacity()[1];
                permgenMetricsChart.getModel().addItemValues(time, new long[] { permgenCapacity, permgenUsed });
                permHeapSizeLabel.setText("<nobr><b>"+PERM_SIZE+":</b> " + formatter.format(permgenCapacity) + " </nobr>"); // NOI18N
                permUsedHeapLabel.setText("<nobr><b>"+USED_PERM+":</b> " + formatter.format(permgenUsed) + " </nobr>"); // NOI18N
                permMaxHeapLabel.setText("<nobr><b>"+MAX_PERM+":</b> " + formatter.format(permgenMax) + " </nobr>");    // NOI18N
              
                permgenMetricsChart.setToolTipText(
                        "<html><nobr><b>"+PERM_SIZE+":</b> " + formatter.format(permgenCapacity) + " </nobr>" + "<br>" +    // NOI18N
                        "<nobr><b>"+USED_PERM+":</b> " + formatter.format(permgenUsed) + " </nobr>" + "<br>" +  // NOI18N
                        "<nobr><b>"+MAX_PERM+":</b> " + formatter.format(permgenMax) + " </nobr>"); // NOI18N
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (memoryMonitoringSupported) {
              // permgenMetricsPanel
              permHeapSizeLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              permHeapSizeLabel.setText("<nobr><b>"+PERM_SIZE+":</b></nobr>");  // NOI18N
              permHeapSizeLabel.setOpaque(false);
              permUsedHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              permUsedHeapLabel.setText("<nobr><b>"+USED_PERM+":</b></nobr>");  // NOI18N
              permUsedHeapLabel.setOpaque(false);
              permMaxHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              permMaxHeapLabel.setText("<nobr><b>"+MAX_PERM+":</b></nobr>");    // NOI18N
              permMaxHeapLabel.setOpaque(false);
              final JPanel permgenMetricsDataPanel = new JPanel(new GridLayout(2, 2));
              permgenMetricsDataPanel.setOpaque(false);
              permgenMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
              permgenMetricsDataPanel.add(permHeapSizeLabel);
              permgenMetricsDataPanel.add(permMaxHeapLabel);
              permgenMetricsDataPanel.add(permUsedHeapLabel);

              permgenMetricsChart = new ChartsSupport.PermGenMetricsChart();
              permgenMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), permgenMetricsChart.getBorder()));
              JPanel permgenMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              permgenMetricsLegendContainer.setOpaque(false);
              permgenMetricsLegendContainer.add(permgenMetricsChart.getBigLegendPanel());
              final JPanel permgenMetricsPanel = new JPanel(new BorderLayout());
              permgenMetricsPanel.setOpaque(true);
              permgenMetricsPanel.setBackground(Color.WHITE);
              permgenMetricsPanel.add(permgenMetricsDataPanel, BorderLayout.NORTH);
              permgenMetricsPanel.add(permgenMetricsChart, BorderLayout.CENTER);
              permgenMetricsPanel.add(permgenMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] permgenMetricsPanelResizing = new boolean[] { false };
              permgenMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              permgenMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (permgenMetricsPanelResizing[0] == true) {
                          permgenMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = permgenMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == permgenMetricsDataPanel.isVisible()) return;

                      permgenMetricsPanelResizing[0] = true;
                      permgenMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
              contents = permgenMetricsPanel;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(contents, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- Classes -------------------------------------------------------------
    
    private static class ClassesViewSupport extends JPanel  {
        
        private boolean classMonitoringSupported;
        private ChartsSupport.Chart classesMetricsChart;
        private HTMLLabel loadedClassesLabel;
        private HTMLLabel loadedSharedClassesLabel;
        private HTMLLabel unloadedClassesLabel;
        private HTMLLabel unloadedSharedClassesLabel;
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        private static final String TOTAL_LOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_loaded_classes");   // NOI18N
        private static final String SHARED_LOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_loaded_classes"); // NOI18N
        private static final String TOTAL_UNLOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_unloaded_classes");   // NOI18N
        private static final String SHARED_UNLOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_unloaded_classes"); // NOI18N
        
        public ClassesViewSupport(Jvm jvm) {
            classMonitoringSupported = jvm.isClassMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Classes"), null, 10, this, null);   // NOI18N
        }
        
        public void refresh(MonitoredData data, long time) {
            if (classMonitoringSupported) {
                long sharedUnloaded = data.getSharedUnloadedClasses();
                long totalUnloaded  = data.getUnloadedClasses();
                long sharedClasses  = data.getSharedLoadedClasses() - sharedUnloaded;
                long totalClasses   = data.getLoadedClasses() - data.getUnloadedClasses() + sharedClasses;
                classesMetricsChart.getModel().addItemValues(time, new long[] { totalClasses, sharedClasses });
                loadedClassesLabel.setText("<nobr><b>"+TOTAL_LOADED+":</b> " + totalClasses + " </nobr>");  // NOI18N
                loadedSharedClassesLabel.setText("<nobr><b>"+SHARED_LOADED+":</b> " + sharedClasses + " </nobr>");  // NOI18N
                unloadedClassesLabel.setText("<nobr><b>"+TOTAL_UNLOADED+":</b> " + totalUnloaded + " </nobr>"); // NOI18N
                unloadedSharedClassesLabel.setText("<nobr><b>"+SHARED_UNLOADED+":</b> " + sharedUnloaded + " </nobr>"); // NOI18N
              
                classesMetricsChart.setToolTipText(
                        "<html><nobr><b>"+TOTAL_LOADED+":</b> " + totalClasses + " </nobr>" + "<br>" +  // NOI18N
                        "<nobr><b>"+SHARED_LOADED+":</b> " + sharedClasses + " </nobr>" + "<br>" +  // NOI18N
                        "<nobr><b>"+TOTAL_UNLOADED+":</b> " + totalUnloaded + " </nobr>" + "<br>" + // NOI18N
                        "<nobr><b>"+SHARED_UNLOADED+":</b> " + sharedUnloaded + " </nobr></html>"); // NOI18N
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (classMonitoringSupported) {
              // classesMetricsPanel
              loadedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              loadedClassesLabel.setText("<nobr><b>"+TOTAL_LOADED+":</b></nobr>");  // NOI18N
              loadedClassesLabel.setOpaque(false);
              loadedSharedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              loadedSharedClassesLabel.setText("<nobr><b>"+SHARED_LOADED+":</b></nobr>");   // NOI18N
              loadedSharedClassesLabel.setOpaque(false);
              unloadedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              unloadedClassesLabel.setText("<nobr><b>"+TOTAL_UNLOADED+":</b></nobr>");  // NOI18N
              unloadedClassesLabel.setOpaque(false);
              unloadedSharedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              unloadedSharedClassesLabel.setText("<nobr><b>"+SHARED_UNLOADED+":</b></nobr>");   // NOI18N
              unloadedSharedClassesLabel.setOpaque(false);
              final JPanel classesMetricsDataPanel = new JPanel(new GridLayout(2, 2));
              classesMetricsDataPanel.setOpaque(false);
              classesMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
              classesMetricsDataPanel.add(loadedClassesLabel);
              classesMetricsDataPanel.add(unloadedClassesLabel);
              classesMetricsDataPanel.add(loadedSharedClassesLabel);
              classesMetricsDataPanel.add(unloadedSharedClassesLabel);

              classesMetricsChart = new ChartsSupport.ClassesMetricsChart();
              classesMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), classesMetricsChart.getBorder()));
              JPanel classesMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              classesMetricsLegendContainer.setOpaque(false);
              classesMetricsLegendContainer.add(classesMetricsChart.getBigLegendPanel());
              final JPanel classesMetricsPanel = new JPanel(new BorderLayout());
              classesMetricsPanel.setOpaque(true);
              classesMetricsPanel.setBackground(Color.WHITE);
              classesMetricsPanel.add(classesMetricsDataPanel, BorderLayout.NORTH);
              classesMetricsPanel.add(classesMetricsChart, BorderLayout.CENTER);
              classesMetricsPanel.add(classesMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] classesMetricsPanelResizing = new boolean[] { false };
              classesMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              classesMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (classesMetricsPanelResizing[0] == true) {
                          classesMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = classesMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == classesMetricsDataPanel.isVisible()) return;

                      classesMetricsPanelResizing[0] = true;
                      classesMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
              contents = classesMetricsPanel;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(contents, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- Threads -------------------------------------------------------------
    
    private static class ThreadsViewSupport extends JPanel  {
        
        private boolean threadsMonitoringSupported;
        private ChartsSupport.Chart threadsMetricsChart;
        private HTMLLabel liveThreadsLabel;
        private HTMLLabel daemonThreadsLabel;
        private HTMLLabel liveThreadsPeakLabel;
        private HTMLLabel startedThreadsLabel;
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        private static final String LIVE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads");   // NOI18N
        private static final String DAEMON = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Daemon_threads");// NOI18N
        private static final String PEAK = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads_peak");  // NOI18N
        private static final String STARTED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Started_threads_total");   // NOI18N
        
        public ThreadsViewSupport(Jvm jvm) {
            threadsMonitoringSupported = jvm.isThreadMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Threads"), null, 10, this, null);   // NOI18N
        }
        
        public void refresh(MonitoredData data, long time) {
            if (threadsMonitoringSupported) {
                long totalThreads   = data.getThreadsLive();
                long daemonThreads  = data.getThreadsDaemon();
                long peakThreads    = data.getThreadsLivePeak();
                long startedThreads = data.getThreadsStarted();
                threadsMetricsChart.getModel().addItemValues(time, new long[] { totalThreads, daemonThreads });
                liveThreadsLabel.setText("<nobr><b>"+LIVE+":</b> " + totalThreads + " </nobr>");    // NOI18N
                daemonThreadsLabel.setText("<nobr><b>"+DAEMON+":</b> " + daemonThreads + " </nobr>");   // NOI18N
                liveThreadsPeakLabel.setText("<nobr><b>"+PEAK+":</b> " + peakThreads + " </nobr>"); // NOI18N
                startedThreadsLabel.setText("<nobr><b>"+STARTED+":</b> " + startedThreads + " </nobr>");    // NOI18N
              
                threadsMetricsChart.setToolTipText(
                        "<html><nobr><b>"+LIVE+":</b> " + totalThreads + " </nobr>" + "<br>" +  // NOI18N
                        "<nobr><b>"+DAEMON+":</b> " + daemonThreads + " </nobr>" + "<br>" + // NOI18N
                        "<nobr><b>"+PEAK+":</b> " + peakThreads + " </nobr>" + "<br>" + // NOI18N
                        "<nobr><b>"+STARTED+":</b> " + startedThreads + " </nobr></html>"); // NOI18N
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (threadsMonitoringSupported) {
              // threadsMetricsPanel
              liveThreadsLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              liveThreadsLabel.setText("<nobr><b>"+LIVE+":</b></nobr>");    // NOI18N
              liveThreadsLabel.setOpaque(false);
              daemonThreadsLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              daemonThreadsLabel.setText("<nobr><b>"+DAEMON+":</b></nobr>");    // NOI18N
              daemonThreadsLabel.setOpaque(false);
              liveThreadsPeakLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              liveThreadsPeakLabel.setText("<nobr><b>"+PEAK+":</b></nobr>");    // NOI18N
              liveThreadsPeakLabel.setOpaque(false);
              startedThreadsLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              startedThreadsLabel.setText("<nobr><b>"+STARTED+":</b></nobr>");  // NOI18N
              startedThreadsLabel.setOpaque(false);
              final JPanel threadsMetricsDataPanel = new JPanel(new GridLayout(2, 2));
              threadsMetricsDataPanel.setOpaque(false);
              threadsMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
              threadsMetricsDataPanel.add(liveThreadsLabel);
              threadsMetricsDataPanel.add(liveThreadsPeakLabel);
              threadsMetricsDataPanel.add(daemonThreadsLabel);
              threadsMetricsDataPanel.add(startedThreadsLabel);

              threadsMetricsChart = new ChartsSupport.ThreadsMetricsChart();
              threadsMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), threadsMetricsChart.getBorder()));
              JPanel threadsMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              threadsMetricsLegendContainer.setOpaque(false);
              threadsMetricsLegendContainer.add(threadsMetricsChart.getBigLegendPanel());
              final JPanel threadsMetricsPanel = new JPanel(new BorderLayout());
              threadsMetricsPanel.setOpaque(true);
              threadsMetricsPanel.setBackground(Color.WHITE);
              threadsMetricsPanel.add(threadsMetricsDataPanel, BorderLayout.NORTH);
              threadsMetricsPanel.add(threadsMetricsChart, BorderLayout.CENTER);
              threadsMetricsPanel.add(threadsMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] threadsMetricsPanelResizing = new boolean[] { false };
              threadsMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              threadsMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (threadsMetricsPanelResizing[0] == true) {
                          threadsMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = threadsMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == threadsMetricsDataPanel.isVisible()) return;

                      threadsMetricsPanelResizing[0] = true;
                      threadsMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
              contents = threadsMetricsPanel;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(contents, BorderLayout.CENTER);
        }
        
    }

}

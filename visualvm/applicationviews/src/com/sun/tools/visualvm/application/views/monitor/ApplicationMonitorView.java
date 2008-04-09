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
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
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
import java.lang.management.MemoryMXBean;
import java.text.NumberFormat;
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
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationMonitorView extends DataSourceView {
    private static final Logger LOGGER = Logger.getLogger(ApplicationMonitorView.class.getName());
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/application/views/resources/monitor.png";

    private Jvm jvm;
    private MemoryMXBean memoryMXBean;
    private MonitoredDataListener monitoredDataListener;
    

    public ApplicationMonitorView(Application application) {
        super(application, "Monitor", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 20, false);
    }
    
    @Override
    protected void willBeAdded() {
        Application application = (Application) getDataSource();
        jvm = JvmFactory.getJVMFor(application);
        memoryMXBean = null;
        JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(JmxModelFactory.getJmxModelFor(application));
        if (mxbeans != null) {
            memoryMXBean = mxbeans.getMemoryMXBean();
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
        
        final HeapViewSupport heapViewSupport = new HeapViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Heap", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(heapViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        final PermGenViewSupport permGenViewSupport = new PermGenViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("PermGen", true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(permGenViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        final ClassesViewSupport classesViewSupport = new ClassesViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Classes", true), DataViewComponent.BOTTOM_LEFT);
        dvc.addDetailsView(classesViewSupport.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
        
        final ThreadsViewSupport threadsViewSupport = new ThreadsViewSupport(jvm);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Threads", true), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(threadsViewSupport.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
        
        monitoredDataListener = new MonitoredDataListener() {
            public void monitoredDataEvent(final MonitoredData data) {
                final long time = System.currentTimeMillis();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        masterViewSupport.refresh(data);
                        heapViewSupport.refresh(data, time);
                        permGenViewSupport.refresh(data, time);
                        classesViewSupport.refresh(data, time);
                        threadsViewSupport.refresh(data, time);
                    }
                });
            }
        };
        jvm.addMonitoredDataListener(monitoredDataListener);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel implements DataRemovedListener<Application> {
        
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
            return new DataViewComponent.MasterView("Monitor", null, this);
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
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            area = new HTMLTextArea(getBasicTelemetry(null));
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            setBackground(area.getBackground());
            
            // TODO: implement listener for Application.oomeHeapDumpEnabled
            
            add(area, BorderLayout.CENTER);

            gcButton = new JButton(new AbstractAction("Perform GC") {
                public void actionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            try { memoryMXBean.gc(); } catch (Exception e) { 
                                LOGGER.throwing(ApplicationMonitorView.class.getName(), "initComponents", e);
                            }
                        };
                    });
                }
            });
            gcButton.setEnabled(memoryMXBean != null);
            
            heapDumpButton = new JButton(new AbstractAction("Heap Dump") {
                public void actionPerformed(ActionEvent e) {
                    HeapDumpSupport.getInstance().takeHeapDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
                }
            });
            heapDumpButton.setEnabled(jvm.isTakeHeapDumpSupported());
            
            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setBackground(area.getBackground());
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setBackground(area.getBackground());
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            buttonsContainer.add(gcButton, BorderLayout.WEST);
            buttonsContainer.add(heapDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);
            
            add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);
            
            application.notifyWhenRemoved(this);
        }

        private String getBasicTelemetry(MonitoredData data) {
            if (data == null) return "<nobr><b>Uptime:</b></nobr>";
            else return "<nobr><b>Uptime:</b> " + getTime(data.getUpTime()) + "</nobr>";
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
    
    
    // --- Heap ----------------------------------------------------------------
    
    private static class HeapViewSupport extends JPanel  {
        
        private boolean memoryMonitoringSupported;
        private ChartsSupport.Chart heapMetricsChart;
        private HTMLLabel heapSizeLabel;
        private HTMLLabel usedHeapLabel;
        private HTMLLabel maxHeapLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public HeapViewSupport(Jvm jvm) {
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Heap", null, 10, this, null);
        }
        
        public void refresh(MonitoredData data, long time) {
            if (memoryMonitoringSupported) {
                long heapCapacity = data.getGenCapacity()[0];
                long heapUsed = data.getGenUsed()[0];
                long maxHeap = data.getGenMaxCapacity()[0];
                heapMetricsChart.getModel().addItemValues(time, new long[] { heapCapacity, heapUsed });
                heapSizeLabel.setText("<nobr><b>Heap size:</b> " + formatter.format(heapCapacity) + " </nobr>");
                usedHeapLabel.setText("<nobr><b>Used heap:</b> " + formatter.format(heapUsed) + " </nobr>");
                maxHeapLabel.setText("<nobr><b>Max heap:</b> " + formatter.format(maxHeap) + " </nobr>");
              
                heapMetricsChart.setToolTipText(
                        "<html><nobr><b>Heap size:</b> " + formatter.format(heapCapacity) + " </nobr>" + "<br>" + 
                        "<nobr><b>Used heap:</b> " + formatter.format(heapUsed) + " </nobr>" + "<br>" +
                        "<nobr><b>Max heap:</b> " + formatter.format(maxHeap) + " </nobr></html>");
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
              heapSizeLabel.setText("<nobr><b>Heap size:</b></nobr>");
              heapSizeLabel.setOpaque(false);
              usedHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              usedHeapLabel.setText("<nobr><b>Used heap:</b></nobr>");
              usedHeapLabel.setOpaque(false);
              maxHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              maxHeapLabel.setText("<nobr><b>Live threads peak:</b></nobr>");
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
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public PermGenViewSupport(Jvm jvm) {
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("PermGen", null, 10, this, null);
        }
        
        public void refresh(MonitoredData data, long time) {
            if (memoryMonitoringSupported) {
                long permgenCapacity = data.getGenCapacity()[1];
                long permgenUsed = data.getGenUsed()[1];
                long permgenMax = data.getGenMaxCapacity()[1];
                permgenMetricsChart.getModel().addItemValues(time, new long[] { permgenCapacity, permgenUsed });
                permHeapSizeLabel.setText("<nobr><b>PermGen size:</b> " + formatter.format(permgenCapacity) + " </nobr>");
                permUsedHeapLabel.setText("<nobr><b>Used PermGen:</b> " + formatter.format(permgenUsed) + " </nobr>");
                permMaxHeapLabel.setText("<nobr><b>Max PermGen size:</b> " + formatter.format(permgenMax) + " </nobr>");
              
                permgenMetricsChart.setToolTipText(
                        "<html><nobr><b>PermGen size:</b> " + formatter.format(permgenCapacity) + " </nobr>" + "<br>" + 
                        "<nobr><b>Used PermGen:</b> " + formatter.format(permgenUsed) + " </nobr>" + "<br>" +
                        "<nobr><b>Max PermGen size:</b> " + formatter.format(permgenMax) + " </nobr>");
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
              permHeapSizeLabel.setText("<nobr><b>PermGen size:</b></nobr>");
              permHeapSizeLabel.setOpaque(false);
              permUsedHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              permUsedHeapLabel.setText("<nobr><b>Used PermGen:</b></nobr>");
              permUsedHeapLabel.setOpaque(false);
              permMaxHeapLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              permMaxHeapLabel.setText("<nobr><b>Max PermGen size:</b></nobr>");
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
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public ClassesViewSupport(Jvm jvm) {
            classMonitoringSupported = jvm.isClassMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Classes", null, 10, this, null);
        }
        
        public void refresh(MonitoredData data, long time) {
            if (classMonitoringSupported) {
                long sharedUnloaded = data.getSharedUnloadedClasses();
                long totalUnloaded  = data.getUnloadedClasses();
                long sharedClasses  = data.getSharedLoadedClasses() - sharedUnloaded;
                long totalClasses   = data.getLoadedClasses() - data.getUnloadedClasses() + sharedClasses;
                classesMetricsChart.getModel().addItemValues(time, new long[] { totalClasses, sharedClasses });
                loadedClassesLabel.setText("<nobr><b>Total loaded classes:</b> " + totalClasses + " </nobr>");
                loadedSharedClassesLabel.setText("<nobr><b>Shared loaded classes:</b> " + sharedClasses + " </nobr>");
                unloadedClassesLabel.setText("<nobr><b>Total unloaded classes:</b> " + totalUnloaded + " </nobr>");
                unloadedSharedClassesLabel.setText("<nobr><b>Shared unloaded classes:</b> " + sharedUnloaded + " </nobr>");
              
                classesMetricsChart.setToolTipText(
                        "<html><nobr><b>Total loaded classes:</b> " + totalClasses + " </nobr>" + "<br>" + 
                        "<nobr><b>Shared loaded classes:</b> " + sharedClasses + " </nobr>" + "<br>" +
                        "<nobr><b>Total unloaded classes:</b> " + totalUnloaded + " </nobr>" + "<br>" +
                        "<nobr><b>Shared unloaded classes:</b> " + sharedUnloaded + " </nobr></html>");
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
              loadedClassesLabel.setText("<nobr><b>Total loaded classes:</b></nobr>");
              loadedClassesLabel.setOpaque(false);
              loadedSharedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              loadedSharedClassesLabel.setText("<nobr><b>Shared loaded classes:</b></nobr>");
              loadedSharedClassesLabel.setOpaque(false);
              unloadedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              unloadedClassesLabel.setText("<nobr><b>Total unloaded classes:</b></nobr>");
              unloadedClassesLabel.setOpaque(false);
              unloadedSharedClassesLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              unloadedSharedClassesLabel.setText("<nobr><b>Shared unloaded classes:</b></nobr>");
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
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public ThreadsViewSupport(Jvm jvm) {
            threadsMonitoringSupported = jvm.isThreadMonitoringSupported();
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Threads", null, 10, this, null);
        }
        
        public void refresh(MonitoredData data, long time) {
            if (threadsMonitoringSupported) {
                long totalThreads   = data.getThreadsLive();
                long daemonThreads  = data.getThreadsDaemon();
                long peakThreads    = data.getThreadsLivePeak();
                long startedThreads = data.getThreadsStarted();
                threadsMetricsChart.getModel().addItemValues(time, new long[] { totalThreads, daemonThreads });
                liveThreadsLabel.setText("<nobr><b>Live threads:</b> " + totalThreads + " </nobr>");
                daemonThreadsLabel.setText("<nobr><b>Daemon threads:</b> " + daemonThreads + " </nobr>");
                liveThreadsPeakLabel.setText("<nobr><b>Live threads peak:</b> " + peakThreads + " </nobr>");
                startedThreadsLabel.setText("<nobr><b>Started threads total:</b> " + startedThreads + " </nobr>");
              
                threadsMetricsChart.setToolTipText(
                        "<html><nobr><b>Live threads:</b> " + totalThreads + " </nobr>" + "<br>" + 
                        "<nobr><b>Daemon threads:</b> " + daemonThreads + " </nobr>" + "<br>" +
                        "<nobr><b>Live threads peak:</b> " + peakThreads + " </nobr>" + "<br>" +
                        "<nobr><b>Started threads total:</b> " + startedThreads + " </nobr></html>");
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
              liveThreadsLabel.setText("<nobr><b>Live threads:</b></nobr>");
              liveThreadsLabel.setOpaque(false);
              daemonThreadsLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              daemonThreadsLabel.setText("<nobr><b>Daemon threads:</b></nobr>");
              daemonThreadsLabel.setOpaque(false);
              liveThreadsPeakLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              liveThreadsPeakLabel.setText("<nobr><b>Live threads peak:</b></nobr>");
              liveThreadsPeakLabel.setOpaque(false);
              startedThreadsLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              startedThreadsLabel.setText("<nobr><b>Started threads total:</b></nobr>");
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

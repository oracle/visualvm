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

package com.sun.tools.visualvm.host.views.overview;

import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.host.model.HostOverview;
import com.sun.tools.visualvm.host.model.HostOverviewFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class HostOverviewView extends DataSourceView implements DataRemovedListener<Host> {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/host/views/resources/overview.png";

    private Timer timer;
    private HostOverview hostOverview;
    

    public HostOverviewView(Host host) {
        super(host, "Overview", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 0, false);
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
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport((Host)getDataSource()).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        final CpuLoadViewSupport cpuLoadViewSupport = new CpuLoadViewSupport(hostOverview);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("CPU", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(cpuLoadViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        final PhysicalMemoryViewSupport physicalMemoryViewSupport = new PhysicalMemoryViewSupport();
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Memory", true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(physicalMemoryViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        final SwapMemoryViewSupport swapMemoryViewSupport = new SwapMemoryViewSupport();
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
    
    private static class MasterViewSupport extends JPanel  {
        
        public MasterViewSupport(Host host) {
            initComponents(host);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Overview", null, this);
        }
        
        
        private void initComponents(Host host) {
            setLayout(new BorderLayout());
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralInfo(host) + "</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            setBackground(area.getBackground());
            
            add(area, BorderLayout.CENTER);
        }
        
        String getGeneralInfo(Host host) {
            HostOverview os = HostOverviewFactory.getSystemOverviewFor(host);
            StringBuilder data = new StringBuilder();

            data.append("<b>Host IP:</b> " + os.getHostAddress() + "<br>");
            data.append("<b>Hostname:</b> " + os.getHostName() + "<br><br>");

            String name = os.getName();
            String ver = os.getVersion();
            String patch = os.getPatchLevel();

            patch = "unknown".equals(patch) ? "" : patch;
            data.append("<b>OS:</b> " + name + " (" + ver + ")" + " " + patch + "<br>");
            data.append("<b>Architecture:</b> " + os.getArch() + "<br>");
            data.append("<b>Processors:</b> " + os.getAvailableProcessors() + "<br><br>");
            data.append("<b>Total physical memory size:</b> " + formatBytes(os.getTotalPhysicalMemorySize()) + " MB<br>");
            data.append("<b>Swap size:</b> " + formatBytes(os.getTotalSwapSpaceSize()) + " MB<br>");

            return data.toString();
        }
        
        private String formatBytes(long l) {
            return NumberFormat.getInstance().format(((10*l)/1024/1024)/10.0);
        }
        
    }
    
    
    // --- CPU load ------------------------------------------------------------
    
    private static class CpuLoadViewSupport extends JPanel  {
        
        private boolean cpuMonitoringSupported;
        private ChartsSupport.Chart cpuMetricsChart;
        private HTMLLabel loadLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public CpuLoadViewSupport(HostOverview hostOverview) {
            cpuMonitoringSupported = hostOverview.getSystemLoadAverage() >= 0;
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("CPU load", null, 10, this, null);
        }
        
        public void refresh(HostOverview hostOverview, long time) {
            if (cpuMonitoringSupported) {
                double load = hostOverview.getSystemLoadAverage();
                cpuMetricsChart.getModel().addItemValues(time, new long[] { (long)(load*1000) });
                loadLabel.setText("<nobr><b>Load average:</b> " + formatter.format(load) + " </nobr>");
                cpuMetricsChart.setToolTipText("<html><nobr><b>Load average:</b> " + formatter.format(load) + " </nobr></html>");
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (cpuMonitoringSupported) {
              // CPUMetricsPanel
              loadLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              loadLabel.setText("<nobr><b>Load average:</b></nobr>");
              loadLabel.setOpaque(false);
              final JPanel cpuMetricsDataPanel = new JPanel(new GridLayout(1, 2));
              cpuMetricsDataPanel.setOpaque(false);
              cpuMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
              cpuMetricsDataPanel.add(loadLabel);

              cpuMetricsChart = new ChartsSupport.CPUMetricsChart();
              cpuMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), cpuMetricsChart.getBorder()));
              JPanel cpuMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              cpuMetricsLegendContainer.setOpaque(false);
              cpuMetricsLegendContainer.add(cpuMetricsChart.getBigLegendPanel());
              final JPanel cpuMetricsPanel = new JPanel(new BorderLayout());
              cpuMetricsPanel.setOpaque(true);
              cpuMetricsPanel.setBackground(Color.WHITE);
              cpuMetricsPanel.add(cpuMetricsDataPanel, BorderLayout.NORTH);
              cpuMetricsPanel.add(cpuMetricsChart, BorderLayout.CENTER);
              cpuMetricsPanel.add(cpuMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] cpuMetricsPanelResizing = new boolean[] { false };
              cpuMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              cpuMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (cpuMetricsPanelResizing[0] == true) {
                          cpuMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = cpuMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == cpuMetricsDataPanel.isVisible()) return;

                      cpuMetricsPanelResizing[0] = true;
                      cpuMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
              contents = cpuMetricsPanel;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.HOST);
            }
            
            add(contents, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- Physical memory -----------------------------------------------------
    
    private static class PhysicalMemoryViewSupport extends JPanel  {
        
        private ChartsSupport.Chart memoryMetricsChart;
        private HTMLLabel usedMemoryLabel;
        private HTMLLabel totalMemoryLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public PhysicalMemoryViewSupport() {
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Physical memory", null, 10, this, null);
        }
        
        public void refresh(HostOverview hostOverview, long time) {
            long memoryMax = hostOverview.getTotalPhysicalMemorySize();
            long memoryUsed = memoryMax - hostOverview.getFreePhysicalMemorySize();

            memoryMetricsChart.getModel().addItemValues(time, new long[] { memoryUsed });
            usedMemoryLabel.setText("<nobr><b>Used memory:</b> " + formatter.format(memoryUsed) + " </nobr>");
            totalMemoryLabel.setText("<nobr><b>Total memory:</b> " + formatter.format(memoryMax) + " </nobr>");
            memoryMetricsChart.setToolTipText(
                            "<html><nobr><b>Used memory:</b> " + formatter.format(memoryUsed) + " </nobr>" + "<br>" + 
                            "<nobr><b>Total memory:</b> " + formatter.format(memoryMax) + " </nobr></html>");
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
              // MemoryMetricsPanel
              usedMemoryLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              usedMemoryLabel.setText("<nobr><b>Used memory:</b></nobr>");
              usedMemoryLabel.setOpaque(false);
              totalMemoryLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              totalMemoryLabel.setText("<nobr><b>Total memory:</b></nobr>");
              totalMemoryLabel.setOpaque(false);
              final JPanel memoryMetricsDataPanel = new JPanel(new GridLayout(1, 2));
              memoryMetricsDataPanel.setOpaque(false);
              memoryMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
              memoryMetricsDataPanel.add(usedMemoryLabel);
              memoryMetricsDataPanel.add(totalMemoryLabel);

              memoryMetricsChart = new ChartsSupport.PhysicalMemoryMetricsChart();
              memoryMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), memoryMetricsChart.getBorder()));
              JPanel memoryMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              memoryMetricsLegendContainer.setOpaque(false);
              memoryMetricsLegendContainer.add(memoryMetricsChart.getBigLegendPanel());
              final JPanel memoryMetricsPanel = new JPanel(new BorderLayout());
              memoryMetricsPanel.setOpaque(true);
              memoryMetricsPanel.setBackground(Color.WHITE);
              memoryMetricsPanel.add(memoryMetricsDataPanel, BorderLayout.NORTH);
              memoryMetricsPanel.add(memoryMetricsChart, BorderLayout.CENTER);
              memoryMetricsPanel.add(memoryMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] memoryMetricsPanelResizing = new boolean[] { false };
              memoryMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              memoryMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (memoryMetricsPanelResizing[0] == true) {
                          memoryMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = memoryMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == memoryMetricsDataPanel.isVisible()) return;

                      memoryMetricsPanelResizing[0] = true;
                      memoryMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
            
            add(memoryMetricsPanel, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- Swap memory ---------------------------------------------------------
    
    private static class SwapMemoryViewSupport extends JPanel  {
        
        private ChartsSupport.Chart memorySwapMetricsChart;
        private HTMLLabel usedSwapMemoryLabel;
        private HTMLLabel totalSwapMemoryLabel;
        private static final NumberFormat formatter = NumberFormat.getNumberInstance();
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height;
        
        public SwapMemoryViewSupport() {
            initComponents();
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Swap memory", null, 20, this, null);
        }
        
        public void refresh(HostOverview hostOverview, long time) {
            long memorySwapMax = hostOverview.getTotalSwapSpaceSize();
            long memorySwapUsed = memorySwapMax - hostOverview.getFreeSwapSpaceSize();

            memorySwapMetricsChart.getModel().addItemValues(time, new long[] { memorySwapUsed });
            usedSwapMemoryLabel.setText("<nobr><b>Used swap:</b> " + formatter.format(memorySwapUsed) + " </nobr>");
            totalSwapMemoryLabel.setText("<nobr><b>Total swap:</b> " + formatter.format(memorySwapMax) + " </nobr>");
            memorySwapMetricsChart.setToolTipText(
                            "<html><nobr><b>Used swap:</b> " + formatter.format(memorySwapUsed) + " </nobr>" + "<br>" + 
                            "<nobr><b>Total swap:</b> " + formatter.format(memorySwapMax) + " </nobr></html>");
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
              // SwapMetricsPanel
              usedSwapMemoryLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              usedSwapMemoryLabel.setText("<nobr><b>Used memory:</b></nobr>");
              usedSwapMemoryLabel.setOpaque(false);
              totalSwapMemoryLabel = new HTMLLabel() {
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
              };
              totalSwapMemoryLabel.setText("<nobr><b>Swap memory:</b></nobr>");
              totalSwapMemoryLabel.setOpaque(false);
              final JPanel memorySwapMetricsDataPanel = new JPanel(new GridLayout(1, 2));
              memorySwapMetricsDataPanel.setOpaque(false);
              memorySwapMetricsDataPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
              memorySwapMetricsDataPanel.add(usedSwapMemoryLabel);
              memorySwapMetricsDataPanel.add(totalSwapMemoryLabel);

              memorySwapMetricsChart = new ChartsSupport.SwapMemoryMetricsChart();
              memorySwapMetricsChart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20), memorySwapMetricsChart.getBorder()));
              JPanel memorySwapMetricsLegendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING));
              memorySwapMetricsLegendContainer.setOpaque(false);
              memorySwapMetricsLegendContainer.add(memorySwapMetricsChart.getBigLegendPanel());
              final JPanel memorySwapMetricsPanel = new JPanel(new BorderLayout());
              memorySwapMetricsPanel.setOpaque(true);
              memorySwapMetricsPanel.setBackground(Color.WHITE);
              memorySwapMetricsPanel.add(memorySwapMetricsDataPanel, BorderLayout.NORTH);
              memorySwapMetricsPanel.add(memorySwapMetricsChart, BorderLayout.CENTER);
              memorySwapMetricsPanel.add(memorySwapMetricsLegendContainer, BorderLayout.SOUTH);

              final boolean[] memorySwapMetricsPanelResizing = new boolean[] { false };
              memorySwapMetricsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
              memorySwapMetricsPanel.addComponentListener(new ComponentAdapter() {
                  public void componentResized(ComponentEvent e) {
                      if (memorySwapMetricsPanelResizing[0] == true) {
                          memorySwapMetricsPanelResizing[0] = false;
                          return;
                      }

                      boolean shouldBeVisible = memorySwapMetricsPanel.getSize().height > ChartsSupport.MINIMUM_CHART_HEIGHT;
                      if (shouldBeVisible == memorySwapMetricsDataPanel.isVisible()) return;

                      memorySwapMetricsPanelResizing[0] = true;
                      memorySwapMetricsDataPanel.setVisible(shouldBeVisible);
                  }
              });
            
            add(memorySwapMetricsPanel, BorderLayout.CENTER);
        }
        
    }

}

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
import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.ColorFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
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
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.management.MemoryMXBean;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
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
        super(application, NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Monitor"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 20, false);   // NOI18N
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
        
        final CpuViewSupport cpuViewSupport = new CpuViewSupport(application, jvm);
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
        private boolean gcMonitoringSupported;
        
        private static final String UNKNOWN = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Unknown"); // NOI18N
        private static final String CPU = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu"); // NOI18N
        private static final String CPU_USAGE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu_Usage"); // NOI18N
        private static final String GC_USAGE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Gc_Usage"); // NOI18N

        private SimpleXYChartSupport chartSupport;

        private long lastUpTime = -1;
        private long lastProcessCpuTime = -1;
        private long lastProcessGcTime = -1;
        private int processors = 1;

        public CpuViewSupport(Application app, Jvm jvm) {
            cpuMonitoringSupported = jvm.isCpuMonitoringSupported();
            gcMonitoringSupported = jvm.isCollectionTimeSupported();
            if (cpuMonitoringSupported || gcMonitoringSupported) {
                // TODO: use HostOverview once implemented to get processors count
                // HostOverviewFactory.getSystemOverviewFor(app.getHost()).getAvailableProcessors();
                JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
                if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                    JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                    if (mxbeans != null)
                        processors = mxbeans.getOperatingSystemMXBean().getAvailableProcessors();
                }
            }
            initModels();
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(CPU, null, 10, this, null);
        }

        public void refresh(MonitoredData data, long time) {
            if (cpuMonitoringSupported || gcMonitoringSupported) {

                long upTime = data.getUpTime() * 1000000;
                long processCpuTime = cpuMonitoringSupported ? data.getProcessCpuTime() / processors : -1;
                long processGcTime  = gcMonitoringSupported  ? data.getCollectionTime() * 1000000 / processors : -1;

                boolean tracksProcessCpuTime = lastProcessCpuTime != -1;
                boolean tracksProcessGcTime  = lastProcessGcTime != -1;

                if (lastUpTime != -1 && (tracksProcessCpuTime || tracksProcessGcTime)) {

                    long upTimeDiff = upTime - lastUpTime;
                    long cpuUsage = -1;
                    long gcUsage = -1;
                    String cpuDetail = UNKNOWN;
                    String gcDetail = UNKNOWN;

                    if (lastProcessCpuTime != -1) {
                        long processTimeDiff = processCpuTime - lastProcessCpuTime;
                        cpuUsage = upTimeDiff > 0 ? Math.min((long)(1000 * (float)processTimeDiff /
                                                             (float)upTimeDiff), 1000) : 0;
                        cpuDetail = cpuUsage == -1 ? UNKNOWN : chartSupport.formatPercent(cpuUsage);
                    }

                    if (lastProcessGcTime != -1) {
                        long processGcTimeDiff = processGcTime - lastProcessGcTime;
                        gcUsage = upTimeDiff > 0 ? Math.min((long)(1000 * (float)processGcTimeDiff /
                                                            (float)upTimeDiff), 1000) : 0;
                        if (cpuUsage != -1 && cpuUsage < gcUsage) gcUsage = cpuUsage;
                        gcDetail = gcUsage == -1 ? UNKNOWN : chartSupport.formatPercent(gcUsage);
                    }
                    
                    chartSupport.addValues(time, new long[] { Math.max(cpuUsage, 0), Math.max(gcUsage, 0) });
                    chartSupport.updateDetails(new String[] { cpuDetail, gcDetail });

                }

                lastUpTime = upTime;
                lastProcessCpuTime = processCpuTime;
                lastProcessGcTime = processGcTime;
            }
        }

        private void initModels() {
            String[] itemNames = new String[] { CPU_USAGE, GC_USAGE };

            Iterator<Color> colors = ColorFactory.predefinedColors();
            Color[] itemColors = new Color[] { colors.next(), colors.next() };

            float[] lineWidths = new float[] { 2f, 2f};

            Color[] lineColors = new Color[] { itemColors[0], itemColors[1] };

            GlobalPreferences preferences = GlobalPreferences.sharedInstance();
            int itemsCount = preferences.getMonitoredDataCache() * 60 /
                             preferences.getMonitoredDataPoll();

            String[] detailsItems = new String[] { CPU_USAGE, GC_USAGE };

            chartSupport = ChartFactory.createSimplePercentXYChart(
                                                            itemNames, itemColors,
                                                            lineWidths, lineColors,
                                                            null, null, false,
                                                            itemsCount, detailsItems);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (cpuMonitoringSupported || gcMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Heap ----------------------------------------------------------------

    private static class HeapViewSupport extends JPanel  {

        private boolean memoryMonitoringSupported;

        private static final String HEAP_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_size"); // NOI18N
        private static final String HEAP_SIZE_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_size_leg"); // NOI18N
        private static final String USED_HEAP = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_heap"); // NOI18N
        private static final String USED_HEAP_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_heap_leg"); // NOI18N
        private static final String MAX_HEAP = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Max_Heap");   // NOI18N

        private SimpleXYChartSupport chartSupport;

        public HeapViewSupport(Jvm jvm) {
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            initModels();
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

                chartSupport.addValues(time, new long[] { heapCapacity, heapUsed });
                chartSupport.updateDetails(new String[] { chartSupport.formatBytes(heapCapacity),
                                                          chartSupport.formatBytes(heapUsed),
                                                          chartSupport.formatBytes(maxHeap) });
            }
        }

        private void initModels() {
            String[] itemNames = new String[] { HEAP_SIZE_LEG, USED_HEAP_LEG };

            Iterator<Color> colors = ColorFactory.predefinedColors();
            Color[] itemColors = new Color[] { colors.next(), colors.next() };

            float[] lineWidths = new float[] { 2f, 2f};

            Color[] lineColors = new Color[] { itemColors[0], itemColors[1] };

            Iterator<Color[]> fills = ColorFactory.predefinedGradients();
            Color[] grads1 = fills.next();
            Color[] grads2 = fills.next();
            Color[] fillColors1 = new Color[] {grads1[0], grads2[0]};
            Color[] fillColors2 = new Color[] {grads1[1], grads2[1]};

            GlobalPreferences preferences = GlobalPreferences.sharedInstance();
            int itemsCount = preferences.getMonitoredDataCache() * 60 /
                             preferences.getMonitoredDataPoll();

            String[] detailsItems = new String[] { HEAP_SIZE, USED_HEAP, MAX_HEAP };

            chartSupport = ChartFactory.createSimpleBytesXYChart(10 * 1024 * 1024,
                                                            itemNames, itemColors,
                                                            lineWidths, lineColors,
                                                            fillColors1, fillColors2, 0,
                                                            SimpleXYChartSupport.MAX_UNDEFINED,
                                                            false, itemsCount, detailsItems);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- PermGen -------------------------------------------------------------

    private static class PermGenViewSupport extends JPanel  {

        private boolean memoryMonitoringSupported;
        
        private static final String PERM_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_PermGen_size");  // NOI18N
        private static final String PERM_SIZE_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_PermGen_size_leg");  // NOI18N
        private static final String USED_PERM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_PermGen");  // NOI18N
        private static final String USED_PERM_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_PermGen_leg");  // NOI18N
        private static final String MAX_PERM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Max_PermGen_size");   // NOI18N

        private SimpleXYChartSupport chartSupport;

        public PermGenViewSupport(Jvm jvm) {
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            initModels();
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

                chartSupport.addValues(time, new long[] { permgenCapacity, permgenUsed });
                chartSupport.updateDetails(new String[] { chartSupport.formatBytes(permgenCapacity),
                                                          chartSupport.formatBytes(permgenUsed),
                                                          chartSupport.formatBytes(permgenMax) });
            }
        }

        private void initModels() {
            String[] itemNames = new String[] { PERM_SIZE_LEG, USED_PERM_LEG };

            Iterator<Color> colors = ColorFactory.predefinedColors();
            Color[] itemColors = new Color[] { colors.next(), colors.next() };

            float[] lineWidths = new float[] { 2f, 2f};

            Color[] lineColors = new Color[] { itemColors[0], itemColors[1] };

            Iterator<Color[]> fills = ColorFactory.predefinedGradients();
            Color[] grads1 = fills.next();
            Color[] grads2 = fills.next();
            Color[] fillColors1 = new Color[] {grads1[0], grads2[0]};
            Color[] fillColors2 = new Color[] {grads1[1], grads2[1]};

            GlobalPreferences preferences = GlobalPreferences.sharedInstance();
            int itemsCount = preferences.getMonitoredDataCache() * 60 /
                             preferences.getMonitoredDataPoll();

            String[] detailsItems = new String[] { PERM_SIZE, USED_PERM, MAX_PERM };

            chartSupport = ChartFactory.createSimpleBytesXYChart(10 * 1024 * 1024,
                                                            itemNames, itemColors,
                                                            lineWidths, lineColors,
                                                            fillColors1, fillColors2, 0,
                                                            SimpleXYChartSupport.MAX_UNDEFINED,
                                                            false, itemsCount, detailsItems);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Classes -------------------------------------------------------------

    private static class ClassesViewSupport extends JPanel  {

        private boolean classMonitoringSupported;
        
        private static final String TOTAL_LOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_loaded_classes");   // NOI18N
        private static final String TOTAL_LOADED_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_loaded_classes_leg");   // NOI18N
        private static final String SHARED_LOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_loaded_classes"); // NOI18N
        private static final String SHARED_LOADED_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_loaded_classes_leg"); // NOI18N
        private static final String TOTAL_UNLOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_unloaded_classes");   // NOI18N
        private static final String SHARED_UNLOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_unloaded_classes"); // NOI18N

        private SimpleXYChartSupport chartSupport;

        public ClassesViewSupport(Jvm jvm) {
            classMonitoringSupported = jvm.isClassMonitoringSupported();
            initModels();
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

                chartSupport.addValues(time, new long[] { totalClasses, sharedClasses });
                chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(totalClasses),
                                                          chartSupport.formatDecimal(sharedClasses),
                                                          chartSupport.formatDecimal(totalUnloaded),
                                                          chartSupport.formatDecimal(sharedUnloaded) });
            }
        }

        private void initModels() {
            String[] itemNames = new String[] { TOTAL_LOADED_LEG, SHARED_LOADED_LEG };

            Iterator<Color> colors = ColorFactory.predefinedColors();
            Color[] itemColors = new Color[] { colors.next(), colors.next() };

            float[] lineWidths = new float[] { 2f, 2f};

            Color[] lineColors = new Color[] { itemColors[0], itemColors[1] };

            GlobalPreferences preferences = GlobalPreferences.sharedInstance();
            int itemsCount = preferences.getMonitoredDataCache() * 60 /
                             preferences.getMonitoredDataPoll();

            String[] detailsItems = new String[] { TOTAL_LOADED, SHARED_LOADED,
                                                   TOTAL_UNLOADED, SHARED_UNLOADED };

            chartSupport = ChartFactory.createSimpleDecimalXYChart(100,
                                                            itemNames, itemColors,
                                                            lineWidths, lineColors,
                                                            null, null, 0,
                                                            SimpleXYChartSupport.MAX_UNDEFINED,
                                                            false, itemsCount, detailsItems);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (classMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Threads -------------------------------------------------------------

    private static class ThreadsViewSupport extends JPanel  {

        private boolean threadsMonitoringSupported;

        private static final String LIVE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads");   // NOI18N
        private static final String LIVE_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads_leg");   // NOI18N
        private static final String DAEMON = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Daemon_threads");// NOI18N
        private static final String DAEMON_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Daemon_threads_leg");// NOI18N
        private static final String PEAK = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads_peak");  // NOI18N
        private static final String STARTED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Started_threads_total");   // NOI18N

        private SimpleXYChartSupport chartSupport;

        public ThreadsViewSupport(Jvm jvm) {
            threadsMonitoringSupported = jvm.isThreadMonitoringSupported();
            initModels();
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

                chartSupport.addValues(time, new long[] { totalThreads, daemonThreads });
                chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(totalThreads),
                                                          chartSupport.formatDecimal(daemonThreads),
                                                          chartSupport.formatDecimal(peakThreads),
                                                          chartSupport.formatDecimal(startedThreads) });
            }
        }

        private void initModels() {
            String[] itemNames = new String[] { LIVE_LEG, DAEMON_LEG };

            Iterator<Color> colors = ColorFactory.predefinedColors();
            Color[] itemColors = new Color[] { colors.next(), colors.next() };

            float[] lineWidths = new float[] { 2f, 2f};

            Color[] lineColors = new Color[] { itemColors[0], itemColors[1] };

            GlobalPreferences preferences = GlobalPreferences.sharedInstance();
            int itemsCount = preferences.getMonitoredDataCache() * 60 /
                             preferences.getMonitoredDataPoll();

            String[] detailsItems = new String[] { LIVE, DAEMON,
                                                   PEAK, STARTED };

            chartSupport = ChartFactory.createSimpleDecimalXYChart(3,
                                                            itemNames, itemColors,
                                                            lineWidths, lineColors,
                                                            null, null, 0,
                                                            SimpleXYChartSupport.MAX_UNDEFINED,
                                                            false, itemsCount, detailsItems);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (threadsMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }

}

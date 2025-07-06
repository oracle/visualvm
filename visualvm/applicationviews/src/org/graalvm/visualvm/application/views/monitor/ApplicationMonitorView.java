/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views.monitor;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
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

    private static final String UNKNOWN = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Unknown"); // NOI18N
    private static final String IMAGE_PATH = "org/graalvm/visualvm/application/views/resources/monitor.png";  // NOI18N

    private final ApplicationMonitorModel model;
    

    ApplicationMonitorView(ApplicationMonitorModel model) {
        super(model.getSource(), NbBundle.getMessage(ApplicationMonitorView.class,
                                 "LBL_Monitor"), new ImageIcon(ImageUtilities.   // NOI18N
                                 loadImage(IMAGE_PATH, true)).getImage(), 20, false);
        this.model = model;
    }
    
    protected void willBeAdded() {
        model.initialize();
    }
    
    protected void removed() {
        model.cleanup();
    }

    ApplicationMonitorModel getModel() {
        return model;
    }
    
    protected DataViewComponent createComponent() {
        final MasterViewSupport masterViewSupport = new MasterViewSupport(model);
        DataViewComponent dvc = new DataViewComponent(
                masterViewSupport.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        final CpuViewSupport cpuViewSupport = new CpuViewSupport(model);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                getMessage(ApplicationMonitorView.class, "LBL_Cpu"), true), DataViewComponent.TOP_LEFT);  // NOI18N
        dvc.addDetailsView(cpuViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);

        final HeapViewSupport heapViewSupport = new HeapViewSupport(model);
        final PermGenViewSupport permGenViewSupport = model.isMemoryMonitoringSupported() ? new PermGenViewSupport(model) : null;
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                getMessage(ApplicationMonitorView.class, "LBL_Memory"), true), DataViewComponent.TOP_RIGHT);  // NOI18N
        dvc.addDetailsView(heapViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        if (permGenViewSupport != null) dvc.addDetailsView(permGenViewSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);

        final ClassesViewSupport classesViewSupport = new ClassesViewSupport(model);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                getMessage(ApplicationMonitorView.class, "LBL_Classes"), true), DataViewComponent.BOTTOM_LEFT);    // NOI18N
        dvc.addDetailsView(classesViewSupport.getDetailsView(), DataViewComponent.BOTTOM_LEFT);

        final ThreadsViewSupport threadsViewSupport = new ThreadsViewSupport(model);
        final VirtualThreadsViewSupport vThreadsViewSupport = model.isVirtualThreadsMonitoringSupported() ? new VirtualThreadsViewSupport(model) : null;
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                getMessage(ApplicationMonitorView.class, "LBL_Threads"), true), DataViewComponent.BOTTOM_RIGHT);   // NOI18N
        dvc.addDetailsView(threadsViewSupport.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
        if (vThreadsViewSupport != null) dvc.addDetailsView(vThreadsViewSupport.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);

        final Runnable refresher = new Runnable() {
            public void run() {
                masterViewSupport.refresh(model);
                cpuViewSupport.refresh(model);
                heapViewSupport.refresh(model);
                if (permGenViewSupport != null) permGenViewSupport.refresh(model);
                classesViewSupport.refresh(model);
                threadsViewSupport.refresh(model);
                if (vThreadsViewSupport != null) vThreadsViewSupport.refresh(model);
            }
        };

        refresher.run();

        model.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refresher.run();
            }
        });
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel implements DataRemovedListener<DataSource>, PropertyChangeListener {

        private HTMLTextArea area;
        private JButton gcButton;
        private JButton heapDumpButton;
        
        MasterViewSupport(ApplicationMonitorModel model) {
            initComponents(model);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Monitor"), null, this);  // NOI18N
        }
        
        public void refresh(ApplicationMonitorModel model) {
            int selStart = area.getSelectionStart();
            int selEnd   = area.getSelectionEnd();
            area.setText(getBasicTelemetry(model));
            area.select(selStart, selEnd);
        }
        
        public void dataRemoved(DataSource dataSource) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    gcButton.setEnabled(false);
                    heapDumpButton.setEnabled(false);
                }
            });
        }

        public void propertyChange(PropertyChangeEvent evt) {
            dataRemoved(null);
        }
        
        private void initComponents(final ApplicationMonitorModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea(getBasicTelemetry(model));
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
                        
            add(area, BorderLayout.CENTER);

            gcButton = new JButton(new AbstractAction(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Perform_GC")) {    // NOI18N
                public void actionPerformed(ActionEvent e) {
                    new RequestProcessor("GC Processor").post(new Runnable() { // NOI18N
                        public void run() {
                            try {
                                model.getMemoryMXBean().gc(); 
                            } catch (SecurityException ex) {
                                String err = NbBundle.getMessage(ApplicationMonitorView.class, "TXT_Perform_GC_failed", ex.getLocalizedMessage());  // NOI18N
                                NotifyDescriptor nd = new NotifyDescriptor.Message(err, NotifyDescriptor.INFORMATION_MESSAGE);
                                DialogDisplayer.getDefault().notify(nd);
                                gcButton.setEnabled(false);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "initComponents", e);   // NOI18N
                                gcButton.setEnabled(false);
                            }
                        }
                    });
                }
            });
            gcButton.setEnabled(model.getMemoryMXBean() != null);
            
            heapDumpButton = new JButton(new AbstractAction(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_Dump")) {   // NOI18N
                public void actionPerformed(ActionEvent e) {
                    Application application = (Application)model.getSource();
                    boolean local = application.isLocalApplication();
                    boolean tagged = (e.getModifiers() & Toolkit.getDefaultToolkit().
                                      getMenuShortcutKeyMask()) != 0;
                    HeapDumpSupport hds = HeapDumpSupport.getInstance();
                    if (local) hds.takeHeapDump(application, !tagged);
                    else hds.takeRemoteHeapDump(application, null, !tagged);
                }
            });
            heapDumpButton.setEnabled(model.isTakeHeapDumpSupported());
            
            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setOpaque(false);
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setBackground(area.getBackground());
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            buttonsContainer.add(gcButton, BorderLayout.WEST);
            buttonsContainer.add(heapDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);
            
            add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);

            if (model.getSource() instanceof Application) {
                Application application = (Application)model.getSource();
                application.notifyWhenRemoved(this);
                application.addPropertyChangeListener(Stateful.PROPERTY_STATE, WeakListeners.propertyChange(this,application));
            }
        }

        private String getBasicTelemetry(ApplicationMonitorModel model) {
            String uptime = model.getUpTime() == -1 ? UNKNOWN : getTime(model.getUpTime());
            return NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Uptime", uptime); // NOI18N
        }
        
        public static String getTime(long millis) {
            // Hours
            long hours = millis / 3600000;
            String sHours = (hours == 0 ? "" : "" + hours); // NOI18N
            millis = millis % 3600000;
            
            // Minutes
            long minutes = millis / 60000;
            String sMinutes = (((hours > 0) && (minutes < 10)) ? "0" + minutes : "" + minutes); // NOI18N
            millis = millis % 60000;
            
            // Seconds
            long seconds = millis / 1000;
            String sSeconds = ((seconds < 10) ? "0" + seconds : "" + seconds); // NOI18N
            
            if (sHours.isEmpty()) {
                 return NbBundle.getMessage(ApplicationMonitorView.class, "FORMAT_ms", // NOI18N
                                            new Object[] { sMinutes, sSeconds });
            } else {
                return NbBundle.getMessage(ApplicationMonitorView.class, "FORMAT_hms", // NOI18N
                                            new Object[] { sHours, sMinutes, sSeconds });
            }
        }
        
    }

    
    // --- CPU -----------------------------------------------------------------
    
    private static class CpuViewSupport extends JPanel  {

        private static final String CPU = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu"); // NOI18N
        private static final String CPU_USAGE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Cpu_Usage"); // NOI18N
        private static final String GC_USAGE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Gc_Usage"); // NOI18N

        private boolean liveModel;
        private int processorsCount;
        private boolean cpuMonitoringSupported;
        private boolean gcMonitoringSupported;

        private SimpleXYChartSupport chartSupport;


        CpuViewSupport(ApplicationMonitorModel model) {
            initModels(model);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(CPU, null, 10, this, null);
        }

        public void refresh(ApplicationMonitorModel model) {
            if (cpuMonitoringSupported || gcMonitoringSupported) {

                long upTime = model.getUpTime() * 1000000;
                long prevUpTime = model.getPrevUpTime() * 1000000;

                boolean tracksProcessCpuTime = cpuMonitoringSupported &&
                                               model.getPrevProcessCpuTime() != -1;
                long processCpuTime = tracksProcessCpuTime ?
                    model.getProcessCpuTime() / processorsCount : -1;
                long prevProcessCpuTime = tracksProcessCpuTime ?
                    model.getPrevProcessCpuTime() / processorsCount : -1;

                boolean tracksProcessGcTime  = gcMonitoringSupported &&
                                               model.getPrevProcessGcTime() != -1;
                long processGcTime  = tracksProcessGcTime  ?
                    model.getProcessGcTime() * 1000000 / processorsCount : -1;
                long prevProcessGcTime  = tracksProcessGcTime  ?
                    model.getPrevProcessGcTime() * 1000000 / processorsCount : -1;

                if (prevUpTime != -1 && (tracksProcessCpuTime || tracksProcessGcTime)) {

                    long upTimeDiff = upTime - prevUpTime;
                    long cpuUsage = -1;
                    long gcUsage = -1;
                    String cpuDetail = UNKNOWN;
                    String gcDetail = UNKNOWN;

                    if (tracksProcessCpuTime) {
                        long processTimeDiff = processCpuTime - prevProcessCpuTime;
                        cpuUsage = upTimeDiff > 0 ? Math.min((long)(1000 * (float)processTimeDiff /
                                                             (float)upTimeDiff), 1000) : 0;
                        cpuDetail = cpuUsage == -1 ? UNKNOWN : chartSupport.formatPercent(cpuUsage);
                    }

                    if (tracksProcessGcTime) {
                        long processGcTimeDiff = processGcTime - prevProcessGcTime;
                        gcUsage = upTimeDiff > 0 ? Math.min((long)(1000 * (float)processGcTimeDiff /
                                                            (float)upTimeDiff), 1000) : 0;
                        if (cpuUsage != -1 && cpuUsage < gcUsage) gcUsage = cpuUsage;
                        gcDetail = gcUsage == -1 ? UNKNOWN : chartSupport.formatPercent(gcUsage);
                    }
                    
                    if (liveModel)
                        chartSupport.addValues(model.getTimestamp(), new long[] { Math.max(cpuUsage, 0), Math.max(gcUsage, 0) });
                    chartSupport.updateDetails(new String[] { cpuDetail, gcDetail });

                }
            }
        }

        private void initModels(ApplicationMonitorModel model) {
            liveModel = model.isLive();
            processorsCount = model.getProcessorsCount();
            cpuMonitoringSupported = model.isCpuMonitoringSupported();
            gcMonitoringSupported = model.isGcMonitoringSupported();

            if (cpuMonitoringSupported || gcMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.percent(false, 0.1d, model.getChartCache());

                chartDescriptor.addLineItems(CPU_USAGE, GC_USAGE);
                chartDescriptor.setDetailsItems(new String[] { CPU_USAGE, GC_USAGE });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
                model.registerCpuChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (cpuMonitoringSupported || gcMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Heap ----------------------------------------------------------------

    private static class HeapViewSupport extends JPanel  {
        private boolean liveModel;
        private boolean memoryMonitoringSupported;
        private String heapName;

        private SimpleXYChartSupport chartSupport;

        HeapViewSupport(ApplicationMonitorModel model) {
            initModels(model);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(heapName, null, 10, this, null);
        }

        public void refresh(ApplicationMonitorModel model) {
            if (memoryMonitoringSupported) {
                long heapCapacity = model.getHeapCapacity();
                long heapUsed = model.getHeapUsed();
                long maxHeap = model.getMaxHeap();

                if (liveModel)
                        chartSupport.addValues(model.getTimestamp(), new long[] { heapCapacity, heapUsed });
                chartSupport.updateDetails(new String[] { chartSupport.formatBytes(heapCapacity),
                                                          chartSupport.formatBytes(heapUsed),
                                                          chartSupport.formatBytes(maxHeap) });
            }
        }

        private void initModels(ApplicationMonitorModel model) {
            liveModel = model.isLive();
            memoryMonitoringSupported = model.isMemoryMonitoringSupported();
            heapName = memoryMonitoringSupported ? model.getHeapName() : NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Memory"); // NOI18N

            if (memoryMonitoringSupported) {
                String HEAP_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_size"); // NOI18N
                String HEAP_SIZE_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Heap_size_leg",heapName); // NOI18N
                String USED_HEAP = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_heap"); // NOI18N
                String USED_HEAP_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_heap_leg",heapName.toLowerCase()); // NOI18N
                String MAX_HEAP = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Max_Heap");   // NOI18N

                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, model.getChartCache());

                chartDescriptor.addLineFillItems(HEAP_SIZE_LEG, USED_HEAP_LEG);
                chartDescriptor.setDetailsItems(new String[] { HEAP_SIZE, USED_HEAP, MAX_HEAP });
                chartDescriptor.setLimitYValue(model.getMaxHeap());

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
                model.registerHeapChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- PermGen -------------------------------------------------------------

    private static class PermGenViewSupport extends JPanel  {

        private boolean liveModel;
        private boolean memoryMonitoringSupported;
        private String permgenName;

        private SimpleXYChartSupport chartSupport;

        PermGenViewSupport(ApplicationMonitorModel model) {
            initModels(model);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(permgenName, null, 10, this, null);
        }

        public void refresh(ApplicationMonitorModel model) {
            if (memoryMonitoringSupported) {
                long permgenCapacity = model.getPermgenCapacity();
                long permgenUsed = model.getPermgenUsed();
                long permgenMax = model.getPermgenMax();

                if (liveModel)
                        chartSupport.addValues(model.getTimestamp(), new long[] { permgenCapacity, permgenUsed });
                chartSupport.updateDetails(new String[] { chartSupport.formatBytes(permgenCapacity),
                                                          chartSupport.formatBytes(permgenUsed),
                                                          chartSupport.formatBytes(permgenMax) });
            }
        }

        private void initModels(ApplicationMonitorModel model) {
            liveModel = model.isLive();
            memoryMonitoringSupported = model.isMemoryMonitoringSupported();
            permgenName = model.getPermgenName();

            if (memoryMonitoringSupported) {
                String PERM_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_PermGen_size");  // NOI18N
                String PERM_SIZE_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_PermGen_size_leg", permgenName);  // NOI18N
                String USED_PERM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_PermGen");  // NOI18N
                String USED_PERM_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Used_PermGen_leg", permgenName);  // NOI18N
                String MAX_PERM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Max_PermGen_size");   // NOI18N
                long permgenMax = model.getPermgenMax();

                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.bytes(10 * 1024 * 1024, false, model.getChartCache());

                chartDescriptor.addLineFillItems(PERM_SIZE_LEG, USED_PERM_LEG);
                chartDescriptor.setDetailsItems(new String[] { PERM_SIZE, USED_PERM, MAX_PERM });
                if (permgenMax != -1) {
                    chartDescriptor.setLimitYValue(permgenMax);
                }

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
                model.registerPermGenChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (memoryMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Classes -------------------------------------------------------------

    private static class ClassesViewSupport extends JPanel  {

        private static final String TOTAL_LOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_loaded_classes");   // NOI18N
        private static final String TOTAL_LOADED_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_loaded_classes_leg");   // NOI18N
        private static final String SHARED_LOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_loaded_classes"); // NOI18N
        private static final String SHARED_LOADED_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_loaded_classes_leg"); // NOI18N
        private static final String TOTAL_UNLOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Total_unloaded_classes");   // NOI18N
        private static final String SHARED_UNLOADED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Shared_unloaded_classes"); // NOI18N

        private boolean liveModel;
        private boolean classMonitoringSupported;

        private SimpleXYChartSupport chartSupport;

        ClassesViewSupport(ApplicationMonitorModel model) {
            initModels(model);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Classes"), null, 10, this, null);   // NOI18N
        }

        public void refresh(ApplicationMonitorModel model) {
            if (classMonitoringSupported) {
                long sharedUnloaded = model.getSharedUnloaded();
                long totalUnloaded  = model.getTotalUnloaded();
                long sharedClasses  = model.getSharedLoaded() - sharedUnloaded;
                long totalClasses   = model.getTotalLoaded() - totalUnloaded + sharedClasses;

                if (liveModel)
                        chartSupport.addValues(model.getTimestamp(), new long[] { totalClasses, sharedClasses });
                chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(totalClasses),
                                                          chartSupport.formatDecimal(sharedClasses),
                                                          chartSupport.formatDecimal(totalUnloaded),
                                                          chartSupport.formatDecimal(sharedUnloaded) });
            }
        }

        private void initModels(ApplicationMonitorModel model) {
            liveModel = model.isLive();
            classMonitoringSupported = model.isClassMonitoringSupported();

            if (classMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.decimal(100, false, model.getChartCache());

                chartDescriptor.addLineItems(TOTAL_LOADED_LEG, SHARED_LOADED_LEG);
                chartDescriptor.setDetailsItems(new String[] { TOTAL_LOADED, SHARED_LOADED,
                                                               TOTAL_UNLOADED, SHARED_UNLOADED });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
                model.registerClassesChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (classMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }


    // --- Threads -------------------------------------------------------------

    private static class ThreadsViewSupport extends JPanel  {

        private static final String LIVE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads");   // NOI18N
        private static final String LIVE_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads_leg");   // NOI18N
        private static final String DAEMON = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Daemon_threads");// NOI18N
        private static final String DAEMON_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Daemon_threads_leg");// NOI18N
        private static final String PEAK = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Live_threads_peak");  // NOI18N
        private static final String STARTED = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Started_threads_total");   // NOI18N

        private boolean liveModel;
        private boolean threadsMonitoringSupported;

        private SimpleXYChartSupport chartSupport;

        ThreadsViewSupport(ApplicationMonitorModel model) {
            initModels(model);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Threads"), null, 10, this, null);   // NOI18N
        }

        public void refresh(ApplicationMonitorModel model) {
            if (threadsMonitoringSupported) {
                long totalThreads   = model.getTotalThreads();
                long daemonThreads  = model.getDeamonThreads();
                long peakThreads    = model.getPeakThreads();
                long startedThreads = model.getStartedThreads();

                if (liveModel)
                        chartSupport.addValues(model.getTimestamp(), new long[] { totalThreads, daemonThreads });
                chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(totalThreads),
                                                          chartSupport.formatDecimal(daemonThreads),
                                                          chartSupport.formatDecimal(peakThreads),
                                                          chartSupport.formatDecimal(startedThreads) });
            }
        }

        private void initModels(ApplicationMonitorModel model) {
            liveModel = model.isLive();
            threadsMonitoringSupported = model.isThreadsMonitoringSupported();

            if (threadsMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.decimal(3, false, model.getChartCache());

                chartDescriptor.addLineItems(LIVE_LEG, DAEMON_LEG);
                chartDescriptor.setDetailsItems(new String[] { LIVE, DAEMON,
                                                               PEAK, STARTED });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
                model.registerThreadsChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (threadsMonitoringSupported) {
                add(chartSupport.getChart(), BorderLayout.CENTER);
                chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN });
            } else {
                add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
            }
        }

    }

    // --- Virtual Threads -------------------------------------------------------------

    private static class VirtualThreadsViewSupport extends JPanel  {

        private static final String PARALLELISM = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Parallelism");   // NOI18N
        private static final String POOL_SIZE = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Pool_size");  // NOI18N
        private static final String MOUNTED_VT_COUNT = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Mounted_virtual_thread_count");   // NOI18N
        private static final String MOUNTED_VT_COUNT_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Mounted_virtual_thread_count_leg");   // NOI18N
        private static final String QUEUED_VT_COUNT = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Queued_virtual_thread_count");// NOI18N
        private static final String QUEUED_VT_COUNT_LEG = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Queued_virtual_thread_count_leg");// NOI18N
        private static final String VIRTUAL_THREADS = NbBundle.getMessage(ApplicationMonitorView.class, "LBL_Virtual_Threads");   // NOI18N

        private boolean liveModel;
        private boolean virtualThreadsMonitoringSupported;

        private SimpleXYChartSupport chartSupport;


        VirtualThreadsViewSupport(ApplicationMonitorModel model) {
            initModels(model);
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(VIRTUAL_THREADS, null, 10, this, null);
        }

        public void refresh(ApplicationMonitorModel model) {
                int parallelism = model.getParallelism();
                int poolSize = model.getPoolSize();
                int mountedVirtualThreadCount = model.getMountedVirtualThreadCount();
                long queuedVirtualThreadCount = model.getQueuedVirtualThreadCount();

                if (liveModel)
                        chartSupport.addValues(model.getTimestamp(), new long[] { mountedVirtualThreadCount, queuedVirtualThreadCount });
                chartSupport.updateDetails(new String[] { chartSupport.formatDecimal(parallelism),
                                                          chartSupport.formatDecimal(poolSize),
                                                          chartSupport.formatDecimal(mountedVirtualThreadCount),
                                                          chartSupport.formatDecimal(queuedVirtualThreadCount) });
        }

        private void initModels(ApplicationMonitorModel model) {
            liveModel = model.isLive();
            virtualThreadsMonitoringSupported = model.isVirtualThreadsMonitoringSupported();

            if (virtualThreadsMonitoringSupported) {
                SimpleXYChartDescriptor chartDescriptor =
                        SimpleXYChartDescriptor.decimal(3, false, model.getChartCache());

                chartDescriptor.addLineItems(MOUNTED_VT_COUNT_LEG, QUEUED_VT_COUNT_LEG);
                chartDescriptor.setDetailsItems(new String[] { PARALLELISM, POOL_SIZE,
                                                               MOUNTED_VT_COUNT, QUEUED_VT_COUNT });

                chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
                model.registerThreadsChartSupport(chartSupport);

                chartSupport.setZoomingEnabled(!liveModel);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            add(chartSupport.getChart(), BorderLayout.CENTER);
            chartSupport.updateDetails(new String[] { UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN });
        }
    }
}

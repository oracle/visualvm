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

package com.sun.tools.visualvm.modules.sampler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.sampler.cpu.SampledLivePanel;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot.NoDataAvailableException;
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ApplicationSamplerView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/sampler/resources/profiler.png";  // NOI18N
    private static final int LIVE_UPDATE_RATE = 1200;
    private MasterViewSupport masterViewSupport;
    private CPUSettingsSupport cpuSettingsSupport;
    private MemorySettingsSupport memorySettingsSupport;
    
    public ApplicationSamplerView(Application application) {
        super(application, NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Profiler"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 45, false);    // NOI18N
        cpuSettingsSupport = new CPUSettingsSupport(application);
        memorySettingsSupport = new MemorySettingsSupport(application);
    }
    
    
    protected DataViewComponent createComponent() {
        Application application = (Application)getDataSource();
        ProfilingResultsSupport profilingResultsSupport = new ProfilingResultsSupport();
        masterViewSupport = new MasterViewSupport(application, profilingResultsSupport, cpuSettingsSupport, memorySettingsSupport);
        
        DataViewComponent dvc = new DataViewComponent(
                masterViewSupport.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Profiling_results"), false), DataViewComponent.TOP_LEFT);   // NOI18N
        dvc.addDetailsView(profilingResultsSupport.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Settings"), true), DataViewComponent.TOP_RIGHT);   // NOI18N
        dvc.addDetailsView(cpuSettingsSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(memorySettingsSupport.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
        
        cpuSettingsSupport = null;
        memorySettingsSupport = null;
        
        return dvc;
    }
    
    protected void willBeAdded() {
    }
    
    protected void removed() {
        masterViewSupport.viewRemoved();
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel implements DataRemovedListener<Application>, PropertyChangeListener {
        private static final int INACTIVE = 0;
        private static final int CPU = 1;
        private static final int MEM = 2;
        private static final int TRANS = 3;
        
        private Application application;
        private ProfilingResultsSupport profilingResultsView;
        private CPUSettingsSupport cpuSettingsSupport;
        private MemorySettingsSupport memorySettingsSupport;
        private AttachSettings attachSettings;
        private Timer timer;
        private ThreadMXBean threadBean;
        private StackTraceSnapshotBuilder builder;
        private int lastInstrValue = -1;
        private volatile boolean sampleRunning;
        private final Object updateLock = new Object();
        private TimerTask ttask;
        private int oldState = -1;
        private int state = INACTIVE;
        private boolean internalChange;
        private boolean applicationTerminated;
        private SampledLivePanel cpuView;
        private long lastLiveUpdate;
        
        public MasterViewSupport(final Application application, ProfilingResultsSupport profilingResultsView,
                CPUSettingsSupport cpuSettingsSupport, MemorySettingsSupport memorySettingsSupport) {
            this.application = application;
            this.profilingResultsView = profilingResultsView;
            this.cpuSettingsSupport = cpuSettingsSupport;
            this.memorySettingsSupport = memorySettingsSupport;
            
            initComponents();
            initSettings();
            refreshStatus();
            String timerName = "CPU sampler for "+application.getPid(); // NOI18N
            
            timer = new Timer(timerName);
            
            // TODO: should listen for PROPERTY_AVAILABLE instead of DataSource removal
            application.notifyWhenRemoved(this);
            application.addPropertyChangeListener(Stateful.PROPERTY_STATE, WeakListeners.propertyChange(this,application));
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Profiler"), null, this);    // NOI18N
        }
        
        public synchronized void dataRemoved(Application application) {
            applicationTerminated = true;
            stopSampling();
            lastInstrValue = -1;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    resetControlButtons();
                    disableControlButtons();
                    statusValueLabel.setText(NbBundle.getMessage(ApplicationSamplerView.class, "MSG_application_terminated")); // NOI18N
                    enableSettings();
                }
            });
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            dataRemoved(application);
        }
        
        public void viewRemoved() {
            stopSampling();
            timer.cancel();
        }
        
        private void stopSampling() {
            if (ttask != null) {
                ttask.cancel();
                ttask = null;
                state = INACTIVE;
                cpuView = null;
            }
        }
        
        private void startSampling() {
            ttask = new TT();
            timer.scheduleAtFixedRate(ttask,100,100);
        }
        
        private JComponent getLiveResultsView() {
            cpuView = new SampledLivePanel(builder);
            cpuView.setPreferredSize(new Dimension(1, 1));
            return cpuView;
        }
        
        private void handleCPUProfiling() {
            if (internalChange) return;
            
            if (cpuButton.isSelected())  {
                internalChange = true;
                memoryButton.setSelected(false);
                internalChange = false;
                cpuSettingsSupport.saveSettings();
                disableControlButtons();
                IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() { startProfiling(application, cpuSettingsSupport.getSettings()); }
                });
            }
        }
        
        private void handleMemoryProfiling() {
            if (internalChange) return;
            
            if (memoryButton.isSelected())  {
                internalChange = true;
                cpuButton.setSelected(false);
                internalChange = false;
                memorySettingsSupport.saveSettings();
                if (NetBeansProfiler.getDefaultNB().getProfilingState() == NetBeansProfiler.PROFILING_RUNNING) {
                    IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() {
                            NetBeansProfiler.getDefaultNB().modifyCurrentProfiling(memorySettingsSupport.getSettings());
                        }
                    });
                } else {
                    disableControlButtons();
                    IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() { startProfiling(application, memorySettingsSupport.getSettings()); }
                    });
                }
            }
        }
        
        private void startProfiling(Application application, ProfilingSettings pSettings) {
            if (application.getState() != Stateful.STATE_AVAILABLE) return;
            
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
            if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                threadBean = mxbeans.getThreadMXBean();
                builder = new StackTraceSnapshotBuilder();
                startSampling();
                state = TRANS;
            }
        }
        
        private void handleStopProfiling() {
            if (internalChange) return;
            
            stopSampling();
            disableControlButtons();
            refreshStatus();
            if (builder != null) {
                takeSnapshot();
            }
        }
        
        private synchronized void refreshStatus() {
            if (oldState != state) {
                oldState = state;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        switch (oldState) {
                            case INACTIVE:
                                lastInstrValue = -1;
                                if (!applicationTerminated) {
                                    stopSampling();
                                    statusValueLabel.setText(NbBundle.getMessage(ApplicationSamplerView.class, "MSG_profiling_inactive"));    // NOI18N
                                    resetControlButtons();
                                    RequestProcessor.getDefault().post(new Runnable() {
                                        public void run() {
                                            enableControlButtons();
                                            enableSettings();
                                        }
                                    }, 500); // Wait for the application to finish
                                }
                                break;
                            case TRANS:
                                statusValueLabel.setText(NbBundle.getMessage(ApplicationSamplerView.class, "MSG_refreshing")); // NOI18N
                                disableControlButtons();
                                disableSettings();
                                break;
                            case CPU:
                            case MEM:
                                updateRunningText();
                                enableControlButtons();
                                updateControlButtons();
                                disableSettings();
                                profilingResultsView.setProfilingResultsDisplay(getLiveResultsView());
                                
                                profilingResultsView.revalidate();
                                profilingResultsView.repaint();
                                revalidate();
                                repaint();
                                break;
                        }
                        updateRunningText();
                    }
                });
            } else if (cpuView != null) {
                final long time = System.currentTimeMillis();
                if (time - lastLiveUpdate > LIVE_UPDATE_RATE) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!sampleRunning) {
                                synchronized (updateLock) {
                                lastLiveUpdate = time;
                                cpuView.updateLiveResults();
                                }
                            } else {
                                SwingUtilities.invokeLater(this);
                            }
                        }
                    });
                }
            }
        }
        
        private void updateRunningText() {
            if (state == CPU) {
                if (lastInstrValue != 0)
                    statusValueLabel.setText(MessageFormat.format(NbBundle.getMessage(ApplicationSamplerView.class,
                            "MSG_profiling_running_methods"), new Object[] { 0 })); // NOI18N
                lastInstrValue = 0;
            } else if (state == MEM) {
                // TODO memory
                lastInstrValue = 0;
            }
        }
        
        private void enableSettings() {
            cpuSettingsSupport.setUIEnabled(true);
            memorySettingsSupport.setUIEnabled(true);
        }
        
        private void disableSettings() {
            cpuSettingsSupport.setUIEnabled(false);
            memorySettingsSupport.setUIEnabled(false);
        }
        
        private void resetControlButtons() {
            internalChange = true;
            cpuButton.setSelected(false);
            memoryButton.setSelected(false);
            internalChange = false;
        }
        
        private void updateControlButtons() {
            if (state == CPU && !cpuButton.isSelected()) {
                internalChange = true;
                cpuButton.setSelected(true);
                memoryButton.setSelected(false);
                internalChange = false;
            } else if (state == MEM && !memoryButton.isSelected()) {
                internalChange = true;
                cpuButton.setSelected(false);
                memoryButton.setSelected(true);
                internalChange = false;
            }
        }
        
        private void enableControlButtons() {
            boolean enabled = application.getState() == Stateful.STATE_AVAILABLE;
            cpuButton.setEnabled(enabled);
            memoryButton.setEnabled(enabled);
            stopButton.setEnabled(state == CPU || state == MEM);
        }
        
        private void disableControlButtons() {
            cpuButton.setEnabled(false);
            memoryButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
        
        
        private void initSettings() {
            // Attach settings default
            attachSettings = new AttachSettings();
            attachSettings.setDirect(false);
            attachSettings.setDynamic16(true);
            attachSettings.setPid(application.getPid());
        }
        
        private void takeSnapshot() {
            assert builder != null;
            LoadedSnapshot loadedSnapshot;
            try {
                CPUResultsSnapshot snapshot = builder.createSnapshot(System.currentTimeMillis());
                loadedSnapshot = new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createCPUPreset(), null);
                DataOutputStream dos = new DataOutputStream(new FileOutputStream("/tmp/snapshot"+application.getPid()+"."+ResultsManager.SNAPSHOT_EXTENSION));
                loadedSnapshot.save(dos);
                loadedSnapshot.setSaved(true);
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (NoDataAvailableException ex) {
                ex.printStackTrace();
            }
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            JPanel controlPanel = new JPanel();
            controlPanel.setOpaque(false);
            controlPanel.setLayout(new GridBagLayout());
            controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 3, 0));
            
            GridBagConstraints constraints;
            
            // modeLabel
            modeLabel = new JLabel(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Profile"));    // NOI18N
            modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
            modeLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(4, 8, 0, 0);
            controlPanel.add(modeLabel, constraints);
            
            // cpuButton
            cpuButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Cpu"));    // NOI18N
            cpuButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/modules/sampler/resources/cpu.png", true))); // NOI18N
            cpuButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleCPUProfiling(); }
            });
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(4, 8, 0, 0);
            controlPanel.add(cpuButton, constraints);
            
            // memoryButton
            memoryButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Memory"));  // NOI18N
            memoryButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/modules/sampler/resources/memory.png", true)));   // NOI18N
            memoryButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleMemoryProfiling(); }
            });
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(4, 8, 0, 0);
            controlPanel.add(memoryButton, constraints);
            
            // stopButton
            stopButton = new JButton(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Stop")); // NOI18N
            stopButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/modules/sampler/resources/stop.png", true)));   // NOI18N
            stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { handleStopProfiling(); }
            });
            stopButton.setEnabled(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 4;
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(4, 8, 0, 0);
            controlPanel.add(stopButton, constraints);
            
            // filler
            JPanel filler1 = new JPanel(new BorderLayout());
            filler1.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 5;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            controlPanel.add(filler1, constraints);
            
            // statusLabel
            statusLabel = new JLabel(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Status"));   // NOI18N
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
            statusLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(6, 8, 4, 0);
            controlPanel.add(statusLabel, constraints);
            
            // statusValueLabel
            statusValueLabel = new HTMLLabel() {
                public void setText(String text) {
                    super.setText("<nobr>" + text + "</nobr>");   // NOI18N
                }
                protected void showURL(URL url) {
                }
                
                // NOTE: overriding dimensions prevents UI "jumping" when changing the link
                public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, refLabelHeight); }
                public Dimension getMinimumSize() { return getPreferredSize(); }
                public Dimension getMaximumSize() { return getPreferredSize(); }
            };
            statusValueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 3;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(6, 8, 4, 8);
            controlPanel.add(statusValueLabel, constraints);
            
            // filler
            JPanel filler2 = new JPanel(new BorderLayout());
            filler2.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 3;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            controlPanel.add(filler2, constraints);
            
            Dimension cpuD     = cpuButton.getPreferredSize();
            Dimension memoryD  = memoryButton.getPreferredSize();
            Dimension stopD    = stopButton.getPreferredSize();
            
            Dimension maxD = new Dimension(Math.max(cpuD.width, memoryD.width), Math.max(cpuD.height, memoryD.height));
            maxD = new Dimension(Math.max(maxD.width, stopD.width), Math.max(maxD.height, stopD.height));
            
            cpuButton.setPreferredSize(maxD);
            cpuButton.setMinimumSize(maxD);
            memoryButton.setPreferredSize(maxD);
            memoryButton.setMinimumSize(maxD);
            stopButton.setPreferredSize(maxD);
            stopButton.setMinimumSize(maxD);
            
            setOpaque(false);
            
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(controlPanel, BorderLayout.CENTER);
        }
        
        private JLabel modeLabel;
        private JToggleButton cpuButton;
        private JToggleButton memoryButton;
        private JButton stopButton;
        private JLabel statusLabel;
        private HTMLLabel statusValueLabel;
        private static final int refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        
        private class TT extends TimerTask {
            Set samplingThreads = new HashSet();
            
            public void run() {
                if (sampleRunning) return;
                synchronized (updateLock) {
                    sampleRunning = true;
                    try {
                        ThreadInfo[] infos = threadBean.getThreadInfo(threadBean.getAllThreadIds(),Integer.MAX_VALUE);
                        long timestamp = System.nanoTime();
                        String samplingThreadName = findSamplingThread(infos);
                        if (samplingThreadName != null) {
                            if (samplingThreads.add(samplingThreadName)) {
                                System.out.println("New ignored thread: "+samplingThreadName);
                                builder.setIgnoredThreads(samplingThreads);
                            }
                        }
                        builder.addStacktrace(infos, timestamp);
                        refreshStatus();
                        state = CPU;
                    } catch (Throwable ex) {
                        Exceptions.printStackTrace(ex);
                    } finally {
                        sampleRunning = false;
                    }
                }
            }
            
            private String findSamplingThread(ThreadInfo[] infos) {
//                for (ThreadInfo info : infos) {
//                    if (info.getThreadState() == Thread.State.RUNNABLE) {
//                        StackTraceElement[] stack = info.getStackTrace();
//                        
//                        if (stack.length > 0) {
//                            StackTraceElement topStack = stack[0];
//                            
//                            if (!topStack.isNativeMethod()) {
//                                continue;
//                            }
//                            if (!"sun.management.ThreadImpl".equals(topStack.getClassName())) {  // NOI18N
//                                continue;
//                            }
//                            if ("getThreadInfo0".equals(topStack.getMethodName())) {
//                                return info.getThreadName();
//                            }
//                        }
//                    }
//                }
                return null;
            }
        }
    }
    
    
    private static final class OneWayToggleButton extends JToggleButton {
        
        public OneWayToggleButton(String text) {
            super(text);
        }
        
        protected void processMouseEvent(MouseEvent e) {
            if (!isSelected()) super.processMouseEvent(e);
        }
        
    }
    
}

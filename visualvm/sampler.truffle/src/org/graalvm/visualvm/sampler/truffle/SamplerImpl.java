/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle;

import org.graalvm.visualvm.sampler.truffle.cpu.ThreadInfoProvider;
import org.graalvm.visualvm.sampler.truffle.memory.MemorySettingsSupport;
import org.graalvm.visualvm.sampler.truffle.cpu.CPUSettingsSupport;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
import org.graalvm.visualvm.profiling.presets.PresetSelector;
import org.graalvm.visualvm.profiling.presets.ProfilerPresets;
import org.graalvm.visualvm.profiling.snapshot.ProfilerSnapshot;
import org.graalvm.visualvm.sampler.truffle.cpu.CPUSamplerSupport;
import org.graalvm.visualvm.sampler.truffle.memory.MemorySamplerSupport;
import org.graalvm.visualvm.threaddump.ThreadDumpSupport;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
import org.graalvm.visualvm.uisupport.HTMLLabel;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.ProfilingSettingsPresets;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.sampler.truffle.memory.MemoryHistogramProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class SamplerImpl {

    private static final Logger LOGGER = Logger.getLogger(SamplerImpl.class.getName());
    
    private static final boolean MEMORY_INITIALLY_AVAILABLE = Boolean.getBoolean("visualvm.graalsampler.memory.alwaysEnabled"); // NOI18N

    private static enum State { TERMINATED, INACTIVE, CPU, MEMORY, TRANSITION };

    private Application application;
    private Timer timer;

    private HTMLTextArea summaryArea;
    private String cpuStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Checking_Availability"); // NOI18N
    private String memoryStatus = NbBundle.getMessage(SamplerImpl.class, MEMORY_INITIALLY_AVAILABLE ? "MSG_Memory_experimental2" : "MSG_Memory_experimental1"); // NOI18N
//    private String memoryStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Checking_Availability"); // NOI18N

    private boolean cpuProfilingSupported;
    private AbstractSamplerSupport cpuSampler;
    private CPUSettingsSupport cpuSettings;

    private boolean memoryProfilingSupported = MEMORY_INITIALLY_AVAILABLE;
    private boolean memoryInitializationPending;
//    private boolean memoryProfilingSupported;
    private AbstractSamplerSupport memorySampler;
    private MemorySettingsSupport memorySettings;
    
    private DefaultComboBoxModel selectorModel;
    private List<PresetSelector> allSelectors;

    private DataViewComponent dvc;
    private String currentName;
    private DataViewComponent.DetailsView[] currentViews;

    private State state = State.TRANSITION;


    SamplerImpl(Application application) {
        this.application = application;
        
        cpuSettings = new CPUSettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return SamplerImpl.this.createSelector(presetSynchronizer);
            }
        };
        memorySettings = new MemorySettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return SamplerImpl.this.createSelector(presetSynchronizer);
            }
        };
    }
    
    private PresetSelector createSelector(Runnable presetSynchronizer) {
        if (selectorModel == null) selectorModel = new DefaultComboBoxModel();
        if (allSelectors == null) allSelectors = new ArrayList();
        PresetSelector selector = ProfilerPresets.getInstance().createSelector(
                                  application, selectorModel, allSelectors, presetSynchronizer);
        allSelectors.add(selector);
        return selector;
    }


    DataViewComponent.MasterView getMasterView() {
        initComponents();
        setState(State.INACTIVE);

        final HierarchyListener hl = new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (view.isShowing()) {
                        initializeCpuSampling();
//                        initializeMemorySampling();
                        view.removeHierarchyListener(this);
                    }
                }
            }
        };
        view.addHierarchyListener(hl);

        return new DataViewComponent.MasterView(NbBundle.getMessage(
                   SamplerImpl.class, "LBL_Sampler"), null, view); // NOI18N
    }


    void setDataViewComponent(DataViewComponent dvc) {
        this.dvc = dvc;

        setCurrentViews(NbBundle.getMessage(SamplerImpl.class,
                        "LBL_Information"), createSummaryView()); // NOI18N

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                NbBundle.getMessage(SamplerImpl.class, "LBL_Settings"), // NOI18N
                true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(cpuSettings.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(memorySettings.getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
    }


    void removed() {
        terminate();
    }

    void applicationFinished() {
        terminate();
    }

    private synchronized void terminate() {
        State currentState = getState();

        if (cpuSampler != null) {
            if (State.CPU.equals(currentState)) cpuSampler.stopSampling();
            cpuSampler.terminate();
        }
        if (memorySampler != null) {
            if (State.MEMORY.equals(currentState)) memorySampler.stopSampling();
            memorySampler.terminate();
        }

        setState(State.TERMINATED);
        dvc = null;
    }


    private void setCurrentViews(String name, DataViewComponent.DetailsView[] views) {
        if (dvc == null) return;

        if (currentName == null || !currentName.equals(name)) {
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                    name, false), DataViewComponent.TOP_LEFT);

            currentName = name;
        }

        if (currentViews != views) {
            if (currentViews != null)
                for (DataViewComponent.DetailsView detailsView : currentViews)
                    dvc.removeDetailsView(detailsView);

            if (views != null)
                for (DataViewComponent.DetailsView detailsView : views)
                    dvc.addDetailsView(detailsView, DataViewComponent.TOP_LEFT);

            currentViews = views;
        }
    }

    private synchronized void setState(State state) {
        if (this.state.equals(state)) return;
        this.state = state;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateStatus();
                updateSettings();
                updateButtons();
            }
        });
    }

    private synchronized State getState() {
        return state;
    }


    private void updateStatus() {
        if (statusValueLabel != null) {
            String status = NbBundle.getMessage(SamplerImpl.class,
                            "LBL_Undefined"); // NOI18N

            switch (getState()) {
                case TERMINATED:
                    status = NbBundle.getMessage(SamplerImpl.class,
                             "MSG_Application_terminated"); // NOI18N
                    break;
                case INACTIVE:
                    status = NbBundle.getMessage(SamplerImpl.class,
                             "MSG_Sampling_inactive"); // NOI18N
                    break;
                case CPU:
                    status = NbBundle.getMessage(SamplerImpl.class,
                             "MSG_Cpu_progress"); // NOI18N
                    break;
                case MEMORY:
                    status = NbBundle.getMessage(SamplerImpl.class,
                             "MSG_Memory_progress"); // NOI18N
                    break;
                case TRANSITION:
                    status = NbBundle.getMessage(SamplerImpl.class,
                             "LBL_Refreshing"); // NOI18N
                    break;
            }

            statusValueLabel.setText(status);
        }
    }

    private void updateSettings() {
        if (cpuSettings != null && memorySettings != null) {
            switch (getState()) {
                case INACTIVE:
                case TERMINATED:
                    cpuSettings.setEnabled(true);
                    memorySettings.setEnabled(true);
                    break;
                case CPU:
                case MEMORY:
                case TRANSITION:
                    cpuSettings.setEnabled(false);
                    memorySettings.setEnabled(false);
                    break;
            }
        }
    }

    private void updateButtons() {
        if (cpuButton != null && memoryButton != null && stopButton != null) {
            switch (getState()) {
                case TERMINATED:
                    cpuButton.setSelected(false);
                    cpuButton.setEnabled(false);

                    memoryButton.setSelected(false);
                    memoryButton.setEnabled(false);

                    stopButton.setEnabled(false);

                    break;

                case INACTIVE:
                    cpuButton.setSelected(false);
                    cpuButton.setEnabled(cpuProfilingSupported);

                    memoryButton.setSelected(false);
                    memoryButton.setEnabled(memoryProfilingSupported || memoryInitializationPending);

                    stopButton.setEnabled(false);

                    break;

                case CPU:
                    cpuButton.setSelected(true);
                    cpuButton.setEnabled(true);

                    memoryButton.setSelected(false);
                    memoryButton.setEnabled(memoryProfilingSupported || memoryInitializationPending);

                    stopButton.setEnabled(true);

                    break;

                case MEMORY:
                    cpuButton.setSelected(false);
                    cpuButton.setEnabled(cpuProfilingSupported);

                    memoryButton.setSelected(true);
                    memoryButton.setEnabled(true);

                    stopButton.setEnabled(true);

                    break;

                case TRANSITION:
                    cpuButton.setEnabled(false);

                    memoryButton.setEnabled(false);

                    stopButton.setEnabled(false);

                    break;
            }
        }
    }


    private void handleCPUProfiling() {
        if (!cpuSettings.settingsValid()) {
            cpuButton.setSelected(false);
            if (dvc != null) cpuSettings.showSettings(dvc);
            ProfilerDialogs.displayError(NbBundle.getMessage(SamplerImpl.class, "MSG_Incorrect_CPU_settings")); // NOI18N
            return;
        }
        
        State currentState = getState();
        if (currentState.equals(State.CPU) ||
           currentState.equals(State.TERMINATED) ||
           currentState.equals(State.TRANSITION)) return;
        setState(State.TRANSITION);
        
        final Runnable sessionStarter = new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setCurrentViews(NbBundle.getMessage(SamplerImpl.class,
                                        "LBL_Cpu_samples"), cpuSampler.getDetailsView()); // NOI18N
                        VisualVM.getInstance().runTask(new Runnable() {
                            public void run() {
                                cpuSettings.saveSettings();
                                setState(cpuSampler.startSampling(
                                         cpuSettings.getSettings(),
                                         cpuSettings.getSamplingRate(),
                                         cpuSettings.getRefreshRate()) ?
                                         State.CPU : State.INACTIVE);
                            }
                        });
                    }
                });
            }
        };

        if (currentState.equals(State.MEMORY)) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    memorySampler.stopSampling();
                    sessionStarter.run();
                }
            });
        } else {
            sessionStarter.run();
        }
    }

    private void handleMemoryProfiling() {
        Runnable memoryProfilingHandler = new Runnable() {
            public void run() {
                if (!memorySettings.settingsValid()) {
                    memoryButton.setSelected(false);
                    if (dvc != null) memorySettings.showSettings(dvc);
                    ProfilerDialogs.displayError(NbBundle.getMessage(SamplerImpl.class, "MSG_Incorrect_Memory_settings")); // NOI18N
                    return;
                }

                State currentState = getState();
                if (currentState.equals(State.MEMORY) ||
                   currentState.equals(State.TERMINATED) ||
                   currentState.equals(State.TRANSITION)) return;
                setState(State.TRANSITION);

                final Runnable sessionStarter = new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                setCurrentViews(NbBundle.getMessage(SamplerImpl.class,
                                                "LBL_Memory_samples"), memorySampler.getDetailsView()); // NOI18N
                                VisualVM.getInstance().runTask(new Runnable() {
                                    public void run() {
                                        memorySettings.saveSettings();
                                        setState(memorySampler.startSampling(
                                                 memorySettings.getSettings(),
                                                 memorySettings.getSamplingRate(),
                                                 memorySettings.getRefreshRate()) ?
                                                 State.MEMORY : State.INACTIVE);
                                    }
                                });
                            }
                        });
                    }
                };

                if (currentState.equals(State.CPU)) {
                    VisualVM.getInstance().runTask(new Runnable() {
                        public void run() {
                            cpuSampler.stopSampling();
                            sessionStarter.run();
                        }
                    });
                } else {
                    sessionStarter.run();
                }
            }
        };
        
        if (memorySampler == null) initializeMemorySampling(memoryProfilingHandler);
        else if (memoryProfilingSupported) memoryProfilingHandler.run();
    }

    private void handleStopProfiling() {
        State currentState = getState();
        if (currentState.equals(State.INACTIVE) ||
           currentState.equals(State.TERMINATED) ||
           currentState.equals(State.TRANSITION)) return;
        setState(State.TRANSITION);

        if (currentState.equals(State.CPU)) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    cpuSampler.stopSampling();
                    setState(State.INACTIVE);
                }
            });
        } else if (currentState.equals(State.MEMORY)) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    memorySampler.stopSampling();
                    setState(State.INACTIVE);
                }
            });
        }
    }


    private void initializeCpuSampling() {
        final String mode = cpuSettings.getMode();
        final boolean splitCompiledInlined = cpuSettings.isSplitCompiledInlined();
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                ThreadInfoProvider ti = new ThreadInfoProvider(application, mode, splitCompiledInlined);
                final String status = ti.getStatus();
                final boolean modeAvailable = ti.isModeVailable();
                
                if (status != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = status;
                            refreshSummary();
                        }
                    });
                    return;
                }

                CPUSamplerSupport.SnapshotDumper snapshotDumper = new CPUSamplerSupport.SnapshotDumper() {
                    public void takeSnapshot(final boolean openView) {
                        VisualVM.getInstance().runTask(new Runnable() {
                            public void run() {
                                LoadedSnapshot ls = null;
                                try {
                                    ls = takeNPSSnapshot(application.getStorage().getDirectory());
                                } catch (CPUResultsSnapshot.NoDataAvailableException e) {
                                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(NbBundle.getMessage(
                                            SamplerImpl.class, "MSG_No_save_data_cpu"), NotifyDescriptor.WARNING_MESSAGE)); // NOI18N
                                } catch (Throwable t) {
                                    LOGGER.log(Level.WARNING, "Failed to save profiler snapshot for " + application, t); // NOI18N
                                }
                                if (ls != null) {
                                    final ProfilerSnapshot ps = ProfilerSnapshot.createSnapshot(ls.getFile(), application);
                                    application.getRepository().addDataSource(ps);
                                    if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
                                        public void run() {
                                            DataSourceWindowManager.sharedInstance().openDataSource(ps);
                                        }
                                    });
                                }
                            }
                        });
                    }
                };

                final ThreadDumpSupport tds = ThreadDumpSupport.getInstance();
                final String noThreadDump = tds.supportsThreadDump(application) ? null : NbBundle.getMessage(
                                            SamplerImpl.class, "MSG_Thread_dump_unsupported"); // NOI18N

                CPUSamplerSupport.ThreadDumper threadDumper = noThreadDump != null ? null :
                    new CPUSamplerSupport.ThreadDumper() {
                        public void takeThreadDump(boolean openView) {
                            tds.takeThreadDump(application, openView);
                        }
                    };
                    
                cpuSampler = new CPUSamplerSupport(application, ti, snapshotDumper, threadDumper) {
                    @Override
                    public boolean startSampling(ProfilingSettings settings, int samplingRate, int refreshRate) {
                        setOptions(cpuSettings.getMode(), cpuSettings.isSplitCompiledInlined());
                        return super.startSampling(settings, samplingRate, refreshRate);
                    }

                    protected Timer getTimer() { return SamplerImpl.this.getTimer(); }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String avail = NbBundle.getMessage(SamplerImpl.class,
                                                           "MSG_Available"); // NOI18N
                        if (noThreadDump != null) {
                            String[] msgs = new String[2];
                            int i = 0;
                            if (noThreadDump != null) {
                                msgs[i++] = noThreadDump;
                            }
                            if (i == 1) {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details", msgs[0]); // NOI18N
                            } else if (i == 2) {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details2", msgs[0], msgs[1]); // NOI18N
                            }
                        }
                        cpuStatus = avail + " " + NbBundle.getMessage(SamplerImpl.class, "MSG_Press_cpu"); // NOI18N
                        cpuProfilingSupported = true;
                        if (!modeAvailable) cpuSettings.enableMode(modeAvailable);
                        refreshSummary();
                        updateButtons();
                        updateSettings();
                    }
                });
            }
        });
    }

    private void initializeMemorySampling(Runnable onSuccess) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                MemoryHistogramProvider histogramProvider = new MemoryHistogramProvider(application);
                final String status = histogramProvider.getStatus();

                if (status != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryProfilingSupported = false; // may be initially true (visualvm.graalsampler.memory.alwaysEnabled)
                            memoryInitializationPending = false;
                            
                            handleStopProfiling();
                            
//                            memoryStatus = status;
//                            refreshSummary();
                            updateButtons();
                            updateSettings();
                            
                            ProfilerDialogs.displayError(status, NbBundle.getMessage(SamplerImpl.class, "CAP_Memory_error"), null); // NOI18N
                        }
                    });
                    return;
                }

                MemoryMXBean memoryBean = null;
                JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
                if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                    JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                    if (mxbeans != null) {
                        memoryBean = mxbeans.getMemoryMXBean();
                    }
                }
                final String noPerformGC = memoryBean == null ? NbBundle.getMessage(
                        SamplerImpl.class, "MSG_Gc_unsupported") : null; // NOI18N

                final HeapDumpSupport hds = HeapDumpSupport.getInstance();
                final boolean local = application.isLocalApplication();
                boolean supportsHD = local ? hds.supportsHeapDump(application) :
                                     hds.supportsRemoteHeapDump(application);
                final String noHeapDump = supportsHD ? null : NbBundle.getMessage(
                        SamplerImpl.class, "MSG_HeapDump_unsupported"); // NOI18N

                MemorySamplerSupport.SnapshotDumper snapshotDumper = new MemorySamplerSupport.SnapshotDumper() {
                    public void takeSnapshot(final boolean openView) {
                        final MemorySamplerSupport.SnapshotDumper dumper = this; 
                        VisualVM.getInstance().runTask(new Runnable() {
                            public void run() {
                                LoadedSnapshot ls = null;
                                DataOutputStream dos = null;
                                try {
                                    long time = System.currentTimeMillis();
                                    SampledMemoryResultsSnapshot snapshot = dumper.createSnapshot(time);
                                    if (snapshot == null) {
                                        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                                                NbBundle.getMessage(SamplerImpl.class, "MSG_No_save_data_memory"), // NOI18N
                                                NotifyDescriptor.WARNING_MESSAGE));
                                    } else {
                                        ls = new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createMemoryPreset(), null, null);
                                        File file = Utils.getUniqueFile(application.getStorage().getDirectory(),
                                                                        ResultsManager.getDefault().getDefaultSnapshotFileName(ls),
                                                                        "." + ResultsManager.SNAPSHOT_EXTENSION); // NOI18N
                                        dos = new DataOutputStream(new FileOutputStream(file));
                                        ls.save(dos);
                                        ls.setFile(file);
                                        ls.setSaved(true);
                                    }
                                } catch (Throwable t) {
                                    LOGGER.log(Level.WARNING, "Failed to save profiler snapshot for " + application, t); // NOI18N
                                } finally {
                                    try {
                                        if (dos != null) dos.close();
                                    } catch (IOException e) {
                                        LOGGER.log(Level.WARNING, "Problem closing output stream for  " + dos, e); // NOI18N
                                    }
                                }
                                if (ls != null) {
                                    final ProfilerSnapshot ps = ProfilerSnapshot.createSnapshot(ls.getFile(), application);
                                    application.getRepository().addDataSource(ps);
                                    if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
                                        public void run() {
                                            DataSourceWindowManager.sharedInstance().openDataSource(ps);
                                        }
                                    });
                                }
                            }
                        });
                    }
                };
                MemorySamplerSupport.HeapDumper heapDumper = noHeapDump != null ? null :
                    new MemorySamplerSupport.HeapDumper() {
                        public void takeHeapDump(boolean openView) {
                            if (local) hds.takeHeapDump(application, openView);
                            else hds.takeRemoteHeapDump(application, null, openView);
                        }
                    };
                memorySampler = new MemorySamplerSupport(application, histogramProvider, memoryBean, snapshotDumper, heapDumper) {
                    protected Timer getTimer() { return SamplerImpl.this.getTimer(); }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        int i = 0;
                        
                        String avail = NbBundle.getMessage(SamplerImpl.class,
                                                           "MSG_Available"); // NOI18N
                        if (noPerformGC != null || noHeapDump != null) {
                            String[] msgs = new String[3];
//                            int i = 0;
                            if (noHeapDump != null) {
                                msgs[i++] = noHeapDump;
                            }
                            if (noPerformGC != null) {
                                msgs[i++] = noPerformGC;
                            }
                            if (i == 1) {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details", msgs[0]); // NOI18N
                            } else if (i == 2) {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details2", msgs[0], msgs[1]); // NOI18N
//                            } else {
//                                avail = NbBundle.getMessage(SamplerImpl.class,
//                                        "MSG_Available_details3", msgs[0], msgs[1], msgs[2]); // NOI18N
                            }
                        }
//                        memoryStatus = avail + " " + NbBundle.getMessage( // NOI18N
//                                SamplerImpl.class, "MSG_Press_mem"); // NOI18N
                        memoryProfilingSupported = true;
                        memoryInitializationPending = false;
//                        refreshSummary();
                        updateButtons();
                        updateSettings();
                        
                        if (i > 0) ProfilerDialogs.displayWarningDNSA(avail, NbBundle.getMessage(SamplerImpl.class, "CAP_Memory_warning"), null, SamplerImpl.class.getName(), false); // NOI18N
                        
                        onSuccess.run();
                    }
                });
            }
        });
    }

    private synchronized Timer getTimer() {
        if (timer == null)
            timer = new Timer("Sampler timer for " + DataSourceDescriptorFactory. // NOI18N
                              getDescriptor(application).getName());
        return timer;
    }

    private DataViewComponent.DetailsView[] createSummaryView() {
        summaryArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        memoryButton.setEnabled(true);
                        memoryInitializationPending = true;
                        memoryStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Memory_experimental2"); // NOI18N
                        refreshSummary();
                    }
                });
            }
        };
        summaryArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        refreshSummary();

        return new DataViewComponent.DetailsView[] {
                        new DataViewComponent.DetailsView(NbBundle.getMessage(
                        SamplerImpl.class, "LBL_Summary"), null, 10, // NOI18N
                        new ScrollableContainer(summaryArea), null) };
    }

    private void refreshSummary() {
        StringBuilder builder = new StringBuilder();

        addCpuHeader(builder);
        builder.append(cpuStatus);

        addMemoryHeader(builder);
        builder.append(memoryStatus);

        int selStart = summaryArea.getSelectionStart();
        int selEnd = summaryArea.getSelectionEnd();
        summaryArea.setText(builder.toString());
        summaryArea.select(selStart, selEnd);
    }

    private static void addCpuHeader(StringBuilder builder) {
        builder.append(NbBundle.getMessage(SamplerImpl.class, "LBL_Cpu_sampling")); // NOI18N
    }

    private static void addMemoryHeader(StringBuilder builder) {
        builder.append(NbBundle.getMessage(SamplerImpl.class, "LBL_Memory_sampling")); // NOI18N
    }

    private void initComponents() {
        view = new JPanel(new GridBagLayout());
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(11, 5, 8, 5));

        GridBagConstraints constraints;

        // modeLabel
        modeLabel = new JLabel(NbBundle.getMessage(SamplerImpl.class, "LBL_Profile")); // NOI18N
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
        Dimension d = modeLabel.getPreferredSize();
        modeLabel.setText(NbBundle.getMessage(SamplerImpl.class, "LBL_Sample")); // NOI18N
        d.width = Math.max(d.width, modeLabel.getPreferredSize().width);
        modeLabel.setPreferredSize(d);
        modeLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 8, 0, 0);
        view.add(modeLabel, constraints);

        // cpuButton
        cpuButton = new OneWayToggleButton(NbBundle.getMessage(SamplerImpl.class, "LBL_Cpu")); // NOI18N
        cpuButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/sampler/resources/cpu.png", true))); // NOI18N
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
        view.add(cpuButton, constraints);

        // memoryButton
        memoryButton = new OneWayToggleButton(NbBundle.getMessage(SamplerImpl.class, "LBL_Memory")); // NOI18N
        memoryButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/sampler/resources/memory.png", true))); // NOI18N
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
        view.add(memoryButton, constraints);

        // stopButton
        stopButton = new JButton(NbBundle.getMessage(SamplerImpl.class, "LBL_Stop")); // NOI18N
        stopButton.setIcon(new ImageIcon(ImageUtilities.loadImage("org/graalvm/visualvm/sampler/resources/stop.png", true))); // NOI18N
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { handleStopProfiling(); }
        });
        stopButton.setEnabled(false);
        stopButton.setDefaultCapable(false); // Button size
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 8, 0, 0);
        view.add(stopButton, constraints);

        // filler1
        constraints = new GridBagConstraints();
        constraints.gridx = 5;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        view.add(Spacer.create(), constraints);

        // statusLabel
        statusLabel = new JLabel(NbBundle.getMessage(SamplerImpl.class, "LBL_Status")); // NOI18N
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(6, 8, 4, 0);
        view.add(statusLabel, constraints);

        // statusValueLabel
        statusValueLabel = new HTMLLabel() {
            public void setText(String text) {super.setText("<nobr>" + text + "</nobr>"); } // NOI18N
            protected void showURL(URL url) {}

            // NOTE: overriding dimensions prevents UI "jumping" when changing the link
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.height = getRefLabelHeight();
                return dim;
            }
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        statusValueLabel.setOpaque(false);
        statusValueLabel.setFocusable(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(6, 8, 4, 8);
        view.add(statusValueLabel, constraints);

        // filler2
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        view.add(Spacer.create(), constraints);

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
    }

    private JPanel view;
    private JLabel modeLabel;
    private JToggleButton cpuButton;
    private JToggleButton memoryButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private HTMLLabel statusValueLabel;

    private static int refLabelHeight = -1;
    private static int getRefLabelHeight() {
        if (refLabelHeight == -1)
            refLabelHeight = new HTMLLabel("X").getPreferredSize().height; // NOI18N
        return refLabelHeight;
    }


    private static final class OneWayToggleButton extends JToggleButton {

        OneWayToggleButton(String text) {
            super(text);
        }

        protected void processMouseEvent(MouseEvent e) {
            if (!isSelected()) super.processMouseEvent(e);
        }

        protected void processKeyEvent(KeyEvent e) {
            if (!isSelected()) super.processKeyEvent(e);
        }

    }

}

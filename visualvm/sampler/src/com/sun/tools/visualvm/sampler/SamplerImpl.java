/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.sampler;

import com.sun.tools.visualvm.sampler.cpu.ThreadInfoProvider;
import com.sun.tools.visualvm.sampler.cpu.ThreadsCPU;
import com.sun.tools.visualvm.sampler.memory.MemorySettingsSupport;
import com.sun.tools.visualvm.sampler.cpu.CPUSettingsSupport;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.HeapHistogram;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import com.sun.tools.visualvm.core.ui.components.Spacer;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.profiling.presets.PresetSelector;
import com.sun.tools.visualvm.profiling.presets.ProfilerPresets;
import com.sun.tools.visualvm.profiling.snapshot.ProfilerSnapshot;
import com.sun.tools.visualvm.sampler.cpu.CPUSamplerSupport;
import com.sun.tools.visualvm.sampler.memory.MemorySamplerSupport;
import com.sun.tools.visualvm.sampler.memory.ThreadsMemory;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import com.sun.tools.visualvm.uisupport.HTMLLabel;
import com.sun.tools.visualvm.uisupport.HTMLTextArea;
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
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class SamplerImpl {

    private static final Logger LOGGER = Logger.getLogger(SamplerImpl.class.getName());

    private static enum State { TERMINATED, INACTIVE, CPU, MEMORY, TRANSITION };

    private Application application;
    private Timer timer;

    private HTMLTextArea summaryArea;
    private String cpuStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Checking_Availability"); // NOI18N
    private String memoryStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Checking_Availability"); // NOI18N

    private boolean cpuProfilingSupported;
    private AbstractSamplerSupport cpuSampler;
    private CPUSettingsSupport cpuSettings;

    private boolean memoryProfilingSupported;
    private AbstractSamplerSupport memorySampler;
    private MemorySettingsSupport memorySettings;
    
    private PresetSelector refSelector;

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
        PresetSelector selector = ProfilerPresets.getInstance().createSelector(
                                  application, refSelector, presetSynchronizer);
        if (refSelector == null) refSelector = selector; else refSelector = null;
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
                        initializeMemorySampling();
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
                    memoryButton.setEnabled(memoryProfilingSupported);

                    stopButton.setEnabled(false);

                    break;

                case CPU:
                    cpuButton.setSelected(true);
                    cpuButton.setEnabled(true);

                    memoryButton.setSelected(false);
                    memoryButton.setEnabled(memoryProfilingSupported);

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
                        RequestProcessor.getDefault().post(new Runnable() {
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
            RequestProcessor.getDefault().post(new Runnable() {
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
                        RequestProcessor.getDefault().post(new Runnable() {
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
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    cpuSampler.stopSampling();
                    sessionStarter.run();
                }
            });
        } else {
            sessionStarter.run();
        }
    }

    private void handleStopProfiling() {
        State currentState = getState();
        if (currentState.equals(State.INACTIVE) ||
           currentState.equals(State.TERMINATED) ||
           currentState.equals(State.TRANSITION)) return;
        setState(State.TRANSITION);

        if (currentState.equals(State.CPU)) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    cpuSampler.stopSampling();
                    setState(State.INACTIVE);
                }
            });
        } else if (currentState.equals(State.MEMORY)) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    memorySampler.stopSampling();
                    setState(State.INACTIVE);
                }
            });
        }
    }


    private void initializeCpuSampling() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ThreadInfoProvider ti = new ThreadInfoProvider(application);
                final String status = ti.getStatus();
                ThreadsCPU tcpu;
                
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
                        RequestProcessor.getDefault().post(new Runnable() {
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
                tcpu = new ThreadsCPU(ti.getThreadMXBean(), JmxModelFactory.getJmxModelFor(application).getMBeanServerConnection());
                try {
                    tcpu.getThreadsCPUInfo();
                } catch (Exception ex) {
                    tcpu = null;
                }

                final ThreadDumpSupport tds = ThreadDumpSupport.getInstance();
                final String noThreadDump = tds.supportsThreadDump(application) ? null : NbBundle.getMessage(
                                            SamplerImpl.class, "MSG_Thread_dump_unsupported"); // NOI18N
                final String noThreadCPU =  tcpu != null ? null : NbBundle.getMessage(
                                            SamplerImpl.class, "MSG_ThreadCPU_unsupported"); // NOI18N

                CPUSamplerSupport.ThreadDumper threadDumper = noThreadDump != null ? null :
                    new CPUSamplerSupport.ThreadDumper() {
                        public void takeThreadDump(boolean openView) {
                            tds.takeThreadDump(application, openView);
                        }
                    };
                    
                cpuSampler = new CPUSamplerSupport(ti, tcpu, snapshotDumper, threadDumper) {
                    protected Timer getTimer() { return SamplerImpl.this.getTimer(); }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String avail = NbBundle.getMessage(SamplerImpl.class,
                                                           "MSG_Available"); // NOI18N
                        if (noThreadDump != null || noThreadCPU != null) {
                            String[] msgs = new String[2];
                            int i = 0;
                            if (noThreadDump != null) {
                                msgs[i++] = noThreadDump;
                            }
                            if (noThreadCPU != null) {
                                msgs[i++] = noThreadCPU;
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
                        refreshSummary();
                        updateButtons();
                        updateSettings();
                    }
                });
            }
        });
    }

    private void initializeMemorySampling() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (application.getState() != Stateful.STATE_AVAILABLE) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                    "MSG_Unavailable"); // NOI18N
                            refreshSummary();
                        }
                    });
                    return;
                }
                final Jvm jvm = JvmFactory.getJVMFor(application);
                boolean hasPermGenHisto;
                try {
                    HeapHistogram histogram = jvm.takeHeapHistogram();
                    if (histogram == null) {
                        if (!application.isLocalApplication()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                            "MSG_Unavailable_remote"); // NOI18N
                                    refreshSummary();
                                }
                            });
                            return;
                        }
                        if (!JvmFactory.getJVMFor(application).isAttachable()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                            "MSG_Unavailable_connect_jdk"); // NOI18N
                                    refreshSummary();
                                }
                            });
                            return;
                        }
                        final AttachModel attachModel = AttachModelFactory.getAttachFor(application);
                        if (attachModel == null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                            "MSG_Unavailable_connect_log"); // NOI18N
                                    refreshSummary();
                                }
                            });
                            LOGGER.log(Level.WARNING, "AttachModelFactory.getAttachFor(application) returns null for " + application); // NOI18N
                            return;
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                    "MSG_Unavailable_read_log"); // NOI18N
                                refreshSummary();
                            }
                        });
                        LOGGER.log(Level.WARNING, "attachModel.takeHeapHistogram() returns null for " + application); // NOI18N
                        return;
                    }
                    hasPermGenHisto = !histogram.getPermGenHistogram().isEmpty();
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                    "MSG_Unavailable_read_log"); // NOI18N
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "attachModel.takeHeapHistogram() throws Throwable for " + application, t); // NOI18N
                    return;
                }

                MemoryMXBean memoryBean = null;
                ThreadsMemory threadsMemory = null;
                JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
                if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                    JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                    if (mxbeans != null) {
                        memoryBean = mxbeans.getMemoryMXBean();
                        try {
                            threadsMemory = new ThreadsMemory(mxbeans.getThreadMXBean(),jmxModel.getMBeanServerConnection());
                            threadsMemory.getThreadsMemoryInfo();
                        } catch (Exception ex) {
                            threadsMemory = null;
                        }
                    }
                }
                final String noPerformGC = memoryBean == null ? NbBundle.getMessage(
                        SamplerImpl.class, "MSG_Gc_unsupported") : null; // NOI18N
                final String noThreadMem = threadsMemory == null ? NbBundle.getMessage(
                        SamplerImpl.class, "MSG_ThreadMemory_unsupported") : null; // NOI18N

                final HeapDumpSupport hds = HeapDumpSupport.getInstance();
                final boolean local = application.isLocalApplication();
                boolean supportsHD = local ? hds.supportsHeapDump(application) :
                                     hds.supportsRemoteHeapDump(application);
                final String noHeapDump = supportsHD ? null : NbBundle.getMessage(
                        SamplerImpl.class, "MSG_HeapDump_unsupported"); // NOI18N

                MemorySamplerSupport.SnapshotDumper snapshotDumper = new MemorySamplerSupport.SnapshotDumper() {
                    public void takeSnapshot(final boolean openView) {
                        final MemorySamplerSupport.SnapshotDumper dumper = this; 
                        RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                LoadedSnapshot ls = null;
                                DataOutputStream dos = null;
                                try {
                                    long time = System.currentTimeMillis();
                                    AllocMemoryResultsSnapshot snapshot = dumper.createSnapshot(time);
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
                memorySampler = new MemorySamplerSupport(jvm, hasPermGenHisto, threadsMemory, memoryBean, snapshotDumper, heapDumper) {
                    protected Timer getTimer() { return SamplerImpl.this.getTimer(); }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String avail = NbBundle.getMessage(SamplerImpl.class,
                                                           "MSG_Available"); // NOI18N
                        if (noPerformGC != null || noHeapDump != null || noThreadMem != null) {
                            String[] msgs = new String[3];
                            int i = 0;
                            if (noHeapDump != null) {
                                msgs[i++] = noHeapDump;
                            }
                            if (noPerformGC != null) {
                                msgs[i++] = noPerformGC;
                            }
                            if (noThreadMem != null) {
                                msgs[i++] = noThreadMem;
                            }
                            if (i == 1) {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details", msgs[0]); // NOI18N
                            } else if (i == 2) {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details2", msgs[0], msgs[1]); // NOI18N
                            } else {
                                avail = NbBundle.getMessage(SamplerImpl.class,
                                        "MSG_Available_details3", msgs[0], msgs[1], msgs[2]); // NOI18N
                            }
                        }
                        memoryStatus = avail + " " + NbBundle.getMessage( // NOI18N
                                SamplerImpl.class, "MSG_Press_mem"); // NOI18N
                        memoryProfilingSupported = true;
                        refreshSummary();
                        updateButtons();
                        updateSettings();
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
        summaryArea = new HTMLTextArea();
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
        cpuButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/sampler/resources/cpu.png", true))); // NOI18N
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
        memoryButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/sampler/resources/memory.png", true))); // NOI18N
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
        stopButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/sampler/resources/stop.png", true))); // NOI18N
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

        public OneWayToggleButton(String text) {
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

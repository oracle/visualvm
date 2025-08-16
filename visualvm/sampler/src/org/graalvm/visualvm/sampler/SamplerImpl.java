/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler;

import org.graalvm.visualvm.sampler.cpu.ThreadInfoProvider;
import org.graalvm.visualvm.sampler.cpu.ThreadsCPU;
import org.graalvm.visualvm.sampler.memory.MemorySettingsSupport;
import org.graalvm.visualvm.sampler.cpu.CPUSettingsSupport;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.Spacer;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
import org.graalvm.visualvm.profiling.presets.PresetSelector;
import org.graalvm.visualvm.profiling.presets.ProfilerPresets;
import org.graalvm.visualvm.profiling.snapshot.ProfilerSnapshot;
import org.graalvm.visualvm.sampler.cpu.CPUSamplerSupport;
import org.graalvm.visualvm.sampler.memory.MemorySamplerSupport;
import org.graalvm.visualvm.sampler.memory.ThreadsMemory;
import org.graalvm.visualvm.threaddump.ThreadDumpSupport;
import org.graalvm.visualvm.tools.attach.AttachModel;
import org.graalvm.visualvm.tools.attach.AttachModelFactory;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import org.graalvm.visualvm.lib.common.ProfilingSettingsPresets;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.profiling.presets.ProfilerPreset;
import org.netbeans.api.options.OptionsDisplayer;
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
    
    private static final String OPTIONS_HANDLE = "ProfilerOptions"; // NOI18N

    private Application application;
    private Timer timer;

    private HTMLTextArea summaryArea;
    private String cpuStatus = NbBundle.getMessage(SamplerImpl.class, SamplerInitialization.getInstance().isAutomatic() ? "MSG_Checking_Availability" : "MSG_Not_Initialized_Cpu"); // NOI18N
    private String memoryStatus = NbBundle.getMessage(SamplerImpl.class, SamplerInitialization.getInstance().isAutomatic() ? "MSG_Checking_Availability" : "MSG_Not_Initialized_Memory"); // NOI18N

    private Boolean cpuProfilingSupported;
    private AbstractSamplerSupport cpuSampler;
    private CPUSettingsSupport cpuSettings;

    private Boolean memoryProfilingSupported;
    private AbstractSamplerSupport memorySampler;
    private MemorySettingsSupport memorySettings;
    
    private DefaultComboBoxModel<ProfilerPreset> selectorModel;
    private List<PresetSelector> allSelectors;

    private DataViewComponent dvc;
    private String currentName;
    private DataViewComponent.DetailsView[] currentViews;
    
    private DataViewComponent.DetailsView[] summaryView;

    private State state = State.TRANSITION;
    private SamplerArguments.Request startRequest;
    private SamplerParameters settingsRequest;


    SamplerImpl(Application application, SamplerArguments.Request startRequest, SamplerParameters settingsRequest) {
        this.application = application;
        this.startRequest = startRequest;
        this.settingsRequest = settingsRequest;
        
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
    
    
    void startCPU(SamplerParameters parameters) {
        if (parameters != null && !parameters.isEmpty()) cpuSettings.setSettings(parameters);
        if (cpuProfilingSupported == null) startRequest = SamplerArguments.Request.CPU; // likely not initialized yet, perform lazily
        else if (cpuButton != null && cpuButton.isEnabled() && !cpuButton.isSelected()) cpuButton.doClick();
    }
    
    void startMemory(SamplerParameters parameters) {
        if (parameters != null && !parameters.isEmpty()) memorySettings.setSettings(parameters);
        if (memoryProfilingSupported == null) startRequest = SamplerArguments.Request.MEMORY; // likely not initialized yet, perform lazily
        else if (memoryButton != null && memoryButton.isEnabled() && !memoryButton.isSelected()) memoryButton.doClick();
    }
    
    void takeSnapshot(boolean openView) {
        if (cpuSampler != null && State.CPU.equals(getState())) {
            cpuSampler.takeSnapshot(openView);
        } else if (memorySampler != null && State.MEMORY.equals(getState())) {
            memorySampler.takeSnapshot(openView);
        }
    }
    
    void stop() {
        if (stopButton != null && stopButton.isEnabled()) stopButton.doClick();
    }
    
    
    private PresetSelector createSelector(Runnable presetSynchronizer) {
        if (selectorModel == null) selectorModel = new DefaultComboBoxModel<>();
        if (allSelectors == null) allSelectors = new ArrayList<>();
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
                        boolean automatic = SamplerInitialization.getInstance().isAutomatic();
                        if (automatic || SamplerArguments.Request.CPU.equals(startRequest)) {
                            initializeCpuSampling();
                        }
                        if (automatic || SamplerArguments.Request.MEMORY.equals(startRequest)) {
                            initializeMemorySampling();
                        }
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
                        "LBL_Information"), getSummaryView()); // NOI18N

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
                    cpuButton.setEnabled(buttonEnabled(cpuProfilingSupported));

                    memoryButton.setSelected(false);
                    memoryButton.setEnabled(buttonEnabled(memoryProfilingSupported));

                    stopButton.setEnabled(false);

                    break;

                case CPU:
                    cpuButton.setSelected(true);
                    cpuButton.setEnabled(true);

                    memoryButton.setSelected(false);
                    memoryButton.setEnabled(buttonEnabled(memoryProfilingSupported));

                    stopButton.setEnabled(true);

                    break;

                case MEMORY:
                    cpuButton.setSelected(false);
                    cpuButton.setEnabled(buttonEnabled(cpuProfilingSupported));

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
    
    private boolean buttonEnabled(Boolean profilingSupported) {
        if (profilingSupported != null) {
            return profilingSupported.booleanValue();
        } else {
            return !SamplerInitialization.getInstance().isAutomatic();
        }
    }


    private void handleCPUProfiling() {
        State currentState = getState();
        if (currentState.equals(State.CPU) ||
           currentState.equals(State.TERMINATED) ||
           currentState.equals(State.TRANSITION)) return;
        
        final RequestProcessor synchronousExecutor = new RequestProcessor("Sampler Worker", 1);
        
        if (currentState.equals(State.MEMORY)) {
            synchronousExecutor.post(new Runnable() {
                public void run() {
                    memorySampler.stopSampling();
                    setState(State.INACTIVE);
                }
            });
        }
        
        if (cpuProfilingSupported == null) {
            cpuStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Checking_Availability");
            updateStatus();
            startRequest = SamplerArguments.Request.CPU;
            initializeCpuSampling();
            return;
        }
        
        if (!cpuSettings.settingsValid()) {
            cpuButton.setSelected(false);
            if (dvc != null) cpuSettings.showSettings(dvc);
            ProfilerDialogs.displayError(NbBundle.getMessage(SamplerImpl.class, "MSG_Incorrect_CPU_settings")); // NOI18N
            return;
        }
        
        setState(State.TRANSITION);
        
        synchronousExecutor.post(new Runnable() {
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
        });
    }

    private void handleMemoryProfiling() {
        State currentState = getState();
        if (currentState.equals(State.MEMORY) ||
           currentState.equals(State.TERMINATED) ||
           currentState.equals(State.TRANSITION)) return;
        
        final RequestProcessor synchronousExecutor = new RequestProcessor("Sampler Worker", 1);
        
        if (currentState.equals(State.CPU)) {
            synchronousExecutor.post(new Runnable() {
                public void run() {
                    cpuSampler.stopSampling();
                    setState(State.INACTIVE);
                }
            });
        }
        
        if (memoryProfilingSupported == null) {
            memoryStatus = NbBundle.getMessage(SamplerImpl.class, "MSG_Checking_Availability");
            updateStatus();
            startRequest = SamplerArguments.Request.MEMORY;
            initializeMemorySampling();
            return;
        }
        
        if (!memorySettings.settingsValid()) {
            memoryButton.setSelected(false);
            if (dvc != null) memorySettings.showSettings(dvc);
            ProfilerDialogs.displayError(NbBundle.getMessage(SamplerImpl.class, "MSG_Incorrect_Memory_settings")); // NOI18N
            return;
        }
        
        setState(State.TRANSITION);

        synchronousExecutor.post(new Runnable() {
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
        });
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
        cpuProfilingSupported = Boolean.FALSE;
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                ThreadInfoProvider ti = new ThreadInfoProvider(application);
                final String status = ti.getStatus();
                ThreadsCPU tcpu;
                
                if (status != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = status;
                            refreshSummary();
                            setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                            updateButtons();
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
                    
                cpuSampler = new CPUSamplerSupport(application, ti, tcpu, snapshotDumper, threadDumper) {
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
                        cpuProfilingSupported = Boolean.TRUE;
                        refreshSummary();
                        updateButtons();
                        updateSettings();
                        if (SamplerArguments.Request.CPU.equals(startRequest)) startCPU(settingsRequest);
                    }
                });
            }
        });
    }

    private void initializeMemorySampling() {
        memoryProfilingSupported = Boolean.FALSE;
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                if (application.getState() != Stateful.STATE_AVAILABLE) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                    "MSG_Unavailable"); // NOI18N
                            refreshSummary();
                            setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                            updateButtons();
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
                                    setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                                    updateButtons();
                                }
                            });
                            return;
                        }
                        if (!jvm.isAttachable()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                            "MSG_Unavailable_connect_jdk"); // NOI18N
                                    refreshSummary();
                                    setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                                    updateButtons();
                                }
                            });
                            return;
                        }
                        final AttachModel attachModel = AttachModelFactory.getAttachFor(application);
                        if (attachModel == null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                            "MSG_Unavailable_connect_log", VisualVM.getInstance().getLogfileHandle()); // NOI18N
                                    refreshSummary();
                                    setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                                    updateButtons();
                                }
                            });
                            LOGGER.log(Level.WARNING, "AttachModelFactory.getAttachFor(application) returns null for " + application); // NOI18N
                            return;
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                memoryStatus = NbBundle.getMessage(SamplerImpl.class,
                                    "MSG_Unavailable_read_log", VisualVM.getInstance().getLogfileHandle()); // NOI18N
                                refreshSummary();
                                setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                                updateButtons();
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
                                    "MSG_Unavailable_read_log", VisualVM.getInstance().getLogfileHandle()); // NOI18N
                            refreshSummary();
                            setCurrentViews(NbBundle.getMessage(SamplerImpl.class, "LBL_Information"), getSummaryView()); // NOI18N
                            updateButtons();
                        }
                    });
                    LOGGER.log(Level.WARNING, "attachModel.takeHeapHistogram() throws Throwable for " + application, t); // NOI18N
                    return;
                }

                MemoryMXBean memoryBean = null;
                ThreadsMemory threadsMemory = null;
                JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
                if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                    JvmMXBeans mxbeans = jmxModel.getJvmMXBeans();
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
                memorySampler = new MemorySamplerSupport(application, jvm, hasPermGenHisto, threadsMemory, memoryBean, snapshotDumper, heapDumper) {
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
                        memoryProfilingSupported = Boolean.TRUE;
                        refreshSummary();
                        updateButtons();
                        updateSettings();
                        if (SamplerArguments.Request.MEMORY.equals(startRequest)) startMemory(settingsRequest);
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

    private DataViewComponent.DetailsView[] getSummaryView() {
        if (summaryView == null) {
            summaryArea = new HTMLTextArea() {
                @Override
                protected void showURL(URL url) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            SamplerInitialization.getInstance().runIfChangedToAutomatic(new Runnable() {
                                @Override
                                public void run() {
                                    if (cpuProfilingSupported == null) initializeCpuSampling();
                                    if (memoryProfilingSupported == null) initializeMemorySampling();
                                }
                            });
                            OptionsDisplayer.getDefault().open(OPTIONS_HANDLE); // NOTE: should better open it as modal?
                        }
                    });
                }
            };
            summaryArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            refreshSummary();

            SamplerInitialization.getInstance().addChangeListener(SamplerInitialization.PROP_INITIALIZE_AUTOMATICALLY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    refreshSummary();
                }
            });

            summaryView = new DataViewComponent.DetailsView[] { new DataViewComponent.DetailsView(NbBundle.getMessage(
                            SamplerImpl.class, "LBL_Summary"), null, 10, // NOI18N
                            new ScrollableContainer(summaryArea), null) };
        }
        
        return summaryView;
    }

    private void refreshSummary() {
        StringBuilder builder = new StringBuilder();

        addCpuHeader(builder);
        builder.append(cpuStatus);

        addMemoryHeader(builder);
        builder.append(memoryStatus);
        
//        String initializationMode = NbBundle.getMessage(SamplerImpl.class, SamplerInitialization.getInstance().isAutomatic() ? "LBL_Initialization_Automatically" : "LBL_Initialization_Manually"); // NOI18N
//        builder.append(NbBundle.getMessage(SamplerImpl.class, "LBL_Initialization_Configuration", initializationMode)); // NOI18N

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

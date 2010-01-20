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

import com.sun.tools.visualvm.modules.sampler.memory.MemorySettingsSupport;
import com.sun.tools.visualvm.modules.sampler.cpu.CPUSettingsSupport;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.modules.sampler.cpu.CPUSamplerSupport;
import com.sun.tools.visualvm.modules.sampler.memory.MemorySamplerSupport;
import com.sun.tools.visualvm.profiler.ProfilerSnapshot;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
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
import java.lang.management.ThreadMXBean;
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
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
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
class SamplerImpl {

    private static final Logger LOGGER = Logger.getLogger(SamplerImpl.class.getName());

    private static enum State { TERMINATED, INACTIVE, CPU, MEMORY, TRANSITION };

    private Application application;
    private Timer timer;

    private HTMLTextArea summaryArea;
    private String cpuStatus = "Checking availability...";
    private String memoryStatus = "Checking availability...";

    private boolean cpuProfilingSupported;
    private AbstractSamplerSupport cpuSampler;
    private CPUSettingsSupport cpuSettings;

    private boolean memoryProfilingSupported;
    private AbstractSamplerSupport memorySampler;
    private MemorySettingsSupport memorySettings;

    private DataViewComponent dvc;
    private String currentName;
    private DataViewComponent.DetailsView[] currentViews;

    private State state = State.TRANSITION;


    SamplerImpl(Application application) {
        this.application = application;
        
        cpuSettings = new CPUSettingsSupport(application);
        memorySettings = new MemorySettingsSupport(application);
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
                   ApplicationSamplerView.class, "LBL_Sampler"), null, view); // NOI18N
    }


    void setDataViewComponent(DataViewComponent dvc) {
        this.dvc = dvc;

        setCurrentViews("Information", createSummaryView());

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Settings"), // NOI18N
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
        String status = "<undefined>"; // NOI18N

        switch (getState()) {
            case TERMINATED:
                status = "application terminated";
                break;
            case INACTIVE:
                status = "sampling inactive";
                break;
            case CPU:
                status = "CPU sampling in progress";
                break;
            case MEMORY:
                status = "memory sampling in progress";
                break;
            case TRANSITION:
                status = "refreshing...";
                break;
        }

        statusValueLabel.setText(status);
    }

    private void updateSettings() {
        if (cpuSettings != null && memorySettings != null) {
            switch (getState()) {
                case INACTIVE:
                    cpuSettings.setUIEnabled(cpuProfilingSupported);
                    memorySettings.setUIEnabled(memoryProfilingSupported);
                    break;
                case TERMINATED:
                case CPU:
                case MEMORY:
                case TRANSITION:
                    cpuSettings.setUIEnabled(false);
                    memorySettings.setUIEnabled(false);
                    break;
            }
        }
    }

    private void updateButtons() {
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
                        setCurrentViews("CPU samples", cpuSampler.getDetailsView());
                        RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                cpuSettings.saveSettings();
                                setState(cpuSampler.startSampling(
                                         cpuSettings.getSettings(), cpuSettings.getSamplingRate()) ?
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
                        setCurrentViews("Memory samples", memorySampler.getDetailsView());
                        RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                memorySettings.saveSettings();
                                setState(memorySampler.startSampling(
                                         memorySettings.getSettings(), -1) ?
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
                if (application.getState() != Stateful.STATE_AVAILABLE) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available.";
                            refreshSummary();
                        }
                    });
                    return;
                }
                JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
                if (jmxModel == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available. Cannot initialize JMX connection to target application. Use 'Add JMX Connection' action to attach to the application.";
                            refreshSummary();
                        }
                    });
                    return;
                }
                if (jmxModel.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available. Failed to create JMX connection to target application. Use 'Add JMX Connection' action to attach to the application.";
                            refreshSummary();
                        }
                    });
                    return;
                }
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                if (mxbeans == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available. Cannot access threads in target application. Check the logfile for details (use Help | About | Logfile).";
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "JvmMXBeansFactory.getJvmMXBeans(jmxModel) returns null for " + application); // NOI18N
                    return;
                }
                ThreadMXBean threadBean = mxbeans.getThreadMXBean();
                if (threadBean == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available. Cannot access threads in target application. Check the logfile for details (use Help | About | Logfile).";
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "mxbeans.getThreadMXBean() returns null for " + application); // NOI18N
                    return;
                }
                try {
                    threadBean.getThreadInfo(threadBean.getAllThreadIds(),Integer.MAX_VALUE);
                } catch (SecurityException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available. Failed to access threads in target application. Check the logfile for details (use Help | About | Logfile).";
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "threadBean.getThreadInfo(ids, maxDepth) throws SecurityException for " + application, e); // NOI18N
                    return;
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            cpuStatus = "Not available. Failed to access threads in target application. Check the logfile for details (use Help | About | Logfile).";
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "threadBean.getThreadInfo(ids, maxDepth) throws Throwable for " + application, t); // NOI18N
                    return;
                }

                CPUSamplerSupport.SnapshotDumper snapshotDumper = new CPUSamplerSupport.SnapshotDumper() {
                    public void takeSnapshot(final boolean openView) {
                        final StackTraceSnapshotBuilder builderF = builder;
                        RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                LoadedSnapshot ls = null;
                                DataOutputStream dos = null;
                                try {
                                    long time = System.currentTimeMillis();
                                    CPUResultsSnapshot snapshot = builderF.createSnapshot(time);
                                    ls = new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createCPUPreset(), null);
                                    File file = Utils.getUniqueFile(application.getStorage().getDirectory(),
                                                                    Long.toString(time),
                                                                    "." + ResultsManager.SNAPSHOT_EXTENSION);
                                    dos = new DataOutputStream(new FileOutputStream(file));
                                    ls.save(dos);
                                    ls.setFile(file);
                                    ls.setSaved(true);
                                } catch (CPUResultsSnapshot.NoDataAvailableException e) {
                                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("<html><b>No data to save</b><br><br>Make sure the application performs some code<br>and the selected filter is not filtering out the calls.</html>", NotifyDescriptor.WARNING_MESSAGE));
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
                                    ProfilerSnapshot ps = new ProfilerSnapshot(ls, application);
                                    application.getRepository().addDataSource(ps);
                                    if (openView)
                                        DataSourceWindowManager.sharedInstance().openDataSource(ps);
                                }
                            }
                        });
                    }
                };

                final ThreadDumpSupport tds = ThreadDumpSupport.getInstance();
                final String noThreadDump = tds.supportsThreadDump(application) ? null : "thread dump not supported";

                CPUSamplerSupport.ThreadDumper threadDumper = noThreadDump != null ? null :
                    new CPUSamplerSupport.ThreadDumper() {
                        public void takeThreadDump(boolean openView) {
                            tds.takeThreadDump(application, openView);
                        }
                    };
                cpuSampler = new CPUSamplerSupport(threadBean, snapshotDumper, threadDumper) {
                    protected Timer getTimer() { return SamplerImpl.this.getTimer(); }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String avail = noThreadDump == null ? "Available." : "Available (" + noThreadDump + ").";
                        cpuStatus = avail + " " + "Press the 'CPU' button to start collecting performance data.";
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
                            memoryStatus = "Not available.";
                            refreshSummary();
                        }
                    });
                    return;
                }
                if (!application.isLocalApplication()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = "Not available. Remote applications are not supported.";
                            refreshSummary();
                        }
                    });
                    return;
                }
                if (!JvmFactory.getJVMFor(application).isAttachable()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = "Not available. Cannot connect to target application. Make sure the application is running on a supported JDK 6 or JDK 7.";
                            refreshSummary();
                        }
                    });
                    return;
                }
                final AttachModel attachModel = AttachModelFactory.getAttachFor(application);
                if (attachModel == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = "Not available. Cannot connect to target application. Check the logfile for details (use Help | About | Logfile).";
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "AttachModelFactory.getAttachFor(application) returns null for " + application); // NOI18N
                    return;
                }
                try {
                    if (attachModel.takeHeapHistogram() == null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                memoryStatus = "Not available. Failed to read objects in target application. Check the logfile for details (use Help | About | Logfile).";
                                refreshSummary();
                            }
                        });
                        LOGGER.log(Level.WARNING, "attachModel.takeHeapHistogram() returns null for " + application); // NOI18N
                        return;
                    }
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            memoryStatus = "Not available. Failed to read objects in target application. Check the logfile for details (use Help | About | Logfile).";
                            refreshSummary();
                        }
                    });
                    LOGGER.log(Level.WARNING, "attachModel.takeHeapHistogram() throws Throwable for " + application, t); // NOI18N
                    return;
                }

                MemoryMXBean memoryBean = null;
                JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
                if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                    JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                    if (mxbeans != null) memoryBean = mxbeans.getMemoryMXBean();
                }
                final String noPerformGC = memoryBean == null ? "perform GC not supported" : null;

                final HeapDumpSupport hds = HeapDumpSupport.getInstance();
                final String noHeapDump = hds.supportsHeapDump(application) ? null : "heap dump not supported";

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
                                        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("<html><b>No data to save</b><br><br>Make sure the application performs some code.</html>", NotifyDescriptor.WARNING_MESSAGE));
                                    } else {
                                        ls = new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createMemoryPreset(), null);
                                        File file = Utils.getUniqueFile(application.getStorage().getDirectory(),
                                                                        Long.toString(time),
                                                                        "." + ResultsManager.SNAPSHOT_EXTENSION);
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
                                    ProfilerSnapshot ps = new ProfilerSnapshot(ls, application);
                                    application.getRepository().addDataSource(ps);
                                    if (openView)
                                        DataSourceWindowManager.sharedInstance().openDataSource(ps);
                                }
                            }
                        });
                    }
                };
                MemorySamplerSupport.HeapDumper heapDumper = noHeapDump != null ? null :
                    new MemorySamplerSupport.HeapDumper() {
                        public void takeHeapDump(boolean openView) {
                            hds.takeHeapDump(application, openView);
                        }
                    };
                memorySampler = new MemorySamplerSupport(attachModel, memoryBean, snapshotDumper, heapDumper) {
                    protected Timer getTimer() { return SamplerImpl.this.getTimer(); }
                };
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String avail = "Available.";
                        if (noPerformGC != null || noHeapDump != null) {
                            if (noPerformGC == null) {
                                avail = "Available (" + noHeapDump + ").";
                            } else if (noHeapDump == null) {
                                avail = "Available (" + noPerformGC + ").";
                            } else {
                                avail = "Available (" + noPerformGC + ", " + noHeapDump + ").";
                            }
                        }
                        memoryStatus = avail + " " + "Press the 'Memory' button to start collecting memory data.";
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
                        new DataViewComponent.DetailsView("Summary", null,
                        10, new ScrollableContainer(summaryArea), null) };
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
        builder.append("<b>CPU sampling:</b><br>");
    }

    private static void addMemoryHeader(StringBuilder builder) {
        builder.append("<br><br><b>Memory sampling:</b><br>");
    }

    private void initComponents() {
        view = new JPanel(new GridBagLayout());
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(11, 5, 8, 5));

        GridBagConstraints constraints;

        // modeLabel
        modeLabel = new JLabel(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Sample")); // NOI18N
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
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
        cpuButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Cpu")); // NOI18N
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
        view.add(cpuButton, constraints);

        // memoryButton
        memoryButton = new OneWayToggleButton(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Memory")); // NOI18N
        memoryButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/modules/sampler/resources/memory.png", true))); // NOI18N
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
        stopButton = new JButton(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Stop")); // NOI18N
        stopButton.setIcon(new ImageIcon(ImageUtilities.loadImage("com/sun/tools/visualvm/modules/sampler/resources/stop.png", true))); // NOI18N
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
        view.add(stopButton, constraints);

        // filler1
        JPanel filler1 = new JPanel(null);
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
        view.add(filler1, constraints);

        // statusLabel
        statusLabel = new JLabel(NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Status")); // NOI18N
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
        JPanel filler2 = new JPanel(null);
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
        view.add(filler2, constraints);

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

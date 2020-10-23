/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle.cpu;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.sampler.truffle.AbstractSamplerSupport;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.ProfilingSettingsPresets;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot.NoDataAvailableException;
import org.graalvm.visualvm.lib.jfluid.results.cpu.StackTraceSnapshotBuilder;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class CPUSamplerSupport extends AbstractSamplerSupport {
    
    private final Application application;

    private final ThreadInfoProvider threadInfoProvider;
    private final SnapshotDumper snapshotDumper;
    private final ThreadDumper threadDumper;

    private Timer timer;
    private TimerTask samplerTask;
    private final Refresher refresher;
    private int refreshRate;

    private StackTraceSnapshotBuilder builder;

    private volatile boolean sampleRunning;
    private final Object updateLock = new Object();
    private long currentLiveUpdate;
    private long lastLiveUpdate;

    private CPUView cpuView;
    private DataViewComponent.DetailsView[] detailsViews;

    public CPUSamplerSupport(Application application, ThreadInfoProvider tip, SnapshotDumper snapshotDumper, ThreadDumper threadDumper) {
        this.application = application;
        
        threadInfoProvider = tip;
        this.snapshotDumper = snapshotDumper;
        this.threadDumper = threadDumper;

        refreshRate = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;

        refresher = new Refresher() {
            public void setRefreshRate(int rr) {
                CPUSamplerSupport.this.refreshRate = rr;
            }
            public int getRefreshRate() {
                return CPUSamplerSupport.this.refreshRate;
            }
            protected boolean checkRefresh() {
                return samplerTask != null && cpuView.isShowing();
            }
            protected void doRefresh() {
                doRefreshImpl();
            }
        };
    }


    public DataViewComponent.DetailsView[] getDetailsView() {
        if (detailsViews == null) {
            cpuView = new CPUView(refresher, snapshotDumper, threadDumper, application);
            detailsViews = new DataViewComponent.DetailsView[1];
            detailsViews[0] = new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerSupport.class, "LBL_Cpu_samples"), null, 10, cpuView, null); // NOI18N
        }
        cpuView.initSession();
        return detailsViews.clone();
    }

    public void setOptions(String mode, boolean trackFlags) {
        threadInfoProvider.setOptions(mode, trackFlags);
    }

    public boolean startSampling(ProfilingSettings settings, int samplingRate, int refreshRate) {
        checkCPUSamplingRate(samplingRate);
        
        GenericFilter sf = settings.getInstrumentationFilter();
        InstrumentationFilter filter = new InstrumentationFilter(sf);
        builder = snapshotDumper.getNewBuilder(filter);
        
        refresher.setRefreshRate(refreshRate);

        final StackTraceSnapshotBuilder _builder = builder;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (cpuView != null) {
                    cpuView.setBuilder(_builder);
                    cpuView.starting();
                }
            }
        });

        if (timer == null) timer = getTimer();
        samplerTask = new SamplerTask(builder);

        timer.scheduleAtFixedRate(samplerTask, 0, samplingRate);
        return true;
    }

    public synchronized void stopSampling() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (cpuView != null) cpuView.stopping();
            }
        });
        
        if (samplerTask != null) {
            samplerTask.cancel();
            samplerTask = null;
        }
    }

    public synchronized void terminate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (cpuView != null) cpuView.terminated();
            }
        });
        
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        
        builder = null;  // release data
    }


    private void doRefreshImpl() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (samplerTask == null) return;
                if (!sampleRunning) {
                    synchronized (updateLock) {
                        lastLiveUpdate = currentLiveUpdate;
                        cpuView.refresh();
                    }
                } else {
                    SwingUtilities.invokeLater(this);
                }
            }
        });
    }
//    private void doRefreshImpl(final javax.swing.Timer stimer, final ThreadsCPUView view) {
//        if (!stimer.isRunning() || view.isPaused()) return;
//        
//        try {
//            timer.schedule(new TimerTask() {
//                public void run() {
//                    try {
//                        if (!stimer.isRunning()) return;
//                    } catch (Exception e) {
//                        terminate();
//                    }
//                }
//            }, 0);
//        } catch (Exception e) {
//            terminate();
//        }
//    }
//    
//    private void doRefreshImplImpl(final ThreadsCPUInfo info, final ThreadsCPUView view) {
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    view.refresh(info);
//                }
//            });
//    }

    private class SamplerTask extends TimerTask {

        private final StackTraceSnapshotBuilder builder;
        private final Set samplingThreads = new HashSet();

        public SamplerTask(StackTraceSnapshotBuilder builder) {
            this.builder = builder;
        }

        public void run() {
            if (sampleRunning) return;
            sampleRunning = true;
            synchronized (updateLock) {
                try {
                    Map<String,Object>[] infos = threadInfoProvider.dumpAllThreads();
                    long timestamp = System.nanoTime();
                    String samplingThreadName = null;
                    if (samplingThreadName != null) {
                        if (samplingThreads.add(samplingThreadName)) {
//                                System.out.println("New ignored thread: "+samplingThreadName);
                            builder.setIgnoredThreads(samplingThreads);
                        }
                    }
                    addSourceNames(infos);
                    builder.addStacktrace(infos, timestamp);

                    currentLiveUpdate = timestamp / 1000000;
                    if (currentLiveUpdate - lastLiveUpdate >= refreshRate)
                        refresher.refresh();

                } catch (Throwable ex) {
                    ex.printStackTrace();
                    terminate();
                } finally {
                    sampleRunning = false;
                }
            }
        }

        private static final int COMPILED  = 1;  // 0001
        private static final int INLINED   = 2;  // 0010

        private void addSourceNames(Map<String,Object>[] infoMap) {
            for (Map<String,Object> threadInfo : infoMap) {
                StackTraceElement[] stack = (StackTraceElement[]) threadInfo.get("stack");  // NOI18N
                byte[] flags = (byte[]) threadInfo.get("flags");  // NOI18N

                for (int i = 0; i <stack.length; i++) {
                    StackTraceElement ste = stack[i];
                    byte flag = flags!=null ? flags[i] : 0;
                    File file = new File(ste.getFileName());
                    String fname = file.getName();
                    String flagSrt = "";
                    if (isCompiled(flag)) {
                        flagSrt = "compiled";
                        if (isInlined(flag)) {
                            flagSrt += ", ";
                            flagSrt += "inlined";
                        }
                        flagSrt=" ["+flagSrt+"]";
                    } else {
                        assert !isInlined(flag);
                    }
                    String detailedName = ste.getMethodName()+flagSrt+"|(L"+fname+":"+ste.getLineNumber()+";)L;";   // NOI18N
                    stack[i] = new StackTraceElement(ste.getClassName(), detailedName, ste.getFileName(), ste.getLineNumber());
                }
            }
        }

        private boolean isCompiled(byte flag) {
            return (flag&COMPILED)==COMPILED;
        }

        private boolean isInlined(byte flag) {
            return (flag&INLINED)==INLINED;
        }
    }

    public static abstract class ThreadDumper {
        public abstract void takeThreadDump(boolean openView);
    }

    public static abstract class SnapshotDumper {
        private StackTraceSnapshotBuilder builder;
                
        StackTraceSnapshotBuilder getNewBuilder(InstrumentationFilter filter) {
            builder = new StackTraceSnapshotBuilder(1,filter);
            return builder;
        }
        
        public final LoadedSnapshot takeNPSSnapshot(File directory) throws IOException, NoDataAvailableException {
            if (builder == null) throw new IllegalStateException("Builder is null"); // NOI18N
            long time = System.currentTimeMillis();
            CPUResultsSnapshot snapshot = builder.createSnapshot(time);
            LoadedSnapshot ls = new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createCPUPreset(), null, null);
            File file = Utils.getUniqueFile(directory,
                    ResultsManager.getDefault().getDefaultSnapshotFileName(ls),
                    "." + ResultsManager.SNAPSHOT_EXTENSION); // NOI18N
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            try {
                ls.save(dos);
                ls.setFile(file);
                ls.setSaved(true);
            } finally {
                dos.close();
            }
            return ls;
        }

        public abstract void takeSnapshot(boolean openView);
    }

}

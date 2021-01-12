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

package org.graalvm.visualvm.sampler.memory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.graalvm.visualvm.sampler.AbstractSamplerSupport;
import org.graalvm.visualvm.sampler.AbstractSamplerSupport.Refresher;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class MemorySamplerSupport extends AbstractSamplerSupport {
    
    private final Application application;
    
    private final Jvm jvm;
    private final MemoryMXBean memoryBean;
    private final ThreadsMemory threadsMemory;
    private final HeapDumper heapDumper;
    private final SnapshotDumper snapshotDumper;
    
    private java.util.Timer processor;
    private final AtomicBoolean updateIsRunning;
    
    private Timer heapTimer;
    private Refresher heapRefresher;
    private MemoryView heapView;
    
    private final boolean hasPermGenHisto;
    private Timer permgenTimer;
    private Refresher permgenRefresher;
    private MemoryView permgenView;
    
    private Timer threadAllocTimer;
    private Refresher threadAllocRefresher;
    private ThreadsMemoryView threadAllocView;
    
    private DataViewComponent.DetailsView[] detailsViews;
    
    public MemorySamplerSupport(Application application, Jvm jvm, boolean hasPermGen, ThreadsMemory mem, MemoryMXBean memoryBean, SnapshotDumper snapshotDumper, HeapDumper heapDumper) {
        this.application = application;
        
        this.jvm = jvm;
        hasPermGenHisto = hasPermGen;
        threadsMemory = mem;
        this.memoryBean = memoryBean;
        this.heapDumper = heapDumper;
        this.snapshotDumper = snapshotDumper;
        updateIsRunning = new AtomicBoolean();
    }
    
    
    public DataViewComponent.DetailsView[] getDetailsView() {
        if (detailsViews == null) {
            initialize();
            detailsViews = createViews();
        }
        heapView.initSession();
        if (permgenView != null) permgenView.initSession();
        if (threadAllocView != null) threadAllocView.initSession();
        return detailsViews.clone();
    }
    
    public boolean startSampling(ProfilingSettings settings, int samplingRate, int refreshRate) {
//        heapTimer.start();
//        permgenTimer.start();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.starting();
                if (permgenView != null) permgenView.starting();
                if (threadAllocView != null) threadAllocView.starting();
            }
        });

        heapRefresher.setRefreshRate(samplingRate);
        if (permgenRefresher != null)
            permgenRefresher.setRefreshRate(samplingRate);
        if (threadAllocRefresher != null)
            threadAllocRefresher.setRefreshRate(samplingRate);
        if (heapView != null) {
            if (permgenView != null) doRefreshImpl(heapTimer, heapView, permgenView);
            else doRefreshImpl(heapTimer, heapView);
        }
        return true;
    }
    
    public synchronized void stopSampling() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.stopping();
                if (permgenView != null) permgenView.stopping();
                if (threadAllocView != null) threadAllocView.stopping();
            }
        });
        
        heapTimer.stop();
        if (permgenTimer != null) {
            permgenTimer.stop();
        }
        if (threadAllocTimer != null)
            threadAllocTimer.stop();
        if (heapView != null) {
            if (permgenView != null) doRefreshImplImpl(snapshotDumper.lastHistogram, heapView, permgenView);
            else doRefreshImplImpl(snapshotDumper.lastHistogram, heapView);
        }
    }
    
    public synchronized void terminate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.terminated();
                if (permgenView != null) permgenView.terminated();
                if (threadAllocView != null) threadAllocView.terminated();
            }
        });
    }
    
    
    private void initialize() {
        int defaultRefresh = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;
        
        processor = getTimer();
        
        heapTimer = new Timer(defaultRefresh, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                heapRefresher.refresh();
            }
        });
        heapRefresher = new Refresher() {
            public final boolean checkRefresh() {
                if (!heapTimer.isRunning()) return false;
                return (heapView.isShowing() || (permgenTimer != null && permgenTimer.getDelay() ==
                        heapTimer.getDelay() && permgenView.isShowing()));
            }
            public final void doRefresh() {
                if (heapView.isShowing()) {
                    doRefreshImpl(heapTimer, heapView);
                } else if (permgenTimer != null && permgenTimer.getDelay() == heapTimer.getDelay() &&
                           permgenView.isShowing()) {
                    doRefreshImpl(heapTimer, permgenView);
                }
            }
            public final void setRefreshRate(int refreshRate) {
                heapTimer.setDelay(refreshRate);
                heapTimer.setInitialDelay(refreshRate);
                heapTimer.restart();
            }
            public final int getRefreshRate() {
                return heapTimer.getDelay();
            }
        };
        
        if (hasPermGenHisto) {
            permgenTimer = new Timer(defaultRefresh, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    permgenRefresher.refresh();
                }
            });
            permgenRefresher = new Refresher() {
                public final boolean checkRefresh() {
                    if (!permgenTimer.isRunning()) return false;
                    if (permgenTimer.getDelay() == heapTimer.getDelay()) return false;
                    return (permgenView.isShowing());
                }
                public final void doRefresh() {
                    doRefreshImpl(permgenTimer, permgenView);
                }
                public final void setRefreshRate(int refreshRate) {
                    permgenTimer.setDelay(refreshRate);
                    permgenTimer.setInitialDelay(refreshRate);
                    permgenTimer.restart();
                }
                public final int getRefreshRate() {
                    return permgenTimer.getDelay();
                }
            };
        }
        
        if (threadsMemory != null) {
            threadAllocTimer = new Timer(defaultRefresh, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    threadAllocRefresher.refresh();
                }
            });
            threadAllocRefresher = new Refresher() {
                public final boolean checkRefresh() {
                    if (!threadAllocTimer.isRunning()) return false;
                    return threadAllocView.isShowing();
                }
                public final void doRefresh() {
                    doRefreshImpl(threadAllocTimer, threadAllocView);
                }
                public final void setRefreshRate(int refreshRate) {
                    threadAllocTimer.setDelay(refreshRate);
                    threadAllocTimer.setInitialDelay(refreshRate);
                    threadAllocTimer.restart();
                }
                public final int getRefreshRate() {
                    return threadAllocTimer.getDelay();
                }
            };
        }
    }
    
    private DataViewComponent.DetailsView[] createViews() {
        int detailIndex = 0;
        int detailsCount = 1;
        if (hasPermGenHisto) detailsCount++;
        if (threadAllocRefresher != null) detailsCount++;
        DataViewComponent.DetailsView[] details = new DataViewComponent.DetailsView[detailsCount];
        
        heapView = new MemoryView(application, heapRefresher, MemoryView.MODE_HEAP, memoryBean, snapshotDumper, heapDumper);
        details[detailIndex++] = new DataViewComponent.DetailsView(
                    NbBundle.getMessage(MemorySamplerSupport.class, "LBL_Heap_histogram"), // NOI18N
                    null, 10, heapView, null);
        if (hasPermGenHisto) {
            permgenView = new MemoryView(application, permgenRefresher, MemoryView.MODE_PERMGEN, memoryBean, null, heapDumper);
            details[detailIndex++] = new DataViewComponent.DetailsView(
                        NbBundle.getMessage(MemorySamplerSupport.class, "LBL_PermGen_histogram"), // NOI18N
                        null, 20, permgenView, null);
        }
        if (threadAllocRefresher != null) {
            threadAllocView = new ThreadsMemoryView(threadAllocRefresher, memoryBean, heapDumper);
            details[detailIndex++] = new DataViewComponent.DetailsView(
                    NbBundle.getMessage(MemorySamplerSupport.class, "LBL_ThreadAlloc"), // NOI18N
                    null, 30, threadAllocView, null);
        }
        return details;
    }
    
    private void doRefreshImpl(final Timer timer, final ThreadsMemoryView view) {
        if (!timer.isRunning() || view.isPaused()) return;
        if (!updateIsRunning.compareAndSet(false, true)) return;
        try {
            processor.schedule(new TimerTask() {
                public void run() {
                    try {
                        if (!timer.isRunning()) return;
                        doRefreshImplImpl(threadsMemory.getThreadsMemoryInfo(), view);
                    } catch (Exception e) {
                        terminate();
                    } finally {
                        updateIsRunning.set(false);
                    }
                }
            }, 0);
        } catch (Exception e) {
            terminate();
        }
    }
    
    private void doRefreshImpl(final Timer timer, final MemoryView... views) {
        if (!timer.isRunning() || (views.length == 1 && views[0].isPaused())) return;
        if (!updateIsRunning.compareAndSet(false, true)) return;
        try {
            processor.schedule(new TimerTask() {
                public void run() {
                    try {
                        if (!timer.isRunning()) return;
                        doRefreshImplImpl(jvm.takeHeapHistogram(), views);
                    } catch (Exception e) {
                        terminate();
                    } finally {
                        updateIsRunning.set(false);
                    }
                }
            }, 0);
        } catch (Exception e) {
            terminate();
        }
    }

    private void doRefreshImplImpl(final HeapHistogram heapHistogram, final MemoryView... views) {
        if (heapHistogram != null)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    snapshotDumper.lastHistogram = heapHistogram;
                    for (MemoryView view : views) view.refresh(heapHistogram);
                }
            });
    }
    
    private void doRefreshImplImpl(final ThreadsMemoryInfo info, final ThreadsMemoryView view) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    view.refresh(info);
                }
            });
    }

    public static abstract class HeapDumper {
        public abstract void takeHeapDump(boolean openView);
    }
    
    public static abstract class SnapshotDumper {
        private volatile HeapHistogram lastHistogram;
        
        public abstract void takeSnapshot(boolean openView);
        
        public SampledMemoryResultsSnapshot createSnapshot(long time) {
            HeapHistogram histogram = lastHistogram;

            if (histogram != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
                DataOutputStream dos = new DataOutputStream(output);
                try {
                    SampledMemoryResultsSnapshot result = new SampledMemoryResultsSnapshot();
                    Set<HeapHistogram.ClassInfo> classes = histogram.getHeapHistogram();
                    
                    dos.writeInt(1);    // version
                    dos.writeLong(histogram.getTime().getTime()); // begin time
                    dos.writeLong(time); // taken time
                    dos.writeInt(classes.size());   // no of classes
                    for (HeapHistogram.ClassInfo info : classes) {
                        dos.writeUTF(info.getName());       // name
                        dos.writeLong(info.getBytes());     // total number of bytes
                    }
                    dos.writeBoolean(false); // no stacktraces
                    dos.writeInt(classes.size());   // no of classes
                    for (HeapHistogram.ClassInfo info : classes) {
                        dos.writeInt((int)info.getInstancesCount());     // number of instances
                    }
                    dos.close();
                    result.readFromStream(new DataInputStream(new ByteArrayInputStream(output.toByteArray())));
                    return result;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }
}

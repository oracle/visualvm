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

package org.graalvm.visualvm.sampler.truffle.memory;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.sampler.truffle.AbstractSamplerSupport;
import org.graalvm.visualvm.sampler.truffle.AbstractSamplerSupport.Refresher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class MemorySamplerSupport extends AbstractSamplerSupport {
    
    private final Application application;
    
    private final MemoryHistogramProvider histogramProvider;
    private final MemoryMXBean memoryBean;
    private final HeapDumper heapDumper;
    private final SnapshotDumper snapshotDumper;
    
    private java.util.Timer processor;
    
    private Timer heapTimer;
    private Refresher heapRefresher;
    private MemoryView heapView;

    private DataViewComponent.DetailsView[] detailsViews;
    
    public MemorySamplerSupport(Application application, MemoryHistogramProvider mhp, MemoryMXBean memoryBean, SnapshotDumper snapshotDumper, HeapDumper heapDumper) {
        this.application = application;
        histogramProvider = mhp;
        this.memoryBean = memoryBean;
        this.heapDumper = heapDumper;
        this.snapshotDumper = snapshotDumper;
    }
    
    
    public DataViewComponent.DetailsView[] getDetailsView() {
        if (detailsViews == null) {
            initialize();
            detailsViews = createViews();
        }
        heapView.initSession();
        return detailsViews.clone();
    }
    
    public boolean startSampling(ProfilingSettings settings, int samplingRate, int refreshRate) {
//        heapTimer.start();
//        permgenTimer.start();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.starting();
            }
        });

        heapRefresher.setRefreshRate(samplingRate);
        if (heapView != null) {
            doRefreshImpl(heapTimer, heapView);
        }
        return true;
    }
    
    public synchronized void stopSampling() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.stopping();
            }
        });
        
        heapTimer.stop();
        if (heapView != null) {
            doRefreshImplImpl(snapshotDumper.lastHistogram, heapView);
        }
    }
    
    public synchronized void terminate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.terminated();
            }
        });
    }
    
    
    private String initialize() {
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
                return heapView.isShowing();
            }
            public final void doRefresh() {
                if (heapView.isShowing()) {
                    doRefreshImpl(heapTimer, heapView);
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
        return null;
    }
    
    private DataViewComponent.DetailsView[] createViews() {
        int detailIndex = 0;
        int detailsCount = 1;
        DataViewComponent.DetailsView[] details = new DataViewComponent.DetailsView[detailsCount];
        
        heapView = new MemoryView(application, heapRefresher, memoryBean, snapshotDumper, heapDumper);
        details[detailIndex++] = new DataViewComponent.DetailsView(
                    NbBundle.getMessage(MemorySamplerSupport.class, "LBL_Heap_histogram"), // NOI18N
                    null, 10, heapView, null);
        return details;
    }
    
    private void doRefreshImpl(final Timer timer, final MemoryView... views) {
        if (!timer.isRunning() || (views.length == 1 && views[0].isPaused())) return;
        
        try {
            processor.schedule(new TimerTask() {
                public void run() {
                    try {
                        if (!timer.isRunning()) return;
                        doRefreshImplImpl(takeHeapHistogram(), views);
                    } catch (Exception e) {
                        terminate();
                    }
                }
            }, 0);
        } catch (Exception e) {
            terminate();
        }
    }

    private TruffleHeapHistogram takeHeapHistogram() {
        try {
            Map<String, Object>[] histo = histogramProvider.heapHistogram();

            return new TruffleHeapHistogram(histo);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private void doRefreshImplImpl(final TruffleHeapHistogram heapHistogram, final MemoryView... views) {
        if (heapHistogram != null)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    snapshotDumper.lastHistogram = heapHistogram;
                    for (MemoryView view : views) view.refresh(heapHistogram);
                }
            });
    }

    public static abstract class HeapDumper {
        public abstract void takeHeapDump(boolean openView);
    }
    
    public static abstract class SnapshotDumper {
        private volatile TruffleHeapHistogram lastHistogram;
        
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

    class TruffleHeapHistogram extends HeapHistogram {

        private long totalInstances;
        private long totalBytes;
        private long totalAllocInstances;
        private long totalAllocBytes;
        private final long time;
        private final Set<TruffleClassInfo> classes;

        private TruffleHeapHistogram(Map<String, Object>[] heap) {
            time = System.currentTimeMillis();
            classes = new HashSet(heap.length);
            for (Map<String, Object> classInfo : heap) {
                TruffleClassInfo info = new TruffleClassInfo(classInfo);

                totalInstances += info.getInstancesCount();
                totalBytes += info.getBytes();
                totalAllocInstances += info.getAllocatedInstances();
                totalAllocBytes += info.getAllocatedBytes();
                classes.add(info);
            }
        }

        @Override
        public Date getTime() {
            return new Date(time);
        }

        @Override
        public long getTotalInstances() {
            return totalInstances;
        }

        @Override
        public long getTotalBytes() {
            return totalBytes;
        }

        public long getTotalAllocInstances() {
            return totalAllocInstances;
        }

        public long getTotalAllocBytes() {
            return totalAllocBytes;
        }

        @Override
        public Set<ClassInfo> getHeapHistogram() {
            return Collections.unmodifiableSet(classes);
        }

        @Override
        public long getTotalHeapInstances() {
            return totalInstances;
        }

        @Override
        public long getTotalHeapBytes() {
            return totalBytes;
        }

        @Override
        public Set<ClassInfo> getPermGenHistogram() {
            return null;
        }

        @Override
        public long getTotalPerGenInstances() {
            return 0;
        }

        @Override
        public long getTotalPermGenHeapBytes() {
            return 0;
        }
    }

    static class TruffleClassInfo extends HeapHistogram.ClassInfo {

        String name;
        long allocatedInstances;
        long liveInstances;
        long bytes;
        long liveBytes;

        TruffleClassInfo() {
        }

        private TruffleClassInfo(Map<String, Object> info) {
            name = info.get("language") + "." + info.get("name");
            allocatedInstances = (Long) info.get("allocatedInstancesCount");
            bytes = (Long) info.get("bytes");
            liveInstances = (Long) info.get("liveInstancesCount");
            liveBytes = (Long) info.get("liveBytes");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getInstancesCount() {
            return liveInstances;
        }

        @Override
        public long getBytes() {
            return liveBytes;
        }

        public long getAllocatedInstances() {
            return allocatedInstances;
        }

        public long getAllocatedBytes() {
            return bytes;
        }
    }
}

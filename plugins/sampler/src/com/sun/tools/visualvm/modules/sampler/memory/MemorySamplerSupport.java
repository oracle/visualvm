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

package com.sun.tools.visualvm.modules.sampler.memory;

import com.sun.tools.visualvm.application.jvm.HeapHistogram;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.sampler.AbstractSamplerSupport;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.management.MemoryMXBean;
import java.util.TimerTask;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.netbeans.lib.profiler.common.ProfilingSettings;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class MemorySamplerSupport extends AbstractSamplerSupport {

    private final AttachModel attachModel;
    private final MemoryMXBean memoryBean;
    private final HeapDumper heapDumper;

    private java.util.Timer processor;

    private Timer heapTimer;
    private Refresher heapRefresher;
    private MemoryView heapView;

    private Timer permgenTimer;
    private Refresher permgenRefresher;
    private MemoryView permgenView;

    private DataViewComponent.DetailsView[] detailsViews;


    public MemorySamplerSupport(AttachModel attachModel, MemoryMXBean memoryBean, HeapDumper heapDumper) {
        this.attachModel = attachModel;
        this.memoryBean = memoryBean;
        this.heapDumper = heapDumper;
    }


    public DataViewComponent.DetailsView[] getDetailsView() {
        if (detailsViews == null) {
            initialize();
            detailsViews = createViews();
        }
        return detailsViews;
    }

    public boolean startSampling(ProfilingSettings settings, int samplingRate) {
        heapTimer.start();
        permgenTimer.start();
        return true;
    }

    public synchronized void stopSampling() {
        heapTimer.stop();
        permgenTimer.stop();
    }

    public synchronized void terminate() {
        if (heapView != null) heapView.terminate();
        if (permgenView != null) permgenView.terminate();
    }


    private void initialize() {
        int defaultRefresh = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;

        processor = getTimer();

        heapTimer = new Timer(defaultRefresh, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                heapRefresher.refresh();
            }
        });
        heapTimer.setInitialDelay(0);
        heapRefresher = new Refresher() {
            public final boolean checkRefresh() {
                if (!heapTimer.isRunning()) return false;
                return (heapView.isShowing() || permgenView.isShowing());
            }
            public final void doRefresh() {
                if (permgenTimer.getDelay() == heapTimer.getDelay()) {
                    doRefreshImpl(heapTimer, heapView, permgenView);
                } else {
                    doRefreshImpl(heapTimer, heapView);
                }
            }
            public final void setRefreshRate(int refreshRate) {
                heapTimer.setDelay(refreshRate);
                heapTimer.restart();
            }
            public final int getRefreshRate() {
                return heapTimer.getDelay();
            }
        };

        permgenTimer = new Timer(defaultRefresh, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                permgenRefresher.refresh();
            }
        });
        permgenTimer.setInitialDelay(0);
        permgenRefresher = new Refresher() {
            public final boolean checkRefresh() {
                if (!permgenTimer.isRunning()) return false;
                if (permgenTimer.getDelay() == heapTimer.getDelay()) return false;
                return (heapView.isShowing() || permgenView.isShowing());
            }
            public final void doRefresh() {
                doRefreshImpl(permgenTimer, permgenView);
            }
            public final void setRefreshRate(int refreshRate) {
                permgenTimer.setDelay(refreshRate);
                permgenTimer.restart();
            }
            public final int getRefreshRate() {
                return permgenTimer.getDelay();
            }
        };
    }

    private DataViewComponent.DetailsView[] createViews() {
        heapView = new MemoryView(heapRefresher, MemoryView.MODE_HEAP, memoryBean, heapDumper);
        permgenView = new MemoryView(permgenRefresher, MemoryView.MODE_PERMGEN, memoryBean, heapDumper);
        return new DataViewComponent.DetailsView[] {
                new DataViewComponent.DetailsView(
                "Heap histogram", null, 10, heapView, null),
                new DataViewComponent.DetailsView("PermGen histogram", null, 20,
                permgenView, null) };
    }


    private void doRefreshImpl(final Timer timer, final MemoryView... views) {
        if (!timer.isRunning()) return;

        try {
            processor.schedule(new TimerTask() {
                public void run() {
                    try {
                        if (!timer.isRunning()) return;
                        final HeapHistogram heapHistogram = attachModel.takeHeapHistogram();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                for (MemoryView view : views)
                                    view.refresh(heapHistogram);
                            }
                        });
                    } catch (Exception e) {
                        terminate();
                    }
                }
            }, 0);
        } catch (Exception e) {
            terminate();
        }
    }


    public static abstract class HeapDumper {
        public abstract void takeHeapDump(boolean openView);
    }

}

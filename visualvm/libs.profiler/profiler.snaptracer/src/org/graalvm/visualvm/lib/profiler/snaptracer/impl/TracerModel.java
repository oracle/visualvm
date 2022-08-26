/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.io.IOException;
import org.graalvm.visualvm.lib.jfluid.results.cpu.PrestimeCPUCCTNode;
import org.graalvm.visualvm.lib.profiler.snaptracer.PackageStateHandler;
import org.graalvm.visualvm.lib.profiler.snaptracer.ProbeStateHandler;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerPackage;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbe;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbeDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.TimelineSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.profiler.snaptracer.Positionable;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TracerModel {

    private static final Logger LOGGER = Logger.getLogger(TracerController.class.getName());

    private final IdeSnapshot snapshot;

    private final Map<TracerPackage, List<TracerProbe>> probesCache = new HashMap();
    private final Map<TracerProbe, TracerProbeDescriptor> descriptorsCache = new HashMap();

    private final Set<Listener> listeners = new HashSet();

    private final TimelineSupport timelineSupport;


    // --- Constructor ---------------------------------------------------------

    public TracerModel(IdeSnapshot snapshot) {
        this.snapshot = snapshot;
        timelineSupport = new TimelineSupport(new TimelineSupport.DescriptorResolver() {
            public TracerProbeDescriptor getDescriptor(TracerProbe p) {
                return TracerModel.this.getDescriptor(p);
            }
        }, snapshot);
    }


    // --- DataSource ----------------------------------------------------------

    IdeSnapshot getSnapshot() {
        return snapshot;
    }
    
    int getSamplesCount() {
        return snapshot.getSamplesCount();
    }

    long firstTimestamp() {
        return getTimestamp(0);
    }

    long lastTimestamp() {
        return getTimestamp(getSamplesCount() - 1);
    }

    long getTimestamp(int sampleIndex) {
        try {
            return snapshot.getTimestamp(sampleIndex) / 1000000;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return -1;
        }
    }


    // --- Packages ------------------------------------------------------------

    List<TracerPackage> getPackages() {
        try {
            return TracerSupportImpl.getInstance().getPackages(snapshot);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Package exception in getPackages", t); // NOI18N
            return null;
        }
    }


    // --- Probes --------------------------------------------------------------

    void addDescriptor(final TracerPackage p,
                       final TracerProbeDescriptor d) {
        TracerSupportImpl.getInstance().perform(new Runnable() {
            public void run() { addProbe(p, d); }
        });
    }

    void removeDescriptor(final TracerPackage p,
                          final TracerProbeDescriptor d) {
        TracerSupportImpl.getInstance().perform(new Runnable() {
            public void run() { removeProbe(p, d); }
        });
    }
    
    void addDescriptors(final TracerPackage p,
                       final TracerProbeDescriptor[] da) {
        for (TracerProbeDescriptor d : da) addProbe(p, d);
    }

    void removeDescriptors(final TracerPackage p,
                          final TracerProbeDescriptor[] da) {
        for (TracerProbeDescriptor d : da) removeProbe(p, d);
    }

    TracerProbeDescriptor getDescriptor(TracerProbe p) {
        synchronized(descriptorsCache) {
            return descriptorsCache.get(p);
        }
    }

    // Must be called in EDT
    List<TracerProbe> getDefinedProbes() {
        List<TracerProbe> probes = new ArrayList();
        probes.addAll(timelineSupport.getProbes());
        return probes;
    }

    Set<Map.Entry<TracerPackage, List<TracerProbe>>> getDefinedProbeSets() {
        Comparator<Map.Entry<TracerPackage, List<TracerProbe>>> comp =
                new Comparator<Map.Entry<TracerPackage, List<TracerProbe>>>() {
            public int compare(Entry<TracerPackage, List<TracerProbe>> o1,
                               Entry<TracerPackage, List<TracerProbe>> o2) {
                return Positionable.STRONG_COMPARATOR.compare(o1.getKey(), o2.getKey());
            }
        };
        Set<Map.Entry<TracerPackage, List<TracerProbe>>> probes = new TreeSet(comp);
        synchronized(probesCache) { probes.addAll(probesCache.entrySet()); }
        return probes;
    }

    boolean areProbesDefined() {
        synchronized(probesCache) { return !probesCache.isEmpty(); }
    }


    private void addProbe(TracerPackage p, TracerProbeDescriptor d) {
        TracerProbe r = p.getProbe(d);
        synchronized(descriptorsCache) {
            descriptorsCache.put(r, d);
        }
        synchronized(probesCache) {
            List<TracerProbe> probes = probesCache.get(p);
            if (probes == null) {
                probes = new ArrayList();
                probesCache.put(p, probes);
            }
            probes.add(r);
        }

        timelineSupport.addProbe(r);

        notifyProbeAdded(p, r);
        fireProbeAdded(r);
    }

    private void removeProbe(TracerPackage p, TracerProbeDescriptor d) {
        TracerProbe probe = null;
        boolean probesDefined = true;

        synchronized(descriptorsCache) {
            for (Map.Entry<TracerProbe, TracerProbeDescriptor> entry : descriptorsCache.entrySet()) {
                if (entry.getValue() == d) {
                    probe = entry.getKey();
                    break;
                }
            }
            descriptorsCache.remove(probe);
        }
        synchronized(probesCache) {
            List<TracerProbe> probes = probesCache.get(p);
            probes.remove(probe);
            if (probes.isEmpty()) {
                probesCache.remove(p);
                probesDefined = !probesCache.isEmpty();
            }
        }

        timelineSupport.removeProbe(probe);

        notifyProbeRemoved(p, probe);
        fireProbeRemoved(probe, probesDefined);
    }

    private void notifyProbeAdded(TracerPackage p, TracerProbe r) {
        PackageStateHandler ph = p.getStateHandler();
        if (ph != null) try {
            ph.probeAdded(r, snapshot);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Package exception in probeAdded", t); // NOI18N
        }

        ProbeStateHandler rh = r.getStateHandler();
        if (rh != null) try {
            rh.probeAdded(snapshot);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Probe exception in probeAdded", t); // NOI18N
        }
    }

    private void notifyProbeRemoved(TracerPackage p, TracerProbe r) {
        PackageStateHandler ph = p.getStateHandler();
        if (ph != null) try {
            ph.probeRemoved(r, snapshot);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Package exception in probeRemoved", t); // NOI18N
        }

        ProbeStateHandler rh = r.getStateHandler();
        if (rh != null) try {
            rh.probeRemoved(snapshot);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Probe exception in probeRemoved", t); // NOI18N
        }
    }


    // --- Events support ------------------------------------------------------

    void addListener(Listener listener) {
        synchronized(listeners) { listeners.add(listener); }
    }

    void removeListener(Listener listener) {
        synchronized(listeners) { listeners.remove(listener); }
    }

    private void fireProbeAdded(final TracerProbe probe) {
        final Set<Listener> toNotify = new HashSet();
        synchronized(listeners) { toNotify.addAll(listeners); }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (Listener listener : toNotify)
                    listener.probeAdded(probe);
            }
        });
        
    }

    private void fireProbeRemoved(final TracerProbe probe, final boolean probesDefined) {
        final Set<Listener> toNotify = new HashSet();
        synchronized(listeners) { toNotify.addAll(listeners); }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (Listener listener : toNotify)
                    listener.probeRemoved(probe, probesDefined);
            }
        });
    }

    static interface Listener {

        public void probeAdded(TracerProbe probe);

        public void probeRemoved(TracerProbe probe, boolean probesDefined);

    }


    // --- Timeline ------------------------------------------------------------

    TimelineSupport getTimelineSupport() {
        return timelineSupport;
    }

    List<Integer> getIntervals(PrestimeCPUCCTNode node) throws IOException {
        TimelineSupport support = getTimelineSupport();
        final int startIndex = Math.min(support.getStartIndex(), support.getEndIndex());
        final int endIndex = Math.max(support.getStartIndex(), support.getEndIndex());
        
        return getSnapshot().getIntervals(startIndex,endIndex,node);
    }

}

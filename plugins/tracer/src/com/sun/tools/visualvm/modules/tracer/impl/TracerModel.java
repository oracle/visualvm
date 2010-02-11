/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.tracer.impl;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.modules.tracer.PackageStateHandler;
import com.sun.tools.visualvm.modules.tracer.ProbeStateHandler;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.TimelineSupport;
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
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerModel {

    private static final Logger LOGGER = Logger.getLogger(TracerController.class.getName());

    private final DataSource dataSource;

    private final Map<TracerPackage, List<TracerProbe>> probesCache = new HashMap();

    private final Set<Listener> listeners = new HashSet();

    private final TimelineSupport timelineSupport;


    // --- Constructor ---------------------------------------------------------

    TracerModel(DataSource dataSource) {
        this.dataSource = dataSource;
        
        timelineSupport = new TimelineSupport();
    }


    // --- DataSource ----------------------------------------------------------

    DataSource getDataSource() {
        return dataSource;
    }


    // --- Packages ------------------------------------------------------------

    List<TracerPackage> getPackages() {
        return TracerSupportImpl.getInstance().getPackages(dataSource);
    }


    // --- Probes --------------------------------------------------------------

    void addDescriptor(final TracerPackage<DataSource> p,
                              final TracerProbeDescriptor d) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { addProbe(p, p.getProbe(d)); }
        });
    }

    void removeDescriptor(final TracerPackage<DataSource> p,
                                 final TracerProbeDescriptor d) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { removeProbe(p, d); }
        });
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
                return Positionable.COMPARATOR.compare(o1.getKey(), o2.getKey());
            }
        };
        Set<Map.Entry<TracerPackage, List<TracerProbe>>> probes = new TreeSet(comp);
        synchronized(probesCache) { probes.addAll(probesCache.entrySet()); }
        return probes;
    }

    boolean areProbesDefined() {
        synchronized(probesCache) { return !probesCache.isEmpty(); }
    }


    private void addProbe(TracerPackage<DataSource> p, TracerProbe<DataSource> r) {
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

    private void removeProbe(TracerPackage<DataSource> p, TracerProbeDescriptor d) {
        TracerProbe probe = null;
        boolean probesDefined = true;

        synchronized(probesCache) {
            List<TracerProbe> probes = probesCache.get(p);
            Iterator<TracerProbe> probesI = probes.iterator();
            while (probesI.hasNext()) {
                TracerProbe r = probesI.next();
                if (r.getDescriptor().equals(d)) {
                    probesI.remove();
                    probe = r;
                    break;
                }
            }
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
            ph.probeAdded(r, dataSource);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Package exception in probeAdded", t); // NOI18N
        }

        ProbeStateHandler rh = r.getStateHandler();
        if (rh != null) try {
            rh.probeAdded(dataSource);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Probe exception in probeAdded", t); // NOI18N
        }
    }

    private void notifyProbeRemoved(TracerPackage p, TracerProbe r) {
        PackageStateHandler ph = p.getStateHandler();
        if (ph != null) try {
            ph.probeRemoved(r, dataSource);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Package exception in probeRemoved", t); // NOI18N
        }

        ProbeStateHandler rh = r.getStateHandler();
        if (rh != null) try {
            rh.probeRemoved(dataSource);
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

    private void fireProbeAdded(TracerProbe probe) {
        Set<Listener> toNotify = new HashSet();
        synchronized(listeners) { toNotify.addAll(listeners); }
        for (Listener listener : toNotify) listener.probeAdded(probe);
    }

    private void fireProbeRemoved(TracerProbe probe, boolean probesDefined) {
        Set<Listener> toNotify = new HashSet();
        synchronized(listeners) { toNotify.addAll(listeners); }
        for (Listener listener : toNotify)
            listener.probeRemoved(probe, probesDefined);
    }

    static interface Listener {

        public void probeAdded(TracerProbe probe);

        public void probeRemoved(TracerProbe probe, boolean probesDefined);

    }


    // --- Timeline ------------------------------------------------------------

    TimelineSupport getTimelineSupport() {
        return timelineSupport;
    }

}

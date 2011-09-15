/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.snaptracer.impl;

import java.io.IOException;
import org.netbeans.modules.profiler.snaptracer.PackageStateHandler;
import org.netbeans.modules.profiler.snaptracer.ProbeStateHandler;
import org.netbeans.modules.profiler.snaptracer.TracerPackage;
import org.netbeans.modules.profiler.snaptracer.TracerProbe;
import org.netbeans.modules.profiler.snaptracer.TracerProbeDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.TimelineSupport;
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
import org.netbeans.modules.profiler.snaptracer.Positionable;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerModel {

    private static final Logger LOGGER = Logger.getLogger(TracerController.class.getName());

    private final IdeSnapshot snapshot;

    private final Map<TracerPackage, List<TracerProbe>> probesCache = new HashMap();
    private final Map<TracerProbe, TracerProbeDescriptor> descriptorsCache = new HashMap();

    private final Set<Listener> listeners = new HashSet();

    private final TimelineSupport timelineSupport;


    // --- Constructor ---------------------------------------------------------

    TracerModel(IdeSnapshot snapshot) {
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
            Iterator<Map.Entry<TracerProbe, TracerProbeDescriptor>> iter =
                    descriptorsCache.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<TracerProbe, TracerProbeDescriptor> entry = iter.next();
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

}

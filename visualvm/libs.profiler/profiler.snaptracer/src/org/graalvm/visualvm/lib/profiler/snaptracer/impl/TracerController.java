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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.snaptracer.PackageStateHandler;
import org.graalvm.visualvm.lib.profiler.snaptracer.ProbeItemDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.ProbeStateHandler;
import org.graalvm.visualvm.lib.profiler.snaptracer.SessionInitializationException;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerPackage;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbe;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProgressObject;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
        "Warning_NegativeValue=<html><b>One or more probes "
        + "returned negative value.</b><br><br>Currently this is "
        + "not supported in Tracer,<br>all negative values will be"
        + " displayed as 0.</html>"
    })
public final class TracerController  {

    private static final Logger LOGGER = Logger.getLogger(TracerController.class.getName());

    private static final String PROPERTY_STATE = "state"; // NOI18N
    static final int STATE_SESSION_INACTIVE = 0;
    static final int STATE_SESSION_RUNNING = 1;
    static final int STATE_SESSION_IMPOSSIBLE = -1;
    static final int STATE_SESSION_STARTING = Integer.MAX_VALUE;
    static final int STATE_SESSION_STOPPING = Integer.MIN_VALUE;

    private final TracerModel model;

    private final PropertyChangeSupport changeSupport;
    private int state;

    private TracerProgressObject progress;
    private String error;
    private boolean wasNegativeValue;

    private RequestProcessor processor;


    // --- Constructor ---------------------------------------------------------

    public TracerController(TracerModel model) {
        this.model = model;

        changeSupport = new PropertyChangeSupport(this);
        state = STATE_SESSION_INACTIVE;
    }


    // --- Session & probes state ----------------------------------------------

    private void setState(final int state) {
        Runnable stateSetter = new Runnable() {
            public void run() {
                if (TracerController.this.state == STATE_SESSION_IMPOSSIBLE) return;
                int oldState = TracerController.this.state;
                TracerController.this.state = state;
                changeSupport.firePropertyChange(PROPERTY_STATE, oldState, state);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) stateSetter.run();
        else SwingUtilities.invokeLater(stateSetter);
    }

    int getState() {
        return state;
    }

    TracerProgressObject getProgress() {
        return progress;
    }

    String getErrorMessage() {
        return error;
    }

    void addListener(PropertyChangeListener listener) {
        if (changeSupport != null && state != STATE_SESSION_IMPOSSIBLE)
            changeSupport.addPropertyChangeListener(PROPERTY_STATE, listener);
    }

    void removeListener(PropertyChangeListener listener) {
        if (changeSupport != null)
            changeSupport.removePropertyChangeListener(PROPERTY_STATE, listener);
    }


    // --- Session control -----------------------------------------------------
    
    void performSession() {
        startSession();
        doPerformSession();
        stopSession();
    }

    void performAfterSession(Runnable task) {
        getProcessor().post(task);
    }

    private void startSession() {
        if (!model.areProbesDefined()) return;
        if (doStartSession()) setState(STATE_SESSION_RUNNING);
        else setState(STATE_SESSION_INACTIVE);
    }

    private void stopSession() {
        if (state == STATE_SESSION_RUNNING) setState(STATE_SESSION_STOPPING);
        doStopSession();
        setState(STATE_SESSION_INACTIVE);
    }

    private boolean doStartSession() {
        wasNegativeValue = false;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { model.getTimelineSupport().resetValues(); }
        });
        Set<Map.Entry<TracerPackage, List<TracerProbe>>> toNotify =
                model.getDefinedProbeSets();
        notifySessionInitializing(toNotify);
        setState(STATE_SESSION_STARTING);
        if (!notifySessionStarting(toNotify)) return false;
        notifySessionRunning(toNotify);
        return true;
    }
    
    private void doPerformSession() {
        int samples = model.getSamplesCount();
        for (int i = 0; i < samples; i++) fetchData(i);
    }

    private void doStopSession() {
        Set<Map.Entry<TracerPackage, List<TracerProbe>>> toNotify =
                model.getDefinedProbeSets();
        notifySessionStopping(toNotify);
        notifySessionFinished(toNotify);
    }

    private void notifySessionInitializing(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        List<TracerProgressObject> progresses = new ArrayList<>();
        int steps = 0;
        for (Map.Entry<TracerPackage, List<TracerProbe>> item : items) {
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[0]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                TracerProgressObject c = ph.sessionInitializing(probesArr, null, -1);
                if (c != null) {
                    steps += c.getSteps();
                    progresses.add(c);
                }
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionInitializing", t); // NOI18N
            }

            for (TracerProbe probe : probes) {
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    TracerProgressObject c = rh.sessionInitializing(null, -1);
                    if (c != null) {
                        steps += c.getSteps();
                        progresses.add(c);
                    }
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionInitializing", t); // NOI18N
                }
            }
        }
        if (steps == 0) {
            progress = null;
        } else {
            progress = new TracerProgressObject(steps, "Starting session...");
            TracerProgressObject.Listener l = new TracerProgressObject.Listener() {
                public void progressChanged(int addedSteps, int currentStep, String text) {
                    progress.addSteps(addedSteps, text);
                }
            };
            for (TracerProgressObject o : progresses) o.addListener(l);
        }
        error = null;
    }

    private boolean notifySessionStarting(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        Iterator<Map.Entry<TracerPackage, List<TracerProbe>>> itemsI = items.iterator();
        Map<TracerPackage, List<TracerProbe>> notifiedItems = new HashMap<>();
        String notifiedName = null;
        try {
            while (itemsI.hasNext()) {
                Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
                TracerPackage pkg = item.getKey();
                notifiedName = pkg.getName();
                List<TracerProbe> probes = item.getValue();
                TracerProbe[] probesArr = probes.toArray(new TracerProbe[0]);

                PackageStateHandler ph = pkg.getStateHandler();
                if (ph != null) ph.sessionStarting(probesArr, null);
                List<TracerProbe> notifiedList = new ArrayList<>();
                notifiedItems.put(pkg, notifiedList);

                for (TracerProbe probe : probes) {
                    notifiedName = model.getDescriptor(probe).getProbeName();
                    ProbeStateHandler rh = probe.getStateHandler();
                    if (rh != null) rh.sessionStarting(null);
                    notifiedList.add(probe);
                }
            }
            return true;
        } catch (SessionInitializationException sie) {
            // TODO: update UI
            LOGGER.log(Level.INFO, "Package or probe failed to start Tracer session", sie); // NOI18N
            error = sie.getUserMessage();
            if (error == null) error = notifiedName + " failed to start";

            Set<Map.Entry<TracerPackage, List<TracerProbe>>> notifiedItemsE =
                    notifiedItems.entrySet();
            notifySessionStopping(notifiedItemsE);
            setState(STATE_SESSION_STOPPING);
            notifySessionFinished(notifiedItemsE);

            return false;
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Package or probe exception in sessionStarting", t); // NOI18N
            return true;
            // TODO: ignore or terminate the session as for the SessionInitializationException?
        }
    }

    private void notifySessionRunning(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        for (Map.Entry<TracerPackage, List<TracerProbe>> item : items) {
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[0]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionRunning(probesArr, null);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionRunning", t); // NOI18N
            }

            for (TracerProbe probe : probes) {
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.sessionRunning(null);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionRunning", t); // NOI18N
                }
            }
        }
    }

    private void notifySessionStopping(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        for (Map.Entry<TracerPackage, List<TracerProbe>> item : items) {
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[0]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionStopping(probesArr, null);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionStopping", t); // NOI18N
            }

            for (TracerProbe probe : probes) {
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.sessionStopping(null);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionStopping", t); // NOI18N
                }
            }
        }
    }

    private void notifySessionFinished(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        for (Map.Entry<TracerPackage, List<TracerProbe>> item : items) {
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[0]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionFinished(probesArr, null);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionFinished", t); // NOI18N
            }

            for (TracerProbe probe : probes) {
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.sessionFinished(null);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionFinished", t); // NOI18N
                }
            }
        }
    }

    private void notifyRefreshRateChanged(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        for (Map.Entry<TracerPackage, List<TracerProbe>> item : items) {
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[0]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.refreshRateChanged(probesArr, null, -1);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in refreshRateChanged", t); // NOI18N
            }

            for (TracerProbe probe : probes) {
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.refreshRateChanged(null, -1);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in refreshRateChanged", t); // NOI18N
                }
            }
        }
    }


    // --- Session runtime -----------------------------------------------------

    private synchronized RequestProcessor getProcessor() {
        if (processor == null)
            processor = new RequestProcessor("Tracer Processor for " + model.getSnapshot().toString()); // NOI18N
        return processor;
    }
    
    private void fetchData(final int sampleIndex) {
        
        final List<TracerProbe> probes = model.getDefinedProbes();
        if (probes.isEmpty()) return;
        
        final int itemsCount = model.getTimelineSupport().getItemsCount();
        getProcessor().post(new Runnable() {
            public void run() { fetchDataImpl(probes, itemsCount, sampleIndex); }
        });
    }

    private void fetchDataImpl(List<TracerProbe> probes, int itemsCount, int sampleIndex) {

        final long[] values = new long[itemsCount];
        int currentIndex = 0;

        final long timestamp = model.getTimestamp(sampleIndex);
        
        for (TracerProbe probe : probes) {
            long[] itemValues;
            try {
                itemValues = probe.getItemValues(sampleIndex);
            } catch (Throwable t) {
                itemValues = new long[probe.getItemsCount()];
                Arrays.fill(itemValues, ProbeItemDescriptor.VALUE_UNDEFINED);
                LOGGER.log(Level.INFO, "Probe exception in getItemValues", t); // NOI18N
            }
            for (long value : itemValues) {
                if (value < 0) {
                    if (!wasNegativeValue) {
                        ProfilerDialogs.displayWarning(Bundle.Warning_NegativeValue());
                        LOGGER.info("Probe " + model.getDescriptor(probe).getProbeName() + // NOI18N
                                " returned negative value: " + value); // NOI18N
                        wasNegativeValue = true;
                    }
                    value = 0;
                }
                values[currentIndex++] = value;
            }
        }


        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                
                model.getTimelineSupport().addValues(timestamp, values);
            }
        });

    }


    // --- DataSource & DataSourceView lifecycle -------------------------------

    void viewRemoved() {
        stopSession();
        setState(STATE_SESSION_IMPOSSIBLE);
    }

}

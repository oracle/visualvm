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

package org.graalvm.visualvm.modules.tracer.impl;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.modules.tracer.PackageStateHandler;
import org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;
import org.graalvm.visualvm.modules.tracer.ProbeStateHandler;
import org.graalvm.visualvm.modules.tracer.SessionInitializationException;
import org.graalvm.visualvm.modules.tracer.TracerPackage;
import org.graalvm.visualvm.modules.tracer.TracerProbe;
import org.graalvm.visualvm.modules.tracer.TracerProgressObject;
import org.graalvm.visualvm.modules.tracer.impl.options.TracerOptions;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
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
import javax.swing.Timer;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerController implements DataRemovedListener<DataSource>,
                                        PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(TracerController.class.getName());

    private static final String PROPERTY_STATE = "state"; // NOI18N
    static final int STATE_SESSION_INACTIVE = 0;
    static final int STATE_SESSION_RUNNING = 1;
    static final int STATE_SESSION_IMPOSSIBLE = -1;
    static final int STATE_SESSION_STARTING = Integer.MAX_VALUE;
    static final int STATE_SESSION_STOPPING = Integer.MIN_VALUE;

    private final DataSource dataSource;
    private final TracerModel model;

    private final PropertyChangeSupport changeSupport;
    private int state;

    private TracerProgressObject progress;
    private String error;
    private boolean wasNegativeValue;

    private boolean running;
    private final Timer timer;
    private RequestProcessor processor;


    // --- Constructor ---------------------------------------------------------

    TracerController(TracerModel model) {
        this.model = model;
        dataSource = model.getDataSource();

        Stateful stateful = dataSource instanceof Stateful ?
                            (Stateful) dataSource : null;

        if (stateful == null || stateful.getState() == Stateful.STATE_AVAILABLE) {
            dataSource.notifyWhenRemoved(this);
            dataSource.addPropertyChangeListener(Stateful.PROPERTY_STATE,
                    WeakListeners.propertyChange(this, dataSource));
            changeSupport = new PropertyChangeSupport(this);
            state = STATE_SESSION_INACTIVE;
            timer = createTimer();
        } else {
            changeSupport = null;
            state = STATE_SESSION_IMPOSSIBLE;
            timer = null;
        }
    }


    // --- Session & probes state ----------------------------------------------

    private void setState(final int state) {
        Runnable stateSetter = new Runnable() {
            public void run() {
                if (TracerController.this.state == STATE_SESSION_IMPOSSIBLE) return;
                int oldState = TracerController.this.state;
                TracerController.this.state = state;
                changeSupport.firePropertyChange(PROPERTY_STATE, oldState, state);
                if (state == STATE_SESSION_RUNNING) startTimer();
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

    void setRefreshRate(int refreshRate) {
        if (timer != null && getRefreshRate() != refreshRate) {
            Set<Map.Entry<TracerPackage, List<TracerProbe>>> toNotify =
                model.getDefinedProbeSets();
            notifyRefreshRateChanged(toNotify);
            timer.setDelay(refreshRate);
        }
    }

    int getRefreshRate() {
        return timer != null ? timer.getDelay() : -1;
    }

    void startSession() {
        if (!model.areProbesDefined()) return;
        if (doStartSession()) setState(STATE_SESSION_RUNNING);
        else setState(STATE_SESSION_INACTIVE);
    }

    void stopSession() {
        stopTimer();
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

    private void doStopSession() {
        Set<Map.Entry<TracerPackage, List<TracerProbe>>> toNotify =
                model.getDefinedProbeSets();
        notifySessionStopping(toNotify);
        notifySessionFinished(toNotify);
    }

    private void notifySessionInitializing(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        List<TracerProgressObject> progresses = new ArrayList();
        int steps = 0;
        Iterator<Map.Entry<TracerPackage, List<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[probes.size()]);

            int refresh = getRefreshRate();

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                TracerProgressObject c = ph.sessionInitializing(probesArr, dataSource, refresh);
                if (c != null) {
                    steps += c.getSteps();
                    progresses.add(c);
                }
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionInitializing", t); // NOI18N
            }

            Iterator<TracerProbe> probesI = probes.iterator();
            while (probesI.hasNext()) {
                TracerProbe probe = probesI.next();
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    TracerProgressObject c = rh.sessionInitializing(dataSource, refresh);
                    if (c != null)  {
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
        Map<TracerPackage, List<TracerProbe>> notifiedItems = new HashMap();
        String notifiedName = null;
        try {
            while (itemsI.hasNext()) {
                Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
                TracerPackage pkg = item.getKey();
                notifiedName = pkg.getName();
                List<TracerProbe> probes = item.getValue();
                TracerProbe[] probesArr = probes.toArray(new TracerProbe[probes.size()]);

                PackageStateHandler ph = pkg.getStateHandler();
                if (ph != null) ph.sessionStarting(probesArr, dataSource);
                List<TracerProbe> notifiedList = new ArrayList();
                notifiedItems.put(pkg, notifiedList);

                Iterator<TracerProbe> probesI = probes.iterator();
                while (probesI.hasNext()) {
                    TracerProbe probe = probesI.next();
                    notifiedName = model.getDescriptor(probe).getProbeName();
                    ProbeStateHandler rh = probe.getStateHandler();
                    if (rh != null) rh.sessionStarting(dataSource);
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
        Iterator<Map.Entry<TracerPackage, List<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[probes.size()]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionRunning(probesArr, dataSource);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionRunning", t); // NOI18N
            }

            Iterator<TracerProbe> probesI = probes.iterator();
            while (probesI.hasNext()) {
                TracerProbe probe = probesI.next();
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.sessionRunning(dataSource);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionRunning", t); // NOI18N
                }
            }
        }
    }

    private void notifySessionStopping(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        Iterator<Map.Entry<TracerPackage, List<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[probes.size()]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionStopping(probesArr, dataSource);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionStopping", t); // NOI18N
            }

            Iterator<TracerProbe> probesI = probes.iterator();
            while (probesI.hasNext()) {
                TracerProbe probe = probesI.next();
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.sessionStopping(dataSource);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionStopping", t); // NOI18N
                }
            }
        }
    }

    private void notifySessionFinished(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        Iterator <Map.Entry<TracerPackage, List<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[probes.size()]);

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionFinished(probesArr, dataSource);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in sessionFinished", t); // NOI18N
            }

            Iterator<TracerProbe> probesI = probes.iterator();
            while (probesI.hasNext()) {
                TracerProbe probe = probesI.next();
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.sessionFinished(dataSource);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in sessionFinished", t); // NOI18N
                }
            }
        }
    }

    private void notifyRefreshRateChanged(Set<Map.Entry<TracerPackage, List<TracerProbe>>> items) {
        Iterator<Map.Entry<TracerPackage, List<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, List<TracerProbe>> item = itemsI.next();
            List<TracerProbe> probes = item.getValue();
            TracerProbe[] probesArr = probes.toArray(new TracerProbe[probes.size()]);

            int refresh = getRefreshRate();

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.refreshRateChanged(probesArr, dataSource, refresh);
            } catch (Throwable t) {
                LOGGER.log(Level.INFO, "Package exception in refreshRateChanged", t); // NOI18N
            }

            Iterator<TracerProbe> probesI = probes.iterator();
            while (probesI.hasNext()) {
                TracerProbe probe = probesI.next();
                ProbeStateHandler rh = probe.getStateHandler();
                if (rh != null) try {
                    rh.refreshRateChanged(dataSource, refresh);
                } catch (Throwable t) {
                    LOGGER.log(Level.INFO, "Probe exception in refreshRateChanged", t); // NOI18N
                }
            }
        }
    }


    // --- Session runtime -----------------------------------------------------

    private Timer createTimer() {
        int rate = TracerOptions.getInstance().getRefreshRate();
        Timer t = new Timer(rate, new ActionListener() {
            public void actionPerformed(ActionEvent e) { fetchData(); }
        });
        t.setInitialDelay(0);
        return t;
    }
    
    private void startTimer() {
        if (timer != null) {
            running = true;
            timer.start();
        }
    }

    private void stopTimer() {
        if (timer != null) {
            running = false;
            timer.stop();
        }
    }

    private void fetchData() {
        if (!running) return;

        if (processor == null)
            processor = new RequestProcessor("Tracer Processor for " + dataSource); // NOI18N

        final List<TracerProbe> probes = model.getDefinedProbes();
        final int itemsCount = model.getTimelineSupport().getItemsCount();
        processor.post(new Runnable() {
            public void run() { fetchDataImpl(probes, itemsCount); }
        });
    }

    private void fetchDataImpl(List<TracerProbe> probes, int itemsCount) {
        if (!running) return;

        final long[] values = new long[itemsCount];
        int currentIndex = 0;

        final long timestamp = System.currentTimeMillis();

        for (TracerProbe probe : probes) {
            long[] itemValues;
            try {
                itemValues = probe.getItemValues(timestamp);
            } catch (Throwable t) {
                itemValues = new long[probe.getItemsCount()];
                Arrays.fill(itemValues, ProbeItemDescriptor.VALUE_UNDEFINED);
                LOGGER.log(Level.INFO, "Probe exception in getItemValues", t); // NOI18N
            }
            for (int i = 0; i < itemValues.length; i++) {
                long value = itemValues[i];
                if (value < 0) {
                    if (!wasNegativeValue) {
                        DialogDisplayer.getDefault().notifyLater(
                        new NotifyDescriptor.Message("<html><b>One or more probes "
                        + "returned negative value.</b><br><br>Currently this is "
                        + "not supported in Tracer,<br>all negative values will be"
                        + " displayed as 0.</html>", NotifyDescriptor.WARNING_MESSAGE));
                        LOGGER.info("Probe " + model.getDescriptor(probe).getProbeName() + // NOI18N
                                    " returned negative value: " + value); // NOI18N
                        wasNegativeValue = true;
                    }
                    value = 0;
                }
                values[currentIndex++] = value;
            }
        }

        if (!running) return;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!running) return;
                model.getTimelineSupport().addValues(timestamp, values);
            }
        });

    }


    // --- DataSource & DataSourceView lifecycle -------------------------------

    void viewRemoved() {
        stopSession();
        setState(STATE_SESSION_IMPOSSIBLE);
    }

    public void dataRemoved(DataSource dataSource) {
        stopTimer();
        setState(STATE_SESSION_IMPOSSIBLE);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        stopTimer();
        setState(STATE_SESSION_IMPOSSIBLE);
    }

}

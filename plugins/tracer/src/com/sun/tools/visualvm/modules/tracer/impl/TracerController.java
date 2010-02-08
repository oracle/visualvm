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
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.modules.tracer.PackageStateHandler;
import com.sun.tools.visualvm.modules.tracer.ProbeStateHandler;
import com.sun.tools.visualvm.modules.tracer.SessionInitializationException;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
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
//                if (state == STATE_SESSION_IMPOSSIBLE) {
//                    RequestProcessor.getDefault().post(new Runnable() {
//                        public void run() { notifySessionImpossible(probesCache.entrySet()); }
//                    });
//                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) stateSetter.run();
        else SwingUtilities.invokeLater(stateSetter);
    }

    int getState() {
        return state;
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

    void startSession() {
        if (!model.areProbesDefined()) return;
        setState(STATE_SESSION_STARTING);
        if (doStartSession()) {
            model.getTimelineSupport().resetValues();
            setState(STATE_SESSION_RUNNING);
        }
        else setState(STATE_SESSION_INACTIVE);
    }

    void stopSession() {
        stopTimer();
        if (state == STATE_SESSION_RUNNING) setState(STATE_SESSION_STOPPING);
        doStopSession();
        setState(STATE_SESSION_INACTIVE);
    }

    private boolean doStartSession() {
        Set<Map.Entry<TracerPackage, Set<TracerProbe>>> toNotify =
                model.getDefinedProbeSets();
        if (!notifySessionStarting(toNotify)) return false;
        notifySessionRunning(toNotify);
        return true;
    }

    private void doStopSession() {
        Set<Map.Entry<TracerPackage, Set<TracerProbe>>> toNotify =
                model.getDefinedProbeSets();
        notifySessionStopping(toNotify);
        notifySessionFinished(toNotify);
    }

    private boolean notifySessionStarting(Set<Map.Entry<TracerPackage, Set<TracerProbe>>> items) {
        Iterator<Map.Entry<TracerPackage, Set<TracerProbe>>> itemsI = items.iterator();
        Map<TracerPackage, Set<TracerProbe>> notifiedItems = new HashMap();
        try {
            while (itemsI.hasNext()) {
                Map.Entry<TracerPackage, Set<TracerProbe>> item = itemsI.next();
                TracerPackage pkg = item.getKey();
                Set<TracerProbe> probes = item.getValue();

                PackageStateHandler ph = pkg.getStateHandler();
                if (ph != null) ph.sessionStarting(probes, dataSource);
                Set<TracerProbe> notifiedSet = new HashSet();
                notifiedItems.put(pkg, notifiedSet);

                Iterator<TracerProbe> probesI = probes.iterator();
                while (probesI.hasNext()) {
                    TracerProbe probe = probesI.next();
                    ProbeStateHandler rh = probe.getStateHandler();
                    if (rh != null) rh.sessionStarting(dataSource);
                    notifiedSet.add(probe);
                }
            }
            return true;
        } catch (SessionInitializationException sie) {
            // TODO: update UI
            LOGGER.log(Level.INFO, "Package or probe failed to start Tracer session", sie); // NOI18N

            Set<Map.Entry<TracerPackage, Set<TracerProbe>>> notifiedItemsE =
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

    private void notifySessionRunning(Set<Map.Entry<TracerPackage, Set<TracerProbe>>> items) {
        Iterator<Map.Entry<TracerPackage, Set<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, Set<TracerProbe>> item = itemsI.next();
            Set<TracerProbe> probes = item.getValue();

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionRunning(probes, dataSource);
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

    private void notifySessionStopping(Set<Map.Entry<TracerPackage, Set<TracerProbe>>> items) {
        Iterator<Map.Entry<TracerPackage, Set<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, Set<TracerProbe>> item = itemsI.next();
            Set<TracerProbe> probes = item.getValue();

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionStopping(probes, dataSource);
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

    private void notifySessionFinished(Set<Map.Entry<TracerPackage, Set<TracerProbe>>> items) {
        Iterator <Map.Entry<TracerPackage, Set<TracerProbe>>> itemsI = items.iterator();
        while (itemsI.hasNext()) {
            Map.Entry<TracerPackage, Set<TracerProbe>> item = itemsI.next();
            Set<TracerProbe> probes = item.getValue();

            PackageStateHandler ph = item.getKey().getStateHandler();
            if (ph != null) try {
                ph.sessionFinished(probes, dataSource);
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

//    private void notifySessionImpossible(Set<Map.Entry<TracerPackage, Set<TracerProbe>>> items) {
//        Iterator<Map.Entry<TracerPackage, Set<TracerProbe>>> itemsI = items.iterator();
//        while (itemsI.hasNext()) {
//            Map.Entry<TracerPackage, Set<TracerProbe>> item = itemsI.next();
//            Set<TracerProbe> probes = item.getValue();
//
//            PackageStateHandler ph = item.getKey().getStateHandler();
//            if (ph != null) try {
//                ph.sessionImpossible(probes, dataSource);
//            } catch (Throwable t) {
//                LOGGER.log(Level.FINE, "Package exception in sessionImpossible", t); // NOI18N
//            }
//
//            Iterator<TracerProbe> probesI = probes.iterator();
//            while (probesI.hasNext()) {
//                TracerProbe probe = probesI.next();
//                ProbeStateHandler rh = probe.getStateHandler();
//                if (rh != null) try {
//                    rh.sessionImpossible(dataSource);
//                } catch (Throwable t) {
//                    LOGGER.log(Level.FINE, "Probe exception in sessionImpossible", t); // NOI18N
//                }
//            }
//        }
//    }


    // --- Session runtime -----------------------------------------------------

    private Timer createTimer() {
        int rate = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;
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
            long[] itemValues = probe.getItemValues(timestamp);
            for (int i = 0; i < itemValues.length; i++)
                values[currentIndex++] = itemValues[i];
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

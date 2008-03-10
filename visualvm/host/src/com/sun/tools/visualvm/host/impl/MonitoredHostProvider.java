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
package com.sun.tools.visualvm.host.impl;

import com.sun.tools.visualvm.core.host.*;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import java.util.prefs.PreferenceChangeEvent;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 *
 * @author Jiri Sedlacek
 */
// A provider for MonitoredHostDS
class MonitoredHostProvider extends DefaultDataSourceProvider<MonitoredHostDS> implements DataChangeListener<Host> {

    private class DiscoveryTask implements SchedulerTask {

        private static final int STATE_NEW = 0;
        private static final int STATE_DISCOVERED = 1;
        private Host host;
        private int state = STATE_NEW;

        public DiscoveryTask(Host host) {
            this.host = host;
        }

        public void onSchedule(long timeStamp) {
            switch (state) {
                case STATE_NEW: {
                    if (!MonitoredHostDS.isAvailableFor(host)) {
                        return; // stay in NEW; no jvmstat detected

                    }
                    processNewHost(host);
                    state = STATE_DISCOVERED;
                    break;
                }
                case STATE_DISCOVERED: {
                    if (MonitoredHostDS.isAvailableFor(host)) {
                        // jvmstat detected; no need to remain scheduled -> unschedule
                        removeWatchedHost(host);
                        state = STATE_NEW;
                    }
                    break;
                }
            }
        }
    }
    private final Map<Host, HostListener> mapping = Collections.synchronizedMap(new HashMap<Host, HostListener>());
    private final Map<Host, ScheduledTask> watchedHosts = new HashMap<Host, ScheduledTask>();
    private final DataFinishedListener<Host> hostFinishedListener = new DataFinishedListener<Host>() {

        public void dataFinished(Host host) {
            removeWatchedHost(host);
            processFinishedHost(host);
        }
    };
    final private Object pollIntervalLock = new Object();
    // @GuardedBy pollIntervalLock
    private Quantum pollInterval = null;
    private final PreferenceChangeListener reschedulingListener = new PreferenceChangeListener() {

        public void preferenceChange(PreferenceChangeEvent evt) {
            int seconds = Integer.parseInt(evt.getNewValue());
            synchronized (pollIntervalLock) {
                pollInterval = Quantum.seconds(seconds);
                for (Map.Entry<Host, ScheduledTask> entry : watchedHosts.entrySet()) {
                    entry.getValue().setInterval(pollInterval);
                }
            }
        }
    };

    public void dataChanged(final DataChangeEvent<Host> event) {
        for (Host host : event.getAdded()) {
            addWatchedHost(host, getPollInterval());
        }
    }

    private Quantum getPollInterval() {
        synchronized (pollIntervalLock) {
            if (pollInterval == null) {
                GlobalPreferences.sharedInstance().watchMonitoredHostPoll(reschedulingListener);
                pollInterval = Quantum.seconds(GlobalPreferences.sharedInstance().getMonitoredHostPoll());

            }
        }

        return pollInterval;
    }

    private void processNewHost(final Host host) {
        try {
            MonitoredHostDS monitoredHostDS = new MonitoredHostDS(host);
            host.getRepository().addDataSource(monitoredHostDS);
            registerDataSource(monitoredHostDS);
            final MonitoredHost monitoredHost = monitoredHostDS.getMonitoredHost();
            HostListener monitoredHostListener = new HostListener() {

                public void vmStatusChanged(
                        final VmStatusChangeEvent e) {
                }

                public void disconnected(HostEvent e) {
                    processFinishedHost(host);
                    addWatchedHost(
                            host,
                            getPollInterval());
                }
            };
            mapping.put(host, monitoredHostListener);
            monitoredHost.addHostListener(monitoredHostListener);

            host.notifyWhenFinished(hostFinishedListener);

        } catch (Exception e) {
            // Host doesn't support jvmstat monitoring (jstatd not running)
            // TODO: maybe display a hint that by running jstatd on that host applications can be discovered automatically
        }
    }

    private synchronized void processFinishedHost(final Host host) {
        Set<MonitoredHostDS> monitoredHosts = host.getRepository().getDataSources(MonitoredHostDS.class);
        host.getRepository().removeDataSources(monitoredHosts);
        unregisterDataSources(monitoredHosts);
        for (MonitoredHostDS monitoredHost : monitoredHosts) {
            try {
                monitoredHost.getMonitoredHost().removeHostListener(mapping.get(host));
            } catch (MonitorException ex) {
            }
            mapping.remove(host);
        }
    }

    @Override
    protected <Y extends MonitoredHostDS> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (MonitoredHostDS monitoredHost : removed) {
            monitoredHost.finished();
        }
    }

    private void removeWatchedHost(Host host) {
        synchronized (watchedHosts) {
            if (watchedHosts.containsKey(host)) {
                Scheduler.sharedInstance().unschedule(watchedHosts.remove(host));
            }
        }
    }

    private void addWatchedHost(Host host, Quantum interval) {
        ScheduledTask task = Scheduler.sharedInstance().schedule(new DiscoveryTask(host), Quantum.SUSPENDED);
        synchronized (watchedHosts) {
            watchedHosts.put(host, task);
            task.setInterval(interval);
        }
    }

    void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
        DataSourceRepository.sharedInstance().addDataChangeListener(MonitoredHostProvider.this, Host.class);
    }
}

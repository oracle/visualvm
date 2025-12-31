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

package org.graalvm.visualvm.core.options;

import org.graalvm.visualvm.core.datasupport.ComparableWeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.util.NbPreferences;

/**
 * General VisualVM settings defined in Options.
 *
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
public final class GlobalPreferences implements PreferenceChangeListener {
    private final static Logger LOGGER = Logger.getLogger("org.graalvm.visualvm.core.options");   // NOI18N

    private static final String INT_KEY_MONHOST_POLL = "MonitoredHostPoll"; // NOI18N
    private static final String INT_KEY_THREADS_POLL = "ThreadsPoll";   // NOI18N
    private static final String INT_KEY_MONDATA_POLL = "MonitoredDataPoll"; // NOI18N
    private static final String INT_KEY_MONHOST_CACHE = "MonitoredHostCache";   // NOI18N
    private static final String INT_KEY_MONDATA_CACHE = "MonitoredDataCache";   // NOI18N
    
    private static final String BOOL_KEY_APPS_OPENED = "FinishedAppsOpened";   // NOI18N
    private static final String BOOL_KEY_APPS_SNAPSHOTS = "FinishedAppsSnapshots";   // NOI18N
    
    private final static int MONHOST_POLL_DEFAULT = 3;
    private final static int THREADS_POLL_DEFAULT = 1;
    private final static int MONDATA_POLL_DEFAULT = 1;
    private final static int MONHOST_CACHE_DEFAULT = 60;
    private final static int MONDATA_CACHE_DEFAULT = 60;
    
    private final static boolean APPS_OPENED_DEFAULT = false;
    private final static boolean APPS_SNAPSHOTS_DEFAULT = false;
    
    private final static GlobalPreferences INSTANCE = new GlobalPreferences();
    private final Preferences prefs;
    private final Map<String, Set<ComparableWeakReference<PreferenceChangeListener>>> listenerMap = new HashMap<>();

    private final ExecutorService dispatcher = Executors.newCachedThreadPool();
    
    private GlobalPreferences() {
        prefs =  NbPreferences.forModule(GlobalPreferences.class);
        prefs.addPreferenceChangeListener(this);
    }
    
    /**
     * Returns singleton instance of GlobalPreferences.
     * 
     * @return singleton instance of GlobalPreferences.
     */
    public static GlobalPreferences sharedInstance() {
        return INSTANCE;
    }

    public void preferenceChange(final PreferenceChangeEvent evt) {
        synchronized(listenerMap) {
            Set<ComparableWeakReference<PreferenceChangeListener>> set = listenerMap.get(evt.getKey());
            if (set != null) {
                final Set<PreferenceChangeListener> tmpListeners = new HashSet<>();
                Collection<ComparableWeakReference<PreferenceChangeListener>> deadRefs = new ArrayList<>();
                for(ComparableWeakReference<PreferenceChangeListener> pclRef : set) {
                    if (pclRef.get() != null) {
                        tmpListeners.add(pclRef.get());
                    } else {
                        deadRefs.add(pclRef);
                    }
                }
                set.removeAll(deadRefs);
                dispatcher.submit(new Runnable() {
                    public void run() {
                        for(PreferenceChangeListener pcl : tmpListeners) {
                            pcl.preferenceChange(evt);
                        }
                    }
                });
            }
        }
    }

    /**
     * Returns polling interval for monitored host.
     * 
     * @return polling interval for monitored host.
     */
    public int getMonitoredHostPoll() {
        return getPollingInterval(INT_KEY_MONHOST_POLL, MONHOST_POLL_DEFAULT);
    }
    
    /**
     * Sets polling interval for monitored host.
     * 
     * @param value polling interval for monitored host.
     */
    public void setMonitoredHostPoll(int value) {
        setPollingInterval(INT_KEY_MONHOST_POLL, value);
    }
    
    /**
     * Registers a listener for changes of polling interval for monitored host.
     * 
     * @param pcl listener for changes of polling interval for monitored host.
     */
    public void watchMonitoredHostPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_MONHOST_POLL, pcl);
    }
    
    /**
     * Returns polling interval for threads.
     * 
     * @return polling interval for threads.
     */
    public int getThreadsPoll() {
        return getPollingInterval(INT_KEY_THREADS_POLL, THREADS_POLL_DEFAULT);
    }
    
    /**
     * Sets polling interval for threads.
     * 
     * @param value polling interval for threads.
     */
    public void setThreadsPoll(int value) {
        setPollingInterval(INT_KEY_THREADS_POLL, value);
    }
    
    /**
     * Registers a listener for changes of polling interval for threads.
     * 
     * @param pcl listener for changes of polling interval for threads.
     */
    public void watchThreadsPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_THREADS_POLL, pcl);
    }
    
    /**
     * Returns polling interval for monitored data.
     * 
     * @return polling interval for monitored data.
     */
    public int getMonitoredDataPoll() {
        return getPollingInterval(INT_KEY_MONDATA_POLL, MONDATA_POLL_DEFAULT);
    }
    
    /**
     * Sets polling interval for monitored data.
     * 
     * @param value polling interval for monitored data.
     */
    public void setMonitoredDataPoll(int value) {
        setPollingInterval(INT_KEY_MONDATA_POLL, value);
    }
    
    /**
     * Registers a listener for changes of polling interval for monitored data.
     * 
     * @param pcl listener for changes of polling interval for monitored data.
     */
    public void watchMonitoredDataPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_MONDATA_POLL, pcl);
    }
    
    /**
     * Returns size of cache for monitored host data.
     * 
     * @return size of cache for monitored host data.
     */
    public int getMonitoredHostCache() {
        return getPollingInterval(INT_KEY_MONHOST_CACHE, MONHOST_CACHE_DEFAULT);
    }
    
    /**
     * Sets size of cache for monitored host data.
     * 
     * @param value size of cache for monitored host data.
     */
    public void setMonitoredHostCache(int value) {
        setPollingInterval(INT_KEY_MONHOST_CACHE, value);
    }
    
    /**
     * Registers a listener for changes of size of cache for monitored host data.
     * 
     * @param pcl listener for changes of size of cache for monitored host data.
     */
    public void watchMonitoredHostCache(PreferenceChangeListener pcl) {
        addListener(INT_KEY_MONHOST_CACHE, pcl);
    }
    
    /**
     * Returns size of cache for monitored data.
     * 
     * @return size of cache for monitored data.
     */
    public int getMonitoredDataCache() {
        return getPollingInterval(INT_KEY_MONDATA_CACHE, MONDATA_CACHE_DEFAULT);
    }
    
    /**
     * Sets size of cache for monitored data.
     * 
     * @param value size of cache for monitored data.
     */
    public void setMonitoredDataCache(int value) {
        setPollingInterval(INT_KEY_MONDATA_CACHE, value);
    }
    
    /**
     * Registers a listener for changes of size of cache for monitored data.
     * 
     * @param pcl listener for changes of size of cache for monitored data.
     */
    public void watchMonitoredDataCache(PreferenceChangeListener pcl) {
        addListener(INT_KEY_MONDATA_CACHE, pcl);
    }
    
    /**
     * Returns true if opened applications can be removed automatically when finished.
     * 
     * @return true if opened applications can be removed automatically when finished, false otherwise.
     */
    public boolean autoRemoveOpenedFinishedApps() {
        return prefs.getBoolean(BOOL_KEY_APPS_OPENED, APPS_OPENED_DEFAULT);
    }
    
    /**
     * Sets whether opened applications can be removed automatically when finished.
     * 
     * @param value flag controlling whether opened applications can be removed automatically when finished.
     */
    public void setAutoRemoveOpenedFinishedApps(boolean value) {
        prefs.putBoolean(BOOL_KEY_APPS_OPENED, value);
    }
    
    /**
     * Returns true if applications with snapshots can be removed automatically when finished.
     * 
     * @return true if applications with snapshots can be removed automatically when finished, false otherwise.
     */
    public boolean autoRemoveFinishedAppsWithSnapshots() {
        return prefs.getBoolean(BOOL_KEY_APPS_SNAPSHOTS, APPS_SNAPSHOTS_DEFAULT);
    }
    
    /**
     * Sets whether applications with snapshots can be removed automatically when finished.
     * 
     * @param value flag controlling whether applications with snapshots can be removed automatically when finished.
     */
    public void setAutoRemoveFinishedAppsWithSnapshots(boolean value) {
        prefs.putBoolean(BOOL_KEY_APPS_SNAPSHOTS, value);
    }
    
    /**
     * Persistently stores preferences values. This method is called automatically,
     * typically you don't need to call it explicitly.
     * 
     * @return true if the preferences have been stored successfully, false otherwise.
     */
    public boolean store() {
        try {
            prefs.sync();
            return true;
        } catch (BackingStoreException ex) {
            LOGGER.log(Level.SEVERE, "Error saving preferences", ex);   // NOI18N
        }
        return false;
    }
    
    private void addListener(String property, PreferenceChangeListener pcl) {
        synchronized(listenerMap) {
            if (listenerMap.containsKey(property)) {
                Set<ComparableWeakReference<PreferenceChangeListener>> set = listenerMap.get(property);
                set.add(new ComparableWeakReference<>(pcl));
            } else {
                Set<ComparableWeakReference<PreferenceChangeListener>> set = new HashSet<>();
                set.add(new ComparableWeakReference<>(pcl));
                listenerMap.put(property, set);
            }
        }
    }
    
    private int getPollingInterval(String property, int deflt) {
        int value = -1;
        synchronized (prefs) {
            value = prefs.getInt(property, -1);
            if (value == -1) {
                value = deflt;
                prefs.putInt(property, value);
            }
        }
        return value;
    }
    
    private void setPollingInterval(String property, int value) {
        synchronized(prefs) {
            prefs.putInt(property, value);
        }
    }
    
    
    /**
     * Allows to set or clear persistent do not show again value associated with given notification identified by the
     * provided key.
     *
     * @param key A key that uniquely identifies the notification
     * @param value The value that should be used without displaying the notification or null to clear the Do not show
     *              again (i.e. start displaying the notifications again).
     */
    public void setDoNotShowAgain(String key, String value) {
        ProfilerIDESettings.getInstance().setDoNotShowAgain(key, value);
    }

    /**
     * Allows to get persistent do not show again value associated with given notification identified by the provided key.
     *
     * @param  key A key that uniquely identifies the notification
     * @return The value that should be used without displaying the notification or null if the notification should
     *         be displayed
     */
    public String getDoNotShowAgain(String key) {
        return ProfilerIDESettings.getInstance().getDoNotShowAgain(key);
    }
    
}

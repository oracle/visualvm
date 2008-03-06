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

package com.sun.tools.visualvm.core.options;

import com.sun.tools.visualvm.core.datasupport.ComparableWeakReference;
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
import org.openide.util.NbPreferences;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlobalPreferences implements PreferenceChangeListener {
    private final static Logger LOGGER = Logger.getLogger("com.sun.tools.visualvm.core.options");

    private static final String INT_KEY_MONHOST_POLL = "MonitoredHostPoll";
    private static final String INT_KEY_THREADS_POLL = "ThreadsPoll";
    private static final String INT_KEY_MONDATA_POLL = "MonitoredDataPoll";
    private static final String BOOL_KEY_PROFILER_FILTER = "ProfilerInstrFilter";
    
    private final static int MONHOST_POLL_DEFAULT = 3;
    private final static int THREADS_POLL_DEFAULT = 1;
    private final static int MONDATA_POLL_DEFAULT = 2;
    
    private final static GlobalPreferences INSTANCE = new GlobalPreferences();
    private final Preferences prefs;
    private final Map<String, Set<ComparableWeakReference<PreferenceChangeListener>>> listenerMap = new HashMap<String, Set<ComparableWeakReference<PreferenceChangeListener>>>();

    private final ExecutorService dispatcher = Executors.newCachedThreadPool();
    
    private GlobalPreferences() {
        prefs =  NbPreferences.forModule(GlobalPreferences.class);
        prefs.addPreferenceChangeListener(this);
    }
    
    public static GlobalPreferences sharedInstance() {
        return INSTANCE;
    }

    public void preferenceChange(final PreferenceChangeEvent evt) {
        synchronized(listenerMap) {
            Set<ComparableWeakReference<PreferenceChangeListener>> set = listenerMap.get(evt.getKey());
            if (set != null) {
                final Set<PreferenceChangeListener> tmpListeners = new HashSet<PreferenceChangeListener>();
                Collection<ComparableWeakReference<PreferenceChangeListener>> deadRefs = new ArrayList<ComparableWeakReference<PreferenceChangeListener>>();
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

    public int getMonitoredHostPoll() {
        return getPollingInterval(INT_KEY_MONHOST_POLL, MONHOST_POLL_DEFAULT);
    }
    
    public void setMonitoredHostPoll(int value) {
        setPollingInterval(INT_KEY_MONHOST_POLL, value);
    }
    
    public void watchMonitoredHostPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_MONHOST_POLL, pcl);
    }
    
    public int getThreadsPoll() {
        return getPollingInterval(INT_KEY_THREADS_POLL, THREADS_POLL_DEFAULT);
    }
    
    public void setThreadsPoll(int value) {
        setPollingInterval(INT_KEY_THREADS_POLL, value);
    }
    
    public void watchThreadsPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_THREADS_POLL, pcl);
    }
    
    public int getMonitoredDataPoll() {
        return getPollingInterval(INT_KEY_MONDATA_POLL, MONDATA_POLL_DEFAULT);
    }
    
    public void setMonitoredDataPoll(int value) {
        setPollingInterval(INT_KEY_MONDATA_POLL, value);
    }
    
    public void watchMonitoredDataPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_MONDATA_POLL, pcl);
    }
    
    public boolean isProfilerInstrFilter() {
        synchronized(prefs) {
            return prefs.getBoolean(BOOL_KEY_PROFILER_FILTER, false);
        }
    }
    
    public void setProfilerInstrFilter(boolean value) {
        synchronized(prefs) {
            prefs.put(BOOL_KEY_PROFILER_FILTER, Boolean.toString(value));
        }
    }
    
    public void watchProfilerInstrFilter(PreferenceChangeListener pcl) {
        addListener(BOOL_KEY_PROFILER_FILTER, pcl);
    }
    
    public boolean store() {
        try {
            prefs.sync();
            return true;
        } catch (BackingStoreException ex) {
            LOGGER.log(Level.SEVERE, "Error saving preferences", ex);
        }
        return false;
    }
    
    private void addListener(String property, PreferenceChangeListener pcl) {
        synchronized(listenerMap) {
            if (listenerMap.containsKey(property)) {
                Set<ComparableWeakReference<PreferenceChangeListener>> set = listenerMap.get(property);
                set.add(new ComparableWeakReference<PreferenceChangeListener>(pcl));
            } else {
                Set<ComparableWeakReference<PreferenceChangeListener>> set = new HashSet<ComparableWeakReference<PreferenceChangeListener>>();
                set.add(new ComparableWeakReference<PreferenceChangeListener>(pcl));
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
}

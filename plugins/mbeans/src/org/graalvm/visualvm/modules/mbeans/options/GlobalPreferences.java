/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.mbeans.options;

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
import org.openide.util.NbPreferences;

/**
 *
 * @author Luis-Miguel Alventosa
 */
public class GlobalPreferences implements PreferenceChangeListener {
    private final static Logger LOGGER = Logger.getLogger("org.graalvm.visualvm.modules.mbeans.options"); // NOI18N

    private static final String INT_KEY_PLOTTERS_POLL = "PlottersPoll"; // NOI18N
    private static final int PLOTTERS_POLL_DEFAULT = 4;

    private static final String STRING_KEY_ORDERED_KEY_PROPERTY_LIST = "OrderedKeyPropertyList"; // NOI18N
    private static final String ORDERED_KEY_PROPERTY_LIST_DEFAULT = ""; // NOI18N

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

    public int getPlottersPoll() {
        return getPollingInterval(INT_KEY_PLOTTERS_POLL, PLOTTERS_POLL_DEFAULT);
    }
    
    public void setPlottersPoll(int value) {
        setPollingInterval(INT_KEY_PLOTTERS_POLL, value);
    }
    
    public void watchPlottersPoll(PreferenceChangeListener pcl) {
        addListener(INT_KEY_PLOTTERS_POLL, pcl);
    }
    
    public String getOrderedKeyPropertyList() {
        synchronized(prefs) {
            return prefs.get(STRING_KEY_ORDERED_KEY_PROPERTY_LIST, ORDERED_KEY_PROPERTY_LIST_DEFAULT);
        }
    }
    
    public void setOrderedKeyPropertyList(String value) {
        synchronized(prefs) {
            prefs.put(STRING_KEY_ORDERED_KEY_PROPERTY_LIST, value);
        }
    }
    
    public void watchOrderedKeyPropertyList(PreferenceChangeListener pcl) {
        addListener(STRING_KEY_ORDERED_KEY_PROPERTY_LIST, pcl);
    }

    public boolean store() {
        try {
            prefs.sync();
            return true;
        } catch (BackingStoreException ex) {
            LOGGER.log(Level.SEVERE, "Error saving preferences", ex); // NOI18N
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

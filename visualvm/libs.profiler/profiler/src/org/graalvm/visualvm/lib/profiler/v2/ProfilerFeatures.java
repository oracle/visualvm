/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.v2;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 * Note: all methods excluding constructor and getAvailable() to be called in EDT.
 *
 * @author Jiri Sedlacek
 */
final class ProfilerFeatures {

    private static final String FLAG_SINGLE_FEATURE = "SINGLE_FEATURE"; // NOI18N
    private static final String FLAG_ACTIVATED_FEATURES = "ACTIVATED_FEATURES"; // NOI18N
    private static final String FLAG_PROFILING_POINTS = "PROFILING_POINTS"; // NOI18N

    private static final Boolean SINGLE_FEATURE_DEFAULT = Boolean.TRUE;
    private static final Boolean PROFILING_POINTS_DEFAULT = Boolean.TRUE;

    private static final Comparator<ProfilerFeature> FEATURES_COMPARATOR =
        new Comparator<ProfilerFeature>() {
            public int compare(ProfilerFeature f1, ProfilerFeature f2) {
                return Integer.compare(f1.getPosition(), f2.getPosition());
            }
        };

    private final ProfilerSession session;

    private final Set<ProfilerFeature> features;
    private final Set<ProfilerFeature> activated;

    private final Set<Listener> listeners;
    private final ChangeListener listener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) { fireSettingsChanged(); }
    };

    private boolean singleFeatured;
    private boolean ppoints;


    ProfilerFeatures(ProfilerSession session) {
        this.session = session;
        
        SessionStorage storage = session.getStorage();
        singleFeatured = Boolean.parseBoolean(storage.readFlag(FLAG_SINGLE_FEATURE, SINGLE_FEATURE_DEFAULT.toString()));
        ppoints = Boolean.parseBoolean(storage.readFlag(FLAG_PROFILING_POINTS, PROFILING_POINTS_DEFAULT.toString()));
        
        features = new TreeSet(FEATURES_COMPARATOR);
        activated = new TreeSet(FEATURES_COMPARATOR);
        
        listeners = new HashSet();
        
        // Populates SessionStorage, can be accessed in EDT from now
        for (ProfilerFeature.Provider provider : ProfilerFeature.Registry.getProviders()) {
            ProfilerFeature feature = provider.getFeature(session);
            if (feature != null) features.add(feature);
//            features.add(provider.getFeature(session));
        }
        
        loadActivatedFeatures();
    }
    
    
    Set<ProfilerFeature> getAvailable() {
        return features;
    }
    
    Set<ProfilerFeature> getActivated() {
        assert SwingUtilities.isEventDispatchThread();
        
        return activated;
    }
    
    static Set<ProfilerFeature> getCompatible(Set<ProfilerFeature> f, Lookup c) {
        Set<ProfilerFeature> s = new TreeSet(FEATURES_COMPARATOR);
        for (ProfilerFeature p : f) if (p.supportsConfiguration(c)) s.add(p);
        return s;
    }
    
    void activateFeature(ProfilerFeature feature) {
        assert SwingUtilities.isEventDispatchThread();
        
        if (singleFeatured) {
            if (activated.size() == 1 && activated.contains(feature)) return;
            for (ProfilerFeature f : activated) {
                f.deactivatedInSession();
                f.removeChangeListener(listener);
            }
            activated.clear();
            activated.add(feature);
            feature.addChangeListener(listener);
            feature.activatedInSession();
            fireFeaturesChanged(feature);
            saveActivatedFeatures();
        } else {
            if (activated.add(feature)) {
                ProfilingSettings ps = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
                feature.configureSettings(ps);
                
                Iterator<ProfilerFeature> it = activated.iterator();
                while (it.hasNext()) {
                    ProfilerFeature f = it.next();
                    if (f != feature && !f.supportsSettings(ps)) {
                        it.remove();
                        f.deactivatedInSession();
                        f.removeChangeListener(listener);
                    }
                }
                
                feature.addChangeListener(listener);
                feature.activatedInSession();
                fireFeaturesChanged(feature);
                saveActivatedFeatures();
            }
        }
    }
    
    void deactivateFeature(ProfilerFeature feature) {
        assert SwingUtilities.isEventDispatchThread();
        
        if (activated.size() == 1 && activated.contains(feature) && session.inProgress()) return;
        if (activated.remove(feature)) {
            feature.deactivatedInSession();
            feature.removeChangeListener(listener);
            fireFeaturesChanged(feature);
            saveActivatedFeatures();
        }
    }
    
    void toggleActivated(ProfilerFeature feature) {
        if (activated.contains(feature)) deactivateFeature(feature);
        else activateFeature(feature);
    }
    
    
    private volatile boolean loading;
    
    private void loadActivatedFeatures() {
        loading = true;
        final String _activated = session.getStorage().readFlag(FLAG_ACTIVATED_FEATURES, ""); // NOI18N
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (ProfilerFeature feature : features)
                    if (_activated.contains(getFeatureID(feature)))
                        activateFeature(feature);
                loading = false;
            }
        });
    }
    
    private void saveActivatedFeatures() {
        if (loading) return;
        final Set<ProfilerFeature> _activated = new HashSet(activated);
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                SessionStorage storage = session.getStorage();
                if (_activated.isEmpty()) {
                    storage.storeFlag(FLAG_ACTIVATED_FEATURES, null);
                } else {
                    StringBuilder b = new StringBuilder();
                    for (ProfilerFeature feature : _activated)
                        b.append(getFeatureID(feature));
                    storage.storeFlag(FLAG_ACTIVATED_FEATURES, b.toString());
                }
            }
        });
    }
    
    private static String getFeatureID(ProfilerFeature feature) {
        return "#" + feature.getClass().getName() + "@"; // NOI18N
    }
    
    
    void setSingleFeatured(boolean single) {
        assert SwingUtilities.isEventDispatchThread();
        
        singleFeatured = single;
        
        if (singleFeatured && !activated.isEmpty())
            activateFeature(activated.iterator().next());
        
        session.getStorage().storeFlag(FLAG_SINGLE_FEATURE, SINGLE_FEATURE_DEFAULT.equals(single) ?
                                       null : Boolean.toString(singleFeatured));
    }
    
    boolean isSingleFeatured() {
        assert SwingUtilities.isEventDispatchThread();
        
        return singleFeatured;
    }
    
    
    void setUseProfilingPoints(boolean use) {
        assert SwingUtilities.isEventDispatchThread();
        
        ppoints = use;
        
        session.getStorage().storeFlag(FLAG_PROFILING_POINTS, PROFILING_POINTS_DEFAULT.equals(use) ?
                                       null : Boolean.toString(use));
    }
    
    boolean getUseProfilingPoints() {
        assert SwingUtilities.isEventDispatchThread();
        
        return ppoints;
    }
    
    
    boolean settingsValid() {
        assert SwingUtilities.isEventDispatchThread();
        
        if (activated.isEmpty()) return false;
        
        for (ProfilerFeature f : activated) if (!f.currentSettingsValid()) return false;
        
        return true;
    }
    
    ProfilingSettings getSettings() {
        assert SwingUtilities.isEventDispatchThread();
        
        session.persistStorage(false);
        
        if (activated.isEmpty()) return null;
        
        ProfilingSettings settings = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
        for (ProfilerFeature f : activated) f.configureSettings(settings);
        
        settings.setUseProfilingPoints(ppoints);
        
        return settings;
    }
    
    
    void sessionFinished() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for (ProfilerFeature activated : getActivated())
                    activated.deactivatedInSession();
            }
        });
    }
    
    
    void addListener(Listener listener) {
        assert SwingUtilities.isEventDispatchThread();
        
        listeners.add(listener);
    }
    
    void removeListener(Listener listener) {
        assert SwingUtilities.isEventDispatchThread();
        
        listeners.remove(listener);
    }
    
    private void fireFeaturesChanged(ProfilerFeature changed) {
        boolean valid = settingsValid();
        for (Listener l : listeners) {
            l.featuresChanged(changed);
            l.settingsChanged(valid); // Not necessarily, but ProfilingSettings don't provide equals() to decide
        }
    }
    
    private void fireSettingsChanged() {
        boolean valid = settingsValid();
        for (Listener l : listeners) {
            l.settingsChanged(valid);
        }
    }
    
    
    static abstract class Listener {
        
        abstract void featuresChanged(ProfilerFeature changed);
        
        abstract void settingsChanged(boolean valid);
        
    }
    
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.v2.ProfilerFeature.Provider;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 * Note: all methods excluding constructor and getAvailable() to be called in EDT.
 *
 * @author Jiri Sedlacek
 */
final class ProfilerFeatures {
    
    private static final String FLAG_SINGLE_FEATURE = "SINGLE_FEATURE";
    private static final String FLAG_SELECTED_FEATURES = "SELECTED_FEATURES";
    private static final String FLAG_PROFILING_POINTS = "PROFILING_POINTS";
    
    private static final Comparator<ProfilerFeature> FEATURES_COMPARATOR =
        new Comparator<ProfilerFeature>() {
            public int compare(ProfilerFeature f1, ProfilerFeature f2) {
                return Integer.compare(f1.getPosition(), f2.getPosition());
            }
        };
    
    private final ProfilerSession session;
    
    private final Set<ProfilerFeature> features;
    private final Set<ProfilerFeature> selected;
    
    private final Set<Listener> listeners;
    private final ChangeListener listener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) { fireSettingsChanged(); }
    };
    
    private boolean singleFeature;
    private boolean ppoints;
    
    
    ProfilerFeatures(ProfilerSession session) {
        this.session = session;
        
        SessionStorage storage = session.getStorage();
        singleFeature = Boolean.parseBoolean(storage.loadFlag(FLAG_SINGLE_FEATURE, "true"));
        ppoints = Boolean.parseBoolean(storage.loadFlag(FLAG_PROFILING_POINTS, "true"));
        
        features = new TreeSet(FEATURES_COMPARATOR);
        selected = new TreeSet(FEATURES_COMPARATOR);
        
        listeners = new HashSet();
        
        for (Provider provider : Lookup.getDefault().lookupAll(Provider.class))
            features.addAll(provider.getFeatures(session.getProject()));
        
        loadSelectedFeatures();
    }
    
    
    Set<ProfilerFeature> getAvailable() {
        return features;
    }
    
    Set<ProfilerFeature> getSelected() {
        return selected;
    }
    
    static Set<ProfilerFeature> getCompatible(Set<ProfilerFeature> f, Lookup c) {
        Set<ProfilerFeature> s = new TreeSet(FEATURES_COMPARATOR);
        for (ProfilerFeature p : f) if (p.supportsConfiguration(c)) s.add(p);
        return s;
    }
    
    void selectFeature(ProfilerFeature feature) {
        if (singleFeature) {
            if (selected.size() == 1 && selected.contains(feature)) return;
            for (ProfilerFeature f : selected) {
                f.detachedFromSession(session);
                f.removeChangeListener(listener);
            }
            selected.clear();
            selected.add(feature);
            feature.addChangeListener(listener);
            feature.attachedToSession(session);
            fireFeaturesChanged(feature);
            saveSelectedFeatures();
        } else {
            if (selected.add(feature)) {
                ProfilingSettings ps = new ProfilingSettings();
                feature.configureSettings(ps);
                
                Iterator<ProfilerFeature> it = selected.iterator();
                while (it.hasNext()) {
                    ProfilerFeature f = it.next();
                    if (f != selected && !f.supportsSettings(ps)) it.remove();
                }
                
                feature.addChangeListener(listener);
                feature.attachedToSession(session);
                fireFeaturesChanged(feature);
                saveSelectedFeatures();
            }
        }
    }
    
    void deselectFeature(ProfilerFeature feature) {
        if (selected.size() == 1 && selected.contains(feature) && session.inProgress()) return;
        if (selected.remove(feature)) {
            feature.detachedFromSession(session);
            feature.removeChangeListener(listener);
            fireFeaturesChanged(feature);
            saveSelectedFeatures();
        }
    }
    
    void toggleFeatureSelection(ProfilerFeature feature) {
        if (selected.contains(feature)) deselectFeature(feature);
        else selectFeature(feature);
    }
    
    private boolean loading;
    
    private void loadSelectedFeatures() {
        loading = true;
        String _selected = session.getStorage().loadFlag(FLAG_SELECTED_FEATURES, "");
        for (ProfilerFeature feature : features)
            if (_selected.contains("#" + feature.getClass().getName() + "@"))
                selectFeature(feature);
        loading = false;
    }
    
    private void saveSelectedFeatures() {
        if (loading) return;
        final Set<ProfilerFeature> _selected = new HashSet(selected);
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                SessionStorage storage = session.getStorage();
                if (_selected.isEmpty()) {
                    storage.saveFlag(FLAG_SELECTED_FEATURES, null);
                } else {
                    StringBuilder b = new StringBuilder();
                    for (ProfilerFeature feature : _selected)
                        b.append("#" + feature.getClass().getName() + "@");
                    storage.saveFlag(FLAG_SELECTED_FEATURES, b.toString());
                }
            }
        });
    }
    
    
    void setSingleFeatureSelection(boolean single) {
        singleFeature = single;
        if (singleFeature && !selected.isEmpty())
            selectFeature(selected.iterator().next());
        session.getStorage().saveFlag(FLAG_SINGLE_FEATURE, Boolean.toString(singleFeature));
    }
    
    boolean isSingleFeatureSelection() {
        return singleFeature;
    }
    
    
    void setUseProfilingPoints(boolean use) {
        ppoints = use;
        session.getStorage().saveFlag(FLAG_PROFILING_POINTS, Boolean.toString(use));
    }
    
    boolean getUseProfilingPoints() {
        return ppoints;
    }
    
    
    boolean settingsValid() {
        if (selected.isEmpty()) return false;
        
        for (ProfilerFeature f : selected) if (!f.settingsValid()) return false;
        
        return true;
    }
    
    ProfilingSettings getSettings() {
        if (selected.isEmpty()) return null;
        
        ProfilingSettings settings = new ProfilingSettings();
        for (ProfilerFeature f : selected) f.configureSettings(settings);
        
        settings.setUseProfilingPoints(ppoints);
        
        return settings;
    }
    
    
    void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    void removeListener(Listener listener) {
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

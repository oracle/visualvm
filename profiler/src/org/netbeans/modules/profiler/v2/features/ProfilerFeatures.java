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

package org.netbeans.modules.profiler.v2.features;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.v2.session.ProjectSession;

/**
 * NOTE: All methods must be invoked from the EDT except of forSession().
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerFeatures {
    
    // Must be called outside of EDT
    public static ProfilerFeatures forSession(ProjectSession session) {
        return new ProfilerFeatures(session);
    }
    
    
    private final ProjectSession session;
    
    private final Set<ProfilerFeature> features;
    private final Set<ProfilerFeature> selected;
    
    private final Set<ChangeListener> listeners;
    private final ChangeListener listener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) { fireChange(); }
    };
    
    private boolean singleFeature;
    
    private boolean ppoints;
    
    
    private ProfilerFeatures(ProjectSession session) {
        this.session = session;
        
        singleFeature = true; // TODO: read last state
        ppoints = true; // TODO: read last state
        
        Comparator<ProfilerFeature> comparator = new Comparator<ProfilerFeature>() {
            public int compare(ProfilerFeature f1, ProfilerFeature f2) {
                return Integer.compare(f1.getPosition(), f2.getPosition());
            }
        };
        
        features = new TreeSet(comparator);
        selected = new TreeSet(comparator);
        
        listeners = new HashSet();
        
        popuplateFeatures(features);
    }
    
    
    public Set<ProfilerFeature> getFeatures() {
        return features;
    }
    
    public Set<ProfilerFeature> getSelectedFeatures() {
        return selected;
    }
    
    public void selectFeature(ProfilerFeature feature) {
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
            fireChange();
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
                fireChange();
            }
        }
    }
    
    public void deselectFeature(ProfilerFeature feature) {
        if (selected.size() == 1 && selected.contains(feature) && session.inProgress()) return;
        if (selected.remove(feature)) {
            feature.detachedFromSession(session);
            feature.removeChangeListener(listener);
            fireChange();
        }
    }
    
    public void toggleFeatureSelection(ProfilerFeature feature) {
        if (selected.contains(feature)) deselectFeature(feature);
        else selectFeature(feature);
    }
    
    
    public void setSingleFeatureSelection(boolean single) {
        singleFeature = single;
        if (singleFeature && !selected.isEmpty())
            selectFeature(selected.iterator().next());
    }
    
    public boolean isSingleFeatureSelection() {
        return singleFeature;
    }
    
    
    public void setUseProfilingPoints(boolean use) {
        ppoints = use;
    }
    
    public boolean getUseProfilingPoints() {
        return ppoints;
    }
    
    
    public ProfilingSettings getSettings() {
        if (selected.isEmpty()) return null;
        
        ProfilingSettings settings = new ProfilingSettings();
        for (ProfilerFeature f : selected) f.configureSettings(settings);
        
        settings.setUseProfilingPoints(ppoints);
        
        return settings;
    }
    
    
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void fireChange() {
        if (listeners.isEmpty()) return;
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener listener : listeners) listener.stateChanged(e);
    }
    
    
    private void popuplateFeatures(Set set) {
        // TODO: read Feature settings
        set.add(new CPUFeature());
        set.add(new MemoryFeature());
        set.add(new MonitorFeature());
        set.add(new ThreadsFeature());
        set.add(new LocksFeature());
    }
    
}

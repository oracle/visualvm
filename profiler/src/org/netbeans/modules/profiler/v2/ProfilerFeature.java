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

import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerFeature {
    
    public abstract Icon getIcon();
    
    public abstract String getName();
    
    public abstract String getDescription();
    
    public abstract int getPosition();
    
    
    // To be called in EDT
    public abstract JPanel getResultsUI();
    
    // To be called in EDT
    public abstract JPanel getSettingsUI();
    
    // To be called in EDT
    public abstract ProfilerToolbar getToolbar();
    
    
    public abstract boolean supportsSettings(ProfilingSettings settings);
    
    // To be called in EDT
    public abstract void configureSettings(ProfilingSettings settings);
    
    // To be called in EDT
    public abstract boolean currentSettingsValid();
    
    
    public abstract boolean supportsConfiguration(Lookup configuration);
    
    // To be called in EDT
    public abstract void configure(Lookup configuration);
    
    
    protected void activatedInSession() {}
    
    protected void deactivatedInSession() {}
    
    
    public abstract void addChangeListener(ChangeListener listener);
    
    public abstract void removeChangeListener(ChangeListener listener);
    
    
    public static abstract class Basic extends ProfilerFeature {
        
        private Set<ChangeListener> listeners;
        
        private final Icon icon;
        private final String name;
        private final String description;
        private final int position;
        
        private final ProfilerSession session;
        
        private volatile boolean isActive;
        
        
        public Basic(Icon icon, String name, String description, int position, ProfilerSession session) {
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.position = position;
            
            this.session = session;
        }
        
        public final Icon getIcon() { return icon; }
        
        public final String getName() { return name; }
    
        public final String getDescription() { return description; }
        
        public final int getPosition() { return position; }
        
        
        protected final ProfilerSession getSession() { return session; }
        
        
        public JPanel getSettingsUI() { return null; }
        
        public boolean supportsSettings(ProfilingSettings settings) { return true; }
        
        public boolean currentSettingsValid() { return true; }
        
        
        public ProfilerToolbar getToolbar() { return null; }
        
        
        public boolean supportsConfiguration(Lookup configuration) { return false; }
    
        public void configure(Lookup configuration) {}
        
        
        protected void notifyActivated() {}
        
        protected void notifyDeactivated() {}
        
        protected final boolean isActivated() { return isActive; }
        
        
        protected final void activatedInSession() {
            isActive = true;
            
            notifyActivated();
            
            session.addListener(getListener());
            
            final int state = session.getState();
            Runnable notifier = new Runnable() {
                public void run() { profilingStateChanged(-1, state); }
            };
            UIUtils.runInEventDispatchThread(notifier);
        }
    
        protected final void deactivatedInSession() {
            isActive = false;
            
            notifyDeactivated();
            
            session.removeListener(getListener());
            listener = null;
            
            final int state = Profiler.PROFILING_INACTIVE;
            Runnable notifier = new Runnable() {
                public void run() { profilingStateChanged(-1, state); }
            };
            UIUtils.runInEventDispatchThread(notifier);
        }
        
        protected final int getSessionState() {
            return isActive ? session.getState() : Profiler.PROFILING_INACTIVE;
        }
        
        
        protected final String readFlag(String flag, String defaultValue) {
            String id = getClass().getName();
            return session.getStorage().readFlag(id + "_" + flag, defaultValue); // NOI18N
        }
        
        protected final void storeFlag(String flag, String value) {
            String id = getClass().getName();
            session.getStorage().storeFlag(id + "_" + flag, value); // NOI18N
        }
        
        
        private ProfilingStateListener listener;
        private ProfilingStateListener getListener() {
            if (listener == null) listener = new ProfilingStateListener() {
                public void serverStateChanged(int serverState, int serverProgress) {
                    if (!isActive) return;
                    Basic.this.serverStateChanged(serverState, serverProgress);
                }
                public void instrumentationChanged(int oldType, int newType) {
                    if (!isActive) return;
                    Basic.this.instrumentationChanged(oldType, newType);
                }
                public void profilingStateChanged(ProfilingStateEvent e) {
                    if (!isActive) return;
                    Basic.this.profilingStateChanged(e.getOldState(), e.getNewState());
                }
                public void threadsMonitoringChanged() {
                    if (!isActive) return;
                    Basic.this.threadsMonitoringChanged();
                }
                public void lockContentionMonitoringChanged() {
                    if (!isActive) return;
                    Basic.this.lockContentionMonitoringChanged();
                }
            };
            return listener;
        }
        
        protected void serverStateChanged(int serverState, int serverProgress) {}
        
        protected void instrumentationChanged(int oldType, int newType) {}

        protected void profilingStateChanged(int oldState, int newState) {}

        protected void threadsMonitoringChanged() {}

        protected void lockContentionMonitoringChanged() {}
        
        
        public synchronized final void addChangeListener(ChangeListener listener) {
            if (listeners == null) listeners = new HashSet();
            listeners.add(listener);
        }
    
        public synchronized final void removeChangeListener(ChangeListener listener) {
            if (listeners != null) listeners.remove(listener);
        }
        
        protected synchronized final void fireChange() {
            if (listeners == null) return;
            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener listener : listeners) listener.stateChanged(e);
        }
        
    }
    
    // --- Provider ------------------------------------------------------------
    
    public static abstract class Provider {
        
        public abstract ProfilerFeature getFeature(ProfilerSession session);
        
    }
    
}

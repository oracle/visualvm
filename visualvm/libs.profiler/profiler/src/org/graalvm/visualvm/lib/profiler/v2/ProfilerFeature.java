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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.event.ProfilingStateEvent;
import org.graalvm.visualvm.lib.common.event.ProfilingStateListener;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
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
    
    
    // --- Registry ------------------------------------------------------------
    
    public static final class Registry {
        
        private static boolean HAS_PROVIDERS;
        
        private Registry() {}
        
        public static boolean hasProviders() {
            return HAS_PROVIDERS;
        }
        
        static Collection<? extends Provider> getProviders() {
            Collection<? extends Provider> providers = Lookup.getDefault().lookupAll(Provider.class);
            HAS_PROVIDERS = !providers.isEmpty();
            return providers;
        }
        
    }
    
}

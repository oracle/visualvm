/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.AttachSettings;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.common.event.ProfilingStateListener;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.actions.ResetResultsAction;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerSession {

    // --- Static access -------------------------------------------------------

    private static ProfilerSession CURRENT_SESSION;
    private static final Object CURRENT_SESSION_LOCK = new Object();


    public static ProfilerSession currentSession() {
        synchronized(CURRENT_SESSION_LOCK) {
            return CURRENT_SESSION;
        }
    }

    public static ProfilerSession forContext(Lookup context) {
        synchronized(CURRENT_SESSION_LOCK) {
            if (CURRENT_SESSION != null) {
                if (CURRENT_SESSION.isCompatibleContext(context)) {
                    // Reuse the compatible active session
                    CURRENT_SESSION.setContext(context);
                    return CURRENT_SESSION;
                } else {
                    // Close the incompatible active session
                    if (!CURRENT_SESSION.close()) return null;
                }
            }
        }

        if (!ProfilerSessions.waitForProfiler()) return null;

        // Create a new session, will eliminate another session when showing UI
        Provider provider = Lookup.getDefault().lookup(Provider.class);
        ProfilerSession session = provider == null ? null : provider.createSession(context);

        synchronized (CURRENT_SESSION_LOCK) { CURRENT_SESSION = session; }
            
        notifyStopAction();

        return session;
        
    };
    
    
    public static void findAndConfigure(Lookup context, Lookup.Provider project, String actionName) {
        ProfilerSession current = currentSession();
        if (current != null) ProfilerSessions.configure(current, context, actionName);
        else ProfilerSessions.createAndConfigure(context, project, actionName);
    }
    
    
    // --- Constructor ---------------------------------------------------------
    
    protected ProfilerSession(Profiler _profiler, Lookup context) {
        if (_profiler == null) throw new IllegalArgumentException("Profiler cannot be null"); // NOI18N
        
        profiler = _profiler;
        setContext(context);
    }
    
    
    // --- Context -------------------------------------------------------------
    
    private Lookup context;
    
    
    protected synchronized final Lookup getContext() { return context; }
    
    private final void setContext(Lookup _context) {
        synchronized(this) { context = _context; }
        notifyWindow();
    }
    
    
    // --- SPI -----------------------------------------------------------------
    
    // Called in EDT, return false for start failure
    protected abstract boolean start();
    
    // Called in EDT, return false for modify failure
    protected abstract boolean modify();
    
    // Called in EDT, return false for termination failure
    protected abstract boolean stop();
    
    
    public abstract Lookup.Provider getProject();
    
    public abstract FileObject getFile();
    
    
    protected abstract boolean isCompatibleContext(Lookup context);
    
    
    // --- API -----------------------------------------------------------------
    
    private final Profiler profiler;
    private ProfilerWindow window;
    
    private ProfilingSettings profilingSettings;
    private AttachSettings attachSettings;
    
    private boolean isAttach;
    
    private SessionStorage storage;
    
    
    public final void setAttach(final boolean attach) {
        synchronized (this) { if (attach == isAttach) return; }
        
        boolean sessionInProgress = inProgress();
        if (sessionInProgress && !confirmedStop()) return;
        
        Runnable updater = new Runnable() {
            public void run() {
                synchronized (ProfilerSession.this) { isAttach = attach; }
                notifyStopAction();
                notifyWindow();
            }
        };
        
        if (!sessionInProgress) updater.run();
        else ProfilerUtils.runInProfilerRequestProcessor(updater);
    }
    
    // Set when configuring profiling session, not a persistent storage!
    public synchronized final boolean isAttach() { return isAttach; }
    
    
    public final Profiler getProfiler() { return profiler; }    
    
    // Set when starting/modifying profiling session, not a persistent storage!
    public final ProfilingSettings getProfilingSettings() { return profilingSettings; }
    
    // Set when starting profiling session, not a persistent storage!
    public final AttachSettings getAttachSettings() { return attachSettings; }
    
    public final synchronized SessionStorage getStorage() {
        if (storage == null) storage = new SessionStorage(getProject());
        return storage;
    }
    
    
    public final void open() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                ProfilerWindow w = getWindow();
                w.open();
                w.requestActive();
            }
        });
    };
    
    
    // --- Profiler API bridge -------------------------------------------------
    
    private final Set<ProfilingStateListener> profilingStateListeners = new HashSet();
    
    
    public final int getState() {
        return profiler.getProfilingState();
    }
    
    public final boolean inProgress() {
        return getState() != Profiler.PROFILING_INACTIVE;
    }
    
    public final void addListener(ProfilingStateListener listener) {
        synchronized (profiler) {
            profiler.addProfilingStateListener(listener);
            profilingStateListeners.add(listener);
        }
    }
    
    public final void removeListener(ProfilingStateListener listener) {
        synchronized (profiler) {
            profiler.removeProfilingStateListener(listener);
            profilingStateListeners.remove(listener);
        }
    }
    
    
    private final void cleanupAllListeners() {
        synchronized (profiler) {
            for (ProfilingStateListener listener : profilingStateListeners)
                profiler.removeProfilingStateListener(listener);
        }
    }
    
    // --- Internal API --------------------------------------------------------
    
    private ProfilerFeatures features;
    private ProfilerPlugins plugins;
    
    final boolean doStart(ProfilingSettings pSettings, AttachSettings aSettings) {
        profilingSettings = pSettings;
        attachSettings = aSettings;
        plugins.notifyStarting();
        return start();
    }
    
    final boolean doModify(ProfilingSettings pSettings) {
        profilingSettings = pSettings;
        return modify();
    }
    
    final boolean doStop() {
        plugins.notifyStopping();
        return stop();
    }
    
    private final boolean confirmedStop() {
        if (inProgress()) {
            if (!ProfilerDialogs.displayConfirmation(Bundle.ProfilerWindow_terminateMsg(),
                                                Bundle.ProfilerWindow_terminateCaption()))
                return false;
            if (!doStop()) return false;
        }
        
        return true;
    }
    
    final boolean close() {
        if (!confirmedStop()) return false;
        
        synchronized (CURRENT_SESSION_LOCK) {
            if (CURRENT_SESSION == this) CURRENT_SESSION = null;
        }
        
        notifyStopAction();
        
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (window != null) {
                    if (!window.closing && window.isOpened()) {
                        window.closing = true;
                        window.close(); // calls session.cleanup()
                    }
                } else {
                    cleanup();
                }
            }
        });
        
        return true;
    }
    
    final ProfilerFeatures getFeatures() {
        assert !SwingUtilities.isEventDispatchThread();
        
        synchronized(this) { if (features == null) features = new ProfilerFeatures(this); }
        
        return features;
    }
    
    final void selectFeature(final ProfilerFeature feature) {
        Runnable task = new Runnable() {
            public void run() { getWindow().selectFeature(feature); }
        };
        UIUtils.runInEventDispatchThread(task);
    }
    
    final ProfilerPlugins getPlugins() {
        assert !SwingUtilities.isEventDispatchThread();
        
        synchronized(this) { if (plugins == null) plugins = new ProfilerPlugins(this); }
        
        return plugins;
    }
    
    final synchronized void persistStorage(boolean immediately) {
        if (storage != null) storage.persist(immediately);
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private ProfilerWindow getWindow() {
        assert SwingUtilities.isEventDispatchThread();
        
        if (window == null) {
            window = new ProfilerWindow(ProfilerSession.this) {
                protected void componentClosed() {
                    super.componentClosed();
                    window = null;
                    cleanup();
                }
            };
        }
        return window;
    }
    
    private void notifyWindow() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() { if (window != null) window.updateSession(); }
        });
    }
    
    private void cleanup() {
        synchronized(this) { if (features != null) features.sessionFinished(); }
        
        cleanupAllListeners();
        
        // Note: should call profiler.resetAllResults() once implemented
        ResetResultsAction.getInstance().performAction();
        
        persistStorage(false);
    }
    
    private static void notifyStopAction() {
        final ProfilerSession CURRENT_SESSION_F;
        synchronized (CURRENT_SESSION_LOCK) { CURRENT_SESSION_F = CURRENT_SESSION; }
        
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() { ProfilerSessions.StopAction.getInstance().setSession(CURRENT_SESSION_F); }
        });
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    public static abstract class Provider {
        
        public abstract ProfilerSession createSession(Lookup context);
        
    }
    
}

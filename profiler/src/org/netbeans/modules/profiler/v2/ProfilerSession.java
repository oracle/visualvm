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
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
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

        synchronized(CURRENT_SESSION_LOCK) {
            CURRENT_SESSION = session;
            notifyStopAction();
        }

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
    
    
    public final void setAttach(boolean attach) {
        synchronized(this) { isAttach = attach; }
        notifyWindow();
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
    
    final boolean close() {
        if (inProgress()) {
            if (!ProfilerDialogs.displayConfirmation(Bundle.ProfilerWindow_terminateMsg(),
                                                Bundle.ProfilerWindow_terminateCaption()))
                return false;
            if (!doStop()) return false;
        }
        
        synchronized(CURRENT_SESSION_LOCK) {
            if (CURRENT_SESSION == this) {
                CURRENT_SESSION = null;
                notifyStopAction();
            }
        }
        
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
        
        persistStorage(false);
    }
    
    private static void notifyStopAction() {
        final ProfilerSession CURRENT_SESSION_F = CURRENT_SESSION;
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() { ProfilerSessions.StopAction.getInstance().setSession(CURRENT_SESSION_F); }
        });
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    public static abstract class Provider {
        
        public abstract ProfilerSession createSession(Lookup context);
        
    }
    
}

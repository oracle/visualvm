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

import java.util.Objects;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.ui.UIUtils;
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
        // Try to reuse the active session first
        synchronized(CURRENT_SESSION_LOCK) {
            if (CURRENT_SESSION != null && CURRENT_SESSION.isCompatibleContext(context)) {
                CURRENT_SESSION.setContext(context);
                return CURRENT_SESSION;
            }
        }
        
        // Create a new session, will eliminate another session when showing UI
        Provider provider = Lookup.getDefault().lookup(Provider.class);
        return provider == null ? null : provider.getSession(context);
    };
    
    
    public static void findAndConfigure(Lookup context, String actionName) {
        ProfilerSession current = currentSession();
        if (current != null) ProfilerSessions.configure(current, context, actionName);
        else ProfilerSessions.createAndConfigure(context, actionName);
    }
    
    
    // --- Constructor ---------------------------------------------------------
    
    protected ProfilerSession(Profiler _profiler, Lookup context) {
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
    
    protected synchronized boolean isCompatibleContext(Lookup _context) {
        return Objects.equals(getProject(), _context.lookup(Lookup.Provider.class));
    }
    
    
    // --- SPI -----------------------------------------------------------------
    
    // Called in EDT, return false for start failure
    protected abstract boolean start();
    
    // Called in EDT, return false for modify failure
    protected abstract boolean modify();
    
    // Called in EDT, return false for termination failure
    protected abstract boolean terminate();
    
    
    public abstract Lookup.Provider getProject();
    
    public abstract FileObject getFile();
    
    
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
    
    
    public final void requestActive() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                synchronized(CURRENT_SESSION_LOCK) {
                    if (CURRENT_SESSION != null && CURRENT_SESSION != ProfilerSession.this) {
                        ProfilerWindow w = CURRENT_SESSION.window;
                        if (w != null && !w.close()) return;
                    }
                }

                ProfilerWindow w = getWindow();
                w.open();
                w.requestActive();

                synchronized(CURRENT_SESSION_LOCK) {
                    CURRENT_SESSION = ProfilerSession.this;
                }
            }
        });
    };
    
    
    // --- Profiler API bridge -------------------------------------------------
    
    public final int getState() {
        return profiler.getProfilingState();
    }
    
    public final boolean inProgress() {
        return getState() != Profiler.PROFILING_INACTIVE;
    }
    
    public final void addListener(ProfilingStateListener listener) {
        profiler.addProfilingStateListener(listener);
    }
    
    public final void removeListener(ProfilingStateListener listener) {
        profiler.removeProfilingStateListener(listener);
    }
    
    
    // --- Internal API --------------------------------------------------------
    
    private ProfilerFeatures features;
    
    final boolean doStart(ProfilingSettings pSettings, AttachSettings aSettings) {
        profilingSettings = pSettings;
        attachSettings = aSettings;
        return start();
    }
    
    final boolean doModify(ProfilingSettings pSettings) {
        profilingSettings = pSettings;
        return modify();
    }
    
    final boolean doTerminate() {
        return terminate();
    }
    
    final ProfilerFeatures getFeatures() {
        assert !SwingUtilities.isEventDispatchThread();
        
        synchronized(this) {
            if (features == null) features = new ProfilerFeatures(this);
        }
        
        return features;
    }
    
    final void selectFeature(final ProfilerFeature feature) {
        Runnable task = new Runnable() {
            public void run() { getWindow().selectFeature(feature); }
        };
        UIUtils.runInEventDispatchThread(task);
    }
    
    final synchronized SessionStorage getStorage() {
        if (storage == null) storage = new SessionStorage(getProject());
        return storage;
    }
    
    final synchronized void persistStorage() {
        if (storage != null) storage.persist();
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private ProfilerWindow getWindow() {
        assert SwingUtilities.isEventDispatchThread();
        
        if (window == null) {
            window = new ProfilerWindow(ProfilerSession.this) {
                protected void componentClosed() {
                    super.componentClosed();
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
        synchronized(CURRENT_SESSION_LOCK) {
            if (CURRENT_SESSION == this) CURRENT_SESSION = null;
        }
        
        persistStorage();
        
        // TODO: unregister listeners (this.addListener) to prevent memory leaks
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    public static abstract class Provider {
        
        public abstract ProfilerSession getSession(Lookup context);
        
    }
    
}

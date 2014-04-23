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
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerSession {
    
    // --- Static access -------------------------------------------------------
    
    private static ProfilerSession ACTIVE_SESSION;
    private static final Object ACTIVE_SESSION_LOCK = new Object();
    
    
    public static ProfilerSession forContext(Lookup context) {
        // Try to reuse the active session first
        synchronized(ACTIVE_SESSION_LOCK) {
            if (ACTIVE_SESSION != null && ACTIVE_SESSION.isCompatibleContext(context)) {
                ACTIVE_SESSION.setContext(context);
                return ACTIVE_SESSION;
            }
        }
        
        // Create a new session, will eliminate another session when showing UI
        Provider provider = Lookup.getDefault().lookup(Provider.class);
        ProfilerSession session = provider == null ? null : provider.getSession(context);
        return session;
    };
    
//    public static ProfilerSession activeSession() {
//        synchronized(ACTIVE_SESSION_LOCK) {
//            return ACTIVE_SESSION;
//        }
//    }
    
    // --- Constructor ---------------------------------------------------------
    
    protected ProfilerSession(NetBeansProfiler _profiler, Lookup context) {
        profiler = _profiler;
        setContext(context);
    }
    
    // --- Context -------------------------------------------------------------
    
    private Lookup context;
    
    
    protected synchronized final Lookup getContext() { return context; }
    
    private final void setContext(Lookup _context) {
        synchronized(this) { context = _context; }
        
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() { if (window != null) window.updateSession(); }
        });
    }
    
    protected synchronized boolean isCompatibleContext(Lookup _context) {
        return Objects.equals(getProject(), _context.lookup(Lookup.Provider.class));
    }
    
    // --- SPI -----------------------------------------------------------------
    
    protected abstract void start();
    
    protected abstract void modify();
    
    protected abstract void terminate();
    
    // --- API -----------------------------------------------------------------
    
    private final NetBeansProfiler profiler;
    private ProfilerWindow window;
    
    private ProfilingSettings profilingSettings;
    private AttachSettings attachSettings;
    
    
    public final NetBeansProfiler getProfiler() { return profiler; }
    
    public abstract Lookup.Provider getProject();
    
    public abstract FileObject getFile();
    
    public final ProfilingSettings getProfilingSettings() { return profilingSettings; }
    
    public final AttachSettings getAttachSettings() { return attachSettings; }
    
    public final void requestActive() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                synchronized(ACTIVE_SESSION_LOCK) {
                    if (ACTIVE_SESSION != null && ACTIVE_SESSION != ProfilerSession.this) {
                        ProfilerWindow w = ACTIVE_SESSION.window;
                        if (w != null && !w.close()) return;
                    }
                }

                if (window == null) {
                    window = new ProfilerWindow(ProfilerSession.this) {
                        protected void componentClosed() {
                            super.componentClosed();
                            cleanup();
                        }
                    };
                    window.open();
                    window.requestActive();
                }

                synchronized(ACTIVE_SESSION_LOCK) {
                    ACTIVE_SESSION = ProfilerSession.this;
                }
            }
        });
    };
    
    public final void configure(ClientUtils.SourceCodeSelection selection) {}
    
    // --- Profiler API bridge -------------------------------------------------
    
    public final int getState() {
        return profiler.getProfilingState();
    }
    
    public final boolean inProgress() {
        return getState() != NetBeansProfiler.PROFILING_INACTIVE;
    }
    
    public final void addListener(ProfilingStateListener listener) {
        profiler.addProfilingStateListener(listener);
    }
    
    public final void removeListener(ProfilingStateListener listener) {
        profiler.removeProfilingStateListener(listener);
    }
    
    // --- Internal API --------------------------------------------------------
    
    private ProfilerFeatures features;
    
    
    final void doStart(ProfilingSettings pSettings, AttachSettings aSettings) {
        profilingSettings = pSettings;
        attachSettings = aSettings;
        start();
    }
    
    final void doModify(ProfilingSettings pSettings) {
        profilingSettings = pSettings;
        modify();
    }
    
    final void doTerminate() {
        terminate();
    }
    
    final ProfilerFeatures getFeatures() {
        assert !SwingUtilities.isEventDispatchThread();
        
        synchronized(this) {
            if (features == null) features = new ProfilerFeatures(this);
        }
        
        return features;
    }
    
    // --- Implementation ------------------------------------------------------
    
    private void cleanup() {
        synchronized(ACTIVE_SESSION_LOCK) {
            if (ACTIVE_SESSION == this) ACTIVE_SESSION = null;
        }
        
        // TODO: unregister listeners (this.addListener) to prevent memory leaks
    }
    
    // --- Provider ------------------------------------------------------------
    
    public static abstract class Provider {
        
        public abstract ProfilerSession getSession(Lookup context);
        
    }
    
}

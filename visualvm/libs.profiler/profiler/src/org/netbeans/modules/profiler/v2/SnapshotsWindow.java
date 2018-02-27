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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.SnapshotsListener;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.v2.impl.SnapshotsWindowHelper;
import org.netbeans.modules.profiler.v2.impl.SnapshotsWindowUI;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
 public final class SnapshotsWindow {
    
    // --- Singleton -----------------------------------------------------------
    
    private static SnapshotsWindow INSTANCE;
    
    private final SnapshotsListener snapshotsListener;
    
    public static synchronized SnapshotsWindow instance() {
        if (INSTANCE == null) INSTANCE = new SnapshotsWindow();
        return INSTANCE;
    }
    
    private SnapshotsWindow() {
        snapshotsListener = Lookup.getDefault().lookup(SnapshotsWindowHelper.class);
        
        TopComponent.getRegistry().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (TopComponent.Registry.PROP_TC_CLOSED.equals(evt.getPropertyName()))
                    if (ui != null && evt.getNewValue() == ui) ui = null;
            }
        });
    }
    
    // --- API -----------------------------------------------------------------
    
    public void showStandalone() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotsWindowUI ui = getUI(true);
                ui.open();
                ui.requestActive();
            }
        });
    }
    
    public void sessionOpened(final ProfilerSession session) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotsWindowUI ui = getUI(false);
                if (ui == null && ProfilerIDESettings.getInstance().getSnapshotWindowOpenPolicy() == ProfilerIDESettings.SNAPSHOT_WINDOW_OPEN_PROFILER) {
                    ui = getUI(true);
                    ui.setProject(session.getProject());
                    ui.open();
                } else if (ui != null) ui.setProject(session.getProject());
            }
        });
    }
    
    public void sessionActivated(final ProfilerSession session) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotsWindowUI ui = getUI(false);
                if (ui == null && ProfilerIDESettings.getInstance().getSnapshotWindowOpenPolicy() == ProfilerIDESettings.SNAPSHOT_WINDOW_SHOW_PROFILER) {
                    ui = getUI(true);
                    ui.setProject(session.getProject());
                    ui.open();
                } else if (ui != null) ui.setProject(session.getProject());
            }
        });
    }
    
    public void sessionDeactivated(final ProfilerSession session) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotsWindowUI ui = getUI(false);
                if (ui != null) {
                    if (ProfilerIDESettings.getInstance().getSnapshotWindowClosePolicy() == ProfilerIDESettings.SNAPSHOT_WINDOW_HIDE_PROFILER)
                        ui.close();
                    ui.resetProject(session.getProject());
                }
            }
        });
    }
    
    public void sessionClosed(final ProfilerSession session) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotsWindowUI ui = getUI(false);
                if (ui != null) {
                    if (ProfilerIDESettings.getInstance().getSnapshotWindowClosePolicy() == ProfilerIDESettings.SNAPSHOT_WINDOW_CLOSE_PROFILER)
                        ui.close();
                    ui.resetProject(session.getProject());
                }
            }
        });
    }
    
    public void snapshotSaved(final LoadedSnapshot snapshot) {
        assert !SwingUtilities.isEventDispatchThread();
        
        int policy = ProfilerIDESettings.getInstance().getSnapshotWindowOpenPolicy();
        if ((policy == ProfilerIDESettings.SNAPSHOT_WINDOW_OPEN_FIRST &&
             ResultsManager.getDefault().getSnapshotsCountFor(snapshot.getProject()) == 1) ||
             policy == ProfilerIDESettings.SNAPSHOT_WINDOW_OPEN_EACH) {
            final Lookup.Provider project = snapshot.getProject();
            ProfilerSession session = ProfilerSession.currentSession();
            if (session != null && Objects.equals(session.getProject(), project))
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SnapshotsWindowUI ui = getUI(false);
                        if (ui == null) {
                            ui = getUI(true);
                            ui.setProject(project);
                            ui.open();
                        }
                    }
                });
        }
    }
    
    public void refreshFolder(final FileObject folder, final boolean fullRefresh) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotsWindowUI ui = getUI(false);
                if (ui != null) ui.refreshFolder(folder, fullRefresh);
            }
        });
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private SnapshotsWindowUI ui;
    
    private SnapshotsWindowUI getUI(boolean create) {
        if (ui == null) {
            WindowManager wm = WindowManager.getDefault();
            
            for (TopComponent tc : TopComponent.getRegistry().getOpened())
                if (tc.getClientProperty(SnapshotsWindowUI.ID) != null)
                    ui = (SnapshotsWindowUI)tc;
            
            if (ui == null && create)
                ui = (SnapshotsWindowUI)wm.findTopComponent(SnapshotsWindowUI.ID);
            
            if (ui == null && create)
                ui = new SnapshotsWindowUI();
        }
        
        return ui;
    }
    
}

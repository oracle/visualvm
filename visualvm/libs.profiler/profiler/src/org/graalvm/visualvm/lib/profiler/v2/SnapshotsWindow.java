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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.SnapshotsListener;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.v2.impl.SnapshotsWindowHelper;
import org.graalvm.visualvm.lib.profiler.v2.impl.SnapshotsWindowUI;
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

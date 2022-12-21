     /*
 *  Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.profiler;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.SnapshotsListener;
import org.graalvm.visualvm.lib.profiler.actions.TakeSnapshotAction;
import org.graalvm.visualvm.profiling.snapshot.ProfilerSnapshotsSupport;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;

    
/**
 *
 * @author Jiri Sedlacek
 */
final class ProfilerSnapshotAction extends SingleDataSourceAction<Application> {
    
    private static final Logger LOGGER = Logger.getLogger(ProfilerSnapshotAction.class.getName());
    
    private static final String NB_PROFILER_SNAPSHOTS_STORAGE = "config" + File.separator + // NOI18N
            "NBProfiler" + File.separator + "Config" + File.separator + "Settings"; // NOI18N
    
    private final TakeSnapshotAction originalAction = TakeSnapshotAction.getInstance();
    private boolean openNextSnapshot = true;
    
    
    private static ProfilerSnapshotAction instance;
    
    public static synchronized ProfilerSnapshotAction instance() {
        if (instance == null) 
            instance = new ProfilerSnapshotAction();
        return instance;
    }
    
    protected void actionPerformed(Application dataSource, ActionEvent actionEvent) {
        openNextSnapshot = (actionEvent.getModifiers() &
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0;
        originalAction.performAction();
    }
    
    protected boolean isEnabled(Application application) {
        return ProfilerSupport.getInstance().getProfiledApplication() == application && originalAction.isEnabled();
    }
    
    protected void initialize() {
        if (ProfilerSupport.getInstance().isInitialized()) {
            super.initialize();

            originalAction.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (SystemAction.PROP_ENABLED.equals(evt.getPropertyName()))
                        ProfilerSnapshotAction.this.updateState(ActionUtils.getSelectedDataSources(Application.class));
                }
            });

        } else {
            setEnabled(false);
        }
    }
    
    
    private ProfilerSnapshotAction() {
        super(Application.class);
        putValue(NAME, NbBundle.getMessage(ProfilerSnapshotAction.class, "MSG_Profiler_Snapshot")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(ProfilerSnapshotAction.class, "DESCR_Profiler_Snapshot"));    // NOI18N
    }

    @ServiceProvider(service=SnapshotsListener.class)
    public static class SnapshotsListenerImpl implements SnapshotsListener {

        public SnapshotsListenerImpl() {
        }

        public void snapshotLoaded(LoadedSnapshot snapshot) {}

        public void snapshotRemoved(LoadedSnapshot snapshot) {}

        public void snapshotTaken(LoadedSnapshot snapshot) {}

        public void snapshotSaved(LoadedSnapshot snapshot) {
            try {
                Application profiledApplication = ProfilerSupport.getInstance().getProfiledApplication();
                File snapshotFile = snapshot.getFile();
                if (profiledApplication != null && snapshotFile.getCanonicalPath().contains(NB_PROFILER_SNAPSHOTS_STORAGE)) {
                    File newSnapshotFile = Utils.getUniqueFile(profiledApplication.getStorage().getDirectory(), snapshotFile.getName());
                    if (!snapshotFile.renameTo(newSnapshotFile)) {
                        Utils.copyFile(snapshotFile, newSnapshotFile);
                        snapshotFile.deleteOnExit();
                    }
                    snapshot.setFile(newSnapshotFile);
                    ProfilerSnapshotsSupport pss = ProfilerSnapshotsSupport.getInstance();
                    ProfilerSnapshotAction psa = ProfilerSnapshotAction.instance();
                    pss.createSnapshot(newSnapshotFile, profiledApplication, psa.openNextSnapshot);
                    psa.openNextSnapshot = true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling saved profiler snapshot", e); // NOI18N
            }
        }
    }
}

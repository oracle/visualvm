/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.SnapshotsListener;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;

    
/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerSnapshotAction extends SingleDataSourceAction<Application> {
    
    private static final String NB_PROFILER_SNAPSHOTS_STORAGE = "config" + File.separator + "NBProfiler" + File.separator + "Config" + File.separator + "Settings";
    private static final Logger LOGGER = Logger.getLogger(ProfilerSnapshotAction.class.getName());
    
    private final TakeSnapshotAction originalAction = SystemAction.get(TakeSnapshotAction.class);
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

            ResultsManager.getDefault().addSnapshotsListener(new SnapshotsListener() {
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
                            ProfilerSupport.getInstance().getSnapshotsProvider().createSnapshot(snapshot, profiledApplication, openNextSnapshot);
                            openNextSnapshot = true;
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error handling saved profiler snapshot", e);
                    }
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
}

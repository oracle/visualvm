/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiling.snapshot;

import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.core.VisualVM;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ProfilerSnapshotCategory extends SnapshotCategory<ProfilerSnapshot> {
    private static final Logger LOGGER =
            Logger.getLogger(ProfilerSnapshotCategory.class.getName());
    
    private static final String NAME = NbBundle.getMessage(
            ProfilerSnapshotCategory.class, "MSG_Profiler_Snapshots");   // NOI18N
    private static final String PREFIX = "snapshot";    // NOI18N
    private static final String NPS_SUFFIX = "."+ResultsManager.SNAPSHOT_EXTENSION;    // NOI18N
    private static final String NPSS_SUFFIX = "."+ResultsManager.STACKTRACES_SNAPSHOT_EXTENSION;    // NOI18N
    
    ProfilerSnapshotCategory() {
        super(NAME, ProfilerSnapshot.class, PREFIX, NPS_SUFFIX, 30);
    }
    
    public boolean supportsOpenSnapshot() {
        return true;
    }
    
    protected boolean isSnapshot(File file) {
        if (super.isSnapshot(file)) {
            return true;
        }
        return file != null && file.getName().endsWith(NPSS_SUFFIX);
    }

    public void openSnapshot(final File file) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandle.createHandle(
                            NbBundle.getMessage(ProfilerSnapshotCategory.class,
                                                "MSG_Opening_Profiler_Snapshot")); // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    try {
                        ProfilerSnapshot snapshot = ProfilerSnapshot.createSnapshot(file, null);
                        DataSourceWindowManager.sharedInstance().openDataSource(snapshot);
                    } catch (Exception e) {
                        LOGGER.log(Level.INFO, "Error loading profiler snapshot", e); // NOI18N
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ProfilerDialogs.displayError(
                                        NbBundle.getMessage(ProfilerSnapshotCategory.class,
                                                            "MSG_Opening_snapshot_failed")); // NOI18N
                            }   
                        });
                    }
                } finally {
                    final ProgressHandle pHandleF = pHandle;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (pHandleF != null) pHandleF.finish(); }
                    });
                }
            }
        });
    }
    
    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || isSnapshot(f);
            }
            public String getDescription() {
                String suff = getSuffix();
                return getName() + (suff != null ? " (*" + suff +", *" + NPSS_SUFFIX + ")" : "");    // NOI18N
            }
        };
    }    

}

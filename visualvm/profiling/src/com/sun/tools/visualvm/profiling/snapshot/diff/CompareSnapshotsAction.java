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
package com.sun.tools.visualvm.profiling.snapshot.diff;

import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.core.ui.actions.MultiDataSourceAction;
import com.sun.tools.visualvm.profiling.snapshot.ProfilerSnapshot;
import java.awt.event.ActionEvent;
import java.util.Set;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
final class CompareSnapshotsAction extends MultiDataSourceAction<ProfilerSnapshot> {
    
    private static CompareSnapshotsAction instance;
    
    public static synchronized CompareSnapshotsAction instance() {
        if (instance == null) instance = new CompareSnapshotsAction();
        return instance;
    }
    
        
    protected void actionPerformed(Set<ProfilerSnapshot> snapshots, ActionEvent actionEvent) {
        ProfilerSnapshot[] snapshotsArr = snapshots.toArray(new ProfilerSnapshot[2]);
        LoadedSnapshot s1 = snapshotsArr[0].getLoadedSnapshot();
        LoadedSnapshot s2 = snapshotsArr[1].getLoadedSnapshot();
        
        // Two memory snapshots of different type (alloc vs. liveness) or different
        // getAllocTrackEvery() values can be selected, perform the full check here
        if (org.netbeans.modules.profiler.actions.CompareSnapshotsAction.areComparableSnapshots(s2, s1)) { 
            SnapshotDiffContainer sdc = new SnapshotDiffContainer(
                    snapshotsArr[0], snapshotsArr[1], snapshotsArr[0].getMaster());
            DataSourceWindowManager.sharedInstance().openDataSource(sdc);
        } else {
            String msg = NbBundle.getMessage(CompareSnapshotsAction.class, "MSG_Not_Comparable");    // NOI18N
            ProfilerDialogs.displayError(msg);
        }
    }
    
    protected boolean isEnabled(Set<ProfilerSnapshot> snapshots) {
        // Action is enabled only when exactly 2 profiler snapshots are selected
        if (snapshots.size() != 2) return false;
        
        ProfilerSnapshot[] snapshotsArr = snapshots.toArray(new ProfilerSnapshot[2]);
        LoadedSnapshot s1 = snapshotsArr[0].getLoadedSnapshot();
        LoadedSnapshot s2 = snapshotsArr[1].getLoadedSnapshot();
        
        // Action is enabled only for profiler memory snapshots
        // If the snapshots are not comparable (like alloc vs. liveness) a dialog
        // is shown after action invocation (actionPerformed())
        if ((s1.getType() & LoadedSnapshot.SNAPSHOT_TYPE_MEMORY) == 0 ||
            (s2.getType() & LoadedSnapshot.SNAPSHOT_TYPE_MEMORY) == 0) return false;
        
        return true;
    }
    
    
    private CompareSnapshotsAction() {
        super(ProfilerSnapshot.class);
        putValue(NAME, NbBundle.getMessage(CompareSnapshotsAction.class,
                                           "MSG_Compare_Snapshots")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(CompareSnapshotsAction.class,
                                                        "DESCR_Compare_Snapshots"));    // NOI18N
    }
}

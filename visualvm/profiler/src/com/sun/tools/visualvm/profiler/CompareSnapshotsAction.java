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

import com.sun.tools.visualvm.core.ui.actions.MultiDataSourceAction;
import java.awt.event.ActionEvent;
import java.util.Set;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ResultsManager;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
class CompareSnapshotsAction extends MultiDataSourceAction<ProfilerSnapshot> {
    
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
            ResultsManager.getDefault().compareSnapshots(s1, s2);
        } else {
            String msg = NbBundle.getMessage(CompareSnapshotsAction.class, "MSG_Not_Comparable");    // NOI18N
            NetBeansProfiler.getDefaultNB().displayError(msg);
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
    
    protected void initialize() {
        if (ProfilerSupport.getInstance().isInitialized()) {
            super.initialize();
        } else {
            setEnabled(false);
        }
    }
    
    
    private CompareSnapshotsAction() {
        super(ProfilerSnapshot.class);
        putValue(NAME, NbBundle.getMessage(CompareSnapshotsAction.class, "MSG_Compare_Snapshots")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(CompareSnapshotsAction.class, "DESCR_Compare_Snapshots"));    // NOI18N
    }
}

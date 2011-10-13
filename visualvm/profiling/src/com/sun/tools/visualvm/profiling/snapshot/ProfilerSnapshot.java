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

package com.sun.tools.visualvm.profiling.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerSnapshot extends Snapshot {
    
    private LoadedSnapshot loadedSnapshot;


    public ProfilerSnapshot() {
        super(null, ProfilerSnapshotsSupport.getInstance().getCategory());
    }
    
    public ProfilerSnapshot(LoadedSnapshot loadedSnapshot, DataSource master) {
        super(loadedSnapshot.getFile(), ProfilerSnapshotsSupport.getInstance().getCategory(), master);
        this.loadedSnapshot = loadedSnapshot;
    }
    
    
    public LoadedSnapshot getLoadedSnapshot() {
        return loadedSnapshot;
    }
    
    public boolean supportsSaveAs() {
        return true;
    }
    
    protected void remove() {
        super.remove();
        ResultsManager.getDefault().closeSnapshot(loadedSnapshot);
    }
    
    public void saveAs() {
        SnapshotsSupport.getInstance().saveAs(this, NbBundle.getMessage(
                ProfilerSnapshot.class, "MSG_Save_Profiler_Snapshot_As"));  // NOI18N
    }

}

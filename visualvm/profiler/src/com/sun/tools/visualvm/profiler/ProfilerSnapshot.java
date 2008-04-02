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

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;

/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerSnapshot extends Snapshot {
    
    private LoadedSnapshot loadedSnapshot;
    
    public ProfilerSnapshot() {
        super(null, ProfilerSupport.getInstance().getCategory());
    }
    
    
    public ProfilerSnapshot(LoadedSnapshot loadedSnapshot, DataSource master) {
        super(loadedSnapshot.getFile(), ProfilerSupport.getInstance().getCategory(), master);
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
        SnapshotsSupport.getInstance().saveAs(this, "Save Profiler Snapshot As");
    }

}

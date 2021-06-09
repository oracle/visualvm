/*
 *  Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.snapshot.SnapshotsSupport;
import java.awt.Image;
import java.io.File;
import javax.swing.JComponent;
import org.graalvm.visualvm.lib.profiler.LoadedSnapshot;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class ProfilerSnapshot extends Snapshot {

    static final Image CPU_ICON = Icons.getImage(ProfilerIcons.CPU);
    static final Image MEMORY_ICON = Icons.getImage(ProfilerIcons.MEMORY);
    static final Image JDBC_ICON = Icons.getImage(ProfilerIcons.SQL_QUERY);
    static final Image SNAPSHOT_ICON = Icons.getImage(ProfilerIcons.SNAPSHOT_DO);
    static final Image NODE_BADGE = ImageUtilities.loadImage(
            "org/graalvm/visualvm/core/ui/resources/snapshotBadge.png", true);    // NOI18N
   
    public static ProfilerSnapshot createSnapshot(File file, DataSource master) {
        if (file.getName().endsWith(ResultsManager.STACKTRACES_SNAPSHOT_EXTENSION)) {
            return new ProfilerSnapshotNPSS(file,master);
        }
        return new ProfilerSnapshotNPS(file,master);
    }

    public ProfilerSnapshot() {
        super(null, ProfilerSnapshotsSupport.getInstance().getCategory());
    }
    
    public ProfilerSnapshot(File file, DataSource master) {
        super(file, ProfilerSnapshotsSupport.getInstance().getCategory(), master);
    }
    
    @Override
    public boolean supportsSaveAs() {
        return getFile() != null;
    }
    
    @Override
    protected void remove() {
        super.remove();
    }
    
    @Override
    public void saveAs() {
        SnapshotsSupport.getInstance().saveAs(this, NbBundle.getMessage(
                ProfilerSnapshot.class, "MSG_Save_Profiler_Snapshot_As"));  // NOI18N
    }

    public abstract LoadedSnapshot getLoadedSnapshot();
    
    abstract Image resolveIcon();
    
    abstract JComponent getUIComponent();
    
    abstract void closeComponent();
    
    
    void forceViewClosable(boolean closable) {
        getStorage().setCustomProperty(PROPERTY_VIEW_CLOSABLE, Boolean.toString(closable));
    }
    
}

/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.application.snapshot.ApplicationSnapshot;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerSnapshotsProvider implements DataChangeListener<ApplicationSnapshot> {
    
    
    public void dataChanged(DataChangeEvent<ApplicationSnapshot> event) {
        Set<ApplicationSnapshot> snapshots = event.getAdded();
        for (ApplicationSnapshot snapshot : snapshots) processNewSnapshot(snapshot);
    }
    
    
    private void processNewSnapshot(final ApplicationSnapshot snapshot) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                Set<ProfilerSnapshot> snapshots = new HashSet();
                File[] files = snapshot.getFile().listFiles(ProfilerSupport.getInstance().getCategory().getFilenameFilter());
                FileObject[] fileObjects = new FileObject[files.length];
                for (int i = 0; i < files.length; i++) fileObjects[i] = FileUtil.toFileObject(FileUtil.normalizeFile(files[i]));
                LoadedSnapshot[] loadedSnapshots = ResultsManager.getDefault().loadSnapshots(fileObjects);
                for (LoadedSnapshot loadedSnapshot : loadedSnapshots)
                    if (loadedSnapshot != null) snapshots.add(new ProfilerSnapshot(loadedSnapshot, snapshot));
                snapshot.getRepository().addDataSources(snapshots);
            }
        });
    }
    
    
    public void createSnapshot(LoadedSnapshot loadedSnapshot, final boolean openView) {
        Application profiledApplication = ProfilerSupport.getInstance().getProfiledApplication();
        final ProfilerSnapshot snapshot = new ProfilerSnapshot(loadedSnapshot, profiledApplication);
        profiledApplication.getRepository().addDataSource(snapshot);
        if (openView) SwingUtilities.invokeLater(new Runnable() {
            public void run() { DataSourceWindowManager.sharedInstance().openDataSource(snapshot); }
        });
    }
    
    public void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(this, ApplicationSnapshot.class);
    }
    
}

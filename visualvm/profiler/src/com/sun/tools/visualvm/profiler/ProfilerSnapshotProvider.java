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
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
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
public class ProfilerSnapshotProvider {
    
    public void createSnapshot(LoadedSnapshot loadedSnapshot, Application application, final boolean openView) {
        final ProfilerSnapshot snapshot = new ProfilerSnapshot(loadedSnapshot, application);
        application.getRepository().addDataSource(snapshot);
        if (openView) SwingUtilities.invokeLater(new Runnable() {
            public void run() { DataSourceWindowManager.sharedInstance().openDataSource(snapshot); }
        });
    }
    
    public void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(new SnapshotListener(), Snapshot.class);
        DataSourceRepository.sharedInstance().addDataChangeListener(new ApplicationListener(), Application.class);
    }
    
    
    private void processNewSnapshot(Snapshot snapshot) {
        if (snapshot instanceof ProfilerSnapshot) return;
        File snapshotFile = snapshot.getFile();
        if (snapshotFile != null && snapshotFile.isDirectory()) {
            Set<ProfilerSnapshot> snapshots = new HashSet();
            LoadedSnapshot[] loadedSnapshots = findSnapshots(snapshotFile);
            for (LoadedSnapshot loadedSnapshot : loadedSnapshots)
                if (loadedSnapshot != null) snapshots.add(new ProfilerSnapshot(loadedSnapshot, snapshot));
            snapshot.getRepository().addDataSources(snapshots);
        }
    }
    
    private void processNewApplication(Application application) {
        Storage storage = application.getStorage();
        if (storage.directoryExists()) {
            Set<ProfilerSnapshot> snapshots = new HashSet();
            LoadedSnapshot[] loadedSnapshots = findSnapshots(storage.getDirectory());
            for (LoadedSnapshot loadedSnapshot : loadedSnapshots)
                if (loadedSnapshot != null) snapshots.add(new ProfilerSnapshot(loadedSnapshot, application));
            application.getRepository().addDataSources(snapshots);
        }
    }
    
    private LoadedSnapshot[] findSnapshots(File directory) {
        File[] files = directory.listFiles(ProfilerSupport.getInstance().getCategory().getFilenameFilter());
        if (files == null) return new LoadedSnapshot[0];
        FileObject[] fileObjects = new FileObject[files.length];
        for (int i = 0; i < files.length; i++) fileObjects[i] = FileUtil.toFileObject(FileUtil.normalizeFile(files[i]));
        return ResultsManager.getDefault().loadSnapshots(fileObjects);
    }
    
    
    private class SnapshotListener implements DataChangeListener<Snapshot> {
        
        public void dataChanged(DataChangeEvent<Snapshot> event) {
            final Set<Snapshot> snapshots = event.getAdded();
            if (!snapshots.isEmpty()) RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    for (Snapshot snapshot : snapshots) processNewSnapshot(snapshot);
                }
            });
        }
        
    }
    
    private class ApplicationListener implements DataChangeListener<Application> {
        
        public void dataChanged(DataChangeEvent<Application> event) {
            final Set<Application> applications = event.getAdded();
            if (!applications.isEmpty()) RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    for (Application application : applications) processNewApplication(application);
                }
            });
        }
        
    }
    
}

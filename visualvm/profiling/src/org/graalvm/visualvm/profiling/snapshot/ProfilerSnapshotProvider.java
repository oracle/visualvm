/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.profiling.snapshot;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.core.VisualVM;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ProfilerSnapshotProvider {
    
    void createSnapshot(File snapshotFile, Application application, final boolean openView) {
        final ProfilerSnapshot snapshot = ProfilerSnapshot.createSnapshot(snapshotFile, application);
        application.getRepository().addDataSource(snapshot);
        if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                DataSourceWindowManager.sharedInstance().openDataSource(snapshot);
            }
        });
    }
    
    void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(
                new SnapshotListener(), Snapshot.class);
        DataSourceRepository.sharedInstance().addDataChangeListener(
                new ApplicationListener(), Application.class);
    }
    
    
    private void processNewSnapshot(Snapshot snapshot) {
        if (snapshot instanceof ProfilerSnapshot) return;
        boolean appSnapshot = snapshot instanceof ApplicationSnapshot;
        File snapshotFile = snapshot.getFile();
        if (snapshotFile != null && snapshotFile.isDirectory()) {
            Set<ProfilerSnapshot> snapshots = findSnapshots(snapshotFile, snapshot, appSnapshot);
            snapshot.getRepository().addDataSources(snapshots);
        }
    }
    
    private void processNewApplication(Application application) {
        Storage storage = application.getStorage();
        if (storage.directoryExists()) {
            Set<ProfilerSnapshot> snapshots = findSnapshots(storage.getDirectory(), application, false);
            application.getRepository().addDataSources(snapshots);
        }
    }
    
    private Set<ProfilerSnapshot> findSnapshots(File directory, DataSource app, boolean forceClosable) {
        File[] files = directory.listFiles(
                ProfilerSnapshotsSupport.getInstance().getCategory().getFilenameFilter());
        if (files == null) return Collections.EMPTY_SET;
        Set<ProfilerSnapshot> snapshots = new HashSet(files.length);
        for (File file : files) {
            ProfilerSnapshot snapshot = ProfilerSnapshot.createSnapshot(file, app);
            if (forceClosable) snapshot.forceViewClosable(true);
            snapshots.add(snapshot);
        }
        return snapshots;
    }
    
    
    private class SnapshotListener implements DataChangeListener<Snapshot> {
        
        public void dataChanged(DataChangeEvent<Snapshot> event) {
            final Set<Snapshot> snapshots = event.getAdded();
            if (!snapshots.isEmpty()) VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    for (Snapshot snapshot : snapshots) processNewSnapshot(snapshot);
                }
            });
        }
        
    }
    
    private class ApplicationListener implements DataChangeListener<Application> {
        
        public void dataChanged(DataChangeEvent<Application> event) {
            final Set<Application> applications = event.getAdded();
            if (!applications.isEmpty()) VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    for (Application application : applications)
                        processNewApplication(application);
                }
            });
        }
        
    }
    
}

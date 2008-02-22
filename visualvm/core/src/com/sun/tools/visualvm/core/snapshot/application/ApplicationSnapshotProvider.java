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

package com.sun.tools.visualvm.core.snapshot.application;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.model.apptype.ApplicationType;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.snapshot.SnapshotProvider;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSnapshotProvider extends SnapshotProvider<ApplicationSnapshot> {
    
    private static ApplicationSnapshotProvider sharedInstance;
    
    public synchronized static ApplicationSnapshotProvider sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ApplicationSnapshotProvider();
        return sharedInstance;
    }
    
    
    private ApplicationSnapshotProvider() {
    }
    
    void createSnapshot(final Application application, final boolean interactive) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                createSnapshotImpl(application, interactive);
            }
        });
    }
    
    private void createSnapshotImpl(final Application application, final boolean interactive) {
        ApplicationSnapshotsSupport support = ApplicationSnapshotsSupport.getInstance();
        Set<Snapshot> snapshots = application.getRepository().getDataSources(Snapshot.class);
        if (snapshots.isEmpty()) return;
        
        File snapshotDirectory = new File(support.getSnapshotsStorageDirectory(), support.getCategory().createFileName());
        if (!snapshotDirectory.exists() && !snapshotDirectory.mkdir())
            throw new IllegalStateException("Cannot save datasource snapshot " + snapshotDirectory);
        
        FileObject snapshotDirectoryObject = FileUtil.toFileObject(snapshotDirectory);
        
        for (Snapshot snapshot : snapshots) {
            File file = snapshot.getFile();
            if (file == null) continue;
            FileObject fileObject = FileUtil.toFileObject(file);
            try {
                fileObject.copy(snapshotDirectoryObject, fileObject.getNameExt(), "");
            } catch (Exception e) { System.err.println("Unable to copy snapshot " + file.getAbsolutePath() + " to persistent storage " + snapshotDirectory); }
        }
        
        ApplicationType applicationType = ApplicationTypeFactory.getApplicationTypeFor(application);
        Properties properties = new Properties();
        properties.put(ApplicationSnapshotsSupport.DISPLAY_NAME, applicationType.getName());
        File iconFile = ApplicationSnapshotsSupport.saveImage(snapshotDirectory, "_" + ApplicationSnapshotsSupport.DISPLAY_ICON, "png", applicationType.getIcon());
        if (iconFile != null) properties.put(ApplicationSnapshotsSupport.DISPLAY_ICON, iconFile.getName());
        ApplicationSnapshotsSupport.storeProperties(properties, snapshotDirectory);
        
        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory);
        SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
        registerDataSource(snapshot);
    }

    void deleteSnapshot(ApplicationSnapshot snapshot, boolean interactive) {
        // TODO: if interactive, show a Do-Not-Show-Again confirmation dialog
        if (snapshot.getOwner() != null) snapshot.getOwner().getRepository().removeDataSource(snapshot);
        unregisterDataSource(snapshot);
        snapshot.delete();
    }
    
    
    protected <Y extends ApplicationSnapshot> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (ApplicationSnapshot snapshot : removed) {
            SnapshotsContainer.sharedInstance().getRepository().removeDataSource(snapshot);
            snapshot.removed();
        }
    }
    
        
    private void loadSnapshots() {
        File[] files = ApplicationSnapshotsSupport.getInstance().getSnapshotsStorageDirectory().listFiles(
                ApplicationSnapshotsSupport.getInstance().getCategory().getFilenameFilter());
        
        Set<ApplicationSnapshot> snapshots = new HashSet();
        for (File file : files) snapshots.add(new ApplicationSnapshot(file));
        
        SnapshotsContainer.sharedInstance().getRepository().addDataSources(snapshots);
        registerDataSources(snapshots);
    }
    
    
    void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(ApplicationSnapshotProvider.sharedInstance());
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { loadSnapshots(); }
        });
    }
    
}
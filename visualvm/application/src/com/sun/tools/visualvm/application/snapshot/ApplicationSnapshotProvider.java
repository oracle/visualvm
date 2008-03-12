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

package com.sun.tools.visualvm.application.snapshot;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.application.type.ApplicationType;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.snapshot.SnapshotProvider;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSnapshotProvider extends SnapshotProvider<ApplicationSnapshot> {
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROPERTIES_FILENAME = "application_snapshot" + Storage.DEFAULT_PROPERTIES_EXT;
    
    private static ApplicationSnapshotProvider sharedInstance;
    
    public synchronized static ApplicationSnapshotProvider sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ApplicationSnapshotProvider();
        return sharedInstance;
    }
    
    
    private ApplicationSnapshotProvider() {
    }
    
    void createSnapshot(final Application application, final boolean interactive) {
        // TODO: open snapshot if interactive
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle("Saving snapshot of " + DataSourceDescriptorFactory.getDescriptor(application).getName() + "...");
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    createSnapshotImpl(application, interactive);
                } finally {
                    final ProgressHandle pHandleF = pHandle;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (pHandleF != null) pHandleF.finish(); }
                    });
                }
            }
        });
    }
    
    private void createSnapshotImpl(final Application application, final boolean interactive) {
        Set<Snapshot> snapshots = application.getRepository().getDataSources(Snapshot.class);
        if (snapshots.isEmpty()) return;
        
        File snapshotDirectory = Utils.getUniqueFile(ApplicationSnapshotsSupport.getStorageDirectory(), ApplicationSnapshotsSupport.getInstance().getCategory().createFileName());
        if (!snapshotDirectory.exists() && !snapshotDirectory.mkdirs())
            throw new IllegalStateException("Cannot save datasource snapshot " + snapshotDirectory);
        
        for (Snapshot snapshot : snapshots) {
            try {
                snapshot.save(snapshotDirectory);
            } catch (Exception e) {
                System.err.println("Error saving snapshot to application snapshot: " + e.getMessage());
            }
        }
        
        ApplicationType applicationType = ApplicationTypeFactory.getApplicationTypeFor(application);
        String[] propNames = new String[] {
            SNAPSHOT_VERSION,
            DataSourceDescriptor.PROPERTY_NAME,
            DataSourceDescriptor.PROPERTY_ICON
        };
        String[] propValues = new String[] {
            CURRENT_SNAPSHOT_VERSION,
            applicationType.getName() + getDisplayNameSuffix(application),
            Utils.imageToString(applicationType.getIcon(), "png")
        };
        
        Storage storage = new Storage(snapshotDirectory, PROPERTIES_FILENAME);
        storage.setCustomProperties(propNames, propValues);
        
        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory, storage);
        SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
        registerDataSource(snapshot);
    }
    
    private static String getDisplayNameSuffix(Application application) {
        StringBuilder builder = new StringBuilder(" (");
        int pid = application.getPid();
        if (pid != Application.UNKNOWN_PID) builder.append("pid " + pid + ", ");
        builder.append(SnapshotsSupport.getInstance().getTimeStamp(System.currentTimeMillis()));
        builder.append(")");
        return builder.toString();
    }
    
    void addSnapshotArchive(final File archive, final boolean deleteArchive) {
        
        // TODO: check if the same snapshot isn't already imported
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle("Adding " + archive.getName() + "...");
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    
                    File snapshotDirectory = Utils.extractArchive(archive, ApplicationSnapshotsSupport.getStorageDirectory());
                    if (snapshotDirectory != null) {
                        Storage storage = new Storage(snapshotDirectory, PROPERTIES_FILENAME);
                        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory, storage);
                        SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
                        registerDataSource(snapshot);
                        if (deleteArchive) if (!archive.delete()) archive.deleteOnExit();
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                NetBeansProfiler.getDefaultNB().displayError("<html><b>Adding snapshot " + archive.getName() + " failed.</b><br><br>Make sure the file is not broken.</html>");
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

    void unregisterSnapshot(ApplicationSnapshot snapshot) {
        unregisterDataSource(snapshot);
    }
    
    
    protected <Y extends ApplicationSnapshot> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (ApplicationSnapshot snapshot : removed) {
            if (snapshot.getOwner() != null) snapshot.getOwner().getRepository().removeDataSource(snapshot);
            snapshot.removed();
        }
    }
    
        
    private void loadSnapshots() {
        if (!ApplicationSnapshotsSupport.storageDirectoryExists()) return;
        
        File[] files = ApplicationSnapshotsSupport.getStorageDirectory().listFiles(
                ApplicationSnapshotsSupport.getInstance().getCategory().getFilenameFilter());
        
        Set<ApplicationSnapshot> snapshots = new HashSet();
        for (File file : files) {
            if (file.isDirectory()) { // NOTE: once archived snapshots are implemented, this is not necessary
                Storage storage = new Storage(file, PROPERTIES_FILENAME);
                snapshots.add(new ApplicationSnapshot(file, storage));
            }
        }
        
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
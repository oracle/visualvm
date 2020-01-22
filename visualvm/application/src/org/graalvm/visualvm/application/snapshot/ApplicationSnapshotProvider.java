/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.snapshot;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.explorer.ExplorerSupport;
import org.graalvm.visualvm.core.snapshot.SnapshotsContainer;
import org.graalvm.visualvm.core.snapshot.SnapshotsSupport;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.VisualVM;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSnapshotProvider {
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";   // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";  // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";    // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";    // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROPERTIES_FILENAME = "application_snapshot" + Storage.DEFAULT_PROPERTIES_EXT;   // NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(ApplicationSnapshotProvider.class.getName());
    
    private static ApplicationSnapshotProvider sharedInstance;
    
    synchronized static ApplicationSnapshotProvider sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ApplicationSnapshotProvider();
        return sharedInstance;
    }
    
    
    private ApplicationSnapshotProvider() {
    }
    
    void createSnapshot(final Application application, final boolean openSnapshot) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ApplicationSnapshotProvider.class, "MSG_Saving_snapshot", DataSourceDescriptorFactory.getDescriptor(application).getName()));  // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    createSnapshotImpl(application, openSnapshot);
                } finally {
                    final ProgressHandle pHandleF = pHandle;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { if (pHandleF != null) pHandleF.finish(); }
                    });
                }
            }
        });
    }
    
    private void createSnapshotImpl(final Application application, final boolean openSnapshot) {
        Set<Snapshot> snapshots = application.getRepository().getDataSources(Snapshot.class);
        if (snapshots.isEmpty() && !DataSourceViewsManager.sharedInstance().canSaveViewsFor(application, ApplicationSnapshot.class)) return;
        
        File snapshotDirectory = null;
        synchronized (ApplicationSnapshotProvider.this) {
            snapshotDirectory = Utils.getUniqueFile(ApplicationSnapshotsSupport.getStorageDirectory(),
                                                    ApplicationSnapshotsSupport.getInstance().
                                                    getCategory().createFileName());
            if (!Utils.prepareDirectory(snapshotDirectory))
                throw new IllegalStateException("Cannot save datasource snapshot " + snapshotDirectory);    // NOI18N
        }
        
        for (Snapshot snapshot : snapshots) {
            try {
                Storage storage = snapshot.getStorage();
                String prop = DataSourceDescriptor.PROPERTY_PREFERRED_POSITION;
                boolean customPos = storage.getCustomProperty(prop) != null;
                if (!customPos) {
                    int pos = ExplorerSupport.sharedInstance().getDataSourcePosition(snapshot);
                    storage.setCustomProperty(prop, Integer.toString(pos));
                }
                snapshot.save(snapshotDirectory);
                if (!customPos) {
                    storage.clearCustomProperty(prop);
                    if (!storage.hasCustomProperties()) storage.deleteCustomPropertiesStorage();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error saving snapshot to application snapshot", e);   // NOI18N
            }
        }

        // See #299
//        ApplicationType applicationType = ApplicationTypeFactory.getApplicationTypeFor(application);
        DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDescriptor(application);
        String[] propNames = new String[] {
            SNAPSHOT_VERSION,
            DataSourceDescriptor.PROPERTY_NAME,
            DataSourceDescriptor.PROPERTY_ICON
        };
        String[] propValues = new String[] {
            CURRENT_SNAPSHOT_VERSION,
            descriptor.getName() + getDisplayNameSuffix(application),
            Utils.imageToString(descriptor.getIcon(), "png")   // NOI18N
        };
        
        Storage storage = new Storage(snapshotDirectory, PROPERTIES_FILENAME);
        storage.setCustomProperties(propNames, propValues);
        
        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory, storage);
        DataSourceViewsManager.sharedInstance().saveViewsFor(application, snapshot);
        SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
        
        if (openSnapshot && DataSourceWindowManager.sharedInstance().canOpenDataSource(snapshot))
            DataSourceWindowManager.sharedInstance().openDataSource(snapshot); // TODO: check #VISUALVM-636
    }
    
    private static String getDisplayNameSuffix(Application application) {
        return ", " + SnapshotsSupport.getInstance().getTimeStamp(System.currentTimeMillis()); // NOI18N
        // See #299
//        StringBuilder builder = new StringBuilder(" (");    // NOI18N
//        int pid = application.getPid();
//        if (pid != Application.UNKNOWN_PID) builder.append("pid " + pid + ", ");
//        builder.append(SnapshotsSupport.getInstance().getTimeStamp(System.currentTimeMillis()));
//        builder.append(")");    // NOI18N
//        return builder.toString();
    }
    
    void addSnapshotArchive(File archive, boolean deleteArchive) {
        processApplicationSnapshotImpl(archive, deleteArchive, true, NbBundle.
                getMessage(ApplicationSnapshotProvider.class, "MSG_Adding", // NOI18N
                archive.getName()), false);
    }

    void loadSnapshotArchive(File archive) {
        processApplicationSnapshotImpl(archive, false, false, NbBundle.
                getMessage(ApplicationSnapshotProvider.class, "MSG_Loading", // NOI18N
                archive.getName()), true);
    }


    private void processApplicationSnapshotImpl(final File archive, final boolean deleteArchive,
                                                final boolean persistent, final String progressMsg,
                                                final boolean openSnapshot) {
        // TODO: check if the same snapshot isn't already imported

        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(progressMsg);
                    pHandle.setInitialDelay(0);
                    pHandle.start();

                    File storageDirectory = persistent ? ApplicationSnapshotsSupport.getStorageDirectory() :
                                                         Storage.getTemporaryStorageDirectory();
                    
                    File snapshotDirectory = new File(storageDirectory, archive.getName());
                    
                    // Only extract the archive if not already extracted (subsequent opening of the same snapshot)
                    if (!snapshotDirectory.isDirectory() || !snapshotDirectory.canRead())
                        snapshotDirectory = Utils.extractArchive(archive, storageDirectory);
                    
                    if (snapshotDirectory != null) {
                        Storage storage = new Storage(snapshotDirectory, PROPERTIES_FILENAME);
                        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory, storage);
                        if (persistent) SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
                        if (openSnapshot) {
                            if (DataSourceWindowManager.sharedInstance().canOpenDataSource(snapshot)) {
                                DataSourceWindowManager.sharedInstance().openDataSource(snapshot); // TODO: check #VISUALVM-636
                            } else {
                                DialogDisplayer.getDefault().notifyLater(
                                        new NotifyDescriptor.Message(NbBundle.
                                        getMessage(ApplicationSnapshotProvider.class,
                                        "MSG_Opening_snapshot_failed", archive. // NOI18N
                                        getName()), NotifyDescriptor.ERROR_MESSAGE));
                            }
                        }
                        if (deleteArchive) if (!archive.delete()) archive.deleteOnExit();
                    } else {
                        DialogDisplayer.getDefault().notifyLater(
                                    new NotifyDescriptor.Message(NbBundle.
                                    getMessage(ApplicationSnapshotProvider.class,
                                    "MSG_Adding_snapshot_failed", archive. // NOI18N
                                    getName()), NotifyDescriptor.ERROR_MESSAGE));
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
        
        if (!snapshots.isEmpty()) 
            SnapshotsContainer.sharedInstance().getRepository().addDataSources(snapshots);
    }
    
    
    void initialize() {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() { loadSnapshots(); }
        });
    }
    
}
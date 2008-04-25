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
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.application.type.ApplicationType;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

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
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ApplicationSnapshotProvider.class, "MSG_Saving_snapshot", DataSourceDescriptorFactory.getDescriptor(application).getName()));  // NOI18N
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
        if (snapshots.isEmpty() && !DataSourceViewsManager.sharedInstance().canSaveViewsFor(application, ApplicationSnapshot.class)) return;
        
        File snapshotDirectory = null;
        synchronized(ApplicationSnapshotProvider.this) {
            snapshotDirectory = Utils.getUniqueFile(ApplicationSnapshotsSupport.getStorageDirectory(), ApplicationSnapshotsSupport.getInstance().getCategory().createFileName());
            if (!Utils.prepareDirectory(snapshotDirectory))
                throw new IllegalStateException("Cannot save datasource snapshot " + snapshotDirectory);    // NOI18N
        }
        
        for (Snapshot snapshot : snapshots) {
            try {
                snapshot.save(snapshotDirectory);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error saving snapshot to application snapshot", e);   // NOI18N
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
            Utils.imageToString(applicationType.getIcon(), "png")   // NOI18N
        };
        
        Storage storage = new Storage(snapshotDirectory, PROPERTIES_FILENAME);
        storage.setCustomProperties(propNames, propValues);
        
        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory, storage);
        DataSourceViewsManager.sharedInstance().saveViewsFor(application, snapshot);
        SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
        
        if (interactive && DataSourceWindowManager.sharedInstance().canOpenDataSource(snapshot))
            DataSourceWindowManager.sharedInstance().openDataSource(snapshot);
    }
    
    private static String getDisplayNameSuffix(Application application) {
        StringBuilder builder = new StringBuilder(" (");    // NOI18N
        int pid = application.getPid();
        if (pid != Application.UNKNOWN_PID) builder.append("pid " + pid + ", ");    // NOI18N
        builder.append(SnapshotsSupport.getInstance().getTimeStamp(System.currentTimeMillis()));
        builder.append(")");    // NOI18N
        return builder.toString();
    }
    
    void addSnapshotArchive(final File archive, final boolean deleteArchive) {
        
        // TODO: check if the same snapshot isn't already imported
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ApplicationSnapshotProvider.class, "MSG_Adding", archive.getName()));  // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    
                    File snapshotDirectory = Utils.extractArchive(archive, ApplicationSnapshotsSupport.getStorageDirectory());
                    if (snapshotDirectory != null) {
                        Storage storage = new Storage(snapshotDirectory, PROPERTIES_FILENAME);
                        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory, storage);
                        SnapshotsContainer.sharedInstance().getRepository().addDataSource(snapshot);
                        if (deleteArchive) if (!archive.delete()) archive.deleteOnExit();
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                NetBeansProfiler.getDefaultNB().displayError(NbBundle.getMessage(ApplicationSnapshotProvider.class, "MSG_Adding_snapshot_failed", archive.getName()));  // NOI18N
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
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { loadSnapshots(); }
        });
    }
    
}
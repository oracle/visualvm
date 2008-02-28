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
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.model.apptype.ApplicationType;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
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
    
    static final String SNAPSHOT_VERSION = "snapshot_version";
    static final String SNAPSHOT_VERSION_DIVIDER = ".";
    static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";
    static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";
    static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
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
        Set<Snapshot> snapshots = application.getSnapshots();
        if (snapshots.isEmpty()) return;
        
        File snapshotDirectory = new File(ApplicationSnapshotsSupport.getSnapshotsStorageDirectory(), ApplicationSnapshotsSupport.getInstance().getCategory().createFileName());
        if (!snapshotDirectory.exists() && !snapshotDirectory.mkdir())
            throw new IllegalStateException("Cannot save datasource snapshot " + snapshotDirectory);
        
        for (Snapshot snapshot : snapshots) {
            try {
                snapshot.save(snapshotDirectory);
            } catch (Exception e) {
                System.err.println("Error saving snapshot to application snapshot: " + e.getMessage());
            }
        }
        
        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory);
        ApplicationType applicationType = ApplicationTypeFactory.getApplicationTypeFor(application);
        String[] propNames = new String[] { SNAPSHOT_VERSION,
            DataSourceDescriptor.PROPERTY_NAME,
            DataSourceDescriptor.PROPERTY_ICON };
        String[] propValues = new String[] { CURRENT_SNAPSHOT_VERSION,
            applicationType.getName() + getDisplayNameSuffix(application),
            Utils.imageToString(applicationType.getIcon(), "png")};
        snapshot.getStorage().setCustomProperties(propNames, propValues);
        
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
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle("Adding " + archive.getName() + "...");
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    
                    File snapshotDirectory = Utils.extractArchive(archive, ApplicationSnapshotsSupport.getSnapshotsStorageDirectory());
                    if (snapshotDirectory != null) {
                        ApplicationSnapshot snapshot = new ApplicationSnapshot(snapshotDirectory);
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

    void deleteSnapshot(ApplicationSnapshot snapshot, boolean interactive) {
        // TODO: if interactive, show a Do-Not-Show-Again confirmation dialog
        if (snapshot.getOwner() != null) snapshot.getOwner().getRepository().removeDataSource(snapshot);
        unregisterDataSource(snapshot);
    }
    
    
    protected <Y extends ApplicationSnapshot> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (ApplicationSnapshot snapshot : removed) {
            SnapshotsContainer.sharedInstance().getRepository().removeDataSource(snapshot);
            snapshot.removed();
        }
    }
    
        
    private void loadSnapshots() {
        if (!ApplicationSnapshotsSupport.snapshotsStorageDirectoryExists()) return;
        
        File[] files = ApplicationSnapshotsSupport.getSnapshotsStorageDirectory().listFiles(
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
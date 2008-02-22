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

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Support for snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class SnapshotsSupport {
    
    private static final String TEMPORARY_STORAGE_DIRNAME = "visualvm.dat";
    private static final String PERSISTENT_STORAGE_DIRNAME = "snapshots";
    
    private static SnapshotsSupport instance;
    
    private File temporaryStorageDirectory;
    private String temporaryStorageDirectoryString;
    private File persistentStorageDirectory;
    private String persistentStorageDirectoryString;


    /**
     * Returns singleton instance of SnapshotsSupport.
     * 
     * @return singleton instance of SnapshotsSupport.
     */
    public static synchronized SnapshotsSupport getInstance() {
        if (instance == null) instance = new SnapshotsSupport();
        return instance;
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource snapshots
     * 
     * @return default storage directory for temporary (runtime) DataSource snapshots
     */
    public String getTemporaryStorageDirectoryString() {
        if (temporaryStorageDirectoryString == null)
            temporaryStorageDirectoryString = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + File.separator + TEMPORARY_STORAGE_DIRNAME;
        return temporaryStorageDirectoryString;
    }
    
    /**
     * Returns default storage directory for temporary (runtime) DataSource snapshots
     * 
     * @return default storage directory for temporary (runtime) DataSource snapshots
     */
    public File getTemporaryStorageDirectory() {
        if (temporaryStorageDirectory == null) {
            String temporaryStorageString = getTemporaryStorageDirectoryString();
            temporaryStorageDirectory = new File(temporaryStorageString);
            if (temporaryStorageDirectory.exists() && temporaryStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString + ", file in the way");
            if (temporaryStorageDirectory.exists() && (!temporaryStorageDirectory.canRead() || !temporaryStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access temporary storage directory " + temporaryStorageString + ", read&write permission required");
            if (!temporaryStorageDirectory.exists() && !temporaryStorageDirectory.mkdir())
                throw new IllegalStateException("Cannot create temporary storage directory " + temporaryStorageString);
        }
        return temporaryStorageDirectory;
    }
    
    /**
     * Returns default storage directory for persistent (snapshots) DataSource snapshots
     * 
     * @return default storage directory for persistent (snapshots) DataSource snapshots
     */
    public String getPersistentStorageDirectoryString() {
        if (persistentStorageDirectoryString == null)
            persistentStorageDirectoryString = new File(System.getProperty("netbeans.user")).getAbsolutePath() + File.separator + PERSISTENT_STORAGE_DIRNAME;
        return persistentStorageDirectoryString;
    }
    
    /**
     * Returns default storage directory for persistent (snapshots) DataSource snapshots
     * 
     * @return default storage directory for persistent (snapshots) DataSource snapshots
     */
    public File getPersistentStorageDirectory() {
        if (persistentStorageDirectory == null) {
            String persistentStorageString = getPersistentStorageDirectoryString();
            persistentStorageDirectory = new File(persistentStorageString);
            if (persistentStorageDirectory.exists() && persistentStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString + ", file in the way");
            if (persistentStorageDirectory.exists() && (!persistentStorageDirectory.canRead() || !persistentStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access persistent storage directory " + persistentStorageString + ", read&write permission required");
            if (!persistentStorageDirectory.exists() && !persistentStorageDirectory.mkdir())
                throw new IllegalStateException("Cannot create persistent storage directory " + persistentStorageString);
        }
        return persistentStorageDirectory;
    }
    
    
    FilenameFilter getFilenameFilter(final SnapshotCategory category) {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return category.isSnapshot(name);
            }
        };
    }
    
    private Set<File> getCategoryFiles(File directory, SnapshotCategory category) {
        File[] filesArr = directory.listFiles(getFilenameFilter(category));
        Set<File> files = new HashSet();
        for (File file : filesArr) files.add(file);
        return files;
    }
    
    public Set<Snapshot> getSnapshots(File directory, SnapshotCategory category, DataSource master) {
        Set<Snapshot> snapshots = new HashSet();
        Set<File> files = getCategoryFiles(directory, category);
        for (File file : files) snapshots.add(category.getLoader().loadSnapshot(file, master));
        return snapshots;
    }
    
    public Set<Snapshot> getSnapshots(File directory, DataSource master) {
        Set<Snapshot> snapshots = new HashSet();
        List<SnapshotCategory> categories = RegisteredSnapshotCategories.sharedInstance().getVisibleCategories();
        for (SnapshotCategory category : categories) snapshots.addAll(getSnapshots(directory, category, master));
        return snapshots;
    }
    
    
    private SnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new SnapshotsContainerDescriptorProvider());
        new SnapshotsContainerProvider().initialize();
    }

}

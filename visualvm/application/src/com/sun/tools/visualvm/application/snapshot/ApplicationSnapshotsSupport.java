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

import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import java.io.File;

/**
 * Support for application snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationSnapshotsSupport {
    
    private static ApplicationSnapshotsSupport instance;
    
    private static final String SNAPSHOTS_STORAGE_DIRNAME = "snapshots";
    
    private static File snapshotsStorageDirectory;
    private static String snapshotsStorageDirectoryString;

    private ApplicationSnapshotProvider snapshotProvider;
    private ApplicationSnapshotCategory snapshotCategory = new ApplicationSnapshotCategory();


    /**
     * Returns singleton instance of ApplicationSnapshotsSupport.
     * 
     * @return singleton instance of ApplicationSnapshotsSupport.
     */
    public static synchronized ApplicationSnapshotsSupport getInstance() {
        if (instance == null) instance = new ApplicationSnapshotsSupport();
        return instance;
    }
    
    
    /**
     * Returns SnapshotCategory instance for application snapshots.
     * 
     * @return SnapshotCategory instance for application snapshots.
     */
    public SnapshotCategory getCategory() {
        return snapshotCategory;
    }
    
    ApplicationSnapshotCategory getApplicationSnapshotCategory() {
        return snapshotCategory;
    } 
    
    
    ApplicationSnapshotProvider getSnapshotProvider() {
        return snapshotProvider;
    }
    
    static String getStorageDirectoryString() {
        if (snapshotsStorageDirectoryString == null)
            snapshotsStorageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + SNAPSHOTS_STORAGE_DIRNAME;
        return snapshotsStorageDirectoryString;
    }
    
    static File getStorageDirectory() {
        if (snapshotsStorageDirectory == null) {
            String snapshotsStorageString = getStorageDirectoryString();
            snapshotsStorageDirectory = new File(snapshotsStorageString);
            if (snapshotsStorageDirectory.exists() && snapshotsStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create snapshots storage directory " + snapshotsStorageString + ", file in the way");
            if (snapshotsStorageDirectory.exists() && (!snapshotsStorageDirectory.canRead() || !snapshotsStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access snapshots storage directory " + snapshotsStorageString + ", read&write permission required");
            if (!snapshotsStorageDirectory.exists() && !snapshotsStorageDirectory.mkdirs())
                throw new IllegalStateException("Cannot create snapshots storage directory " + snapshotsStorageString);
        }
        return snapshotsStorageDirectory;
    }
    
    static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }
    
    
//    private static File getUniqueFile(File directory, String prefix, String suffix) {
//        File file = new File(directory, prefix + suffix);
//        while (file.exists()) {
//            prefix = prefix + "_";
//            file = new File(directory, prefix + suffix);
//        }
//        return file;
//    }
    
    private ApplicationSnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new ApplicationSnapshotDescriptorProvider());
        snapshotProvider = ApplicationSnapshotProvider.sharedInstance();
        
        RegisteredSnapshotCategories.sharedInstance().addCategory(snapshotCategory);
        
        snapshotProvider.initialize();
        ApplicationSnapshotActionProvider.getInstance().initialize();
    }

}

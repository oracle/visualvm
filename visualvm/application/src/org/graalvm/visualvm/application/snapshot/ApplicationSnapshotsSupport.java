/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.snapshot.SnapshotsContainer;
import java.io.File;

/**
 * Support for application snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ApplicationSnapshotsSupport {
    
    private static ApplicationSnapshotsSupport instance;
    
    private static final String SNAPSHOTS_STORAGE_DIRNAME = "snapshots";    // NOI18N
    
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
    
    static synchronized String getStorageDirectoryString() {
        if (snapshotsStorageDirectoryString == null)
            snapshotsStorageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + SNAPSHOTS_STORAGE_DIRNAME;
        return snapshotsStorageDirectoryString;
    }
    
    static synchronized File getStorageDirectory() {
        if (snapshotsStorageDirectory == null) {
            String snapshotsStorageString = getStorageDirectoryString();
            snapshotsStorageDirectory = new File(snapshotsStorageString);
            if (snapshotsStorageDirectory.exists() && snapshotsStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create snapshots storage directory " + snapshotsStorageString + ", file in the way");   // NOI18N
            if (snapshotsStorageDirectory.exists() && (!snapshotsStorageDirectory.canRead() || !snapshotsStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access snapshots storage directory " + snapshotsStorageString + ", read&write permission required");    // NOI18N
            if (!Utils.prepareDirectory(snapshotsStorageDirectory))
                throw new IllegalStateException("Cannot create snapshots storage directory " + snapshotsStorageString); // NOI18N
        }
        return snapshotsStorageDirectory;
    }
    
    static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }
    
    
    private ApplicationSnapshotsSupport() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new ApplicationSnapshotDescriptorProvider());
        snapshotProvider = ApplicationSnapshotProvider.sharedInstance();
        
        RegisteredSnapshotCategories.sharedInstance().registerCategory(snapshotCategory);
        SnapshotsContainer.sharedInstance(); // Notify SnapshotsContainer
        snapshotProvider.initialize();
    }

}

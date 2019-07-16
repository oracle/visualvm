/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jfr;

import org.graalvm.visualvm.jfr.impl.JFRSnapshotCategory;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;
import org.graalvm.visualvm.jfr.impl.JFRSnapshotDescriptorProvider;
import org.graalvm.visualvm.jfr.views.overview.JFRSnapshotOverviewViewProvider;
import org.graalvm.visualvm.jfr.impl.JFRSnapshotProvider;
import java.io.File;
import org.graalvm.visualvm.jfr.views.browser.JFRSnapshotBrowserViewProvider;
import org.graalvm.visualvm.jfr.views.environment.JFRSnapshotEnvironmentViewProvider;
import org.graalvm.visualvm.jfr.views.fileio.JFRSnapshotFileIOViewProvider;
import org.graalvm.visualvm.jfr.views.locks.JFRSnapshotLocksViewProvider;
import org.graalvm.visualvm.jfr.views.monitor.JFRSnapshotMonitorViewProvider;
import org.graalvm.visualvm.jfr.views.recording.JFRSnapshotRecordingViewProvider;
import org.graalvm.visualvm.jfr.views.sampler.JFRSnapshotSamplerViewProvider;
import org.graalvm.visualvm.jfr.views.socketio.JFRSnapshotSocketIOViewProvider;
import org.graalvm.visualvm.jfr.views.threads.JFRSnapshotThreadsViewProvider;

/**
 * Support for JFR snapshots in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class JFRSnapshotSupport {
    
    private static final String JFRSNAPSHOTS_STORAGE_DIRNAME = "jfrsnapshots";    // NOI18N
    
    private static final Object jfrSnapshotsStorageDirectoryLock = new Object();
    // @GuardedBy jfrSnapshotsStorageDirectoryLock
    private static File jfrSnapshotsStorageDirectory;
    private static final Object jfrSnapshotsStorageDirectoryStringLock = new Object();
    // @GuardedBy jfrSnapshotsStorageDirectoryStringLock
    private static String jfrSnapshotsStorageDirectoryString;
    
    private static JFRSnapshotOverviewViewProvider viewProvider = new JFRSnapshotOverviewViewProvider();
    private static JFRSnapshotCategory category = new JFRSnapshotCategory();
    
    
    /**
     * Returns PluggableDataSourceViewProvider for Overview subtab.
     * 
     * @return PluggableDataSourceViewProvider for Overview subtab.
     */
    public static PluggableDataSourceViewProvider<JFRSnapshot> getOverviewView() {
        return viewProvider;
    } 
    
    /**
     * Returns SnapshotCategory instance for JFR snapshots.
     * 
     * @return SnapshotCategory instance for JFR snapshots.
     */
    public static SnapshotCategory getCategory() {
        return category;
    }
    
    static String getStorageDirectoryString() {
        synchronized(jfrSnapshotsStorageDirectoryStringLock) {
            if (jfrSnapshotsStorageDirectoryString == null)
                jfrSnapshotsStorageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + JFRSNAPSHOTS_STORAGE_DIRNAME;
            return jfrSnapshotsStorageDirectoryString;
        }
    }
    
    /**
     * Returns storage directory for JFR snapshots.
     * 
     * @return storage directory for JFR snapshots.
     */
    public static File getStorageDirectory() {
        synchronized(jfrSnapshotsStorageDirectoryLock) {
            if (jfrSnapshotsStorageDirectory == null) {
                String snapshotsStorageString = getStorageDirectoryString();
                jfrSnapshotsStorageDirectory = new File(snapshotsStorageString);
                if (jfrSnapshotsStorageDirectory.exists() && jfrSnapshotsStorageDirectory.isFile())
                    throw new IllegalStateException("Cannot create JFR snapshots storage directory " + snapshotsStorageString + ", file in the way");   // NOI18N
                if (jfrSnapshotsStorageDirectory.exists() && (!jfrSnapshotsStorageDirectory.canRead() || !jfrSnapshotsStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access JFR snapshots storage directory " + snapshotsStorageString + ", read&write permission required");    // NOI18N
                if (!Utils.prepareDirectory(jfrSnapshotsStorageDirectory))
                    throw new IllegalStateException("Cannot create JFR snapshots storage directory " + snapshotsStorageString); // NOI18N
            }
            return jfrSnapshotsStorageDirectory;
        }
    }

    /**
     * Returns true if the storage directory for JFR snapshots already exists, false otherwise.
     * 
     * @return true if the storage directory for JFR snapshots already exists, false otherwise.
     */
    public static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }

    
    static void register() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new JFRSnapshotDescriptorProvider());
        JFRSnapshotsContainer.sharedInstance();
        JFRSnapshotProvider.register();
        RegisteredSnapshotCategories.sharedInstance().registerCategory(category);
        DataSourceViewsManager.sharedInstance().addViewProvider(viewProvider, JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotMonitorViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotThreadsViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotSamplerViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotLocksViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotFileIOViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotSocketIOViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotBrowserViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotEnvironmentViewProvider(), JFRSnapshot.class);
        DataSourceViewsManager.sharedInstance().addViewProvider(new JFRSnapshotRecordingViewProvider(), JFRSnapshot.class);
    }

}

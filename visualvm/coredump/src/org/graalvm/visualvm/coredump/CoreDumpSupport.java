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

package org.graalvm.visualvm.coredump;

import org.graalvm.visualvm.coredump.impl.CoreDumpCategory;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;
import org.graalvm.visualvm.coredump.impl.CoreDumpDescriptorProvider;
import org.graalvm.visualvm.coredump.impl.CoreDumpOverviewViewProvider;
import org.graalvm.visualvm.coredump.impl.CoreDumpProvider;
import java.io.File;

/**
 * Support for coredumps in VisualVM.
 *
 * @author Tomas Hurka
 */
public final class CoreDumpSupport {
    
    private static final String COREDUMPS_STORAGE_DIRNAME = "coredumps";    // NOI18N
    
    private static final Object coredumpsStorageDirectoryLock = new Object();
    // @GuardedBy coredumpsStorageDirectoryLock
    private static File coredumpsStorageDirectory;
    private static final Object coredumpsStorageDirectoryStringLock = new Object();
    // @GuardedBy coredumpsStorageDirectoryStringLock
    private static String coredumpsStorageDirectoryString;
    
    private static CoreDumpOverviewViewProvider viewProvider = new CoreDumpOverviewViewProvider();
    private static CoreDumpCategory category = new CoreDumpCategory();
    private static final Object currentJDKHomeLock = new Object();
    // @GuardedBy currentJDKHomeLock
    private static String currentJDKHome;
    
    
    /**
     * Returns PluggableDataSourceViewProvider for Overview coredump subtab.
     * 
     * @return PluggableDataSourceViewProvider for Overview coredump subtab.
     */
    public static PluggableDataSourceViewProvider<CoreDump> getOverviewView() {
        return viewProvider;
    } 
    
    /**
     * Returns SnapshotCategory instance for coredumps.
     * 
     * @return SnapshotCategory instance for coredumps.
     */
    public static SnapshotCategory getCategory() {
        return category;
    }
    
    // TODO: should be moved to some public Utils class
    /**
     * Returns JDK_HOME for JDK running the actual VisualVM instance.
     * 
     * @return JDK_HOME for JDK running the actual VisualVM instance.
     */
    public static String getCurrentJDKHome() {
        synchronized(currentJDKHomeLock) {
            if (currentJDKHome == null) {
                currentJDKHome = System.getProperty("java.home");   // NOI18N
                String jreSuffix = File.separator + "jre";  // NOI18N
                if (currentJDKHome.endsWith(jreSuffix)) currentJDKHome = currentJDKHome.substring(0, currentJDKHome.length() - jreSuffix.length());
            }
            return currentJDKHome;
        }
    }
    
    static String getStorageDirectoryString() {
        synchronized(coredumpsStorageDirectoryStringLock) {
            if (coredumpsStorageDirectoryString == null)
                coredumpsStorageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + COREDUMPS_STORAGE_DIRNAME;
            return coredumpsStorageDirectoryString;
        }
    }
    
    /**
     * Returns storage directory for coredumps.
     * 
     * @return storage directory for coredumps.
     */
    public static File getStorageDirectory() {
        synchronized(coredumpsStorageDirectoryLock) {
            if (coredumpsStorageDirectory == null) {
                String snapshotsStorageString = getStorageDirectoryString();
                coredumpsStorageDirectory = new File(snapshotsStorageString);
                if (coredumpsStorageDirectory.exists() && coredumpsStorageDirectory.isFile())
                    throw new IllegalStateException("Cannot create coredumps storage directory " + snapshotsStorageString + ", file in the way");   // NOI18N
                if (coredumpsStorageDirectory.exists() && (!coredumpsStorageDirectory.canRead() || !coredumpsStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access coredumps storage directory " + snapshotsStorageString + ", read&write permission required");    // NOI18N
                if (!Utils.prepareDirectory(coredumpsStorageDirectory))
                    throw new IllegalStateException("Cannot create coredumps storage directory " + snapshotsStorageString); // NOI18N
            }
            return coredumpsStorageDirectory;
        }
    }

    /**
     * Returns true if the storage directory for coredumps already exists, false otherwise.
     * 
     * @return true if the storage directory for coredumps already exists, false otherwise.
     */
    public static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }

    
    static void register() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new CoreDumpDescriptorProvider());
        CoreDumpsContainer.sharedInstance();
        CoreDumpProvider.register();
        RegisteredSnapshotCategories.sharedInstance().registerCategory(category);
        DataSourceViewsManager.sharedInstance().addViewProvider(viewProvider, CoreDump.class);
    }

}

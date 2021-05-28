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

import java.io.File;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.jfr.impl.JFRSnapshotCategory;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.jfr.impl.JFRRecordingProvider;
import org.graalvm.visualvm.jfr.impl.JFRSnapshotDescriptorProvider;
import org.graalvm.visualvm.jfr.impl.JFRSnapshotProvider;
import org.graalvm.visualvm.jfr.view.JFRViewProvider;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.openide.util.NbBundle;

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
    
    private static final JFRSnapshotCategory category = new JFRSnapshotCategory();
    private static JFRRecordingProvider jfrDumpProvider;
    
    
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
        
        DataSourceViewsManager views = DataSourceViewsManager.sharedInstance();
        views.addViewProvider(new JFRViewProvider(), JFRSnapshot.class);
        jfrDumpProvider = new JFRRecordingProvider();
        jfrDumpProvider.initialize();
    }

    public static void takeJfrDump(Application application, boolean openView) {
        jfrDumpProvider.createJfrDump(application, openView);
    }

    public static void takeRemoteJfrDump(Application application, String dumpFile, boolean customizeDumpFile) {
        jfrDumpProvider.createRemoteJfrDump(application, dumpFile, customizeDumpFile);
    }

    public static boolean supportsJfrDump(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (jvm == null || !jvm.isJfrAvailable()) return false;
        return !jvm.jfrCheck().isEmpty();
    }

    public static boolean supportsRemoteJfrDump(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        if (application.isLocalApplication()) return false; // Should be allowed???
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null || !jmxModel.isJfrAvailable()) return false;
        return !jmxModel.jfrCheck().isEmpty();
    }

    public static void jfrStartRecording(Application application) {
        checkNotifyCommercialFeatures(application);
        jfrDumpProvider.jfrStartRecording(application);
    }

    public static void remoteJfrStartRecording(Application application) {
        checkNotifyCommercialFeatures(application);
        jfrDumpProvider.remoteJfrStartRecording(application);
    }

    public static boolean supportsJfrStart(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (jvm == null || !jvm.isJfrAvailable()) return false;
        return jvm.jfrCheck().isEmpty();
    }

    public static boolean supportsRemoteJfrStart(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        if (application.isLocalApplication()) return false; // Should be allowed???
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null || !jmxModel.isJfrAvailable()) return false;
        return jmxModel.jfrCheck().isEmpty();
    }

    public static void jfrStopRecording(Application application) {
        jfrDumpProvider.jfrStopRecording(application);
    }

    public static void remoteJfrStopRecording(Application application) {
        jfrDumpProvider.remoteJfrStopRecording(application);
    }

    public static boolean supportsJfrStop(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (jvm == null || !jvm.isJfrAvailable()) return false;
        return !jvm.jfrCheck().isEmpty();
    }

    public static boolean supportsRemoteJfrStop(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        if (application.isLocalApplication()) return false; // Should be allowed???
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null || !jmxModel.isJfrAvailable()) return false;
        return !jmxModel.jfrCheck().isEmpty();
    }
    
    
    public static void checkNotifyCommercialFeatures(Application application) {
        if (requiresUnlockCommercialFeatures(application))
            displayUnlockCommercialFeaturesNotification(application);
    }
    
    public static boolean requiresUnlockCommercialFeatures(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (!jvm.is18() && !jvm.is19() && !jvm.is100()) return false;
        String vmVendor = jvm.getVmVendor();
        return vmVendor != null && vmVendor.contains("Oracle");                 // NOI18N
    }
    
    private static void displayUnlockCommercialFeaturesNotification(Application application) {
        ProfilerDialogs.displayWarningDNSA(NbBundle.getMessage(JFRSnapshotDescriptor.class, "Msg_CommercialFeatures"), // NOI18N
                                           NbBundle.getMessage(JFRSnapshotDescriptor.class, "Caption_CommercialFeatures"), // NOI18N
                                           null, "JFRSnapshotSupport_NotifyCommercialFeatures", true); // NOI18N
    }
    
}

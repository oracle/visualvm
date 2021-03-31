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

package org.graalvm.visualvm.jfr.impl;

import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.explorer.ExplorerSupport;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.jfr.JFRSnapshotSupport;
import org.graalvm.visualvm.jfr.JFRSnapshotsContainer;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.VisualVM;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.ui.SwingWorker;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
public class JFRSnapshotProvider {
    private static final Logger LOGGER = Logger.getLogger(JFRSnapshotProvider.class.getName());
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";  // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static class SnapshotAdder extends SwingWorker {
        volatile private ProgressHandle ph = null;
        volatile private boolean success = false;
        private JFRSnapshotImpl newSnapshot;
        private Storage storage;
        private String[] propNames, propValues;
        
        SnapshotAdder(JFRSnapshotImpl newSnapshot, Storage storage, String[] propNames, String[] propValues) {
            this.newSnapshot = newSnapshot;
            this.storage = storage;
            this.propValues = propValues;
            this.propNames = propNames;
        }
        
        @Override
        protected void doInBackground() {
//            SaModel model = SaModelFactory.getSAAgentFor(newSnapshot);
//            if (model != null) {
                storage.setCustomProperties(propNames, propValues);
                JFRSnapshotsContainer.sharedInstance().getRepository().addDataSource(newSnapshot);

                success = true;
//            }
        }

        @Override
        protected void nonResponding() {
            ph = ProgressHandle.createHandle(NbBundle.getMessage(JFRSnapshotProvider.class, "LBL_Inspecting_core_dump"));   // NOI18N
            ph.start();
        }

        @Override
        protected void done() {
            if (ph != null) {
                ph.finish();
            }
            if (!success) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(NbBundle.getMessage(JFRSnapshotProvider.class, "MSG_not_valid_core_dump", newSnapshot.getFile().getAbsolutePath())));  // NOI18N
            }
        }
    }
    
    static void createJFRSnapshot(final String jfrSnapshotFile, final String displayName, final boolean deleteJFRSnapshot) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                createJFRSnapshotImpl(jfrSnapshotFile, displayName, deleteJFRSnapshot);
            }
        });
    }
    
    private static void createJFRSnapshotImpl(String jfrSnapshotFile, final String displayName, boolean deleteJFRSnapshot) {
        // TODO: check if the same JFR snapshot isn't already imported (can happen for moved coredumps)
        final JFRSnapshotImpl knownJFRSnapshot = getJFRSnapshotByFile(new File(jfrSnapshotFile));
        if (knownJFRSnapshot != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExplorerSupport.sharedInstance().selectDataSource(knownJFRSnapshot);
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(JFRSnapshotProvider.class,
                            "MSG_Core_dump_already_added", new Object[] {displayName, // NOI18N
                            DataSourceDescriptorFactory.getDescriptor(knownJFRSnapshot).
                            getName()}), NotifyDescriptor.ERROR_MESSAGE));
                }
            });
            return;
        }
        
        if (deleteJFRSnapshot) {
            ProgressHandle pHandle = null;
            try {
                pHandle = ProgressHandle.createHandle(NbBundle.getMessage(JFRSnapshotProvider.class, "MSG_Adding", displayName));   // NOI18N
                pHandle.setInitialDelay(0);
                pHandle.start();
                
                File file = new File(jfrSnapshotFile);
                File copy = Utils.getUniqueFile(JFRSnapshotSupport.getStorageDirectory(), file.getName());
                if (Utils.copyFile(file, copy)) {
                    jfrSnapshotFile = copy.getAbsolutePath();
                    if (!file.delete()) file.deleteOnExit();
                }
            } finally {
                final ProgressHandle pHandleF = pHandle;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { if (pHandleF != null) pHandleF.finish(); }
                });
            }
        }
        
        final String[] propNames = new String[] {
            SNAPSHOT_VERSION,
            Snapshot.PROPERTY_FILE,
            DataSourceDescriptor.PROPERTY_NAME };
        final String[] propValues = new String[] {
            CURRENT_SNAPSHOT_VERSION,
            jfrSnapshotFile,
            displayName
        };

        File customPropertiesStorage = Utils.getUniqueFile(JFRSnapshotSupport.getStorageDirectory(), new File(jfrSnapshotFile).getName(), Storage.DEFAULT_PROPERTIES_EXT);
        Storage storage = new Storage(customPropertiesStorage.getParentFile(), customPropertiesStorage.getName());
        
        try {
            JFRSnapshotImpl newJFRSnapshot = new JFRSnapshotImpl(new File(jfrSnapshotFile), storage);
            if (newJFRSnapshot != null) new SnapshotAdder(newJFRSnapshot, storage, propNames, propValues).execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating JFR snapshot", e); // NOI18N
        }
    }
    
    private static JFRSnapshotImpl getJFRSnapshotByFile(File file) {
        if (!file.isFile()) return null;
        Set<JFRSnapshotImpl> knownJFRSnapshots = DataSourceRepository.sharedInstance().getDataSources(JFRSnapshotImpl.class);
        for (JFRSnapshotImpl knownJFRSnapshot : knownJFRSnapshots)
            if (knownJFRSnapshot.getFile().equals(file)) return knownJFRSnapshot;
        return null;
    }
    
    private void initPersistedJFRSnapshots() {
        if (!JFRSnapshotSupport.storageDirectoryExists()) return;
        
        File[] files = JFRSnapshotSupport.getStorageDirectory().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
            }
        });
        
        Set<File> unresolvedJFRSnapshotsF = new HashSet();
        Set<String> unresolvedJFRSnapshotsS = new HashSet();
        Set<JFRSnapshotImpl> snapshots = new HashSet();
        for (File file : files) {
            Storage storage = new Storage(file.getParentFile(), file.getName());
            String[] propNames = new String[] {
                Snapshot.PROPERTY_FILE
            };
            String[] propValues = storage.getCustomProperties(propNames);
            if (propValues[0] == null) continue;
                
            JFRSnapshotImpl persistedSnapshot = null;
            try {
                persistedSnapshot = new JFRSnapshotImpl(new File(propValues[0]), storage);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error loading persisted JFR snapshot", e);    // NOI18N
                unresolvedJFRSnapshotsF.add(file);
                unresolvedJFRSnapshotsS.add(propValues[0]);
            }
            
            if (persistedSnapshot != null) snapshots.add(persistedSnapshot);
        }
        
        if (!unresolvedJFRSnapshotsF.isEmpty()) notifyUnresolvedJFRSnapshots(unresolvedJFRSnapshotsF, unresolvedJFRSnapshotsS);
        
        if (!snapshots.isEmpty())
            JFRSnapshotsContainer.sharedInstance().getRepository().addDataSources(snapshots);
    }

    private static void notifyUnresolvedJFRSnapshots(final Set<File> unresolvedJFRSnapshotsF, final Set<String> unresolvedJFRSnapshotsS) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
                messagePanel.add(new JLabel(NbBundle.getMessage(JFRSnapshotProvider.class, "MSG_Unresolved_CoreDumps")), BorderLayout.NORTH); // NOI18N
                JList list = new JList(unresolvedJFRSnapshotsS.toArray());
                list.setVisibleRowCount(4);
                messagePanel.add(new JScrollPane(list), BorderLayout.CENTER);
                NotifyDescriptor dd = new NotifyDescriptor(
                        messagePanel, NbBundle.getMessage(JFRSnapshotProvider.class, "Title_Unresolved_CoreDumps"), // NOI18N
                        NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.ERROR_MESSAGE,
                        null, NotifyDescriptor.YES_OPTION);
                if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.NO_OPTION)
                    for (File file : unresolvedJFRSnapshotsF) Utils.delete(file, true);

                unresolvedJFRSnapshotsF.clear();
                unresolvedJFRSnapshotsS.clear();
            }
        }, 1000);
    }

    
    JFRSnapshotProvider() {
    }
    
    public static void register() {
        final JFRSnapshotProvider provider = new JFRSnapshotProvider();
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                VisualVM.getInstance().runTask(new Runnable() {
                    public void run() {
                        provider.initPersistedJFRSnapshots();
                    }
                });
            }
        });
    }
  
}

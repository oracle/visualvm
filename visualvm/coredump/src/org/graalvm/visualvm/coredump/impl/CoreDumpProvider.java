/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.coredump.impl;

import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.explorer.ExplorerSupport;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.coredump.CoreDumpSupport;
import org.graalvm.visualvm.coredump.CoreDumpsContainer;
import org.graalvm.visualvm.tools.sa.SaModel;
import org.graalvm.visualvm.tools.sa.SaModelFactory;
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
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class CoreDumpProvider {
    private static final Logger LOGGER = Logger.getLogger(CoreDumpProvider.class.getName());
    
    private static final String SNAPSHOT_VERSION = "snapshot_version";  // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROPERTY_JAVA_HOME = "prop_java_home";  // NOI18N
    
    private static class CoreDumpAdder extends SwingWorker {
        volatile private ProgressHandle ph = null;
        volatile private boolean success = false;
        private CoreDumpImpl newCoreDump;
        private Storage storage;
        private String[] propNames, propValues;
        
        CoreDumpAdder(CoreDumpImpl newCoreDump, Storage storage, String[] propNames, String[] propValues) {
            this.newCoreDump = newCoreDump;
            this.storage = storage;
            this.propValues = propValues;
            this.propNames = propNames;
        }
        
        @Override
        protected void doInBackground() {
            SaModel model = SaModelFactory.getSAAgentFor(newCoreDump);
            if (model != null) {
                storage.setCustomProperties(propNames, propValues);
                CoreDumpsContainer.sharedInstance().getRepository().addDataSource(newCoreDump);

                success = true;
            }
        }

        @Override
        protected void nonResponding() {
            ph = ProgressHandle.createHandle(NbBundle.getMessage(CoreDumpProvider.class, "LBL_Inspecting_core_dump"));   // NOI18N
            ph.start();
        }

        @Override
        protected void done() {
            if (ph != null) {
                ph.finish();
            }
            if (!success) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(NbBundle.getMessage(CoreDumpProvider.class, "MSG_not_valid_core_dump", newCoreDump.getFile().getAbsolutePath())));  // NOI18N
            }
        }
    }
    
    static void createCoreDump(final String coreDumpFile, final String displayName, final String jdkHome, final boolean deleteCoreDump) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                createCoreDumpImpl(coreDumpFile, displayName, jdkHome, deleteCoreDump);
            }
        });
    }
    
    private static void createCoreDumpImpl(String coreDumpFile, final String displayName, String jdkHome, boolean deleteCoreDump) {
        
        // TODO: check if the same coredump isn't already imported (can happen for moved coredumps)
        
        final CoreDumpImpl knownCoreDump = getCoreDumpByFile(new File(coreDumpFile));
        if (knownCoreDump != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExplorerSupport.sharedInstance().selectDataSource(knownCoreDump);
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(CoreDumpProvider.class,
                            "MSG_Core_dump_already_added", new Object[] {displayName, // NOI18N
                            DataSourceDescriptorFactory.getDescriptor(knownCoreDump).
                            getName()}), NotifyDescriptor.ERROR_MESSAGE));
                }
            });
            return;
        }
        
        if (deleteCoreDump) {
            ProgressHandle pHandle = null;
            try {
                pHandle = ProgressHandle.createHandle(NbBundle.getMessage(CoreDumpProvider.class, "MSG_Adding", displayName));   // NOI18N
                pHandle.setInitialDelay(0);
                pHandle.start();
                
                File file = new File(coreDumpFile);
                File copy = Utils.getUniqueFile(CoreDumpSupport.getStorageDirectory(), file.getName());
                if (Utils.copyFile(file, copy)) {
                    coreDumpFile = copy.getAbsolutePath();
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
            DataSourceDescriptor.PROPERTY_NAME,
            PROPERTY_JAVA_HOME };
        final String[] propValues = new String[] {
            CURRENT_SNAPSHOT_VERSION,
            coreDumpFile,
            displayName,
            jdkHome
        };

        File customPropertiesStorage = Utils.getUniqueFile(CoreDumpSupport.getStorageDirectory(), new File(coreDumpFile).getName(), Storage.DEFAULT_PROPERTIES_EXT);
        Storage storage = new Storage(customPropertiesStorage.getParentFile(), customPropertiesStorage.getName());
        
        try {
            CoreDumpImpl newCoreDump = new CoreDumpImpl(new File(coreDumpFile), new File(jdkHome), storage);
            if (newCoreDump != null) {
                new CoreDumpAdder(newCoreDump, storage, propNames, propValues).execute();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating coredump", e); // NOI18N
            return;
        }
    }
    
    private static CoreDumpImpl getCoreDumpByFile(File file) {
        if (!file.isFile()) return null;
        Set<CoreDumpImpl> knownCoredumps = DataSourceRepository.sharedInstance().getDataSources(CoreDumpImpl.class);
        for (CoreDumpImpl knownCoredump : knownCoredumps)
            if (knownCoredump.getFile().equals(file)) return knownCoredump;
        return null;
    }
    
    private void initPersistedCoreDumps() {
        if (!CoreDumpSupport.storageDirectoryExists()) return;
        
        File[] files = CoreDumpSupport.getStorageDirectory().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
            }
        });
        
        Set<File> unresolvedCoreDumpsF = new HashSet<>();
        Set<String> unresolvedCoreDumpsS = new HashSet<>();
        Set<CoreDumpImpl> coredumps = new HashSet<>();
        for (File file : files) {
            Storage storage = new Storage(file.getParentFile(), file.getName());
            String[] propNames = new String[] {
                Snapshot.PROPERTY_FILE,
                PROPERTY_JAVA_HOME
            };
            String[] propValues = storage.getCustomProperties(propNames);
            if (propValues[0] == null || propValues[1] == null) continue;
                
            CoreDumpImpl persistedCoredump = null;
            try {
                persistedCoredump = new CoreDumpImpl(new File(propValues[0]), new File(propValues[1]), storage);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error loading persisted coredump", e);    // NOI18N
                unresolvedCoreDumpsF.add(file);
                unresolvedCoreDumpsS.add(propValues[0]);
            }
            
            if (persistedCoredump != null) coredumps.add(persistedCoredump);
        }
        
        if (!unresolvedCoreDumpsF.isEmpty()) notifyUnresolvedCoreDumps(unresolvedCoreDumpsF, unresolvedCoreDumpsS);
        
        if (!coredumps.isEmpty())
            CoreDumpsContainer.sharedInstance().getRepository().addDataSources(coredumps);
    }

    private static void notifyUnresolvedCoreDumps(final Set<File> unresolvedCoreDumpsF, final Set<String> unresolvedCoreDumpsS) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
                messagePanel.add(new JLabel(NbBundle.getMessage(CoreDumpProvider.class, "MSG_Unresolved_CoreDumps")), BorderLayout.NORTH); // NOI18N
                JList<Object> list = new JList<>(unresolvedCoreDumpsS.toArray());
                list.setVisibleRowCount(4);
                messagePanel.add(new JScrollPane(list), BorderLayout.CENTER);
                NotifyDescriptor dd = new NotifyDescriptor(
                        messagePanel, NbBundle.getMessage(CoreDumpProvider.class, "Title_Unresolved_CoreDumps"), // NOI18N
                        NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.ERROR_MESSAGE,
                        null, NotifyDescriptor.YES_OPTION);
                if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.NO_OPTION)
                    for (File file : unresolvedCoreDumpsF) Utils.delete(file, true);

                unresolvedCoreDumpsF.clear();
                unresolvedCoreDumpsS.clear();
            }
        }, 1000);
    }

    
    CoreDumpProvider() {
    }
    
    public static void register() {
        final CoreDumpProvider provider = new CoreDumpProvider();
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                VisualVM.getInstance().runTask(provider::initPersistedCoreDumps);
            }
        });
    }
  
}

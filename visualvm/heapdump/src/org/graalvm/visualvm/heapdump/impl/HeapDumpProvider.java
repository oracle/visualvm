/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapdump.impl;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.coredump.CoreDump;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.sa.SaModel;
import org.graalvm.visualvm.tools.sa.SaModelFactory;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.core.VisualVM;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class HeapDumpProvider {
    
    private final static Logger LOGGER = Logger.getLogger(HeapDumpProvider.class.getName());
    
    public void createHeapDump(final Application application, final boolean openView) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                Jvm jvm = JvmFactory.getJVMFor(application);
                if (!jvm.isTakeHeapDumpSupported()) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(HeapDumpProvider.class,
                            "MSG_Cannot_take_heap_dump") + DataSourceDescriptorFactory. // NOI18N
                            getDescriptor(application).getName(), NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }
                
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(
                            HeapDumpProvider.class, "LBL_Creating_Heap_Dump"));    // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    try {
                        File file = jvm.takeHeapDump();
                        if (file != null && file.isFile()) {
                            final HeapDumpImpl heapDump = new HeapDumpImpl(file, application);
                            application.getRepository().addDataSource(heapDump);
                            if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
                                public void run() { DataSourceWindowManager.sharedInstance().openDataSource(heapDump); }
                            });
                        } else {
                            notifyHeapDumpFailed(application);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.INFO, "createHeapDump-Application", ex); // NOI18N
                        notifyHeapDumpFailed(application);
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
    
    public void createRemoteHeapDump(final Application application, final String dumpFile,
                                     final boolean customizeDumpFile) {
        
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                JmxModel model = JmxModelFactory.getJmxModelFor(application);
                if (model == null || !model.isTakeHeapDumpSupported()) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(HeapDumpProvider.class,
                            "MSG_Dump_failed"), NotifyDescriptor.ERROR_MESSAGE)); // NOI18N
                    return;
                }
                
                String file = dumpFile;
                if (file == null) file = defineRemoteFile(model, customizeDumpFile);
                if (file == null) return;

                if (model.takeHeapDump(file)) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(HeapDumpProvider.class,
                            "MSG_Dump_ok", file), NotifyDescriptor.INFORMATION_MESSAGE)); // NOI18N
                } else {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(HeapDumpProvider.class,
                            "MSG_Dump_save_failed", file), NotifyDescriptor.ERROR_MESSAGE)); // NOI18N
                }
            }
        });
    }
    
    private static String defineRemoteFile(JmxModel model, boolean customizeDumpFile) {
        final String[] path = new String[1];
        path[0] = defaultHeapDumpPath(model);
        
        if (customizeDumpFile) try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JLabel label = new JLabel();
                    Mnemonics.setLocalizedText(label, NbBundle.getMessage(
                            HeapDumpProvider.class, "MSG_Remote_heap_dump")); // NOI18N
                    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                    JTextField field = new JTextField();
                    label.setLabelFor(field);
                    field.setText(path[0]);
                    Dimension dim = field.getPreferredSize();
                    dim.width = 350;
                    field.setPreferredSize(dim);
                    field.selectAll();
                    JPanel selector = new JPanel(new BorderLayout());
                    selector.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
                    selector.add(label, BorderLayout.NORTH);
                    selector.add(field, BorderLayout.SOUTH);

                    DialogDescriptor dd = new DialogDescriptor(selector,
                            NbBundle.getMessage(HeapDumpProvider.class,
                            "CAPTION_Remote_heap_dump"), true, null); // NOI18N
                    Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                    d.pack();
                    d.setVisible(true);

                    path[0] = dd.getValue() == DialogDescriptor.OK_OPTION ?
                                               field.getText() : null;
                }
            });
        } catch (Throwable t) {
            path[0] = null;
        }
        
        return path[0];
    }
    
    private static String defaultHeapDumpPath(JmxModel model) {
        String fileName = HeapDumpSupport.getInstance().getCategory().createFileName();
        
        Properties sysprops = model.getSystemProperties();
        if (sysprops == null) return fileName;
        String heapDumpTarget = getHeapDumpTarget(sysprops);
        if (heapDumpTarget == null || heapDumpTarget.isEmpty()) return fileName;
        
        String pathsep = sysprops.getProperty("file.separator"); // NOI18N
        if (!heapDumpTarget.endsWith(pathsep)) heapDumpTarget += pathsep;
        return heapDumpTarget + fileName;
    }
    
    // OS codes listed in org.graalvm.visualvm.lib.jfluid.global.Platform.getOperatingSystem()
    private static String getHeapDumpTarget(Properties sysprops) {
        String targetDir = null;
        
        // Select directory based on target OS
        String osName = sysprops.getProperty("os.name"); // NOI18N
        if (osName != null) {
            if (osName.equals("Solaris") || osName.startsWith("SunOS")) // NOI18N
                targetDir = sysprops.getProperty("user.home"); // NOI18N

            targetDir = sysprops.getProperty("java.io.tmpdir"); // NOI18N
        }
        
        // Fallback to current working directory
        if (targetDir == null) targetDir = sysprops.getProperty("user.dir"); // NOI18N
        
        return targetDir;
    }
    
    public void createHeapDump(final CoreDump coreDump, final boolean openView) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(
                            HeapDumpProvider.class, "LBL_Creating_Heap_Dump"));    // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    File snapshotDir = coreDump.getStorage().getDirectory();
                    String name = HeapDumpSupport.getInstance().getCategory().createFileName();
                    File dumpFile = new File(snapshotDir,name);
                    SaModel saAget = SaModelFactory.getSAAgentFor(coreDump);
                    try {
                        if (saAget.takeHeapDump(dumpFile.getAbsolutePath())) {
                            final HeapDumpImpl heapDump = new HeapDumpImpl(dumpFile, coreDump);
                            coreDump.getRepository().addDataSource(heapDump);
                            if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
                                public void run() { DataSourceWindowManager.sharedInstance().openDataSource(heapDump); }
                            });
                        } else {
                            notifyHeapDumpFailed(coreDump);
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.INFO, "createHeapDump-CoreDump", ex); // NOI18N
                        notifyHeapDumpFailed(coreDump);
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
    
    public void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(new SnapshotListener(), Snapshot.class);
        DataSourceRepository.sharedInstance().addDataChangeListener(new ApplicationListener(), Application.class);
    }
    
    
    private void processNewSnapshot(Snapshot snapshot) {
        if (snapshot instanceof HeapDumpImpl) return;
        boolean appSnapshot = snapshot instanceof ApplicationSnapshot;
        File snapshotFile = snapshot.getFile();
        if (snapshotFile != null && snapshotFile.isDirectory()) {
            File[] files = snapshotFile.listFiles(HeapDumpSupport.getInstance().getCategory().getFilenameFilter());
            if (files == null) return;
            Set<HeapDumpImpl> heapDumps = new HashSet();
            for (File file : files) {
                HeapDumpImpl heapDump = new HeapDumpImpl(file, snapshot);
                if (appSnapshot) heapDump.forceViewClosable(true);
                heapDumps.add(heapDump);
            }
            snapshot.getRepository().addDataSources(heapDumps);
        }
    }
    
    private void processNewApplication(Application application) {
        Storage storage = application.getStorage();
        if (storage.directoryExists()) {
            File[] files = storage.getDirectory().listFiles(HeapDumpSupport.getInstance().getCategory().getFilenameFilter());
            if (files == null) return;
            Set<HeapDumpImpl> heapDumps = new HashSet();
            for (File file : files) heapDumps.add(new HeapDumpImpl(file, application));
            application.getRepository().addDataSources(heapDumps);
        }
    }
    
    private void notifyHeapDumpFailed(final DataSource dataSource) {
        String displayName = DataSourceDescriptorFactory.getDescriptor(dataSource).getName();
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                Message(NbBundle.getMessage(HeapDumpProvider.class,
                "MSG_Cannot_take_heap_dump") + displayName, // NOI18N
                NotifyDescriptor.ERROR_MESSAGE));
    }
    
    
    private class SnapshotListener implements DataChangeListener<Snapshot> {
        
        public void dataChanged(DataChangeEvent<Snapshot> event) {
            final Set<Snapshot> snapshots = event.getAdded();
            if (!snapshots.isEmpty()) VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    for (Snapshot snapshot : snapshots) processNewSnapshot(snapshot);
                }
            });
        }
        
    }
    
    private class ApplicationListener implements DataChangeListener<Application> {
        
        public void dataChanged(DataChangeEvent<Application> event) {
            final Set<Application> applications = event.getAdded();
            if (!applications.isEmpty()) VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    for (Application application : applications) processNewApplication(application);
                }
            });
        }
        
    }
    
}

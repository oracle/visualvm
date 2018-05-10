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

package org.graalvm.visualvm.threaddump.impl;

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
import org.graalvm.visualvm.threaddump.ThreadDumpSupport;
import org.graalvm.visualvm.tools.sa.SaModel;
import org.graalvm.visualvm.tools.sa.SaModelFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class ThreadDumpProvider {
    
    public void createThreadDump(final Application application, final boolean openView) {
         RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                Jvm jvm = JvmFactory.getJVMFor(application);
                if (!jvm.isTakeThreadDumpSupported()) {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.
                            Message(NbBundle.getMessage(ThreadDumpProvider.class,
                            "MSG_Cannot_take_thread_dump_for_") + DataSourceDescriptorFactory. // NOI18N
                            getDescriptor(application).getName(), NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }
                
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ThreadDumpProvider.class, "MSG_Creating_Thread_Dump"));     // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    try {
                        final ThreadDumpImpl threadDump = new ThreadDumpImpl(jvm.takeThreadDump(), application);
                        application.getRepository().addDataSource(threadDump);
                        if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
                            public void run() { DataSourceWindowManager.sharedInstance().openDataSource(threadDump); }
                        });
                    } catch (IOException ex) {
                        ErrorManager.getDefault().notify(ex);
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
    
    public void createThreadDump(final CoreDump coreDump, final boolean openView) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ThreadDumpProvider.class, "MSG_Creating_Thread_Dump"));     // NOI18N
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    File snapshotDir = coreDump.getStorage().getDirectory();
                    String name = ThreadDumpSupport.getInstance().getCategory().createFileName();
                    File dumpFile = new File(snapshotDir,name);
                    SaModel saAget = SaModelFactory.getSAAgentFor(coreDump);
                    String dump = saAget.takeThreadDump();
                    if (dump != null) {
                        try {
                            OutputStream os = new FileOutputStream(dumpFile);
                            os.write(dump.getBytes("UTF-8"));    // NOI18N
                            os.close();
                            final ThreadDumpImpl threadDump = new ThreadDumpImpl(dumpFile, coreDump);
                            coreDump.getRepository().addDataSource(threadDump);
                            if (openView) DataSource.EVENT_QUEUE.post(new Runnable() {
                                public void run() { DataSourceWindowManager.sharedInstance().openDataSource(threadDump); }
                            });
                        } catch (Exception ex) {
                            ErrorManager.getDefault().notify(ex);
                        }
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
        if (snapshot instanceof ThreadDumpImpl) return;
        File snapshotFile = snapshot.getFile();
        if (snapshotFile != null && snapshotFile.isDirectory()) {
            File[] files = snapshotFile.listFiles(ThreadDumpSupport.getInstance().getCategory().getFilenameFilter());
            if (files == null) return;
            Set<ThreadDumpImpl> threadDumps = new HashSet();
            for (File file : files) threadDumps.add(new ThreadDumpImpl(file, snapshot));
            snapshot.getRepository().addDataSources(threadDumps);
        }
    }
    
    private void processNewApplication(Application application) {
        Storage storage = application.getStorage();
        if (storage.directoryExists()) {
            File[] files = storage.getDirectory().listFiles(ThreadDumpSupport.getInstance().getCategory().getFilenameFilter());
            if (files == null) return;
            Set<ThreadDumpImpl> threadDumps = new HashSet();
            for (File file : files) threadDumps.add(new ThreadDumpImpl(file, application));
            application.getRepository().addDataSources(threadDumps);
        }
    }
    
    
    private class SnapshotListener implements DataChangeListener<Snapshot> {
        
        public void dataChanged(DataChangeEvent<Snapshot> event) {
            final Set<Snapshot> snapshots = event.getAdded();
            if (!snapshots.isEmpty()) RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    for (Snapshot snapshot : snapshots) processNewSnapshot(snapshot);
                }
            });
        }
        
    }
    
    private class ApplicationListener implements DataChangeListener<Application> {
        
        public void dataChanged(DataChangeEvent<Application> event) {
            final Set<Application> applications = event.getAdded();
            if (!applications.isEmpty()) RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    for (Application application : applications) processNewApplication(application);
                }
            });
        }
        
    }
    
}

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

package com.sun.tools.visualvm.core.threaddump;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.CoreDump;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.snapshot.SnapshotProvider;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.tools.sa.SAAgent;
import com.sun.tools.visualvm.core.tools.sa.SAAgentFactory;
import com.sun.tools.visualvm.core.ui.DataSourceUIManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ThreadDumpProvider extends SnapshotProvider<ThreadDumpImpl> {
    
    void createThreadDump(final Application application, final boolean openView) {
         RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle("Creating Thread Dump...");
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    JVM jvm = JVMFactory.getJVMFor(application);
                    if (jvm.isTakeThreadDumpSupported()) {
                        try {
                            final ThreadDumpImpl threadDump = new ThreadDumpImpl(jvm.takeThreadDump(), application);
                            application.getRepository().addDataSource(threadDump);
                            registerDataSource(threadDump);
                            if (openView) SwingUtilities.invokeLater(new Runnable() {
                                public void run() { DataSourceUIManager.sharedInstance().addView(application, threadDump); }
                            });
                            application.notifyWhenFinished(new DataFinishedListener<Application>() {
                                public void dataFinished(Application dataSource) { removeThreadDumps(dataSource, false); }
                            });
                        } catch (IOException ex) {
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
    
    private void removeThreadDumps(Application application, boolean delete) {
        Set<ThreadDumpImpl> threadDumps = application.getRepository().getDataSources(ThreadDumpImpl.class);
        application.getRepository().removeDataSources(threadDumps);
        unregisterDataSources(threadDumps);
        if (delete) for (ThreadDumpImpl threadDump : threadDumps) threadDump.delete();
    }
    
    void createThreadDump(final CoreDump coreDump, final boolean openView) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProgressHandle pHandle = null;
                try {
                    pHandle = ProgressHandleFactory.createHandle("Creating Thread Dump...");
                    pHandle.setInitialDelay(0);
                    pHandle.start();
                    File snapshotDir = coreDump.getStorage();
                    String name = ThreadDumpSupport.getInstance().getCategory().createSnapshotName();
                    File dumpFile = new File(snapshotDir,name);
                    SAAgent saAget = SAAgentFactory.getSAAgentFor(coreDump);
                    String dump = saAget.takeThreadDump();
                    if (dump != null) {
                        try {
                            OutputStream os = new FileOutputStream(dumpFile);
                            os.write(dump.getBytes("UTF-8"));
                            os.close();
                            final ThreadDumpImpl threadDump = new ThreadDumpImpl(dumpFile, coreDump);
                            coreDump.getRepository().addDataSource(threadDump);
                            registerDataSource(threadDump);
                            if (openView) SwingUtilities.invokeLater(new Runnable() {
                                public void run() { DataSourceUIManager.sharedInstance().addView(coreDump, threadDump); }
                            });
                            coreDump.notifyWhenFinished(new DataFinishedListener<CoreDump>() {
                                public void dataFinished(CoreDump dataSource) { removeThreadDumps(dataSource, false); }
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
    
    private void removeThreadDumps(CoreDump coreDump, boolean delete) {
        Set<ThreadDumpImpl> threadDumps = coreDump.getRepository().getDataSources(ThreadDumpImpl.class);
        coreDump.getRepository().removeDataSources(threadDumps);
        unregisterDataSources(threadDumps);
        if (delete) for (ThreadDumpImpl threadDump : threadDumps) threadDump.delete();
    }
    
    void deleteThreadDump(ThreadDumpImpl threadDump) {
        if (threadDump.getOwner() != null) threadDump.getOwner().getRepository().removeDataSource(threadDump);
        unregisterDataSource(threadDump);
        threadDump.delete();
    }
    
    protected <Y extends ThreadDumpImpl> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (ThreadDumpImpl threadDump : removed) threadDump.removed();
    }
    
    void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
    }
    
}

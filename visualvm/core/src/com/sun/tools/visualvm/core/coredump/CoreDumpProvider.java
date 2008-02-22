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

package com.sun.tools.visualvm.core.coredump;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.core.snapshot.SnapshotProvider;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
// A provider for Coredumps
class CoreDumpProvider extends SnapshotProvider<CoreDumpImpl> {
    
    private static CoreDumpProvider sharedInstance;
    
    public synchronized static CoreDumpProvider sharedInstance() {
        if (sharedInstance == null) sharedInstance = new CoreDumpProvider();
        return sharedInstance;
    }
    
    
    public Snapshot loadSnapshot(File file, DataSource master) {
        // TODO: check how to process registering/unregistering new DataSource
        try {
            return new CoreDumpImpl(file, file.getName(), CoreDumpSupport.getCurrentJDKHome());
        } catch (Exception e) { return null; }
    }
    
    
    void createCoreDump(final String coreDumpFile, final String displayName, final String jdkHome) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                createCoreDumpImpl(coreDumpFile, displayName, jdkHome);
            }
        });
    }
    
    private void createCoreDumpImpl(final String coreDumpFile, final String displayName, final String jdkHome) {
        final CoreDumpImpl coreDump;
        try {
            coreDump = new CoreDumpImpl(new File(coreDumpFile), displayName, jdkHome);
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
            return;
        }
        final Set<CoreDumpImpl> knownCoreDumps = getDataSources(CoreDumpImpl.class);
        if (knownCoreDumps.contains(coreDump)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExplorerSupport.sharedInstance().selectDataSource(coreDump);
                    NetBeansProfiler.getDefaultNB().displayWarning("<html>Core dump " + displayName + " already added </html>");
                }
            });
        } else {
            CoreDumpsContainer.sharedInstance().getRepository().addDataSource(coreDump);
            registerDataSource(coreDump);
        }
    }

    void removeCoreDump(CoreDumpImpl coreDump, boolean interactive) {
        // TODO: if interactive, show a Do-Not-Show-Again confirmation dialog
        unregisterDataSource(coreDump);
    }
    
    
    protected <Y extends CoreDumpImpl> void unregisterDataSources(final Set<Y> removed) {
        super.unregisterDataSources(removed);
        for (CoreDumpImpl coreDump : removed) {
            CoreDumpsContainer.sharedInstance().getRepository().removeDataSource(coreDump);
            coreDump.finished();
        }
    }
    
    CoreDumpProvider() {
    }
    
    void register() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
    }
  
}
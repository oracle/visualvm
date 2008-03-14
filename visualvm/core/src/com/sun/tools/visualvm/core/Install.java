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

package com.sun.tools.visualvm.core;

//import com.sun.tools.visualvm.application.ApplicationsSupport;

import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import java.io.File;
import org.openide.modules.ModuleInstall;

/**
 *
 * @author Jiri Sedlacek
 */
// Class implementing logic on VisualVM module install
public class Install extends ModuleInstall {

    public void restored() {
        // NOTE: this has to be called before any of DataSourceProviders initializes
        cleanupPreviousSession();
        
        DataSourceRepository.sharedInstance();
        
        // Initialize snapshots
        SnapshotsSupport.getInstance();
        
//        org.openide.windows.WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
//            public void run() {
//                RequestProcessor.getDefault().post(new Runnable() {
//                    public void run() { init(); }
//                });
//            }
//        });
    }
    
    private void init() {
        
//        // Initialize hosts
//        HostsSupport.getInstance();
//        
//        // Initialize applications
//        ApplicationsSupport.getInstance();
//
//        // Initialize core dumps
//        CoreDumpSupport.register();
//        
//        // Initialize explorer
//        ExplorerSupport.sharedInstance();
//        
//        // Initialize snapshots
//        SnapshotsSupport.getInstance();
//        
//        // Initialize views
//        OverviewViewSupport.getInstance();
//        MonitorViewSupport.getInstance();
//        ThreadsViewSupport.getInstance();
//        
//        // Initialize profiler
//        ProfilerSupport.getInstance();
//        
//        // Initialize thread dumps
//        ThreadDumpSupport.getInstance();
// 
//        // Initialize heap dumps
//        HeapDumpSupport.getInstance();
//        
//        // Initialize Application snapshots support
//        ApplicationSnapshotsSupport.getInstance();

    }
    
    private void cleanupPreviousSession() {
        File temporaryStorage = new File(Storage.getTemporaryStorageDirectoryString());
        Utils.delete(temporaryStorage, false);
    }
    
}

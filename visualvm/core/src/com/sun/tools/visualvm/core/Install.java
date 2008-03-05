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

import com.sun.tools.visualvm.core.application.ApplicationsSupport;
import com.sun.tools.visualvm.core.coredump.CoreDumpSupport;
import com.sun.tools.visualvm.core.datasupport.Storage;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.dataview.monitor.MonitorViewSupport;
import com.sun.tools.visualvm.core.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.core.host.HostsSupport;
import com.sun.tools.visualvm.core.dataview.overview.OverviewViewSupport;
import com.sun.tools.visualvm.core.dataview.threads.ThreadsViewSupport;
import com.sun.tools.visualvm.core.profiler.ProfilerSupport;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import com.sun.tools.visualvm.core.snapshot.application.ApplicationSnapshotsSupport;
import com.sun.tools.visualvm.core.threaddump.ThreadDumpSupport;
import java.io.File;
import org.openide.modules.ModuleInstall;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
// Class implementing logic on VisualVM module install
public class Install extends ModuleInstall {
    
    // TODO: needs to be implemented differently!!!
    public static final RequestProcessor LAZY_INIT_QUEUE = new RequestProcessor("Lazy Init Queue");

    public void restored() {
        // NOTE: this has to be called before any of DataSourceProviders initializes
        cleanupPreviousSession();
        
        org.openide.windows.WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() { init(); }
                });
            }
        });
    }
    
    private void init() {
        
        // Initialize hosts
        HostsSupport.getInstance();
        
        // Initialize applications
        ApplicationsSupport.getInstance();

        // Initialize core dumps
        CoreDumpSupport.register();
        
        // Initialize snapshots
        SnapshotsSupport.getInstance();
        
        // Initialize views
        OverviewViewSupport.getInstance();
        MonitorViewSupport.getInstance();
        ThreadsViewSupport.getInstance();
        
        // Initialize profiler
        ProfilerSupport.getInstance();
        
        // Initialize thread dumps
        ThreadDumpSupport.getInstance();
 
        // Initialize heap dumps
        HeapDumpSupport.getInstance();
        
        // Initialize Application snapshots support
        ApplicationSnapshotsSupport.getInstance();

    }
    
    private void cleanupPreviousSession() {
        File temporaryStorage = new File(Storage.getTemporaryStorageDirectoryString());
        Utils.delete(temporaryStorage, false);
    }
    
}

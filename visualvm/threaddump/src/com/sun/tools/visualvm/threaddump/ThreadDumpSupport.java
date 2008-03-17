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

package com.sun.tools.visualvm.threaddump;

import com.sun.tools.visualvm.threaddump.impl.ThreadDumpPluggableView;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.core.ui.PluggableViewSupport;
import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.threaddump.impl.ThreadDumpActionsProvider;
import com.sun.tools.visualvm.threaddump.impl.ThreadDumpCategory;
import com.sun.tools.visualvm.threaddump.impl.ThreadDumpDescriptorProvider;
import com.sun.tools.visualvm.threaddump.impl.ThreadDumpProvider;
import com.sun.tools.visualvm.threaddump.impl.ThreadDumpViewProvider;

/**
 * A public entrypoint to the thread dump support in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ThreadDumpSupport {

    private static ThreadDumpSupport instance;

    private final SnapshotCategory category = new ThreadDumpCategory();
    private final ThreadDumpProvider threadDumpProvider;
    private final ThreadDumpViewProvider threadDumpViewProvider;
    private final ThreadDumpPluggableView threadDumpPluggableView;


    /**
     * Returns singleton instance of ThreadDumpSupport.
     * 
     * @return singleton instance of ThreadDumpSupport.
     */
    public static synchronized ThreadDumpSupport getInstance() {
        if (instance == null) instance = new ThreadDumpSupport();
        return instance;
    }
    
    
    /**
     * Returns SnapshotCategory instance for thread dumps.
     * 
     * @return SnapshotCategory instance for thread dumps.
     */
    public SnapshotCategory getCategory() {
        return category;
    }
    
    /**
     * Takes thread dump from Application.
     * 
     * @param application Application to take the thread dump,
     * @param openView true if taken thread dump should be opened, false otherwise.
     */
    public void takeThreadDump(Application application, boolean openView) {
        threadDumpProvider.createThreadDump(application, openView);
    }
    
    public void takeThreadDump(CoreDump coreDump, boolean openView) {
        threadDumpProvider.createThreadDump(coreDump, openView);
    }
    
    /**
     * Returns PluggableView instance to be used to customize the thread dump view.
     * 
     * @return PluggableView instance to be used to customize the thread dump view.
     */
    public PluggableViewSupport getThreadDumpView() {
        return threadDumpPluggableView;
    }


    private ThreadDumpSupport() {
        DataSourceDescriptorFactory.getDefault().registerFactory(new ThreadDumpDescriptorProvider());
        threadDumpProvider = new ThreadDumpProvider();
        threadDumpProvider.initialize();
        
        threadDumpPluggableView = new ThreadDumpPluggableView();
        
        threadDumpViewProvider = new ThreadDumpViewProvider();
        
        RegisteredSnapshotCategories.sharedInstance().addCategory(category);
        
        threadDumpViewProvider.initialize();
        new ThreadDumpActionsProvider().initialize();
    }

}

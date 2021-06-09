/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.threaddump;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;
import org.graalvm.visualvm.coredump.CoreDump;
import org.graalvm.visualvm.threaddump.impl.ThreadDumpCategory;
import org.graalvm.visualvm.threaddump.impl.ThreadDumpDescriptorProvider;
import org.graalvm.visualvm.threaddump.impl.ThreadDumpProvider;
import org.graalvm.visualvm.threaddump.impl.ThreadDumpViewProvider;

/**
 * Support for thread dumps in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ThreadDumpSupport {

    private static ThreadDumpSupport instance;

    private final SnapshotCategory category = new ThreadDumpCategory();
    private final ThreadDumpProvider threadDumpProvider;
    private final ThreadDumpViewProvider threadDumpViewProvider;


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
     * Returns true if taking thread dumps is supported for the application, false otherwise.
     * 
     * @param application application from which to take the thread dump.
     * @return true if taking thread dumps is supported for the application, false otherwise.
     */
    public boolean supportsThreadDump(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        return JvmFactory.getJVMFor(application).isTakeThreadDumpSupported();
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
    
    /**
     * Takes thread dump from CoreDump.
     * 
     * @param coreDump CoreDump to take the thread dump,
     * @param openView true if taken thread dump should be opened, false otherwise.
     */
    public void takeThreadDump(CoreDump coreDump, boolean openView) {
        threadDumpProvider.createThreadDump(coreDump, openView);
    }
    
    /**
     * Returns PluggableDataSourceViewProvider for thread dumps.
     * 
     * @return PluggableDataSourceViewProvider for thread dumps.
     */
    public PluggableDataSourceViewProvider<ThreadDump> getThreadDumpView() {
        return threadDumpViewProvider;
    }


    private ThreadDumpSupport() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new ThreadDumpDescriptorProvider());
        threadDumpProvider = new ThreadDumpProvider();
        threadDumpProvider.initialize();
        
        threadDumpViewProvider = new ThreadDumpViewProvider();
        
        RegisteredSnapshotCategories.sharedInstance().registerCategory(category);
        
        threadDumpViewProvider.initialize();
    }

}

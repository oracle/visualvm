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

package org.graalvm.visualvm.heapdump;

import org.graalvm.visualvm.heapdump.impl.HeapDumpDescriptorProvider;
import org.graalvm.visualvm.heapdump.impl.HeapDumpCategory;
import org.graalvm.visualvm.heapdump.impl.HeapDumpViewProvider;
import org.graalvm.visualvm.heapdump.impl.HeapDumpProvider;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;
import org.graalvm.visualvm.coredump.CoreDump;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;

/**
 * Support for heap dumps in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class HeapDumpSupport {

    private static HeapDumpSupport instance;

    private final SnapshotCategory category = new HeapDumpCategory();
    private final HeapDumpProvider heapDumpProvider;
    private final HeapDumpViewProvider heapDumpViewProvider;


    /**
     * Returns singleton instance of HeapDumpSupport.
     * 
     * @return singleton instance of HeapDumpSupport.
     */
    public static synchronized HeapDumpSupport getInstance() {
        if (instance == null) instance = new HeapDumpSupport();
        return instance;
    }
    
    
    /**
     * Returns SnapshotCategory instance for heap dumps.
     * 
     * @return SnapshotCategory instance for heap dumps.
     */
    public SnapshotCategory getCategory() {
        return category;
    }
    
    /**
     * Returns true if taking heap dumps is supported for the application, false otherwise.
     * 
     * @param application application from which to take the heap dump.
     * @return true if taking heap dumps is supported for the application, false otherwise.
     */
    public boolean supportsHeapDump(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        return JvmFactory.getJVMFor(application).isTakeHeapDumpSupported();
    }
    
    /**
     * Takes heap dump from Application.
     * 
     * @param application Application to take the heap dump,
     * @param openView true if taken heap dump should be opened, false otherwise.
     */
    public void takeHeapDump(Application application, boolean openView) {
        heapDumpProvider.createHeapDump(application, openView);
    }
    
    /**
     * Returns true if taking heap dumps is supported for the remote application.
     * 
     * @param application remote application from which to take the heap dump
     * @return true if taking heap dumps is supported for the remote application, false otherwise
     * 
     * @since VisualVM 1.3
     */
    public boolean supportsRemoteHeapDump(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        if (application.isLocalApplication()) return false; // Should be allowed???
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        return jmxModel == null ? false : jmxModel.isTakeHeapDumpSupported();
    }
    
    /**
     * Takes heap dump from remote Application.
     * 
     * @param application remote Application to take the heap dump
     * @param dumpFile target dump file on the remote machine
     * @param customizeDumpFile true if the dumpFile customization dialog should be displayed, false otherwise
     * 
     * @since VisualVM 1.3
     */
    public void takeRemoteHeapDump(Application application, String dumpFile,
                                   boolean customizeDumpFile) {
        heapDumpProvider.createRemoteHeapDump(application, dumpFile, customizeDumpFile);
    }
    
    /**
     * Takes heap dump from CoreDump.
     * 
     * @param coreDump CoreDump to take the heap dump,
     * @param openView true if taken heap dump should be opened, false otherwise.
     */
    public void takeHeapDump(CoreDump coreDump, boolean openView) {
        heapDumpProvider.createHeapDump(coreDump, openView);
    }
    
    
    /**
     * Returns PluggableDataSourceViewProvider for heap dumps.
     * 
     * @return PluggableDataSourceViewProvider for heap dumps.
     */
    public PluggableDataSourceViewProvider<HeapDump> getHeapDumpView() {
        return heapDumpViewProvider;
    }


    private HeapDumpSupport() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new HeapDumpDescriptorProvider());
        heapDumpProvider = new HeapDumpProvider();
        heapDumpProvider.initialize();
        
        heapDumpViewProvider = new HeapDumpViewProvider();
        RegisteredSnapshotCategories.sharedInstance().registerCategory(category);

        heapDumpViewProvider.initialize();
    }

}

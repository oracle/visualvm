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

package com.sun.tools.visualvm.core.heapdump;

import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.core.ui.PluggableViewSupport;

/**
 * A public entrypoint to the heap dump support in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class HeapDumpSupport {

    private static HeapDumpSupport instance;

    private final SnapshotCategory category = new HeapDumpCategory();
    private final HeapDumpProvider heapDumpProvider;
    private final HeapDumpViewProvider heapDumpViewProvider;
    private final HeapDumpPluggableView heapDumpPluggableView;


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
     * Returns PluggableView instance to be used to customize the heap dump view.
     * 
     * @return PluggableView instance to be used to customize the heap dump view.
     */
    public PluggableViewSupport getPluggableView() {
        return heapDumpPluggableView;
    }
    
    HeapDumpPluggableView getHeapDumpPluggableView() {
        return heapDumpPluggableView;
    }


    HeapDumpProvider getHeapDumpProvider() {
        return heapDumpProvider;
    }


    private HeapDumpSupport() {
        RegisteredSnapshotCategories.sharedInstance().addCategory(category);
        DataSourceDescriptorFactory.getDefault().registerFactory(new HeapDumpDescriptorProvider());
        
        heapDumpProvider = new HeapDumpProvider();
        heapDumpProvider.initialize();
        
        heapDumpPluggableView = new HeapDumpPluggableView();
        
        heapDumpViewProvider = new HeapDumpViewProvider();
        heapDumpViewProvider.initialize();

        new HeapDumpActionsProvider().initialize();
    }

}

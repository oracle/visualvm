/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.java.impl;

import com.sun.tools.visualvm.heapviewer.java.JavaHeapFragment;
import java.io.File;
import java.io.IOException;
import org.netbeans.lib.profiler.heap.Heap;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapFragment.Provider.class, position = 100)
public class JavaHeapFragmentProvider extends HeapFragment.Provider {
    
    public HeapFragment getFragment(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
        return heap.getJavaClassByName("java.lang.Object") == null ? null : new JavaHeapFragment(heap); // NOI18N
    }
    
}

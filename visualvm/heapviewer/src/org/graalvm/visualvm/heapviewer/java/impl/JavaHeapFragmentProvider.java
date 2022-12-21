/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapFactory;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapFragment.Provider.class, position = 100)
public class JavaHeapFragmentProvider extends HeapFragment.Provider {
    
    public List<HeapFragment> getFragments(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
        if (heap.getJavaClassByName("java.lang.Object") == null) return null; // NOI18N
        
        List<HeapFragment> fragments = new ArrayList();
        int segments = HeapFactory.getTotalNumberOfSegments(heap);
        
        if (segments == 1) {
            fragments.add(new JavaHeapFragment(heap));
        } else {
            fragments.add(new JavaHeapFragment(heap, 0));
            for (int segment = 1; segment < segments; segment++) {
                Heap segmentHeap = HeapFactory.createHeap(heapDumpFile, segment);
                fragments.add(new JavaHeapFragment(segmentHeap, segment));
            }
        }
        
        return fragments;
    }
    
}

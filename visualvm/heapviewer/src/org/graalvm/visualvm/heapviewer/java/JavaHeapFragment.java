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

package org.graalvm.visualvm.heapviewer.java;

import java.io.IOException;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaHeapFragment_Name=Java Heap",
    "JavaHeapFragment_Description=Java Heap",
    "JavaHeapFragment_NameSegment=Java Heap (Segment {0})",
    "JavaHeapFragment_DescriptionSegment=Java Heap (Segment {0})"
})
public class JavaHeapFragment extends HeapFragment {
    
    public JavaHeapFragment(Heap heap) throws IOException {
        super("java_heap", Bundle.JavaHeapFragment_Name(), Bundle.JavaHeapFragment_Description(), heap); // NOI18N
    }
    
    public JavaHeapFragment(Heap heap, int segment) throws IOException {
        super("java_heap", Bundle.JavaHeapFragment_NameSegment(segment), Bundle.JavaHeapFragment_DescriptionSegment(segment), heap); // NOI18N
    }
    
    public static boolean isJavaHeap(HeapContext context) {
        return "java_heap".equals(context.getFragment().getID()); // NOI18N
    }
    
    public static HeapContext getJavaContext(HeapContext context) {
        if (isJavaHeap(context)) return context;
        
        for (HeapContext otherContext : context.getOtherContexts())
            if (isJavaHeap(otherContext)) return otherContext;
        
        return null;
    }
    
}

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
package com.sun.tools.visualvm.heapviewer.truffle.r;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RHeapFragment extends TruffleLanguageHeapFragment<RObject, RType> {
    
    private static final String R_HEAP_ID = "r_heap";
    
    
    RHeapFragment(Instance langID, Heap heap) throws IOException {
        super(R_HEAP_ID, "R Heap", fragmentDescription(langID, heap), heap);
    }
    
    
    static RHeapFragment fromContext(HeapContext context) {
        return (RHeapFragment)context.getFragment();
    }
    
    
    @Override
    public Iterator<Instance> getInstancesIterator() {
        String[] topClasses = new String[] { RObject.R_OBJECT_FQN, RObject.R_SCALAR_FQN, RObject.R_WRAPPER_FQN };
        return instancesIterator(topClasses);
    }
    
    @Override
    public Iterator<RObject> getObjectsIterator() {
        return super.getObjectsIterator();
    }
    
    
    @Override
    protected RObject createObject(Instance instance) {
        return new RObject(instance);
    }

    @Override
    protected RType createTruffleType(String name) {
        return new RType(name);
    }
    
    
    static boolean isRHeap(HeapContext context) {
        return R_HEAP_ID.equals(context.getFragment().getID()); // NOI18N
    }
    
    public static HeapContext getRContext(HeapContext context) {
        if (isRHeap(context)) return context;
        
        for (HeapContext otherContext : context.getOtherContexts())
            if (isRHeap(otherContext)) return otherContext;
        
        return null;
    }
    
    
    @ServiceProvider(service=HeapFragment.Provider.class, position = 400)
    public static class Provider extends HeapFragment.Provider {

        private static final String R_LANGINFO_ID = "R";  // NOI18N

        public HeapFragment getFragment(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
            Instance langID = TruffleLanguageSupport.getLanguageInfo(heap, R_LANGINFO_ID);
            JavaClass rMainClass = heap.getJavaClassByName(RObject.R_OBJECT_FQN);

            return langID != null && rMainClass != null ? new RHeapFragment(langID, heap) : null;
        }

    }
    
}

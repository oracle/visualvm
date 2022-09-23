/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.r;

import java.util.Iterator;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RHeapFragment_Name=R Heap"
})
class RHeapFragment extends TruffleLanguageHeapFragment<RObject, RType> {
    
    private static final String R_HEAP_ID = "r_heap"; // NOI18N
    
    
    RHeapFragment(RLanguage language, Instance langID, Heap heap) {
        super(R_HEAP_ID, Bundle.RHeapFragment_Name(), fragmentDescription(langID, heap), language, heap);
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
    
    
    static boolean isRHeap(HeapContext context) {
        return R_HEAP_ID.equals(context.getFragment().getID());
    }
    
    public static HeapContext getRContext(HeapContext context) {
        if (isRHeap(context)) return context;
        
        for (HeapContext otherContext : context.getOtherContexts())
            if (isRHeap(otherContext)) return otherContext;
        
        return null;
    }
    
}

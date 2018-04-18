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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;

/**
 *
 * @author Tomas Hurka
 */
public class PythonHeapFragment extends TruffleLanguageHeapFragment<PythonObject, PythonType> {

    private static final String PYTHON_HEAP_ID = "python_heap";


    PythonHeapFragment(PythonLanguage language, Instance langID, Heap heap) {
        super(PYTHON_HEAP_ID, "Python Heap", fragmentDescription(langID, heap), language, heap);
    }
    
    
    static PythonHeapFragment fromContext(HeapContext context) {
        return (PythonHeapFragment)context.getFragment();
    }
    
    static boolean isPythonHeap(HeapContext context) {
        return PYTHON_HEAP_ID.equals(context.getFragment().getID()); // NOI18N
    }
    
    
    @Override
    public Iterator<Instance> getInstancesIterator() {
        return instancesIterator(PythonObject.PYTHON_OBJECT_FQN);
    }
    
    @Override
    public Iterator<PythonObject> getObjectsIterator() {
        return super.getObjectsIterator();
    }
    

//    public static HeapContext getPythonContext(HeapContext context) {
//        if (isPythonHeap(context)) return context;
//
//        for (HeapContext otherContext : context.getOtherContexts())
//            if (isPythonHeap(otherContext)) return otherContext;
//
//        return null;
//    }

}

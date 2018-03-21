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
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSupport;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
public class PythonHeapFragment extends TruffleLanguageHeapFragment<PythonObject, PythonType> {

    private static final String PYTHON_HEAP_ID = "python_heap";


    PythonHeapFragment(Instance langID, Heap heap) throws IOException {
        super(PYTHON_HEAP_ID, "Python Heap", fragmentDescription(langID, heap), heap);
    }
    
    
    @Override
    protected PythonObject createObject(Instance instance) {
        return new PythonObject(instance);
    }

    @Override
    protected PythonType createTruffleType(String name) {
        return new PythonType(name);
    }

    
    @Override
    protected Iterator<Instance> getInstancesIterator() {
        return instancesIterator(PythonObject.PYTHON_OBJECT_FQN);
    }
    
    @Override
    protected Iterator<PythonObject> getObjectsIterator() {
        return super.getObjectsIterator();
    }


    static boolean isPythonHeap(HeapContext context) {
        return PYTHON_HEAP_ID.equals(context.getFragment().getID()); // NOI18N
    }

    public static HeapContext getPythonContext(HeapContext context) {
        if (isPythonHeap(context)) return context;

        for (HeapContext otherContext : context.getOtherContexts())
            if (isPythonHeap(otherContext)) return otherContext;

        return null;
    }


    @ServiceProvider(service=HeapFragment.Provider.class, position = 400)
    public static class Provider extends HeapFragment.Provider {

        private static final String PYTHON_LANGINFO_ID_OLD = "python";  // NOI18N
        private static final String PYTHON_LANGINFO_ID = "Python";  // NOI18N

        public HeapFragment getFragment(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
            Instance langID = TruffleLanguageSupport.getLanguageInfo(heap, PYTHON_LANGINFO_ID);
            JavaClass pythonMainClass = heap.getJavaClassByName(PythonObject.PYTHON_OBJECT_FQN);

            if (langID == null) {
                langID = TruffleLanguageSupport.getLanguageInfo(heap, PYTHON_LANGINFO_ID_OLD);
            }

            return langID != null && pythonMainClass != null ? new PythonHeapFragment(langID, heap) : null;
        }

    }

}

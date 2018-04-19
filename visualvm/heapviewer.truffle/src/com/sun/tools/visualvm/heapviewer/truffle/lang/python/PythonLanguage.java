/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle.lang.python;

import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguage;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProviders(value={
    @ServiceProvider(service=HeapFragment.Provider.class, position = 300),
    @ServiceProvider(service=PythonLanguage.class, position = 300)}
)
public class PythonLanguage extends TruffleLanguage<PythonObject, PythonType, PythonHeapFragment> {
    
    private static final String ID = "python"; // NOI18N
    
    private static final String PYTHON_LANGINFO_ID_OLD = "python";  // NOI18N
    private static final String PYTHON_LANGINFO_ID = "Python";  // NOI18N
        
    
    static PythonLanguage instance() {
        return Lookup.getDefault().lookup(PythonLanguage.class);
    }
    
    
    public String getID() { return ID; }
    
    
    @Override
    protected PythonHeapFragment createFragment(Heap heap) {
        Instance langID = getLanguageInfo(heap, PYTHON_LANGINFO_ID);
        if (langID == null) langID = getLanguageInfo(heap, PYTHON_LANGINFO_ID_OLD);
        if (langID == null) return null;
        
        JavaClass pythonMainClass = heap.getJavaClassByName(PythonObject.PYTHON_OBJECT_FQN);
        if (pythonMainClass == null) return null;

        return new PythonHeapFragment(this, langID, heap);
    }
    

    @Override
    public boolean isLanguageObject(Instance instance) {
        return PythonObject.isPythonObject(instance);
    }
    
    @Override
    public PythonObject createObject(Instance instance) {
        return new PythonObject(instance);
    }
    
    @Override
    public PythonType createType(String name) {
        return new PythonType(name);
    }
    
    
    @Override
    public PythonNodes.PythonObjectNode createObjectNode(PythonObject object, String type) {
        return new PythonNodes.PythonObjectNode(object, type);
    }
    
    @Override
    public PythonNodes.PythonLocalObjectNode createLocalObjectNode(PythonObject object, String type) {
        return new PythonNodes.PythonLocalObjectNode(object, type);
    }
    
    @Override
    public PythonNodes.PythonTypeNode createTypeNode(PythonType type, Heap heap) {
        return new PythonNodes.PythonTypeNode(type);
    }
    
}

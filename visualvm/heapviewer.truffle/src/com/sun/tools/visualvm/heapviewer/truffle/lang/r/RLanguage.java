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
package com.sun.tools.visualvm.heapviewer.truffle.lang.r;

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
    @ServiceProvider(service=HeapFragment.Provider.class, position = 400),
    @ServiceProvider(service=RLanguage.class, position = 400)}
)
public class RLanguage extends TruffleLanguage<RObject, RType, RHeapFragment> {
    
    private static final String ID = "r"; // NOI18N
    
    private static final String R_LANGINFO_ID = "R";  // NOI18N
        
    
    static RLanguage instance() {
        return Lookup.getDefault().lookup(RLanguage.class);
    }
    
    
    public String getID() { return ID; }
    
    
    @Override
    protected RHeapFragment createFragment(Heap heap) {
        Instance langID = getLanguageInfo(heap, R_LANGINFO_ID);
        if (langID == null) return null;
        
        JavaClass rMainClass = heap.getJavaClassByName(RObject.R_OBJECT_FQN);
        if (rMainClass == null) return null;

        return new RHeapFragment(this, langID, heap);
    }
    

    @Override
    public boolean isLanguageObject(Instance instance) {
        return RObject.isRObject(instance);
    }
    
    @Override
    public RObject createObject(Instance instance) {
        return new RObject(instance);
    }
    
    @Override
    public RType createType(String name) {
        return new RType(name);
    }
    
    
    @Override
    public RNodes.RObjectNode createObjectNode(RObject object, String type) {
        return new RNodes.RObjectNode(object, type);
    }
    
    @Override
    public RNodes.RLocalObjectNode createLocalObjectNode(RObject object, String type) {
        return new RNodes.RLocalObjectNode(object, type);
    }
    
    @Override
    public RNodes.RTypeNode createTypeNode(RType type, Heap heap) {
        return new RNodes.RTypeNode(type);
    }
    
}

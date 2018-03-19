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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import java.io.File;
import java.io.IOException;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSupport;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptHeapFragment extends TruffleLanguageHeapFragment.DynamicObjectBased<JavaScriptDynamicObject, JavaScriptType> {
    
    static final String JS_LANG_ID = "com.oracle.truffle.js.runtime.builtins.JSClass";
    
    private static final String JS_HEAP_ID = "javascript_heap";
    
    
    private final Map<Instance, String> typesCache;
    
    
    JavaScriptHeapFragment(Instance langID, Heap heap) throws IOException {
        super(JS_HEAP_ID, "JavaScript Heap", fragmentDescription(langID, heap) , heap);
        typesCache = new HashMap();
    }
    
    
    @Override
    protected JavaScriptDynamicObject createObject(Instance instance) {
        return new JavaScriptDynamicObject(instance);
    }
    
    @Override
    protected JavaScriptType createTruffleType(String name) {
        return new JavaScriptType(name);
    }    
    
    
    @Override
    protected Iterator<Instance> getInstancesIterator() {
        return languageInstancesIterator(JS_LANG_ID);
    }

    @Override
    protected Iterator<JavaScriptDynamicObject> getObjectsIterator() {
        return languageObjectsIterator(JS_LANG_ID);
    }
    

    @Override
    protected long getObjectSize(JavaScriptDynamicObject object) {
        return object.getInstance().getSize();
    }
    
    @Override
    protected long getObjectRetainedSize(JavaScriptDynamicObject object) {
        return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
               object.getInstance().getRetainedSize() :
               DataType.RETAINED_SIZE.getNotAvailableValue();
    }

    @Override
    protected String getObjectType(JavaScriptDynamicObject object) {
        Instance instance = object.getInstance();
        Instance prototype = JavaScriptDynamicObject.getPrototype(instance);
        
//        return JavaScriptDynamicObject.getJSTypeOrig(instance, heap);
        
        String type = typesCache.get(prototype);
        if (type == null) {
            type = JavaScriptDynamicObject.getJSType(instance, prototype, heap);
            typesCache.put(prototype, type);
        }
        
        return type;
    }
    
    
    static boolean isJavaScriptHeap(HeapContext context) {
        return JS_HEAP_ID.equals(context.getFragment().getID()); // NOI18N
    }
    
    public static HeapContext getJavaScriptContext(HeapContext context) {
        if (isJavaScriptHeap(context)) return context;
        
        for (HeapContext otherContext : context.getOtherContexts())
            if (isJavaScriptHeap(otherContext)) return otherContext;
        
        return null;
    }
    
    
    @ServiceProvider(service=HeapFragment.Provider.class, position = 200)
    public static class Provider extends HeapFragment.Provider {
        private static final String JS_LANGINFO_ID = "JS";  // NOI18N
        private static final String JAVASCRIPT_LANGINFO_ID = "JavaScript";  // NOI18N

        public HeapFragment getFragment(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
            Instance langID = TruffleLanguageSupport.getLanguageInfo(heap, JS_LANGINFO_ID);

            if (langID == null) {
                langID = TruffleLanguageSupport.getLanguageInfo(heap, JAVASCRIPT_LANGINFO_ID);
            }
            JavaClass JSMainClass = heap.getJavaClassByName(JS_LANG_ID);

            return langID != null && JSMainClass != null ? new JavaScriptHeapFragment(langID, heap) : null;
        }
    }
    
}

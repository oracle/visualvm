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
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptHeapFragment extends TruffleLanguageHeapFragment.DynamicObjectBased<JavaScriptDynamicObject, JavaScriptType> {
    
    static final String JS_LANG_ID = "com.oracle.truffle.js.runtime.builtins.JSClass";
    
    private static final String JS_HEAP_ID = "javascript_heap";
    
    // Copied from DynamicObjectDetailsProvider, unify!
    private static final String JS_NULL_CLASS_FQN = "com.oracle.truffle.js.runtime.objects.Null";     // NOI18N
    private static final String JS_UNDEFIED_CLASS_FQN = "com.oracle.truffle.js.runtime.objects.Undefined";     // NOI18N
    
    private static final Map<Heap, Reference<JavaScriptHeapFragment>> FRAGMENTS = Collections.synchronizedMap(new WeakHashMap());
    
    final Instance nullInstance;
    final Instance undefinedInstance;
    
    private final Map<Instance, String> typesCache;
    
    
    JavaScriptHeapFragment(Instance langID, Heap heap) throws IOException {
        super(JS_HEAP_ID, "JavaScript Heap", fragmentDescription(langID, heap) , heap);
        
        JavaClass nullClass = heap.getJavaClassByName(JS_NULL_CLASS_FQN);
        nullInstance = (Instance)nullClass.getValueOfStaticField("instance"); // NOI18N
        
        JavaClass undefinedClass = heap.getJavaClassByName(JS_UNDEFIED_CLASS_FQN);
        undefinedInstance = (Instance)undefinedClass.getValueOfStaticField("instance"); // NOI18N
        
        typesCache = new HashMap();
        
        FRAGMENTS.put(heap, new WeakReference(this));
    }
    
    
    static JavaScriptHeapFragment fromContext(HeapContext context) {
        return (JavaScriptHeapFragment)context.getFragment();
    }
    
    static JavaScriptHeapFragment fromHeap(Heap heap) {
        Reference<JavaScriptHeapFragment> fragmentRef = FRAGMENTS.get(heap);
        return fragmentRef == null ? null : fragmentRef.get();
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
        return new ExcludingInstancesIterator(languageInstancesIterator(JS_LANG_ID)) {
            @Override
            protected boolean exclude(Instance instance) {
                return (Objects.equals(nullInstance, instance) || Objects.equals(undefinedInstance, instance));
            }
        };
//        return languageInstancesIterator(JS_LANG_ID);
    }

    @Override
    protected Iterator<JavaScriptDynamicObject> getObjectsIterator() {
        return new ExcludingObjectsIterator(languageObjectsIterator(JS_LANG_ID)) {
            @Override
            protected boolean exclude(JavaScriptDynamicObject object) {
                Instance instance = object.getInstance();
                return (Objects.equals(nullInstance, instance) || Objects.equals(undefinedInstance, instance));
            }
        };
//        return super.getObjectsIterator();
//        return languageObjectsIterator(JS_LANG_ID);
    }
    

    String getObjectType(Instance instance) {
        return getObjectType(instance, null);
    }
    
    String getObjectType(Instance instance, Instance shape) {
        if (shape == null) shape = JavaScriptDynamicObject.getShape(instance);
        String type = typesCache.get(shape);

        if (type == null) {
            Instance prototype = getPrototype(instance);

            type = typesCache.get(prototype);
            if (type == null) {
                type = getJSType(prototype, this);
                typesCache.put(prototype, type);
            }
            typesCache.put(shape, type);
        }
        
        return type;
    }
    
    private static Instance getPrototype(Instance instance) {
        DynamicObject dobj = new DynamicObject(instance);
        List<FieldValue> staticFields = dobj.getStaticFieldValues();
        for (FieldValue staticField : staticFields) {
            if ("__proto__ (hidden)".equals(staticField.getField().getName())) { // NOI18N
                return ((ObjectFieldValue)staticField).getInstance();
            }
        }
        
        FieldValue field = dobj.getFieldValue("__proto__ (hidden)"); // NOI18N
        if (field != null) return ((ObjectFieldValue)field).getInstance();
        
        return null;
    }
    
    private static String getJSType(Instance prototype, JavaScriptHeapFragment fragment) {
        if (prototype == null) return "<unknown type>";
        
        if (Objects.equals(fragment.nullInstance, prototype)) return "<no prototype>";
        
        Heap heap = fragment.getHeap();
        
        DynamicObject dprototype = new DynamicObject(prototype);
        ObjectFieldValue constructorValue = (ObjectFieldValue)dprototype.getFieldValue("constructor"); // NOI18N
        if (constructorValue != null) {
            Instance constructor = constructorValue.getInstance();
            DynamicObject dconstructor = new DynamicObject(constructor);
            String type = JavaScriptNodes.getLogicalValue(dconstructor, dconstructor.getType(heap), heap);
            if (type == null) return "<anonymous prototype>";
            return type.endsWith("()") ? type.substring(0, type.length() - 2) : type; // NOI18N
        } else {
            return "<unknown prototype>";
        }
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

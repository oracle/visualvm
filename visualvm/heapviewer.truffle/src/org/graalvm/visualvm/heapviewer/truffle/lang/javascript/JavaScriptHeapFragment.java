/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.javascript;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectLanguageHeapFragment;
import org.graalvm.visualvm.heapviewer.utils.ExcludingIterator;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaScriptHeapFragment_Name=JavaScript Heap",
    "JavaScriptHeapFragment_UnknownType=<unknown type>",
    "JavaScriptHeapFragment_NoPrototype=<no prototype>",
    "JavaScriptHeapFragment_AnonymousPrototype=<anonymous prototype>",
    "JavaScriptHeapFragment_UnknownPrototype=<unknown prototype>"
})
class JavaScriptHeapFragment extends DynamicObjectLanguageHeapFragment<JavaScriptObject, JavaScriptType> {
    
    static final String JS_LANG_ID = "com.oracle.truffle.js.runtime.builtins.JSClass"; // NOI18N
    
    private static final String JS_HEAP_ID = "javascript_heap"; // NOI18N
    
    // Copied from DynamicObjectDetailsProvider, unify!
    private static final String JS_NULL_CLASS_FQN = "com.oracle.truffle.js.runtime.objects.Null";     // NOI18N
    private static final String JS_UNDEFIED_CLASS_FQN = "com.oracle.truffle.js.runtime.objects.Undefined";     // NOI18N
    
    
    final Instance nullInstance;
    final Instance undefinedInstance;
    
    private final Map<Instance, String> typesCache;
    
    
    JavaScriptHeapFragment(JavaScriptLanguage language, Instance langID, Heap heap) {
        super(JS_HEAP_ID, Bundle.JavaScriptHeapFragment_Name(), fragmentDescription(langID), language, heap);
        
        JavaClass nullClass = heap.getJavaClassByName(JS_NULL_CLASS_FQN);
        nullInstance = (Instance)nullClass.getValueOfStaticField("instance"); // NOI18N
        
        JavaClass undefinedClass = heap.getJavaClassByName(JS_UNDEFIED_CLASS_FQN);
        undefinedInstance = (Instance)undefinedClass.getValueOfStaticField("instance"); // NOI18N
        
        typesCache = new HashMap<>();
    }
    
    
    static JavaScriptHeapFragment fromContext(HeapContext context) {
        return (JavaScriptHeapFragment)context.getFragment();
    }
    
    static boolean isJavaScriptHeap(HeapContext context) {
        return JS_HEAP_ID.equals(context.getFragment().getID());
    }
    
    
    @Override
    public Iterator<Instance> getInstancesIterator() {
        return new ExcludingIterator<Instance>(languageInstancesIterator(JS_LANG_ID)) {
            @Override
            protected boolean exclude(Instance instance) {
                return (Objects.equals(nullInstance, instance) || Objects.equals(undefinedInstance, instance));
            }
        };
//        return languageInstancesIterator(JS_LANG_ID);
    }

    @Override
    public Iterator<JavaScriptObject> getObjectsIterator() {
        return new ExcludingIterator<JavaScriptObject>(languageObjectsIterator(JS_LANG_ID)) {
            @Override
            protected boolean exclude(JavaScriptObject object) {
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
        if (shape == null) shape = JavaScriptObject.getShape(instance);
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
        JavaScriptObject jsobj = new JavaScriptObject(instance);
        Instance prototype = getPrototype(jsobj.getStaticFieldValues());

        if (prototype != null) {
            return prototype;
        }
        return getPrototype(jsobj.getFieldValues());
    }

    private static Instance getPrototype(List<FieldValue> fields) {
        for (FieldValue field : fields) {
            String fieldName = field.getField().getName();
            if ("__proto__ (hidden)".equals(fieldName) ||     // NOI18N
                "[[Prototype]] (hidden)".equals(fieldName)) { // NOI18N
                return ((ObjectFieldValue)field).getInstance();
            }
        }
        return null;
    }

    private static String getJSType(Instance prototype, JavaScriptHeapFragment fragment) {
        if (prototype == null) return Bundle.JavaScriptHeapFragment_UnknownType();
        
        if (Objects.equals(fragment.nullInstance, prototype)) return Bundle.JavaScriptHeapFragment_NoPrototype();
        
        Heap heap = fragment.getHeap();
        
        JavaScriptObject dprototype = new JavaScriptObject(prototype);
        ObjectFieldValue constructorValue = (ObjectFieldValue)dprototype.getFieldValue("constructor"); // NOI18N
        if (constructorValue != null) {
            Instance constructor = constructorValue.getInstance();
            JavaScriptObject dconstructor = new JavaScriptObject(constructor);
            String dconstructorT = DynamicObject.getType(constructor);
            String type = JavaScriptNodes.getLogicalValue(dconstructor, dconstructorT);
            if (type == null) return Bundle.JavaScriptHeapFragment_AnonymousPrototype();
            return type.endsWith("()") ? type.substring(0, type.length() - 2) : type; // NOI18N
        } else {
            return Bundle.JavaScriptHeapFragment_UnknownPrototype();
        }
    }
    
    
//    public static HeapContext getJavaScriptContext(HeapContext context) {
//        if (isJavaScriptHeap(context)) return context;
//        
//        for (HeapContext otherContext : context.getOtherContexts())
//            if (isJavaScriptHeap(otherContext)) return otherContext;
//        
//        return null;
//    }
    
}

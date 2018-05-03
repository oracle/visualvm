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
package com.sun.tools.visualvm.heapviewer.truffle.lang.javascript;

import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleInstancePropertyProvider;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaScriptObjectProperties_Properties=properties",
    "JavaScriptObjectProperties_Items=items",
    "JavaScriptObjectProperties_References=references"
})
final class JavaScriptObjectProperties {
    
    // -------------------------------------------------------------------------
    // --- Fields --------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 200),
        @ServiceProvider(service=FieldsProvider.class, position = 200)}
    )
    public static class FieldsProvider extends TruffleObjectPropertyProvider.Fields<JavaScriptObject, JavaScriptType, JavaScriptHeapFragment, JavaScriptLanguage> {

        public FieldsProvider() {
            super(Bundle.JavaScriptObjectProperties_Properties(), JavaScriptLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("javascript_"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            return node instanceof JavaScriptNodes.JavaScriptNode && !(node instanceof JavaScriptNodes.JavaScriptObjectReferenceNode);
        }

        @Override
        protected HeapViewerNode createObjectFieldNode(JavaScriptObject object, String type, FieldValue field) {
            return new JavaScriptNodes.JavaScriptObjectFieldNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(JavaScriptObject object, Heap heap) {
            List<FieldValue> fields = new ArrayList();

            fields.addAll(object.getFieldValues());
            fields.addAll(object.getStaticFieldValues());

            return fields;
        }

        @Override
        protected boolean includeInstance(Instance instance) {
            String className = instance.getJavaClass().getName();

            if (className.startsWith("java.lang.") || // NOI18N
                className.startsWith("com.oracle.truffle.js.runtime.objects.") || // NOI18N
                className.startsWith("com.oracle.truffle.api.object.DynamicObject[]")) // NOI18N
                return true;

            return false;
        }

    }
    
    
    // -------------------------------------------------------------------------
    // --- Items ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 300)
    public static class ArrayItemsProvider extends TruffleInstancePropertyProvider.ArrayItems<JavaScriptObject, JavaScriptType, JavaScriptHeapFragment, JavaScriptLanguage> {

        public ArrayItemsProvider() {
            super(Bundle.JavaScriptObjectProperties_Items(), JavaScriptLanguage.instance(), false);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("javascript_"); // NOI18N
        }


        @Override
        protected HeapViewerNode createObjectArrayItemNode(JavaScriptObject object, String type, ArrayItemValue item) {
            return new JavaScriptNodes.JavaScriptObjectArrayItemNode(object, type, item);
        }

    }
    
    
    // -------------------------------------------------------------------------
    // --- References ----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 400),
        @ServiceProvider(service=ReferencesProvider.class, position = 400)}
    )
    public static class ReferencesProvider extends TruffleObjectPropertyProvider.References<JavaScriptObject, JavaScriptType, JavaScriptHeapFragment, JavaScriptLanguage> {

        public ReferencesProvider() {
            super(Bundle.JavaScriptObjectProperties_References(), JavaScriptLanguage.instance(), false);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("javascript_"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            return node instanceof JavaScriptNodes.JavaScriptNode && !(node instanceof JavaScriptNodes.JavaScriptObjectFieldNode || node instanceof JavaScriptNodes.JavaScriptObjectArrayItemNode);
        }

        @Override
        protected HeapViewerNode createObjectReferenceNode(JavaScriptObject object, String type, FieldValue field) {
            return new JavaScriptNodes.JavaScriptObjectReferenceNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(JavaScriptObject object, Heap heap) {
            return object.getReferences();
        }

    }
    
}

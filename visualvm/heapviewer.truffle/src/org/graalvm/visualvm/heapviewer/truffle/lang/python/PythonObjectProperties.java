/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.python;

import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import java.util.Collection;
import java.util.List;
import org.graalvm.visualvm.heapviewer.utils.HeapOperations;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "PythonObjectProperties_Properties=attributes",
    "PythonObjectProperties_Items=items",
    "PythonObjectProperties_References=references"
})
final class PythonObjectProperties {
    
    // -------------------------------------------------------------------------
    // --- Attributes ----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 200),
        @ServiceProvider(service=AttributesProvider.class, position = 200)}
    )
    public static class AttributesProvider extends TruffleObjectPropertyProvider.Fields<PythonObject, PythonType, PythonHeapFragment, PythonLanguage> {

        public AttributesProvider() {
            super(Bundle.PythonObjectProperties_Properties(), PythonLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("python_") && !viewID.endsWith("_references"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            if (node instanceof PythonNodes.PythonNode && !(node instanceof PythonNodes.PythonObjectReferenceNode || node instanceof PythonNodes.PythonObjectAttributeReferenceNode)) {
                TruffleObject object = HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
                PythonObject pyobject = object instanceof PythonObject ? (PythonObject)object : null;
                if (pyobject != null) return !getPropertyItems(pyobject, heap).isEmpty();
            }
            return false;
        }

        @Override
        protected HeapViewerNode createObjectFieldNode(PythonObject object, String type, FieldValue field) {
            return new PythonNodes.PythonObjectFieldNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(PythonObject object, Heap heap) {
            return object.getAttributes();
        }

        @Override
        protected boolean includeInstance(Instance instance) {
            String className = instance.getJavaClass().getName();

            if (className.startsWith("java.lang.") || // NOI18N
                className.startsWith("java.math.") || // NOI18N
                className.startsWith("com.oracle.graal.python.runtime.datatype.") || // NOI18N
                className.startsWith("com.oracle.truffle.api.strings."))             // NOI18N
                return true;

            return false;
        }

    }
    
    
    // -------------------------------------------------------------------------
    // --- Items ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 300),
        @ServiceProvider(service=ItemsProvider.class, position = 300)}
    )
    public static class ItemsProvider extends TruffleObjectPropertyProvider.Fields<PythonObject, PythonType, PythonHeapFragment, PythonLanguage> {

        public ItemsProvider() {
            super(Bundle.PythonObjectProperties_Items(), PythonLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("python_") && !viewID.endsWith("_references"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            if (node instanceof PythonNodes.PythonNode && !(node instanceof PythonNodes.PythonObjectReferenceNode || node instanceof PythonNodes.PythonObjectAttributeReferenceNode)) {
                TruffleObject object = HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
                PythonObject pyobject = object instanceof PythonObject ? (PythonObject)object : null;
                if (pyobject != null) return !getPropertyItems(pyobject, heap).isEmpty();
            }
            return false;
        }

        @Override
        protected HeapViewerNode createObjectFieldNode(PythonObject object, String type, FieldValue field) {
            return new PythonNodes.PythonObjectFieldNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(PythonObject object, Heap heap) {
            return object.getItems();
        }

        @Override
        protected boolean includeInstance(Instance instance) {
            String className = instance.getJavaClass().getName();

            if (className.startsWith("java.lang.") || // NOI18N
                className.startsWith("java.math.") || // NOI18N
                className.startsWith("com.oracle.graal.python.runtime.datatype.") || // NOI18N
                className.startsWith("com.oracle.truffle.api.strings."))             // NOI18N
                return true;

            return false;
        }
        
        @Override
        protected String getMergedPropertiesKey() {
            return null;
        }

    }
    
    
    // -------------------------------------------------------------------------
    // --- References ----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 400),
        @ServiceProvider(service=ReferencesProvider.class, position = 400)}
    )
    public static class ReferencesProvider extends TruffleObjectPropertyProvider.References<PythonObject, PythonType, PythonHeapFragment, PythonLanguage> {

        public ReferencesProvider() {
            super(Bundle.PythonObjectProperties_References(), PythonLanguage.instance(), false);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("python_") && !viewID.endsWith("_attributes"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            return node instanceof PythonNodes.PythonNode && !(node instanceof PythonNodes.PythonObjectFieldNode);
        }

        @Override
        protected HeapViewerNode createObjectReferenceNode(PythonObject object, String type, FieldValue field) {
            return new PythonNodes.PythonObjectReferenceNode(object, type, field);
        }

        @Override
        protected HeapViewerNode createForeignReferenceNode(Instance instance, FieldValue field, Heap heap) {
            List<Value> references = instance.getReferences();
            for (Value reference : references) {
                instance = reference.getDefiningInstance();
                if (getLanguage().isLanguageObject(instance)) {
                    PythonObject robj = getLanguage().createObject(instance);
                    return new PythonNodes.PythonObjectAttributeReferenceNode(robj, robj.getType(), field);
                }
            }

            return super.createForeignReferenceNode(instance, field, heap);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(PythonObject object, Heap heap) throws InterruptedException {
            HeapOperations.initializeReferences(heap);
            return object.getReferences();
        }

    }
    
}

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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import java.util.Collection;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
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
            super("attributes", PythonObject.class, PythonLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("python_");
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

            if (className.startsWith("java.lang.") ||
                className.startsWith("com.oracle.graal.python.runtime.datatype."))
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
            super("items", PythonObject.class, PythonLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("python_");
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

            if (className.startsWith("java.lang.") ||
                className.startsWith("com.oracle.graal.python.runtime.datatype."))
                return true;

            return false;
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
            super("references", PythonObject.class, PythonLanguage.instance(), false);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("python_");
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
            List<Value> references = (List<Value>)instance.getReferences();
            for (Value reference : references) {
                instance = reference.getDefiningInstance();
                if (getLanguage().isLanguageObject(instance)) {
                    PythonObject robj = getLanguage().createObject(instance);
                    return new PythonNodes.PythonObjectAttributeReferenceNode(robj, robj.getType(heap), field);
                }
            }

            return super.createForeignReferenceNode(instance, field, heap);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(PythonObject object, Heap heap) {
            return object.getReferences();
        }

    }
    
}

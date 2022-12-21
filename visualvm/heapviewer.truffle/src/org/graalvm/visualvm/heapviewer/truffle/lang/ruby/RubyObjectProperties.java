/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.ruby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleInstancePropertyProvider;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import org.graalvm.visualvm.heapviewer.utils.HeapOperations;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RubyObjectProperties_Properties=variables",
    "RubyObjectProperties_Items=items",
    "RubyObjectProperties_References=references"
})
final class RubyObjectProperties {
    
    // -------------------------------------------------------------------------
    // --- Fields --------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 200),
        @ServiceProvider(service=FieldsProvider.class, position = 200)}
    )
    public static class FieldsProvider extends TruffleObjectPropertyProvider.Fields<RubyObject, RubyType, RubyHeapFragment, RubyLanguage> {

        public FieldsProvider() {
            super(Bundle.RubyObjectProperties_Properties(), RubyLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("ruby_") && !viewID.endsWith("_references"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            return node instanceof RubyNodes.RubyNode && !(node instanceof RubyNodes.RubyObjectReferenceNode);
        }

        @Override
        protected HeapViewerNode createObjectFieldNode(RubyObject object, String type, FieldValue field) {
            return new RubyNodes.RubyObjectFieldNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(RubyObject object, Heap heap) {
            List<FieldValue> fields = new ArrayList<>();

            fields.addAll(object.getFieldValues());
            fields.addAll(object.getStaticFieldValues());

            return fields;
        }

        @Override
        protected boolean includeInstance(Instance instance) {
            String className = instance.getJavaClass().getName();

            if (className.startsWith("java.lang.") || // NOI18N
                className.startsWith("java.math.") || // NOI18N
                className.startsWith("org.truffleruby.core.rope.") || // NOI18N
                className.startsWith("com.oracle.truffle.api.strings."))    // NOI18N
                return true;

            return false;
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- Items ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 300)
    public static class ArrayItemsProvider extends TruffleInstancePropertyProvider.ArrayItems<RubyObject, RubyType, RubyHeapFragment, RubyLanguage> {

        public ArrayItemsProvider() {
            super(Bundle.RubyObjectProperties_Items(), RubyLanguage.instance(), false);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("ruby_"); // NOI18N
        }


        @Override
        protected HeapViewerNode createObjectArrayItemNode(RubyObject object, String type, ArrayItemValue item) {
            return new RubyNodes.RubyObjectArrayItemNode(object, type, item);
        }

    }
    
     // -------------------------------------------------------------------------
    // --- Items ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProviders(value={
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 250),
        @ServiceProvider(service=ItemsProvider.class, position = 250)}
    )
    public static class ItemsProvider extends TruffleObjectPropertyProvider.Fields<RubyObject, RubyType, RubyHeapFragment, RubyLanguage> {

        public ItemsProvider() {
            super(Bundle.RubyObjectProperties_Items(), RubyLanguage.instance(), true);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("ruby_") && !viewID.endsWith("_references"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            if (node instanceof RubyNodes.RubyNode && !(node instanceof RubyNodes.RubyObjectReferenceNode)) {
                TruffleObject object = HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
                RubyObject robject = object instanceof RubyObject ? (RubyObject)object : null;
                if (robject != null) return !getPropertyItems(robject, heap).isEmpty();
            }
            return false;
        }

        @Override
        protected HeapViewerNode createObjectFieldNode(RubyObject object, String type, FieldValue field) {
            return new RubyNodes.RubyObjectFieldNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(RubyObject object, Heap heap) {
            return object.getItems();
        }

        @Override
        protected boolean includeInstance(Instance instance) {
            String className = instance.getJavaClass().getName();

            if (className.startsWith("java.lang.") || // NOI18N
                className.startsWith("java.math.") || // NOI18N
                className.startsWith("org.truffleruby.core.rope.") || // NOI18N
                className.startsWith("com.oracle.truffle.api.strings."))    // NOI18N
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
        @ServiceProvider(service=HeapViewerNode.Provider.class, position = 300),
        @ServiceProvider(service=ReferencesProvider.class, position = 300)}
    )
    public static class ReferencesProvider extends TruffleObjectPropertyProvider.References<RubyObject, RubyType, RubyHeapFragment, RubyLanguage> {

        public ReferencesProvider() {
            super(Bundle.RubyObjectProperties_References(), RubyLanguage.instance(), false);
        }


        @Override
        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("ruby_") && !viewID.endsWith("_fields"); // NOI18N
        }

        @Override
        public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
            return node instanceof RubyNodes.RubyNode && !(node instanceof RubyNodes.RubyObjectFieldNode || node instanceof RubyNodes.RubyObjectArrayItemNode);
        }

        @Override
        protected HeapViewerNode createObjectReferenceNode(RubyObject object, String type, FieldValue field) {
            return new RubyNodes.RubyObjectReferenceNode(object, type, field);
        }

        @Override
        protected Collection<FieldValue> getPropertyItems(RubyObject object, Heap heap) throws InterruptedException {
            HeapOperations.initializeReferences(heap);
            return object.getReferences();
        }

    }
    
}

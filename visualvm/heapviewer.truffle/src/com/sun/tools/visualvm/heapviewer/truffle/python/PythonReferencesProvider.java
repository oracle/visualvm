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
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyPlugin;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import java.util.Collection;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProviders(value={
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 300),
    @ServiceProvider(service=PythonReferencesProvider.class, position = 300)}
)
public class PythonReferencesProvider extends TruffleObjectPropertyProvider.References<PythonObject, PythonType, PythonHeapFragment, PythonLanguage> {
    
    public PythonReferencesProvider() {
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
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 300)
    public static class PluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!PythonHeapFragment.isPythonHeap(context)) return null;
            
            PythonReferencesProvider fieldsProvider = Lookup.getDefault().lookup(PythonReferencesProvider.class);
            return new TruffleObjectPropertyPlugin("References", "References", Icons.getIcon(ProfilerIcons.NODE_REVERSE), "python_objects_references", context, actions, fieldsProvider);
        }
        
    }

}

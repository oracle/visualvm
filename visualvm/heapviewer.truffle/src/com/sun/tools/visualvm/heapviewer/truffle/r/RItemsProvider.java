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
package com.sun.tools.visualvm.heapviewer.truffle.r;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleFrame;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyPlugin;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import java.util.Collection;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service = HeapViewerNode.Provider.class, position = 200)
public class RItemsProvider extends TruffleObjectPropertyProvider.Fields<RObject, RType, RHeapFragment, RLanguage> {
    
    public RItemsProvider() {
        super("items", RObject.class, RLanguage.instance(), true);
    }
    
    
    @Override
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("r_");
    }

    @Override
    public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
        if (node instanceof RNodes.RNode && !(node instanceof RNodes.RObjectReferenceNode || node instanceof RNodes.RObjectAttributeReferenceNode)) {
            TruffleObject object = HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
            RObject robject = object instanceof RObject ? (RObject)object : null;
            if (robject != null) {
                if (robject.getFieldValues().isEmpty()) {
                    TruffleFrame frame = robject.getFrame();
                    return frame != null && frame.isTruffleFrame();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected HeapViewerNode createObjectFieldNode(RObject object, String type, FieldValue field) {
        return new RNodes.RObjectFieldNode(object, type, field);
    }
    
    @Override
    protected Collection<FieldValue> getPropertyItems(RObject object, Heap heap) {
        List<FieldValue> fields = object.getFieldValues();
        if (fields.isEmpty()) {
            TruffleFrame frame = object.getFrame();
            fields = frame == null ? null : frame.getLocalFieldValues();
        }

        return fields;
    }
    
    @Override
    protected boolean includeInstance(Instance instance) {
        String className = instance.getJavaClass().getName();
        
        if (className.startsWith("java.lang.") ||
            className.startsWith("com.oracle.truffle.r.runtime.data."))
            return true;
        
        return false;
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 200)
    public static class PluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!RHeapFragment.isRHeap(context)) return null;
            
            Lookup.getDefault().lookupAll(HeapViewerNode.Provider.class);
            RItemsProvider fieldsProvider = Lookup.getDefault().lookup(RItemsProvider.class);
            
            return new TruffleObjectPropertyPlugin("Items", "Items", Icons.getIcon(ProfilerIcons.NODE_FORWARD), "r_objects_items", context, actions, fieldsProvider);
        }
        
    }

}

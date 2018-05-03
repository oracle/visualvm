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

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPreviewPlugin;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaScriptViewPlugins_PropertiesName=Properties",
    "JavaScriptViewPlugins_PropertiesDescription=Properties",
    "JavaScriptViewPlugins_ReferencesName=References",
    "JavaScriptViewPlugins_ReferencesDescription=References"
})
final class JavaScriptViewPlugins {
    
    // -------------------------------------------------------------------------
    // --- Preview -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    static class PreviewPlugin extends TruffleObjectPreviewPlugin {
    
        PreviewPlugin(HeapContext context) {
            super(context);
        }


        @Override
        protected boolean supportsNode(HeapViewerNode node) {
            return node instanceof JavaScriptNodes.JavaScriptObjectNode;
        }

        @Override
        protected Instance getPreviewInstance(HeapViewerNode node) {
            JavaScriptNodes.JavaScriptObjectNode jsnode = (JavaScriptNodes.JavaScriptObjectNode)node;
            if ("Function".equals(jsnode.getTypeName()) || "JSFunction".equals(jsnode.getTypeName())) { // NOI18N
                JavaScriptObject jsobj = jsnode.getTruffleObject();
                FieldValue dataField = jsobj.getFieldValue("functionData (hidden)"); // NOI18N
                Instance data = dataField instanceof ObjectFieldValue ? ((ObjectFieldValue)dataField).getInstance() : null;
                if (data == null) return null;

                Object rootNode = ((Instance)data).getValueOfField("lazyInit"); // NOI18N
                if (!(rootNode instanceof Instance)) {
                    Object callTarget = data.getValueOfField("callTarget"); // NOI18N
                    if (!(callTarget instanceof Instance)) return null;

                    rootNode = ((Instance)callTarget).getValueOfField("rootNode"); // NOI18N
                    if (!(rootNode instanceof Instance)) return null;
                }

                Instance sourceSection = null;
                List<FieldValue> rootNodeFields = ((Instance)rootNode).getFieldValues();
                for (FieldValue field : rootNodeFields) {
                    if ("sourceSection".equals(field.getField().getName()) && field instanceof ObjectFieldValue) { // NOI18N
                        Instance instance = ((ObjectFieldValue)field).getInstance();
                        if (instance != null) {
                            sourceSection = instance;
                            break;
                        }
                    }
                }

                if (!(sourceSection instanceof Instance)) {
                    Object nnode = ((Instance)rootNode).getValueOfField("node"); // NOI18N
                    if (!(nnode instanceof Instance)) return null;

                    Object ssourceSection = ((Instance)nnode).getValueOfField("source"); // NOI18N
                    if (!(ssourceSection instanceof Instance)) return null;

                    sourceSection = (Instance)ssourceSection;
                }

                return (Instance)sourceSection;
            } else {
                return null;
            }
        }
    
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 100)
    public static class PreviewPluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new PreviewPlugin(context);
            return null;
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- Fields --------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 200)
    public static class FieldsPluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!JavaScriptHeapFragment.isJavaScriptHeap(context)) return null;
            
            JavaScriptObjectProperties.FieldsProvider fieldsProvider = Lookup.getDefault().lookup(JavaScriptObjectProperties.FieldsProvider.class);
            return new TruffleObjectPropertyPlugin(Bundle.JavaScriptViewPlugins_PropertiesName(), Bundle.JavaScriptViewPlugins_PropertiesDescription(), Icons.getIcon(ProfilerIcons.NODE_FORWARD), "javascript_objects_fields", context, actions, fieldsProvider); // NOI18N
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- References ----------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 400)
    public static class ReferencesPluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!JavaScriptHeapFragment.isJavaScriptHeap(context)) return null;
            
            JavaScriptObjectProperties.ReferencesProvider fieldsProvider = Lookup.getDefault().lookup(JavaScriptObjectProperties.ReferencesProvider.class);
            return new TruffleObjectPropertyPlugin(Bundle.JavaScriptViewPlugins_ReferencesName(), Bundle.JavaScriptViewPlugins_ReferencesDescription(), Icons.getIcon(ProfilerIcons.NODE_REVERSE), "javascript_objects_references", context, actions, fieldsProvider); // NOI18N
        }
        
    }
    
}

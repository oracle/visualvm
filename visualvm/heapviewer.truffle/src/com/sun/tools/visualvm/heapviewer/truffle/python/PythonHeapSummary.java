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

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.ui.TruffleSummaryView;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class PythonHeapSummary {
    
    private static final String VIEW_ID = "python_summary"; // NOI18N
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class PythonSummaryProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (PythonHeapFragment.isPythonHeap(context)) {
                Icon icon = PythonSupport.createLanguageIcon(Icons.getIcon(HeapWalkerIcons.PROPERTIES));
                return new TruffleSummaryView(VIEW_ID, icon, context, actions);
            }

            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class PythonSummaryOverviewProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new PythonSummaryOverview(context);
            
            return null;
        }
        
    }
    
    private static class PythonSummaryOverview extends TruffleSummaryView.OverviewSection {
        
        PythonSummaryOverview(HeapContext context) {
            super(context, 3, 3);
        }
        
        protected void computeEnvironmentData(Object[][] environmentData) {
            super.computeEnvironmentData(environmentData);
            
            environmentData[1][0] = "Version:";
            environmentData[2][0] = "Platform:";
            
            PythonHeapFragment fragment = (PythonHeapFragment)getContext().getFragment();
            PythonType moduleType = fragment.getType("module", null);
            
            if (moduleType != null) {
                Heap heap = fragment.getHeap();
                
                PythonObject sysModule = null;
                Iterator<PythonObject> objects = moduleType.getObjectsIterator();
                while (objects.hasNext()) {
                    PythonObject object = objects.next();
                    if ("sys".equals(DetailsSupport.getDetailsString(object.getInstance(), heap))) {
                        sysModule = object;
                        break;
                    }
                }
                if (sysModule != null) {
                    String version = attributeValue(sysModule, "version", heap);
                    int graalInfoIdx = version.indexOf('[');
                    if (graalInfoIdx != -1) version = version.substring(0, graalInfoIdx);
                    environmentData[1][1] = version;
                    environmentData[2][1] = attributeValue(sysModule, "platform", heap);
                    
                    PythonObject implementation = attributeObject(sysModule, "implementation");                    
                    if (implementation != null) {
                        PythonObject _ns_ = attributeObject(implementation, "__ns__");
                        if (_ns_ != null) {
                            environmentData[1][1] = attributeValue(_ns_, "name", heap) + " " + version;
                            environmentData[2][1] = attributeValue(_ns_, "_multiarch", heap);
                        }
                    }
                }
            }
            
            if (environmentData[1][1] == null) environmentData[1][1] = "<unknown>";
            if (environmentData[2][1] == null) environmentData[2][1] = "<unknown>";
        }
        
        private static PythonObject attributeObject(PythonObject object, String attribute) {
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attr : attributes) {
                if (attribute.equals(attr.getField().getName())) {
                    Instance instance = attr instanceof ObjectFieldValue ? ((ObjectFieldValue)attr).getInstance() : null;
                    return PythonObject.isPythonObject(instance) ? new PythonObject(instance) : null;
                }
            }
            return null;
        }
        
        private static String attributeValue(PythonObject object, String attribute, Heap heap) {
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attr : attributes) {
                if (attribute.equals(attr.getField().getName())) {
                    Instance instance = attr instanceof ObjectFieldValue ? ((ObjectFieldValue)attr).getInstance() : null;
                    return instance == null ? null : DetailsSupport.getDetailsString(instance, heap);
                }
            }
            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 300)
    public static class PythonSummaryObjectsProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new PythonSummaryObjects(context, actions, actionProviders);
            
            return null;
        }
        
    }
    
    private static class PythonSummaryObjects extends TruffleSummaryView.ObjectsSection<PythonObject, PythonType> {
        
        PythonSummaryObjects(HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            super(PythonObjectsView.class, context, actions, actionProviders);
        }

        
        @Override
        protected Icon createLanguageIcon(Icon icon) {
            return PythonSupport.createLanguageIcon(icon);
        }
        
        @Override
        protected boolean isLanguageObject(Instance instance) {
            return PythonObject.isPythonObject(instance);
        }
        
        @Override
        protected PythonObject createObject(Instance instance) {
            return new PythonObject(instance);
        }
        
        @Override
        protected HeapViewerNode createTypeNode(PythonType type, Heap heap) {
            return new PythonNodes.PythonTypeNode(type);
        }
        
        @Override
        protected HeapViewerNode createObjectNode(PythonObject object, Heap heap) {
            return new PythonNodes.PythonObjectNode(object, object.getType(heap));
        }

    }
    
}

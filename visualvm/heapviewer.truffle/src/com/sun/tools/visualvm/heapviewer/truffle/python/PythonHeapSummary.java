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
import com.sun.tools.visualvm.heapviewer.truffle.AbstractObjectsProvider;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleSummaryView;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleType;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
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
                Icon icon = PythonSupport.createBadgedIcon(HeapWalkerIcons.PROPERTIES);
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
                    environmentData[1][1] = attributeValue(sysModule, "version", heap);
                    environmentData[2][1] = attributeValue(sysModule, "platform", heap);
                }
            }
            
            if (environmentData[1][1] == null) environmentData[1][1] = "<unknown>";
            if (environmentData[2][1] == null) environmentData[2][1] = "<unknown>";
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
    
    private static class PythonSummaryObjects extends TruffleSummaryView.ObjectsSection {
        
        PythonSummaryObjects(HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            super(context, actions, actionProviders);
        }

        
        @Override
        protected Runnable typesByCountDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {
                    PythonObjectsView objectsView = actions.findFeature(PythonObjectsView.class);
                    if (objectsView != null) {
                        objectsView.configureTypesByObjectsCount();
                        actions.selectFeature(objectsView);
                    }
                }
            };
        }

        @Override
        protected Runnable typesBySizeDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {
                    PythonObjectsView objectsView = actions.findFeature(PythonObjectsView.class);
                    if (objectsView != null) {
                        objectsView.configureTypesByObjectsSize();
                        actions.selectFeature(objectsView);
                    }
                }
            };
        }

        @Override
        protected Runnable objectsBySizeDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {
                    PythonObjectsView objectsView = actions.findFeature(PythonObjectsView.class);
                    if (objectsView != null) {
                        objectsView.configureObjectsBySize();
                        actions.selectFeature(objectsView);
                    }
                }
            };
        }

        @Override
        protected Runnable dominatorsByRetainedSizeDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {
                    PythonObjectsView objectsView = actions.findFeature(PythonObjectsView.class);
                    if (objectsView != null) {
                        objectsView.configureDominatorsByRetainedSize();
                        actions.selectFeature(objectsView);
                    }
                }
            };
        }
        
        
        @Override
        protected List<PythonObject> dominators(TruffleLanguageHeapFragment heapFragment) {
            int maxSearchInstances = 10000;

            List<Instance> searchInstances = heapFragment.getHeap().getBiggestObjectsByRetainedSize(maxSearchInstances);
            Iterator<Instance> searchInstancesI = searchInstances.iterator();
            while (searchInstancesI.hasNext()) {
                Instance instance = searchInstancesI.next();
                if (!PythonObject.isPythonObject(instance))
                    searchInstancesI.remove();
            }
            
            Set<Instance> rootInstances = AbstractObjectsProvider.getDominatorRoots(searchInstances);
            List<PythonObject> rootObjects = new ArrayList();
            for (Instance root : rootInstances) rootObjects.add(new PythonObject(root));
            
            return rootObjects;
        }
        
        
        @Override
        protected HeapViewerNode typeNode(TruffleType type, Heap heap) {
            return new PythonTypeNode((PythonType)type);
        }

        @Override
        protected ProfilerRenderer typeRenderer(Heap heap) {
            return new PythonObjectsContainer.Renderer();
        }
        
        @Override
        protected HeapViewerNode objectNode(TruffleObject object, Heap heap) {
            return new PythonObjectNode((PythonObject)object, object.getType(heap));
        }

        @Override
        protected ProfilerRenderer objectRenderer(Heap heap) {
            return new PythonObjectNode.Renderer(heap);
        }

    }
    
}

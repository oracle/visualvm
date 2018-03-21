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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.AbstractObjectsProvider;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectsContainer;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleSummaryView;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleType;
import com.sun.tools.visualvm.heapviewer.truffle.python.PythonObject;
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
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptHeapSummary {
    
    private static final String VIEW_ID = "javascript_summary"; // NOI18N
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class JavaScriptSummaryProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context)) {
                Icon icon = JavaScriptSupport.createBadgedIcon(HeapWalkerIcons.PROPERTIES);
                return new TruffleSummaryView(VIEW_ID, icon, context, actions);
            }

            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class JavaScriptSummaryOverviewProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new JavaScriptSummaryOverview(context);
            
            return null;
        }
        
    }
    
    private static class JavaScriptSummaryOverview extends TruffleSummaryView.OverviewSection {
        
        JavaScriptSummaryOverview(HeapContext context) {
            super(context);
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 200)
    public static class JavaScriptSummaryObjectsProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new JavaScriptSummaryObjects(context, actions, actionProviders);
            
            return null;
        }
        
    }
    
    private static class JavaScriptSummaryObjects extends TruffleSummaryView.ObjectsSection {
        
        JavaScriptSummaryObjects(HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            super(context, actions, actionProviders);
        }

        
        @Override
        protected Runnable classesByCountDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {}
            };
        }

        @Override
        protected Runnable classesBySizeDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {}
            };
        }

        @Override
        protected Runnable instancesBySizeDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {}
            };
        }

        @Override
        protected Runnable dominatorsByRetainedSizeDisplayer(HeapViewerActions actions) {
            return new Runnable() {
                public void run() {}
            };
        }
        
        
        @Override
        protected List<JavaScriptDynamicObject> dominators(TruffleLanguageHeapFragment heapFragment) {
            int maxSearchInstances = 10000;

            List<Instance> searchInstances = heapFragment.getHeap().getBiggestObjectsByRetainedSize(maxSearchInstances);
            Iterator<Instance> searchInstancesI = searchInstances.iterator();
            while (searchInstancesI.hasNext()) {
                Instance instance = searchInstancesI.next();
                if (!JavaScriptDynamicObject.isJavaScriptObject(instance))
                    searchInstancesI.remove();
            }
            
            Set<Instance> rootInstances = AbstractObjectsProvider.getDominatorRoots(searchInstances);
            List<JavaScriptDynamicObject> rootObjects = new ArrayList();
            for (Instance root : rootInstances) rootObjects.add(new JavaScriptDynamicObject(root));
            
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException ex) {
//                Exceptions.printStackTrace(ex);
//            }
            
            return rootObjects;
        }
        
        
        @Override
        protected HeapViewerNode typeNode(TruffleType type, Heap heap) {
            return new JavaScriptNodes.JavaScriptTypeNode((JavaScriptType)type);
        }

        @Override
        protected ProfilerRenderer typeRenderer(Heap heap) {
            Icon packageIcon = JavaScriptSupport.createBadgedIcon(LanguageIcons.PACKAGE);
            return new DynamicObjectsContainer.Renderer(packageIcon);
        }
        
        @Override
        protected HeapViewerNode objectNode(TruffleObject object, Heap heap) {
            return new JavaScriptNodes.JavaScriptDynamicObjectNode((JavaScriptDynamicObject)object, object.getType(heap));
        }

        @Override
        protected ProfilerRenderer objectRenderer(Heap heap) {
            Icon instanceIcon = JavaScriptSupport.createBadgedIcon(LanguageIcons.INSTANCE);
            return new DynamicObjectNode.Renderer(heap, instanceIcon);
        }

    }
    
}

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
package org.graalvm.visualvm.heapviewer.truffle.lang.ruby;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleObjectsView;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleSummaryView;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleThreadsView;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.SummaryView;
import java.util.Collection;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RubyViews_Platform=Platform:",
    "RubyViews_Unknown=<unknown>"
})
final class RubyViews {
    
    // -------------------------------------------------------------------------
    // --- Summary -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class SummaryViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new TruffleSummaryView(RubyLanguage.instance(), context, actions);

            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class SummaryOverviewProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new SummaryOverview(context);
            
            return null;
        }
        
    }
    
    private static class SummaryOverview extends TruffleSummaryView.OverviewSection {
        
        SummaryOverview(HeapContext context) {
            super(context, 3, 2);
        }
        
        protected void computeEnvironmentData(Object[][] environmentData) {
            super.computeEnvironmentData(environmentData);
            
            environmentData[1][0] = Bundle.RubyViews_Platform();
            
            RubyHeapFragment fragment = (RubyHeapFragment)getContext().getFragment();
            RubyType gemPlatform = fragment.getType("Gem::Platform", null); // NOI18N
            RubyObject platformO = gemPlatform == null || gemPlatform.getObjectsCount() == 0 ?
                                         null : gemPlatform.getObjectsIterator().next();
            
            if (platformO != null) {
                Heap heap = fragment.getHeap();
                
                String osFV = variableValue(platformO, "@os", heap); // NOI18N
                String cpuFV = variableValue(platformO, "@cpu", heap); // NOI18N
                if (osFV != null || cpuFV != null) {
                    String platform = osFV;
                    if (cpuFV != null) {
                        if (platform != null) platform += " "; else platform = ""; // NOI18N
                        platform += cpuFV;
                    }
                    environmentData[1][1] = platform;
                }
            }
            
            if (environmentData[1][1] == null) environmentData[1][1] = Bundle.RubyViews_Unknown();
        }
        
        private static String variableValue(RubyObject object, String field, Heap heap) {
            FieldValue value = object == null ? null : object.getFieldValue(field);
            Instance instance = value instanceof ObjectFieldValue ? ((ObjectFieldValue)value).getInstance() : null;
            if (instance == null || !RubyObject.isRubyObject(instance)) return null;
            RubyObject variableO = new RubyObject(instance);
            return RubyNodes.getLogicalValue(variableO, variableO.getType(heap), heap);
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 300)
    public static class SummaryObjectsProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new TruffleSummaryView.ObjectsSection(RubyLanguage.instance(), context, actions, actionProviders);
            
            return null;
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- Objects -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class ObjectsViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new TruffleObjectsView(RubyLanguage.instance(), context, actions);
            
            return null;
        }

    }
    
    
    // -------------------------------------------------------------------------
    // --- Threads -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class ThreadsViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new TruffleThreadsView(RubyLanguage.instance(), context, actions);
            
            return null;
        }

    }
    
}

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
package org.graalvm.visualvm.heapviewer.truffle.lang.r;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.TruffleFrame;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleObjectsView;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleSummaryView;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.SummaryView;
import java.util.Collection;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RViews_Version=Version:",
    "RViews_Platform=Platform:",
    "RViews_Unknown=<unknown>"
})
final class RViews {
    
    // -------------------------------------------------------------------------
    // --- Summary -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class SummaryViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RHeapFragment.isRHeap(context))
                return new TruffleSummaryView(RLanguage.instance(), context, actions);

            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class SummaryOverviewProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (RHeapFragment.isRHeap(context))
                return new SummaryOverview(context);
            
            return null;
        }
        
    }
    
    private static class SummaryOverview extends TruffleSummaryView.OverviewSection {
        
        SummaryOverview(HeapContext context) {
            super(context, 3, 3);
        }
        
        protected void computeEnvironmentData(Object[][] environmentData) {
            super.computeEnvironmentData(environmentData);
            
            environmentData[1][0] = Bundle.RViews_Version();
            environmentData[2][0] = Bundle.RViews_Platform();
            
            RHeapFragment fragment = (RHeapFragment)getContext().getFragment();
            Heap heap = fragment.getHeap();
            
            JavaClass envClass = heap.getJavaClassByName("com.oracle.truffle.r.runtime.env.REnvironment$Base"); // NOI18N
            if (envClass != null && envClass.getInstancesCount() > 0) {
                Instance envInstance = envClass.getInstancesIterator().next();
                if (RObject.isRObject(envInstance)) {
                    RObject envObj = new RObject(envInstance);
                    TruffleFrame envFrame = envObj.getFrame();
                    if (envFrame != null && envFrame.isTruffleFrame()) {
                        List<FieldValue> fields = envFrame.getLocalFieldValues();
                        for (FieldValue field : fields) {
                            String fieldName = field.getField().getName();
                            if (("R.version".equals(fieldName) || "version".equals(fieldName)) && field instanceof ObjectFieldValue) { // NOI18N
                                Instance envI = ((ObjectFieldValue)field).getInstance();
                                if (RObject.isRObject(envI)) {
                                    RObject env = new RObject(envI);
                                    environmentData[1][1] = itemValue(env, "version.string", heap); // NOI18N
                                    environmentData[2][1] = itemValue(env, "system", heap); // NOI18N
                                }
                                                                
                                break;
                            }
                        }
                    }
                }
            }
            
            if (environmentData[1][1] == null) environmentData[1][1] = Bundle.RViews_Unknown();
            if (environmentData[2][1] == null) environmentData[2][1] = Bundle.RViews_Unknown();
        }
        
        private static String itemValue(RObject object, String item, Heap heap) {
            item = "(" + item + ")"; // NOI18N
            List<FieldValue> items = object.getFieldValues();
            for (FieldValue itemv : items) {
                String itemn = itemv.getField().getName();
                if (itemn != null && itemn.contains(item)) {
                    Instance instance = itemv instanceof ObjectFieldValue ? ((ObjectFieldValue)itemv).getInstance() : null;
                    return instance == null ? null : DetailsSupport.getDetailsString(instance, heap);
                }
            }
            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 300)
    public static class SummaryObjectsProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (RHeapFragment.isRHeap(context))
                return new TruffleSummaryView.ObjectsSection(RLanguage.instance(), context, actions, actionProviders);
            
            return null;
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- Objects -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class ObjectsViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RHeapFragment.isRHeap(context))
                return new TruffleObjectsView(RLanguage.instance(), context, actions);
            
            return null;
        }

    }
    
}

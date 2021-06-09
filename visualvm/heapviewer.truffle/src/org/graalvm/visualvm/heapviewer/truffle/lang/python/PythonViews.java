/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.python;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleObjectsView;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleSummaryView;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.SummaryView;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleThreadsView;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "PythonViews_Version=Version:",
    "PythonViews_Platform=Platform:",
    "PythonViews_Unknown=<unknown>"
})
final class PythonViews {
    
    // -------------------------------------------------------------------------
    // --- Summary -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class SummaryViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new TruffleSummaryView(PythonLanguage.instance(), context, actions);

            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class SummaryOverviewProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (PythonHeapFragment.isPythonHeap(context))
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
            
            environmentData[1][0] = Bundle.PythonViews_Version();
            environmentData[2][0] = Bundle.PythonViews_Platform();
            
            PythonHeapFragment fragment = (PythonHeapFragment)getContext().getFragment();
            PythonType moduleType = fragment.getType("module", null); // NOI18N
            
            if (moduleType != null) {
                PythonObject sysModule = null;
                Iterator<PythonObject> objects = moduleType.getObjectsIterator();
                while (objects.hasNext()) {
                    PythonObject object = objects.next();
                    if ("sys".equals(DetailsSupport.getDetailsString(object.getInstance()))) { // NOI18N
                        sysModule = object;
                        break;
                    }
                }
                if (sysModule != null) {
                    String version = attributeValue(sysModule, "version"); // NOI18N
                    int graalInfoIdx = version == null ? -1 : version.indexOf('['); // NOI18N
                    if (graalInfoIdx != -1) version = version.substring(0, graalInfoIdx);
                    environmentData[1][1] = version;
                    environmentData[2][1] = attributeValue(sysModule, "platform"); // NOI18N
                    
                    PythonObject implementation = attributeObject(sysModule, "implementation");      // NOI18N               
                    if (implementation != null) {
                        PythonObject _ns_ = attributeObject(implementation, "__ns__"); // NOI18N
                        if (_ns_ != null) {
                            environmentData[1][1] = attributeValue(_ns_, "name") + " " + version; // NOI18N
                            environmentData[2][1] = attributeValue(_ns_, "_multiarch"); // NOI18N
                        }
                    }
                }
            }
            
            if (environmentData[1][1] == null) environmentData[1][1] = Bundle.PythonViews_Unknown();
            if (environmentData[2][1] == null) environmentData[2][1] = Bundle.PythonViews_Unknown();
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
        
        private static String attributeValue(PythonObject object, String attribute) {
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attr : attributes) {
                if (attribute.equals(attr.getField().getName())) {
                    Instance instance = attr instanceof ObjectFieldValue ? ((ObjectFieldValue)attr).getInstance() : null;
                    return instance == null ? null : DetailsSupport.getDetailsString(instance);
                }
            }
            return null;
        }

    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 300)
    public static class SummaryObjectsProvider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new TruffleSummaryView.ObjectsSection(PythonLanguage.instance(), context, actions, actionProviders);
            
            return null;
        }
        
    }
    
    
    // -------------------------------------------------------------------------
    // --- Objects -------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class ObjectsViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new TruffleObjectsView(PythonLanguage.instance(), context, actions);

            return null;
        }

    }


    // -------------------------------------------------------------------------
    // --- Threads -------------------------------------------------------------
    // -------------------------------------------------------------------------

    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class ThreadsViewProvider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new TruffleThreadsView(PythonLanguage.instance(), context, actions);

            return null;
        }
    }
}

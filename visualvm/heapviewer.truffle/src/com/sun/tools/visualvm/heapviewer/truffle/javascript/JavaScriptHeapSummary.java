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
import com.sun.tools.visualvm.heapviewer.truffle.TruffleSummaryView;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import java.util.Collection;
import javax.swing.Icon;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
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
        
//        @Override
//        protected Iterator getObjectsIterator() {
//            JavaScriptHeapFragment fragment = (JavaScriptHeapFragment)getContext().getFragment();
//            return fragment.getObjectsIterator();
//        }
//
//        @Override
//        protected String getType(Object object, Map<Object, String> typesCache) {
//            Heap heap = getContext().getFragment().getHeap();
//            return ((JavaScriptDynamicObject)object).getType(heap);
////            DynamicObject dobject = (DynamicObject)object;
////            Instance shape = dobject.getShape();
////            
////            String type = typesCache.get(shape);
////            if (type == null) {
////                type = DetailsSupport.getDetailsString(shape, getContext().getFragment().getHeap());
////                typesCache.put(shape, type);
////            }
////            
////            return type;
//        }
//        
//        @Override
//        protected long updateObjectsSize(Object object, long objectsSize) {
//            return objectsSize == 0 ? ((JavaScriptDynamicObject)object).getInstance().getSize() : objectsSize;
//        }
//
//        @Override
//        protected long getObjectsSize(long objectsSize, long objectsCount) {
//            return objectsCount * objectsSize;
//        }

    }
    
}

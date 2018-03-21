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
package com.sun.tools.visualvm.heapviewer.truffle.ruby;

import java.util.List;
import javax.swing.Icon;
import javax.swing.SortOrder;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectsView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyObjectsView extends TruffleObjectsView {
    
    private static final String FEATURE_ID = "ruby_objects"; // NOI18N
    
    
    RubyObjectsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, context, actions);
    }
    

    @Override
    protected Icon languageBrandedIcon(String iconKey) {
        return RubySupport.createBadgedIcon(iconKey);
    }

    @Override
    protected HeapViewerNode[] computeData(Preset preset, Aggregation aggregation, RootNode root, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        switch (preset) {
            case ALL_OBJECTS:
                switch (aggregation) {
                    case TYPES:
                        return RubyObjectsProvider.getAllObjects(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                    default:
                        return RubyObjectsProvider.getAllObjects(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                }
            case DOMINATORS:
                switch (aggregation) {
                    case TYPES:
                        return RubyObjectsProvider.getDominators(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                    default:
                        return RubyObjectsProvider.getDominators(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                }
            case GC_ROOTS:
                switch (aggregation) {
                    case TYPES:
                        return RubyObjectsProvider.getGCRoots(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                    default:
                        return RubyObjectsProvider.getGCRoots(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                }
            default:
                return HeapViewerNode.NO_NODES;
        }
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new RubyObjectsView(context, actions);
            
            return null;
        }

    }
    
}

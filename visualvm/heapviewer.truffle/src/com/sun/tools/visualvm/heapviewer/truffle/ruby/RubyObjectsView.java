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

import javax.swing.Icon;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectsProvider;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.truffle.ui.TruffleObjectsView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyObjectsView extends TruffleObjectsView {
    
    private static final String FEATURE_ID = "ruby_objects"; // NOI18N
    
    
    RubyObjectsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, context, actions, new RubyObjectsProvider());
    }
    

    @Override
    protected Icon languageBrandedIcon(String iconKey) {
        return RubySupport.createBadgedIcon(iconKey);
    }
    
    
    private static class RubyObjectsProvider extends TruffleObjectsProvider<RubyObject, RubyType> {
    
        @Override
        protected TruffleObjectNode<RubyObject> createObjectNode(RubyObject object, String type) {
            return new RubyNodes.RubyObjectNode(object, type);
        }

        @Override
        protected TruffleTypeNode<RubyObject, RubyType> createTypeNode(RubyType type) {
            return new RubyNodes.RubyTypeNode(type);
        }

        @Override
        protected boolean isLanguageObject(Instance instance) {
            return RubyObject.isRubyObject(instance);
        }

        @Override
        protected RubyObject createObject(Instance instance) {
            return new RubyObject(instance);
        }

        @Override
        protected RubyType createTypeContainer(String name) {
            return new RubyType(name);
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

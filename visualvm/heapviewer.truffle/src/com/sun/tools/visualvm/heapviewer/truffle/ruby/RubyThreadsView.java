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

import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLocalObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleThreadsProvider;
import com.sun.tools.visualvm.heapviewer.truffle.ui.TruffleThreadsView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class RubyThreadsView extends TruffleThreadsView {
    
    private static final String FEATURE_ID = "ruby_threads"; // NOI18N
    
    
    public RubyThreadsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, RubySupport.createLanguageIcon(Icons.getIcon(ProfilerIcons.WINDOW_THREADS)), context, actions, new RubyThreadsProvider());
    }
    
    
    private static class RubyThreadsProvider extends TruffleThreadsProvider<RubyObject> {

        @Override
        protected boolean isLanguageObject(Instance instance) {
            return RubyObject.isRubyObject(instance);
        }

        @Override
        protected RubyObject createObject(Instance instance) {
            return new RubyObject(instance);
        }

        @Override
        protected TruffleObjectNode<RubyObject> createObjectNode(RubyObject object, String type) {
            return new RubyNodes.RubyObjectNode(object, type);
        }

        @Override
        protected TruffleLocalObjectNode<RubyObject> createLocalObjectNode(RubyObject object, String type) {
            return new RubyNodes.RubyLocalObjectNode(object, type);
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (RubyHeapFragment.isRubyHeap(context))
                return new RubyThreadsView(context, actions);
            
            return null;
        }

    }
    
}

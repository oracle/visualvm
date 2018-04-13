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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectsProvider;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.truffle.ui.TruffleObjectsView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class PythonObjectsView extends TruffleObjectsView {
    
    private static final String FEATURE_ID = "python_objects"; // NOI18N
    
    
    PythonObjectsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, context, actions, new PythonObjectsProvider());
    }
    

    @Override
    protected Icon languageBrandedIcon(String iconKey) {
        return PythonSupport.createBadgedIcon(iconKey);
    }
    
    
    private static class PythonObjectsProvider extends TruffleObjectsProvider<PythonObject, PythonType> {
    
        @Override
        protected TruffleObjectNode<PythonObject> createObjectNode(PythonObject object, String type) {
            return new PythonNodes.PythonObjectNode(object, type);
        }

        @Override
        protected TruffleTypeNode<PythonObject, PythonType> createTypeNode(PythonType type) {
            return new PythonNodes.PythonTypeNode(type);
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
        protected PythonType createTypeContainer(String name) {
            return new PythonType(name);
        }

    }


    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (PythonHeapFragment.isPythonHeap(context))
                return new PythonObjectsView(context, actions);

            return null;
        }

    }

}

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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSummaryView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaScriptHeapSummaryView extends TruffleLanguageSummaryView {
    
    public JavaScriptHeapSummaryView(HeapContext context) {
        super(JavaScriptSupport.createBadgedIcon(HeapWalkerIcons.PROPERTIES), context);
    }

    @Override
    protected String computeSummary(HeapContext context) {
        long jsObjects = 0;
        long objectSize = 0;
        NumberFormat numberFormat = NumberFormat.getInstance();
        Map<Instance, String> shapes = new HashMap();
        Map<String, Object> types = new HashMap();
        List nodes = new ArrayList();
        TruffleLanguageHeapFragment fragment = (TruffleLanguageHeapFragment) context.getFragment();
        Heap heap = fragment.getHeap();
        Iterator<DynamicObject> instancesI = fragment.getDynamicObjectsIterator();

        while (instancesI.hasNext()) {
            DynamicObject dobject = instancesI.next();
            Instance shape = dobject.getShape();
            String type = shapes.get(shape);
            if (type == null) {
                type = DetailsSupport.getDetailsString(shape, heap);
                shapes.put(shape, type);
            }

            Object typeNode = types.get(type);
            if (typeNode == null) {
                String langid = dobject.getLanguageId().getName();
                if (JavaScriptObjectsProvider.JS_LANG_ID.equals(langid)) {
                    typeNode = new Object();
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
            }
            if (typeNode != null) {
                jsObjects++;
                if (objectSize == 0) {
                    objectSize = dobject.getInstance().getSize();
                }
            }
        }
        String header = super.computeSummary(context);
        String bytes = LINE_PREFIX + "<b>Total bytes:&nbsp;</b>" + numberFormat.format(jsObjects * objectSize) + "<br>"; // NOI18N
        String typesCount = LINE_PREFIX + "<b>Total types:&nbsp;</b>" + numberFormat.format(nodes.size()) + "<br>"; // NOI18N
        String jobjects = LINE_PREFIX + "<b>Total objects:&nbsp;</b>" + numberFormat.format(jsObjects) + "<br><br>"; // NOI18N
        return header + bytes + typesCount + jobjects;
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (JavaScriptHeapFragment.isJavaScriptHeap(context))
                return new JavaScriptHeapSummaryView(context);
            
            return null;
        }

    }
    
}

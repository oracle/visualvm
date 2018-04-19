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

import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Heap;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleInstancePropertyProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNode.Provider.class, position = 300)
public class JavaScriptArrayItemsProvider extends TruffleInstancePropertyProvider.ArrayItems<JavaScriptObject, JavaScriptType, JavaScriptHeapFragment, JavaScriptLanguage> {
    
    public JavaScriptArrayItemsProvider() {
        super("items", JavaScriptLanguage.instance(), false);
    }
    
    
    @Override
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("javascript_");
    }
    
    
    @Override
    protected HeapViewerNode createObjectArrayItemNode(JavaScriptObject object, String type, ArrayItemValue item) {
        return new JavaScriptNodes.JavaScriptObjectArrayItemNode(object, type, item);
    }
    
}

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
package com.sun.tools.visualvm.heapviewer.truffle.lang.javascript;

import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguage;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProviders(value={
    @ServiceProvider(service=HeapFragment.Provider.class, position = 200),
    @ServiceProvider(service=JavaScriptLanguage.class, position = 200)}
)
public class JavaScriptLanguage extends TruffleLanguage<JavaScriptObject, JavaScriptType, JavaScriptHeapFragment> {
    
    private static final String ID = "javascript"; // NOI18N
    
    private static final String JS_LANGINFO_ID = "JS";  // NOI18N
    private static final String JAVASCRIPT_LANGINFO_ID = "JavaScript";  // NOI18N
    
    
    static JavaScriptLanguage instance() {
        return Lookup.getDefault().lookup(JavaScriptLanguage.class);
    }
    
    
    public String getID() { return ID; }
    
    
    @Override
    protected JavaScriptHeapFragment createFragment(Heap heap) {
        Instance langID = getLanguageInfo(heap, JS_LANGINFO_ID);
        if (langID == null) langID = getLanguageInfo(heap, JAVASCRIPT_LANGINFO_ID);
        if (langID == null) return null;
        
        JavaClass JSMainClass = heap.getJavaClassByName(JavaScriptHeapFragment.JS_LANG_ID);
        if (JSMainClass == null) return null;

        return new JavaScriptHeapFragment(this, langID, heap);
    }
    

    @Override
    public boolean isLanguageObject(Instance instance) {
        return JavaScriptObject.isJavaScriptObject(instance);
    }
    
    @Override
    public JavaScriptObject createObject(Instance instance) {
        return new JavaScriptObject(instance);
    }
    
    @Override
    public JavaScriptType createType(String name) {
        return new JavaScriptType(name);
    }
    
    
    @Override
    public JavaScriptNodes.JavaScriptObjectNode createObjectNode(JavaScriptObject object, String type) {
        return new JavaScriptNodes.JavaScriptObjectNode(object, type);
    }
    
    @Override
    public JavaScriptNodes.JavaScriptLocalObjectNode createLocalObjectNode(JavaScriptObject object, String type) {
        return new JavaScriptNodes.JavaScriptLocalObjectNode(object, type);
    }
    
    @Override
    public JavaScriptNodes.JavaScriptTypeNode createTypeNode(JavaScriptType type, Heap heap) {
        return new JavaScriptNodes.JavaScriptTypeNode(type);
    }
    
}

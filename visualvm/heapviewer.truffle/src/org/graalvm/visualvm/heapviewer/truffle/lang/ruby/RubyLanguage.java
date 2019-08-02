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
package org.graalvm.visualvm.heapviewer.truffle.lang.ruby;

import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguage;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProviders(value={
    @ServiceProvider(service=HeapFragment.Provider.class, position = 500),
    @ServiceProvider(service=RubyLanguage.class, position = 500)}
)
public class RubyLanguage extends TruffleLanguage<RubyObject, RubyType, RubyHeapFragment> {
    
    private static final String ID = "ruby"; // NOI18N
    
    private static final String RUBY_LANGINFO_ID = "Ruby";  // NOI18N
    
    
    private static RubyLanguage INSTANCE; 
    static synchronized RubyLanguage instance() {
        if (INSTANCE == null) Lookup.getDefault().lookup(RubyLanguage.class);
        return INSTANCE;
    }
    public RubyLanguage() { INSTANCE = this; }
    
    
    public String getID() { return ID; }
    
    
    @Override
    protected RubyHeapFragment createFragment(Heap heap) {
        Instance langID = getLanguageInfo(heap, RUBY_LANGINFO_ID);
        if (langID == null) return null;
        
        JavaClass rubyMainClass = heap.getJavaClassByName(RubyHeapFragment.RUBY_LANG_ID1);
        if (rubyMainClass == null) {
            rubyMainClass = heap.getJavaClassByName(RubyHeapFragment.RUBY_LANG_ID);
            if (rubyMainClass == null) {
                return null;
            }
        }

        return new RubyHeapFragment(this, rubyMainClass, langID, heap);
    }
    
    
    @Override
    public Class<RubyObject> getLanguageObjectClass() {
        return RubyObject.class;
    }
    

    @Override
    public boolean isLanguageObject(Instance instance) {
        return RubyObject.isRubyObject(instance);
    }
    
    @Override
    public RubyObject createObject(Instance instance) {
        return new RubyObject(instance);
    }
    
    @Override
    public RubyType createType(String name) {
        return new RubyType(name);
    }
    
    
    @Override
    public RubyNodes.RubyObjectNode createObjectNode(RubyObject object, String type) {
        return new RubyNodes.RubyObjectNode(object, type);
    }
    
    @Override
    public RubyNodes.RubyLocalObjectNode createLocalObjectNode(RubyObject object, String type) {
        return new RubyNodes.RubyLocalObjectNode(object, type);
    }
    
    @Override
    public RubyNodes.RubyTypeNode createTypeNode(RubyType type, Heap heap) {
        return new RubyNodes.RubyTypeNode(type);
    }
    
}

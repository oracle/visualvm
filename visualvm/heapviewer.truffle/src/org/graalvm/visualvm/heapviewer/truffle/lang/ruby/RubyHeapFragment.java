/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectLanguageHeapFragment;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RubyHeapFragment_Name=Ruby Heap"
})
class RubyHeapFragment extends DynamicObjectLanguageHeapFragment<RubyObject, RubyType> {
    
    static final String RUBY_LANG_ID = "org.truffleruby.language.RubyObjectType"; // NOI18N
    static final String RUBY_LANG_ID1 = "org.truffleruby.interop.RubyObjectType"; // NOI18N
    static final String RUBY_LANG_ID2 = "org.truffleruby.language.objects.RubyObjectType"; // NOI18N
    
    private static final String RUBY_HEAP_ID = "ruby_heap"; // NOI18N
    
    private final String rubyLangId;
    
    RubyHeapFragment(RubyLanguage language, JavaClass rubyLangIdClass, Instance langID, Heap heap) {
        super(RUBY_HEAP_ID, Bundle.RubyHeapFragment_Name(), fragmentDescription(langID), language, heap);
        rubyLangId = rubyLangIdClass.getName();
    }
    
    
    static RubyHeapFragment fromContext(HeapContext context) {
        return (RubyHeapFragment)context.getFragment();
    }
    
    
    @Override
    public Iterator<Instance> getInstancesIterator() {
        return languageInstancesIterator(rubyLangId);
    }
    
    @Override
    public Iterator<RubyObject> getObjectsIterator() {
        return languageObjectsIterator(rubyLangId);
    }

    
    static boolean isRubyHeap(HeapContext context) {
        return RUBY_HEAP_ID.equals(context.getFragment().getID());
    }
    
//    public static HeapContext getRubyContext(HeapContext context) {
//        if (isRubyHeap(context)) return context;
//        
//        for (HeapContext otherContext : context.getOtherContexts())
//            if (isRubyHeap(otherContext)) return otherContext;
//        
//        return null;
//    }
    
}

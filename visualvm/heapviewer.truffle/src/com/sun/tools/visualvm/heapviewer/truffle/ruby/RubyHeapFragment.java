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

import java.io.File;
import java.io.IOException;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageSupport;
import java.util.Iterator;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyHeapFragment extends TruffleLanguageHeapFragment.DynamicObjectBased<RubyDynamicObject, RubyType> {
    
    static final String RUBY_LANG_ID = "org.truffleruby.language.RubyObjectType"; // NOI18N
    
    private static final String RUBY_HEAP_ID = "ruby_heap";
    
    
    RubyHeapFragment(Instance langID, Heap heap) throws IOException {
        super(RUBY_HEAP_ID, "Ruby Heap", fragmentDescription(langID, heap), heap);
    }
    
    
    @Override
    protected RubyDynamicObject createObject(Instance instance) {
        return new RubyDynamicObject(instance);
    }

    @Override
    protected RubyType createTruffleType(String name) {
        return new RubyType(name);
    }

    
    @Override
    protected Iterator<Instance> getInstancesIterator() {
        return languageInstancesIterator(RUBY_LANG_ID);
    }
    
    @Override
    protected Iterator<RubyDynamicObject> getObjectsIterator() {
        return languageObjectsIterator(RUBY_LANG_ID);
    }

    
    @Override
    protected long getObjectSize(RubyDynamicObject object) {
        return object.getInstance().getSize();
    }
    
    @Override
    protected long getObjectRetainedSize(RubyDynamicObject object) {
        return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
               object.getInstance().getRetainedSize() :
               DataType.RETAINED_SIZE.getNotAvailableValue();
    }

    @Override
    protected String getObjectType(RubyDynamicObject object) {
        return object.getType(heap);
    }
    
    
    static boolean isRubyHeap(HeapContext context) {
        return RUBY_HEAP_ID.equals(context.getFragment().getID()); // NOI18N
    }
    
    public static HeapContext getRubyContext(HeapContext context) {
        if (isRubyHeap(context)) return context;
        
        for (HeapContext otherContext : context.getOtherContexts())
            if (isRubyHeap(otherContext)) return otherContext;
        
        return null;
    }
    
    
    @ServiceProvider(service=HeapFragment.Provider.class, position = 300)
    public static class Provider extends HeapFragment.Provider {

        private static final String RUBY_LANGINFO_ID = "Ruby";  // NOI18N

        public HeapFragment getFragment(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
            Instance langID = TruffleLanguageSupport.getLanguageInfo(heap, RUBY_LANGINFO_ID);
            JavaClass rubyMainClass = heap.getJavaClassByName(RUBY_LANG_ID);

            return langID != null && rubyMainClass != null ? new RubyHeapFragment(langID, heap) : null;
        }

    }
    
}

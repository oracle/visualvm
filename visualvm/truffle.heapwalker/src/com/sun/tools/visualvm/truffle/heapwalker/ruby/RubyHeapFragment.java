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
package com.sun.tools.visualvm.truffle.heapwalker.ruby;

import java.io.File;
import java.io.IOException;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.HeapFragment;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleLanguageSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyHeapFragment extends TruffleLanguageHeapFragment {
    
    private static final String RUBY_HEAP_ID = "ruby_heap";
    
    
    RubyHeapFragment(Instance langInfo, Heap heap) throws IOException {
        super("Ruby Heap", RUBY_HEAP_ID, langInfo, heap);
    }
    
    
    static boolean isRubyHeap(HeapContext context) {
        return RUBY_HEAP_ID.equals(context.getFragment().getID()); // NOI18N
    }
    
    static RubyHeapFragment heap(Heap heap) {
        return (RubyHeapFragment)heap;
    }
    
    
    @ServiceProvider(service=HeapFragment.Provider.class, position = 300)
    public static class Provider extends HeapFragment.Provider {

        private static final String RUBY_LANG_ID = "Ruby";  // NOI18N

        public HeapFragment getFragment(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
            Instance langInfo = TruffleLanguageSupport.getLanguageInfo(heap, RUBY_LANG_ID);
            JavaClass RubyMainClass = heap.getJavaClassByName(RubyObjectsProvider.RUBY_LANG_ID);

            return langInfo != null && RubyMainClass != null ? new RubyHeapFragment(langInfo, heap) : null;
        }

    }
    
}

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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import com.sun.tools.visualvm.heapviewer.truffle.TruffleType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.netbeans.lib.profiler.heap.Instance;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptType extends TruffleType<JavaScriptDynamicObject> {
    
    private final List<Instance> instances;
    
    
    public JavaScriptType(String name) {
        super(name);
        this.instances = new ArrayList();
    }
    
    
    @Override
    public Iterator<JavaScriptDynamicObject> getObjectsIterator() {
        return new ObjectsIterator(getName(), instances.iterator());
    }
    
    @Override
    protected void addObject(JavaScriptDynamicObject object, long objectSize, long objectRetainedSize) {
        super.addObject(object, objectSize, objectRetainedSize);
        instances.add(object.getInstance());
    }
    
    
    // Copied from TruffleLanguageHeapFragment, share somehow!
    private static class ObjectsIterator implements Iterator<JavaScriptDynamicObject> {
        
        private final String type;
        private final Iterator<Instance> instancesIter;
        
        protected ObjectsIterator(String t, Iterator<Instance> iter) {
            type = t;
            instancesIter = iter;
        }

        @Override
        public boolean hasNext() {
            return instancesIter.hasNext();
        }

        @Override
        public JavaScriptDynamicObject next() {
            return new JavaScriptDynamicObject(type, instancesIter.next());
        }
        
    }
    
}

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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.model.DataType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TruffleType<O extends TruffleObject> {
    
    private final String name;
    
    protected int count;
    protected long size;
    protected long retained = DataType.RETAINED_SIZE.getNotAvailableValue();


    public TruffleType(String name) {
        this.name = name;
    }


    public String getName() { return name; }

    public int getObjectsCount() { return count; }

    public long getAllObjectsSize() { return size; }
    
    // TODO: slow objectsIterator called in EDT, resolve somehow!
    public long getRetainedSizeByType(Heap heap) {
        if (retained < 0 && DataType.RETAINED_SIZE.valuesAvailable(heap)) {
            retained = 0;
            Iterator<O> objects = getObjectsIterator();
            while (objects.hasNext()) retained += objects.next().getRetainedSize();
        }
        return retained;
    }

    
    public abstract Iterator<O> getObjectsIterator();


    protected void addObject(O object, long objectSize, long objectRetainedSize) {
        count++;
        this.size += objectSize;
        if (objectRetainedSize >= 0) {
            if (this.retained < 0) this.retained = 0;
            this.retained += objectRetainedSize;
        }
    }


    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TruffleType)) return false;
        return name.equals(((TruffleType)o).name);
    }
    
    
    public static abstract class InstanceBased<O extends TruffleObject.InstanceBased> extends TruffleType<O> {
        
        private final List<Instance> instances;
        
        
        public InstanceBased(String name) {
            super(name);
            this.instances = new ArrayList();
        }
        
        
        protected abstract O createObject(Instance i);
        
        
        @Override
        protected void addObject(O object, long objectSize, long objectRetainedSize) {
            super.addObject(object, objectSize, objectRetainedSize);
            instances.add(object.getInstance());
        }
        
        @Override
        public Iterator<O> getObjectsIterator() {
            return new Iterator<O>() {
                private final Iterator<Instance> i = instances.iterator();
                @Override public boolean hasNext() { return i.hasNext(); }
                @Override public O next() { return createObject(i.next()); }
            };
        }
        
    }
    
}

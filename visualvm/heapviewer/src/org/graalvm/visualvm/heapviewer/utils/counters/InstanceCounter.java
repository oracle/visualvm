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
package org.graalvm.visualvm.heapviewer.utils.counters;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;

/**
 *
 * @author Jiri Sedlacek
 */
public final class InstanceCounter {
    
    private int size;
    
    private int nullCount;    
    
    private final long[] keys;
    private final int[] counts;
    
    
    public InstanceCounter(int bufferSize) {
        bufferSize = (bufferSize * 9) / 8;
        keys = new long[bufferSize];
        counts = new int[bufferSize];
    }
    
    
    public void count(Instance ikey) {
        if (ikey == null) {
            nullCount++;
        } else {
            long key = ikey.getInstanceId();           
            
            int idx = hash(key);
            int index = idx;

            while (keys[index] != key && counts[index] != 0) {
                index = incIndex(index);
                if (index == idx) throw new RuntimeException("Full when counting " + ikey);
            }

            if (counts[index] == 0) {
                keys[index] = key;
                size++;
            }

            counts[index]++;
        }
    }
    
    public Iterator iterator() {
        return new Iterator();
    }
    
    public int size() {
        return size + (nullCount > 0 ? 1 : 0);
    }
    
    
    private int hash(long key) {
        return (int)Math.abs(key % keys.length);
    }
    
    private int incIndex(int index) {
        return ++index < keys.length ? index : 0;
    }
    
    
    public static final class Record {
        
        private long value;
        private int count;
        
        private Record() {}
        
        public Instance getInstance(Heap heap) {
            return value == -1 ? null : heap.getInstanceByID(value);
        }
        
        public int getCount() {
            return count;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Record)) return false;
            return value == ((Record)o).value;
        }
        
    }
    
    
    public final class Iterator implements java.util.Iterator<Record> {
        
        private final boolean hasNullCount;
        
        private int index;
        private int count;
        
        private Iterator() {
            hasNullCount = nullCount > 0;
        }

        @Override
        public boolean hasNext() {
            return hasNullCount ? count <= size : count < size;
        }

        @Override
        public Record next() {
            Record entry = new Record();
            if (count == size) {
                entry.value = -1;
                entry.count = nullCount;
            } else {
                entry.count = counts[index];
                while (entry.count == 0) entry.count = counts[++index];
                entry.value = keys[index++];
            }
            
            count++;
            
            return entry;
        }
        
    }
    
}

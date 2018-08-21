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

/**
 *
 * @author Jiri Sedlacek
 */
public final class ByteCounter extends PrimitiveCounter {
    
    private int size;
    
    private final int[] counts;
    
    
    public ByteCounter() {
        counts = new int[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
    }
    
    
    public void count(byte key) {
        int index = key - Byte.MIN_VALUE;
        if (counts[index] == 0) size++;
        counts[index]++;
    }
    
    @Override
    public void count(String value) {
        count(Byte.parseByte(value));
    }
    
    @Override
    public Iterator iterator() {
        return new Iterator();
    }
    
    @Override
    public int size() {
        return size;
    }
    
    
    public static final class Record extends PrimitiveCounter.Record {
        
        private byte value;
        private int count;
        
        private Record() {}
        
        public byte getPrimitive() {
            return value;
        }
        
        @Override
        public String getValue() {
            return Byte.toString(value);
        }
        
        @Override
        public int getCount() {
            return count;
        }
        
        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Record)) return false;
            return value == ((Record)o).value;
        }
        
    }
    
    
    public final class Iterator implements java.util.Iterator<Record> {
        
        private int index;
        private int count;
        
        private Iterator() {
        }

        @Override
        public boolean hasNext() {
            return count < size;
        }

        @Override
        public Record next() {
            Record entry = new Record();
            
            entry.count = counts[index];
            while (entry.count == 0) entry.count = counts[++index];
            entry.value = (byte)(index++ + Byte.MIN_VALUE);
            
            count++;
            
            return entry;
        }
        
    }
    
}

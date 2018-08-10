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

import java.util.Iterator;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class PrimitiveCounter {
    
    public static PrimitiveCounter create(String type, int instancesCount) {
        switch (type) {
            case "boolean": // NOI18N
                return new BooleanCounter();
            case "char": // NOI18N
                return new CharCounter();
            case "byte": // NOI18N
                return new ByteCounter();
            case "short": // NOI18N
                return new ShortCounter();
            case "int": // NOI18N
                return new IntCounter(instancesCount);
            case "long": // NOI18N
                return new LongCounter(instancesCount);
            case "float": // NOI18N
                return new FloatCounter(instancesCount);
            case "double": // NOI18N
                return new DoubleCounter(instancesCount);
            default:
                throw new IllegalArgumentException("Unknown primitive type " + type); // NOI18N
        }
    }
    
    
    public abstract void count(String value);
    
    public abstract int size();
    
    public abstract Iterator<? extends Record> iterator();
    
    
    public static abstract class Record {
        
        public abstract String getValue();
        
        public abstract int getCount();
        
    }
    
}

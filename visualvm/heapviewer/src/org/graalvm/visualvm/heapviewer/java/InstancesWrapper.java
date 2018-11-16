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

package org.graalvm.visualvm.heapviewer.java;

import java.util.Iterator;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class InstancesWrapper {
    
    public abstract JavaClass getJavaClass();
    
    public abstract int getInstancesCount();
    
    public abstract Iterator<Instance> getInstancesIterator();
    
    
    public static abstract class Simple extends InstancesWrapper {
        
        private final JavaClass jclass;
        
        private final int instancesCount;
        
        
        public Simple(JavaClass jclass, int instancesCount) {
            this.jclass = jclass;
            this.instancesCount = instancesCount;
        }
        
        
        @Override
        public final JavaClass getJavaClass() {
            return jclass;
        }
        
        @Override
        public final int getInstancesCount() {
            return instancesCount;
        }
        
    }
    
}

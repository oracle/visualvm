/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Tomas Hurka
 */
class HprofGCRoot extends HprofObject implements GCRoot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Map<Integer,String> kindMap;

    static {
        kindMap = new HashMap<>();
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_UNKNOWN), GCRoot.UNKNOWN);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_JNI_GLOBAL), GCRoot.JNI_GLOBAL);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_JNI_LOCAL), GCRoot.JNI_LOCAL);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_JAVA_FRAME), GCRoot.JAVA_FRAME);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_NATIVE_STACK), GCRoot.NATIVE_STACK);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_STICKY_CLASS), GCRoot.STICKY_CLASS);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_THREAD_BLOCK), GCRoot.THREAD_BLOCK);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_MONITOR_USED), GCRoot.MONITOR_USED);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_THREAD_OBJECT), GCRoot.THREAD_OBJECT);
        // HPROF HEAP 1.0.3
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_INTERNED_STRING), GCRoot.INTERNED_STRING);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_FINALIZING), GCRoot.FINALIZING);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_DEBUGGER), GCRoot.DEBUGGER);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_REFERENCE_CLEANUP), GCRoot.REFERENCE_CLEANUP);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_VM_INTERNAL), GCRoot.VM_INTERNAL);
        kindMap.put(Integer.valueOf(HprofHeap.ROOT_JNI_MONITOR), GCRoot.JNI_MONITOR);
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofGCRoots roots;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofGCRoot(HprofGCRoots r, long offset) {
        super(offset);
        roots = r;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Instance getInstance() {
        return roots.heap.getInstanceByID(getInstanceId());
    }

    public String getKind() {
        int k = getHprofBuffer().get(fileOffset);

        return kindMap.get(Integer.valueOf(k & 0xff));
    }

    long getInstanceId() {
        return getHprofBuffer().getID(fileOffset + 1);
    }
    
    HprofByteBuffer getHprofBuffer() {
        return roots.heap.dumpBuffer;
    }
}

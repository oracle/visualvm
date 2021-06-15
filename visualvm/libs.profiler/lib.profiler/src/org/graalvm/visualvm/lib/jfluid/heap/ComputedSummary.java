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

/**
 *
 * @author Tomas Hurka
 */
class ComputedSummary implements HeapSummary {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final long bytes;
    private final long instances;
    private final long time;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    ComputedSummary(HprofHeap heap) {
        long bytesCount = 0;
        long instancesCount = 0;

        for (JavaClass jcls : heap.getAllClasses()) {
            instancesCount += jcls.getInstancesCount();
            bytesCount += jcls.getAllInstancesSize();
        }
        bytes = bytesCount;
        instances = instancesCount;
        long headerTime = heap.dumpBuffer.getTime();
        long tagTime = heap.getHeapTime() / 1000;
        time = headerTime + tagTime;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getTime() {
        return time;
    }

    public long getTotalAllocatedBytes() {
        return -1;
    }

    public long getTotalAllocatedInstances() {
        return -1;
    }

    public long getTotalLiveBytes() {
        return bytes;
    }

    public long getTotalLiveInstances() {
        return instances;
    }
}

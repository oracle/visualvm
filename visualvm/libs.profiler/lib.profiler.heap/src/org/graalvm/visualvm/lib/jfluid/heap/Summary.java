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
final class Summary implements HeapSummary {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    final int totalLiveBytes;
    final int totalLiveInstances;
    final long time;
    final long totalAllocatedBytes;
    final long totalAllocatedInstances;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    Summary(HprofByteBuffer dumpBuffer, long startOffset) {
        assert dumpBuffer.get(startOffset) == HprofHeap.HEAP_SUMMARY;
        dumpBuffer.getInt(startOffset + 1); // time
        dumpBuffer.getInt(startOffset + 1 + 4); // tag length
        totalLiveBytes = dumpBuffer.getInt(startOffset + 1 + 4 + 4);
        totalLiveInstances = dumpBuffer.getInt(startOffset + 1 + 4 + 4 + 4);
        totalAllocatedBytes = dumpBuffer.getLong(startOffset + 1 + 4 + 4 + 4 + 4);
        totalAllocatedInstances = dumpBuffer.getLong(startOffset + 1 + 4 + 4 + 4 + 4 + 8);
        time = dumpBuffer.getTime();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getTime() {
        return time;
    }

    public long getTotalAllocatedBytes() {
        return totalAllocatedBytes;
    }

    public long getTotalAllocatedInstances() {
        return totalAllocatedInstances;
    }

    public long getTotalLiveBytes() {
        return totalLiveBytes;
    }

    public long getTotalLiveInstances() {
        return totalLiveInstances;
    }
}

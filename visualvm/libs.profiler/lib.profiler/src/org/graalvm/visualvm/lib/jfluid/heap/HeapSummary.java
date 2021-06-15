/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * This is optional summary information. It contains summary heap data and
 * time of the heap dump.
 * @author Tomas Hurka
 */
public interface HeapSummary {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * the time when the memory heap dump occurred.
     * @return the time when the memory heap dump occurred in milliseconds since 0:00 GMT 1/1/1970
     */
    long getTime();

    /**
     * number of total bytes allocated on the heap during the run of JVM.
     * Returned only if this summary information is available in heap dump.
     * @return number of total allocated bytes on the heap during the run of JVM
     * or -1 if the information is not available in the heap dump.
     */
    long getTotalAllocatedBytes();

    /**
     * number of all instances allocated on the heap during the run of JVM.
     * Returned only if this summary information is available in heap dump.
     * @return number of instances allocated on the heap during the run of JVM
     * or -1 if the information is not available in the heap dump.
     */
    long getTotalAllocatedInstances();

    /**
     * number of total bytes allocated on the heap at the time of the heap dump.
     * If this summary information is not available in heap dump, it is computed
     * from the dump.
     * @return number of total allocated bytes in the heap
     */
    long getTotalLiveBytes();

    /**
     * total number of instances allocated on the heap at the time of the heap dump.
     * If this summary information is not available in heap dump, it is computed
     * from the dump.
     * @return number of total live instances in the heap
     */
    long getTotalLiveInstances();
}

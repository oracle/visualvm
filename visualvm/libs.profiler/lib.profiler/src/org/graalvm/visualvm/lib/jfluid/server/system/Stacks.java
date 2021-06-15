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

package org.graalvm.visualvm.lib.jfluid.server.system;


/**
 * Provides methods for accessing thread stacks contents.
 *
 * @author  Misha Dmitriev
 */
public class Stacks {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Returns the number of Java frames on the stack of the current thread */
    public static native int getCurrentJavaStackDepth(Thread thread);

    /**
     * stackDepth parameter is the maximum number of stack frames that can be sampled. Returns the actual number of
     * stack frames sampled.
     */
    public static native int getCurrentStackFrameIds(Thread thread, int stackDepth, int[] stackFrameIds);

    /**
     * For the given array of jmethodIds, returns the names of the respective methods as
     * (class name, method name and method signature) triplets.
     * All this symbolic information is returned as a single packed array of bytes (with each string in UTF8 format).
     * packedArrayOffsets is filled out with offsets of all of these strings.
     *
     * @param nMethods The number of methods, length of the methodIds array
     * @param methodIds An array of jMethodIds for which we need their names
     * @param packedArrayOffsets An array that, upon return from this method, will contain the indexes into the returned
     *        array
     * @return A packed array of bytes of triplets [class name, method name, method signature], packedArrayOffsets
     *         contains indexes into this array for individual items
     */
    public static native byte[] getMethodNamesForJMethodIds(int nMethods, int[] methodIds, int[] packedArrayOffsets);

    /**
     * Get information about the stacks of all live threads
     * @param threads used to return all threads
     * @param states used to return thread's states
     * @param frames used to return jMethodIds of frames of all threads
     */
    public static native void getAllStackTraces(Thread[][] threads, int[][] states, int[][][] frames);
    
    /** Clear the above stack frame buffer permanently. */
    public static native void clearNativeStackFrameBuffer();

    /**
     * Creates the internal, C-level stack frame buffer, used for intermediate storage of data obtained using
     * getCurrentStackFrameIds. Since just a single buffer is used, getCurrentStackFrameIds is obviously not
     * multithread-safe. The code that uses this stuff has to use a single lock - so far not a problem for memory
     * profiling where we use it, since normally it collects data for just every 10th object, thus the probability
     * of contention is not very high.
     */
    public static native void createNativeStackFrameBuffer(int sizeInFrames);

    /** Should be called at earliest possible time */
    public static void initialize() {
        // Doesn't do anything in this version
    }
}

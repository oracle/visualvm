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
 * Provides methods for accessing various information related to threads.
 *
 * @author  Misha Dmitriev
 */
public class Threads {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns all live Java threads in this JVM. If the number of threads fits into the threads array, it is reused
     * (unused elements are filled with nulls). Otherwise, or if threads is null, a new array is created.
     */
    public static native Thread[] getAllThreads(Thread[] threads);

    //----------------- Miscellaneous
    public static native String getJVMArguments();

    public static native String getJavaCommand();

    /** For each passed thread, stores its status as defined in CommonConstants, in the status array. threads may contain nulls. */
    public static native void getThreadsStatus(Thread[] threads, int[] status);

    /** Returns the total number of live Java threads. */
    public static native int getTotalNumberOfThreads();

    /** Should be called at earliest possible time */
    public static void initialize() {
        // Doesn't do anything in this version
    }

    /**
     * Records a given thread as a profiler's own thread, so that targetAppTreadsExist() does not treat it as a
     * target app thread. Note that the current implementation allows only one additional profiler thread; if this
     * is called more than once, only the latest thread is remembered.
     */
    public static native void recordAdditionalProfilerOwnThread(Thread specialThread);

    /**
     * Record profiler's own threads. If excludeSpecialThread is true, record all the Java threads currently existing
     * in this JVM, minus specialThread. Otherwise, record only the specialThread. Returns the number of recorded threads.
     */
    public static native int recordProfilerOwnThreads(boolean excludeSpecialThread, Thread specialThread);

    public static synchronized native void resumeTargetAppThreads(Thread excludedThread);

    public static synchronized native void suspendTargetAppThreads(Thread excludedThread);

    /**
     * Checks if any live target application threads still exist. A target application thread is any thread not recorded
     * previously by recordProfilerOwnThreads() or recordAdditionalProfilerOwnThread().
     */
    public static native boolean targetAppThreadsExist();

    public static void terminateTargetAppThreads() {
        terminateTargetAppThreads(new ThreadDeath());
    }

    public static native void terminateTargetAppThreads(Object exception);
}

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
 * Provides methods for obtaining various high-resolution system times. A
 * version for CVM
 *
 * @author Misha Dmitriev
 */
public class Timers {

    /**
     * Should be called at earliest possible time
     */
    public static void initialize() {
        getThreadCPUTimeInNanos();
    }


    /**
     * "counts" instead of nanoseconds in this method are for compatibility with
     * the previous versions of JFluid, that call a native method for system
     * timer, which, in turn, returns the result in sub-microsecond "counts" on
     * Windows.
     */
    public static native long getCurrentTimeInCounts();

    public static long getNoOfCountsInSecond() {
        return 1000000000;
    }


    public static native long getThreadCPUTimeInNanos();

    /**
     * Returns the approximate accumulated process CPU elapsed time in
     * nanoseconds. Note that the time is normalized to one processor.
     * This method returns <tt>-1</tt> if the collection elapsed
     * time is undefined for this collector.
     *
     * @return the approximate accumulated process CPU elapsed time in
     * nanoseconds.
     */
    public static long getProcessCpuTime() {
        return -1;
    }

    /**
     * WORKS ONLY ON UNIX, calls nanosleep(). On Solaris, this is more precise
     * than the built-in Thread.sleep() call implementation that, at least in
     * JDK 1.4.2, goes to select(3C). On Linux, it should be more precise, but
     * it turns out that nanosleep() in this OS, at least in version 7.3 that I
     * tested, has a resolution of at least 20ms. This seems to be a known
     * issue; hopefully they fix it in future.
     */
    public static native void osSleep(int ns);


    /**
     * This is relevant only on Solaris. By default, the resolution of the
     * thread local CPU timer is 10 ms. If we enable micro state accounting, it
     * enables significantly (but possibly at a price of some overhead). So I
     * turn it on only when thread CPU timestamps are really collected.
     */
    public static native void enableMicrostateAccounting(boolean v);
}

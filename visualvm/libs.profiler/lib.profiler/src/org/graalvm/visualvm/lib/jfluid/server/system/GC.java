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
 * Provides methods for monitoring GC activities.
 *
 * @author  Misha Dmitriev
 */
public class GC {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int OBSERVED_PERIODS = 10; // must match OBSERVED_PERIODS in GC.c

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static native int getCurrentGCEpoch();

    /**
     * Returns the following two numbers:
     * metrics[0] - (Sum GC pause time for the last 10 periods) / (Total execution time for the last 10 periods) * 1000
     * metrics[1] - last GC pause in microseconds
     */
    public static native void getGCRelativeTimeMetrics(long[] metrics);

    public static native void getGCStartFinishTimes(long[] start, long[] finish);

    public static native void activateGCEpochCounter(boolean activate);

    /** Should be called at earliest possible time */
    public static void initialize() {
        // Doesn't do anything in this version
    }

    /**
     * Returns true if two instances of class Object are adjacent in memory.
     * Used by our memory leak monitoring mechanism.
     * Doesn't work (always returns true) in vanilla JDK 1.5
     */
    public static native boolean objectsAdjacent(Object o1, Object o2);

    public static native void resetGCEpochCounter();

    public static native void runGC();
}

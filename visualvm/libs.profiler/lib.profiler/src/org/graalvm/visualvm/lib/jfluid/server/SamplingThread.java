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

package org.graalvm.visualvm.lib.jfluid.server;

import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.jfluid.server.system.Timers;


/**
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
abstract class SamplingThread extends Thread {
    //~ Static fields/initializers -------------------------------------------------------------------------------------------

    private static final boolean isSolaris = Platform.isSolaris();
    private static final boolean isLinux = Platform.isLinux();
    private static final boolean isUnix = isSolaris || isLinux;
    private static final int VIOLATION_THRESHOLD = 10;
    private static final boolean DEBUG = false;

    //~ Instance fields ------------------------------------------------------------------------------------------------------

    private volatile boolean terminated;
    private int count;
    private int samplingInterval;

    //~ Methods --------------------------------------------------------------------------------------------------------------

    SamplingThread(int interval) {
        ThreadInfo.addProfilerServerThread(this);
        setPriority(Thread.MAX_PRIORITY);
        setDaemon(true);
        samplingInterval = interval;
        if (isSolaris) {
            samplingInterval *= 1000000; // Convert into nanos - the Solaris hires timer resolution
        } else if (isLinux) {
            samplingInterval *= 1000; // Convert into microseconds - the Linux hires timer resolution
        }
    }

    public void run() {
        int adjustedSamplingInterval = samplingInterval;
        int upperBound = (samplingInterval * 5) / 4;
        int lowerBound = samplingInterval / 10;
        int violationCount = VIOLATION_THRESHOLD;

        long startTime = Timers.getCurrentTimeInCounts();

        while (!terminated) {
            if (!isUnix) {
                try {
                    Thread.sleep(samplingInterval);
                }  catch (InterruptedException ex) { /* Should not happen */
                }
            }  else { // Solaris and Linux

                long time = Timers.getCurrentTimeInCounts();
                // On Solaris, the resolution of Thread.sleep(), which boils down to the select(3C) system call, seems to be
                // around 20 ms. So we have to use our own call, which eventually calls nanosleep() and takes an argument in nanos.
                // On Linux (at least version 7.3 + patches, which I tried), nanosleep() seems to have a 20 ms resolution (or even
                // give 20 ms no matter what?), which is a documented bug (see 'man nanosleep'). Well, maybe it improves in future...
                Timers.osSleep(adjustedSamplingInterval);
                time = Timers.getCurrentTimeInCounts()  - time;

                if ((time > upperBound) && (adjustedSamplingInterval > lowerBound)) {
                    if (violationCount > 0) {
                        violationCount--;
                    } else {
                        adjustedSamplingInterval = (adjustedSamplingInterval * 95) / 100;
                        violationCount = VIOLATION_THRESHOLD;
                    }
                }
            }
            
            sample();

            if (DEBUG) {
                count++;
            }
        }

        if (DEBUG && isUnix) {
            long time = ((Timers.getCurrentTimeInCounts() - startTime) * 1000) / Timers.getNoOfCountsInSecond();
            System.out.println("JFluid sampling thread: elapsed time: " + time + " ms, avg interval: " + (((double) time) / count) + "ms, adjusted interval: " + adjustedSamplingInterval + " OS units"); // NOI18N
        }
        ThreadInfo.removeProfilerServerThread(this);
    }

    abstract void sample();

    void terminate() {
        terminated = true;

        try {
            Thread.sleep(100);
        }  catch (InterruptedException ex) { /* Should not happen */
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.server;

import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.server.system.Timers;


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
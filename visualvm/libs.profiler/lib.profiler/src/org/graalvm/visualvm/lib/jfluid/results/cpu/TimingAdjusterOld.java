/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import java.lang.ref.WeakReference;


/**
 * A utility class to perform time measurements adjustments
 * @author Jaroslav Bachorik
 */
public class TimingAdjusterOld {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static TimingAdjusterOld instance;
    private static WeakReference lastStatusRef;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private InstrTimingData timingData;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private TimingAdjusterOld(InstrTimingData timing) {
        timingData = timing;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized TimingAdjusterOld getDefault() {
        if (instance == null) {
            instance = new TimingAdjusterOld(InstrTimingData.DEFAULT);
        }

        return instance;
    }

    public static synchronized TimingAdjusterOld getInstance(final ProfilingSessionStatus status) {
        if (instance != null) {
            ProfilingSessionStatus lastStatus = (lastStatusRef != null) ? (ProfilingSessionStatus) lastStatusRef.get() : null;

            if (((lastStatus != null) && !lastStatus.equals(status)) || (lastStatus == null)) {
                lastStatusRef = new WeakReference(status);

                switch (status.currentInstrType) {
                    case CommonConstants.INSTR_RECURSIVE_FULL: {
                        instance = new TimingAdjusterOld(getFullInstrTimingData(status));

                        break;
                    }
                    case CommonConstants.INSTR_RECURSIVE_SAMPLED: {
                        instance = new TimingAdjusterOld(getSampledInstrTimingData(status));

                        break;
                    }
                }
            }
        } else {
            lastStatusRef = new WeakReference(status);

            switch (status.currentInstrType) {
                case CommonConstants.INSTR_RECURSIVE_FULL: {
                    instance = new TimingAdjusterOld(getFullInstrTimingData(status));

                    break;
                }
                case CommonConstants.INSTR_RECURSIVE_SAMPLED: {
                    instance = new TimingAdjusterOld(getSampledInstrTimingData(status));

                    break;
                }
            }
        }

        return instance;
    }

    public final double adjustTime(long time, int incomingInv, int outgoingInv, boolean secondTimestamp) {
        if (timingData == null) {
            return (double) time;
        }

        if (timingData.methodEntryExitCallTime0 == 0) {
            if (secondTimestamp) {
                return (double) time / timingData.timerCountsInSecond1;
            } else {
                return (double) time / timingData.timerCountsInSecond0;
            }
        }

        if (secondTimestamp) {
            return (((double) time - (incomingInv * timingData.methodEntryExitInnerTime1)
                    - (outgoingInv * timingData.methodEntryExitOuterTime1)) * 1000000) / timingData.timerCountsInSecond1;
        } else {
            return (((double) time - (incomingInv * timingData.methodEntryExitInnerTime0)
                    - (outgoingInv * timingData.methodEntryExitOuterTime0)) * 1000000) / timingData.timerCountsInSecond0;
        }
    }

    public final double delta(int incomingInv, int outgoingInv, boolean secondTimestamp) {
        if (timingData == null  || timingData.methodEntryExitCallTime0 == 0) {
            return 0d;
        }

        double adjusted = 0;

        if (secondTimestamp) {
            adjusted = ((((double) incomingInv * timingData.methodEntryExitInnerTime1)
                        + (outgoingInv * timingData.methodEntryExitOuterTime1)) * 1000000) / timingData.timerCountsInSecond1;
        } else {
            adjusted = ((((double) incomingInv * timingData.methodEntryExitInnerTime0)
                        + (outgoingInv * timingData.methodEntryExitOuterTime0)) * 1000000) / timingData.timerCountsInSecond0;
        }

        if (adjusted < 0) {
            adjusted = 0;
        }

        return adjusted;
    }

    public InstrTimingData getInstrTimingData() {
        InstrTimingData data = getFullInstrTimingData(lastStatusRef != null ? (ProfilingSessionStatus)lastStatusRef.get() : null);
        return data != null ? (InstrTimingData)data.clone() : (InstrTimingData)InstrTimingData.DEFAULT.clone();
    }

    private static InstrTimingData getFullInstrTimingData(ProfilingSessionStatus status) {
        if (status == null) {
            return null;
        }
        InstrTimingData timingData = new InstrTimingData();

        // We use the following "safety margins" to artificially decrease the time spent in instrumentation.
        // That's because it looks like due to various CPU level speed optimizations the execution time for some code with
        // added instrumentation may be pretty much equal to the execution time of the instrumentation itself. The profiled
        // code, esp. if it's just one or two lines, is effectively executed in parallel with instrumentation.
        // Furthermore, highly optimizing CPU plus possibly smth. like rounding errors may occasionally result in the
        // measured time that is even *smaller* than the execution time of the instrumentation. This will result in
        // *negative* cleansed times presented to the user.
        // This safety margin is rough, but probably most reliable way to deal with these issues. The value has been
        // chosen experimentally, and seems to result in reasonable compromise between data accuracy and no occurrences
        // of zero time results.
        double safetyMargin0 = 0.88;
        double safetyMargin1 = 0.88;
        double safetyMargin2 = 0.88;

        if (status.collectingTwoTimeStamps()) {
            // Note that all methodEntryExitXXX times are measured in status in absolute timer counts.
            // For usage in factor-out operations, we need to convert those that are used with thread CPU time values
            // into thread CPU time units.
            timingData.methodEntryExitCallTime0 = status.methodEntryExitCallTime[2] * safetyMargin2;
            timingData.methodEntryExitCallTime1 = (status.methodEntryExitCallTime[3] * status.timerCountsInSecond[1]) / status.timerCountsInSecond[0] * safetyMargin2;
            timingData.methodEntryExitInnerTime0 = status.methodEntryExitInnerTime[2] * safetyMargin2;
            timingData.methodEntryExitOuterTime0 = status.methodEntryExitOuterTime[2] * safetyMargin2;
            timingData.methodEntryExitInnerTime1 = (status.methodEntryExitInnerTime[3] * status.timerCountsInSecond[1]) / status.timerCountsInSecond[0] * safetyMargin2;
            timingData.methodEntryExitOuterTime1 = (status.methodEntryExitOuterTime[3] * status.timerCountsInSecond[1]) / status.timerCountsInSecond[0] * safetyMargin2;
            timingData.timerCountsInSecond0 = status.timerCountsInSecond[0];
            timingData.timerCountsInSecond1 = status.timerCountsInSecond[1];
        } else {
            if (status.absoluteTimerOn) {
                timingData.methodEntryExitCallTime0 = status.methodEntryExitCallTime[0] * safetyMargin0;
                timingData.methodEntryExitInnerTime0 = status.methodEntryExitInnerTime[0] * safetyMargin0;
                timingData.methodEntryExitOuterTime0 = status.methodEntryExitOuterTime[0] * safetyMargin0;
                timingData.timerCountsInSecond0 = status.timerCountsInSecond[0];
            } else {
                // Thread CPU only timer used. On Windows (and till NB 7.0 also Linux), with extremely low
                // (10 ms or even worse) resolution of
                // this timer, it doesn't make sense to use cleansing - it introduces negative figures for short-running
                // methods, for which I didn't find any graceful way to deal with.
                // Also, cleansing is needed to make results sensible for short-running methods - but here for such methods
                // they are already non-sensible, with 0 as a most typical result.
                String targetMachineOSName = status.targetMachineOSName;

                if (Platform.isWindows(targetMachineOSName) && status.targetJDKVersionString.equals(Platform.JDK_16_STRING)) {
                    safetyMargin1 = 0;
                }

                timingData.methodEntryExitCallTime0 = (status.methodEntryExitCallTime[1] * status.timerCountsInSecond[1]) / status.timerCountsInSecond[0] * safetyMargin1;
                timingData.methodEntryExitInnerTime0 = (status.methodEntryExitInnerTime[1] * status.timerCountsInSecond[1]) / status.timerCountsInSecond[0] * safetyMargin1;
                timingData.methodEntryExitOuterTime0 = (status.methodEntryExitOuterTime[1] * status.timerCountsInSecond[1]) / status.timerCountsInSecond[0] * safetyMargin1;
                timingData.timerCountsInSecond0 = status.timerCountsInSecond[1];
            }
        }

        return timingData;
    }

    private static InstrTimingData getSampledInstrTimingData(ProfilingSessionStatus status) {
        if (status == null) {
            return null;
        }
        
        InstrTimingData timingData = new InstrTimingData();

        double entryExitTimeInAbsCounts = status.methodEntryExitCallTime[4];
        double entryExitTimeInThreadCPUCounts = entryExitTimeInAbsCounts / status.timerCountsInSecond[0] * status.timerCountsInSecond[1];

        if (status.collectingTwoTimeStamps()) {
            timingData.methodEntryExitCallTime0 = entryExitTimeInAbsCounts;
            timingData.methodEntryExitCallTime1 = entryExitTimeInThreadCPUCounts;
            timingData.methodEntryExitInnerTime0 = entryExitTimeInAbsCounts;
            timingData.methodEntryExitOuterTime0 = 0;
            timingData.methodEntryExitInnerTime1 = entryExitTimeInThreadCPUCounts;
            timingData.methodEntryExitOuterTime1 = 0;
            timingData.timerCountsInSecond0 = status.timerCountsInSecond[0];
            timingData.timerCountsInSecond1 = status.timerCountsInSecond[1];
        } else {
            if (status.absoluteTimerOn) {
                timingData.methodEntryExitCallTime0 = entryExitTimeInAbsCounts;
                timingData.methodEntryExitInnerTime0 = entryExitTimeInAbsCounts;
                timingData.methodEntryExitOuterTime0 = 0;
                timingData.timerCountsInSecond0 = status.timerCountsInSecond[0];
            } else {
                timingData.methodEntryExitCallTime0 = entryExitTimeInThreadCPUCounts;
                timingData.methodEntryExitInnerTime0 = entryExitTimeInThreadCPUCounts;
                timingData.methodEntryExitOuterTime0 = 0;
                timingData.timerCountsInSecond0 = status.timerCountsInSecond[1];
            }
        }

        return timingData;
    }
}

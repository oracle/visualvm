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

package org.graalvm.visualvm.lib.jfluid.results.cpu.cct;

import java.util.HashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import org.graalvm.visualvm.lib.jfluid.results.cpu.TimingAdjusterOld;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.marking.MarkAwareNodeProcessorPlugin;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TimeCollector extends MarkAwareNodeProcessorPlugin {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class TimingData {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        int inCalls;
        int outCalls;
        long netTime0;
        long netTime1;
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Map timing;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MarkTimer
     */
    public TimeCollector() {
        this.timing = new HashMap();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized long getNetTime0(Mark mark) {
        if (isReset()) {
            return 0;
        }

        TimingData currentTiming = (TimingData) timing.get(mark);
        long time = (currentTiming != null)
                    ? (long) TimingAdjusterOld.getDefault()
                                              .adjustTime(currentTiming.netTime0, currentTiming.inCalls, currentTiming.outCalls,
                                                          false) : 0;

        return (time > 0) ? time : 0;
    }

    public synchronized long getNetTime1(Mark mark) {
        if (isReset()) {
            return 0;
        }

        TimingData currentTiming = (TimingData) timing.get(mark);
        long time = (currentTiming != null)
                    ? (long) TimingAdjusterOld.getDefault()
                                              .adjustTime(currentTiming.netTime1, currentTiming.inCalls, currentTiming.outCalls,
                                                          true) : 0;

        return (time > 0) ? time : 0;
    }

    @Override
    public void onStart() {
        super.onStart();
        timing.clear();
    }

    @Override
    public void onStop() {
        if (isReset()) {
            this.timing = new HashMap();
        }
        super.onStop();
    }

    @Override
    public void onNode(MethodCPUCCTNode node) {
        if (isReset()) {
            return;
        }

        Mark mark = getCurrentMark();
        Mark parentMark = getParentMark();

        if (mark != null) {
            TimingData data = (TimingData) timing.get(mark);

            if (data == null) {
                data = new TimingData();
                timing.put(mark, data);
            }

            data.inCalls += node.getNCalls();
            data.netTime0 += node.getNetTime0();
            data.netTime1 += node.getNetTime1();
        }

        if (parentMark != null) {
            TimingData parentData = (TimingData) timing.get(parentMark);

            if (parentData == null) {
                parentData = new TimingData();
                timing.put(parentMark, parentData);
            }

            parentData.outCalls += node.getNCalls();
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.results.cpu.cct;

import org.netbeans.lib.profiler.results.cpu.TimingAdjusterOld;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.results.cpu.marking.MarkBasedNodeVisitor;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TimeCollector extends MarkBasedNodeVisitor {
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
                    ? Math.round(TimingAdjusterOld.getDefault()
                                              .adjustTime(currentTiming.netTime0, currentTiming.inCalls, currentTiming.outCalls,
                                                          false)) : 0;

        return (time > 0) ? time : 0;
    }

    public synchronized long getNetTime1(Mark mark) {
        if (isReset()) {
            return 0;
        }

        TimingData currentTiming = (TimingData) timing.get(mark);
        long time = (currentTiming != null)
                    ? Math.round(TimingAdjusterOld.getDefault()
                                              .adjustTime(currentTiming.netTime1, currentTiming.inCalls, currentTiming.outCalls,
                                                          true)) : 0;

        return (time > 0) ? time : 0;
    }

    public void beforeWalk() {
        super.beforeWalk();
        timing.clear();
    }

    public void endTrans() {
        if (isReset()) {
            this.timing = new HashMap();
        }

        super.endTrans();
    }

    public void visit(final MethodCPUCCTNode node) {
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

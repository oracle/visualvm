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

package org.netbeans.lib.profiler.results.cpu;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is simply a container for instrumentation timing data that needs to be passed around between
 * a number of different objects when processing CPU profiling results.
 *
 * @author Misha Dmitriev
 */
public class InstrTimingData implements Cloneable {
    final public static InstrTimingData DEFAULT = new InstrTimingData();

    // Of these variables, the xxx0 ones are used when either only absolute or only thread CPU timer is used.
    // xxx0 and xxx1 together are used only when both timers are used.
    double methodEntryExitCallTime0 = 0;
    double methodEntryExitCallTime1 = 0;
    double methodEntryExitInnerTime0 = 0;
    double methodEntryExitInnerTime1 = 0;
    double methodEntryExitOuterTime0 = 0;
    double methodEntryExitOuterTime1 = 0;
    long timerCountsInSecond0 = 1000; // default is a millisecond timer granularity; will get replaced from the calibration data
    long timerCountsInSecond1 = 1000; // default is a millisecond timer granularity; will get replaced from the calibration data

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Logger.getLogger(InstrTimingData.class.getName()).log(Level.SEVERE, "Unable to clone " + InstrTimingData.class.getName(), e);
            return null;
        }
    }

    public InstrTimingData() {}

    //~ Methods ------------------------------------------------------------------------------------------------------------------



    public String toString() {
        String s1 = "callTime0 = " + methodEntryExitCallTime0 + ", innerTime0 = " + methodEntryExitInnerTime0
                    + ", outerTime0 = " // NOI18N
                    + methodEntryExitOuterTime0 + "\n" // NOI18N
                    + "callTime1 = " + methodEntryExitCallTime1 + ", innerTime1 = " + methodEntryExitInnerTime1
                    + ", outerTime1 = " // NOI18N
                    + methodEntryExitOuterTime1 + "\n" // NOI18N
                    + "countsInSec0 = " + timerCountsInSecond0 + ", countsInSec1 = " + timerCountsInSecond1 + "\n"; // NOI18N

        return s1;
    }
}

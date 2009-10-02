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

package org.netbeans.lib.profiler.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Contains the calibration information obtained for CPU instrumentation used for profiling.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class CalibrationDataResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // The following is the same stuff that we have in ProfilingSessionStatus
    private double[] methodEntryExitCallTime;
    private double[] methodEntryExitInnerTime;
    private double[] methodEntryExitOuterTime;
    private long[] timerCountsInSecond; // This is always of length 2

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CalibrationDataResponse(double[] callTime, double[] innerTime, double[] outerTime, long[] timerCountsInSecond) {
        super(true, CALIBRATION_DATA);
        this.methodEntryExitCallTime = callTime;
        this.methodEntryExitInnerTime = innerTime;
        this.methodEntryExitOuterTime = outerTime;
        this.timerCountsInSecond = timerCountsInSecond;
    }

    // Custom serialization support
    CalibrationDataResponse() {
        super(true, CALIBRATION_DATA);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public double[] getMethodEntryExitCallTime() {
        return methodEntryExitCallTime;
    }

    public double[] getMethodEntryExitInnerTime() {
        return methodEntryExitInnerTime;
    }

    public double[] getMethodEntryExitOuterTime() {
        return methodEntryExitOuterTime;
    }

    public long[] getTimerCountsInSecond() {
        return timerCountsInSecond;
    }

    // For debugging
    public String toString() {
        return "CalibrationDataResponse, " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        int len = in.readInt();
        methodEntryExitCallTime = new double[len];
        methodEntryExitInnerTime = new double[len];
        methodEntryExitOuterTime = new double[len];

        for (int i = 0; i < len; i++) {
            methodEntryExitCallTime[i] = in.readDouble();
        }

        for (int i = 0; i < len; i++) {
            methodEntryExitInnerTime[i] = in.readDouble();
        }

        for (int i = 0; i < len; i++) {
            methodEntryExitOuterTime[i] = in.readDouble();
        }

        timerCountsInSecond = new long[2];
        timerCountsInSecond[0] = in.readLong();
        timerCountsInSecond[1] = in.readLong();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        int len = methodEntryExitCallTime.length;
        out.writeInt(len);

        for (int i = 0; i < len; i++) {
            out.writeDouble(methodEntryExitCallTime[i]);
        }

        for (int i = 0; i < len; i++) {
            out.writeDouble(methodEntryExitInnerTime[i]);
        }

        for (int i = 0; i < len; i++) {
            out.writeDouble(methodEntryExitOuterTime[i]);
        }

        out.writeLong(timerCountsInSecond[0]);
        out.writeLong(timerCountsInSecond[1]);
    }
}

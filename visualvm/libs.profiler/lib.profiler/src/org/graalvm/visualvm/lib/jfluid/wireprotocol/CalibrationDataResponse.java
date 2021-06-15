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

package org.graalvm.visualvm.lib.jfluid.wireprotocol;

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

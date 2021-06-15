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
 * Response containing instrumentation- and profiling-related statistics - most of the data that is presented if one
 * invokes Profile | Get internal statistics command in the tool.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class InternalStatsResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public double averageHotswappingTime;
    public double clientDataProcTime;
    public double clientInstrTime;
    public double maxHotswappingTime;
    public double methodEntryExitCallTime0;
    public double methodEntryExitCallTime1;
    public double methodEntryExitCallTime2;
    public double minHotswappingTime;
    public double totalHotswappingTime;
    public int nClassLoads;
    public int nEmptyInstrMethodGroupResponses;
    public int nFirstMethodInvocations;
    public int nNonEmptyInstrMethodGroupResponses;
    public int nSingleMethodInstrMethodGroupResponses;

    // Fields made public as an exception, to avoid too many accessors
    public int nTotalInstrMethods;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * We don't use a normal constructor with parameters here, since there are too many parameters to pass.
     * Instead we use public data fields.
     */
    public InternalStatsResponse() {
        super(true, INTERNAL_STATS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // For debugging
    public String toString() {
        return "InternalStatsResponse, " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        nTotalInstrMethods = in.readInt();
        nClassLoads = in.readInt();
        nFirstMethodInvocations = in.readInt();
        nNonEmptyInstrMethodGroupResponses = in.readInt();
        nEmptyInstrMethodGroupResponses = in.readInt();
        nSingleMethodInstrMethodGroupResponses = in.readInt();
        clientInstrTime = in.readDouble();
        clientDataProcTime = in.readDouble();
        totalHotswappingTime = in.readDouble();
        averageHotswappingTime = in.readDouble();
        minHotswappingTime = in.readDouble();
        maxHotswappingTime = in.readDouble();
        methodEntryExitCallTime0 = in.readDouble();
        methodEntryExitCallTime1 = in.readDouble();
        methodEntryExitCallTime2 = in.readDouble();
    }

    // Custom serialization support
    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(nTotalInstrMethods);
        out.writeInt(nClassLoads);
        out.writeInt(nFirstMethodInvocations);
        out.writeInt(nNonEmptyInstrMethodGroupResponses);
        out.writeInt(nEmptyInstrMethodGroupResponses);
        out.writeInt(nSingleMethodInstrMethodGroupResponses);
        out.writeDouble(clientInstrTime);
        out.writeDouble(clientDataProcTime);
        out.writeDouble(totalHotswappingTime);
        out.writeDouble(averageHotswappingTime);
        out.writeDouble(minHotswappingTime);
        out.writeDouble(maxHotswappingTime);
        out.writeDouble(methodEntryExitCallTime0);
        out.writeDouble(methodEntryExitCallTime1);
        out.writeDouble(methodEntryExitCallTime2);
    }
}

/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * This command, sent by the client, contains the instrumentation parameters (settings) that
 * can be changed once instrumentation is active and profiling is going on.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Tomas Hurka
 */
public class SetChangeableInstrParamsCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private boolean runGCOnGetResultsInMemoryProfiling;
    private boolean sleepTrackingEnabled;
    private boolean waitTrackingEnabled;
    private boolean threadsSamplingEnabled;
    private boolean lockContentionMonitoringEnabled;
    private int nProfiledThreadsLimit;
    private int maxStringLength;
    private int stackDepthLimit;
    private int objAllocStackSamplingDepth;
    private int objAllocStackSamplingInterval;
    private int samplingInterval;
    private int threadsSamplingFrequency;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SetChangeableInstrParamsCommand(boolean lockContentionMonitoringEnabled, int nProfiledThreadsLimit,
                                           int maxStringLength, int stackDepthLimit,
                                           int samplingInterval, int objAllocStackSamplingInterval,
                                           int objAllocStackSamplingDepth, boolean runGCOnGetResults,
                                           boolean waitTrackingEnabled, boolean sleepTrackingEnabled,
                                           boolean threadsSamplingEnabled, int threadsSamplingFrequency) {
        super(SET_CHANGEABLE_INSTR_PARAMS);
        this.lockContentionMonitoringEnabled = lockContentionMonitoringEnabled;
        this.nProfiledThreadsLimit = nProfiledThreadsLimit;
        this.maxStringLength = maxStringLength;
        this.stackDepthLimit = stackDepthLimit;
        this.samplingInterval = samplingInterval;
        this.threadsSamplingFrequency = threadsSamplingFrequency;
        this.objAllocStackSamplingInterval = objAllocStackSamplingInterval;
        this.objAllocStackSamplingDepth = objAllocStackSamplingDepth;
        this.runGCOnGetResultsInMemoryProfiling = runGCOnGetResults;
        this.waitTrackingEnabled = waitTrackingEnabled;
        this.sleepTrackingEnabled = sleepTrackingEnabled;
        this.threadsSamplingEnabled = threadsSamplingEnabled;
    }

    // Custom serialization support
    SetChangeableInstrParamsCommand() {
        super(SET_CHANGEABLE_INSTR_PARAMS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isLockContentionMonitoringEnabled() {
        return lockContentionMonitoringEnabled;
    }

    public int getNProfiledThreadsLimit() {
        return nProfiledThreadsLimit;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public int getStackDepthLimit() {
        return stackDepthLimit;
    }

    public int getObjAllocStackSamplingDepth() {
        return objAllocStackSamplingDepth;
    }

    public int getObjAllocStackSamplingInterval() {
        return objAllocStackSamplingInterval;
    }

    public boolean getRunGCOnGetResultsInMemoryProfiling() {
        return runGCOnGetResultsInMemoryProfiling;
    }

    public int getSamplingInterval() {
        return samplingInterval;
    }

    public int getThreadsSamplingFrequency() {
        return threadsSamplingFrequency;
    }

    public boolean isSleepTrackingEnabled() {
        return sleepTrackingEnabled;
    }

    public boolean isWaitTrackingEnabled() {
        return waitTrackingEnabled;
    }

    public boolean isThreadsSamplingEnabled() {
        return threadsSamplingEnabled;
    }

    // For debugging
    public String toString() {
        return super.toString() + ", lockContentionMonitoringEnabled: " + lockContentionMonitoringEnabled // NOI18N
               + ", nProfiledThreadsLimit: " + nProfiledThreadsLimit // NOI18N
               + ", maxStringLength: " + maxStringLength // NOI18N
               + ", stackDepthLimit: " + stackDepthLimit // NOI18N
               + ", samplingInterval: " + samplingInterval // NOI18N
               + ", objAllocStackSamplingInterval: " + objAllocStackSamplingInterval // NOI18N
               + ", objAllocStackSamplingDepth: " + objAllocStackSamplingDepth // NOI18N
               + ", runGCOnGetResultsInMemoryProfiling: " + runGCOnGetResultsInMemoryProfiling // NOI18N
               + ", waitTrackingEnabled: " + waitTrackingEnabled // NOI18N
               + ", sleepTrackingEnabled: " + sleepTrackingEnabled // NOI18N
               + ", threadsSamplingEnabled: " + threadsSamplingEnabled // NOI18N
               + ", threadsSamplingFrequency: " + threadsSamplingFrequency; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        lockContentionMonitoringEnabled = in.readBoolean();
        nProfiledThreadsLimit = in.readInt();
        maxStringLength = in.readInt();
        stackDepthLimit = in.readInt();
        samplingInterval = in.readInt();
        objAllocStackSamplingInterval = in.readInt();
        objAllocStackSamplingDepth = in.readInt();
        runGCOnGetResultsInMemoryProfiling = in.readBoolean();
        waitTrackingEnabled = in.readBoolean();
        sleepTrackingEnabled = in.readBoolean();
        threadsSamplingEnabled = in.readBoolean();
        threadsSamplingFrequency = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(lockContentionMonitoringEnabled);
        out.writeInt(nProfiledThreadsLimit);
        out.writeInt(maxStringLength);
        out.writeInt(stackDepthLimit);
        out.writeInt(samplingInterval);
        out.writeInt(objAllocStackSamplingInterval);
        out.writeInt(objAllocStackSamplingDepth);
        out.writeBoolean(runGCOnGetResultsInMemoryProfiling);
        out.writeBoolean(waitTrackingEnabled);
        out.writeBoolean(sleepTrackingEnabled);
        out.writeBoolean(threadsSamplingEnabled);
        out.writeInt(threadsSamplingFrequency);
    }
}

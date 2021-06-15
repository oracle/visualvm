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
import java.util.Arrays;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 * This Response, issued by the back end, contains the current information about free and total memory available
 * for the target application.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class MonitoredNumbersResponse extends Response {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int FREE_MEMORY_IDX = 0;
    public static final int TOTAL_MEMORY_IDX = 1;
    public static final int USER_THREADS_IDX = 2;
    public static final int SYSTEM_THREADS_IDX = 3;
    public static final int SURVIVING_GENERATIONS_IDX = 4;
    public static final int GC_TIME_IDX = 5;
    public static final int GC_PAUSE_IDX = 6;
    public static final int LOADED_CLASSES_IDX = 7;
    public static final int CPU_TIME_IDX = 8;
    public static final int TIMESTAMP_IDX = 9;
    public static final int GENERAL_NUMBERS_SIZE = 10;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private long[] gcFinishs;
    private long[] gcStarts;
    private long[] generalNumbers = new long[GENERAL_NUMBERS_SIZE];
    private String[] newThreadClassNames;
    private int[] newThreadIds;
    private String[] newThreadNames;
    private long[] stateTimestamps = new long[10];
    private int[] threadIds = new int[10];
    private byte[] threadStates = new byte[100];
    private int nNewThreads;
    private int nThreadStates;
    private int nThreads;

    private int[] exactThreadIds;
    private byte[] exactThreadStates;
    private long[] exactTimeStamps;
    private int mode = CommonConstants.MODE_THREADS_NONE;
    private int serverState = CommonConstants.SERVER_RUNNING;
    private int serverProgress = CommonConstants.SERVER_PROGRESS_INDETERMINATE;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MonitoredNumbersResponse(long[] generalNumbers, int serverState, int serverProgress) {
        super(true, MONITORED_NUMBERS);
        this.generalNumbers = generalNumbers;
        this.nNewThreads = 0;
        this.serverState = serverState;
        this.serverProgress = serverProgress;
    }

    // Custom serialization support
    MonitoredNumbersResponse() {
        super(true, MONITORED_NUMBERS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setDataOnNewThreads(int nNewThreads, int[] newThreadIds, String[] newThreadNames, String[] newThreadClassNames) {
        this.nNewThreads = nNewThreads;
        this.newThreadIds = newThreadIds;
        this.newThreadNames = newThreadNames;
        this.newThreadClassNames = newThreadClassNames;
    }

    public void setDataOnThreads(int nThreads, int nThreadStates, int[] threadIds, long[] stateTimestamps, byte[] threadStates) {
        this.mode = CommonConstants.MODE_THREADS_SAMPLING;
        this.nThreads = nThreads;
        this.nThreadStates = nThreadStates;
        this.threadIds = threadIds;
        this.stateTimestamps = stateTimestamps;
        this.threadStates = threadStates;
    }

    public void setExplicitDataOnThreads(int[] explicitThreads, byte[] explicitStates, long[] explicitTimeStamps) {
        assert (explicitStates.length == explicitThreads.length && explicitThreads.length == explicitTimeStamps.length);
        this.mode = CommonConstants.MODE_THREADS_EXACT;
        this.exactThreadIds = explicitThreads;
        this.exactThreadStates = explicitStates;
        this.exactTimeStamps = explicitTimeStamps;
    }

    public long[] getGCFinishs() {
        return gcFinishs;
    }

    public long[] getGCStarts() {
        return gcStarts;
    }

    public void setGCstartFinishData(long[] start, long[] finish) {
        gcStarts = start;
        gcFinishs = finish;
    }

    public long[] getGeneralMonitoredNumbers() {
        return generalNumbers;
    }

    public int getNNewThreads() {
        return nNewThreads;
    }

    public int getNThreadStates() {
        return nThreadStates;
    }

    public int getNThreads() {
        return nThreads;
    }

    public String[] getNewThreadClassNames() {
        return newThreadClassNames;
    }

    public int[] getNewThreadIds() {
        return newThreadIds;
    }

    public String[] getNewThreadNames() {
        return newThreadNames;
    }

    public long[] getStateTimestamps() {
        return stateTimestamps;
    }

    public int[] getThreadIds() {
        return threadIds;
    }

    public byte[] getThreadStates() {
        return threadStates;
    }
  
    public int getThreadsDataMode(){
        return mode;
    }

    // sampling-based data, to be used in case mode=MODE_THREADS_EXACT
    public int[] getExactThreadIds() {
        return exactThreadIds;
    }
    public byte[] getExactThreadStates() {
        return exactThreadStates;
    }
    public long[] getExactStateTimestamps() {
        return exactTimeStamps;
    }
    
    public int getServerState() {
        return serverState;
    }

    public int getServerProgress() {
        return serverProgress;
    }

    // For debugging
    public String toString() {
        return super.toString() + ", mode=" + mode +     // NOI18N
                ", serverState=" + serverState           // NOI18N
                 + ", serverProgress=" + serverProgress; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        int arrSize;

        mode = in.readInt();
        for (int i = 0; i < generalNumbers.length; i++) {
            generalNumbers[i] = in.readLong();
        }

        if (mode == CommonConstants.MODE_THREADS_SAMPLING) {
            nThreads = in.readInt();
            nThreadStates = in.readInt();
            if (threadIds.length < nThreads) {
                threadIds = new int[nThreads];
            }
            if (stateTimestamps.length < nThreadStates) {
                stateTimestamps = new long[nThreadStates];
            }
            
            int len = nThreads * nThreadStates;
            
            if (threadStates.length < len) {
                threadStates = new byte[len];
            }
            for (int i = 0; i < nThreads; i++) {
                threadIds[i] = in.readInt();
            }
            for (int i = 0; i < nThreadStates; i++) {
                stateTimestamps[i] = in.readLong();
            }
            in.readFully(threadStates, 0, len);
        }  else if (mode == CommonConstants.MODE_THREADS_EXACT) {
            int exactLen = in.readInt();
            exactThreadIds = new int[exactLen];
            exactThreadStates = new byte[exactLen];
            exactTimeStamps = new long[exactLen];
            
            for (int i = 0; i < exactLen; i++) {
                exactThreadIds[i] = in.readInt();
                exactThreadStates[i] = in.readByte();
                exactTimeStamps[i] = in.readLong();
            }
        }

        nNewThreads = in.readInt();

        if (nNewThreads > 0) {
            if ((newThreadIds == null) || (newThreadIds.length < nNewThreads)) {
                newThreadIds = new int[nNewThreads];
                newThreadNames = new String[nNewThreads];
                newThreadClassNames = new String[nNewThreads];
            }

            for (int i = 0; i < nNewThreads; i++) {
                newThreadIds[i] = in.readInt();
                newThreadNames[i] = in.readUTF();
                newThreadClassNames[i] = in.readUTF();
            }
        }

        arrSize = in.readInt();
        gcStarts = new long[arrSize];

        for (int i = 0; i < arrSize; i++) {
            gcStarts[i] = in.readLong();
        }

        arrSize = in.readInt();
        gcFinishs = new long[arrSize];

        for (int i = 0; i < arrSize; i++) {
            gcFinishs[i] = in.readLong();
        }

        Arrays.sort(gcStarts);
        Arrays.sort(gcFinishs);

        serverState = in.readInt();
        serverProgress = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(mode);
        for (int i = 0; i < generalNumbers.length; i++) {
            out.writeLong(generalNumbers[i]);
        }

        if (mode == CommonConstants.MODE_THREADS_SAMPLING) {
            out.writeInt(nThreads);
            out.writeInt(nThreadStates);
            for (int i = 0; i < nThreads; i++) {
                out.writeInt(threadIds[i]);
            }
            for (int i = 0; i < nThreadStates; i++) {
                out.writeLong(stateTimestamps[i]);
            }
            int len = nThreads * nThreadStates;
            out.write(threadStates, 0, len);
        } else if (mode == CommonConstants.MODE_THREADS_EXACT) {
            out.writeInt(exactThreadStates.length);
            for (int i = 0; i < exactThreadIds.length; i++) {
                out.writeInt(exactThreadIds[i]);
                out.writeByte(exactThreadStates[i]);
                out.writeLong(exactTimeStamps[i]);
            }
        }

        if (nNewThreads == 0) {
            out.writeInt(0);
        } else {
            out.writeInt(nNewThreads);

            for (int i = 0; i < nNewThreads; i++) {
                out.writeInt(newThreadIds[i]);
                out.writeUTF(newThreadNames[i]);
                out.writeUTF(newThreadClassNames[i]);
            }
        }

        out.writeInt(gcStarts.length);

        for (int i = 0; i < gcStarts.length; i++) {
            out.writeLong(gcStarts[i]);
        }

        out.writeInt(gcFinishs.length);

        for (int i = 0; i < gcFinishs.length; i++) {
            out.writeLong(gcFinishs[i]);
        }

        out.writeInt(serverState);
        out.writeInt(serverProgress);
    }
}

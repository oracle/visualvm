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
import java.util.Arrays;


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
    public static final int TIMESTAMP_IDX = 8;
    public static final int GENERAL_NUMBERS_SIZE = 9;

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

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MonitoredNumbersResponse(long[] generalNumbers) {
        super(true, MONITORED_NUMBERS);
        this.generalNumbers = generalNumbers;
        this.nNewThreads = 0;
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
        this.nThreads = nThreads;
        this.nThreadStates = nThreadStates;
        this.threadIds = threadIds;
        this.stateTimestamps = stateTimestamps;
        this.threadStates = threadStates;
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

    // For debugging
    public String toString() {
        return "MonitoredNumbersResponse, " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        int arrSize;

        for (int i = 0; i < generalNumbers.length; i++) {
            generalNumbers[i] = in.readLong();
        }

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
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        for (int i = 0; i < generalNumbers.length; i++) {
            out.writeLong(generalNumbers[i]);
        }

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
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.results.monitor;

import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.results.DataManager;


/**
 * A class that holds telemetry data about the target VM for a single profiling
 * session. It consumes/processes data obtained from the server via the
 * MonitoredData class. A listener is provided for those who want to be notified
 * about newly arrived data.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class VMTelemetryDataManager extends DataManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public long[] freeMemory;
    public long[] gcFinishs;
    public long[] gcStarts;
    public long[] lastGCPauseInMS;
    public long[] loadedClassesCount;
    public long[] nSurvivingGenerations;
    public long[] nSystemThreads;
    public long[] nTotalThreads;
    public long[] nUserThreads;
    public long[] relativeGCTimeInPerMil;
    public long[] timeStamps;
    public long[] totalMemory;
    public long[] usedMemory;
    public long maxHeapSize = Long.MAX_VALUE; // value of Xmx, constant within one profiling session

    // --- Data storage ----------------------------------------------------------
    private MonitoredData lastData = null; // last data processed

    // --- Arrays extending policy -----------------------------------------------
    private int arrayBufferSize;
    private int currentArraysSize;
    private int currentGCArraysSize;
    private int gcItemCount;
    private int itemCount;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------

    /**
     * Creates a new instance of VMTelemetryDataManager
     */
    public VMTelemetryDataManager() {
        this(50);
    }

    /**
     * Creates a new instance of VMTelemetryDataManager
     */
    public VMTelemetryDataManager(int arrayBufferSize) {
        this.arrayBufferSize = arrayBufferSize;
        reset();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized void setArrayBufferSize(int arrayBufferSize) {
        this.arrayBufferSize = arrayBufferSize;
    }

    public synchronized int getArrayBufferSize() {
        return arrayBufferSize;
    }

    public synchronized int getGCItemCount() {
        return gcItemCount;
    }

    // --- Getters / setters -----------------------------------------------------
    public synchronized int getItemCount() {
        return itemCount;
    }

    public synchronized MonitoredData getLastData() {
        return lastData;
    }

    // --- Public runtime API ----------------------------------------------------
    public synchronized void processData(MonitoredData data) {
        addValuesInternal(data.getTimestamp(), data.getFreeMemory(), data.getTotalMemory(), data.getNUserThreads(),
                          data.getNSystemThreads(), data.getNSurvivingGenerations(), data.getRelativeGCTimeInPerMil(),
                          data.getLastGCPauseInMS(), data.getLoadedClassesCount(), data.getGCStarts(), data.getGCFinishs());
        lastData = data;
    }

    public synchronized void reset() {
        lastData = null;

        itemCount = 0;
        gcItemCount = 0;

        timeStamps = new long[arrayBufferSize];

        freeMemory = new long[arrayBufferSize];

        totalMemory = new long[arrayBufferSize];

        usedMemory = new long[arrayBufferSize];

        nUserThreads = new long[arrayBufferSize];

        nSystemThreads = new long[arrayBufferSize];

        nTotalThreads = new long[arrayBufferSize];

        nSurvivingGenerations = new long[arrayBufferSize];

        relativeGCTimeInPerMil = new long[arrayBufferSize];

        lastGCPauseInMS = new long[arrayBufferSize];

        loadedClassesCount = new long[arrayBufferSize];

        currentArraysSize = arrayBufferSize;
        currentGCArraysSize = arrayBufferSize;

        gcStarts = new long[arrayBufferSize];
        gcFinishs = new long[arrayBufferSize];

        fireDataReset();
    }

    // --- Data storage management -----------------------------------------------
    private void addValuesInternal(long timeStamp, long freeMemory, long totalMemory, long nUserThreads, long nSystemThreads,
                                   long nSurvivingGenerations, long relativeGCTimeInPerMil, long lastGCPauseInMS,
                                   long loadedClassesCount, long[] gcStarts, long[] gcFinishs) {
        checkArraysSize();

        this.timeStamps[itemCount] = timeStamp;
        this.freeMemory[itemCount] = freeMemory;
        this.totalMemory[itemCount] = totalMemory;
        this.usedMemory[itemCount] = totalMemory - freeMemory;
        this.nUserThreads[itemCount] = nUserThreads;
        this.nSystemThreads[itemCount] = nSystemThreads;
        this.nTotalThreads[itemCount] = nSystemThreads + nUserThreads;
        this.nSurvivingGenerations[itemCount] = nSurvivingGenerations;

        // TODO: should be one tenth (relativeGCTimeInPerMil / 10)
        this.relativeGCTimeInPerMil[itemCount] = relativeGCTimeInPerMil;
        this.lastGCPauseInMS[itemCount] = lastGCPauseInMS;
        this.loadedClassesCount[itemCount] = loadedClassesCount;

        itemCount++;

        int gcDataCount = Math.min(gcStarts.length, gcFinishs.length);

        for (int i = 0; i < gcDataCount; i++) {
            this.gcStarts[gcItemCount] = gcStarts[i];
            this.gcFinishs[gcItemCount] = gcFinishs[i];
            gcItemCount++;
            checkArraysSize();
        }

        fireDataChanged();
    }

    private void checkArraysSize() {
        // array extension is needed
        if (currentArraysSize == itemCount) {
            timeStamps = extendArray(timeStamps, arrayBufferSize);
            freeMemory = extendArray(freeMemory, arrayBufferSize);
            totalMemory = extendArray(totalMemory, arrayBufferSize);
            usedMemory = extendArray(usedMemory, arrayBufferSize);
            nUserThreads = extendArray(nUserThreads, arrayBufferSize);
            nSystemThreads = extendArray(nSystemThreads, arrayBufferSize);
            nTotalThreads = extendArray(nTotalThreads, arrayBufferSize);
            nSurvivingGenerations = extendArray(nSurvivingGenerations, arrayBufferSize);
            relativeGCTimeInPerMil = extendArray(relativeGCTimeInPerMil, arrayBufferSize);
            lastGCPauseInMS = extendArray(lastGCPauseInMS, arrayBufferSize);
            loadedClassesCount = extendArray(loadedClassesCount, arrayBufferSize);

            // update current array size
            currentArraysSize += arrayBufferSize;
        }

        if (currentGCArraysSize == gcItemCount) {
            gcStarts = extendArray(gcStarts, arrayBufferSize);
            gcFinishs = extendArray(gcFinishs, arrayBufferSize);

            currentGCArraysSize += arrayBufferSize;
        }
    }

    // extends 1-dimensional array
    private static long[] extendArray(long[] array, int extraLength) {
        int originalLength = array.length;
        long[] newArray = new long[originalLength + extraLength];
        System.arraycopy(array, 0, newArray, 0, originalLength);

        return newArray;
    }
}

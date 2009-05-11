/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package org.netbeans.lib.profiler.results.memory;

import org.netbeans.lib.profiler.results.DataManager;

/**
 *
 * @author Jiri Sedlacek
 */
public class ClassHistoryDataManager extends DataManager {

    private int trackedClassID;
    private String trackedClassName;

    public long[] timeStamps; // Timeline
    public int[] nTotalAllocObjects; // Objects Allocated
    public long[] totalAllocObjectsSize; // Bytes Allocated
    public int[] nTrackedLiveObjects; // Live Objects
    public long[] trackedLiveObjectsSize; // Live Bytes


    private int arrayBufferSize;
    private int currentArraysSize;
    private int itemCount;


    public ClassHistoryDataManager() {
        this(50);
    }

    public ClassHistoryDataManager(int arrayBufferSize) {
        this.arrayBufferSize = arrayBufferSize;
        reset();
    }


    public synchronized void setArrayBufferSize(int arrayBufferSize) {
        this.arrayBufferSize = arrayBufferSize;
    }

    public synchronized int getArrayBufferSize() {
        return arrayBufferSize;
    }

    public synchronized int getItemCount() {
        return itemCount;
    }

    public synchronized int getTrackedClassID() {
        return trackedClassID;
    }

    public synchronized String getTrackedClassName() {
        return trackedClassName;
    }

    public synchronized boolean isTrackingClass() {
        return trackedClassName != null;
    }


    public synchronized void setupClass(int trackedClassID, String trackedClassName) {
        reset();
        
        this.trackedClassID = trackedClassID;
        this.trackedClassName = trackedClassName;
    }

    public synchronized void resetClass() {
        reset();
    }

    public synchronized void processData(int[] nTotalAllocObjects,
                                         long[] totalAllocObjectsSize) {

        checkArraysSize();

        timeStamps[itemCount] = System.currentTimeMillis();
        this.nTotalAllocObjects[itemCount] = nTotalAllocObjects[trackedClassID];
        this.totalAllocObjectsSize[itemCount] = totalAllocObjectsSize[trackedClassID];

        itemCount++;

        fireDataChanged();

    }

    public synchronized void processData(int[] nTotalAllocObjects,
                                         int[] nTrackedLiveObjects,
                                         long[] trackedLiveObjectsSize) {

        checkArraysSize();

        timeStamps[itemCount] = System.currentTimeMillis();
        this.nTotalAllocObjects[itemCount] = nTotalAllocObjects[trackedClassID];
        this.nTrackedLiveObjects[itemCount] = nTrackedLiveObjects[trackedClassID];
        this.trackedLiveObjectsSize[itemCount] = trackedLiveObjectsSize[trackedClassID];

        itemCount++;

        fireDataChanged();

    }


    private void reset() {
        itemCount = 0;

        trackedClassID = -1;
        trackedClassName = null;

        timeStamps = new long[arrayBufferSize];
        nTotalAllocObjects = new int[arrayBufferSize];
        totalAllocObjectsSize = new long[arrayBufferSize];
        nTrackedLiveObjects = new int[arrayBufferSize];
        trackedLiveObjectsSize = new long[arrayBufferSize];

        currentArraysSize = arrayBufferSize;

        fireDataReset();
    }
    
    private void checkArraysSize() {
        // array extension is needed
        if (currentArraysSize == itemCount) {
            timeStamps = extendArray(timeStamps, arrayBufferSize);
            nTotalAllocObjects = extendArray(nTotalAllocObjects, arrayBufferSize);
            totalAllocObjectsSize = extendArray(totalAllocObjectsSize, arrayBufferSize);
            nTrackedLiveObjects = extendArray(nTrackedLiveObjects, arrayBufferSize);
            trackedLiveObjectsSize = extendArray(trackedLiveObjectsSize, arrayBufferSize);

            // update current array size
            currentArraysSize += arrayBufferSize;
        }
    }

    // extends 1-dimensional long array
    private static long[] extendArray(long[] array, int extraLength) {
        int originalLength = array.length;
        long[] newArray = new long[originalLength + extraLength];
        System.arraycopy(array, 0, newArray, 0, originalLength);

        return newArray;
    }

    // extends 1-dimensional int array
    private static int[] extendArray(int[] array, int extraLength) {
        int originalLength = array.length;
        int[] newArray = new int[originalLength + extraLength];
        System.arraycopy(array, 0, newArray, 0, originalLength);

        return newArray;
    }

}

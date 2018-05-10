/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

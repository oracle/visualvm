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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Class representing a difference between two snapshots,
 * diffing only values available in LivenessMemoryResultsSnapshot
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class LivenessMemoryResultsDiff extends LivenessMemoryResultsSnapshot {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final LivenessMemoryResultsSnapshot snapshot1;
    private final LivenessMemoryResultsSnapshot snapshot2;

    private float[] avgObjectAge;
    private String[] classNames;
    private int[] maxSurvGen;
    private int[] nTotalAllocObjects;

    //  private int[] objectsCounts;
    private long[] objectsSizePerClass;
    private long[] nTrackedAllocObjects;
    private int[] nTrackedLiveObjects;
    private long[] trackedLiveObjectsSize;
    private int nClasses;
    private long maxTrackedLiveObjectsSizeDiff;
    private long minTrackedLiveObjectsSizeDiff;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LivenessMemoryResultsDiff(LivenessMemoryResultsSnapshot snapshot1, LivenessMemoryResultsSnapshot snapshot2) {
        this.snapshot1 = snapshot1;
        this.snapshot2 = snapshot2;

        computeDiff(snapshot1, snapshot2);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public float[] getAvgObjectAge() {
        return avgObjectAge;
    }

    public long getBeginTime() {
        return -1;
    }

    public String getClassName(int classId) {
        return classNames[classId];
    }

    public String[] getClassNames() {
        return classNames;
    }

    public JMethodIdTable getJMethodIdTable() {
        return null;
    }

    public int[] getMaxSurvGen() {
        return maxSurvGen;
    }

    public long getMaxTrackedLiveObjectsSizeDiff() {
        return maxTrackedLiveObjectsSizeDiff;
    }

    public long getMaxValue() {
        return -1;
    }

    public long getMinTrackedLiveObjectsSizeDiff() {
        return minTrackedLiveObjectsSizeDiff;
    }

    // seems to be unused in LivenessMemoryResultsSnapshot
    public int getNAlloc() {
        return getNProfiledClasses();
    }

    public int getNInstrClasses() {
        return getNProfiledClasses();
    }

    public int getNProfiledClasses() {
        return nClasses;
    }

    public long getNTotalTracked() {
        return -1;
    }

    public long getNTotalTrackedBytes() {
        return -1;
    }

    //---
    public long[] getNTrackedAllocObjects() {
        return nTrackedAllocObjects;
    }

    public int getNTrackedItems() {
        return getNProfiledClasses();
    }

    public int[] getNTrackedLiveObjects() {
        return nTrackedLiveObjects;
    }

    public long[] getObjectsSizePerClass() {
        return objectsSizePerClass;
    }

    public long getTimeTaken() {
        return -1;
    }

    public long[] getTrackedLiveObjectsSize() {
        return trackedLiveObjectsSize;
    }

    public boolean containsStacks() {
        return false;
    }

    public PresoObjLivenessCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths) {
        PresoObjLivenessCCTNode node1 = snapshot1.createPresentationCCT(classId1(classId), dontShowZeroLiveObjAllocPaths);
        PresoObjLivenessCCTNode node2 = snapshot2.createPresentationCCT(classId2(classId), dontShowZeroLiveObjAllocPaths);
        return new DiffObjLivenessCCTNode(node1, node2);
    }

    public int[] getnTotalAllocObjects() {
        return nTotalAllocObjects;
    }

    public void readFromStream(DataInputStream in) throws IOException {
        throw new UnsupportedOperationException("Persistence not supported for snapshot comparison"); // NOI18N
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        throw new UnsupportedOperationException("Persistence not supported for snapshot comparison"); // NOI18N
    }

    protected PresoObjLivenessCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                         boolean dontShowZeroLiveObjAllocPaths) {
        PresoObjLivenessCCTNode node1 = snapshot1.createPresentationCCT(rootNode, classId1(classId), dontShowZeroLiveObjAllocPaths);
        PresoObjLivenessCCTNode node2 = snapshot2.createPresentationCCT(rootNode, classId2(classId), dontShowZeroLiveObjAllocPaths);
        return new DiffObjLivenessCCTNode(node1, node2);
    }
    
    private int classId1(int classId) {
        return classId(classId, snapshot1);
    }
    
    private int classId2(int classId) {
        return classId(classId, snapshot2);
    }
    
    private int classId(int classId, LivenessMemoryResultsSnapshot snapshot) {
        if (snapshot == null) return -1;
        
        String className = getClassName(classId);
        String[] classNames = snapshot.getClassNames();
        if (classNames == null) return -1;
        
        for (int i = 0; i < classNames.length; i++)
            if (classNames[i].equals(className)) return i;
        
        return -1;
    }

    private void computeDiff(LivenessMemoryResultsSnapshot snapshot1, LivenessMemoryResultsSnapshot snapshot2) {
        // number of classes in snapshots
        int s1nClasses = snapshot1.getNTrackedItems();
        int s2nClasses = snapshot2.getNTrackedItems();

        // temporary cache for creating diff
        HashMap<String, Integer> classNamesIdxMap = new HashMap(s1nClasses);
        ArrayList<Long> nTrackedAllocObjectsArr = new ArrayList(s1nClasses);
        ArrayList<Long> objectsSizePerClassArr = new ArrayList(s1nClasses);
        ArrayList<Integer> nTrackedLiveObjectsArr = new ArrayList(s1nClasses);
        ArrayList<Integer> maxSurvGenArr = new ArrayList(s1nClasses);
        ArrayList<Float> avgObjectAgeArr = new ArrayList(s1nClasses);
        ArrayList<Long> trackedLiveObjectsSizeArr = new ArrayList(s1nClasses);
        ArrayList<Integer> nTotalAllocObjectsArr = new ArrayList(s1nClasses);

        // fill the cache with negative values from snapshot1
        String[] s1ClassNames = snapshot1.getClassNames();
        long[] s1NTrackedAllocObjects = snapshot1.getNTrackedAllocObjects();
        long[] s1ObjectsSizePerClass = snapshot1.getObjectsSizePerClass();
        int[] s1NTrackedLiveObjects = snapshot1.getNTrackedLiveObjects();
        int[] s1MaxSurvGen = snapshot1.getMaxSurvGen();
        float[] s1AvgObjectAge = snapshot1.getAvgObjectAge();
        long[] s1TrackedLiveObjectsSize = snapshot1.getTrackedLiveObjectsSize();
        int[] s1NTotalAllocObjects = snapshot1.getnTotalAllocObjects();

        int idx = 0;

        for (int i = 0; i < s1nClasses; i++) {
//            if (s1NTotalAllocObjects[i] > 0) {
                Integer classIdx = classNamesIdxMap.get(s1ClassNames[i]);

                if (classIdx != null) { // duplicate classname
                    nTrackedAllocObjectsArr.set(classIdx, nTrackedAllocObjectsArr.get(classIdx) - s1NTrackedAllocObjects[i]);
                    objectsSizePerClassArr.set(classIdx, objectsSizePerClassArr.get(classIdx) - s1ObjectsSizePerClass[i]);
                    nTrackedLiveObjectsArr.set(classIdx, nTrackedLiveObjectsArr.get(classIdx) - s1NTrackedLiveObjects[i]);
                    maxSurvGenArr.set(classIdx, maxSurvGenArr.get(classIdx) - s1MaxSurvGen[i]);
                    avgObjectAgeArr.set(classIdx, avgObjectAgeArr.get(classIdx) - s1AvgObjectAge[i]);
                    trackedLiveObjectsSizeArr.set(classIdx, trackedLiveObjectsSizeArr.get(classIdx) - s1TrackedLiveObjectsSize[i]);
                    nTotalAllocObjectsArr.set(classIdx, nTotalAllocObjectsArr.get(classIdx) - s1NTotalAllocObjects[i]);
                } else {
                    classNamesIdxMap.put(s1ClassNames[i], idx++);
                    nTrackedAllocObjectsArr.add(-s1NTrackedAllocObjects[i]);
                    objectsSizePerClassArr.add(-s1ObjectsSizePerClass[i]);
                    nTrackedLiveObjectsArr.add(-s1NTrackedLiveObjects[i]);
                    maxSurvGenArr.add(-s1MaxSurvGen[i]);
                    avgObjectAgeArr.add(-s1AvgObjectAge[i]);
                    trackedLiveObjectsSizeArr.add(-s1TrackedLiveObjectsSize[i]);
                    nTotalAllocObjectsArr.add(-s1NTotalAllocObjects[i]);
                }
//            }
        }

        // create diff using values from snapshot2
        String[] s2ClassNames = snapshot2.getClassNames();
        long[] s2NTrackedAllocObjects = snapshot2.getNTrackedAllocObjects();
        long[] s2ObjectsSizePerClass = snapshot2.getObjectsSizePerClass();
        int[] s2NTrackedLiveObjects = snapshot2.getNTrackedLiveObjects();
        int[] s2MaxSurvGen = snapshot2.getMaxSurvGen();
        float[] s2AvgObjectAge = snapshot2.getAvgObjectAge();
        long[] s2TrackedLiveObjectsSize = snapshot2.getTrackedLiveObjectsSize();
        int[] s2NTotalAllocObjects = snapshot2.getnTotalAllocObjects();

        for (int i = 0; i < s2nClasses; i++) {
//            if (s2NTotalAllocObjects[i] > 0) {
                Integer classIdx = classNamesIdxMap.get(s2ClassNames[i]);

                if (classIdx != null) {
                    // class already present in snapshot1
                    nTrackedAllocObjectsArr.set(classIdx, nTrackedAllocObjectsArr.get(classIdx) + s2NTrackedAllocObjects[i]);
                    objectsSizePerClassArr.set(classIdx, objectsSizePerClassArr.get(classIdx) + s2ObjectsSizePerClass[i]);
                    nTrackedLiveObjectsArr.set(classIdx, nTrackedLiveObjectsArr.get(classIdx) + s2NTrackedLiveObjects[i]);
                    maxSurvGenArr.set(classIdx, maxSurvGenArr.get(classIdx) + s2MaxSurvGen[i]);
                    avgObjectAgeArr.set(classIdx, avgObjectAgeArr.get(classIdx) + s2AvgObjectAge[i]);
                    trackedLiveObjectsSizeArr.set(classIdx, trackedLiveObjectsSizeArr.get(classIdx) + s2TrackedLiveObjectsSize[i]);
                    nTotalAllocObjectsArr.set(classIdx, nTotalAllocObjectsArr.get(classIdx) + s2NTotalAllocObjects[i]);
                } else {
                    // class not present in snapshot1
                    classNamesIdxMap.put(s2ClassNames[i], nTrackedAllocObjectsArr.size());
                    nTrackedAllocObjectsArr.add(s2NTrackedAllocObjects[i]);
                    objectsSizePerClassArr.add(s2ObjectsSizePerClass[i]);
                    nTrackedLiveObjectsArr.add(s2NTrackedLiveObjects[i]);
                    maxSurvGenArr.add(s2MaxSurvGen[i]);
                    avgObjectAgeArr.add(s2AvgObjectAge[i]);
                    trackedLiveObjectsSizeArr.add(s2TrackedLiveObjectsSize[i]);
                    nTotalAllocObjectsArr.add(s2NTotalAllocObjects[i]);
                }
//            }
        }

        // move the diff to instance variables
        nClasses = classNamesIdxMap.size();
        classNames = new String[nClasses];
        nTrackedAllocObjects = new long[nClasses];
        objectsSizePerClass = new long[nClasses];
        nTrackedLiveObjects = new int[nClasses];
        maxSurvGen = new int[nClasses];
        avgObjectAge = new float[nClasses];
        trackedLiveObjectsSize = new long[nClasses];
        nTotalAllocObjects = new int[nClasses];
        minTrackedLiveObjectsSizeDiff = Integer.MAX_VALUE;
        maxTrackedLiveObjectsSizeDiff = Integer.MIN_VALUE;

        Iterator<Map.Entry<String, Integer>> classNamesIter = classNamesIdxMap.entrySet().iterator();
        int index = 0;

        while (classNamesIter.hasNext()) {
            Map.Entry<String, Integer> entry = classNamesIter.next();
            int classIndex = entry.getValue();

            classNames[index] = entry.getKey();
            nTrackedAllocObjects[index] = nTrackedAllocObjectsArr.get(classIndex);
            objectsSizePerClass[index] = objectsSizePerClassArr.get(classIndex);
            nTrackedLiveObjects[index] = nTrackedLiveObjectsArr.get(classIndex);
            maxSurvGen[index] = maxSurvGenArr.get(classIndex);
            avgObjectAge[index] = avgObjectAgeArr.get(classIndex);
            trackedLiveObjectsSize[index] = trackedLiveObjectsSizeArr.get(classIndex);
            nTotalAllocObjects[index] = nTotalAllocObjectsArr.get(classIndex);

            minTrackedLiveObjectsSizeDiff = Math.min(minTrackedLiveObjectsSizeDiff, trackedLiveObjectsSize[index]);
            maxTrackedLiveObjectsSizeDiff = Math.max(maxTrackedLiveObjectsSizeDiff, trackedLiveObjectsSize[index]);

            index++;
        }

        if ((minTrackedLiveObjectsSizeDiff > 0) && (maxTrackedLiveObjectsSizeDiff > 0)) {
            minTrackedLiveObjectsSizeDiff = 0;
        } else if ((minTrackedLiveObjectsSizeDiff < 0) && (maxTrackedLiveObjectsSizeDiff < 0)) {
            maxTrackedLiveObjectsSizeDiff = 0;
        }
    }
}

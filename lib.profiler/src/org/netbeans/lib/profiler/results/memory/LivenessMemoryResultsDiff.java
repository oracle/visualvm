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

package org.netbeans.lib.profiler.results.memory;

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

    private float[] avgObjectAge;
    private String[] classNames;
    private int[] maxSurvGen;
    private int[] nTotalAllocObjects;

    //  private int[] objectsCounts;
    //  private long[] objectsSizePerClass;
    private long[] nTrackedAllocObjects;
    private int[] nTrackedLiveObjects;
    private long[] trackedLiveObjectsSize;
    private int nClasses;
    private long maxTrackedLiveObjectsSizeDiff;
    private long minTrackedLiveObjectsSizeDiff;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LivenessMemoryResultsDiff(LivenessMemoryResultsSnapshot snapshot1, LivenessMemoryResultsSnapshot snapshot2) {
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
        return null;
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

    //---
    public long[] getObjectsSizePerClass() {
        return null;
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

    public PresoObjAllocCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths) {
        return null;
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

    protected PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                         boolean dontShowZeroLiveObjAllocPaths) {
        return null;
    }

    private void computeDiff(LivenessMemoryResultsSnapshot snapshot1, LivenessMemoryResultsSnapshot snapshot2) {
        // number of classes in snapshots
        int s1nClasses = snapshot1.getNTrackedItems();
        int s2nClasses = snapshot2.getNTrackedItems();

        // temporary cache for creating diff
        HashMap classNamesIdxMap = new HashMap(s1nClasses);
        ArrayList nTrackedAllocObjectsArr = new ArrayList(s1nClasses);
        ArrayList nTrackedLiveObjectsArr = new ArrayList(s1nClasses);
        ArrayList maxSurvGenArr = new ArrayList(s1nClasses);
        ArrayList avgObjectAgeArr = new ArrayList(s1nClasses);
        ArrayList trackedLiveObjectsSizeArr = new ArrayList(s1nClasses);
        ArrayList nTotalAllocObjectsArr = new ArrayList(s1nClasses);

        // fill the cache with negative values from snapshot1
        String[] s1ClassNames = snapshot1.getClassNames();
        long[] s1NTrackedAllocObjects = snapshot1.getNTrackedAllocObjects();
        int[] s1NTrackedLiveObjects = snapshot1.getNTrackedLiveObjects();
        int[] s1MaxSurvGen = snapshot1.getMaxSurvGen();
        float[] s1AvgObjectAge = snapshot1.getAvgObjectAge();
        long[] s1TrackedLiveObjectsSize = snapshot1.getTrackedLiveObjectsSize();
        int[] s1NTotalAllocObjects = snapshot1.getnTotalAllocObjects();

        int idx = 0;

        for (int i = 0; i < s1nClasses; i++) {
            if (s1NTotalAllocObjects[i] > 0) {
                Integer classIdx = (Integer) classNamesIdxMap.get(s1ClassNames[i]);

                if (classIdx != null) { // duplicate classname

                    int classIndex = classIdx.intValue();
                    long nTrackedAllocObjects = ((Long) nTrackedAllocObjectsArr.get(classIndex)).longValue();
                    int nTrackedLiveObjects = ((Integer) nTrackedLiveObjectsArr.get(classIndex)).intValue();
                    int maxSurvGen = ((Integer) maxSurvGenArr.get(classIndex)).intValue();
                    float avgObjectAge = ((Float) avgObjectAgeArr.get(classIndex)).floatValue();
                    long trackedLiveObjectsSize = ((Long) trackedLiveObjectsSizeArr.get(classIndex)).longValue();
                    int nTotalAllocObjects = ((Integer) nTotalAllocObjectsArr.get(classIndex)).intValue();

                    nTrackedAllocObjectsArr.set(classIndex, new Long(nTrackedAllocObjects - s1NTrackedAllocObjects[i]));
                    nTrackedLiveObjectsArr.set(classIndex, new Integer(nTrackedLiveObjects - s1NTrackedLiveObjects[i]));
                    maxSurvGenArr.set(classIndex, new Integer(maxSurvGen - s1MaxSurvGen[i]));
                    avgObjectAgeArr.set(classIndex, new Float(avgObjectAge - s1AvgObjectAge[i]));
                    trackedLiveObjectsSizeArr.set(classIndex, new Long(trackedLiveObjectsSize - s1TrackedLiveObjectsSize[i]));
                    nTotalAllocObjectsArr.set(classIndex, new Integer(nTotalAllocObjects - s1NTotalAllocObjects[i]));
                } else {
                    classNamesIdxMap.put(s1ClassNames[i], new Integer(idx));
                    nTrackedAllocObjectsArr.add(new Long(0 - s1NTrackedAllocObjects[i]));
                    nTrackedLiveObjectsArr.add(new Integer(0 - s1NTrackedLiveObjects[i]));
                    maxSurvGenArr.add(new Integer(0 - s1MaxSurvGen[i]));
                    avgObjectAgeArr.add(new Float(0 - s1AvgObjectAge[i]));
                    trackedLiveObjectsSizeArr.add(new Long(0 - s1TrackedLiveObjectsSize[i]));
                    nTotalAllocObjectsArr.add(new Integer(0 - s1NTotalAllocObjects[i]));
                    idx++;
                }
            }
        }

        // create diff using values from snapshot2
        String[] s2ClassNames = snapshot2.getClassNames();
        long[] s2NTrackedAllocObjects = snapshot2.getNTrackedAllocObjects();
        int[] s2NTrackedLiveObjects = snapshot2.getNTrackedLiveObjects();
        int[] s2MaxSurvGen = snapshot2.getMaxSurvGen();
        float[] s2AvgObjectAge = snapshot2.getAvgObjectAge();
        long[] s2TrackedLiveObjectsSize = snapshot2.getTrackedLiveObjectsSize();
        int[] s2NTotalAllocObjects = snapshot2.getnTotalAllocObjects();

        for (int i = 0; i < s2nClasses; i++) {
            String className = s2ClassNames[i];
            long nTrackedAllocObject = s2NTrackedAllocObjects[i];
            int nTrackedLiveObject = s2NTrackedLiveObjects[i];
            int maxSGen = s2MaxSurvGen[i];
            float avtOAge = s2AvgObjectAge[i];
            long trackedLiveObjectSize = s2TrackedLiveObjectsSize[i];
            int nTotalAllocObject = s2NTotalAllocObjects[i];

            if (s2NTotalAllocObjects[i] > 0) {
                Integer classIdx = (Integer) classNamesIdxMap.get(className);
                int classIndex;

                if (classIdx != null) {
                    // class already present in snapshot1
                    classIndex = classIdx.intValue();
                    nTrackedAllocObjectsArr.set(classIndex,
                                                new Long(((Long) nTrackedAllocObjectsArr.get(classIndex)).longValue()
                                                         + nTrackedAllocObject));
                    nTrackedLiveObjectsArr.set(classIndex,
                                               new Integer(((Integer) nTrackedLiveObjectsArr.get(classIndex)).intValue()
                                                           + nTrackedLiveObject));
                    maxSurvGenArr.set(classIndex, new Integer(((Integer) maxSurvGenArr.get(classIndex)).intValue() + maxSGen));
                    avgObjectAgeArr.set(classIndex, new Float(((Float) avgObjectAgeArr.get(classIndex)).floatValue() + avtOAge));
                    trackedLiveObjectsSizeArr.set(classIndex,
                                                  new Long(((Long) trackedLiveObjectsSizeArr.get(classIndex)).longValue()
                                                           + trackedLiveObjectSize));
                    nTotalAllocObjectsArr.set(classIndex,
                                              new Integer(((Integer) nTotalAllocObjectsArr.get(classIndex)).intValue()
                                                          + nTotalAllocObject));
                } else {
                    // class not present in snapshot1
                    classNamesIdxMap.put(className, new Integer(nTrackedAllocObjectsArr.size()));
                    nTrackedAllocObjectsArr.add(new Long(nTrackedAllocObject));
                    nTrackedLiveObjectsArr.add(new Integer(nTrackedLiveObject));
                    maxSurvGenArr.add(new Integer(maxSGen));
                    avgObjectAgeArr.add(new Float(avtOAge));
                    trackedLiveObjectsSizeArr.add(new Long(trackedLiveObjectSize));
                    nTotalAllocObjectsArr.add(new Integer(nTotalAllocObject));
                }
            }
        }

        // move the diff to instance variables
        nClasses = classNamesIdxMap.size();
        classNames = new String[nClasses];
        nTrackedAllocObjects = new long[nClasses];
        nTrackedLiveObjects = new int[nClasses];
        maxSurvGen = new int[nClasses];
        avgObjectAge = new float[nClasses];
        trackedLiveObjectsSize = new long[nClasses];
        nTotalAllocObjects = new int[nClasses];
        minTrackedLiveObjectsSizeDiff = Integer.MAX_VALUE;
        maxTrackedLiveObjectsSizeDiff = Integer.MIN_VALUE;

        Iterator classNamesIter = classNamesIdxMap.entrySet().iterator();
        int index = 0;

        while (classNamesIter.hasNext()) {
            Map.Entry entry = (Map.Entry) classNamesIter.next();
            String className = (String) entry.getKey();
            int classIndex = ((Integer) entry.getValue()).intValue();

            classNames[index] = className;
            nTrackedAllocObjects[index] = ((Long) nTrackedAllocObjectsArr.get(classIndex)).longValue();
            nTrackedLiveObjects[index] = ((Integer) nTrackedLiveObjectsArr.get(classIndex)).intValue();
            maxSurvGen[index] = ((Integer) maxSurvGenArr.get(classIndex)).intValue();
            avgObjectAge[index] = ((Float) avgObjectAgeArr.get(classIndex)).floatValue();
            trackedLiveObjectsSize[index] = ((Long) trackedLiveObjectsSizeArr.get(classIndex)).longValue();
            nTotalAllocObjects[index] = ((Integer) nTotalAllocObjectsArr.get(classIndex)).intValue();

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

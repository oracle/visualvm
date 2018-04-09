/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Class representing a difference between two snapshots,
 * diffing only values available in AllocMemoryResultsSnapshot
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class AllocMemoryResultsDiff extends AllocMemoryResultsSnapshot {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final AllocMemoryResultsSnapshot snapshot1;
    private final AllocMemoryResultsSnapshot snapshot2;
    
    private String[] classNames;
    private int[] objectsCounts;
    private long[] objectsSizePerClass;
    private int nClasses;
    private long maxObjectsSizePerClassDiff;
    private long minObjectsSizePerClassDiff;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AllocMemoryResultsDiff(AllocMemoryResultsSnapshot snapshot1, AllocMemoryResultsSnapshot snapshot2) {
        this.snapshot1 = snapshot1;
        this.snapshot2 = snapshot2;
        
        computeDiff(snapshot1, snapshot2);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

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

    public long getMaxObjectsSizePerClassDiff() {
        return maxObjectsSizePerClassDiff;
    }

    public long getMinObjectsSizePerClassDiff() {
        return minObjectsSizePerClassDiff;
    }

    public int getNProfiledClasses() {
        return nClasses;
    }

    public int[] getObjectsCounts() {
        return objectsCounts;
    }

    public long[] getObjectsSizePerClass() {
        return objectsSizePerClass;
    }

    public long getTimeTaken() {
        return -1;
    }

    public boolean containsStacks() {
        return snapshot1.containsStacks() && snapshot2.containsStacks();
    }

    public PresoObjAllocCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths) {
        PresoObjAllocCCTNode node1 = snapshot1.createPresentationCCT(classId1(classId), dontShowZeroLiveObjAllocPaths);
        PresoObjAllocCCTNode node2 = snapshot2.createPresentationCCT(classId2(classId), dontShowZeroLiveObjAllocPaths);
        return new DiffObjAllocCCTNode(node1, node2);
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
        PresoObjAllocCCTNode node1 = snapshot1.createPresentationCCT(rootNode, classId1(classId), dontShowZeroLiveObjAllocPaths);
        PresoObjAllocCCTNode node2 = snapshot2.createPresentationCCT(rootNode, classId2(classId), dontShowZeroLiveObjAllocPaths);
        return new DiffObjAllocCCTNode(node1, node2);
    }
    
    private int classId1(int classId) {
        return classId(classId, snapshot1);
    }
    
    private int classId2(int classId) {
        return classId(classId, snapshot2);
    }
    
    private int classId(int classId, AllocMemoryResultsSnapshot snapshot) {
        if (snapshot == null) return -1;
        
        String className = getClassName(classId);
        String[] classNames = snapshot.getClassNames();
        if (classNames == null) return -1;
        
        for (int i = 0; i < classNames.length; i++)
            if (classNames[i].equals(className)) return i;
        
        return -1;
    }

    private void computeDiff(AllocMemoryResultsSnapshot snapshot1, AllocMemoryResultsSnapshot snapshot2) {
        // must detect the minimum, same approach as in SnapshotAllocResultsPanel.fetchResultsFromSnapshot()
        int s1nClasses = Math.min(snapshot1.getNProfiledClasses(), snapshot1.getObjectsCounts().length);
        s1nClasses = Math.min(s1nClasses, snapshot1.getObjectsSizePerClass().length);

        int s2nClasses = Math.min(snapshot2.getNProfiledClasses(), snapshot2.getObjectsCounts().length);
        s2nClasses = Math.min(s2nClasses, snapshot2.getObjectsSizePerClass().length);

        // temporary cache for creating diff
        HashMap<String, Integer> classNamesIdxMap = new HashMap(s1nClasses);
        ArrayList<Integer> objCountsArr = new ArrayList(s1nClasses);
        ArrayList<Long> objSizesArr = new ArrayList(s1nClasses);

        // fill the cache with negative values from snapshot1
        String[] s1ClassNames = snapshot1.getClassNames();
        int[] s1ObjectsCount = snapshot1.getObjectsCounts();
        long[] s1ObjectsSizes = snapshot1.getObjectsSizePerClass();

        for (int i = 0; i < s1nClasses; i++) {
            Integer classIdx = classNamesIdxMap.get(s1ClassNames[i]);

            if (classIdx != null) { // duplicate classname - add objCountsArr and objSizesArr to original classname
                objCountsArr.set(classIdx, objCountsArr.get(classIdx) - s1ObjectsCount[i]);
                objSizesArr.set(classIdx, objSizesArr.get(classIdx) - s1ObjectsSizes[i]);
            } else {
                classNamesIdxMap.put(s1ClassNames[i], objCountsArr.size());
                objCountsArr.add(-s1ObjectsCount[i]);
                objSizesArr.add(-s1ObjectsSizes[i]);
            }
        }

        // create diff using values from snapshot2
        String[] s2ClassNames = snapshot2.getClassNames();
        int[] s2ObjectsCount = snapshot2.getObjectsCounts();
        long[] s2ObjectsSizes = snapshot2.getObjectsSizePerClass();

        for (int i = 0; i < s2nClasses; i++) {
            Integer classIdx = classNamesIdxMap.get(s2ClassNames[i]);

            if (classIdx != null) {
                // class already present in snapshot1
//                if (objectsCount != 0 || objCountsArr.get(classIndex) != 0) { // Do not add classes not displayed in compared snapshots (zero instances number)
                objCountsArr.set(classIdx, objCountsArr.get(classIdx) + s2ObjectsCount[i]);
                objSizesArr.set(classIdx, objSizesArr.get(classIdx) + s2ObjectsSizes[i]);
//                } else {
//                    classNamesIdxMap.remove(s2ClassNames[i]); // Remove classname that should not be displayed
//                }
            } else {
                // class not present in snapshot1
//                if (objectsCount != 0) { // Do not add classes not displayed in compared snapshots (zero instances number)
                    classNamesIdxMap.put(s2ClassNames[i], objCountsArr.size());
                    objCountsArr.add(s2ObjectsCount[i]);
                    objSizesArr.add(s2ObjectsSizes[i]);
//                }
            }
        }

        // move the diff to instance variables
        nClasses = classNamesIdxMap.size();
        classNames = new String[nClasses];
        objectsCounts = new int[nClasses];
        objectsSizePerClass = new long[nClasses];
        minObjectsSizePerClassDiff = Long.MAX_VALUE;
        maxObjectsSizePerClassDiff = Long.MIN_VALUE;

        Iterator<Map.Entry<String, Integer>> classNamesIter = classNamesIdxMap.entrySet().iterator();
        int index = 0;

        while (classNamesIter.hasNext()) {
            Map.Entry<String, Integer> entry = classNamesIter.next();
            int classIndex = entry.getValue();

            classNames[index] = entry.getKey();
            objectsCounts[index] = objCountsArr.get(classIndex);
            objectsSizePerClass[index] = objSizesArr.get(classIndex);

            minObjectsSizePerClassDiff = Math.min(minObjectsSizePerClassDiff, objectsSizePerClass[index]);
            maxObjectsSizePerClassDiff = Math.max(maxObjectsSizePerClassDiff, objectsSizePerClass[index]);

            index++;
        }

        if ((minObjectsSizePerClassDiff > 0) && (maxObjectsSizePerClassDiff > 0)) {
            minObjectsSizePerClassDiff = 0;
        } else if ((minObjectsSizePerClassDiff < 0) && (maxObjectsSizePerClassDiff < 0)) {
            maxObjectsSizePerClassDiff = 0;
        }
    }
}

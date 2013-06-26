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

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.TransactionalSupport;
import org.netbeans.lib.profiler.results.BaseCallGraphBuilder;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;


/**
 * @author Misha Dmitirev
 * @author Jaroslav Bachorik
 */
public class MemoryCallGraphBuilder extends BaseCallGraphBuilder implements MemoryProfilingResultsListener, MemoryCCTProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * A hashtable specialized for mapping object ids to references to the respective CCT terminating nodes.
     * <p/>
     * Only used for updating of existing structures with new data.
     */
    private static class ObjIdToCCTNodeMap {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        /** [0-capacity] individual tracked instances Ids, also replicated at the server side -
         * bit combination opf epoch, classId & sequential counter */
        private long[] keys;

        /** [0-capacity] size in Bytes of tracked instance */
        private long[] objSize;

        /** [0-capacity] pointer to the "Term" node containing data - see RuntimeObjLivenessTermCCTNode */
        private RuntimeObjLivenessTermCCTNode[] values;
        private int capacity;
        private int k;
        private int nObjects;
        private int threshold; // nObjects - number of tracked objects
        private long a = 5700357409661599241L;
        private long lastRemovedObjSize;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ObjIdToCCTNodeMap() {
            init();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public long getLastRemovedObjSize() {
            return lastRemovedObjSize;
        }

        public RuntimeObjLivenessTermCCTNode getNode(long key) {
            int pos = hash(key);
            long keyAtPos = keys[pos];
            int iter = capacity >> 2;

            while ((keyAtPos != key) && (iter > 0)) { // -1 after a large num of iterations can happen only if "reset collectors" was performed
                pos = (pos + 1) % capacity;
                keyAtPos = keys[pos];
                iter--;
            }

            if (iter == 0) {
                return null;
            }

            keys[pos] = -1;

            RuntimeObjLivenessTermCCTNode ret = values[pos];
            values[pos] = null;
            lastRemovedObjSize = objSize[pos];
            nObjects--;

            return ret;
        }

        public void clear() {
            keys = null;
            values = null;
            init();
        }

        public void put(long key, RuntimeObjLivenessTermCCTNode value, long size) {
            if (nObjects > threshold) {
                rehash();
            }

            int pos = hash(key);

            while (keys[pos] != -1) {
                pos = (pos + 1) % capacity;
            }

            keys[pos] = key;
            values[pos] = value;
            objSize[pos] = size;
            nObjects++;
        }

        public int sizeInBytes() {
            return (keys.length * 8) + (values.length * 4) + (objSize.length * 8);
        }

        private void setThreshold() {
            threshold = (capacity * 3) / 4;
        }

        private int hash(long key) {
            return (int) ((key * a) >>> (64 - k));
        }

        private void init() {
            capacity = 1024;
            k = 10; // 2^k == capacity
            nObjects = 0;
            setThreshold();
            keys = new long[capacity];

            for (int i = 0; i < capacity; i++) {
                keys[i] = -1;
            }

            values = new RuntimeObjLivenessTermCCTNode[capacity];
            objSize = new long[capacity];
        }

        private void rehash() {
            long[] oldKeys = keys;
            RuntimeObjLivenessTermCCTNode[] oldValues = values;
            long[] oldObjSize = objSize;
            int oldCapacity = capacity;
            capacity = capacity * 2;
            k++;
            keys = new long[capacity];

            for (int i = 0; i < capacity; i++) {
                keys[i] = -1;
            }

            values = new RuntimeObjLivenessTermCCTNode[capacity];
            objSize = new long[capacity];

            for (int i = 0; i < oldCapacity; i++) {
                if (oldKeys[i] != -1) {
                    int pos = hash(oldKeys[i]);

                    while (keys[pos] != -1) {
                        pos = (pos + 1) % capacity;
                    }

                    keys[pos] = oldKeys[i];
                    values[pos] = oldValues[i];
                    objSize[pos] = oldObjSize[i];
                }
            }

            setThreshold();
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /** used to allow us to update the structures dynamically at runtime (maps internal objId to its corresponding
     * RuntimeObjLivenessTermNode - table of surviving generations (same class & same allocation path))
     */
    private ObjIdToCCTNodeMap objMap;
    private final TransactionalSupport transaction = new TransactionalSupport();

    /**
     * [0 - nProfiledClasses] index: classId, average object age for class
     */
    private float[] avgObjectAge;

    /**
     * [0 - nProfiledClasses] index: classId
     */
    private int[] maxSurvGen;

    /** [0 - nProfiledClasses] index: classId, contains tracked allocated instances # for this class */
    private long[] nTrackedAllocObjects;

    /** [0 - nProfiledClasses] index: classId,
     * contains tracked live instances # for this class - assumption: smaller than allocated, thus only int
     */
    private int[] nTrackedLiveObjects;
    private long[] objectsSizePerClass; // [0-nProfiledClasses] total size in bytes for tracked instances of this class
    private RuntimeMemoryCCTNode[] stacksForClasses; // [0-nProfiledClasses] class Id -> root of its allocation traces tree

    /**
     * [0 - nProfiledClasses] index: classId, if true, the class has been selected for not being profiled
     */
    private boolean[] unprofiledClass;

    /**
     * the latest GC, numbered from 0 (the first one)
     */
    private int currentEpoch;
    private int nProfiledClasses; // total number of profiled classes

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long[] getAllocObjectNumbers() {
        transaction.beginTrans(false);

        try {
            long[] res = new long[nProfiledClasses];
            System.arraycopy(objectsSizePerClass, 0, res, 0, res.length);

            return res;
        } finally {
            transaction.endTrans();
        }
    }

    // temporary methods for exposing builder's internals
    public int getCurrentEpoch() {
        transaction.beginTrans(false);

        try {
            return currentEpoch;
        } finally {
            transaction.endTrans();
        }
    }

    public MemoryCCTProvider.ObjectNumbersContainer getLivenessObjectNumbers() {
        transaction.beginTrans(false);

        try {
            if (getClient().getCurrentInstrType() != CommonConstants.INSTR_OBJECT_LIVENESS) {
                throw new IllegalStateException("MemoryCallGraphBuilder must be running in TRACKING_LIVENESS mode in order to provide liveness statistics"); // NOI18N
            }

            // Note that for average ages and epoch tails to be realistic we have to have all dead object removed from our data.
            // That's currently more or less the case, since we call System.gc() before invoking "get results" when doing
            // Object Liveness Profiling.
            // However, it would be better if we had a native call to perform full GC, since System.gc() may be disabled easily.
            updateNumberOfClasses();
            calculateAverageObjectAges();
            calculateTotalNumberOfSurvGens();

            ObjectNumbersContainer res = new ObjectNumbersContainer(nTrackedAllocObjects, nTrackedLiveObjects,
                                                                    objectsSizePerClass, avgObjectAge, maxSurvGen,
                                                                    unprofiledClass, nProfiledClasses);

            return res;
        } finally {
            transaction.endTrans();
        }
    }

    public int getNProfiledClasses() {
        transaction.beginTrans(false);

        try {
            updateNumberOfClasses(); // performance hit is ~ 1ms
            return nProfiledClasses;
        } finally {
            transaction.endTrans();
        }
    }

    public long[] getObjectsSizePerClass() {
        transaction.beginTrans(false);

        try {
            return objectsSizePerClass;
        } finally {
            transaction.endTrans();
        }
    }

    public RuntimeMemoryCCTNode[] getStacksForClasses() {
        transaction.beginTrans(false);

        try {
            return stacksForClasses;
        } finally {
            transaction.endTrans();
        }
    }

    public void beginTrans(boolean mutable) {
        transaction.beginTrans(mutable);
    }

    public boolean classMarkedUnprofiled(int classId) {
        transaction.beginTrans(false);

        try {
            return unprofiledClass[classId];
        } finally {
            transaction.endTrans();
        }
    }

    public PresoObjAllocCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths)
                                               throws ClientUtils.TargetAppOrVMTerminated {
        transaction.beginTrans(false);

        try {
            PresoObjAllocCCTNode presNode = null;
            RuntimeMemoryCCTNode classNode = getClassNode(classId);
            String className = getClassName(classId);

            if ((classNode == null) || (className == null)) {
                return null;
            }

            switch (getClient().getCurrentInstrType()) {
                case CommonConstants.INSTR_OBJECT_LIVENESS: {
                    presNode = PresoObjLivenessCCTNode.createPresentationCCTFromVM(getClient(), classNode, className,
                                                                                   currentEpoch, dontShowZeroLiveObjAllocPaths);

                    break;
                }
                case CommonConstants.INSTR_OBJECT_ALLOCATIONS: {
                    presNode = PresoObjAllocCCTNode.createPresentationCCTFromVM(getClient(), classNode, className);

                    break;
                }
                default:throw new IllegalStateException("MemoryCallGraphBuilder runs in an illegal mode"); // NOI18N
            }

            return presNode;
        } finally {
            transaction.endTrans();
        }
    }

    public void endTrans() {
        transaction.endTrans();
    }

    public void markClassUnprofiled(int classId) {
        transaction.beginTrans(true);

        try {
            unprofiledClass[classId] = true;
        } finally {
            transaction.endTrans();
        }
    }

    public void onAllocStackTrace(char classId, long objSize, int[] methodIds) {
        RuntimeObjAllocTermCCTNode termNode = (RuntimeObjAllocTermCCTNode) processStackTrace(classId, methodIds, false);

        if (termNode != null) {
            termNode.updateForNewObject(objSize);
            objectsSizePerClass[classId] += objSize;
        }

        batchNotEmpty = true;
    }

    public void onGcPerformed(char classId, long objectId, int objEpoch) {
        if (currentEpoch < objEpoch) {
            currentEpoch = objEpoch;
        }

        RuntimeObjLivenessTermCCTNode termNode = objMap.getNode(objectId);
        long objSize = objMap.getLastRemovedObjSize();

        if (termNode == null) {
            return; // Can happen if "reset collectors" previously performed
        }

        termNode.updateForRemovedObject(objSize);
        termNode.removeLiveObjectForEpoch(objEpoch);
        nTrackedLiveObjects[classId]--;
        objectsSizePerClass[classId] -= objSize;

        batchNotEmpty = true;
    }

    public void onLivenessStackTrace(char classId, long objectId, int objEpoch, long objSize, int[] methodIds) {
        if (getClient().getCurrentInstrType() != CommonConstants.INSTR_OBJECT_LIVENESS) {
            return; // ignore liveness events when not in appropriate mode
        }

        if (currentEpoch < objEpoch) {
            currentEpoch = objEpoch;
        }

        try {
            RuntimeObjLivenessTermCCTNode termNode = (RuntimeObjLivenessTermCCTNode) processStackTrace(classId, methodIds, true);

            if (termNode != null) {
                termNode.updateForNewObject(objSize);
                termNode.addLiveObjectForEpoch(objEpoch);
                objMap.put(objectId, termNode, objSize);

                nTrackedAllocObjects[classId]++;
                nTrackedLiveObjects[classId]++;
                objectsSizePerClass[classId] += objSize;
            }
        } catch (OutOfMemoryError e) {
            ProfilerLogger.warning("OOME, resetting collectors!!!"); // NOI18N // TODO
            reset();
        }

        batchNotEmpty = true;
    }

    @Override
    public void monitorEntry(int threadId, long timeStamp0, long timeStamp1, int monitorId) {
        // ignore
    }

    @Override
    public void monitorExit(int threadId, long timeStamp0, long timeStamp1, int monitorId) {
        // ignore
    }

    @Override
    public void newThread(int threadId, String threadName, String threadClassName) {
        // ignore
    }

    @Override
    public void newMonitor(int hash, String className) {
        // ignore
    }

    @Override
    public void timeAdjust(int threadId, long timeDiff0, long timeDiff1) {
        // ignore
    }
    
    public void updateInternals() {
        loadNamesForJMethodIds();
    }

    protected RuntimeCCTNode getAppRootNode() {
        return new RuntimeMemoryCCTNode();
    }

    protected void doBatchStart() {
        transaction.beginTrans(true);
        updateNumberOfClasses();
    }

    protected void doBatchStop() {
        transaction.endTrans();
    }

    protected void doReset() {
        transaction.beginTrans(true);

        try {
            if (stacksForClasses != null) {
                for (int i = 0; i < stacksForClasses.length; i++) {
                    stacksForClasses[i] = null;
                    objectsSizePerClass[i] = 0;
                }
            }

            if (objMap != null) {
                objMap.clear();
            }

            if (nTrackedAllocObjects != null) {
                for (int i = 0; i < nTrackedAllocObjects.length; i++) {
                    nTrackedAllocObjects[i] = 0;
                    objectsSizePerClass[i] = 0;
                }
            }

            if (nTrackedLiveObjects != null) {
                for (int i = 0; i < nTrackedLiveObjects.length; i++) {
                    nTrackedLiveObjects[i] = 0;
                }
            }

            if (objectsSizePerClass != null) {
                for (int i = 0; i < objectsSizePerClass.length; i++) {
                    objectsSizePerClass[i] = 0;
                }
            }
        } finally {
            transaction.endTrans();
        }
    }

    protected void doShutdown() {
        // #204978: 'loadNamesForJMethodIds()' must be called on SHUTDOWN_INITIATED command
    }

    protected void doStartup(ProfilerClient profilerClient) {
        objMap = new ObjIdToCCTNodeMap();
        currentEpoch = 0;

        profilerClient.registerMemoryCCTProvider(this);

        nProfiledClasses = 0;
        stacksForClasses = null;
        objectsSizePerClass = null;
        nTrackedAllocObjects = null;
        nTrackedLiveObjects = null;
        maxSurvGen = null;
        avgObjectAge = null;
        unprofiledClass = null;
        currentEpoch = -1;
    }

    private String getClassName(int classId) {
        status.beginTrans(false);

        try {
            return status.getClassNames()[classId];
        } finally {
            status.endTrans();
        }
    }

    private RuntimeMemoryCCTNode getClassNode(int classId) {
        return stacksForClasses[classId];
    }

    private boolean isInitialized() {
        return (unprofiledClass != null) && (stacksForClasses != null);
    }

    private RuntimeMemoryCCTNode getNewTerminalNode(int methodId, boolean live) {
        return live ? new RuntimeObjLivenessTermCCTNode(methodId) : new RuntimeObjAllocTermCCTNode(methodId);
    }

    private void calculateAverageObjectAges() {
        if (!isInitialized()) {
            return; // in the middle of initialization; don't recalculate
        }

        int nClasses = nProfiledClasses;

        avgObjectAge = new float[nClasses];

        for (int i = 0; i < nClasses; i++) {
            if (unprofiledClass[i]) {
                continue;
            }

            RuntimeMemoryCCTNode rootNode = stacksForClasses[i];

            if (rootNode == null) {
                continue;
            }

            float age = RuntimeObjLivenessTermCCTNode.calculateAvgObjectAgeForAllPaths(rootNode, currentEpoch);

            if (age < 0.0f) {
                age = 0.0f; // May happen after "Reset collectors"
            }

            avgObjectAge[i] = age;
        }
    }

    private void calculateTotalNumberOfSurvGens() {
        if (!isInitialized()) {
            return;
        }

        maxSurvGen = new int[nProfiledClasses];

        for (int i = 0; i < maxSurvGen.length; i++) {
            if (unprofiledClass[i]) {
                continue;
            }

            RuntimeMemoryCCTNode rootNode = stacksForClasses[i];

            if (rootNode == null) {
                continue;
            }

            maxSurvGen[i] = RuntimeObjLivenessTermCCTNode.calculateTotalNumberOfSurvGensForAllPaths(rootNode);
        }
    }

    //************************************************************

    /**********************************************************************************************
     * Private implementation
     **********************************************************************************************/
    private void loadNamesForJMethodIds() {
        // problem: unloaded classes in the meantime
        // unloaded classes need to be handled specifically - see ClassLoaderManager, JMethodIdTable
        transaction.beginTrans(false);

        try {
            PresoObjAllocCCTNode.getNamesForMethodIdsFromVM(getClient(), stacksForClasses);
        } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            ProfilerLogger.log(ex.getMessage()); /* No longer ignore silently */
        } finally {
            transaction.endTrans();
        }
    }

    /**
     * Given the classId and the array of methodIds of the stack trace for
     * the newly allocated object, update the reverse Calling Context Tree for this
     * class. Adds new nodes and/or increases allocated object counters/size in nodes. Returns the terminating
     * node in the resulting CCT branch.
     */
    private RuntimeMemoryCCTNode processStackTrace(char classId, int[] methodIds, boolean live) {
        if (classId >= stacksForClasses.length) {
            ProfilerLogger.severe("Received stack for non existent class Id: " + (int) classId + ", current length: " + stacksForClasses.length); // NOI18N
            updateNumberOfClasses();
            ProfilerLogger.severe("Received stack for non existent class Id: " + (int) classId
                                  + ", current length after updateNumberOfClasses: " // NOI18N
                                  + stacksForClasses.length);

            if (classId >= stacksForClasses.length) {
                return null;
            }
        }

        RuntimeMemoryCCTNode curNode = stacksForClasses[classId];
        RuntimeMemoryCCTNode parentNode = null;

        if (curNode == null) {
            curNode = new RuntimeMemoryCCTNode(0);
            stacksForClasses[classId] = curNode;
        }

        int depth = methodIds.length;
        int depthMinusOne = depth - 1;

        for (int i = 0; i < depth; i++) {
            int methodId = methodIds[i];
            parentNode = curNode;

            Object children = curNode.children;

            boolean found = false;

            if (children != null) {
                if (children instanceof RuntimeMemoryCCTNode) {
                    if (((RuntimeMemoryCCTNode) children).methodId == methodId) {
                        curNode = (RuntimeMemoryCCTNode) children;
                        found = true;
                    }
                } else {
                    RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) children;

                    for (int j = 0; j < ar.length; j++) {
                        if (ar[j].methodId == methodId) {
                            curNode = ar[j];
                            found = true;

                            break;
                        }
                    }
                }
            }

            if (!found) {
                // Appropriate subnode not found or there are no subnodes yet - create one.
                if (i < depthMinusOne) {
                    curNode = curNode.addNewChild(methodId); // Non-terminal node
                } else { // Terminal node - need to create a specialized one depending on the profiling type (obj alloc or obj liveness)

                    RuntimeMemoryCCTNode newNode = getNewTerminalNode(methodId, live);
                    curNode.attachNodeAsChild(newNode);
                    curNode = newNode;
                }
            }
        }

        // Now check if the curNode that we are going to return is actually not an instance of one of classes representing
        // "terminal nodes", like RuntimeObjAllocTermCCTNode or RuntimeObjLivenessTermCCTNode. Such nodes contain information
        // that is normally the same for the whole call chain - such as total number/size of objects of the given type allocated
        // by this call chain. However, it looks like in some cases (different threads?) it may happen that one complete call
        // chain may become a fragment of another, longer call chain. In that case we will neeed to have a "terminal" node in the middle
        // of the chain. Here we are checking for the case when first a longer chain is created, and then a shorter one that
        // matches a part of the longer one is found, and taking measures.
        if (curNode.getClass() == RuntimeMemoryCCTNode.class) {
            RuntimeMemoryCCTNode newNode = getNewTerminalNode(curNode.methodId, live);
            newNode.children = curNode.children;

            if (parentNode != null) {
                Object parChildren = parentNode.children;
                assert (parChildren != null); // parent will always have chilren

                if (parChildren instanceof RuntimeMemoryCCTNode) {
                    if (parChildren == curNode) {
                        parentNode.children = newNode;
                    }
                } else {
                    RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) parChildren;

                    for (int i = 0; i < ar.length; i++) {
                        if (ar[i] == curNode) {
                            ar[i] = newNode;

                            break;
                        }
                    }
                }
            } else {
                stacksForClasses[classId] = newNode;
            }

            curNode = newNode;
        }

        return curNode;
    }

    private void updateNumberOfClasses() {
        status.beginTrans(false);

        try {
            nProfiledClasses = status.getNInstrClasses();
        } finally {
            status.endTrans();
        }

        if ((stacksForClasses == null) || (stacksForClasses.length < nProfiledClasses)) {
            int newSize = (nProfiledClasses * 3) / 2;
            RuntimeMemoryCCTNode[] newStacks = new RuntimeMemoryCCTNode[newSize];

            if (stacksForClasses != null) {
                System.arraycopy(stacksForClasses, 0, newStacks, 0, stacksForClasses.length);
            }

            stacksForClasses = newStacks;

            long[] newObjSize = new long[newSize];

            if (objectsSizePerClass != null) {
                System.arraycopy(objectsSizePerClass, 0, newObjSize, 0, objectsSizePerClass.length);
            }

            objectsSizePerClass = newObjSize;
        }

        if (getClient().getCurrentInstrType() == CommonConstants.INSTR_OBJECT_LIVENESS) {
            if ((nTrackedLiveObjects == null) || (nTrackedLiveObjects.length < nProfiledClasses)) {
                int newSize = (nProfiledClasses * 3) / 2;
                int[] tmpIOldData = nTrackedLiveObjects;
                int[] tmpINewData = null;
                long[] tmpLOldData = nTrackedAllocObjects;
                long[] tmpLNewData = null;
                boolean[] tmpBOldData = unprofiledClass;
                boolean[] tmpBNewData = null;
                tmpINewData = new int[newSize];

                if (tmpIOldData != null) {
                    System.arraycopy(tmpIOldData, 0, tmpINewData, 0, tmpIOldData.length);
                }

                tmpLNewData = new long[newSize];

                if (tmpLOldData != null) {
                    System.arraycopy(tmpLOldData, 0, tmpLNewData, 0, tmpLOldData.length);
                }

                tmpBNewData = new boolean[newSize];

                if (tmpBOldData != null) {
                    System.arraycopy(tmpBOldData, 0, tmpBNewData, 0, tmpBOldData.length);
                }

                nTrackedLiveObjects = tmpINewData;
                nTrackedAllocObjects = tmpLNewData;
                unprofiledClass = tmpBNewData;

                /* Currently unused - avgObjectAge is created on demand only
                   float[][] tmpFOldData = new float[][]{avgObjectAge};
                   float[][] tmpFNewData = new float[1][];
                   for (int i = 0; i < 1; i++) {
                     tmpFNewData[i] = new float[newSize];
                     if (tmpFOldData[i] != null) System.arraycopy(tmpFOldData[i], 0, tmpFNewData[i], 0, tmpFOldData[i].length);
                   }
                   avgObjectAge = tmpFNewData[0]; */
            }
        }
    }
}

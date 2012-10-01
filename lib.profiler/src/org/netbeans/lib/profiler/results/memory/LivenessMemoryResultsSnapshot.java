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

import java.util.logging.Level;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils.TargetAppOrVMTerminated;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * Results snapshot for Allocations Memory Profiling.
 *
 * @author Ian Formanek
 */
public class LivenessMemoryResultsSnapshot extends MemoryResultsSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MEMORY_LIVENESS_MSG = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.memory.Bundle").getString("LivenessMemoryResultsSnapshot_MemoryLivenessMsg"); // NOI18N
                                                                                                                             // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /** [0 - nProfiledClasses] index: classId, average object age for class */
    private float[] avgObjectAge;

    /** [0 - nProfiledClasses] index: classId, contains tracked live instrances # for this class -
     * assumption: smaller than allocated, thus only int */
    private int[] maxSurvGen;
    private int[] nTotalAllocObjects;

    /** [0 - nProfiledClasses] index: classId, contains tracked allocated instrances # for this class */
    private long[] nTrackedAllocObjects;
    private int[] nTrackedLiveObjects;
    private long[] trackedLiveObjectsSize;
    private int currentEpoch;

    /** used to keep track of total allocations number, which is in turn needed to monitor
     * JFluid tool's used heap space */
    private int nInstrClasses;
    private int nTotalTracked;
    private int nTrackedItems;
    private long maxValue;
    private long nTotalTrackedBytes;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LivenessMemoryResultsSnapshot() {
    } // No-arg constructor needed for above serialization methods to work

    public LivenessMemoryResultsSnapshot(long beginTime, long timeTaken, MemoryCCTProvider provider, ProfilerClient client)
                                  throws TargetAppOrVMTerminated {
        super(beginTime, timeTaken, provider, client);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @return An average object age for each class
     */
    public float[] getAvgObjectAge() {
        return avgObjectAge;
    }

    /**
     * @return An array of maximum number of surviving generations for each class
     */
    public int[] getMaxSurvGen() {
        return maxSurvGen;
    }

    public long getMaxValue() {
        return maxValue;
    }

    /**
     * @return Number of classes
     */
    public int getNAlloc() {
        return nInstrClasses;
    }

    public int getNInstrClasses() {
        return nInstrClasses;
    }

    public long getNTotalTracked() {
        return nTotalTracked;
    }

    public long getNTotalTrackedBytes() {
        return nTotalTrackedBytes;
    }

    /**
     * @return An array of number of tracked allocations for each class
     */
    public long[] getNTrackedAllocObjects() {
        return nTrackedAllocObjects;
    }

    public int getNTrackedItems() {
        return nTrackedItems;
    }

    /**
     * @return An array of number of tracked live objects for each class
     */
    public int[] getNTrackedLiveObjects() {
        return nTrackedLiveObjects;
    }

    public long[] getTrackedLiveObjectsSize() {
        return trackedLiveObjectsSize;
    }

    public int[] getnTotalAllocObjects() {
        return nTotalAllocObjects;
    }

    public void performInit(ProfilerClient client, MemoryCCTProvider provider)
                     throws TargetAppOrVMTerminated {
        super.performInit(client, provider);
        
        MemoryCCTProvider.ObjectNumbersContainer onc = provider.getLivenessObjectNumbers();

        int len;
        len = onc.nTrackedAllocObjects.length;
        nTrackedAllocObjects = new long[len];
        System.arraycopy(onc.nTrackedAllocObjects, 0, nTrackedAllocObjects, 0, len);
        len = onc.nTrackedLiveObjects.length;
        nTrackedLiveObjects = new int[len];
        System.arraycopy(onc.nTrackedLiveObjects, 0, nTrackedLiveObjects, 0, len);
        len = onc.trackedLiveObjectsSize.length;
        trackedLiveObjectsSize = new long[len];
        System.arraycopy(onc.trackedLiveObjectsSize, 0, trackedLiveObjectsSize, 0, len);
        len = onc.avgObjectAge.length;
        avgObjectAge = new float[len];
        System.arraycopy(onc.avgObjectAge, 0, avgObjectAge, 0, len);
        len = onc.maxSurvGen.length;
        maxSurvGen = new int[len];
        System.arraycopy(onc.maxSurvGen, 0, maxSurvGen, 0, len);

        nInstrClasses = onc.nInstrClasses;

        int[] tmp = client.getAllocatedObjectsCountResults();
        len = tmp.length;
        nTotalAllocObjects = new int[len];
        System.arraycopy(tmp, 0, nTotalAllocObjects, 0, len);

        // Below is a bit of "defensive programming". Normally the sizes of arrays here should be same
        // except for nTotalAllocObjects, that is returned from the server, and may be shorter if some
        // instrumented classes have not propagated to the server yet.
        nTrackedItems = Math.min(nTrackedAllocObjects.length, nTrackedLiveObjects.length);
        nTrackedItems = Math.min(nTrackedItems, trackedLiveObjectsSize.length);
        nTrackedItems = Math.min(nTrackedItems, avgObjectAge.length);
        nTrackedItems = Math.min(nTrackedItems, maxSurvGen.length);
        nTrackedItems = Math.min(nTrackedItems, nInstrClasses);
        nTrackedItems = Math.min(nTrackedItems, nTotalAllocObjects.length);

        // Now if some classes are unprofiled, reflect that in nTotalAllocObjects
        //for (int i = 0; i < nTrackedAllocObjects.length; i++) {
        for (int i = 0; i < nTrackedItems; i++) {
            if (nTrackedAllocObjects[i] == -1) {
                nTotalAllocObjects[i] = 0;
            }
        }

        // Operations necessary for correct bar representation of results
        maxValue = 0;
        nTotalTrackedBytes = 0;
        nTotalTracked = 0;

        //for (int i = 0; i < trackedLiveObjectsSize.length; i++) {
        for (int i = 0; i < nTrackedItems; i++) {
            if (maxValue < trackedLiveObjectsSize[i]) {
                maxValue = trackedLiveObjectsSize[i];
            }

            nTotalTrackedBytes += trackedLiveObjectsSize[i];
            nTotalTracked += nTrackedLiveObjects[i];
        }

        currentEpoch = provider.getCurrentEpoch();
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);

        int len = in.readInt();
        nTrackedAllocObjects = new long[len];

        for (int i = 0; i < len; i++) {
            nTrackedAllocObjects[i] = in.readLong();
        }

        len = in.readInt();
        nTrackedLiveObjects = new int[len];

        for (int i = 0; i < len; i++) {
            nTrackedLiveObjects[i] = in.readInt();
        }

        len = in.readInt();
        maxSurvGen = new int[len];

        for (int i = 0; i < len; i++) {
            maxSurvGen[i] = in.readInt();
        }

        len = in.readInt();
        trackedLiveObjectsSize = new long[len];

        for (int i = 0; i < len; i++) {
            trackedLiveObjectsSize[i] = in.readLong();
        }

        len = in.readInt();
        avgObjectAge = new float[len];

        for (int i = 0; i < len; i++) {
            avgObjectAge[i] = in.readFloat();
        }

        nInstrClasses = in.readInt();

        len = in.readInt();
        nTotalAllocObjects = new int[len];

        for (int i = 0; i < len; i++) {
            nTotalAllocObjects[i] = in.readInt();
        }

        nTrackedItems = in.readInt();
        maxValue = in.readLong();
        nTotalTrackedBytes = in.readLong();
        nTotalTracked = in.readInt();
        currentEpoch = in.readInt();
    }

    public String toString() {
        return MessageFormat.format(MEMORY_LIVENESS_MSG, new Object[] { super.toString() });
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        out.writeInt(nTrackedAllocObjects.length);

        for (int i = 0; i < nTrackedAllocObjects.length; i++) {
            out.writeLong(nTrackedAllocObjects[i]);
        }

        out.writeInt(nTrackedLiveObjects.length);

        for (int i = 0; i < nTrackedLiveObjects.length; i++) {
            out.writeInt(nTrackedLiveObjects[i]);
        }

        out.writeInt(maxSurvGen.length);

        for (int i = 0; i < maxSurvGen.length; i++) {
            out.writeInt(maxSurvGen[i]);
        }

        out.writeInt(trackedLiveObjectsSize.length);

        for (int i = 0; i < trackedLiveObjectsSize.length; i++) {
            out.writeLong(trackedLiveObjectsSize[i]);
        }

        out.writeInt(avgObjectAge.length);

        for (int i = 0; i < avgObjectAge.length; i++) {
            out.writeFloat(avgObjectAge[i]);
        }

        out.writeInt(nInstrClasses);

        out.writeInt(nTotalAllocObjects.length);

        for (int i = 0; i < nTotalAllocObjects.length; i++) {
            out.writeInt(nTotalAllocObjects[i]);
        }

        out.writeInt(nTrackedItems);
        out.writeLong(maxValue);
        out.writeLong(nTotalTrackedBytes);
        out.writeInt(nTotalTracked);
        out.writeInt(currentEpoch);
    }

    protected PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                         boolean dontShowZeroLiveObjAllocPaths) {
        return PresoObjLivenessCCTNode.createPresentationCCTFromSnapshot(this, rootNode, getClassName(classId), currentEpoch,
                                                                         dontShowZeroLiveObjAllocPaths);
    }

    void debugValues() {
        super.debugValues();
        LOGGER.log(Level.FINEST, "nTrackedAllocObjects.length: {0}", debugLength(nTrackedAllocObjects));
        LOGGER.log(Level.FINEST, "nTrackedLiveObjects.length: {0}", debugLength(nTrackedLiveObjects));
        LOGGER.log(Level.FINEST, "maxSurvGen.length: {0}", debugLength(maxSurvGen)); // NOI18N
        LOGGER.log(Level.FINEST, "trackedLiveObjectsSize.length: {0}", debugLength(trackedLiveObjectsSize));
        LOGGER.log(Level.FINEST, "avgObjectAge.length: {0}", debugLength(avgObjectAge)); // NOI18N
        LOGGER.log(Level.FINEST, "nInstrClasses: {0}", nInstrClasses); // NOI18N
        LOGGER.log(Level.FINEST, "nTotalAllocObjects.length: {0}", debugLength(nTotalAllocObjects)); // NOI18N
        LOGGER.log(Level.FINEST, "nTrackedItems: {0}", nTrackedItems); // NOI18N
        LOGGER.log(Level.FINEST, "maxValue: {0}", maxValue); // NOI18N
        LOGGER.log(Level.FINEST, "nTotalTrackedBytes: {0}", nTotalTrackedBytes); // NOI18N
        LOGGER.log(Level.FINEST, "nTotalTracked: {0}", nTotalTracked); // NOI18N
        LOGGER.log(Level.FINEST, "currentEpoch: {0}", currentEpoch); // NOI18N
    }
}

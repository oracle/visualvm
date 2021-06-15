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

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.CCTProvider;


/**
 * @author Jaroslav Bachorik
 */
public interface MemoryCCTProvider extends CCTProvider {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Listener extends CCTProvider.Listener {
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // *****************************************************

    /**
     * A container class, needed just for correct data transfer to its consumers.
     * @author Misha Dmitirev
     */
    public static class ObjectNumbersContainer {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public float[] avgObjectAge;
        public int[] maxSurvGen;
        public long[] nTrackedAllocObjects;
        public int[] nTrackedLiveObjects;
        public long[] trackedLiveObjectsSize;
        public int nInstrClasses;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ObjectNumbersContainer(long[] nTrackedAllocObjects, int[] nTrackedLiveObjects, long[] trackedLiveObjectsSize,
                               float[] avgObjectAge, int[] maxSurvGen, boolean[] unprofiledClass, int nProfiledClasses) {
            nInstrClasses = nProfiledClasses;

            int len = nProfiledClasses;
            this.nTrackedAllocObjects = new long[len];
            this.nTrackedLiveObjects = new int[len];
            this.trackedLiveObjectsSize = new long[len];
            this.avgObjectAge = avgObjectAge;
            this.maxSurvGen = maxSurvGen;

            if (nTrackedAllocObjects != null) {
                System.arraycopy(nTrackedAllocObjects, 0, this.nTrackedAllocObjects, 0, len);
            }

            if (nTrackedLiveObjects != null) {
                System.arraycopy(nTrackedLiveObjects, 0, this.nTrackedLiveObjects, 0, len);
            }

            if (trackedLiveObjectsSize != null) {
                System.arraycopy(trackedLiveObjectsSize, 0, this.trackedLiveObjectsSize, 0, len);
            }

            for (int i = 0; i < unprofiledClass.length; i++) {
                if (unprofiledClass[i]) {
                    this.nTrackedAllocObjects[i] = -1; // Special value
                    this.nTrackedLiveObjects[i] = 0;
                    this.trackedLiveObjectsSize[i] = 0;
                    this.avgObjectAge[i] = 0.0f;
                    this.maxSurvGen[i] = 0;
                }
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    long[] getAllocObjectNumbers();

    int getCurrentEpoch();

    ObjectNumbersContainer getLivenessObjectNumbers();

    // following methods should be used only temporarily
    int getNProfiledClasses();

    long[] getObjectsSizePerClass();

    RuntimeMemoryCCTNode[] getStacksForClasses();

    void beginTrans(boolean mutable);

    boolean classMarkedUnprofiled(int classId);

    PresoObjAllocCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths)
                                        throws ClientUtils.TargetAppOrVMTerminated;

    void endTrans();

    /**
     * Marks the class with the given id as "unprofiled". Instrumentation for the class is not removed at this point.
     */
    void markClassUnprofiled(int classId);

    void updateInternals();
}

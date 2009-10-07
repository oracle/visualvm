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

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.CCTProvider;


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

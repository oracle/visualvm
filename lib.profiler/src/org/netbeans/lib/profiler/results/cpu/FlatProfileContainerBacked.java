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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;


/**
 * Container for CPU profiling results in the flat profile form. A concrete subclass of FlatProfileContainer,
 * where the data is backed by CPUCCTContainer.
 *
 * @author Misha Dmitriev
 */
public class FlatProfileContainerBacked extends FlatProfileContainer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected CPUCCTContainer cctContainer;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * The data passed to this constructor may contain some zero-invocation rows. That's because the size of passed arrays
     * is equal to the number of currently instrumented methods, but in general not all of the methods may be invoked even
     * once at an arbitrary moment.
     *
     * @param cctContainer The CPU results
     * @param timeInMcs0   Array of Absolute timer values for each method - always used
     * @param timeInMcs1   Array of CPU timer values for each method - optional, may be null
     * @param nInvocations Array of number of invocations for each method
     * @param nMethods     Total number of profiled methods - length of the provided arrays
     */
    public FlatProfileContainerBacked(CPUCCTContainer cctContainer, long[] timeInMcs0, long[] timeInMcs1, int[] nInvocations,
                                      int nMethods) {
        super(timeInMcs0, timeInMcs1, nInvocations, null, nMethods);
        this.cctContainer = cctContainer;

        collectingTwoTimeStamps = cctContainer.isCollectingTwoTimeStamps();

        // Now get rid of zero-invocation entries once and forever. Also set nTotalInvocations and set negative times
        // (that may be possible due to time cleansing inaccuracies) to zero.
        removeZeroInvocationEntries();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public CPUCCTContainer getCCTContainer() {
        return cctContainer;
    }

    public String getMethodNameAtRow(int row) {
        int methodId = methodIds[row];
        String[] methodClassNameAndSig = cctContainer.getMethodClassNameAndSig(methodId);

        return MethodNameFormatterFactory.getDefault().getFormatter()
                                         .formatMethodName(methodClassNameAndSig[0], methodClassNameAndSig[1],
                                                           methodClassNameAndSig[2]).toFormatted();

        //    return format.getFormattedClassAndMethod();
    }

    public double getWholeGraphNetTime0() {
        return cctContainer.getWholeGraphNetTime0();
    }

    public double getWholeGraphNetTime1() {
        return cctContainer.getWholeGraphNetTime1();
    }
}

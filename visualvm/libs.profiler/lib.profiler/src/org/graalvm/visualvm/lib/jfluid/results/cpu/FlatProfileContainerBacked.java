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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.utils.formatting.MethodNameFormatterFactory;


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
     * @param timeInMcs0   Array of Absolute timer values (self-time) for each method - always used
     * @param timeInMcs1   Array of CPU timer values (self-time) for each method - optional, may be null
     * @param totalTimeInMcs0   Array of Absolute timer (total time) values for each method - always used
     * @param totalTimeInMcs1   Array of CPU timer (total time) values for each method - optional, may be null
     * @param nInvocations Array of number of invocations for each method
     * @param nMethods     Total number of profiled methods - length of the provided arrays
     */
    public FlatProfileContainerBacked(CPUCCTContainer cctContainer, long[] timeInMcs0, long[] timeInMcs1,
            long[] totalTimeInMcs0, long[] totalTimeInMcs1, int[] nInvocations, int nMethods) {
        super(timeInMcs0, timeInMcs1, totalTimeInMcs0, totalTimeInMcs1, nInvocations, null, nMethods);
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
        ClientUtils.SourceCodeSelection sel = getSourceCodeSelectionAtRow(row);
        return MethodNameFormatterFactory.getDefault().getFormatter().formatMethodName(sel).toFormatted();
        
//        int methodId = methodIds[row];
//        String[] methodClassNameAndSig = cctContainer.getMethodClassNameAndSig(methodId);
//
//        return MethodNameFormatterFactory.getDefault().getFormatter()
//                                         .formatMethodName(methodClassNameAndSig[0], methodClassNameAndSig[1],
//                                                           methodClassNameAndSig[2]).toFormatted();
//
//        //    return format.getFormattedClassAndMethod();
    }
    
    public ClientUtils.SourceCodeSelection getSourceCodeSelectionAtRow(int row) {
        int methodId = methodIds[row];
        String[] sel = cctContainer.getMethodClassNameAndSig(methodId);
        return new ClientUtils.SourceCodeSelection(sel[0], sel[1], sel[2]);
    }

    public double getWholeGraphNetTime0() {
        return cctContainer.getWholeGraphNetTime0();
    }

    public double getWholeGraphNetTime1() {
        return cctContainer.getWholeGraphNetTime1();
    }
}

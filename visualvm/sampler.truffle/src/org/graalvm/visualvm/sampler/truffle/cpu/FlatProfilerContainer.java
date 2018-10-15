/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sampler.truffle.cpu;

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.results.cpu.MethodInfoMapper;
import org.graalvm.visualvm.lib.jfluid.utils.formatting.MethodNameFormatter;
import org.graalvm.visualvm.lib.jfluid.utils.formatting.MethodNameFormatterFactory;


/**
 *
 * @author Tomas Hurka
 */
final class FlatProfilerContainer extends FlatProfileContainer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected double wholeGraphNetTime0;
    protected double wholeGraphNetTime1;
    private MethodInfoMapper methodInfoMapper;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * The data passed to this constructor may contain some zero-invocation rows. That's because the size of passed arrays
     * is equal to the number of currently instrumented methods, but in general not all of the methods may be invoked even
     * once at an arbitrary moment.
     *
     * @param timeInMcs0         Array of Absolute timer values for each method - always used
     * @param timeInMcs1         Array of CPU timer values for each method - optional, may be null
     * @param totalTimeInMcs0   Array of Absolute timer (total time) values for each method - always used
     * @param totalTimeInMcs1   Array of CPU timer (total time) values for each method - optional, may be null
     * @param nInvocations       Array of number of invocations for each method
     * @param wholeGraphNetTime0 Total absolute time
     * @param wholeGraphNetTime1 Total CPU time - not used if CPU timer is not used
     * @param nMethods           Total number of profiled methods - length of the provided arrays
     */
    FlatProfilerContainer(MethodInfoMapper mapper,boolean twoStamps,long[] timeInMcs0, long[] timeInMcs1, 
                                    long[] totalTimeInMcs0, long[] totalTimeInMcs1,int[] nInvocations,
                                    char[] marks, double wholeGraphNetTime0, double wholeGraphNetTime1, int nMethods) {
        super(timeInMcs0, timeInMcs1, totalTimeInMcs0, totalTimeInMcs1, nInvocations, marks, nMethods);
        this.wholeGraphNetTime0 = wholeGraphNetTime0;
        this.wholeGraphNetTime1 = wholeGraphNetTime1;

        collectingTwoTimeStamps = twoStamps;
        methodInfoMapper = mapper;

        // Now get rid of zero-invocation entries once and forever. Also set nTotalInvocations and set negative times
        // (that may be possible due to time cleansing inaccuracies) to zero.
        removeZeroInvocationEntries();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getMethodNameAtRow(int row) {
        int methodId = methodIds[row];
        MethodNameFormatter formatter = MethodNameFormatterFactory.getDefault().getFormatter(null);

        String className = methodInfoMapper.getInstrMethodClass(methodId);
        String methodName = methodInfoMapper.getInstrMethodName(methodId);
        String signature = methodInfoMapper.getInstrMethodSignature(methodId);

        return formatter.formatMethodName(className, methodName, signature).toFormatted();
    }

    public double getWholeGraphNetTime0() {
        return wholeGraphNetTime0;
    }

    public double getWholeGraphNetTime1() {
        return wholeGraphNetTime1;
    }

    @Override
    public ClientUtils.SourceCodeSelection getSourceCodeSelectionAtRow(int i) {
        throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
    }
}

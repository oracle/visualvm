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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;


/**
 * Container for CPU profiling results in the flat profile form. A concrete subclass of FlatProfileContainer,
 * where the data is partially backed by ProfilingSessionStatus and partially is self-contained.
 *
 * @author Misha Dmitriev
 */
public class FlatProfileContainerFree extends FlatProfileContainer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected ProfilingSessionStatus status;
    protected double wholeGraphNetTime0;
    protected double wholeGraphNetTime1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * The data passed to this constructor may contain some zero-invocation rows. That's because the size of passed arrays
     * is equal to the number of currently instrumented methods, but in general not all of the methods may be invoked even
     * once at an arbitrary moment.
     *
     * @param status             Reference to ProfilingSessionStatus
     * @param timeInMcs0         Array of Absolute timer values (self-time) for each method - always used
     * @param timeInMcs1         Array of CPU timer values (self-time) for each method - optional, may be null
     * @param totalTimeInMcs0    Array of Absolute timer (total time) values for each method - always used
     * @param totalTimeInMcs1    Array of CPU timer (total time) values for each method - optional, may be null
     * @param nInvocations       Array of number of invocations for each method
     * @param wholeGraphNetTime0 Total absolute time
     * @param wholeGraphNetTime1 Total CPU time - not used if CPU timer is not used
     * @param nMethods           Total number of profiled methods - length of the provided arrays
     */
    public FlatProfileContainerFree(ProfilingSessionStatus sessionStatus, long[] timeInMcs0, long[] timeInMcs1, 
            long[] totalTimeInMcs0, long[] totalTimeInMcs1, int[] nInvocations, char[] marks, 
            double wholeGraphNetTime0, double wholeGraphNetTime1, int nMethods) {
        super(timeInMcs0, timeInMcs1, totalTimeInMcs0, totalTimeInMcs1, nInvocations, marks, nMethods);
        this.status = sessionStatus;
        this.wholeGraphNetTime0 = wholeGraphNetTime0;
        this.wholeGraphNetTime1 = wholeGraphNetTime1;

        collectingTwoTimeStamps = sessionStatus.collectingTwoTimeStamps();

        // Now get rid of zero-invocation entries once and forever. Also set nTotalInvocations and set negative times
        // (that may be possible due to time cleansing inaccuracies) to zero.
        removeZeroInvocationEntries();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getMethodNameAtRow(int row) {
        ClientUtils.SourceCodeSelection sel = getSourceCodeSelectionAtRow(row);
        return MethodNameFormatterFactory.getDefault().getFormatter()
                                         .formatMethodName(sel.getClassName(), sel.getMethodName(),
                                                           sel.getMethodSignature()).toFormatted();
        
//        int methodId = methodIds[row];
//        MethodNameFormatter formatter = MethodNameFormatterFactory.getDefault().getFormatter(null);
//
//        status.beginTrans(false);
//
//        try {
//            String[] classes = status.getInstrMethodClasses();
//            String[] methods = status.getInstrMethodNames();
//            String[] signatures = status.getInstrMethodSignatures();
//
//            return formatter.formatMethodName((classes != null && classes.length > methodId) ? classes[methodId] : null,
//                                              (methods != null && methods.length > methodId) ? methods[methodId] : null,
//                                              (signatures != null && signatures.length > methodId) ? signatures[methodId] : null).toFormatted();
//        } finally {
//            status.endTrans();
//        }
    }
    
    public ClientUtils.SourceCodeSelection getSourceCodeSelectionAtRow(int row) {
        int methodId = methodIds[row];

        status.beginTrans(false);

        try {
            String[] classes = status.getInstrMethodClasses();
            String[] methods = status.getInstrMethodNames();
            String[] signatures = status.getInstrMethodSignatures();
            
            String _class = classes != null && classes.length > methodId ? classes[methodId] : null;
            String _method = methods != null && methods.length > methodId ? methods[methodId] : null;
            String _signature = signatures != null && signatures.length > methodId ? signatures[methodId] : null;

            return new ClientUtils.SourceCodeSelection(_class, _method, _signature);
        } finally {
            status.endTrans();
        }
    }

    public ProfilingSessionStatus getStatus() {
        return status;
    }

    public double getWholeGraphNetTime0() {
        return wholeGraphNetTime0;
    }

    public double getWholeGraphNetTime1() {
        return wholeGraphNetTime1;
    }
}

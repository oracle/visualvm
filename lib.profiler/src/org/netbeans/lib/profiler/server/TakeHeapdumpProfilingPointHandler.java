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

package org.netbeans.lib.profiler.server;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.global.ProfilingPointServerHandler;
import org.netbeans.lib.profiler.server.system.HeapDump;
import org.netbeans.lib.profiler.server.system.Timers;
import java.io.File;


/**
 * Performs special handling of Take HeapDump profiling points on server side.
 *
 * @author Tomas Hurka
 */
public class TakeHeapdumpProfilingPointHandler extends ProfilingPointServerHandler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static ProfilingPointServerHandler instance;
    private static final String TAKEN_HEAPDUMP_PREFIX = "heapdump-"; // NOI18N
    private static final String HEAPDUMP_EXTENSION = "hprof"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String heapdumpFilePrefix;
    private boolean remoteProfiling;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private TakeHeapdumpProfilingPointHandler(String dir) {
        heapdumpFilePrefix = dir + File.separatorChar + TAKEN_HEAPDUMP_PREFIX;
        remoteProfiling = ProfilerServer.getProfilingSessionStatus().remoteProfiling;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilingPointServerHandler getInstance(String clientInfo) {
        if (instance == null) {
            instance = new TakeHeapdumpProfilingPointHandler(clientInfo);
        }

        return instance;
    }

    public void profilingPointHit(int id) {
        int instrType = ProfilerInterface.getCurrentInstrType();
        boolean cpuProfiling = (instrType == CommonConstants.INSTR_RECURSIVE_FULL)
                               || (instrType == CommonConstants.INSTR_RECURSIVE_SAMPLED);

        if (cpuProfiling) { // CPU profiling
            ProfilerRuntimeCPU.suspendCurrentThreadTimer();
        }

        long absTimeStamp = Timers.getCurrentTimeInCounts();

        if (!remoteProfiling) { // take heap dump is supported only for local profiling

            String heapdumpName = getHeapDumpName(absTimeStamp);
            String error = HeapDump.takeHeapDump(Platform.getJDKVersionNumber() == Platform.JDK_15, heapdumpName);

            if (error != null) {
                System.err.println("Dump to " + heapdumpName + " failed with " + error); // NOI18N
            }
        }

        super.profilingPointHit(id, absTimeStamp);

        if (cpuProfiling) {
            ProfilerRuntimeCPU.resumeCurrentThreadTimer();
        }
    }

    private String getHeapDumpName(long time) {
        return heapdumpFilePrefix + (time & 0xFFFFFFFFFFFFFFL) + "." + HEAPDUMP_EXTENSION;
    }
}

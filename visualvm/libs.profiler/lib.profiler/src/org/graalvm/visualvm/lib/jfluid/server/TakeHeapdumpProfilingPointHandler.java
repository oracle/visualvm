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

package org.graalvm.visualvm.lib.jfluid.server;

import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.server.system.HeapDump;
import org.graalvm.visualvm.lib.jfluid.server.system.Timers;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Performs special handling of Take HeapDump profiling points on server side.
 *
 * @author Tomas Hurka
 */
public class TakeHeapdumpProfilingPointHandler extends ProfilingPointServerHandler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Map instances;
    private static final String TAKEN_HEAPDUMP_PREFIX = "heapdump-"; // NOI18N
    private static final String HEAPDUMP_EXTENSION = "hprof"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final String heapdumpFilePrefix;
    private final boolean remoteProfiling;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private TakeHeapdumpProfilingPointHandler(String dir) {
        heapdumpFilePrefix = dir + File.separatorChar + TAKEN_HEAPDUMP_PREFIX;
        remoteProfiling = ProfilerServer.getProfilingSessionStatus().remoteProfiling;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilingPointServerHandler getInstance(String clientInfo) {
        TakeHeapdumpProfilingPointHandler instance;

        if (instances == null) {
            instances = new HashMap();
        }
        instance = (TakeHeapdumpProfilingPointHandler) instances.get(clientInfo);
        if (instance == null) {
            instance = new TakeHeapdumpProfilingPointHandler(clientInfo);
            instances.put(clientInfo, instance);
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
            String error = HeapDump.takeHeapDump(heapdumpName);

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

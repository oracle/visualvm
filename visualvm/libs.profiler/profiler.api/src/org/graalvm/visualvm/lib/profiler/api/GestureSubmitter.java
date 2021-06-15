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

package org.graalvm.visualvm.lib.profiler.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.common.AttachSettings;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;


/**
 * A utility class for submitting UI Gestures Collector records
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
public class GestureSubmitter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger USG_LOGGER = Logger.getLogger("org.netbeans.ui.metrics.profiler"); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

//    public static void logConfig(ProfilingSettings settings, InstrumentationFilter filter) {
//        List<Object> paramList = new ArrayList<Object>();
//
//        fillParamsForProfiling(settings, paramList);
//
//        logUsage("CONFIG", paramList); // NOI18N
//    }

    public static void logProfileApp(Lookup.Provider p, ProfilingSettings ps) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(p, paramList);
        fillParamsForProfiling(ps, paramList);

        logUsage("PROFILE_APP", paramList); // NOI18N
    }

    public static void logAttachApp(Lookup.Provider p, ProfilingSettings ps, AttachSettings as) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(p, paramList);
        fillParamsForProfiling(ps, paramList);
        fillParamsForAttach(as, paramList);

        logUsage("ATTACH_APP", paramList); // NOI18N
    }
    
    public static void logAttachExternal(ProfilingSettings ps, AttachSettings as) {
        List<Object> paramList = new ArrayList<Object>();

        fillParamsForProfiling(ps, paramList);
        fillParamsForAttach(as, paramList);

        logUsage("ATTACH_EXT", paramList); // NOI18N
    }

//    public static void logProfileClass(Lookup.Provider profiledProject, SessionSettings session) {
//        List<Object> paramList = new ArrayList<Object>();
//
//        fillProjectParam(profiledProject, paramList);
//        fillParamsForSession(session, paramList);
//
//        logUsage("PROFILE_CLASS", paramList); // NOI18N
//    }

//    public static void logAttach(Lookup.Provider profiledProject, AttachSettings attach) {
//        List<Object> paramList = new ArrayList<Object>();
//
//        fillProjectParam(profiledProject, paramList);
//        fillParamsForAttach(attach, paramList);
//
//        logUsage("ATTACH", paramList); // NOI18N
//    }
    
//    public static void logRMSSearch(String pattern) {
//        logUsage("RMS_SEARCH", Arrays.asList(new Object[]{pattern}));
//    }

    private static void fillProjectParam(Lookup.Provider profiledProject, List<Object> paramList) {
        String param = ""; // NOI18N
        if (profiledProject != null) {
            param = profiledProject.getClass().getName();
        }
        paramList.add(0, param);
    }
    
    private static void fillParamsForAttach(AttachSettings as, List<Object> paramList) {
//        paramList.add("OS_" + as.getHostOS());
        paramList.add(as.isDirect() ? "ATTACH_DIRECT" : "ATTACH_DYNAMIC"); // NOI18N
        paramList.add(as.isRemote() ? "ATTACH_REMOTE" : "ATTACH_LOCAL"); // NOI18N
    }
    
    private static void fillParamsForProfiling(ProfilingSettings ps, List<Object> paramList) {
        switch (ps.getProfilingType()) {
            case ProfilingSettings.PROFILE_CPU_ENTIRE:
                paramList.add("TYPE_CPU_ENTIRE"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_PART:
                paramList.add("TYPE_CPU_PART"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_JDBC:
                paramList.add("TYPE_CPU_JDBC"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_SAMPLING:
                paramList.add("TYPE_CPU_SAMPLING"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                paramList.add("TYPE_CPU_STOPWATCH"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                paramList.add("TYPE_MEM_ALLOC"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                paramList.add("TYPE_MEM_LIVENESS"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
                paramList.add("TYPE_MEM_SAMPLING"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MONITOR:
                paramList.add("TYPE_MONITOR"); // NOI18N

                break;
        }
        
        if (ps.getThreadsMonitoringEnabled()) paramList.add("TYPE_THREADS"); // NOI18N
        
        if (ps.getLockContentionMonitoringEnabled()) paramList.add("TYPE_LOCKS"); // NOI18N
    }

//    private static void fillParamsForProfiling(ProfilingSettings ps, List<Object> paramList) {
//        switch (ps.getProfilingType()) {
//            case ProfilingSettings.PROFILE_CPU_ENTIRE:
//                paramList.add("TYPE_CPU_ENTIRE"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_CPU_PART:
//                paramList.add("TYPE_CPU_PART"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_CPU_SAMPLING:
//                paramList.add("TYPE_CPU_SAMPLING"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
//                paramList.add("TYPE_CPU_STOPWATCH"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
//                paramList.add("TYPE_MEM_ALLOC"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
//                paramList.add("TYPE_MEM_LIVENESS"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
//                paramList.add("TYPE_MEM_SAMPLING"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MONITOR:
//                paramList.add("TYPE_MONITOR"); // NOI18N
//
//                break;
//        }
//
//        switch (ps.getInstrScheme()) {
//            case CommonConstants.INSTRSCHEME_EAGER:
//                paramList.add("INSTR_EAGER"); // NOI18N
//
//                break;
//            case CommonConstants.INSTRSCHEME_LAZY:
//                paramList.add("INSTR_LAZY"); // NOI18N
//
//                break;
//            case CommonConstants.INSTRSCHEME_TOTAL:
//                paramList.add("INSTR_TOTAL"); // NOI18N
//
//                break;
//        }
//
//        paramList.add(ps.getProfileUnderlyingFramework() ? "FRAMEWORK_YES" : "FRAMEWORK_NO");
//        paramList.add(ps.getExcludeWaitTime() ? "WAIT_EXCLUDE" : "WAIT_INCLUDE"); // NOI18N
//        paramList.add(ps.getInstrumentMethodInvoke() ? "REFL_INVOKE_YES" : "REFL_INVOKE_NO"); // NOI18N
//        paramList.add(ps.getInstrumentSpawnedThreads() ? "SPAWNED_THREADS_YES" : "SPAWNED_THREADS_NO"); // NOI18N
//        paramList.add(ps.getThreadCPUTimerOn() ? "THREAD_CPU_YES" : "THREAD_CPU_NO"); // NOI18N
//        paramList.add(ps.useProfilingPoints() ? "PPOINTS_YES" : "PPOINTS_NO"); //NOI18N
//    }

//    private static void fillParamsForSession(SessionSettings ss, List<Object> paramList) {
//        paramList.add("JAVA_" + ss.getJavaVersionString()); // NOI18N
//    }

    private static void logUsage(String startType, List<Object> params) {
        LogRecord record = new LogRecord(Level.INFO, "USG_PROFILER_" + startType); // NOI18N
        record.setResourceBundle(NbBundle.getBundle(GestureSubmitter.class));
        record.setResourceBundleName(GestureSubmitter.class.getPackage().getName() + ".Bundle"); // NOI18N
        record.setLoggerName(USG_LOGGER.getName());
        record.setParameters(params.toArray(new Object[0]));

        USG_LOGGER.log(record);
    }
}

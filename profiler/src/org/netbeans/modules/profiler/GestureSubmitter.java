/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.SessionSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.openide.util.NbBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * A utility class for submitting UI Gestures Collector records
 * @author Jaroslav Bachorik
 */
class GestureSubmitter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger USG_LOGGER = Logger.getLogger("org.netbeans.ui.metrics.profiler"); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    static void logConfig(ProfilingSettings settings) {
        List<Object> paramList = new ArrayList<Object>();

        fillParamsForProfiling(settings, paramList);

        logUsage("CONFIG", paramList); // NOI18N
    }

    static void logProfileApp(Project profiledProject, SessionSettings session) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(profiledProject, paramList);
        fillParamsForSession(session, paramList);

        logUsage("PROFILE_APP", paramList); // NOI18N
    }

    static void logProfileClass(Project profiledProject, SessionSettings session) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(profiledProject, paramList);
        fillParamsForSession(session, paramList);

        logUsage("PROFILE_CLASS", paramList); // NOI18N
    }

    static void logAttach(Project profiledProject, AttachSettings attach) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(profiledProject, paramList);
        fillParamsForAttach(attach, paramList);

        logUsage("ATTACH", paramList); // NOI18N
    }

    private static void fillProjectParam(Project profiledProject, List<Object> paramList) {
        String param = ""; // NOI18N
        if (profiledProject != null) {
            param = profiledProject.getClass().getName();
        }
        paramList.add(0, param);
    }
    
    private static void fillParamsForAttach(AttachSettings as, List<Object> paramList) {
        paramList.add("OS_" + as.getHostOS());
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
            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                paramList.add("TYPE_CPU_STOPWATCH"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                paramList.add("TYPE_MEM_ALLOC"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                paramList.add("TYPE_MEM_LIVENESS"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MONITOR:
                paramList.add("TYPE_MONITOR"); // NOI18N

                break;
        }

        switch (ps.getInstrScheme()) {
            case CommonConstants.INSTRSCHEME_EAGER:
                paramList.add("INSTR_EAGER"); // NOI18N

                break;
            case CommonConstants.INSTRSCHEME_LAZY:
                paramList.add("INSTR_LAZY"); // NOI18N

                break;
            case CommonConstants.INSTRSCHEME_TOTAL:
                paramList.add("INSTR_TOTAL"); // NOI18N

                break;
        }

        paramList.add(ps.getProfileUnderlyingFramework() ? "FRAMEWORK_YES" : "FRAMEWORK_NO");
        paramList.add(ps.getExcludeWaitTime() ? "WAIT_EXCLUDE" : "WAIT_INCLUDE"); // NOI18N
        paramList.add(ps.getInstrumentMethodInvoke() ? "REFL_INVOKE_YES" : "REFL_INVOKE_NO"); // NOI18N
        paramList.add(ps.getInstrumentSpawnedThreads() ? "SPAWNED_THREADS_YES" : "SPAWNED_THREADS_NO"); // NOI18N
        paramList.add(ps.getThreadCPUTimerOn() ? "THREAD_CPU_YES" : "THREAD_CPU_NO"); // NOI18N
        paramList.add(ps.useProfilingPoints() ? "PPOINTS_YES" : "PPOINTS_NO"); //NOI18N
    }

    private static void fillParamsForSession(SessionSettings ss, List<Object> paramList) {
        paramList.add("JAVA_" + ss.getJavaVersionString()); // NOI18N
    }

    private static void logUsage(String startType, List<Object> params) {
        LogRecord record = new LogRecord(Level.INFO, "USG_PROFILER_" + startType); // NOI18N
        record.setResourceBundle(NbBundle.getBundle(NetBeansProfiler.class));
        record.setResourceBundleName(NetBeansProfiler.class.getPackage().getName() + ".Bundle"); // NOI18N
        record.setLoggerName(USG_LOGGER.getName());
        record.setParameters(params.toArray(new Object[params.size()]));

        USG_LOGGER.log(record);
    }
}

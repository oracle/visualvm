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


/**
 * This class contains instrumentation methods for object allocation profiling.
 *
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeObjAlloc extends ProfilerRuntimeMemory {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static boolean objAllocProfilingDisabled = true;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void enableProfiling(boolean v) {
        if (!v) {
            objAllocProfilingDisabled = true;
        }

        if (v) {
            createNewDataStructures();
            ProfilerRuntimeMemory.enableProfiling(v);
        } else {
            ProfilerRuntimeMemory.enableProfiling(v);

            // Give the threads that are currently executing instrumentation enough time to finish
            // before we nullify the data structures that are used in instrumentation code.
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
            }

            clearDataStructures();
        }

        if (v) {
            objAllocProfilingDisabled = false;
        }
    }

    public static void traceObjAlloc(Object object, char classId) {
        if (objAllocProfilingDisabled) {
            return;
        }

        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // Avoid counting objects allocated by our own agent threads, or by this method's callees
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        if (!ti.isInitialized()) {
            ti.initialize(true);
        }

        ti.inProfilingRuntimeMethod++;

        // See comment marked with (***) in ProfilerRuntimeCPUFullInstr
        classId = (char) ((int) classId);

        synchronized (allocatedInstancesCount) {
            allocatedInstancesCount[classId]++;
        }

        if (allocatedInstThreshold[classId] <= 0) {
            long objSize = getCachedObjectSize(classId, object);
            getAndSendCurrentStackTrace(classId, objSize);
            allocatedInstThreshold[classId] = nextRandomizedInterval();
        }

        allocatedInstThreshold[classId]--;
        ti.inProfilingRuntimeMethod--;
    }

    protected static void clearDataStructures() {
        ProfilerRuntimeMemory.clearDataStructures();
    }

    protected static void createNewDataStructures() {
        ProfilerRuntimeMemory.createNewDataStructures();
    }
}

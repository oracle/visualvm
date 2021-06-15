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

        if (ThreadInfo.profilingSuspended()
            || ThreadInfo.isCurrentThreadProfilerServerThread()
            || (classId == 0 && isInternalClass(object.getClass()))) {
            // Avoid counting objects allocated by our own agent threads, or by this method's callees
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        if (!ti.isInitialized()) {
            ti.initialize();
            if (lockContentionMonitoringEnabled) writeThreadCreationEvent(ti);
        }

        ti.inProfilingRuntimeMethod++;

        int classInt;

        if (classId == 0) {
            //System.out.println("traceObjAlloc(Object object, 0) "+ object.getClass());
            classInt = getClassId(object.getClass());
            if (classInt == -1) {
                ti.inProfilingRuntimeMethod--;
                return;
            }
        } else {
            // See comment marked with (***) in ProfilerRuntimeCPUFullInstr
            classInt = classId&0xff;
            classInt |= classId&0xff00;
        }
        synchronized (allocatedInstancesCount) {
            allocatedInstancesCount[classInt]++;
        }

        if (allocatedInstThreshold[classInt] <= 0) {
            long objSize = getCachedObjectSize(classInt, object);
            getAndSendCurrentStackTrace(classInt, objSize);
            allocatedInstThreshold[classInt] = nextRandomizedInterval();
        }

        allocatedInstThreshold[classInt]--;
        ti.inProfilingRuntimeMethod--;
    }

    protected static void clearDataStructures() {
        ProfilerRuntimeMemory.clearDataStructures();
    }

    protected static void createNewDataStructures() {
        ProfilerRuntimeMemory.createNewDataStructures();
    }
}

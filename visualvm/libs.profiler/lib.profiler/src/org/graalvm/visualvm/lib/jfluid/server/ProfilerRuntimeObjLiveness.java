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

import org.graalvm.visualvm.lib.jfluid.server.system.GC;
import org.graalvm.visualvm.lib.jfluid.server.system.Threads;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;


/**
 * This class contains instrumentation methods for object liveness profiling.
 *
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeObjLiveness extends ProfilerRuntimeMemory {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // ------------------------------------- Support classes --------------------------------------------------
    static class ProfilerRuntimeObjLivenessWeakRef extends WeakReference {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        long objId;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ProfilerRuntimeObjLivenessWeakRef(Object obj, ReferenceQueue rq, long objId) {
            super(obj, rq);
            this.objId = objId;
        }
    }

    /** A thread that waits on a ReferenceQueue managing our marked objects */
    static class ReferenceManagerThread extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private volatile boolean terminated;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ReferenceManagerThread() {
            ThreadInfo.addProfilerServerThread(this);
            setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 3"); // NOI18N
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            while (!terminated) {
                try {
                    ProfilerRuntimeObjLivenessWeakRef wr = (ProfilerRuntimeObjLivenessWeakRef) rq.remove(200);

                    if (wr != null && !terminated) {
                        signalObjGC(wr);
                    }
                } catch (InterruptedException ex) { /* Should not happen */
                }
            }

            ThreadInfo.removeProfilerServerThread(this);
        }

        public void terminate() {
            terminated = true;
        }
    }

    /** A hashtable keeping a set of all tracked objects */
    static class WeakRefSet {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private WeakReference[] keys;
        private int capacity;
        private int nObjects;
        private int threshold;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        WeakRefSet() {
            capacity = 1003;
            setThreshold();
            keys = new WeakReference[capacity];
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized void put(WeakReference key) {
            if (nObjects > threshold) {
                rehash();
            }

            int pos = (key.hashCode() & 0x7FFFFFFF) % capacity;

            while (keys[pos] != null) {
                pos = (pos + 1) % capacity;
            }

            keys[pos] = key;
            nObjects++;
        }

        public synchronized void remove(WeakReference key) {
            int pos = (key.hashCode() & 0x7FFFFFFF) % capacity;

            while (keys[pos] != key) {
                pos = (pos + 1) % capacity;
            }

            keys[pos] = null;
            nObjects--;
        }

        private void setThreshold() {
            threshold = (capacity * 3) / 4;
        }

        private void rehash() {
            WeakReference[] oldKeys = keys;
            int oldCapacity = capacity;
            capacity = (capacity * 2) + 1;
            keys = new WeakReference[capacity];

            for (int i = 0; i < oldCapacity; i++) {
                if (oldKeys[i] != null) {
                    int pos = (oldKeys[i].hashCode() & 0x7FFFFFFF) % capacity;

                    while (keys[pos] != null) {
                        pos = (pos + 1) % capacity;
                    }

                    keys[pos] = oldKeys[i];
                }
            }

            setThreshold();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static ReferenceQueue rq;
    private static WeakRefSet objSet;
    private static ReferenceManagerThread rmt;
    protected static boolean runGCOnGetResults;
    protected static boolean objLivenessProfilingDisabled = true;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void enableProfiling(boolean v) {
        if (v) {
            createNewDataStructures();
            // activateGCEpochCounter(true);  We don't do this anymore, it's activated forever in profiler.server.Monitors
            GC.resetGCEpochCounter();
            ProfilerRuntimeMemory.enableProfiling(true);
            objLivenessProfilingDisabled = false;
        } else {
            objLivenessProfilingDisabled = true;
            ProfilerRuntimeMemory.enableProfiling(false);

            // Give the threads that are currently executing instrumentation enough time to finish
            // before we nullify the data structures that are used in instrumentation code.
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
            }

            clearDataStructures();
        }
    }

    public static void resetProfilerCollectors() {
        if (rmt != null) {
            GC.runGC();
            rmt.terminate();
        }

        createNewDataStructures();

        // We don't reset the epoch counter anymore, since there is still a chance that some objects with a higher
        // epoch counter are reported after this event, which may confuse the tool. So we keep the ever-growing epoch counter.
        //GC.resetGCEpochCounter();

        // TODO [ian] - check this furhter - it was reported by Jon Christianssen that objects with high surviving gen
        // numbers were reported after resetting the results, which he (rightfully so) considered wrong
    }

    static void signalObjGC(ProfilerRuntimeObjLivenessWeakRef wr) {
        long objectId = wr.objId;
        objSet.remove(wr);
        writeObjGCEvent(objectId);
    }

    public static void traceObjAlloc(Object object, char classId) {
        if (objLivenessProfilingDisabled) {
            return;
        }

        if (ThreadInfo.profilingSuspended()
            || ThreadInfo.isCurrentThreadProfilerServerThread()
            || (classId == 0 && isInternalClass(object.getClass()))) {
            // Avoid counting objects allocated by our own agent threads, or by this method's callees
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized()) {
            ti.initialize();
            if (lockContentionMonitoringEnabled) writeThreadCreationEvent(ti);
        }

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
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

        int objCount = 0;

        synchronized (allocatedInstancesCount) {
            objCount = ++allocatedInstancesCount[classInt];
        }

        if (allocatedInstThreshold[classInt] <= 0) {
            //System.out.print("+++ Alloc object "); //System.out.print((int) classId); System.out.print(" "); System.out.println(object);
            char epoch = (char) GC.getCurrentGCEpoch();

            // Generate a 64-bit object id. Make sure the function is the same at the tool side!
            long objectId = (((long) classInt) << 48) | (((long) epoch) << 32) | ((long) objCount);
            ProfilerRuntimeObjLivenessWeakRef wr = new ProfilerRuntimeObjLivenessWeakRef(object, rq, objectId);
            objSet.put(wr);

            long objSize = getCachedObjectSize(classInt, object);

            getAndSendCurrentStackTrace(classInt, epoch, objCount, objSize);

            allocatedInstThreshold[classInt] = nextRandomizedInterval();
        }

        allocatedInstThreshold[classInt]--;
        ti.inProfilingRuntimeMethod--;
    }

    protected static void setRunGCOnGetResults(boolean v) {
        runGCOnGetResults = v;
    }

    protected static boolean getRunGCOnGetResults() {
        return runGCOnGetResults;
    }

    protected static void clearDataStructures() {
        ProfilerRuntimeMemory.clearDataStructures();

        if (rmt != null) {
            GC.runGC();
            rmt.terminate();
        }

        rq = null;
        objSet = null;
        rmt = null;

        // activateGCEpochCounter(false);  See the comment in enableProfiling() above
    }

    protected static void createNewDataStructures() {
        ProfilerRuntimeMemory.createNewDataStructures();
        rq = new ReferenceQueue();
        objSet = new WeakRefSet();
        rmt = new ReferenceManagerThread();
        Threads.recordAdditionalProfilerOwnThread(rmt);
        rmt.start();
    }
}

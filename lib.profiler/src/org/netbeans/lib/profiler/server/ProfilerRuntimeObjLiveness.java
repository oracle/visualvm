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

package org.netbeans.lib.profiler.server;

import org.netbeans.lib.profiler.server.system.GC;
import org.netbeans.lib.profiler.server.system.Threads;
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

        private boolean terminated;

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

                    if (wr != null) {
                        signalObjGC(wr);
                    }
                } catch (InterruptedException ex) { /* Should not happen */
                }
            }

            ThreadInfo.removeProfilerServerThread(this);
        }

        public void terminate() {
            terminated = true;

            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) { /* Should not happen */
            }
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
    protected static WeakRefSet objSet;
    protected static ReferenceManagerThread rmt;
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

    public static void signalObjGC(ProfilerRuntimeObjLivenessWeakRef wr) {
        long objectId = wr.objId;
        objSet.remove(wr);
        writeObjGCEvent(objectId);
    }

    public static void traceObjAlloc(Object object, char classId) {
        if (objLivenessProfilingDisabled) {
            return;
        }

        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // Avoid counting objects allocated by our own agent threads, or by this method's callees
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized()) {
            ti.initialize(true);
        }

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;

        // See comment marked with (***) in ProfilerRuntimeCPUFullInstr
        classId = (char) ((int) classId);

        int objCount = 0;

        synchronized (allocatedInstancesCount) {
            objCount = ++allocatedInstancesCount[classId];
        }

        if (allocatedInstThreshold[classId] <= 0) {
            //System.out.print("+++ Alloc object "); //System.out.print((int) classId); System.out.print(" "); System.out.println(object);
            char epoch = (char) GC.getCurrentGCEpoch();

            // Generate a 64-bit object id. Make sure the function is the same at the tool side!
            long objectId = (((long) classId) << 48) | (((long) epoch) << 32) | ((long) objCount);
            ProfilerRuntimeObjLivenessWeakRef wr = new ProfilerRuntimeObjLivenessWeakRef(object, rq, objectId);
            objSet.put(wr);

            long objSize = getCachedObjectSize(classId, object);

            getAndSendCurrentStackTrace(classId, epoch, objCount, objSize);

            allocatedInstThreshold[classId] = nextRandomizedInterval();
        }

        allocatedInstThreshold[classId]--;
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

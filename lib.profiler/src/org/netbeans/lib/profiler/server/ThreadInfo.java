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

package org.netbeans.lib.profiler.server;


/**
 * An instance of this class is allocated for each profiled thread, to hold thread-local information, such as whether
 * the thread is currently in the profiled call subgraph, the simulated call stack, the thread-local rough generated
 * data buffer (for CPU profiling), etc. Static methods to create and lookup instances of ThreadInfo given a Thread
 * object are also provided in this class.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ThreadInfo {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final int MAX_EVENT_ENTRIES_IN_LOCAL_BUFFER = 500; // Thread-local buffer, in events
    static final int MAX_EVENT_SIZE = 1 + 2 + 7 + 7; // In bytes; comprises event type, method id, and two long timestamps
    static int evBufSize; // Size and threshold, same for each thread-local buffer
    static int evBufPosThreshold; // Size and threshold, same for each thread-local buffer

    static {
        setDefaultEvBufParams();
    }

    static Thread[] profilerServerThreads;
    static int nProfilerServerThreads;
    static int nProfiledAppThreads;
    static ThreadInfo dummyThreadInfo = new ThreadInfo(null); // Used just to avoid null checks in some situations
    private static boolean profilingSuspended = false;

    // ThreadInfo hash table
    private static ThreadInfo[] threadInfos = new ThreadInfo[1]; // To avoid null checks - important!
    private static final Object threadInfosLock = new Object();
    private static int threadInfosSize;
    private static int nThreads;
    private static boolean hasDeadThreads;
    private static ThreadInfo lastThreadInfo = dummyThreadInfo;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    Thread thread; // Thread object for this ThreadInfo
    byte[] evBuf; // Thread-local event (rough profiling data) buffer. Currently used in CPU profiling only.
    boolean inCallGraph; // Indicates whether the thread is currently in the profiled subgraph
    boolean sampleDue; // In sampled instrumentation mode, indicates that next sampling should be done
    int evBufDumpLastPos; // Used to avoid synchronization in writeEvent() and yet to allow for asynchronous event buffer dumps.
    int evBufPos; // Current position in the local event buffer
    int inProfilingRuntimeMethod; // Indicates whether currently some profiling runtime method is executed on behalf of this thread
    int rootMethodStackDepth; // logical stack depth of the root method which is inside of marker method
    int stackDepth; // Current logical (i.e. relative to the root method frame) stack depth
    int threadId; // Integer ID
    long absEntryTime; // Used to support thread suspension and code fragment profiling
    long lastWaitStartTime; // Used in Code Region profiling for tracking wait times
    long threadEntryTime; // Used to support thread suspension and code fragment profiling
    private boolean initialized; // To signal that this thread is not initialized or was reset, so this threadInfo is unusable

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ThreadInfo(Thread thread) {
        this.thread = thread;
        inProfilingRuntimeMethod = 1; // To make possible trace method calls while ThreadInfo is initialized return immediately
        threadId = nThreads & 0xFFFF;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public final boolean isInCallGraph() {
        return inCallGraph;
    }

    public final Thread getThread() {
        return thread;
    }

    public final int getThreadId() {
        return threadId;
    }

    static boolean isProfilerServerMonitor(Object monitor) {
        return monitor == threadInfosLock;
    }
    
    public static boolean isCurrentThreadProfilerServerThread() {
        return isProfilerServerThread(Thread.currentThread());
    }

    public static boolean isProfilerServerThread(Thread thread) {
        if (profilerServerThreads == null) {
            return false;
        }

        for (int i = 0; i < nProfilerServerThreads; i++) {
            if (profilerServerThreads[i] == thread) {
                return true;
            }
        }

        return false;
    }

    public static synchronized void addProfilerServerThread(Thread thread) {
        profilerServerThreads[nProfilerServerThreads++] = thread;
    }

    //-----------------------------------------------------------------------------------------------
    // Accounting for profiler's own threads
    //-----------------------------------------------------------------------------------------------

    // We use a simple array of Threads, not a Vector (as once before) here, since Vector's "contains()" method's
    // performance is really worse than that of simple compare in isProfilerThread() below. It's likely related to the
    // fact that contains() calls one or two other methods and uses "equals()" for compares, rather than simple "==".
    // Otherwise these methods don't have to be very sophisticated, since there are not going to be more than 3-6 such
    // threads. Also, during CPU profiling each of them is called only once, when a ThreadInfo is created for the
    // corresponding thread. A field is marked accordingly in ThreadInfo, and then checked in each methodEntry/Exit().
    public static synchronized void clearProfilerServerThreads() {
        if (profilerServerThreads == null) {
            profilerServerThreads = new Thread[10];
        } else {
            for (int i = 0; i < nProfilerServerThreads; i++) {
                profilerServerThreads[i] = null;
            }

            nProfilerServerThreads = 0;
        }
    }

    public static boolean profilingSuspended() {
        return profilingSuspended;
    }

    public static synchronized void removeProfilerServerThread(Thread thread) {
        if (profilerServerThreads == null) {
            return;
        }

        for (int i = 0; i < nProfilerServerThreads; i++) {
            if (profilerServerThreads[i] == thread) {
                if (i == (nProfilerServerThreads - 1)) {
                    profilerServerThreads[i] = null;
                } else {
                    System.arraycopy(profilerServerThreads, i + 1, profilerServerThreads, i, nProfilerServerThreads - i - 1);
                }

                nProfilerServerThreads--;

                return;
            }
        }
    }

    static int getLiveServerThreads() {
        int serverThreads = 0;
        for (int i = 0; i < nProfilerServerThreads; i++) {
            if (profilerServerThreads[i].isAlive()) {
                serverThreads++;
            }
        }
        return serverThreads;
    }

    public static void resumeProfiling() {
        profilingSuspended = false;
    }

    public static void suspendProfiling() {
        profilingSuspended = true;
    }

    static byte[] getCurrentLivenessStatus() {
        ThreadInfo[] tis = threadInfos;
        int resLen = nThreads;
        byte[] res = new byte[resLen];

        for (int i = 0; i < tis.length; i++) {
            ThreadInfo ti = tis[i];

            if ((ti != null) && (ti.threadId < resLen) && (ti.thread != null)) {
                // We don't care if a new thread was created, but we don't list it - at the tool side the currently observed
                // number of threads is also <= nThreads
                res[ti.threadId] = ti.thread.isAlive() ? (byte) 1 : 0;
            }
        }

        return res;
    }

    static void setDefaultEvBufParams() {
        evBufSize = MAX_EVENT_ENTRIES_IN_LOCAL_BUFFER * MAX_EVENT_SIZE;
        evBufPosThreshold = evBufSize - (4 * MAX_EVENT_SIZE) - 1;
        threadInfos = new ThreadInfo[1]; // To avoid null checks
        threadInfosSize = 0;
    }

    final boolean isInitialized() {
        return initialized;
    }

    final void initialize() {
        initialize(false);
    }

    final void initialize(boolean trackResultsAvailability) {
        inProfilingRuntimeMethod++;

        if (!isProfilerServerThread(thread)) {
            if (trackResultsAvailability && (nProfiledAppThreads == 0)) {
                ProfilerServer.notifyClientOnResultsAvailability();
            }

            nProfiledAppThreads++;
        }

        resetInternalState();
        initialized = true;
        inProfilingRuntimeMethod--;
    }

    final void useEventBuffer() {
        evBuf = new byte[evBufSize];
    }

    static int getNProfiledAppThreads() {
        return nProfiledAppThreads;
    }

    static void setSampleDueForAllThreads() {
        ThreadInfo[] tis = threadInfos;

        for (int i = 0; i < tis.length; i++) {
            ThreadInfo ti = tis[i];

            if (ti != null) { // We don't care if a new thread was created, but we don't list it
                ti.sampleDue = true;
            }
        }
    }

    static ThreadInfo getThreadInfo() {
        Thread thread = Thread.currentThread();
        ThreadInfo ti = lastThreadInfo;

        if (ti.thread == thread) {
            return ti;
        }

        return getThreadInfo(thread);
    }

    static ThreadInfo getThreadInfo(Thread thread) {
        ThreadInfo ti = getThreadInfoOrNull(thread);

        if (ti == null) {
            ti = newThreadInfo(thread);
        }

        return ti;
    }

    static ThreadInfo[] getThreadInfos() {
        return threadInfos;
    }

    static void changeAllThreadsInProfRuntimeMethodStatus(int val) {
        synchronized (threadInfosLock) {
            for (int i = 0; i < threadInfos.length; i++) {
                ThreadInfo ti = threadInfos[i];

                if (!ti.inCallGraph) {
                    continue;
                }

                ti.inProfilingRuntimeMethod += val;
            }
        }
    }

    static void resetThreadInfoTable() {
        ThreadInfo[] oldTIs = threadInfos;

        synchronized (threadInfosLock) {
            nProfiledAppThreads = 0;
            lastThreadInfo = dummyThreadInfo; // To avoid null checks

            for (int i = 0; i < oldTIs.length; i++) {
                ThreadInfo ti = oldTIs[i];

                if ((ti == null) || (ti.thread == null)) {
                    oldTIs[i] = null;

                    continue;
                }

                ti.initialized = false;
            }
        }
    }

    //-----------------------------------------------------------------------------------------------
    // Special methods for non-standard usage of ThreadInfo
    //-----------------------------------------------------------------------------------------------

    // This method is used only by ProfilerCalibrator
    void setEvBuf(byte[] buf) {
        evBuf = buf;
        evBufSize = buf.length;
        evBufPosThreshold = buf.length - (2 * MAX_EVENT_SIZE) - 1;
    }

    static void releaseDeadThreads() {
        ThreadInfo[] tis = threadInfos;

        for (int i = 0; i < tis.length; i++) {
            ThreadInfo ti = tis[i];

            if (ti != null) {
                Thread t = ti.thread;

                if ((t != null) && !t.isAlive()) {
                    if (ti.evBuf != null) {
                        if (ti.evBufPos > 0) { // dump local event buffer
                            ProfilerRuntimeCPU.copyLocalBuffer(ti);
                        }
                        ti.evBuf = null; // release results buffer
                    }
                    ti.thread = null; // release dead thread
                    hasDeadThreads = true;
                }
            }
        }
    }

    private static int getThreadHashCode(Thread t) {
        return System.identityHashCode(t) & 0x7fffffff;
    }

    private static ThreadInfo getThreadInfoOrNull(Thread thread) {
        ThreadInfo[] tis = threadInfos;
        int capacity = tis.length;
        int pos = getThreadHashCode(thread) % capacity;
        ThreadInfo ti;

        while ((ti = tis[pos]) != null) {
            if (ti.thread == thread) {
                return ti;
            } else {
                pos = (pos + 1) % capacity;
            }
        }

        return null;
    }

    private static void addThreadInfo(final ThreadInfo res, final Thread thread) {
        if (threadInfosSize >= ((threadInfos.length * 3) / 4)) {
            rehash();
        }

        int capacity = threadInfos.length;
        int pos = getThreadHashCode(thread) % capacity;

        while (threadInfos[pos] != null) {
            pos = (pos + 1) % capacity;
        }

        threadInfos[pos] = res;
        threadInfosSize++;
    }

    private static ThreadInfo newThreadInfo(Thread thread) {
        synchronized (threadInfosLock) {
            ThreadInfo ti = getThreadInfoOrNull(thread);

            if (ti != null) {
                return ti;
            }

            ThreadInfo res = new ThreadInfo(thread);

            nThreads++;
            addThreadInfo(res, thread);
            res.inProfilingRuntimeMethod = 0;

            return res;
        }
    }

    private static void rehash() {
        int capacity = hasDeadThreads ? threadInfos.length : (threadInfos.length * 2) + 1;
        ThreadInfo[] newTIs = new ThreadInfo[capacity];
        int size = 0;

        for (int i = 0; i < threadInfos.length; i++) {
            ThreadInfo ti = threadInfos[i];

            if ((ti == null) || (ti.thread == null)) {
                continue;
            }

            int pos = getThreadHashCode(ti.thread) % capacity;

            while (newTIs[pos] != null) {
                pos = (pos + 1) % capacity;
            }

            newTIs[pos] = ti;
            size++;
        }

        threadInfos = newTIs;
        threadInfosSize = size;
        hasDeadThreads = false;
    }

    private void resetInternalState() {
        evBufPos = evBufDumpLastPos = 0;
        absEntryTime = lastWaitStartTime = threadEntryTime = 0;
        rootMethodStackDepth = stackDepth = 0;
        inCallGraph = sampleDue = false;
        evBuf = null;
    }
}

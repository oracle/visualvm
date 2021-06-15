/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.locks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.global.TransactionalSupport;
import org.graalvm.visualvm.lib.jfluid.results.BaseCallGraphBuilder;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUProfilingResultListener;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryProfilingResultsListener;

/**
 *
 * @author Tomas Hurka
 */
public class LockGraphBuilder extends BaseCallGraphBuilder implements LockProfilingResultListener, LockCCTProvider {

    static final Logger LOG = Logger.getLogger(LockGraphBuilder.class.getName());
    final private ThreadInfos threadInfos = new ThreadInfos();
    private Map<Integer, MonitorInfo> monitorInfos = new HashMap();
    private final TransactionalSupport transaction = new TransactionalSupport();

    @Override
    protected RuntimeCCTNode getAppRootNode() {
        Map<ThreadInfo, List<List<ThreadInfo.MonitorDetail>>> threadsCopy = new HashMap(threadInfos.threadInfos.length);
        Map<MonitorInfo, List<List<MonitorInfo.ThreadDetail>>> monitorsCopy = new HashMap(monitorInfos.size());

        for (ThreadInfo ti : threadInfos.threadInfos) {
            if (ti != null) {
                List<List<ThreadInfo.MonitorDetail>> monitors = new ArrayList(2);

                if (!ti.isEmpty()) {
                    monitors.add(ti.cloneWaitMonitorDetails());
                    monitors.add(ti.cloneOwnerMonitorDetails());
                    threadsCopy.put(ti, monitors);
                }
            }
        }
        for (MonitorInfo mi : monitorInfos.values()) {
            List<List<MonitorInfo.ThreadDetail>> threads = new ArrayList(2);
            
            threads.add(mi.cloneWaitThreadDetails());
            threads.add(mi.cloneOwnerThreadDetails());
            monitorsCopy.put(mi, threads);
        }
        return new LockRuntimeCCTNode(threadsCopy, monitorsCopy);
    }

    @Override
    protected void doBatchStart() {
        transaction.beginTrans(true);
    }

    @Override
    protected void doBatchStop() {
        transaction.endTrans();
    }

    @Override
    protected void doReset() {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Do Reset called");
        }
        boolean threadLocked = transaction.beginTrans(true, true);

        if (threadLocked) { // ignore request for reset received durin an ongoing active transaction

            try {
                threadInfos.reset();
                monitorInfos = new HashMap();
            } finally {
                transaction.endTrans();
            }
        }
    }

    @Override
    protected void doShutdown() {
        threadInfos.reset();
        monitorInfos = new HashMap();
    }

    @Override
    protected void doStartup(ProfilerClient profilerClient) {
        // do nothing
    }

    @Override
    public void monitorEntry(int threadId, long timeStamp0, long timeStamp1, int monitorId, int ownerThreadId) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Monitor entry thread id = {0}, mId = {1}, owner id = {2}", new Object[]{threadId, Integer.toHexString(monitorId), ownerThreadId});
        }
        MonitorInfo m = getMonitorInfo(monitorId);
        ThreadInfo ownerTi = getThreadInfo(ownerThreadId);
        assert ownerTi != null;
        ti.openMonitor(ownerTi, m, timeStamp0);
        m.openThread(ti, ownerTi, timeStamp0);
    }

    @Override
    public void monitorExit(int threadId, long timeStamp0, long timeStamp1, int monitorId) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Monitor exit thread id = {0}, mId = {1}", new Object[]{threadId, Integer.toHexString(monitorId)});
        }
        MonitorInfo m = getMonitorInfo(monitorId);
        ti.closeMonitor(m, timeStamp0);
        m.closeThread(ti, timeStamp0);
        batchNotEmpty = true;
    }

    @Override
    public void newThread(int threadId, String threadName, String threadClassName) {
        if (!isReady()) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "New thread creation for thread id = {0}, name = {1}", new Object[]{threadId, threadName});
        }
        threadInfos.newThreadInfo(threadId, threadName, threadClassName);
    }

    @Override
    public void newMonitor(int hash, String className) {
        if (!isReady()) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "New monitor creation, mId = {0}, className = {1}", new Object[]{Integer.toHexString(hash), className});
        }
        registerNewMonitor(hash,className);
    }

/*
    @Override
    public void sleepEntry(int threadId, long timeStamp0, long timeStamp1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Sleep entry thread id = {0}", threadId);
        }
    }

    @Override
    public void sleepExit(int threadId, long timeStamp0, long timeStamp1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Sleep exit thread id = {0}", threadId);
        }
    }
*/

    public void profilingPoint(final int threadId, final int ppId, final long timeStamp) {
        // do nothing
    }

    @Override
    public void timeAdjust(int threadId, long timeDiff0, long timeDiff1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Time adjust thread id = {0}, time = {1}, CPU time = {2}", new Object[]{threadId, timeDiff0, timeDiff1});
        }
        ti.timeAdjust(timeDiff0);
    }
/*
    @Override
    public void waitEntry(int threadId, long timeStamp0, long timeStamp1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Wait entry thread id = {0}", threadId);
        }
    }

    @Override
    public void waitExit(int threadId, long timeStamp0, long timeStamp1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Wait exit thread id = {0}", threadId);
        }
    }

    @Override
    public void parkEntry(int threadId, long timeStamp0, long timeStamp1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Park entry thread id = {0}", threadId);
        }
    }

    @Override
    public void parkExit(int threadId, long timeStamp0, long timeStamp1) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Park entry thread id = {0}", threadId);
        }
    }
*/
    private boolean isReady() {
        return (status != null);
    }

    private ThreadInfo getThreadInfo(int threadId) {
        if (!isReady()) {
            return null;
        }

        return threadInfos.getThreadInfo(threadId);
    }

    private MonitorInfo getMonitorInfo(int monitorId) {
        Integer mid = new Integer(monitorId);
        MonitorInfo mi = monitorInfos.get(mid);
        if (mi == null) {
            mi = new MonitorInfo(monitorId);
            monitorInfos.put(mid, mi);
        }
        return mi;
    }

    private void registerNewMonitor(int monitorId, String className) {
        Integer mid = new Integer(monitorId);
        MonitorInfo mi = monitorInfos.get(mid);
        if (mi == null) {
            mi = new MonitorInfo(monitorId,className);
            monitorInfos.put(mid, mi);        
        } else {
            mi.setClassName(className);
        }
    }
    
    public static final class CPULockGraphBuilder extends LockGraphBuilder implements CPUProfilingResultListener {

        @Override
        public void methodEntry(int methodId, int threadId, int methodType, long timeStamp0, long timeStamp1, List parameters, int[] methoIds) {
        }

        @Override
        public void methodEntryUnstamped(int methodId, int threadId, int methodType, List parameters, int[] methoIds) {
        }

        @Override
        public void methodExit(int methodId, int threadId, int methodType, long timeStamp0, long timeStamp1, Object retVal) {
        }

        @Override
        public void methodExitUnstamped(int methodId, int threadId, int methodType) {
        }

        @Override
        public void servletRequest(int threadId, int requestType, String servletPath, int sessionId) {
        }

        @Override
        public void sleepEntry(int threadId, long timeStamp0, long timeStamp1) {
        }

        @Override
        public void sleepExit(int threadId, long timeStamp0, long timeStamp1) {
        }

        @Override
        public void threadsResume(long timeStamp0, long timeStamp1) {
        }

        @Override
        public void threadsSuspend(long timeStamp0, long timeStamp1) {
        }

        @Override
        public void waitEntry(int threadId, long timeStamp0, long timeStamp1) {
        }

        @Override
        public void waitExit(int threadId, long timeStamp0, long timeStamp1) {
        }

        @Override
        public void parkEntry(int threadId, long timeStamp0, long timeStamp1) {
        }

        @Override
        public void parkExit(int threadId, long timeStamp0, long timeStamp1) {
        }
        
    }
    public static final class MemoryLockGraphBuilder extends LockGraphBuilder implements MemoryProfilingResultsListener {

        @Override
        public void onAllocStackTrace(char classId, long objSize, int[] methodIds) {
        }

        @Override
        public void onGcPerformed(char classId, long objectId, int objEpoch) {
        }

        @Override
        public void onLivenessStackTrace(char classId, long objectId, int objEpoch, long objSize, int[] methodIds) {
        }
        
    }
}

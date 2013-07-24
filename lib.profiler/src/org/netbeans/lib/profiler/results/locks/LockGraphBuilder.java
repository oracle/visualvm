/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.locks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.global.TransactionalSupport;
import org.netbeans.lib.profiler.results.BaseCallGraphBuilder;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUProfilingResultListener;
import org.netbeans.lib.profiler.results.memory.MemoryProfilingResultsListener;

/**
 *
 * @author Tomas Hurka
 */
public class LockGraphBuilder extends BaseCallGraphBuilder implements LockProfilingResultListener, LockCCTProvider {

    static final Logger LOG = Logger.getLogger(LockGraphBuilder.class.getName());
    final private ThreadInfos threadInfos = new ThreadInfos();
    private Map<Integer, MonitorInfo> monitorInfos = new HashMap();
    private TransactionalSupport transaction = new TransactionalSupport();

    @Override
    protected RuntimeCCTNode getAppRootNode() {
        Map<ThreadInfo, List<ThreadInfo.Monitor>> threadsCopy = new HashMap(threadInfos.threadInfos.length);
        Map<MonitorInfo, List<MonitorInfo.ThreadDetail>> monitorsCopy = new HashMap(monitorInfos.size());

        for (ThreadInfo ti : threadInfos.threadInfos) {
            if (ti != null) {
                threadsCopy.put(ti, ti.cloneMonitorDetails());
            }
        }
        for (MonitorInfo mi : monitorInfos.values()) {
            monitorsCopy.put(mi, mi.cloneThreadDetails());
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
    }

    @Override
    protected void doStartup(ProfilerClient profilerClient) {
        // do nothing
    }

    @Override
    public void monitorEntry(int threadId, long timeStamp0, long timeStamp1, int monitorId) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Monitor entry thread id = {0}, id = {1}", new Object[]{threadId, Integer.toHexString(monitorId)});
        }
        MonitorInfo m = getMonitorInfo(monitorId);
        ti.openMonitor(m, timeStamp0);
    }

    @Override
    public void monitorExit(int threadId, long timeStamp0, long timeStamp1, int monitorId) {
        ThreadInfo ti = getThreadInfo(threadId);

        if (ti == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Monitor exit thread id = {0}, id = {1}", new Object[]{threadId, Integer.toHexString(monitorId)});
        }
        MonitorInfo m = getMonitorInfo(monitorId);
        ti.closeMonitor(m, timeStamp0);
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
            LOG.log(Level.FINEST, "New monitor creation, mId = {0}, className = {1}", new Object[]{hash, className});
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
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return null;
        }

        return threadInfos.threadInfos[threadId];
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
        public void methodEntry(int methodId, int threadId, int methodType, long timeStamp0, long timeStamp1) {
        }

        @Override
        public void methodEntryUnstamped(int methodId, int threadId, int methodType) {
        }

        @Override
        public void methodExit(int methodId, int threadId, int methodType, long timeStamp0, long timeStamp1) {
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

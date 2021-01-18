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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.client.ProfilingPointsProcessor;
import org.graalvm.visualvm.lib.jfluid.client.RuntimeProfilingPoint;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import org.graalvm.visualvm.lib.jfluid.results.BaseCallGraphBuilder;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNodeProcessor;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.ServletRequestCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.SimpleCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.ThreadCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.TimedCPUCCTNode;


/**
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Jaroslav Bachorik
 */
public class CPUCallGraphBuilder extends BaseCallGraphBuilder implements CPUProfilingResultListener, CPUCCTProvider {

    private class DebugInfoCollector extends RuntimeCCTNodeProcessor.PluginAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private StringBuffer buffer = new StringBuffer();

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public DebugInfoCollector() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public StringBuffer getBuffer() {
            return buffer;
        }

        public synchronized String getInfo(RuntimeCPUCCTNode node) {
            buffer = new StringBuffer();
            RuntimeCCTNodeProcessor.process(node, this);

            return buffer.toString();
        }

        @Override
        public void onNode(MethodCPUCCTNode node) {
            buffer.append(debugMethod(node.getMethodId()));
        }
        @Override
        public void onNode(ServletRequestCPUCCTNode node) {
            buffer.append("Boundary"); // NOI18N
        }
        
        @Override
        public void onNode(ThreadCPUCCTNode node) {
            buffer.append("threadId = ").append(node.getThreadId()); // NOI18N
        }
        
        @Override
        public void onNode(MarkedCPUCCTNode node) {
            buffer.append("Category ").append(node.getMark()); // NOI18N
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DebugInfoCollector debugCollector = null;
    private InstrumentationFilter instrFilter;
    private boolean stackIntegrityViolationReported;
    private long delta;

    private MethodInfoMapper methodInfoMapper = MethodInfoMapper.DEFAULT;
    private TimingAdjusterOld timingAdjuster = TimingAdjusterOld.getDefault();
    final private ThreadInfos threadInfos = new ThreadInfos();

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public CPUCCTContainer[] createPresentationCCTs(CPUResultsSnapshot cpuSnapshot) {
        threadInfos.beginTrans(false);

        try {
            // process the ThreadInfo data structure to create a CCT presentation
            String[] threadNames = threadInfos.getThreadNames();

            // There is a chance that the data has not been initialized yet
            if (threadNames == null) {
                return null;
            }

            int len = threadNames.length;

            if (len == 0) {
                return null;
            }

            List ccts = new ArrayList(len);

            int threadId = 0;

            for (int i = 0; i < len; i++) {
                ThreadInfo ti = threadInfos.threadInfos[i];

                if ((ti == null) || (ti.stack[0] == null)) {
                    continue; // Can happen if thread just created, but nothing has been executed on its behalf yet
                }

                // Fix the problem with inconsistent thread times that otherwise will occur for e.g. threads sitting in wait()
                // for long enough time when "get results" is pressed
                applyDiffAtGetResultsMoment(ti);
                double[] activeTimes = calculateThreadActiveTimes(ti);

                TimedCPUCCTNode rootNode = ti.stack[0];

                CPUCCTContainer cct = new CPUCCTContainer(rootNode, cpuSnapshot, methodInfoMapper, timingAdjuster, 
                                                          instrFilter, ti.totalNNodes, activeTimes, threadId++, threadNames[i]);

                if ((cct.rootNode != null) && (cct.rootNode.getNChildren() > 0)) {
                    ccts.add(cct);
                }
                undoDiffAtGetResultsMoment(ti);
            }

            return (CPUCCTContainer[]) ccts.toArray(new CPUCCTContainer[0]);
        } finally {
            threadInfos.endTrans();
        }
    }

    public void setMethodInfoMapper(MethodInfoMapper mapper) {
        this.methodInfoMapper = mapper != null ? mapper : MethodInfoMapper.DEFAULT;
    }

    protected boolean isCollectingTwoTimeStamps() {
        return status.collectingTwoTimeStamps();
    }

    /** See the comment to ThreadInfo.diffAtGetResultsMoment field. */
    protected void applyDiffAtGetResultsMoment(ThreadInfo ti) {
    }

    /**
     * See the comment to ThreadInfo.diffAtGetResultsMoment field. When we resume data processing for the given thread,
     * we need to undo the effect of applyDiffAtGetResultsMoment.
     */
    protected void undoDiffAtGetResultsMoment(ThreadInfo ti) {
    }

    protected long getDumpAbsTimeStamp() {
        return status.dumpAbsTimeStamp;
    }

    public void methodEntry(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                            final long timeStamp1, final List parameters, int[] methodIds) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];

        if (ti == null) {
            return;
        }

        switch (methodType) {
            case METHODTYPE_NORMAL: {
                plainMethodEntry(methodId, ti, timeStamp0, timeStamp1);

                break;
            }
            case METHODTYPE_ROOT: {
                rootMethodEntry(methodId, ti, timeStamp0, timeStamp1);

                break;
            }
            case METHODTYPE_MARKER: {
                markerMethodEntry(methodId, ti, timeStamp0, timeStamp1, parameters, methodIds);

                break;
            }
        }

        batchNotEmpty = true;
    }

    public void methodEntryUnstamped(final int methodId, final int threadId, final int methodType, final List parameters, final int[] methodIds) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];

        if (ti == null) {
            return;
        }

        switch (methodType) {
            case METHODTYPE_NORMAL: {
                plainMethodEntry(methodId, ti);

                break;
            }
            case METHODTYPE_MARKER: {
                markerMethodEntry(methodId, ti, parameters, methodIds);

                break;
            }
        }

        batchNotEmpty = true;
    }

    public void methodExit(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                           final long timeStamp1, final Object retVal) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];

        if (ti == null) {
            return;
        }

        TimedCPUCCTNode oldNode = null;

        switch (methodType) {
            case METHODTYPE_MARKER:
            case METHODTYPE_NORMAL: {
                oldNode = plainMethodExit(methodId, ti, timeStamp0, timeStamp1);

                break;
            }
            case METHODTYPE_ROOT: {
                oldNode = rootMethodExit(methodId, ti, timeStamp0, timeStamp1);

                break;
            }
        }

        if (oldNode != null) {
            TimedCPUCCTNode oneMoreNode = ti.peek();

            // category must go with a method node; so close them together
            if (oneMoreNode instanceof MarkedCPUCCTNode) {
                //        oneMoreNode.addNCalls(oldNode.getNCalls());
                //        oneMoreNode.addNetTime0(oldNode.getNetTime0());
                //        oneMoreNode.addNetTime1(oldNode.getNetTime1());
                //        oneMoreNode.addSleepTime0(oldNode.getSleepTime0());
                //        oneMoreNode.addWaitTime0(oldNode.getWaitTime0());
                ti.pop();
                oneMoreNode = ti.peek();
            }
            // Servlet node must go with a method node; so close them together
            if (oneMoreNode instanceof ServletRequestCPUCCTNode) {
                //        oneMoreNode.addNCalls(oldNode.getNCalls());
                //        oneMoreNode.addNetTime0(oldNode.getNetTime0());
                //        oneMoreNode.addNetTime1(oldNode.getNetTime1());
                //        oneMoreNode.addSleepTime0(oldNode.getSleepTime0());
                //        oneMoreNode.addWaitTime0(oldNode.getWaitTime0());
                ti.pop();
            }        
        }

        batchNotEmpty = true;
    }

    public void methodExitUnstamped(final int methodId, final int threadId, final int methodType) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];

        if (ti == null) {
            return;
        }

        TimedCPUCCTNode oldNode = null;

        switch (methodType) {
            case METHODTYPE_MARKER:
            case METHODTYPE_NORMAL: {
                oldNode = plainMethodExit(methodId, ti);

                break;
            }
        }

        if (oldNode != null) {
            TimedCPUCCTNode oneMoreNode = ti.peek();

            // category must go with a method node; so close them together
            if (oneMoreNode instanceof MarkedCPUCCTNode) {
                //        oneMoreNode.addNCalls(oldNode.getNCalls());
                //        oneMoreNode.addNetTime0(oldNode.getNetTime0());
                //        oneMoreNode.addNetTime1(oldNode.getNetTime1());
                //        oneMoreNode.addSleepTime0(oldNode.getSleepTime0());
                //        oneMoreNode.addWaitTime0(oldNode.getWaitTime0());
                ti.pop();
                oneMoreNode = ti.peek();
            }
            // Servlet node must go with a method node; so close them together
            if (oneMoreNode instanceof ServletRequestCPUCCTNode) {
                //        oneMoreNode.addNCalls(oldNode.getNCalls());
                //        oneMoreNode.addNetTime0(oldNode.getNetTime0());
                //        oneMoreNode.addNetTime1(oldNode.getNetTime1());
                //        oneMoreNode.addSleepTime0(oldNode.getSleepTime0());
                //        oneMoreNode.addWaitTime0(oldNode.getWaitTime0());
                ti.pop();
            }
        }

        batchNotEmpty = true;
    }

    public void monitorEntry(final int threadId, final long timeStamp0, final long timeStamp1, final int monitorId, int ownerThreadId) {
        waitEntry(threadId, timeStamp0, timeStamp1);
        batchNotEmpty = true;
    }

    public void monitorExit(final int threadId, final long timeStamp0, final long timeStamp1, final int monitorId) {
        waitExit(threadId, timeStamp0, timeStamp1);
        batchNotEmpty = true;
    }

    public void newThread(final int threadId, final String threadName, final String threadClassName) {
        if (!isReady()) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "New thread creation for thread id = {0}, name = {1}", new Object[]{threadId, threadName});
        }

        threadInfos.newThreadInfo(threadId, threadName, threadClassName);
        batchNotEmpty = true;
    }

    public void newMonitor(int hash, String className) {
        if (!isReady()) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "New monitor creation, mId = {0}, className = {1}", new Object[]{Integer.toHexString(hash), className});
        }
    }

    public void servletRequest(final int threadId, final int requestType, final String servletPath, final int sessionId) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }
        
        ThreadInfo ti = threadInfos.threadInfos[threadId];

        if (ti == null) {
            return;
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (curNode == null) {
            curNode = new ThreadCPUCCTNode(threadId);
            ti.totalNNodes++;
            ti.push(curNode);
            ti.totalNInv--;
        }

        ServletRequestCPUCCTNode servletNode = ServletRequestCPUCCTNode.Locator.locate(requestType, servletPath,
                                                                                       curNode.getChildren());

        if (servletNode == null) {
            servletNode = new ServletRequestCPUCCTNode(requestType, servletPath);
            curNode.attachNodeAsChild(servletNode);
        }

        ti.push(servletNode);
    }

    public void sleepEntry(final int threadId, long timeStamp0, long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.peek();
        if (curNode == null) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("ENTRY SLEEP: " // + debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", tid: " + ti.threadId // NOI18N
                          );
            delta = timeStamp0;
        }

        long diff = timeStamp0 - ti.topMethodEntryTime0;

        if (diff > 0) {
            curNode.addNetTime0(diff);
        } else {
            timeStamp0 = ti.topMethodEntryTime0;
        }

        ti.topMethodEntryTime0 = timeStamp0;

        curNode.setLastWaitOrSleepStamp(timeStamp0);
        batchNotEmpty = true;
    }

    public void sleepExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.peek();
        if (curNode == null) {
            return;
        }

        long lastSleep = timeStamp0 - curNode.getLastWaitOrSleepStamp();

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("EXIT SLEEP: " //+ debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", slept: " + lastSleep // NOI18N
                          + ", tid: " + ti.threadId // NOI18N
                          );
            delta = timeStamp0;
            lastSleep = 0;
        }

        curNode.setLastWaitOrSleepStamp(0);
        curNode.addSleepTime0(lastSleep);

        // move start timer for current method, so that the time spent sleeping is ignored
        if ((timeStamp0 - ti.topMethodEntryTime0) > 0) {
            ti.topMethodEntryTime0 = timeStamp0;
        }

        batchNotEmpty = true;
    }

    public void threadsResume(final long timeStamp0, final long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo[] tis = threadInfos.threadInfos;

        for (int i = 0; i < tis.length; i++) {
            ThreadInfo ti = tis[i];

            if ((ti == null) || (ti.stackTopIdx < 0)) {
                continue;
            }

            ti.topMethodEntryTime0 = timeStamp0;

            if (isCollectingTwoTimeStamps()) {
                ti.topMethodEntryTime1 = timeStamp1;
            }

            if (isCollectingTwoTimeStamps()) {
                ti.rootMethodEntryTimeAbs = timeStamp0;
                ti.rootMethodEntryTimeThreadCPU = timeStamp1;
            } else {
                ti.rootMethodEntryTimeAbs = timeStamp0;

                // rootMethodEntryTimeThreadCPU can remain the same - thread was suspended and time wasn't increasing
            }
        }

        batchNotEmpty = true;
    }

    public void threadsSuspend(final long timeStamp0, final long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo[] tis = threadInfos.threadInfos;

        for (int i = 0; i < tis.length; i++) {
            ThreadInfo ti = tis[i];

            if ((ti == null) || (ti.stackTopIdx < 0)) {
                continue;
            }

            TimedCPUCCTNode curNode = ti.stack[ti.stackTopIdx];

            long diff = timeStamp0 - ti.topMethodEntryTime0;

            if (diff > 0) {
                curNode.addNetTime0(diff);
            }

            if (isCollectingTwoTimeStamps()) {
                ti.rootGrossTimeAbs += (timeStamp0 - ti.rootMethodEntryTimeAbs);
                diff = timeStamp1 - ti.topMethodEntryTime1;

                if (diff > 0) {
                    curNode.addNetTime1(diff);
                }

                ti.rootGrossTimeThreadCPU += (timeStamp1 - ti.rootMethodEntryTimeThreadCPU);
            } else { // Collecting only absolute timestamps
                ti.rootGrossTimeAbs += (timeStamp0 - ti.rootMethodEntryTimeAbs);

                // Shouldn't do anything with rootGrossTimeThreadCPU, since while thread is suspended,
                // thread CPU time is stopped
            }
        }

        batchNotEmpty = true;
    }
    
    public void profilingPoint(final int threadId, final int ppId, final long timeStamp) {
        ProfilerClient client = getClient();

        if (client == null) {
            return;
        }

        final ProfilingPointsProcessor ppp = TargetAppRunner.getDefault().getProfilingPointsProcessor();

        afterBatchCommands.add(new Runnable() {
            public void run() {
                ppp.profilingPointHit(new RuntimeProfilingPoint.HitEvent(ppId, timeStamp, threadId));
            }
        });
    }

    /**
     * Called when the TA is suspended waiting for the tool to process the buffer
     */
    public void timeAdjust(final int threadId, final long timeDiff0, final long timeDiff1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        final ProfilingPointsProcessor ppp = TargetAppRunner.getDefault().getProfilingPointsProcessor();
        ThreadInfo ti = threadInfos.threadInfos[threadId];

        // In this case, time stamps are actually time adjustments.
        // timeStamp0 is always absolute and timeStamp1 is always thread CPU.
        ti.rootMethodEntryTimeAbs += timeDiff0;
        ti.rootMethodEntryTimeThreadCPU += timeDiff1;
        ti.topMethodEntryTime0 += timeDiff0;

        if (isCollectingTwoTimeStamps()) {
            ti.topMethodEntryTime1 += timeDiff1;
        }

        if (ppp != null) {
            afterBatchCommands.add(new Runnable() {
                public void run() {
                    ppp.timeAdjust(threadId, timeDiff0, timeDiff1);
                }
            });
        }
        batchNotEmpty = true;
    }

    public void waitEntry(final int threadId, long timeStamp0, long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.peek();
        if (curNode == null) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("ENTRY WAIT: " //+ debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", tid: " + ti.threadId // NOI18N
                          );
            delta = timeStamp0;
            //LOGGER.finest(dumpStack(ti));
        }

        long diff = timeStamp0 - ti.topMethodEntryTime0;

        if (diff > 0) {
            curNode.addNetTime0(diff);
        } else {
            timeStamp0 = ti.topMethodEntryTime0;
        }

        ti.topMethodEntryTime0 = timeStamp0;

        curNode.setLastWaitOrSleepStamp(timeStamp0);
        batchNotEmpty = true;
    }

    public void waitExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.peek();
        if (curNode == null) {
            return;
        }

        long lastWait = timeStamp0 - curNode.getLastWaitOrSleepStamp();
        curNode.setLastWaitOrSleepStamp(0);
        curNode.addWaitTime0(lastWait);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("EXIT WAIT: " //+ debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", waited: " + lastWait // NOI18N
                          + ", tid: " + ti.threadId // NOI18N
                          );
            delta = timeStamp0;
            //LOGGER.finest(dumpStack(ti));
        }

        // move start timer for current method, so that the time spent waiting is ignored
        if ((timeStamp0 - ti.topMethodEntryTime0) > 0) {
            ti.topMethodEntryTime0 = timeStamp0;
        }

        batchNotEmpty = true;
    }

    public void parkEntry(final int threadId, long timeStamp0, long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.peek();
        if (curNode == null) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("ENTRY PARK: " //+ debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", tid: " + ti.threadId // NOI18N
                          );
            delta = timeStamp0;
            //LOGGER.finest(dumpStack(ti));
        }

        long diff = timeStamp0 - ti.topMethodEntryTime0;

        if (diff > 0) {
            curNode.addNetTime0(diff);
        } else {
            timeStamp0 = ti.topMethodEntryTime0;
        }

        ti.topMethodEntryTime0 = timeStamp0;

        curNode.setLastWaitOrSleepStamp(timeStamp0);
        batchNotEmpty = true;
    }

    public void parkExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.peek();
        if (curNode == null) {
            return;
        }

        long lastWait = timeStamp0 - curNode.getLastWaitOrSleepStamp();
        curNode.setLastWaitOrSleepStamp(0);
        curNode.addWaitTime0(lastWait);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("EXIT PARK: " //+ debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", waited: " + lastWait // NOI18N
                          + ", tid: " + ti.threadId // NOI18N
                          );
            delta = timeStamp0;
            //LOGGER.finest(dumpStack(ti));
        }

        // move start timer for current method, so that the time spent park is ignored
        if ((timeStamp0 - ti.topMethodEntryTime0) > 0) {
            ti.topMethodEntryTime0 = timeStamp0;
        }

        batchNotEmpty = true;
    }

    /**
     * For each monitored thread, returns the current absolute and thread-local CPU time. Returned thread-local time
     * may be just -1, indicating that it can't be reliably calculated for the given thread (at this moment or at all).
     */
    protected long[][] getAllThreadsActiveTimes() {
        int len = threadInfos.getThreadNames().length;
        long[][] res = new long[2][len];

        for (int i = 0; i < len; i++) {
            ThreadInfo ti = threadInfos.threadInfos[i];
            double[] times = calculateThreadActiveTimes(ti);

            res[0][i] = (long) (((times[0] - times[2]) * 1000) / timingAdjuster.getInstrTimingData().timerCountsInSecond0);
            res[1][i] = (times[1] != -1) ? (long) (((times[1] - times[3]) * 1000) / timingAdjuster.getInstrTimingData().timerCountsInSecond1) : (-1);
        }

        return res;
    }

    protected RuntimeCCTNode getAppRootNode() {
        if (threadInfos.isEmpty()) {
            return null;
        }

        RuntimeCPUCCTNode appNode = null;

        threadInfos.beginTrans(false);

        try {
            ProfilerClient client = getClient();
            if (client != null) {
                appNode = new SimpleCPUCCTNode(client.getStatus().getNInstrMethods());                
            } else {
                appNode = new SimpleCPUCCTNode(true);
            }

            int len = (threadInfos.getThreadNames() != null) ? threadInfos.getThreadNames().length : 0;

            for (int i = 0; i < len; i++) {
                ThreadInfo ti = threadInfos.threadInfos[i];

                if ((ti == null) || (ti.stack[0] == null)) {
                    continue;
                }

                appNode.attachNodeAsChild(ti.stack[0]);
            }
        } finally {
            threadInfos.endTrans();
        }

        return appNode;
    }

    //----------------------------- Calculation of supporting numeric data ---------------------------

    /**
     * For the given thread, calculate the following values (not all may be available, depending on the active timers and
     * the current thread state (inside or outside the profiled call graph):
     * absolute gross time, thread CPU gross time, time spent in instrumentation code in absolute counts, same time in
     * thread-local time counts.
     * The values are assigned to the relevant fields of cgm parameter (if it's not null), and returned as an array of
     * doubles.
     * NOTE THAT setMethodEntryExitTimesGivenCollectedTimeStampsKinds() SHOULD BE CALLED ONCE BEFORE CALLING THIS METHOD!
     */
    double[] calculateThreadActiveTimes(ThreadInfo ti) {
        TimedCPUCCTNode rootNode = ti.stack[0];

        if (rootNode == null) {
            // May happen if thread just created, but nothing ran its behalf yet
            return new double[] { 0, 0, 0, 0 };
        }

        // Now calculate and return gross times for the whole call subgraph.
        // Note that absolute time is provided irrespective of the timers (absolute only, thread CPU only, or both) used
        // for methods.
        // Correct thread CPU time can be provided only if either thread CPU timer is used, or if execution is currently
        // not within the call graph.
        // If we can't provide a correct thread CPU time, we set cgm.wholeGraphGrossTimeThreadCPU to a negative value,
        // indicating that this time should not be displayed at all.
        long rootGrossTimeAbs = ti.rootGrossTimeAbs;

        // System.err.println("\n*** CPUCallGraphBuilder: rootGrossTimeAbs = "
        // + rootGrossTimeAbs + ", totalNInv = " + ti.totalNInv);  // NOI18N
        if (ti.stackTopIdx != -1) {
            long time0 = getDumpAbsTimeStamp();

            if (ti.topMethodEntryTime0 > time0) {
                time0 = ti.topMethodEntryTime0;
            }

            rootGrossTimeAbs += (time0 - ti.rootMethodEntryTimeAbs);

            //System.err.println("*** dumpAbsTimeStamp = " + status.dumpAbsTimeStamp + ", ti.topMethodEntryTime0 = "
            // + ti.topMethodEntryTime0 + ", ti.rootMethodEntryTimeAbs = " + ti.rootMethodEntryTimeAbs); // NOI18N
            //System.err.println("*** Adjusted rootGrossTimeAbs = " + rootGrossTimeAbs); // NOI18N
        }

        //System.err.println("*** rootGrossTimeAbs in ms = " + ((double) rootGrossTimeAbs) * 1000 / status.timerCountsInSecond[0]); // NOI18N
        long rootGrossTimeCPU = ti.rootGrossTimeThreadCPU;

        //System.err.println("*** ti.rootGrossTimeThreadCPU = " + ti.rootGrossTimeThreadCPU + ", totalNInv = " + ti.totalNInv); // NOI18N
        if (ti.stackTopIdx != -1) {
            if (isCollectingTwoTimeStamps()) {
                rootGrossTimeCPU += (ti.topMethodEntryTime1 - ti.rootMethodEntryTimeThreadCPU);

                //System.err.println("*** ti.topMethodEntryTime1 = " + ti.topMethodEntryTime1 + ", ti.rootMethodEntryTimeThreadCPU = " + ti.rootMethodEntryTimeThreadCPU);
                //System.err.println("*** adjustment for CPU time = " + (ti.topMethodEntryTime1 - ti.rootMethodEntryTimeThreadCPU)); // NOI18N
            } else {
                rootGrossTimeCPU = -1;
            }
        }

        //System.err.println("*** Adjusted rootGrossTimeCPU = " + rootGrossTimeCPU + ", in ms = " + ((double) rootGrossTimeCPU) * 1000 / status.timerCountsInSecond[1]); // NOI18N
        int nRootInv = rootNode.getNCalls();
        double timeInInjectedCodeInAbsCounts;
        double timeInInjectedCodeInThreadCPUCounts = 0;
        // Calculate timeInInjectedCodeInAbsCounts.
        timeInInjectedCodeInAbsCounts = timingAdjuster.delta(nRootInv, (int) (ti.totalNInv - nRootInv), false);

        //System.err.println("*** timeInInjectedCodeInAbsCounts = " + timeInInjectedCodeInAbsCounts + ", in ms = " + ((double) timeInInjectedCodeInAbsCounts) * 1000 / status.timerCountsInSecond[0]); // NOI18N

        // Now calculate timeInInjectedCodeInThreadCPUCounts
        if (isCollectingTwoTimeStamps()) {
            timeInInjectedCodeInThreadCPUCounts = timingAdjuster.delta(nRootInv, (int) (ti.totalNInv - nRootInv), true);
        } else { // Same calculation whether we have absoluteTimerOn == true or not
                 // Just convert the known time into thread CPU time units
            timeInInjectedCodeInThreadCPUCounts = (timeInInjectedCodeInAbsCounts * timingAdjuster.getInstrTimingData().timerCountsInSecond1) / timingAdjuster.getInstrTimingData().timerCountsInSecond0;
        }

        //System.err.println("*** timeInInjectedCodeInThreadCPUCounts = " + timeInInjectedCodeInThreadCPUCounts); // NOI18N
        return new double[] {
                   (double) rootGrossTimeAbs, (double) rootGrossTimeCPU, timeInInjectedCodeInAbsCounts,
                   timeInInjectedCodeInThreadCPUCounts
               };
    }

    protected void doBatchStart() {
        /****************************************************************************/
        /* Timing adjuster must be initialized here as in doStartup() it may happen */
        /* that the instrumentation type has not been set yet                       */
        /****************************************************************************/
        ProfilerClient client = getClient();
        if (client != null) {
            timingAdjuster = TimingAdjusterOld.getInstance(client.getStatus());
        }
        threadInfos.beginTrans(true);
    }

    protected void doBatchStop() {
        threadInfos.endTrans();
    }

    protected void doReset() {
        boolean threadLocked = threadInfos.beginTrans(true, true);

        if (threadLocked) { // ignore request for reset received during an ongoing active transaction

            try {
                threadInfos.reset();
            } finally {
                threadInfos.endTrans();
            }
        }
    }

    protected void doShutdown() {
        threadInfos.reset();
        instrFilter = null;
    }

    protected void doStartup(final ProfilerClient profilerClient) {
        instrFilter = profilerClient.getSettings().getInstrumentationFilter();
        
        setMethodInfoMapper(new MethodInfoMapper() {
            final private String INVALID_MID=ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.cpu.Bundle").getString("MSG_INVALID_METHODID"); // NOI18N
            @Override
            public String getInstrMethodClass(int methodId) {
                String[] cNames = profilerClient.getStatus().getInstrMethodClasses();
                if (methodId < cNames.length) {
                    return cNames[methodId];
                } else {
                    LOGGER.log(Level.WARNING, INVALID_MID, new Object[]{methodId, cNames.length - 1});
                    return null;
                }
            }

            @Override
            public String getInstrMethodName(int methodId) {
                String[] mNames = profilerClient.getStatus().getInstrMethodNames();
                if (methodId < mNames.length) {
                    return mNames[methodId];
                } else {
                    LOGGER.log(Level.WARNING, INVALID_MID, new Object[]{methodId, mNames.length - 1});
                    return null;
                }
            }

            @Override
            public String getInstrMethodSignature(int methodId) {
                String[] sNames = profilerClient.getStatus().getInstrMethodSignatures();
                if (methodId < sNames.length) {
                    return sNames[methodId];
                } else {
                    LOGGER.log(Level.WARNING, INVALID_MID, new Object[]{methodId, sNames.length - 1});
                    return null;
                }
            }

            @Override
            public int getMinMethodId() {
                return 1;
            }

            @Override
            public int getMaxMethodId() {
                return profilerClient.getStatus().getNInstrMethods();
            }

            @Override
            public void lock(boolean mutable) {
                profilerClient.getStatus().beginTrans(mutable);
            }

            @Override
            public void unlock() {
                profilerClient.getStatus().endTrans();
            }


        });

        profilerClient.registerCPUCCTProvider(this);
    }

    protected void setFilter(InstrumentationFilter filter) {
        this.instrFilter = filter;
    }

    private synchronized DebugInfoCollector getDebugCollector() {
        if (debugCollector == null) {
            debugCollector = new DebugInfoCollector();
        }

        return debugCollector;
    }

    protected boolean isReady() {
        return (status != null) && (instrFilter != null);
    }

    private String debugMethod(int methodId) {
        StringBuilder buffer = new StringBuilder();
        try {
            methodInfoMapper.lock(false);

            buffer.append(methodInfoMapper.getInstrMethodClass(methodId)).append('.').append(methodInfoMapper.getInstrMethodName(methodId)); // NOI18N
            buffer.append(methodInfoMapper.getInstrMethodSignature(methodId)).append(" (methodId = ").append(methodId).append(')'); // NOI18N
        } finally {
            methodInfoMapper.unlock();
        }

        return buffer.toString();
    }

    private String debugNode(RuntimeCPUCCTNode node) {
        return getDebugCollector().getInfo(node);
    }

    private String dumpStack(ThreadInfo ti) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("*** Thread stack dump:\n"); // NOI18N

        for (int i = ti.stackTopIdx; i >= 0; i--) {
            DebugInfoCollector collector = new DebugInfoCollector();
            TimedCPUCCTNode frame = ti.stack[i];
            RuntimeCCTNodeProcessor.process(frame, collector);
            buffer.append(collector.getInfo(frame)).append('\n'); // NOI18N
        }

        return buffer.toString();
    }

    private TimedCPUCCTNode markerMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1,
                                              boolean stamped, List parameters, int[] methodIds) {
//        Mark mark = MarkingEngine.getDefault().markMethod(methodId, status);
        Mark mark = Mark.DEFAULT;

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "MarkerMEntry{0} for tId = {1}, time: {2}, method:  {3}, inRoot: {4}, rootEntryTimeThread: {5}", new Object[]{(!stamped) ? "(unstamped)" : "", (int) ti.threadId, timeStamp0, debugMethod(methodId), ti.rootMethodEntryTimeAbs, ti.rootMethodEntryTimeThreadCPU});
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (curNode == null) {
            TimedCPUCCTNode rootNode = new ThreadCPUCCTNode(ti.threadId);
            ti.totalNNodes++;
            ti.push(rootNode);
            ti.totalNInv--;

            if (!mark.isDefault()) {
                curNode = new MarkedCPUCCTNode(mark);
                rootNode.attachNodeAsChild(curNode);
                ti.totalNNodes++;
                ti.push(curNode);
                rootNode = curNode;
            }

            curNode = new MethodCPUCCTNode(methodId);
            rootNode.attachNodeAsChild(curNode);
            ti.totalNNodes++;
            ti.push(curNode);

            ti.topMethodEntryTime0 = timeStamp0;

            if (isCollectingTwoTimeStamps()) {
                ti.topMethodEntryTime1 = timeStamp1;
            }
        } else {
            if (stamped) {
                long diff = timeStamp0 - ti.topMethodEntryTime0;

                if (diff > 0) {
                    curNode.addNetTime0(diff);
                } else {
                    timeStamp0 = ti.topMethodEntryTime0;
                }

                ti.topMethodEntryTime0 = timeStamp0;

                if (isCollectingTwoTimeStamps()) {
                    diff = timeStamp1 - ti.topMethodEntryTime1;

                    if (diff > 0) {
                        curNode.addNetTime1(diff);
                    } else {
                        timeStamp1 = ti.topMethodEntryTime1;
                    }

                    ti.topMethodEntryTime1 = timeStamp1;
                }
            }

            TimedCPUCCTNode calleeNode;

            if (!mark.isDefault()) {
                // try to locate the category node; or create a new node for the category
                calleeNode = MarkedCPUCCTNode.Locator.locate(mark, curNode.getChildren());

                if (calleeNode == null) {
                    calleeNode = new MarkedCPUCCTNode(mark);
                    curNode.attachNodeAsChild(calleeNode);
                    ti.totalNNodes++;
                }

                ti.push(calleeNode);
                curNode = calleeNode;
            }

            // Now find the appropriate callee in this node or create one
            calleeNode = MethodCPUCCTNode.Locator.locate(methodId, curNode.getChildren());

            if (calleeNode == null) {
                calleeNode = new MethodCPUCCTNode(methodId);
                curNode.attachNodeAsChild(calleeNode);
                ti.totalNNodes++;
            }

            ti.push(calleeNode);
            curNode = calleeNode; // make the callee node be the current one
        }

        //    curNode.setMarkID(categoryId);
        if (!ti.isInRoot()) { // not within root method CCT
            curNode.setFilteredStatus(TimedCPUCCTNode.FILTERED_YES);

            if (stamped) { // stamped marker method called before any root method; must set rootMethodEntryTimes
                           // Be careful - with MARKER_ENTRY and MARKER_EXIT events, unlike with others, timeStamp0 is *always* absolute time,
                           // and timeStamp1 is *always* thread CPU time
                ti.rootMethodEntryTimeAbs = timeStamp0;
                ti.rootMethodEntryTimeThreadCPU = timeStamp1;
                ti.topMethodEntryTime0 = timeStamp0;

                if (isCollectingTwoTimeStamps()) {
                    ti.topMethodEntryTime1 = timeStamp1;
                }
            }
        } else {
            try {
                methodInfoMapper.lock(false);
                String jvmClassName = methodInfoMapper.getInstrMethodClass(((MethodCPUCCTNode) curNode).getMethodId()).replace('.', '/'); // NOI18N
                ProfilerClient client = getClient();

                if (client != null) {
                    if (!client.getSettings().getInstrumentationFilter().passes(jvmClassName)) {
                        curNode.setFilteredStatus(TimedCPUCCTNode.FILTERED_YES);
                    }
                } else {
                    curNode.setFilteredStatus(TimedCPUCCTNode.FILTERED_YES);
                }
            } finally {
                methodInfoMapper.unlock();
            }
        }

        return curNode;
    }

    private TimedCPUCCTNode markerMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1, List parameters, int[] methodIds) {
        return markerMethodEntry(methodId, ti, timeStamp0, timeStamp1, true, parameters, methodIds);
    }

    private TimedCPUCCTNode markerMethodEntry(final int methodId, final ThreadInfo ti, List parameters, int[] methodIds) {
        return markerMethodEntry(methodId, ti, 0, 0, false, parameters, methodIds);
    }

    private TimedCPUCCTNode plainMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1,
                                             boolean stamped) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "MethodEntry {0}: for tId = {1}, time: {2}, delta: {3}, method:  {4}", new Object[]{(!stamped) ? "(unstamped)" : "", (int) ti.threadId, timeStamp0, timeStamp0 - delta, debugMethod(methodId)});
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (stamped) {
            long diff = timeStamp0 - ti.topMethodEntryTime0;

            if (diff > 0) {
                curNode.addNetTime0(diff);
            } else {
                timeStamp0 = ti.topMethodEntryTime0;
            }

            ti.topMethodEntryTime0 = timeStamp0;

            if (isCollectingTwoTimeStamps()) {
                diff = timeStamp1 - ti.topMethodEntryTime1;

                if (diff > 0) {
                    curNode.addNetTime1(diff);
                } else {
                    timeStamp1 = ti.topMethodEntryTime1;
                }

                ti.topMethodEntryTime1 = timeStamp1;
            }
        }

        // Now find the appropriate callee in this node or create one
        MethodCPUCCTNode methodNode = MethodCPUCCTNode.Locator.locate(methodId, curNode.getChildren());

        if (methodNode != null) {
            ti.push(methodNode);

            return methodNode;
        }

        // Appropriate sub-node not found, or there are no sub-nodes yet - create one
        methodNode = new MethodCPUCCTNode(methodId);
        curNode.attachNodeAsChild(methodNode);

        curNode = methodNode;

        ti.totalNNodes++;
        ti.push(curNode);

        if (!ti.isInRoot()) {
            try {
                methodInfoMapper.lock(false);
                String jvmClassName = methodInfoMapper.getInstrMethodClass(((MethodCPUCCTNode) curNode).getMethodId()).replace('.', '/'); // NOI18N
                ProfilerClient client = getClient();

                if (client != null) {
                    if (!client.getSettings().getInstrumentationFilter().passes(jvmClassName)) {
                        curNode.setFilteredStatus(TimedCPUCCTNode.FILTERED_YES);
                    }
                } else {
                    curNode.setFilteredStatus(TimedCPUCCTNode.FILTERED_YES);
                }
            } finally {
                methodInfoMapper.unlock();
            }
        }

        return curNode;
    }

    private TimedCPUCCTNode plainMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1) {
        return plainMethodEntry(methodId, ti, timeStamp0, timeStamp1, true);
    }

    private TimedCPUCCTNode plainMethodEntry(final int methodId, final ThreadInfo ti) {
        return plainMethodEntry(methodId, ti, 0, 0, false);
    }

    private TimedCPUCCTNode plainMethodExit(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1,
                                            boolean stamped) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "MethodExit  {0}: for tId = {1}, time: {2}, delta: {3}, method:  {4}", new Object[]{(!stamped) ? "(unstamped)" : "", ti.threadId, timeStamp0, timeStamp0 - delta, debugMethod(methodId)});
            delta = timeStamp0;
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (curNode == null) {
            LOGGER.severe(CommonConstants.ENGINE_WARNING + "critical: stack integrity violation on method exit.\n" // NOI18N
                          + "*** methodId on simulated stack top is unidentifiable\n"); // NOI18N

            return null;
        }

        if (!(curNode instanceof MethodCPUCCTNode)) {
            LOGGER.severe(CommonConstants.ENGINE_WARNING + "critical: stack integrity violation on method exit.\n" // NOI18N
                          + "*** methodId on simulated stack top is unidentifiable\n"); // NOI18N

            return null;
        }

        MethodCPUCCTNode methodNode = (MethodCPUCCTNode) curNode;

        if (methodId != methodNode.getMethodId()) {
            StringBuilder message = new StringBuilder();
            message.append(CommonConstants.ENGINE_WARNING).append("critical: stack integrity violation on method exit.\n"); // NOI18N
            message.append("*** methodId on simulated stack top: ").append((int) methodNode.getMethodId()); // NOI18N
            message.append(", received methodId (should match) = ").append((int) methodId).append('\n'); // NOI18N
            message.append("received method debug: ").append(debugMethod(methodId)).append('\n'); // NOI18N
            message.append(CommonConstants.PLEASE_REPORT_PROBLEM);

            if (!stackIntegrityViolationReported) {
                message.append(dumpStack(ti));
                stackIntegrityViolationReported = true;
            }

            message.append('\n'); // NOI18N
            LOGGER.severe(message.toString());

            return null;
        }

        // Timer's coarse granularities etc. may occasionally cause this issue. FIXME: maybe need a warning, though not
        // every time this happens, but probably rather in the end of run, so that the problem could then be investigated.
        if (stamped) {
            long diff = timeStamp0 - ti.topMethodEntryTime0;

            if (diff > 0) {
                curNode.addNetTime0(diff);
            } else {
                timeStamp0 = ti.topMethodEntryTime0;
            }

            if (isCollectingTwoTimeStamps()) {
                diff = timeStamp1 - ti.topMethodEntryTime1;

                if (diff > 0) {
                    curNode.addNetTime1(diff);
                } else {
                    timeStamp1 = ti.topMethodEntryTime1;
                }
            }
        }

        TimedCPUCCTNode oldNode = ti.pop();

        //    if ((ti.stackTopIdx < 0 || ti.stack[ti.stackTopIdx].getMethodId() == 0) && checkStack) {
        //      System.err.println(ENGINE_WARNING + "critical: stack state on methodExit is like at rootMethodExit"); // NOI18N
        //      System.err.println(PLEASE_REPORT_PROBLEM);
        //    }
        // Resume the net time for the caller
        if (stamped) {
            ti.topMethodEntryTime0 = timeStamp0;

            if (isCollectingTwoTimeStamps()) {
                ti.topMethodEntryTime1 = timeStamp1;
            }
        }

        return oldNode;
    }

    private TimedCPUCCTNode plainMethodExit(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1) {
        return plainMethodExit(methodId, ti, timeStamp0, timeStamp1, true);
    }

    private TimedCPUCCTNode plainMethodExit(final int methodId, final ThreadInfo ti) {
        return plainMethodExit(methodId, ti, 0, 0, false);
    }

    private TimedCPUCCTNode rootMethodEntry(final int methodId, final ThreadInfo ti, final long timeStamp0, final long timeStamp1) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "RootMEntry for tId = {0}, time: {1}, method:  {2}", new Object[]{(int) ti.threadId, timeStamp0, debugMethod(methodId)});
        }

//        Mark mark = MarkingEngine.getDefault().markMethod(methodId, status);
        Mark mark = Mark.DEFAULT;

        TimedCPUCCTNode curNode = ti.peek();

        if (ti.isInRoot()) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(CommonConstants.ENGINE_WARNING)
                  .append("critical: at root method entry thread stack is not at 0 - should not happen!\n"); // NOI18N
            buffer.append("*** thread = ").append(threadInfos.threadNames[ti.threadId]); // NOI18N
            buffer.append(", ti.stackTopIdx = ").append(ti.stackTopIdx); // NOI18N

            if (curNode != null) {
                buffer.append(", curNode = ").append(curNode).append('\n'); // NOI18N
            }

            buffer.append(CommonConstants.PLEASE_REPORT_PROBLEM);
            LOGGER.severe(buffer.toString());
        }

        if (curNode == null) { // no node on stack

            TimedCPUCCTNode rootNode = new ThreadCPUCCTNode(ti.threadId); // create a new thread node
            ti.totalNNodes++;
            ti.push(rootNode); // and place it on the stack
            ti.totalNInv--;

            if (!mark.isDefault()) {
                curNode = new MarkedCPUCCTNode(mark);
                rootNode.attachNodeAsChild(curNode);
                ti.totalNNodes++;
                ti.push(curNode);
                rootNode = curNode;
            }

            curNode = new MethodCPUCCTNode(methodId); // now create the root method node
            rootNode.attachNodeAsChild(curNode); // and attach it to the previously created thread node
            ti.totalNNodes++;
        } else {
            TimedCPUCCTNode calleeNode;

            if (!mark.isDefault()) {
                // try to locate the category node; or create a new node for the category
                calleeNode = MarkedCPUCCTNode.Locator.locate(mark, curNode.getChildren());

                if (calleeNode == null) {
                    calleeNode = new MarkedCPUCCTNode(mark);
                    curNode.attachNodeAsChild(calleeNode);
                    ti.totalNNodes++;
                }

                ti.push(calleeNode);
                curNode = calleeNode;
            }

            calleeNode = MethodCPUCCTNode.Locator.locate(methodId, curNode.getChildren());

            if (calleeNode == null) {
                calleeNode = new MethodCPUCCTNode(methodId);
                curNode.attachNodeAsChild(calleeNode);
                ti.totalNNodes++;
            }

            curNode = calleeNode;
        }

        ti.push(curNode); // make the new node the current one
                          //    curNode.setMarkID(categoryId);

        // Be careful - with ROOT_ENTRY and ROOT_EXIT events, unlike with others, timeStamp0 is *always* absolute time,
        // and timeStamp1 is *always* thread CPU time
        ti.rootMethodEntryTimeAbs = timeStamp0;
        ti.rootMethodEntryTimeThreadCPU = timeStamp1;
        ti.topMethodEntryTime0 = timeStamp0;

        if (isCollectingTwoTimeStamps()) {
            ti.topMethodEntryTime1 = timeStamp1;
        }

        ti.inRoot++;

        return curNode;
    }

    private TimedCPUCCTNode rootMethodExit(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "RootMExit for tId = {0}, time: {1}, delta: {2}, method: {3}", new Object[]{(int) ti.threadId, timeStamp0, timeStamp0 - delta, debugMethod(methodId)});
            delta = timeStamp0;
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (curNode == null) {
            LOGGER.severe(CommonConstants.ENGINE_WARNING + "critical: stack integrity violation on root method exit.\n" // NOI18N
                          + "*** methodId on simulated stack top is unidentifiable\n"); // NOI18N

            return null;
        }

        if (!(curNode instanceof MethodCPUCCTNode)) {
            LOGGER.severe(CommonConstants.ENGINE_WARNING + "critical: stack integrity violation on root method exit.\n" // NOI18N
                          + "*** methodId on simulated stack top is unidentifiable\n"); // NOI18N

            return null;
        }

        MethodCPUCCTNode methodNode = (MethodCPUCCTNode) curNode;

        if (methodId != methodNode.getMethodId()) {
            StringBuilder message = new StringBuilder();
            message.append(CommonConstants.ENGINE_WARNING).append("critical: stack integrity violation on root method exit.\n"); // NOI18N
            message.append("*** methodId on simulated stack top: ").append((int) methodNode.getMethodId()).append('\n'); // NOI18N
            message.append(", received methodId (should match) = ").append((int) methodId).append('\n'); // NOI18N
            message.append("received method debug: ").append(debugMethod(methodId)).append('\n'); // NOI18N
            message.append(CommonConstants.PLEASE_REPORT_PROBLEM);

            if ((status != null) && (status.getInstrMethodClasses() != null) && !stackIntegrityViolationReported) {
                message.append(dumpStack(ti));
                stackIntegrityViolationReported = true;
            }

            message.append('\n'); // NOI18N
            LOGGER.severe(message.toString());

            return null;
        }

        // Be careful - with ROOT_ENTRY and ROOT_EXIT events, unlike with others, timeStamp0 is *always* absolute time,
        // and timeStamp1 is *always* thread CPU time
        long diff = timeStamp0 - ti.topMethodEntryTime0;

        if (diff > 0) {
            curNode.addNetTime0(diff);
        } else {
            timeStamp0 = ti.topMethodEntryTime0;
        }

        if (isCollectingTwoTimeStamps()) {
            diff = timeStamp1 - ti.topMethodEntryTime1;

            if (diff > 0) {
                curNode.addNetTime1(diff);
            } else {
                timeStamp1 = ti.topMethodEntryTime1;
            }
        }

        ti.inRoot--;

        TimedCPUCCTNode oldNode = ti.pop();

        if (ti.isInRoot()) { // We are actually exiting a non-root invocation of the root method
            ti.topMethodEntryTime0 = timeStamp0;

            if (isCollectingTwoTimeStamps()) {
                ti.topMethodEntryTime1 = timeStamp1;
            }
        } else {
            ti.rootGrossTimeAbs += (timeStamp0 - ti.rootMethodEntryTimeAbs);
            ti.rootGrossTimeThreadCPU += (timeStamp1 - ti.rootMethodEntryTimeThreadCPU);
            ti.rootMethodEntryTimeAbs = 0;
            ti.rootMethodEntryTimeThreadCPU = 0;
        }

        return oldNode;
    }
}

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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ProfilingPointsProcessor;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.global.TransactionalSupport;
import org.netbeans.lib.profiler.results.BaseCallGraphBuilder;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTNodeFactory;
import org.netbeans.lib.profiler.results.cpu.cct.RuntimeCPUCCTNodeVisitorAdaptor;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.ServletRequestCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.SimpleCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.ThreadCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;
import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.results.cpu.marking.MarkingEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Jaroslav Bachorik
 */
public class CPUCallGraphBuilder extends BaseCallGraphBuilder implements CPUProfilingResultListener, CPUCCTProvider {

    private class DebugInfoCollector extends RuntimeCPUCCTNodeVisitorAdaptor {
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
            node.accept(this);

            return buffer.toString();
        }

        public void visit(MethodCPUCCTNode node) {
            buffer.append(debugMethod(node.getMethodId()));
        }

        public void visit(ServletRequestCPUCCTNode node) {
            buffer.append("Boundary"); // NOI18N
        }

        public void visit(ThreadCPUCCTNode node) {
            buffer.append("threadId = ").append(node.getThreadId()); // NOI18N
        }

        public void visit(MarkedCPUCCTNode node) {
            buffer.append("Category ").append(node.getMark()); // NOI18N
        }

        public void visit(SimpleCPUCCTNode node) {
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CPUCCTNodeFactory factory;
    private DebugInfoCollector debugCollector = null;
    private InstrumentationFilter instrFilter;
    private boolean stackIntegrityViolationReported;
    private long delta;

    private MethodInfoMapper methodInfoMapper = MethodInfoMapper.DEFAULT;
    private TimingAdjusterOld timingAdjuster = TimingAdjusterOld.getDefault();
    private ThreadInfos threadInfos = new ThreadInfos();

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
                /* not in use any longer
                   ti.applyDiffAtGetResultsMoment();
                 */
                double[] activeTimes = calculateThreadActiveTimes(ti);

                TimedCPUCCTNode rootNode = ti.stack[0];

                CPUCCTContainer cct = new CPUCCTContainer(rootNode, cpuSnapshot, methodInfoMapper, timingAdjuster, 
                                                          instrFilter, ti.totalNNodes, activeTimes, threadId++, threadNames[i]);

                if ((cct.rootNode != null) && (cct.rootNode.getNChildren() > 0)) {
                    ccts.add(cct);
                }
            }

            return (CPUCCTContainer[]) ccts.toArray(new CPUCCTContainer[ccts.size()]);
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

    protected long getDumpAbsTimeStamp() {
        return status.dumpAbsTimeStamp;
    }

    public void methodEntry(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                            final long timeStamp1) {
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
                markerMethodEntry(methodId, ti, timeStamp0, timeStamp1);

                break;
            }
        }

        batchNotEmpty = true;
    }

    public void methodEntryUnstamped(final int methodId, final int threadId, final int methodType) {
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
                markerMethodEntry(methodId, ti);

                break;
            }
        }

        batchNotEmpty = true;
    }

    public void methodExit(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                           final long timeStamp1) {
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
            // Servelt node must go with a method node; so close them together
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
            // Servelt node must go with a method node; so close them together
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

    public void monitorEntry(final int threadId, final long timeStamp0, final long timeStamp1) {
        waitEntry(threadId, timeStamp0, timeStamp1);
        batchNotEmpty = true;
    }

    public void monitorExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        waitExit(threadId, timeStamp0, timeStamp1);
        batchNotEmpty = true;
    }

    public void newThread(final int threadId, final String threadName, final String threadClassName) {
        if (!isReady()) {
            return;
        }

        LOGGER.finest("New thread creation for thread id = " + threadId // NOI18N
                      + ", name = " + threadName // NOI18N
                      );

        threadInfos.newThreadInfo(threadId, threadName, threadClassName);
        batchNotEmpty = true;
    }

    public void servletRequest(final int threadId, final int requestType, final String servletPath, final int sessionId) {
        ThreadInfo ti = threadInfos.threadInfos[threadId];

        if (ti == null) {
            return;
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (curNode == null) {
            curNode = factory.createThreadNode(threadId);
            ti.totalNNodes++;
            ti.push(curNode);
            ti.totalNInv--;
        }

        ServletRequestCPUCCTNode servletNode = ServletRequestCPUCCTNode.Locator.locate(requestType, servletPath,
                                                                                       curNode.getChildren());

        if (servletNode == null) {
            servletNode = factory.createServletRequestNode(requestType, servletPath);
            curNode.attachNodeAsChild(servletNode);
        }

        ti.push(servletNode);
    }

    public void sleepEntry(final int threadId, long timeStamp0, long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.stack[ti.stackTopIdx];

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("ENTRY SLEEP: " + debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", ti: " + ti // NOI18N
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
        TimedCPUCCTNode curNode = ti.stack[ti.stackTopIdx];

        long lastSleep = timeStamp0 - curNode.getLastWaitOrSleepStamp();

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("EXIT SLEEP: " + debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", slept: " + lastSleep // NOI18N
                          + ", ti: " + ti // NOI18N
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
        // timeStamp0 is always abosolute and timeStamp1 is always thread CPU.
        ti.rootMethodEntryTimeAbs += timeDiff0;
        ti.rootMethodEntryTimeThreadCPU += timeDiff1;
        ti.topMethodEntryTime0 += timeDiff0;

        if (isCollectingTwoTimeStamps()) {
            ti.topMethodEntryTime1 += timeDiff1;
        }

        afterBatchCommands.add(new Runnable() {
                public void run() {
                    ppp.timeAdjust(threadId, timeDiff0, timeDiff1);
                }
            });
        batchNotEmpty = true;
    }

    public void waitEntry(final int threadId, long timeStamp0, long timeStamp1) {
        if (!isReady() || (threadInfos.threadInfos == null)) {
            return;
        }

        ThreadInfo ti = threadInfos.threadInfos[threadId];
        TimedCPUCCTNode curNode = ti.stack[ti.stackTopIdx];

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("ENTRY WAIT: " + debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", ti: " + ti // NOI18N
                          );
            delta = timeStamp0;
            LOGGER.finest(dumpStack(ti));
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
        TimedCPUCCTNode curNode = ti.stack[ti.stackTopIdx];

        long lastWait = timeStamp0 - curNode.getLastWaitOrSleepStamp();
        curNode.setLastWaitOrSleepStamp(0);
        curNode.addWaitTime0(lastWait);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("EXIT WAIT: " + debugNode(curNode) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", waited: " + lastWait // NOI18N
                          + ", ti: " + ti // NOI18N
                          );
            delta = timeStamp0;
            LOGGER.finest(dumpStack(ti));
        }

        // move start timer for current method, so that the time spent waiting is ignored
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
            appNode = new SimpleCPUCCTNode(true);

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
            // + ti.topMethodEntryTime0 + "ti.rootMethodEntryTimeAbs = " + ti.rootMethodEntryTimeAbs); // NOI18N
            //System.err.println("*** Adjusted rootGrossTimeAbs = " + rootGrossTimeAbs); // NOI18N
        }

        //System.err.println("*** rootGrossTimeAbs in ms = "
        // + ((double) rootGrossTimeAbs) * 1000 / status.timerCountsInSecond[0]); // NOI18N
        long rootGrossTimeCPU = ti.rootGrossTimeThreadCPU;

        //System.err.println("*** ti.rootGrossTimeThreadCPU = "
        // + ti.rootGrossTimeThreadCPU + ", totalNInv = " + ti.totalNInv); // NOI18N
        if (ti.stackTopIdx != -1) {
            if (isCollectingTwoTimeStamps()) {
                rootGrossTimeCPU += (ti.topMethodEntryTime1 - ti.rootMethodEntryTimeThreadCPU);

                //System.err.println("*** ti.topMethodEntryTime1 = " + ti.topMethodEntryTime1
                // + ", ti.rootMethodEntryTimeThreadCPU = " + ti.rootMethodEntryTimeThreadCPU);
                //System.err.println("*** adjustment for CPU time = "
                // + (ti.topMethodEntryTime1 - ti.rootMethodEntryTimeThreadCPU)); // NOI18N
            } else {
                rootGrossTimeCPU = -1;
            }
        }

        //System.err.println("*** Adjusted rootGrossTimeCPU = " + rootGrossTimeCPU + ", in ms = "
        // + ((double) rootGrossTimeCPU) * 1000 / status.timerCountsInSecond[1]); // NOI18N
        int nRootInv = rootNode.getNCalls();
        double timeInInjectedCodeInAbsCounts;
        double timeInInjectedCodeInThreadCPUCounts = 0;
        // Calculate timeInInjectedCodeInAbsCounts.
        timeInInjectedCodeInAbsCounts = timingAdjuster.delta(nRootInv, (int) (ti.totalNInv - nRootInv), false);

        //System.err.println("*** timeInInjectedCodeInAbsCounts = " + timeInInjectedCodeInAbsCounts + ", in ms = "
        // + ((double) timeInInjectedCodeInAbsCounts) * 1000 / status.timerCountsInSecond[0]); // NOI18N

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

        if (threadLocked) { // ignore request for reset received durin an ongoing active transaction

            try {
                threadInfos.reset();
            } finally {
                threadInfos.endTrans();
            }
        }
    }

    protected void doShutdown() {
        threadInfos.reset();
        factory = null;
        instrFilter = null;
    }

    protected void doStartup(final ProfilerClient profilerClient) {
        instrFilter = profilerClient.getSettings().getInstrumentationFilter();
        factory = new CPUCCTNodeFactory(isCollectingTwoTimeStamps());
        
        setMethodInfoMapper(new MethodInfoMapper() {

            @Override
            public String getInstrMethodClass(int methodId) {
                return profilerClient.getStatus().getInstrMethodClasses()[methodId];
            }

            @Override
            public String getInstrMethodName(int methodId) {
                return profilerClient.getStatus().getInstrMethodNames()[methodId];
            }

            @Override
            public String getInstrMethodSignature(int methodId) {
                return profilerClient.getStatus().getInstrMethodSignatures()[methodId];
            }

            @Override
            public int getMinMethodId() {
                return profilerClient.getStatus().getStartingMethodId();
            }

            @Override
            public int getMaxMethodId() {
                return profilerClient.getStatus().getNInstrMethods() + profilerClient.getStatus().getStartingMethodId() - 1;
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

    protected void setFactory(CPUCCTNodeFactory factory) {
        this.factory = factory;
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
        return (status != null) && (factory != null) && (instrFilter != null);
    }

    private String debugMethod(int methodId) {
        StringBuffer buffer = new StringBuffer();
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
        StringBuffer buffer = new StringBuffer();
        buffer.append("*** Thread stack dump:\n"); // NOI18N

        for (int i = ti.stackTopIdx; i >= 0; i--) {
            DebugInfoCollector collector = new DebugInfoCollector();
            TimedCPUCCTNode frame = ti.stack[i];
            frame.accept(collector);
            buffer.append(collector.getInfo(frame)).append('\n'); // NOI18N
        }

        return buffer.toString();
    }

    private TimedCPUCCTNode markerMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1,
                                              boolean stamped) {
        Mark mark = MarkingEngine.getDefault().markMethod(methodId, status);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("MarkerMEntry" + ((!stamped) ? "(unstamped)" : "") + " for tId = " + (int) ti.threadId // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", method:  " + debugMethod(methodId) // NOI18N
                          + ", inRoot: " + ti.rootMethodEntryTimeAbs // NOI18N
                          + ", rootEntryTimeThread: " + ti.rootMethodEntryTimeThreadCPU);
        }

        TimedCPUCCTNode curNode = ti.peek();

        if (curNode == null) {
            TimedCPUCCTNode rootNode = factory.createThreadNode(ti.threadId);
            ti.totalNNodes++;
            ti.push(rootNode);
            ti.totalNInv--;

            if (!mark.isDefault()) {
                curNode = factory.createCategory(mark);
                rootNode.attachNodeAsChild(curNode);
                ti.totalNNodes++;
                ti.push(curNode);
                rootNode = curNode;
            }

            curNode = factory.createMethodNode(methodId);
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
                    calleeNode = factory.createCategory(mark);
                    curNode.attachNodeAsChild(calleeNode);
                    ti.totalNNodes++;
                }

                ti.push(calleeNode);
                curNode = calleeNode;
            }

            // Now find the appropriate callee in this node or create one
            calleeNode = MethodCPUCCTNode.Locator.locate(methodId, curNode.getChildren());

            if (calleeNode == null) {
                calleeNode = factory.createMethodNode(methodId);
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
                    if (!client.getSettings().getInstrumentationFilter().passesFilter(jvmClassName)) {
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

    private TimedCPUCCTNode markerMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1) {
        return markerMethodEntry(methodId, ti, timeStamp0, timeStamp1, true);
    }

    private TimedCPUCCTNode markerMethodEntry(final int methodId, final ThreadInfo ti) {
        return markerMethodEntry(methodId, ti, 0, 0, false);
    }

    private TimedCPUCCTNode plainMethodEntry(final int methodId, final ThreadInfo ti, long timeStamp0, long timeStamp1,
                                             boolean stamped) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("MethodEntry " + ((!stamped) ? "(unstamped)" : "") + " for tId = " + (int) ti.threadId // NOI18N
                              + ", time: " + timeStamp0 // NOI18N
                              + ", delta: " + (timeStamp0 - delta) // NOI18N
                              + ", method:  " + debugMethod(methodId) // NOI18N
                );
            }
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
        methodNode = factory.createMethodNode(methodId);
        curNode.attachNodeAsChild(methodNode);

        curNode = methodNode;

        ti.totalNNodes++;
        ti.push(curNode);

        if (!ti.isInRoot()) {
            try {
                methodInfoMapper.lock(false);
                String jvmClassName = methodInfoMapper.getInstrMethodClass(((MethodCPUCCTNode) curNode).getMethodId()).replace('.', '/');
                ProfilerClient client = getClient();

                if (client != null) {
                    if (!client.getSettings().getInstrumentationFilter().passesFilter(jvmClassName)) {
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
            LOGGER.finest("MethodExit" + ((!stamped) ? "(unstamped)" : "") + ": " + debugMethod(methodId) // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", ti: " + ti // NOI18N
                          );
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
            StringBuffer message = new StringBuffer();
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
            LOGGER.finest("RootMEntry for tId = " + (int) ti.threadId // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", method:  " + debugMethod(methodId) // NOI18N
            );
        }

        Mark mark = MarkingEngine.getDefault().markMethod(methodId, status);

        TimedCPUCCTNode curNode = ti.peek();

        if (ti.isInRoot()) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(CommonConstants.ENGINE_WARNING)
                  .append("critical: at root method entry thread stack is not at 0 - should not happen!\n"); // NOI18N
            buffer.append("*** thread = ").append(threadInfos.threadNames[ti.threadId]); // NOI18N
            buffer.append(", ti.stackTopIdx = ").append(ti.stackTopIdx); // NOI18N

            if (curNode != null) {
                buffer.append(", curNode = " + curNode).append('\n'); // NOI18N
            }

            buffer.append(CommonConstants.PLEASE_REPORT_PROBLEM);
            LOGGER.severe(buffer.toString());
        }

        if (curNode == null) { // no node on stack

            TimedCPUCCTNode rootNode = factory.createThreadNode(ti.threadId); // create a new thread node
            ti.totalNNodes++;
            ti.push(rootNode); // and place it on the stack
            ti.totalNInv--;

            if (!mark.isDefault()) {
                curNode = factory.createCategory(mark);
                rootNode.attachNodeAsChild(curNode);
                ti.totalNNodes++;
                ti.push(curNode);
                rootNode = curNode;
            }

            curNode = factory.createMethodNode(methodId); // now create the root method node
            rootNode.attachNodeAsChild(curNode); // and attach it to the previously created thread node
            ti.totalNNodes++;
        } else {
            TimedCPUCCTNode calleeNode;

            if (!mark.isDefault()) {
                // try to locate the category node; or create a new node for the category
                calleeNode = MarkedCPUCCTNode.Locator.locate(mark, curNode.getChildren());

                if (calleeNode == null) {
                    calleeNode = factory.createCategory(mark);
                    curNode.attachNodeAsChild(calleeNode);
                    ti.totalNNodes++;
                }

                ti.push(calleeNode);
                curNode = calleeNode;
            }

            calleeNode = MethodCPUCCTNode.Locator.locate(methodId, curNode.getChildren());

            if (calleeNode == null) {
                calleeNode = factory.createMethodNode(methodId);
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
            LOGGER.finest("RootMExit for tId = " + (int) ti.threadId // NOI18N
                          + ", time: " + timeStamp0 // NOI18N
                          + ", delta: " + (timeStamp0 - delta) // NOI18N
                          + ", method: " + debugMethod(methodId) // NOI18N
            );
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
            StringBuffer message = new StringBuffer();
            message.append(CommonConstants.ENGINE_WARNING).append("critical: stack integrity violation on root thod exit.\n"); // NOI18N
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

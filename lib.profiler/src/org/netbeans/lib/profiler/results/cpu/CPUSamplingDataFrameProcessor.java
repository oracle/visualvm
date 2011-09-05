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

package org.netbeans.lib.profiler.results.cpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils.TargetAppOrVMTerminated;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.results.AbstractDataFrameProcessor;
import org.netbeans.lib.profiler.results.ProfilingResultListener;
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder.SampledThreadInfo;
import org.netbeans.lib.profiler.results.memory.JMethodIdTable;
import org.netbeans.lib.profiler.results.memory.JMethodIdTable.JMethodIdTableEntry;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;

/**
 * This class main purpose is to parse a chunk of CPU sampled data received from
 * the JFluid server agent and dispatch the resulting events to all interested parties
 * @author Tomas Hurka
 */
public class CPUSamplingDataFrameProcessor extends AbstractDataFrameProcessor {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------    
    private volatile int currentThreadId = -1;
    private String currentThreadName, currentThreadClassName;
    private SampledThreadInfo currentThread;
    private long currentTimestamp;
    private Map<Integer,ThreadInfo> currentThreadsDump;
    private Map<Integer,ThreadInfo> lastThreadsDump;
    private List<ThreadDump> threadDumps = new ArrayList();
    private MethodNameFormatter formatter = MethodNameFormatterFactory.getDefault(new DefaultMethodNameFormatter(DefaultMethodNameFormatter.VERBOSITY_FULLMETHOD)).getFormatter();
    private StackTraceSnapshotBuilder builder;
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public void doProcessDataFrame(byte[] buffer) {
        int position = 0;
        JMethodIdTable methodIdsTable = JMethodIdTable.getDefault();
        
        threadDumps = new ArrayList();
        while (position < buffer.length) {
            byte eventType = buffer[position++];
            
            switch (eventType) {    
                case CommonConstants.THREAD_DUMP_START:
                    currentThreadsDump = new HashMap();
                    currentTimestamp = (((long) buffer[position++] & 0xFF) << 48) | (((long) buffer[position++] & 0xFF) << 40)
                                     | (((long) buffer[position++] & 0xFF) << 32) | (((long) buffer[position++] & 0xFF) << 24)
                                     | (((long) buffer[position++] & 0xFF) << 16) | (((long) buffer[position++] & 0xFF) << 8)
                                     | ((long) buffer[position++] & 0xFF);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Thread dump start: Timestamps:"+currentTimestamp); // NOI18N
                    }
                    break;
                case CommonConstants.NEW_THREAD: {
                    int threadId = (char) ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                    int strLen = ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                    String threadName = new String(buffer, position, strLen);
                    position += strLen;
                    strLen = ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                    
                    String threadClassName = new String(buffer, position, strLen);
                    position += strLen;
                    
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Creating new thread , tId={0}", threadId); // NOI18N
                    }
                    
                    currentThreadId = threadId;
                    currentThreadName = threadName;
                    currentThreadClassName = threadClassName;
                    break;
                }
                case CommonConstants.THREAD_INFO_IDENTICAL: {
                    int threadId = (char) ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                    Integer threadIdObj = Integer.valueOf(threadId);
                    ThreadInfo lastInfo = lastThreadsDump.get(threadIdObj);
                    assert lastInfo != null;
                    currentThreadsDump.put(threadIdObj,lastInfo);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Thread info identical: tId:"+threadId); // NOI18N
                    }
                    break;
                }
                case CommonConstants.THREAD_INFO: {
                    int threadId = (char) ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                    byte state = buffer[position++];
                    int stackLen = (char) ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                    int methodIds[] = new int[stackLen];
                    ThreadInfo info;
                    
                    for (int i=0; i<stackLen; i++) {
                        methodIds[i] = (((int) buffer[position++] & 0xFF) << 24) | (((int) buffer[position++] & 0xFF) << 16)
                                | (((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF);
                        methodIdsTable.checkMethodId(methodIds[i]);
                    }
                    if (currentThreadId == threadId) {
                        info = new ThreadInfo(currentThreadName,threadId,state,methodIds);
                    } else {
                        info = new ThreadInfo(null,threadId,state,methodIds);                        
                    }
                    currentThreadsDump.put(Integer.valueOf(threadId),info);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Thread info: tId:"+threadId+" state:"+state+" mIds:"+Arrays.toString(methodIds)); // NOI18N
                    }
                    break;  
                }
                case CommonConstants.THREAD_DUMP_END: {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Thread dump end"); // NOI18N
                    }
                    lastThreadsDump = currentThreadsDump;
                    threadDumps.add(new ThreadDump(currentTimestamp,currentThreadsDump));
                    break;
                }
                case CommonConstants.RESET_COLLECTORS: {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Profiling data reset"); // NOI18N
                    }
                    fireReset();
                    builder.reset();
                    break;
                }
                default: {
                    LOGGER.log(Level.SEVERE, "*** Profiler Engine: internal error: got unknown event type in CallGraphBuilder: {0} at {1}", // NOI18N
                            new Object[]{(int) eventType, position});
                    
                    break;
                }
            }
            try {
                methodIdsTable.getNamesForMethodIds(client);
            } catch (TargetAppOrVMTerminated ex) {
                ex.printStackTrace();
                return;
            }
            processCollectedDumps(methodIdsTable,threadDumps);
            threadDumps.clear();
        }
    }

    public void startup(ProfilerClient client) {
        final CPUCallGraphBuilder[] ccgb = new CPUCallGraphBuilder[1];
        
        super.startup(client);
        
         foreachListener(new ListenerFunctor() {
            public void execute(ProfilingResultListener listener) {
                ccgb[0] = (CPUCallGraphBuilder) listener;
            }
        });
        builder = new StackTraceSnapshotBuilder(ccgb[0],client.getSettings().getInstrumentationFilter(),client.getStatus());
    }
    
    private static Thread.State getThreadState(int threadState) {
        switch (threadState) {
            case CommonConstants.THREAD_STATUS_UNKNOWN:
                return Thread.State.TERMINATED;
            case CommonConstants.THREAD_STATUS_ZOMBIE:
                return Thread.State.TERMINATED;
            case CommonConstants.THREAD_STATUS_RUNNING:
                return Thread.State.RUNNABLE;
            case CommonConstants.THREAD_STATUS_SLEEPING:
                return Thread.State.TIMED_WAITING;
            case CommonConstants.THREAD_STATUS_MONITOR:
                return Thread.State.BLOCKED;
            case CommonConstants.THREAD_STATUS_WAIT:
                return Thread.State.WAITING;
            default:
                return Thread.State.TERMINATED;
        }
    }

    private void processCollectedDumps(JMethodIdTable methodIdTable, List<ThreadDump> threadDumps) {
        Map<Integer,StackTraceElement> stackTraceElements = new HashMap();
        InstrumentationFilter filter = builder.getFilter();
        
        for (ThreadDump td : threadDumps) {
            SampledThreadInfo[] sampledThreadInfos = new SampledThreadInfo[td.threadDumps.length];
            int tindex = 0;
            
            for (ThreadInfo ti : td.threadDumps) {
                int[] methodIds = ti.methodsIds;
                StackTraceElement[] stackTrace = new StackTraceElement[methodIds.length];

                for (int i=0; i<methodIds.length; i++) {
                    int methodId = methodIds[i];
                    StackTraceElement el = stackTraceElements.get(Integer.valueOf(methodId));
                    
                    if (el == null) {
                        JMethodIdTableEntry entry = methodIdTable.getEntry(methodId);
                        String method = formatter.formatMethodName(entry.className, entry.methodName, entry.methodSig).toFormatted();
                        String className = entry.className.replace('/','.');
                        el = new StackTraceElement(className, method, null, -1);
                        stackTraceElements.put(Integer.valueOf(methodId),el);
                    }
                    stackTrace[i] = el;
                }
                sampledThreadInfos[tindex++] = new SampledThreadInfo(ti.threadName,ti.threadId,ti.state,stackTrace,filter);
            }
            builder.addStacktrace(sampledThreadInfos,td.timestamp);
        }
    }
    
    private static final class ThreadInfo {
        private int[] methodsIds;
        private Thread.State state;
        private String threadName;
        private long threadId;
 
        ThreadInfo(String tn, long tid, byte ts, int[] st) {
            threadName = tn;
            threadId = tid;
            state = getThreadState(ts);
            methodsIds = st;
        }
    }
     
    private static final class ThreadDump {
        private long timestamp;
        private ThreadInfo[] threadDumps;
        
        ThreadDump(long ts, Map<Integer,ThreadInfo> threadsMap) {
            timestamp = ts;
            threadDumps = threadsMap.values().toArray(new ThreadInfo[threadsMap.size()]);
        }
    }    
}

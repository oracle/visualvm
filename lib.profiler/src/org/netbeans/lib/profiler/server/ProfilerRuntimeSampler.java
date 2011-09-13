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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.server.system.Stacks;
import org.netbeans.lib.profiler.server.system.Timers;

/**
 * @author Tomas Hurka
 */
class ProfilerRuntimeSampler extends ProfilerRuntime {

    private static Sampling sampling;
    
    static class Sampling extends SamplingThread {
        private int[] states = new int[0];
        private int[][] methodIds = new int[0][];        
        private Map arrayOffsetMap = new HashMap();
        private Map threadIdMap = new HashMap();
        private int threadCount = 0;
        private volatile boolean resetData = false;
        
        Sampling(int samplingInterval) {
            super(samplingInterval);
            setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 10"); // NOI18N
        }
        
        void sample() {
            Thread[][] newThreads = new Thread[1][];
            int[][] newStates = new int[1][];
            int[][][] newMethodIds = new int[1][][];        
            Map newArrayOffsetMap = new HashMap();
            Map newThreadIdMap = new HashMap();
            long timestamp;
            
            if (resetData) {
                resetProfilerCollectors();
                resetData = false;
            }
            Stacks.getAllStackTraces(newThreads, newStates, newMethodIds);
            timestamp = Timers.getCurrentTimeInCounts();
            
            synchronized (eventBuffer) {
                writeThreadDumpStart(timestamp);
                for (int i = 0; i < newThreads[0].length; i++) {
                    Thread t = newThreads[0][i];
                    int[] mids = newMethodIds[0][i];
                    
                    if (!ThreadInfo.isProfilerServerThread(t) && mids.length>0) {
                        int status = newStates[0][i];
                        Long ltid = Long.valueOf(t.getId());
                        Integer index = (Integer) arrayOffsetMap.get(ltid);
                        Integer tid = (Integer) threadIdMap.get(ltid);

                        if (index != null) {
                            if (status == states[index.intValue()] && Arrays.equals(mids,methodIds[index.intValue()])) {
                                writeThreadInfoNoChange(tid);
                            }
                            writeThreadInfo(tid,status,mids);
                        } else if (status != CommonConstants.THREAD_STATUS_ZOMBIE) { // new thread
                            tid = Integer.valueOf(++threadCount);
                            ProfilerRuntime.writeThreadCreationEvent(t,tid.intValue());
                            writeThreadInfo(tid,status,mids);
                        } else { // new thread which is not started yet
                            continue; 
                        }
                        newArrayOffsetMap.put(ltid, Integer.valueOf(i));
                        newThreadIdMap.put(ltid,tid);
                    }
                }
                writeThreadDumpEnd();
            }
            arrayOffsetMap = newArrayOffsetMap;
            threadIdMap = newThreadIdMap;
            states = newStates[0];
            methodIds = newMethodIds[0];
        }

        private void resetProfilerCollectors() {
            arrayOffsetMap = new HashMap();
            threadIdMap = new HashMap();
            states = new int[0];
            methodIds = new int[0][];        
            threadCount = 0;    
        }
         
        private void writeThreadDumpStart(long absTimeStamp) {
            if (eventBuffer == null) {
                return; 
            }

            if (globalEvBufPos == 0) {
                ProfilerServer.notifyClientOnResultsAvailability();
            }

            int curPos = globalEvBufPos;

            if (curPos + 8 > globalEvBufPosThreshold) { // Dump the buffer
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
                curPos = 0;
            }

            eventBuffer[curPos++] = THREAD_DUMP_START;
            eventBuffer[curPos++] = (byte) ((absTimeStamp >> 48) & 0xFF);
            eventBuffer[curPos++] = (byte) ((absTimeStamp >> 40) & 0xFF);
            eventBuffer[curPos++] = (byte) ((absTimeStamp >> 32) & 0xFF);
            eventBuffer[curPos++] = (byte) ((absTimeStamp >> 24) & 0xFF);
            eventBuffer[curPos++] = (byte) ((absTimeStamp >> 16) & 0xFF);
            eventBuffer[curPos++] = (byte) ((absTimeStamp >> 8) & 0xFF);
            eventBuffer[curPos++] = (byte) ((absTimeStamp) & 0xFF);

            globalEvBufPos = curPos;
        }
              
        private void writeThreadDumpEnd() {
            if (eventBuffer == null) {
                return; 
            }

            if (globalEvBufPos == 0) {
                ProfilerServer.notifyClientOnResultsAvailability();
            }

            int curPos = globalEvBufPos;

            if (curPos + 1 > globalEvBufPosThreshold) { // Dump the buffer
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
                curPos = 0;
            }

            eventBuffer[curPos++] = THREAD_DUMP_END;
            globalEvBufPos = curPos;
        }

        private void writeThreadInfoNoChange(Integer tid) {
            if (eventBuffer == null) {
                return; 
            }

            int curPos = globalEvBufPos;

            if (curPos + 3 > globalEvBufPosThreshold) { // Dump the buffer
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
                curPos = 0;
            }

            int threadId = tid.intValue();
            
            eventBuffer[curPos++] = THREAD_INFO_IDENTICAL;
            eventBuffer[curPos++] = (byte) ((threadId >> 8) & 0xFF);
            eventBuffer[curPos++] = (byte) ((threadId) & 0xFF);
            globalEvBufPos = curPos;
        }

        private void writeThreadInfo(Integer tid, int status, int[] mids) {
            if (eventBuffer == null) {
                return; 
            }

            int curPos = globalEvBufPos;

            if (curPos + 6 + mids.length*4 > globalEvBufPosThreshold) { // Dump the buffer
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
                curPos = 0;
            }

            int threadId = tid.intValue();
            int stackLen = mids.length;
            
            eventBuffer[curPos++] = THREAD_INFO;
            eventBuffer[curPos++] = (byte) ((threadId >> 8) & 0xFF);
            eventBuffer[curPos++] = (byte) ((threadId) & 0xFF);
            eventBuffer[curPos++] = (byte) ((status) & 0xFF);
            eventBuffer[curPos++] = (byte) ((stackLen >> 8) & 0xFF);
            eventBuffer[curPos++] = (byte) ((stackLen) & 0xFF);
            for (int i = 0; i < mids.length; i++) {
                eventBuffer[curPos++] = (byte) ((mids[i] >> 24) & 255);
                eventBuffer[curPos++] = (byte) ((mids[i] >> 16) & 255);
                eventBuffer[curPos++] = (byte) ((mids[i] >> 8) & 255);
                eventBuffer[curPos++] = (byte) ((mids[i]) & 255);                
            }
            globalEvBufPos = curPos;
        }
    }

    static void initialize() {
        sampling = new Sampling(500);
        sampling.start();
    }

    public static void shutdown() {
        sampling.terminate();
    }
    
    static void resetProfilerCollectors() {
        sampling.resetData = true;   
    }
}
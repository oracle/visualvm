/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

import org.netbeans.lib.profiler.server.system.Classes;
import org.netbeans.lib.profiler.server.system.Stacks;
import java.util.HashMap;
import java.util.Map;


/**
 * This class contains methods and data structures that are common for both modes of memory profiling - object
 * allocation and object liveness.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeMemory extends ProfilerRuntime {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final int MAX_STACK_FRAMES = 100; // Maximum number of stack frames that we can sample
    protected static final int NO_OF_PROFILER_FRAMES = 3; // Number of our own frames on stack when we take a stack sample
    protected static int[] allocatedInstancesCount;
    protected static int allocatedInstArrayLength;
    protected static short[] allocatedInstThreshold;
    protected static char[] objectSize;
    protected static short samplingInterval;
    protected static int samplingDepth;
    private static int stackDepth;
    private static int[] stackFrameIds;
    private static Map classIdMap;

    // -------------------------------------- Miscellaneous support routines ------------------------------------------
    private static long randSeed;
    private static int bits;
    private static short samplingIntervalBase;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void setAllocatedInstancesCountArray(int[] aic) {
        allocatedInstancesCount = aic;

        if (aic == null) {
            allocatedInstThreshold = null;
            objectSize = null;
            stackFrameIds = null;
            Stacks.clearNativeStackFrameBuffer();

            return;
        } else if (allocatedInstArrayLength < aic.length) {
            short[] oldThresh = (allocatedInstThreshold != null) ? allocatedInstThreshold : null;
            allocatedInstThreshold = new short[aic.length];

            if (oldThresh != null) {
                System.arraycopy(oldThresh, 0, allocatedInstThreshold, 0, allocatedInstArrayLength);
            }

            char[] oldObjectSize = (objectSize != null) ? objectSize : null;
            objectSize = new char[aic.length];

            if (oldObjectSize != null) {
                System.arraycopy(oldObjectSize, 0, objectSize, 0, allocatedInstArrayLength);
            }

            allocatedInstArrayLength = aic.length;
        }
    }

    /** Negative parameter means the depth is not limited. val == 0 means we should not take stack samples */
    public static void setSamplingDepth(int val) {
        if (val < 0) {
            val = MAX_STACK_FRAMES;
        } else if (val > 0) {
            val += NO_OF_PROFILER_FRAMES; // Top frames are always our own methods

            if (val > MAX_STACK_FRAMES) {
                val = MAX_STACK_FRAMES;
            }
        } else {
            stackDepth = 0;
        }

        samplingDepth = val;
    }

    public static void setSamplingInterval(short val) {
        samplingInterval = val;
        initRandomGenerator();
    }

    /** Memory profiling-specific reset collectors functionality. */
    public static void resetProfilerCollectors(int instrType) {
        if (allocatedInstancesCount != null) {
            for (int i = 0; i < allocatedInstancesCount.length; i++) {
                allocatedInstancesCount[i] = 0;
                allocatedInstThreshold[i] = 0;
            }
        }

        if (instrType == INSTR_OBJECT_LIVENESS) {
            ProfilerRuntimeObjLiveness.resetProfilerCollectors();
        }

        classIdMap = new HashMap();
    }

    public static void traceVMObjectAlloc(Object instance, Class clazz) {
        if ((classIdMap == null) || ThreadInfo.profilingSuspended() || ThreadInfo.isProfilerServerThread(Thread.currentThread())) {
            // Avoid counting objects allocated by our own agent threads, or by this method's callees
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();
        String className;
        String classNameId;
        int classLoaderId;
        Integer classIdInt;
        char classId;
        boolean isObjectLiveness;

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        if (!ti.isInitialized()) {
            ti.initialize(true);
        }

        ti.inProfilingRuntimeMethod++;
        className = clazz.getName();
        classLoaderId = ClassLoaderManager.registerLoader(clazz);
        classNameId = new StringBuffer(className).append('#').append(classLoaderId).toString();
        classIdInt = (Integer) classIdMap.get(classNameId);

        if (classIdInt == null) {
            int newClassId = externalActionsHandler.handleFirstTimeVMObjectAlloc(className, classLoaderId);

            if (newClassId != -1) {
                classIdInt = new Integer(newClassId);
                classIdMap.put(classNameId, classIdInt);
            } else {
                // System.err.println("*** JFluid warning: Invalid classId for class:"+classNameId);
                ti.inProfilingRuntimeMethod--;

                return;
            }
        }

        isObjectLiveness = ProfilerInterface.getCurrentInstrType() == INSTR_OBJECT_LIVENESS;
        classId = (char) classIdInt.intValue();
        ti.inProfilingRuntimeMethod--;

        if (isObjectLiveness) {
            ProfilerRuntimeObjLiveness.traceObjAlloc(instance, classId);
        } else {
            ProfilerRuntimeObjAlloc.traceObjAlloc(instance, classId);
        }
    }

    // ------------------------------------------ Stack trace obtaining -----------------------------------------------

    /** This is used in Object Allocation profiling mode */
    protected static synchronized void getAndSendCurrentStackTrace(char classId, long objSize) {
        if (eventBuffer == null) {
            return; // Chances are that instrumentation has been removed while we were executing instrumentation code
        }

        synchronized (eventBuffer) { // Note that we have to use synchronization here due to the static stackFrameIds[] array

            if (samplingDepth != 0) {
                stackDepth = Stacks.getCurrentStackFrameIds(Thread.currentThread(), samplingDepth, stackFrameIds);
            }

            writeObjAllocStackTraceEvent(classId, objSize);
        }
    }

    /** This is used in Object Liveness profiling mode */
    protected static synchronized void getAndSendCurrentStackTrace(char classId, char epoch, int objCount, long objSize) {
        if (eventBuffer == null) {
            return; // Chances are that instrumentation has been removed while we were executing instrumentation code
        }

        synchronized (eventBuffer) { // Note that we have to use synchronization here due to the static stackFrameIds[] array

            if (samplingDepth != 0) {
                stackDepth = Stacks.getCurrentStackFrameIds(Thread.currentThread(), samplingDepth, stackFrameIds);
            }

            writeObjLivenessStackTraceEvent(classId, epoch, objCount, objSize);
        }
    }

    protected static long getCachedObjectSize(char classId, Object object) {
        long objSize = objectSize[classId];

        if (objSize <= 1) { // An array (variable size, value 1) or cached size unset (value 0)

            if (object == null) { // Should not happen, this is a debugging/critical error statement
                System.err.println("*** JFluid critical error: received null object for classId = " + (int) classId
                                   + " in getCachedObjectSize"); // NOI18N
                Thread.dumpStack();
                System.err.println("*** End JFluid critical error message ---------------------------"); // NOI18N
            }

            if (objSize == 0) { // Size not determined yet
                objSize = Classes.getObjectSize(object);

                if (object.getClass().isArray() || (objSize > 0xFFFF)) {
                    objectSize[classId] = 1; // Size will be determined separately every time
                } else {
                    objectSize[classId] = (char) objSize; // Size will be used for all objects of this class
                }
            } else {
                objSize = Classes.getObjectSize(object);
            }
        }

        return objSize;
    }

    protected static void clearDataStructures() {
        ProfilerRuntime.clearDataStructures();
        allocatedInstancesCount = null;
        stackFrameIds = null;
        Stacks.clearNativeStackFrameBuffer();
    }

    protected static void createNewDataStructures() {
        ProfilerRuntime.createNewDataStructures();
        stackFrameIds = new int[MAX_STACK_FRAMES];
        Stacks.createNativeStackFrameBuffer(MAX_STACK_FRAMES);
        classIdMap = new HashMap();
    }

    protected static void enableProfiling(boolean v) {
        // Doesn't call createNewDataStructures() or clearDataStructures() since this is an "abstract" class
    }

    protected static void initRandomGenerator() {
        randSeed = System.currentTimeMillis();

        if (samplingInterval == 1) {
            return;
        } else if ((samplingInterval == 2) || (samplingInterval == 3)) {
            bits = 1;
            samplingIntervalBase = (short) (samplingInterval - 1);

            return;
        } else {
            bits = 1;

            int val = 1; // That should be the nearest power of two smaller than samplingInterval

            while (true) {
                int newVal = val << 1;

                if (newVal < samplingInterval) {
                    bits++;
                    val = newVal;
                } else {
                    break;
                }
            }

            // Set samplingIntervalBase to (samplingInterval - val/2), and make bits correspond to val/2
            samplingIntervalBase = (short) (samplingInterval - (val >> 1));
            bits -= 1;
        }
    }

    protected static short nextRandomizedInterval() {
        if (samplingInterval == 1) {
            return 1;
        }

        // This is copied from 'int java.util.Random.next(int bits)'
        randSeed = ((randSeed * 0x5DEECE66DL) + 0xBL) & ((1L << 48) - 1);

        return (short) (samplingIntervalBase + ((int) (randSeed >>> (48 - bits))));
    }

    // ---------------------------------------- Writing profiler events -----------------------------------------

    /** Note that there is no synchronized(eventBuffer) in this method, since synchronization is already required by its callers */
    protected static void writeObjAllocStackTraceEvent(char classId, long objSize) {
        if (eventBuffer == null) {
            return; // Instrumentation removal happened when we were in instrumentation 
        }

        if (stackDepth != 0) {
            stackDepth -= NO_OF_PROFILER_FRAMES; // Top frames are our own methods
        }

        if (globalEvBufPos == 0) {
            ProfilerServer.notifyClientOnResultsAvailability();
        }

        int curPos = globalEvBufPos;

        if ((curPos + 16 + (stackDepth * 4)) > globalEvBufPosThreshold) { // Dump the buffer
            externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
            curPos = 0;
        }

        eventBuffer[curPos++] = OBJ_ALLOC_STACK_TRACE;
        eventBuffer[curPos++] = (byte) ((classId >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) ((classId) & 0xFF);

        eventBuffer[curPos++] = (byte) ((objSize >> 32) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objSize >> 24) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objSize >> 16) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objSize >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) (objSize & 0xFF);

        curPos = writeStack(curPos);
        globalEvBufPos = curPos;
    }

    protected static void writeObjGCEvent(long objectId) {
        if (eventBuffer == null) {
            return; // Instrumentation removal happened when we were in instrumentation 
        }

        synchronized (eventBuffer) {
            int curPos = globalEvBufPos;

            if (curPos > globalEvBufPosThreshold) { // Dump the buffer
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
                curPos = 0;
            }

            eventBuffer[curPos++] = OBJ_GC_HAPPENED;
            eventBuffer[curPos++] = (byte) ((objectId >> 56) & 0xFF);
            eventBuffer[curPos++] = (byte) ((objectId >> 48) & 0xFF);
            eventBuffer[curPos++] = (byte) ((objectId >> 40) & 0xFF);
            eventBuffer[curPos++] = (byte) ((objectId >> 32) & 0xFF);
            eventBuffer[curPos++] = (byte) ((objectId >> 24) & 0xFF);
            eventBuffer[curPos++] = (byte) ((objectId >> 16) & 0xFF);
            eventBuffer[curPos++] = (byte) ((objectId >> 8) & 0xFF);
            eventBuffer[curPos++] = (byte) (objectId & 0xFF);
            globalEvBufPos = curPos;
        }
    }

    /** Note that there is no synchronized(eventBuffer) in this method, since synchronization is already required by its callers */
    protected static void writeObjLivenessStackTraceEvent(char classId, char epoch, int objCount, long objSize) {
        if (eventBuffer == null) {
            return; // Instrumentation removal happened when we were in instrumentation 
        }

        if (stackDepth != 0) {
            stackDepth -= NO_OF_PROFILER_FRAMES; // Top 4 frames are our own methods
        }

        if (globalEvBufPos == 0) {
            ProfilerServer.notifyClientOnResultsAvailability();
        }

        int curPos = globalEvBufPos;

        if ((curPos + 24 + (stackDepth * 4)) > globalEvBufPosThreshold) { // Dump the buffer
            externalActionsHandler.handleEventBufferDump(eventBuffer, 0, curPos);
            curPos = 0;
        }

        eventBuffer[curPos++] = OBJ_LIVENESS_STACK_TRACE;
        eventBuffer[curPos++] = (byte) ((classId >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) ((classId) & 0xFF);
        eventBuffer[curPos++] = (byte) ((epoch >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) ((epoch) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objCount >> 24) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objCount >> 16) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objCount >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objCount) & 0xFF);

        eventBuffer[curPos++] = (byte) ((objSize >> 32) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objSize >> 24) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objSize >> 16) & 0xFF);
        eventBuffer[curPos++] = (byte) ((objSize >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) (objSize & 0xFF);

        curPos = writeStack(curPos);
        globalEvBufPos = curPos;
    }

    private static int writeStack(int curPos) {
        eventBuffer[curPos++] = (byte) ((stackDepth >> 16) & 0xFF);
        eventBuffer[curPos++] = (byte) ((stackDepth >> 8) & 0xFF);
        eventBuffer[curPos++] = (byte) ((stackDepth) & 0xFF);

        /// A variant when we send non-reversed call graph
        //int base = depth + NO_OF_PROFILER_FRAMES - 1;
        //for (int i = 0; i < depth; i++) {
        //  eventBuffer[curPos++] = (char) ((stackFrameIds[base-i] >> 16) & 0xFFFF);
        //  eventBuffer[curPos++] = (char) ((stackFrameIds[base-i]) & 0xFFFF);
        //}
        int frameIdx = NO_OF_PROFILER_FRAMES;

        for (int i = 0; i < stackDepth; i++) {
            eventBuffer[curPos++] = (byte) ((stackFrameIds[frameIdx] >> 24) & 0xFF);
            eventBuffer[curPos++] = (byte) ((stackFrameIds[frameIdx] >> 16) & 0xFF);
            eventBuffer[curPos++] = (byte) ((stackFrameIds[frameIdx] >> 8) & 0xFF);
            eventBuffer[curPos++] = (byte) ((stackFrameIds[frameIdx]) & 0xFF);
            frameIdx++;
        }

        return curPos;
    }
}

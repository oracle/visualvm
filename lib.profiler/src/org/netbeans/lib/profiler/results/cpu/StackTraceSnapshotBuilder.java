/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.cpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTNodeFactory;

/**
 *
 * @author Jaroslav Bachorik, Tomas Hurka
 */
public class StackTraceSnapshotBuilder {

    private static final StackTraceElement[] NO_STACK_TRACE = new StackTraceElement[0];
    private static final boolean COLLECT_TWO_TIMESTAMPS = true;
    private static final List<MethodInfo> knownBLockingMethods = Arrays.asList(new MethodInfo[] {
        new MethodInfo("java.net.PlainSocketImpl", "socketAccept[native]",null),
        new MethodInfo("sun.awt.windows.WToolkit", "eventLoop[native]",null),
    });

    static class MethodInfo {

        final public String className;
        final public String methodName;
        final public String signature;

        public MethodInfo(String className, String methodName, String signature) {
            this.className = className;
            this.methodName = methodName;
            this.signature = signature;
        }

        public MethodInfo(StackTraceElement element) {
            className = element.getClassName();
            methodName = element.getMethodName() + (element.isNativeMethod() ? "[native]" : ""); // NOI18N
            signature = element.getFileName() + ":" + element.getLineNumber(); // NOI18N
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MethodInfo other = (MethodInfo) obj;
            if ((this.className == null) ? (other.className != null) : !this.className.equals(other.className)) {
                return false;
            }
            if ((this.methodName == null) ? (other.methodName != null) : !this.methodName.equals(other.methodName)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.className != null ? this.className.hashCode() : 0);
            hash = 29 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return className + "." + methodName + "(" + signature + ")";
        }
    }
    final List<Long> threadIds = new ArrayList<Long>();
    final List<String> threadNames = new ArrayList<String>();
    final List<byte[]> threadCompactData = new ArrayList<byte[]>();
    final List<MethodInfo> methodInfos = new ArrayList<MethodInfo>();
    final MethodInfoMapper mapper = new MethodInfoMapper() {

        @Override
        public String getInstrMethodClass(int methodId) {
            return methodInfos.get(methodId).className;
        }

        @Override
        public String getInstrMethodName(int methodId) {
            return methodInfos.get(methodId).methodName;
        }

        @Override
        public String getInstrMethodSignature(int methodId) {
            return methodInfos.get(methodId).signature;
        }

        @Override
        public int getMaxMethodId() {
            return methodInfos.size();
        }

        @Override
        public int getMinMethodId() {
            return 0;
        }
    };
    final CPUCallGraphBuilder ccgb = new CPUCallGraphBuilder() {

        {
            setFactory(new CPUCCTNodeFactory(COLLECT_TWO_TIMESTAMPS));
            setFilter(InstrumentationFilter.getDefault());
        }

        @Override
        protected boolean isCollectingTwoTimeStamps() {
            return COLLECT_TWO_TIMESTAMPS;
        }

        @Override
        protected boolean isReady() {
            return true;
        }

        @Override
        protected long getDumpAbsTimeStamp() {
            synchronized(stampLock) {
                return currentDumpTimeStamp;
            }
        }
    };
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    final Object stampLock = new Object();
    // @GuardedBy stampLock
    long currentDumpTimeStamp = -1L;
    final AtomicReference<Map<Long, java.lang.management.ThreadInfo>> lastStackTrace = new AtomicReference<Map<Long, java.lang.management.ThreadInfo>>(Collections.EMPTY_MAP);
    int stackTraceCount = 0;
    final Set<String> ignoredThreadNames = new HashSet<String>();
    final Map<Long,Long> threadtimes = new HashMap();
    
    public StackTraceSnapshotBuilder() {
        ccgb.setMethodInfoMapper(mapper);
    }

    final public void setIgnoredThreads(Set<String> ignoredThreadNames) {
        try {
            lock.writeLock().lock();
            this.ignoredThreadNames.clear();
            this.ignoredThreadNames.addAll(ignoredThreadNames);
        } finally {
            lock.writeLock().unlock();
        }
    }

    final public void addStacktrace(java.lang.management.ThreadInfo[] threads, long dumpTimeStamp) throws IllegalStateException {
        long timediff;
        
        synchronized(stampLock) {
            if (dumpTimeStamp <= currentDumpTimeStamp) {
                throw new IllegalStateException("Adding stacktrace with timestamp " + dumpTimeStamp + " is not allowed after a stacktrace with timestamp " + currentDumpTimeStamp + " has been added");
            }
            timediff = dumpTimeStamp - currentDumpTimeStamp;
            currentDumpTimeStamp = dumpTimeStamp;
        }

        try {
            lock.writeLock().lock();
            Map<Long,java.lang.management.ThreadInfo> tinfoMap = new HashMap();

            for (java.lang.management.ThreadInfo tinfo : threads) {
                if (tinfo != null) {
                    tinfoMap.put(tinfo.getThreadId(),tinfo);
                }
            }

            for (java.lang.management.ThreadInfo tinfo : tinfoMap.values()) {
                String tname = tinfo.getThreadName();
                
                if (ignoredThreadNames.contains(tname)) continue;

                long threadId = tinfo.getThreadId();
                if (!threadIds.contains(threadId)) {
                    threadIds.add(threadId);
                    threadNames.add(tname);
                    ccgb.newThread((int) threadId, tname, "<none>");
                    threadtimes.put(threadId,dumpTimeStamp);
                }
                StackTraceElement[] newElements = tinfo.getStackTrace();
                Thread.State newState = tinfo.getThreadState();
                java.lang.management.ThreadInfo oldTinfo = lastStackTrace.get().get(threadId);
                StackTraceElement[] oldElements = NO_STACK_TRACE;
                Thread.State oldState = Thread.State.NEW;
                
                if (oldTinfo != null) {
                    oldElements = oldTinfo.getStackTrace();
                    oldState = oldTinfo.getThreadState();
                }
                processDiffs((int) threadId, oldElements, newElements, dumpTimeStamp, timediff, oldState, newState);
            }

            for (java.lang.management.ThreadInfo oldTinfo : lastStackTrace.get().values()) {
                if (ignoredThreadNames.contains(oldTinfo.getThreadName())) continue;

                if (!tinfoMap.containsKey(oldTinfo.getThreadId())) {
                    Thread.State oldState = oldTinfo.getThreadState();
                    Thread.State newState = Thread.State.TERMINATED;
                    processDiffs((int) oldTinfo.getThreadId(), oldTinfo.getStackTrace(), NO_STACK_TRACE, dumpTimeStamp, timediff, oldState, newState);
                }
            }

            lastStackTrace.set(tinfoMap);

            stackTraceCount++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void processDiffs(int threadId, StackTraceElement[] oldElements, StackTraceElement[] newElements, long timestamp, long timediff, Thread.State oldState, Thread.State newState) throws IllegalStateException {
        if (newState == Thread.State.NEW) {
            throw new IllegalStateException("Invalid thread state " + Thread.State.NEW.name() + " for taking a stack trace");
        }
        if (oldState == Thread.State.TERMINATED && newState != Thread.State.TERMINATED) {
            throw new IllegalStateException("Thread has already been set to " + Thread.State.TERMINATED.name() + " - stack trace can not be taken");
        }
        long threadtime = threadtimes.get(Long.valueOf(threadId));
        
//        switch (oldState) {
//            case NEW: {
//                switch (newState) {
//                    case RUNNABLE: {
//                        processDiffs(threadId, oldElements, newElements, timestamp);
//                        break;
//                    }
//                }
//                break;
//            }
//            case RUNNABLE: {
//                break;
//            }
//            case WAITING:
//            case TIMED_WAITING: {
//                ccgb.waitExit(threadId, timestamp, threadtime);
//                break;
//            }
//            case BLOCKED: {
//                ccgb.monitorExit(threadId, timestamp, threadtime);
//                break;
//            }
//        }
        if (newState == Thread.State.RUNNABLE && !containsKnownBlockingMethod(newElements)) {
            threadtime += timediff;
            threadtimes.put(Long.valueOf(threadId),threadtime);
        }
        processDiffs(threadId, oldElements, newElements, timestamp, threadtime);
//        switch (newState) {
//            case RUNNABLE: {
//                break;
//            }
//            case WAITING:
//            case TIMED_WAITING: {
//                ccgb.waitEntry(threadId, timestamp, threadtime);
//                break;
//            }
//            case BLOCKED: {
//                ccgb.monitorEntry(threadId, timestamp, threadtime);
//                break;
//            }
//        }
    }

    private void processDiffs(int threadId, StackTraceElement[] oldElements, StackTraceElement[] newElements, long timestamp, long threadtimestamp) throws IllegalStateException {
        if (oldElements.length == 0 && newElements.length == 0) {
            return;
        }

        int newMax = newElements.length - 1;
        int oldMax = oldElements.length - 1;
        int globalMax = Math.max(oldMax, newMax);

        List<StackTraceElement> newElementsList = Collections.EMPTY_LIST;
        List<StackTraceElement> oldElementsList = Collections.EMPTY_LIST;

        for (int iteratorIndex = 0; iteratorIndex <= globalMax; iteratorIndex++) {
            StackTraceElement oldElement = oldMax >= iteratorIndex ? oldElements[oldMax - iteratorIndex] : null;
            StackTraceElement newElement = newMax >= iteratorIndex ? newElements[newMax - iteratorIndex] : null;

            if (oldElement != null && newElement != null) {
                if (!oldElement.equals(newElement)) {
                    if (hasSameMethodInfo(oldElement,newElement)) {
                        iteratorIndex++;
                    }
                    newElementsList = Arrays.asList(newElements).subList(0, newMax - iteratorIndex + 1);
                    oldElementsList = Arrays.asList(oldElements).subList(0, oldMax - iteratorIndex + 1);
                    break;
                }
            } else if (oldElement == null && newElement != null) {
                newElementsList = Arrays.asList(newElements).subList(0, newMax - iteratorIndex + 1);
                break;

            } else if (oldElement != null && newElement == null) {
                oldElementsList = Arrays.asList(oldElements).subList(0, oldMax - iteratorIndex + 1);
                break;

            }
        }

        // !!! The order is important - first we need to exit from the
        // already entered methods and only then we can enter the new ones !!!
        addMethodExits(threadId, oldElementsList, timestamp, threadtimestamp, newElements.length == 0);
        addMethodEntries(threadId, newElementsList, timestamp, threadtimestamp, oldElements.length == 0);
    }

    private void addMethodEntries(int threadId, List<StackTraceElement> elements, long timestamp, long threadtimestamp, boolean asRoot) throws IllegalStateException {
        boolean inRoot = false;
        ListIterator<StackTraceElement> reverseIt = elements.listIterator(elements.size());
        
        while(reverseIt.hasPrevious()) {
            StackTraceElement element = reverseIt.previous();
            MethodInfo mi = new MethodInfo(element);
            if (!methodInfos.contains(mi)) {
                methodInfos.add(mi);
            }

            int index = methodInfos.indexOf(mi);
            if (index == -1) {
                System.err.println("*** Not found: " + mi);
                throw new IllegalStateException();
            }
            if (asRoot && !inRoot) {
                inRoot = true;
                ccgb.methodEntry(index, threadId, CPUCallGraphBuilder.METHODTYPE_ROOT, timestamp, threadtimestamp);
            } else {
                ccgb.methodEntry(index, threadId, CPUCallGraphBuilder.METHODTYPE_NORMAL, timestamp, threadtimestamp);
            }

        }
    }

    private void addMethodExits(int threadId, List<StackTraceElement> elements, long timestamp, long threadtimestamp, boolean asRoot) throws IllegalStateException {
        int rootIndex = elements.size();
        for (StackTraceElement element : elements) {
            MethodInfo mi = new MethodInfo(element);
            int index = methodInfos.indexOf(mi);
            if (index == -1) {
                System.err.println("*** Not found: " + mi);
                throw new IllegalStateException();
            }

            if (asRoot && --rootIndex == 0) {
                ccgb.methodExit(index, threadId, CPUCallGraphBuilder.METHODTYPE_ROOT, timestamp, threadtimestamp);
            } else {
                ccgb.methodExit(index, threadId, CPUCallGraphBuilder.METHODTYPE_NORMAL, timestamp, threadtimestamp);
            }

        }
    }

    private boolean hasSameMethodInfo(StackTraceElement oldElement, StackTraceElement newElement) {
        MethodInfo oldMethodInfo = new MethodInfo(oldElement);
        MethodInfo newMethodInfo = new MethodInfo(newElement);
        
        return oldMethodInfo.equals(newMethodInfo);
    }

    private boolean containsKnownBlockingMethod(StackTraceElement[] stackTrace) {
        if (stackTrace.length > 0) {
            MethodInfo firstFrame = new MethodInfo(stackTrace[0]);
            if (knownBLockingMethods.contains(firstFrame)) {
                return true;
            }
        }
        return false;
    }

    public final CPUResultsSnapshot createSnapshot(
            long since) throws CPUResultsSnapshot.NoDataAvailableException {
        if (stackTraceCount < 1) {
            throw new CPUResultsSnapshot.NoDataAvailableException();
        }

        String[] instrMethodClasses;
        String[] instrMethodNames;
        String[] instrMethodSigs;
        int miCount;
        try {
            lock.readLock().lock();
            miCount = methodInfos.size();
            instrMethodClasses = new String[methodInfos.size()];
            instrMethodNames = new String[methodInfos.size()];
            instrMethodSigs = new String[methodInfos.size()];

            int counter = 0;
            for (MethodInfo mi : methodInfos) {
                instrMethodClasses[counter] = mi.className;
                instrMethodNames[counter] = mi.methodName;
                instrMethodSigs[counter] = mi.signature;
                counter++;
            }
        } finally {
            lock.readLock().unlock();
        }

        addStacktrace(new java.lang.management.ThreadInfo[0], currentDumpTimeStamp+1);

        return new CPUResultsSnapshot(since, System.currentTimeMillis(), ccgb, ccgb.isCollectingTwoTimeStamps(), instrMethodClasses, instrMethodNames, instrMethodSigs, miCount);
    }

    public final void reset() {
        try {
            lock.writeLock().lock();
            ccgb.reset();
            methodInfos.clear();
            threadIds.clear();
            threadNames.clear();
            stackTraceCount = 0;
            lastStackTrace.set(Collections.EMPTY_MAP);
        } finally {
            lock.writeLock().unlock();
            synchronized(stampLock) {
                currentDumpTimeStamp = -1L;
            }
        }
    }
}

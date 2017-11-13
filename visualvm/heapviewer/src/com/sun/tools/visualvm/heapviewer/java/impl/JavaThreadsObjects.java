/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.java.impl;

import com.sun.tools.visualvm.heapviewer.java.StackFrameNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaFrameGCRoot;
import org.netbeans.lib.profiler.heap.ThreadObjectGCRoot;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.java.LocalObjectNode;
import com.sun.tools.visualvm.heapviewer.java.ThreadNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.RootNode;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaThreadsObjects {
    
    static HeapViewerNode[] getThreads(RootNode rootNode, Heap heap) {
        List<HeapViewerNode> threadNodes = new ArrayList();
        
        Collection<GCRoot> roots = heap.getGCRoots();
        Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> javaFrameMap = computeJavaFrameMap(roots);
        
        for (GCRoot root : roots) {
            if (root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                Instance threadInstance = threadRoot.getInstance();
                if (threadInstance != null) {
                    String threadName = getThreadName(heap, threadInstance);
                    Boolean daemon = (Boolean)threadInstance.getValueOfField("daemon"); // NOI18N
                    Integer priority = (Integer)threadInstance.getValueOfField("priority"); // NOI18N
                    Long threadId = (Long)threadInstance.getValueOfField("tid");    // NOI18N
                    Integer threadStatus = (Integer)threadInstance.getValueOfField("threadStatus"); // NOI18N
                    StackTraceElement stack[] = threadRoot.getStackTrace();
                    Map<Integer,List<JavaFrameGCRoot>> localsMap = javaFrameMap.get(threadRoot);

                    String tName = "\"" + threadName + "\"" + (daemon.booleanValue() ? " daemon" : "") + " prio=" + priority;
                    if (threadId != null) tName += " tid=" + threadId;
                    if (threadStatus != null) tName += " " + toThreadState(threadStatus.intValue());

                    final List<HeapViewerNode> stackFrameNodes = new ArrayList();
                    ThreadNode threadNode = new ThreadNode(tName, threadInstance) {
                        protected HeapViewerNode[] computeChildren(RootNode root) {
                            return stackFrameNodes.toArray(HeapViewerNode.NO_NODES);
                        }
                    };

                    // -------------------------------------------------------------------
                    if(stack != null) {
                        for(int i = 0; i < stack.length; i++) {
                            final List<HeapViewerNode> localVariableNodes = new ArrayList();
                            if (localsMap != null) {
                                List<JavaFrameGCRoot> locals = localsMap.get(i);
                                if (locals != null) {
                                    for (JavaFrameGCRoot localVar : locals) {
                                        Instance localInstance = localVar.getInstance();
                                        if (localInstance != null) {
                                            localVariableNodes.add(new LocalObjectNode(localInstance, "local variable"));
                                        } else {
                                            localVariableNodes.add(new LocalObjectNode.Unknown());                                              
                                        }
                                    }
                                }
                            }
                            
                            StackTraceElement stackElement = stack[i];
                            StackFrameNode stackFrameNode = new StackFrameNode(stackElement.toString(), localVariableNodes.toArray(HeapViewerNode.NO_NODES));
                            stackFrameNodes.add(stackFrameNode);
                        }
                    }

                    threadNodes.add(threadNode);
                } else {
                    threadNodes.add(new ThreadNode.Unknown());
                }
            }
        }
        
        return threadNodes.toArray(HeapViewerNode.NO_NODES);
    }
    
    
    private static Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> computeJavaFrameMap(Collection<GCRoot> roots) {
        Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> javaFrameMap = new HashMap();
        
        for (GCRoot root : roots) {
            if (GCRoot.JAVA_FRAME.equals(root.getKind())) {
                JavaFrameGCRoot frameGCroot = (JavaFrameGCRoot) root;
                ThreadObjectGCRoot threadObj = frameGCroot.getThreadGCRoot();
                Integer frameNo = frameGCroot.getFrameNumber();
                Map<Integer,List<JavaFrameGCRoot>> stackMap = javaFrameMap.get(threadObj);
                List<JavaFrameGCRoot> locals;
                
                if (stackMap == null) {
                    stackMap = new HashMap();
                    javaFrameMap.put(threadObj,stackMap);
                }
                locals = stackMap.get(frameNo);
                if (locals == null) {
                    locals = new ArrayList(2);
                    stackMap.put(frameNo,locals);
                }
                locals.add(frameGCroot);
            }
        }
        return javaFrameMap;
    }
    
    private static String getThreadName(final Heap heap, final Instance threadInstance) {
        Object threadName = threadInstance.getValueOfField("name");  // NOI18N
        if (threadName == null) return "*null*"; // NOI18N
        return DetailsSupport.getDetailsString((Instance)threadName, heap);
    }
    
    /** taken from sun.misc.VM
     * 
     * Returns Thread.State for the given threadStatus
     */
    private static Thread.State toThreadState(int threadStatus) {
        if ((threadStatus & JVMTI_THREAD_STATE_RUNNABLE) != 0) {
            return Thread.State.RUNNABLE;
        } else if ((threadStatus & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) != 0) {
            return Thread.State.BLOCKED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_INDEFINITELY) != 0) {
            return Thread.State.WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT) != 0) {
            return Thread.State.TIMED_WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_TERMINATED) != 0) {
            return Thread.State.TERMINATED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_ALIVE) == 0) {
            return Thread.State.NEW;
        } else {
            return Thread.State.RUNNABLE;
        }
    }
    
     /* The threadStatus field is set by the VM at state transition
     * in the hotspot implementation. Its value is set according to
     * the JVM TI specification GetThreadState function.
     */
    private final static int JVMTI_THREAD_STATE_ALIVE = 0x0001;
    private final static int JVMTI_THREAD_STATE_TERMINATED = 0x0002;
    private final static int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;
    private final static int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;
    private final static int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;
    private final static int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;
    
}

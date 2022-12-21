/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
class HprofGCRoots {

    final HprofHeap heap;
    final private Object threadSerialMapLock = new Object();
    private ThreadMap threadSerialMap;
    private int rootThreadsCount;
    private Map<Long,Object> gcRoots;
    final private Object gcRootLock = new Object();
    private List<GCRoot> gcRootsList;

    HprofGCRoots(HprofHeap h) {
        heap = h;
    }

    List<GCRoot> getGCRoots() {
        synchronized (gcRootLock) {
            if (gcRoots == null) {
                gcRoots = new HashMap<>(16384);
                gcRootsList = new ArrayList<>(16384);
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_UNKNOWN));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_JNI_GLOBAL));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_JNI_LOCAL));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_JAVA_FRAME));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_NATIVE_STACK));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_STICKY_CLASS));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_THREAD_BLOCK));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_MONITOR_USED));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_THREAD_OBJECT));

                // HPROF HEAP 1.0.3
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_INTERNED_STRING));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_FINALIZING));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_DEBUGGER));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_REFERENCE_CLEANUP));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_VM_INTERNAL));
                computeGCRootsFor(heap.getHeapTagBound(HprofHeap.ROOT_JNI_MONITOR));

                gcRootsList = Collections.unmodifiableList(gcRootsList);
            }

            return gcRootsList;
        }
    }
    
    Object getGCRoots(Long instanceId) {
        synchronized (gcRootLock) {
            if (gcRoots == null) {
                heap.getGCRoots();
            }

            return gcRoots.get(instanceId);
        }
    }
    
    ThreadObjectGCRoot getThreadGCRoot(int threadSerialNumber) {
        List<GCRoot> roots = getGCRoots();
        synchronized (threadSerialMapLock) {
            if (threadSerialMap == null) {
                threadSerialMap = new ThreadMap(rootThreadsCount);
            
                for (int i = 0; i < roots.size(); i++) {
                    GCRoot gcRoot = roots.get(i);
                    if (gcRoot instanceof ThreadObjectHprofGCRoot) {
                        threadSerialMap.putThreadIndex((ThreadObjectHprofGCRoot) gcRoot, i);
                    }
                }
            }
            int threadIndex = threadSerialMap.getThreadIndex(threadSerialNumber);

            if (threadIndex != -1) {
                return (ThreadObjectGCRoot)roots.get(threadIndex);
            }
            return null;
        }
    }
    

    private void computeGCRootsFor(TagBounds tagBounds) {
        if (tagBounds != null) {
            int rootTag = tagBounds.tag;
            long[] offset = new long[] { tagBounds.startOffset };

            while (offset[0] < tagBounds.endOffset) {
                long start = offset[0];

                if (heap.readDumpTag(offset) == rootTag) {
                    HprofGCRoot root;
                    if (rootTag == HprofHeap.ROOT_THREAD_OBJECT) {
                        root = new ThreadObjectHprofGCRoot(this, start);
                        rootThreadsCount++;
                    } else if (rootTag == HprofHeap.ROOT_JAVA_FRAME) {
                        root = new JavaFrameHprofGCRoot(this, start);
                    } else if (rootTag == HprofHeap.ROOT_JNI_LOCAL) {
                        root = new JniLocalHprofGCRoot(this, start);
                    } else {
                        root = new HprofGCRoot(this, start);
                    }
                    Long objectId = Long.valueOf(root.getInstanceId());
                    Object val = gcRoots.get(objectId);
                    if (val == null) {
                        gcRoots.put(objectId, root);
                    } else if (val instanceof GCRoot) {
                        Collection<GCRoot> vals = new ArrayList<>(2);
                        vals.add((GCRoot)val);
                        vals.add(root);
                        gcRoots.put(objectId, vals);
                    } else {
                        ((Collection)val).add(root);
                    }
                    gcRootsList.add(root);
                }
            }
        }
    }

    private static class ThreadMap {
        private final int[] serialMap;
        // gracefully handle hprof dumps, which does not follow spec -
        // thread serial number should be sequential starting from 1
        private final Map<Integer,Integer> serialMapOverflow = new HashMap<>();

        ThreadMap(int threadCount) {
            serialMap = new int[threadCount+1];
        }

        private void putThreadIndex(ThreadObjectHprofGCRoot threadGCRoot, int index) {
            int serialNum = threadGCRoot.getThreadSerialNumber();
            if (serialNum < serialMap.length) {
                serialMap[serialNum] = index;
            } else {
                serialMapOverflow.put(serialNum, index);
            }
        }

        private int getThreadIndex(int serialNum) {
            if (serialNum >= 0 && serialNum < serialMap.length) {
                return serialMap[serialNum];
            } else {
                Integer threadIndexObj = serialMapOverflow.get(serialNum);
                if (threadIndexObj == null) return -1;
                return threadIndexObj.intValue();
            }
        }
    }
}

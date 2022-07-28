/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Tomas Hurka
 */
class DominatorTree {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int BUFFER_SIZE = (64 * 1024) / 8;
    private static final int ADDITIONAL_IDS_THRESHOLD = 30;
    private static final int ADDITIONAL_IDS_THRESHOLD_DIRTYSET_SAME_SIZE = 5;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HprofHeap heap;
    private LongBuffer multipleParents;
    private LongBuffer revertedMultipleParents;
    private LongBuffer currentMultipleParents;
    private LongHashMap map;
    private LongSet dirtySet;
    private int dirtySetSameSize;
    private Map<ClassDump,Boolean> canContainItself;
    private Map<Long,Long> nearestGCRootCache = new NearestGCRootCache<>(400000);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    DominatorTree(HprofHeap h, LongBuffer multiParents) {
        heap = h;
        multipleParents = multiParents;
        currentMultipleParents = multipleParents;
        map = new LongHashMap(multiParents.getSize());
        dirtySet = new LongSet();
        try {
            revertedMultipleParents = multiParents.revertBuffer();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage(),ex);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------    
    
    synchronized void computeDominators() {
        boolean changed = true;
        boolean igonoreDirty;
        try {
            do {
                currentMultipleParents.rewind();
                igonoreDirty = !changed;
                changed = computeOneLevel(igonoreDirty);
                switchParents();
            } while (changed || !igonoreDirty);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        deleteBuffers();
        dirtySet = new LongSet();
    }
    
    private boolean computeOneLevel(boolean ignoreDirty) throws IOException {
        boolean changed = false;
        LongSet newDirtySet = new LongSet(map.size()/10);
        List<Long> additionalIds = new ArrayList<>();
        int additionalIndex = 0;
        // debug 
//        long processedId = 0;
//        long changedId = 0;
//        long index = 0;
//        List<Long> changedIds = new ArrayList();
//        List<Long> changedIdx = new ArrayList();
//        List<Boolean> addedBynewDirtySet = new ArrayList();
//        List<Long> oldDomIds = new ArrayList();
//        List<Long> newDomIds = new ArrayList();

//System.out.println("New level, dirtyset size: "+dirtySet.size());
        for (;;) {
            long instanceId = readLong();
            if (instanceId == 0) {  // end of level
                if (additionalIndex >= additionalIds.size()) {
                    if (additionalIndex>0) {
//System.out.println("Additional instances "+additionalIndex);
                    }
                    break;
                }
                instanceId = additionalIds.get(additionalIndex++).longValue();
            }
            long oldIdom = map.get(instanceId);
//index++;
            if (oldIdom == -1 || (oldIdom > 0 && (ignoreDirty || dirtySet.contains(oldIdom) || dirtySet.contains(instanceId)))) {            
//processedId++;
                LongMap.Entry entry = heap.idToOffsetMap.get(instanceId);
                LongIterator refIt = entry.getReferences();
                long newIdomId = refIt.next();
                boolean dirty = false;
                
                while(refIt.hasNext() && newIdomId != 0) {
                    long refIdObj = refIt.next();
                    newIdomId = intersect(newIdomId, refIdObj);
                }
                if (oldIdom == -1) {
//addedBynewDirtySet.add(newDirtySet.contains(instanceId) && !dirtySet.contains(instanceId));
                    map.put(instanceId, newIdomId);
                    if (newIdomId != 0) newDirtySet.add(newIdomId);
                    changed = true;
//changedId++;
//changedIds.add(instanceIdObj);
//changedIdx.add(index);
//oldDomIds.add(null);
//newDomIds.add(newIdomIdObj);
                } else if (oldIdom != newIdomId) {
//addedBynewDirtySet.add((newDirtySet.contains(oldIdom) || newDirtySet.contains(instanceId)) && !(dirtySet.contains(oldIdom) || dirtySet.contains(instanceId)));
                    newDirtySet.add(oldIdom);
                    if (newIdomId != 0) newDirtySet.add(newIdomId);
                    map.put(instanceId,newIdomId);
                    if (dirtySet.size() < ADDITIONAL_IDS_THRESHOLD || dirtySetSameSize >= ADDITIONAL_IDS_THRESHOLD_DIRTYSET_SAME_SIZE) {
                        updateAdditionalIds(instanceId, additionalIds);
                    }
                    changed = true;
//changedId++;
//changedIds.add(instanceIdObj);
//changedIdx.add(index);
//oldDomIds.add(oldIdomObj);
//newDomIds.add(newIdomIdObj);
                }
            }
        }
        if (dirtySet.size() != newDirtySet.size()) {
            dirtySetSameSize = 0;
        } else {
            dirtySetSameSize++;
        }
        dirtySet = newDirtySet;
//System.out.println("Processed: "+processedId);
//System.out.println("Changed:   "+changedId);
//System.out.println("-------------------");
//printObjs(changedIds,oldDomIds,newDomIds, addedBynewDirtySet, changedIdx);
//System.out.println("-------------------");
        return changed;
    }
        
    private void updateAdditionalIds(final long instanceId, final List<Long> additionalIds) {
        Instance i = heap.getInstanceByID(instanceId);
//System.out.println("Inspecting "+printInstance(instanceIdObj));
        if (i != null) {
            for (FieldValue v : i.getFieldValues()) {
                if (v instanceof ObjectFieldValue) {
                    Instance val = ((ObjectFieldValue)v).getInstance();
                    if (val != null) {
                        long idp = val.getInstanceId();
                        Long idO = new Long(idp);
                        long idomO = map.get(idp);
                        if (idomO > 0) {
                            additionalIds.add(idO);
//System.out.println("  Adding "+printInstance(idO));
                        }
                    }
                }
            }
        }
    }
    
    private void deleteBuffers() {
        multipleParents.delete();
        revertedMultipleParents.delete();
    }
        
    private long readLong() throws IOException {
        return currentMultipleParents.readLong();
    }
    
    long getIdomId(long instanceId, LongMap.Entry entry) {
        long idomEntry = map.get(instanceId);
        if (idomEntry != -1) {
            return idomEntry;
        }
        if (entry == null) {
            entry = heap.idToOffsetMap.get(instanceId);
        }
        return entry.getNearestGCRootPointer();
    }
    
    boolean hasInstanceInChain(int tag, Instance i) {
        ClassDump javaClass;
        long idom;
        long instanceId;
        
        if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
            return false;
        }        
        javaClass = (ClassDump) i.getJavaClass();
        if (canContainItself == null) {
            canContainItself = new HashMap<>(heap.getAllClasses().size()/2);
        }
        if (tag == HprofHeap.INSTANCE_DUMP) {
            Boolean canContain = canContainItself.get(javaClass);

            if (canContain == null) {
                canContain = Boolean.valueOf(javaClass.canContainItself());
                canContainItself.put(javaClass,canContain);
            }
            if (!canContain.booleanValue()) {
                return false;
            }
        }
        instanceId = i.getInstanceId();
        idom = getIdomId(instanceId);
        for (;idom!=0;idom=getIdomId(idom)) {
            Instance ip = heap.getInstanceByID(idom);
            JavaClass cls = ip.getJavaClass();
            
            if (javaClass.equals(cls)) {
                return true;
            }
        }
        return false;
    }

    private Long getNearestGCRootPointer(Long instanceIdLong) {
        LongMap.Entry entry;
        Long nearestGCLong = nearestGCRootCache.get(instanceIdLong);
        Long nearestGC;
        if (nearestGCLong != null) {
            return nearestGCLong;
        }
        entry = heap.idToOffsetMap.get(instanceIdLong.longValue());
        nearestGC = Long.valueOf(entry.getNearestGCRootPointer());
        nearestGCRootCache.put(instanceIdLong,nearestGC);
        return nearestGC;
    }
    
    private long getIdomId(long instanceIdLong) {
        long idom = map.get(instanceIdLong);
        
        if (idom != -1) {
            return idom;
        }
        return getNearestGCRootPointer(instanceIdLong);
    }
    
    private long intersect(long idomId, long refId) {
        if (idomId == refId) {
            return idomId;
        }
        if (idomId == 0 || refId == 0) {
            return 0;
        }
        LongSet leftIdoms = new LongSet(200);
        LongSet rightIdoms = new LongSet(200);        
        long leftIdom = idomId;
        long rightIdom = refId;

        
        leftIdoms.add(leftIdom);
        rightIdoms.add(rightIdom);
        while(true) {
            if (rightIdom == 0 && leftIdom == 0) return 0;
            if (leftIdom != 0) {
                leftIdom = getIdomId(leftIdom);
                if (leftIdom != 0) {
                    if (rightIdoms.contains(leftIdom)) {
                        return leftIdom;
                    }
                    leftIdoms.add(leftIdom);
                }
            }
            if (rightIdom != 0) {
                rightIdom = getIdomId(rightIdom);
                if (rightIdom != 0) {
                    if (leftIdoms.contains(rightIdom)) {
                        return rightIdom;
                    }
                    rightIdoms.add(rightIdom);
                }
            }
        }
    }

    private void switchParents() {
        if (currentMultipleParents == revertedMultipleParents) {
            currentMultipleParents = multipleParents;
        } else {
            currentMultipleParents = revertedMultipleParents;
        }
    }

    // debugging 
    private void printObjs(List<Long> changedIds, List<Long> oldDomIds, List<Long> newDomIds, List<Boolean> addedByDirtySet, List<Long> changedIdx) {
        if (changedIds.size()>20) return;
        TreeMap<Integer,String> m = new TreeMap<>();
        
        for (int i=0; i<changedIds.size(); i++) {
            Long iid = changedIds.get(i);
            Long oldDom = oldDomIds.get(i);
            Long newDom = newDomIds.get(i);
            Long index = changedIdx.get(i);
            Boolean addedByDirt = addedByDirtySet.get(i);
            Instance ii = heap.getInstanceByID(iid.longValue());
            int number = ii.getInstanceNumber();
            String text = "Index: "+index+(addedByDirt?" New ":" Old ")+printInstance(iid);
            
            text+=" OldDom "+printInstance(oldDom);
            text+=" NewDom: "+printInstance(newDom);
            m.put(number,text);
        }
        for (Integer in : m.keySet()) {
            System.out.println(m.get(in));
        }
    }
    
    // debugging
    String printInstance(Long instanceid) {
        if (instanceid == null || instanceid.longValue() == 0) {
            return "null";
        }
        Instance ii = heap.getInstanceByID(instanceid.longValue());
        return ii.getJavaClass().getName()+"#"+ii.getInstanceNumber();
        
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        map.writeToStream(out);
    }

    DominatorTree(HprofHeap h, DataInputStream dis) throws IOException {
        heap = h;
        map = new LongHashMap(dis);
    }
    
    private static final class NearestGCRootCache<K,V> extends LinkedHashMap<K,V> {
        private final int maxSize;
        
        private NearestGCRootCache(int size) {
            super(size,0.75F,true);
            maxSize = size;
        }

        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxSize;
        }

    }
}

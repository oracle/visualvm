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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
class TreeObject {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int BUFFER_SIZE = (64 * 1024) / 8;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HprofHeap heap;
    private LongBuffer readBuffer;
    private LongBuffer writeBuffer;
    private Set<Long> unique;
//private long nextLevelSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    TreeObject(HprofHeap h, LongBuffer leaves) {
        heap = h;
        writeBuffer = leaves;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------


    synchronized void computeTrees() {
        boolean changed;
        try {
            createBuffers();
            do {
                switchBuffers();
                changed = computeOneLevel();
//System.out.println("Tree obj.   "+heap.idToOffsetMap.treeObj);
//if (changed) System.out.println("Next level  "+nextLevelSize);
            } while (changed);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        deleteBuffers();
//System.out.println("Done!");
    }

    private boolean computeOneLevel() throws IOException {
//nextLevelSize = 0;
        boolean changed = false;
        int idSize = heap.dumpBuffer.getIDSize();
        for (;;) {
            long instanceId = readLong();
            Instance instance;
            List<FieldValue> fieldValues;
            Iterator<FieldValue> valuesIt;
            long retainedSize = 0;
            
            if (instanceId == 0) {  // end of level
                break;
            }
            instance = heap.getInstanceByID(instanceId);
            if (instance instanceof ObjectArrayInstance) {
                ObjectArrayDump array = (ObjectArrayDump) instance;
                int arrSize = array.getLength();
                long offset = array.getOffset();
                long size = 0;
                LongSet refs = new LongSet();
                
                for  (int i=0; i<arrSize && size != -1; i++) {
                    long refInstanceId = heap.dumpBuffer.getID(offset + (i * idSize));
                    size = checkInstance(refInstanceId, refs);
                    retainedSize += size;
                }
                changed |= processInstance(instance, size, retainedSize);
                continue;
            } else if (instance instanceof PrimitiveArrayInstance) {
                assert false:"Error - PrimitiveArrayInstance not allowed "+instance.getJavaClass().getName()+"#"+instance.getInstanceNumber();
                continue;
            } else if (instance instanceof ClassDumpInstance) {
                ClassDump javaClass = ((ClassDumpInstance) instance).classDump;
                
                fieldValues = javaClass.getStaticFieldValues();
            } else if (instance instanceof InstanceDump) {
                fieldValues = instance.getFieldValues();
            } else {
                if (instance == null) {
                    System.err.println("HeapWalker Warning - null instance for " + instanceId); // NOI18N
                    continue;
                }
                throw new IllegalArgumentException("Illegal type " + instance.getClass()); // NOI18N
            }
            long size = 0;
            LongSet refs = new LongSet();
            valuesIt = fieldValues.iterator();
            while (valuesIt.hasNext() && size != -1) {
                FieldValue val = valuesIt.next();
                
                if (val instanceof ObjectFieldValue) {
                    Instance refInstance = ((ObjectFieldValue) val).getInstance();
                    size = checkInstance(refInstance, refs);
                    retainedSize += size;
                }
            }
            changed |= processInstance(instance, size, retainedSize);
        }
        return changed;
    }
    
    private boolean processInstance(Instance instance, long size, long retainedSize) throws IOException {
        if (size != -1) {
            LongMap.Entry entry = heap.idToOffsetMap.get(instance.getInstanceId());
            entry.setRetainedSize(instance.getSize()+retainedSize);
            entry.setTreeObj();
            if (entry.hasOnlyOneReference()) {
                long gcRootPointer = entry.getNearestGCRootPointer();
                if (gcRootPointer != 0) {
                    if (unique.add(gcRootPointer)) {
                        writeLong(gcRootPointer);
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    private void createBuffers() {
        readBuffer = new LongBuffer(BUFFER_SIZE, heap.cacheDirectory);
    }
    
    private void deleteBuffers() {
        readBuffer.delete();
        writeBuffer.delete();
    }
        
    private long readLong() throws IOException {
        return readBuffer.readLong();
    }
    
    private void switchBuffers() throws IOException {
        LongBuffer b = readBuffer;
        readBuffer = writeBuffer;
        writeBuffer = b;
        readBuffer.startReading();
        writeBuffer.reset();
        unique = new HashSet<>(4000);
    }
    
    private void writeLong(long instanceId) throws IOException {
        if (instanceId != 0) {
            writeBuffer.writeLong(instanceId);
//nextLevelSize++;
        }
    }
    
    private long checkInstance(Instance refInstance, LongSet refs) throws IOException {
        if (refInstance != null) {
            return checkInstance(refInstance.getInstanceId(), refs);
        }
        return 0;
    }
    
    private long checkInstance(long refInstanceId, LongSet refs) throws IOException {
        if (refInstanceId != 0L) {
            LongMap.Entry refEntry = heap.idToOffsetMap.get(refInstanceId);
            
            if (refEntry == null) {
                return 0;
            }
            if (!refEntry.hasOnlyOneReference()) {
                return -1;
            }
            if (!refEntry.isTreeObj()) {
                return -1;
            }
            if (refs.add(refInstanceId)) {
                return 0;
            }
            return refEntry.getRetainedSize();
        }
        return 0;
    }
}

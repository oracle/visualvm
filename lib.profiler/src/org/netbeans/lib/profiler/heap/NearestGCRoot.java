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

package org.netbeans.lib.profiler.heap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 * @author Tomas Hurka
 */
class NearestGCRoot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int BUFFER_SIZE = (64 * 1024) / 8;
    private static final String[] REF_CLASSES = {
        "java.lang.ref.WeakReference",    // NOI18N
        "java.lang.ref.SoftReference",    // NOI18N
        "java.lang.ref.FinalReference",   // NOI18N
        "java.lang.ref.PhantomReference"  // NOI18N
    };
    private static final String JAVA_LANG_REF_REFERENCE = "java.lang.ref.Reference";   // NOI18N
    private static final String REFERENT_FILED_NAME = "referent"; // NOI18N
    private static final String SVM_REFFERENCE = "com.oracle.svm.core.heap.heapImpl.DiscoverableReference";    // NOI18N
    private static final String SVM_REFERENT_FILED_NAME = "rawReferent"; // NOI18N
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Field referentFiled;
    private HprofHeap heap;
    private LongBuffer readBuffer;
    private LongBuffer writeBuffer;
    private LongBuffer leaves;
    private LongBuffer multipleParents;
    private Set referenceClasses;
    private boolean gcRootsComputed;
    private long allInstances;
    private long processedInstances;
//private long leavesCount;
//private long firstLevel;
//private long multiParentsCount;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    NearestGCRoot(HprofHeap h) {
        heap = h;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    Instance getNearestGCRootPointer(Instance instance) {
        if (heap.getGCRoot(instance) != null) {
            return instance;
        }
        computeGCRoots();
        long nextGCPathId = heap.idToOffsetMap.get(instance.getInstanceId()).getNearestGCRootPointer();
        return heap.getInstanceByID(nextGCPathId);
    }

    private boolean isSpecialReference(FieldValue value, Instance instance) {
        Field f = value.getField();

        return f.equals(referentFiled) && referenceClasses.contains(instance.getJavaClass());
    }

    private synchronized void computeGCRoots() {
        if (gcRootsComputed) {
            return;
        }
        if (!initHotSpotReference()) {
            if (!initSVMReference()) {
                throw new IllegalArgumentException("reference field not found"); // NOI18N
            }
        }
        heap.computeReferences(); // make sure references are computed first
        allInstances = heap.getSummary().getTotalLiveInstances();
        Set processedClasses = new HashSet(heap.getAllClasses().size()*4/3);
        
        try {
            createBuffers();
            fillZeroLevel();

            do {
                switchBuffers();
                computeOneLevel(processedClasses);
            } while (hasMoreLevels());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        deleteBuffers();
        heap.idToOffsetMap.flush();
        HeapProgress.progressFinish();
        gcRootsComputed = true;
    }

    private boolean initHotSpotReference() {
        referentFiled = computeReferentFiled(JAVA_LANG_REF_REFERENCE, REFERENT_FILED_NAME);
        if (referentFiled != null) {
            referenceClasses = new HashSet();
            for (int i=0; i<REF_CLASSES.length; i++) {
                JavaClass ref = heap.getJavaClassByName(REF_CLASSES[i]);
                if (ref != null) {
                    referenceClasses.add(ref);
                    referenceClasses.addAll(ref.getSubClasses());
                }
            }
            return referenceClasses.size() >= REF_CLASSES.length;
        }
        return false;
    }

    private boolean initSVMReference() {
        referentFiled = computeReferentFiled(SVM_REFFERENCE, SVM_REFERENT_FILED_NAME);
        if (referentFiled != null) {
            JavaClass ref = referentFiled.getDeclaringClass();

            referenceClasses = new HashSet();
            referenceClasses.add(ref);
            referenceClasses.addAll(ref.getSubClasses());
            return !referenceClasses.isEmpty();
        }
        return false;
    }

    private void computeOneLevel(Set processedClasses) throws IOException {
        int idSize = heap.dumpBuffer.getIDSize();
        for (;;) {
            Instance instance;
            long instanceOffset = readLong();
            List fieldValues;
            Iterator valuesIt;
            boolean hasValues = false;
            
            if (instanceOffset == 0L) { // end of level
                break;
            }
            HeapProgress.progress(processedInstances++,allInstances);
            instance = heap.getInstanceByOffset(new long[] {instanceOffset});
            if (instance instanceof ObjectArrayInstance) {
                ObjectArrayDump array = (ObjectArrayDump) instance;
                int size = array.getLength();
                long offset = array.getOffset();
                long instanceId = instance.getInstanceId();

                for (int i=0;i<size;i++) {
                    long referenceId = heap.dumpBuffer.getID(offset + (i * idSize));

                    if (writeConnection(instanceId, referenceId)) {
                        hasValues = true;
                    }
                }
                if (!hasValues) {
                    writeLeaf(instanceId,instance.getSize());
                }
                continue;
            } else if (instance instanceof PrimitiveArrayInstance) {
                writeLeaf(instance.getInstanceId(),instance.getSize());
                continue;
            } else if (instance instanceof ClassDumpInstance) {
                ClassDump javaClass = ((ClassDumpInstance) instance).classDump;

                fieldValues = javaClass.getStaticFieldValues();
            } else if (instance instanceof InstanceDump) {
                fieldValues = instance.getFieldValues();
            } else {
                if (instance == null) {
                    System.err.println("HeapWalker Warning - null instance for " + heap.dumpBuffer.getID(instanceOffset + 1)); // NOI18N
                    continue;
                }
                throw new IllegalArgumentException("Illegal type " + instance.getClass()); // NOI18N
            }
            long instanceId = instance.getInstanceId();
            valuesIt = fieldValues.iterator();
            while (valuesIt.hasNext()) {
                FieldValue val = (FieldValue) valuesIt.next();

                if (val instanceof ObjectFieldValue) {
                     // skip Soft, Weak, Final and Phantom References
                    if (!isSpecialReference(val, instance)) {
                        long refInstanceId;

                        if (val instanceof HprofFieldObjectValue) {
                            refInstanceId = ((HprofFieldObjectValue) val).getInstanceID();
                        } else {
                             refInstanceId = ((HprofInstanceObjectValue) val).getInstanceId();
                        }
                        if (writeConnection(instanceId, refInstanceId)) {
                            hasValues = true;
                        }
                    }
                }
            }
            if (writeClassConnection(processedClasses, instanceId, instance.getJavaClass())) {
                hasValues = true;
            }
            if (!hasValues) {
                writeLeaf(instanceId,instance.getSize());
            }

        }
    }

    private Field computeReferentFiled(String className, String fieldName) {
        JavaClass reference = heap.getJavaClassByName(className);

        if (reference != null) {
            Iterator fieldRef = reference.getFields().iterator();

            while (fieldRef.hasNext()) {
                Field f = (Field) fieldRef.next();

                if (f.getName().equals(fieldName)) {

                    return f;
                }
            }
        }
        return null;
    }

    private void createBuffers() {
        readBuffer = new LongBuffer(BUFFER_SIZE, heap.cacheDirectory);
        writeBuffer = new LongBuffer(BUFFER_SIZE, heap.cacheDirectory);
        leaves = new LongBuffer(BUFFER_SIZE, heap.cacheDirectory);
        multipleParents = new LongBuffer(BUFFER_SIZE, heap.cacheDirectory);
    }

    private void deleteBuffers() {
        readBuffer.delete();
        writeBuffer.delete();
    }

    private void fillZeroLevel() throws IOException {
        Iterator gcIt = heap.getGCRoots().iterator();

        while (gcIt.hasNext()) {
            HprofGCRoot root = (HprofGCRoot) gcIt.next();
            long id = root.getInstanceId();
            LongMap.Entry entry = heap.idToOffsetMap.get(id);
            
            if (entry != null) {
                writeLong(entry.getOffset());
            }
        }
    }

    private boolean hasMoreLevels() {
        return writeBuffer.hasData();
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
    }

    private boolean writeClassConnection(final Set processedClasses, final long instanceId, final JavaClass jcls) throws IOException {
        if (!processedClasses.contains(jcls)) {
            long jclsId = jcls.getJavaClassId();
            
            processedClasses.add(jcls);
            if (writeConnection(instanceId, jclsId, true)) {
                return true;
            }
        }
        return false;
    }

    private boolean writeConnection(long instanceId, long refInstanceId)
                          throws IOException {
        return writeConnection(instanceId, refInstanceId, false);
    }
    
    private boolean writeConnection(long instanceId, long refInstanceId, boolean addRefInstanceId)
                          throws IOException {
        if (refInstanceId != 0) {
            LongMap.Entry entry = heap.idToOffsetMap.get(refInstanceId);

            if (entry != null && entry.getNearestGCRootPointer() == 0L && heap.gcRoots.getGCRoot(refInstanceId) == null) {
                writeLong(entry.getOffset());
                if (addRefInstanceId) {
                    if (!checkReferences(refInstanceId, instanceId)) {
                        entry.addReference(instanceId);
                    }
                }
                entry.setNearestGCRootPointer(instanceId);
                if (!entry.hasOnlyOneReference()) {
                    multipleParents.writeLong(refInstanceId);
//multiParentsCount++;
                }
                return true;
            }
            return !addRefInstanceId && entry != null;
        }
        return false;
    }

    private boolean checkReferences(final long refInstanceId, final long instanceId) {
        Instance instance = heap.getInstanceByID(instanceId);        
        Iterator fieldIt = instance.getFieldValues().iterator();
        
        while (fieldIt.hasNext()) {
            Object field = fieldIt.next();

            if (field instanceof HprofInstanceObjectValue) {
                HprofInstanceObjectValue objectValue = (HprofInstanceObjectValue) field;

                if (objectValue.getInstanceId() == refInstanceId) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeLong(long instanceOffset) throws IOException {
        writeBuffer.writeLong(instanceOffset);
    }

    private void writeLeaf(long instanceId, long size) throws IOException {
        LongMap.Entry entry = heap.idToOffsetMap.get(instanceId);
        
        entry.setTreeObj();
        entry.setRetainedSize(size);
//leavesCount++;
        if (entry.hasOnlyOneReference()) {
            long gcRootPointer = entry.getNearestGCRootPointer();
            if (gcRootPointer != 0) {
                LongMap.Entry gcRootPointerEntry = heap.idToOffsetMap.get(gcRootPointer);
                
                if (gcRootPointerEntry.getRetainedSize() == 0) {
                    gcRootPointerEntry.setRetainedSize(-1);
                    leaves.writeLong(gcRootPointer);
//firstLevel++;
                }
            }
        }
    }

    LongBuffer getLeaves() {
        computeGCRoots();
//System.out.println("Multi par.  "+multiParentsCount);
//System.out.println("Leaves      "+leavesCount);
//System.out.println("Tree obj.   "+heap.idToOffsetMap.treeObj);
//System.out.println("First level "+firstLevel);
        return leaves;
    }
    
    LongBuffer getMultipleParents() {
        computeGCRoots();
        return multipleParents;
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeBoolean(gcRootsComputed);
        if (gcRootsComputed) {
            leaves.writeToStream(out);
            multipleParents.writeToStream(out);
        }
    }

    NearestGCRoot(HprofHeap h, DataInputStream dis) throws IOException {
        this(h);
        gcRootsComputed = dis.readBoolean();
        if (gcRootsComputed) {
            leaves = new LongBuffer(dis, heap.cacheDirectory);
            multipleParents = new LongBuffer(dis, heap.cacheDirectory);
        }
    }
}

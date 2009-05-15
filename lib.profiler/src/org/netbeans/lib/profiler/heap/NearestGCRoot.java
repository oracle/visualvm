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

package org.netbeans.lib.profiler.heap;

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
        referenceClasses = new HashSet();
        for (int i=0; i<REF_CLASSES.length; i++) {
            JavaClass ref = heap.getJavaClassByName(REF_CLASSES[i]);
            referenceClasses.add(ref);
            referenceClasses.addAll(ref.getSubClasses());
        }
        referentFiled = computeReferentFiled();
        heap.computeReferences(); // make sure references are computed first
        allInstances = heap.getSummary().getTotalLiveInstances();
        
        try {
            createBuffers();
            fillZeroLevel();

            do {
                switchBuffers();
                computeOneLevel();
            } while (hasMoreLevels());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        deleteBuffers();
        heap.idToOffsetMap.flush();
        HeapProgress.progressFinish();
        gcRootsComputed = true;
    }

    private void computeOneLevel() throws IOException {
        for (;;) {
            long instanceId = readLong();
            Instance instance;
            List fieldValues;
            Iterator valuesIt;
            boolean hasValues = false;
            
            if (instanceId == 0L) { // end of level
                break;
            }
            HeapProgress.progress(processedInstances++,allInstances);
            instance = heap.getInstanceByID(instanceId);
            if (instance instanceof ObjectArrayInstance) {
                Iterator instanceIt = ((ObjectArrayInstance) instance).getValues().iterator();

                while (instanceIt.hasNext()) {
                    Instance refInstance = (Instance) instanceIt.next();
                    writeConnection(instanceId, refInstance);
                    if (refInstance != null) {
                        hasValues = true;
                    }
                }
                if (!hasValues) {
                    writeLeaf(instanceId,instance.getSize());
                }
                continue;
            } else if (instance instanceof PrimitiveArrayInstance) {
                writeLeaf(instanceId,instance.getSize());
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
            valuesIt = fieldValues.iterator();
            while (valuesIt.hasNext()) {
                FieldValue val = (FieldValue) valuesIt.next();

                if (val instanceof ObjectFieldValue) {
                     // skip Soft, Weak, Final and Phantom References
                    if (!isSpecialReference(val, instance)) {
                        Instance refInstance = ((ObjectFieldValue) val).getInstance();
                        
                        writeConnection(instanceId, refInstance);
                        if (refInstance != null) {
                            hasValues = true;
                        }
                    }
                }
            }
            if (!hasValues) {
                writeLeaf(instanceId,instance.getSize());
            }

        }
    }

    private Field computeReferentFiled() {
        JavaClass reference = heap.getJavaClassByName(JAVA_LANG_REF_REFERENCE);
        Iterator fieldRef = reference.getFields().iterator();

        while (fieldRef.hasNext()) {
            Field f = (Field) fieldRef.next();

            if (f.getName().equals(REFERENT_FILED_NAME)) {

                return f;
            }
        }

        throw new IllegalArgumentException("reference field not found in " + reference.getName()); // NOI18N
    }

    private void createBuffers() {
        readBuffer = new LongBuffer(BUFFER_SIZE);
        writeBuffer = new LongBuffer(BUFFER_SIZE);
        leaves = new LongBuffer(BUFFER_SIZE);
        multipleParents = new LongBuffer(BUFFER_SIZE);
    }

    private void deleteBuffers() {
        readBuffer.delete();
        writeBuffer.delete();
    }

    private void fillZeroLevel() throws IOException {
        Iterator gcIt = heap.getGCRoots().iterator();

        while (gcIt.hasNext()) {
            HprofGCRoot root = (HprofGCRoot) gcIt.next();

            writeLong(root.getInstanceId());
        }
    }

    private boolean hasMoreLevels() {
        return writeBuffer.hasData();
    }

    private long readLong() throws IOException {
        return readBuffer.readLong();
    }

    private void switchBuffers() {
        LongBuffer b = readBuffer;
        readBuffer = writeBuffer;
        writeBuffer = b;
        readBuffer.startReading();
        writeBuffer.reset();
    }

    private void writeConnection(long instanceId, Instance refInstance)
                          throws IOException {
        if (refInstance != null) {
            long refInstanceId = refInstance.getInstanceId();
            LongMap.Entry entry = heap.idToOffsetMap.get(refInstanceId);

            if (entry.getNearestGCRootPointer() == 0L && heap.getGCRoot(refInstance) == null) {
                writeLong(refInstanceId);
                entry.setNearestGCRootPointer(instanceId);
                if (!entry.hasOnlyOneReference()) {
                    multipleParents.writeLong(refInstanceId);
//multiParentsCount++;
                }
            }
        }
    }

    private void writeLong(long instanceId) throws IOException {
        writeBuffer.writeLong(instanceId);
    }

    private void writeLeaf(long instanceId, int size) throws IOException {
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
}

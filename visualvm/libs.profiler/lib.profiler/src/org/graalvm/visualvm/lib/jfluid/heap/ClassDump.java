/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 *
 * @author Tomas Hurka
 */
class ClassDump extends HprofObject implements JavaClass {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final Set<String> CANNOT_CONTAIN_ITSELF = new HashSet<>(Arrays.asList(
        "java.lang.String",         // NOI18N
        "java.lang.StringBuffer",   // NOI18N
        "java.lang.StringBuilder",  // NOI18N
        "java.io.File"              // NOI18N   
        ));

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    final ClassDumpSegment classDumpSegment;
    private int instances;
    private long firstInstanceOffset;
    private long loadClassOffset;
    private long retainedSizeByClass;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    ClassDump(ClassDumpSegment segment, long offset) {
        super(offset);
        classDumpSegment = segment;
        assert getHprofBuffer().get(offset) == HprofHeap.CLASS_DUMP;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getAllInstancesSize() {
        Long allInstancesSizeArr = (Long) classDumpSegment.arrayMap.get(this);

        if (allInstancesSizeArr != null) {
            return allInstancesSizeArr.longValue();
        }

        return ((long)getInstancesCount()) * getInstanceSize();
    }

    public boolean isArray() {
        boolean isArrayWithInstances = classDumpSegment.arrayMap.get(this) != null;;

        if (isArrayWithInstances) {
            return true;
        }
        if (instances != 0) {
            return false;
        }
        return getName().endsWith("[]");
    }

    public Instance getClassLoader() {
        return getHprof().getInstanceByID(getClassLoaderId());
    }

    public Field getField(String name) {
        List<Field> fields = getFields();

        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    public List /*<Field>*/ getFields() {
        List<Field> filedsList = classDumpSegment.fieldsCache.get(this);
        if (filedsList == null) {
            filedsList = Collections.unmodifiableList(computeFields());
            classDumpSegment.fieldsCache.put(this,filedsList);
        }
        return filedsList;
    }

    public int getInstanceSize() {
        if (isArray()) {
            return -1;
        }

        int size = getRawInstanceSize();
        if (!classDumpSegment.newSize) {
            size += classDumpSegment.getMinimumInstanceSize();
        }
        return size;
    }

    public long getRetainedSizeByClass() {
        getHprof().computeRetainedSizeByClass();
        return retainedSizeByClass;
    }

    public List /*<Instance>*/ getInstances() {
        int instancesCount = getInstancesCount();

        if (instancesCount == 0) {
            return Collections.EMPTY_LIST;
        }

        long classId = getJavaClassId();
        HprofHeap heap = getHprof();
        HprofByteBuffer dumpBuffer = getHprofBuffer();
        int idSize = dumpBuffer.getIDSize();
        List<Instance> instancesList = new ArrayList<>(instancesCount);
        TagBounds allInstanceDumpBounds = heap.getAllInstanceDumpBounds();
        long[] offset = new long[] { firstInstanceOffset };

        while (offset[0] < allInstanceDumpBounds.endOffset) {
            long start = offset[0];
            int classIdOffset = 0;
            long instanceClassId = 0L;
            int tag = heap.readDumpTag(offset);
            Instance instance;

            if (tag == HprofHeap.INSTANCE_DUMP) {
                classIdOffset = idSize + 4;
            } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                classIdOffset = idSize + 4 + 4;
            } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                byte type = dumpBuffer.get(start + 1 + idSize + 4 + 4);
                instanceClassId = classDumpSegment.getPrimitiveArrayClass(type).getJavaClassId();
            }

            if (classIdOffset != 0) {
                instanceClassId = dumpBuffer.getID(start + 1 + classIdOffset);
            }

            if (instanceClassId == classId) {
                if (tag == HprofHeap.INSTANCE_DUMP) {
                    instance = new InstanceDump(this, start);
                } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                    instance = new ObjectArrayDump(this, start);
                } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                    instance = new PrimitiveArrayDump(this, start);
                } else {
                    throw new IllegalArgumentException("Illegal tag " + tag); // NOI18N
                }

                instancesList.add(instance);

                if (--instancesCount == 0) {
                    return instancesList;
                }
            }
        }

        if (DEBUG) {
            System.out.println("Class " + getName() + " Col " + instancesList.size() + " instances " + getInstancesCount()); // NOI18N
        }

        return instancesList;
    }

    public Iterator /*<Instance>*/ getInstancesIterator() {
        int instancesCount = getInstancesCount();
        if (instancesCount == 0) {
            return Collections.EMPTY_LIST.iterator();
        }
        return new InstancesIterator(instancesCount);
    }

    public int getInstancesCount() {
        if (instances == 0) {
            getHprof().computeInstances();
        }

        return instances;
    }

    public long getJavaClassId() {
        return getHprofBuffer().getID(fileOffset + classDumpSegment.classIDOffset);
    }

    public String getName() {
        return getLoadClass().getName();
    }

    public List /*<FieldValue>*/ getStaticFieldValues() {
        return getStaticFieldValues(true);
    }

    public Collection /*<JavaClass>*/ getSubClasses() {
        List<JavaClass> classes = getHprof().getAllClasses();
        List<JavaClass> subclasses = new ArrayList<>(classes.size() / 10);
        Map<JavaClass, Boolean> subclassesMap = new HashMap<>((classes.size() * 4) / 3);

        subclassesMap.put(this, Boolean.TRUE);

        for (int i = 0; i < classes.size(); i++) {
            JavaClass jcls = classes.get(i);
            Boolean b = subclassesMap.get(jcls);

            if (b == null) {
                b = isSubClass(jcls, subclassesMap);
            }

            if (b.booleanValue() && (jcls != this)) {
                subclasses.add(jcls);
            }
        }

        return subclasses;
    }

    public JavaClass getSuperClass() {
        long superClassId = getHprofBuffer().getID(fileOffset + classDumpSegment.superClassIDOffset);

        return classDumpSegment.getClassDumpByID(superClassId);
    }

    public Object getValueOfStaticField(String name) {
        List<FieldValue> staticFieldValues = getStaticFieldValues();

        for (FieldValue fieldValue : staticFieldValues) {
            if (fieldValue.getField().getName().equals(name)) {
                if (fieldValue instanceof HprofFieldObjectValue) {
                    return ((HprofFieldObjectValue) fieldValue).getInstance();
                } else {
                    return ((HprofFieldValue) fieldValue).getTypeValue();
                }
            }
        }

        return null;
    }

    private List<Field> computeFields() {
        HprofByteBuffer buffer = getHprofBuffer();
        long offset = fileOffset + getInstanceFieldOffset();
        int i;
        int fields = buffer.getShort(offset);
        List<Field> filedsList = new ArrayList<>(fields);

        for (i = 0; i < fields; i++) {
            filedsList.add(new HprofField(this, offset + 2 + (i * classDumpSegment.fieldSize)));
        }

        return filedsList;
    }

    List /*<FieldValue>*/ getStaticFieldValues(boolean addClassLoader) {
        HprofByteBuffer buffer = getHprofBuffer();
        long offset = fileOffset + getStaticFieldOffset();
        int i;
        int fields;
        List<FieldValue> filedsList;
        HprofHeap heap = getHprof();

        fields = buffer.getShort(offset);
        offset += 2;
        filedsList = new ArrayList<>(fields+(addClassLoader?0:1));

        for (i = 0; i < fields; i++) {
            byte type = buffer.get(offset + classDumpSegment.fieldTypeOffset);
            int fieldSize = classDumpSegment.fieldSize + heap.getValueSize(type);
            HprofFieldValue value;

            if (type == HprofHeap.OBJECT) {
                value = new HprofFieldObjectValue(this, offset);
            } else {
                value = new HprofFieldValue(this, offset);
            }

            filedsList.add(value);
            offset += fieldSize;
        }
        if (addClassLoader) {
            long classLoaderOffset = fileOffset + classDumpSegment.classLoaderIDOffset;
            
            filedsList.add(new ClassLoaderFieldValue(this, classLoaderOffset));
        }
        return filedsList;
    }
    
    List getAllInstanceFields() {
        List<Field> fields = new ArrayList<>(50);

        for (JavaClass jcls = this; jcls != null; jcls = jcls.getSuperClass()) {
            fields.addAll(jcls.getFields());
        }

        return fields;
    }

    void setClassLoadOffset(long offset) {
        loadClassOffset = offset;
    }

    int getConstantPoolSize() {
        long cpOffset = fileOffset + classDumpSegment.constantPoolSizeOffset;
        HprofByteBuffer buffer = getHprofBuffer();
        int cpRecords = buffer.getShort(cpOffset);
        HprofHeap heap = getHprof();

        cpOffset += 2;

        for (int i = 0; i < cpRecords; i++) {
            byte type = buffer.get(cpOffset + 2);
            int size = heap.getValueSize(type);
            cpOffset += (2 + 1 + size);
        }

        return (int) (cpOffset - (fileOffset + classDumpSegment.constantPoolSizeOffset));
    }

    int getRawInstanceSize() {
        return getHprofBuffer().getInt(fileOffset + classDumpSegment.instanceSizeOffset);
    }

    HprofHeap getHprof() {
        return classDumpSegment.hprofHeap;
    }

    HprofByteBuffer getHprofBuffer() {
        return classDumpSegment.hprofHeap.dumpBuffer;
    }

    int getInstanceFieldOffset() {
        int staticFieldOffset = getStaticFieldOffset();

        return staticFieldOffset + getStaticFiledSize(staticFieldOffset);
    }

    LoadClass getLoadClass() {
        return new LoadClass(getHprof().getLoadClassSegment(), loadClassOffset);
    }

    long getClassLoaderId() {
        return getHprofBuffer().getID(fileOffset + classDumpSegment.classLoaderIDOffset);
    }

    List getReferences() {
        return getHprof().findReferencesFor(getJavaClassId());
    }

    int getStaticFieldOffset() {
        return classDumpSegment.constantPoolSizeOffset + getConstantPoolSize();
    }

    int getStaticFiledSize(int staticFieldOffset) {
        int i;
        HprofByteBuffer buffer = getHprofBuffer();
        int idSize = buffer.getIDSize();
        long fieldOffset = fileOffset + staticFieldOffset;
        int fields = buffer.getShort(fieldOffset);
        HprofHeap heap = getHprof();

        fieldOffset += 2;

        for (i = 0; i < fields; i++) {
            byte type = buffer.get(fieldOffset + idSize);
            int size = heap.getValueSize(type);
            fieldOffset += (idSize + 1 + size);
        }

        return (int) (fieldOffset - staticFieldOffset - fileOffset);
    }

    void findStaticReferencesFor(long instanceId, List<Value> refs) {
        int i;
        HprofByteBuffer buffer = getHprofBuffer();
        int idSize = buffer.getIDSize();
        long fieldOffset = fileOffset + getStaticFieldOffset();
        int fields = buffer.getShort(fieldOffset);
        List<FieldValue> staticFileds = null;
        HprofHeap heap = getHprof();

        fieldOffset += 2;

        for (i = 0; i < fields; i++) {
            byte type = buffer.get(fieldOffset + idSize);
            int size = heap.getValueSize(type);

            if ((type == HprofHeap.OBJECT) && (instanceId == buffer.getID(fieldOffset + idSize + 1))) {
                if (staticFileds == null) {
                    staticFileds = getStaticFieldValues();
                }

                refs.add(staticFileds.get(i));
            }

            fieldOffset += (idSize + 1 + size);
        }
        if (instanceId == getClassLoaderId()) {
            if (staticFileds == null) {
                staticFileds = getStaticFieldValues();
            }
            refs.add(staticFileds.get(fields));
        }
    }

    void registerInstance(long offset) {
        instances++;
        if (firstInstanceOffset == 0) {
            firstInstanceOffset = offset;
            if (DEBUG) {
                System.out.println("First instance :"+getName()+" "+offset/1024/1024); // NOI18N
            }
        }
    }

    void addSizeForInstance(Instance i) {
        retainedSizeByClass+=i.getRetainedSize();
    }

    boolean canContainItself() {
        if (getInstancesCount()>=2 && !CANNOT_CONTAIN_ITSELF.contains(getName())) {
            List<Field> allInstanceFields = getAllInstanceFields();

            for (Field f : allInstanceFields) {
                if (f.getType().getName().equals("object")) {   // NOI18N
                    return true;
                }
            }
        }
        if (DEBUG) {
            if (instances>10) System.out.println(getName()+" cannot contain itself "+instances);    // NOI18N
        }
        return false;
    }
    
    private static Boolean isSubClass(JavaClass jcls, Map<JavaClass, Boolean> subclassesMap) {
        JavaClass superClass = jcls.getSuperClass();
        Boolean b;

        if (superClass == null) {
            b = Boolean.FALSE;
        } else {
            b = subclassesMap.get(superClass);

            if (b == null) {
                b = isSubClass(superClass, subclassesMap);
            }
        }

        subclassesMap.put(jcls, b);

        return b;
    }
    
    private class InstancesIterator implements Iterator<Instance> {
        
        private long instancesCount;
        private long[] offset;
        TagBounds allInstanceDumpBounds;
        HprofHeap heap;
        long classId;
        
        InstancesIterator(long ic) {
            instancesCount = ic;
            allInstanceDumpBounds = getHprof().getAllInstanceDumpBounds();
            offset = new long[] { firstInstanceOffset };
            heap = getHprof();
            classId = getJavaClassId();

        }

        
        public boolean hasNext() {
            if (instancesCount>0 && offset[0] < allInstanceDumpBounds.endOffset) {
                return true;
            }
            return false;
        }

        public Instance next() {
            while (hasNext()) {
                Instance i = heap.getInstanceByOffset(offset, ClassDump.this, classId);
                if (i != null) {
                    instancesCount--;
                    return i;
                }
            }
            throw new NoSuchElementException();
        } 
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeLong(fileOffset);
        out.writeInt(instances);
        out.writeLong(firstInstanceOffset);
        out.writeLong(loadClassOffset);
        out.writeLong(retainedSizeByClass);        
    }

    ClassDump(ClassDumpSegment segment, long offset, DataInputStream dis) throws IOException {
        this(segment, offset);
        instances = dis.readInt();
        firstInstanceOffset = dis.readLong();
        loadClassOffset = dis.readLong();
        retainedSizeByClass = dis.readLong();        
    }

}

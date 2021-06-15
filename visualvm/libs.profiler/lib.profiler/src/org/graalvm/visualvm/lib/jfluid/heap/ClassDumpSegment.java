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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 *
 * @author Tomas Hurka
 */
class ClassDumpSegment extends TagBounds {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofHeap hprofHeap;
    // Map <JavaClass represeting array,Long - allInstanceSize>
    Map<JavaClass,Long> arrayMap;
    final int classIDOffset;
    final int classLoaderIDOffset;
    final int constantPoolSizeOffset;
    final int fieldNameIDOffset;
    final int fieldSize;
    final int fieldTypeOffset;
    final int fieldValueOffset;
    final int instanceSizeOffset;
    final int minimumInstanceSize;
    final int protectionDomainIDOffset;
    final int reserved1;
    final int reserver2;
    final int signersID;
    final int stackTraceSerialNumberOffset;
    final int superClassIDOffset;
    ClassDump java_lang_Class;
    boolean newSize;
    Map<JavaClass,List<Field>> fieldsCache;
    private List<JavaClass> classes;
    private Map<Integer,JavaClass> primitiveArrayMap;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    ClassDumpSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.CLASS_DUMP, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        // initialize offsets
        classIDOffset = 1;
        stackTraceSerialNumberOffset = classIDOffset + idSize;
        superClassIDOffset = stackTraceSerialNumberOffset + 4;
        classLoaderIDOffset = superClassIDOffset + idSize;
        signersID = classLoaderIDOffset + idSize;
        protectionDomainIDOffset = signersID + idSize;
        reserved1 = protectionDomainIDOffset + idSize;
        reserver2 = reserved1 + idSize;
        instanceSizeOffset = reserver2 + idSize;
        constantPoolSizeOffset = instanceSizeOffset + 4;

        fieldNameIDOffset = 0;
        fieldTypeOffset = fieldNameIDOffset + idSize;
        fieldValueOffset = fieldTypeOffset + 1;

        fieldSize = fieldTypeOffset + 1;

        minimumInstanceSize = 2 * idSize;
        
        fieldsCache = Collections.synchronizedMap(new FieldsCache());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    ClassDump getClassDumpByID(long classObjectID) {
        if (classObjectID == 0) {
            return null;
        }
        List allClasses = createClassCollection();
        LongMap.Entry entry = hprofHeap.idToOffsetMap.get(classObjectID);

        if (entry != null) {
            try {
                ClassDump dump = (ClassDump) allClasses.get(entry.getIndex() - 1);
                if (dump.fileOffset == entry.getOffset()) {
                    return dump;
                }
            } catch (IndexOutOfBoundsException ex) { // classObjectID do not reffer to ClassDump, its instance number is > classes.size()

                return null;
            } catch (ClassCastException ex) { // classObjectID do not reffer to ClassDump

                return null;
            }
        }

        return null;
    }

    JavaClass getJavaClassByName(String fqn) {
        for (JavaClass cls : createClassCollection()) {
            if (fqn.equals(cls.getName())) {
                return cls;
            }
        }

        return null;
    }

    Collection<JavaClass> getJavaClassesByRegExp(String regexp) {
        Collection result = new ArrayList(256);
        Pattern pattern = Pattern.compile(regexp);
        
        for (JavaClass cls : createClassCollection()) {
            if (pattern.matcher(cls.getName()).matches()) {
                result.add(cls);
            }
        }
        return result;
    }

    int getMinimumInstanceSize() {
        return minimumInstanceSize;
    }

    ClassDump getPrimitiveArrayClass(byte type) {
        ClassDump primitiveArray = (ClassDump) primitiveArrayMap.get(Integer.valueOf(type));

        if (primitiveArray == null) {
            throw new IllegalArgumentException("Invalid type " + type); // NOI18N
        }

        return primitiveArray;
    }

    Map getClassIdToClassMap() {
        List<JavaClass> allClasses = createClassCollection();
        Map map = new HashMap(allClasses.size()*4/3);
        
        for (JavaClass cls : allClasses) {
            map.put(new Long(cls.getJavaClassId()),cls);
        }
        return map;
    }
    
    void addInstanceSize(ClassDump cls, int tag, long instanceOffset) {
        if ((tag == HprofHeap.OBJECT_ARRAY_DUMP) || (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP)) {
            Long sizeLong = arrayMap.get(cls);
            long size = 0;
            HprofByteBuffer dumpBuffer = hprofHeap.dumpBuffer;
            int idSize = dumpBuffer.getIDSize();
            long elementsOffset = instanceOffset + 1 + idSize + 4;

            if (sizeLong != null) {
                size = sizeLong.longValue();
            }

            int elements = dumpBuffer.getInt(elementsOffset);
            int elSize;

            if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                elSize = hprofHeap.getValueSize(dumpBuffer.get(elementsOffset + 4));
            } else {
                elSize = idSize;
            }

            size += (getMinimumInstanceSize() + ArrayDump.HPROF_ARRAY_OVERHEAD + (((long)elements) * elSize));
            arrayMap.put(cls, Long.valueOf(size));
        }
    }

    synchronized List<JavaClass> createClassCollection() {
        if (classes != null) {
            return classes;
        }

        List<JavaClass> cls = new ArrayList /*<JavaClass>*/(1000);

        long[] offset = new long[] { startOffset };

        while (offset[0] < endOffset) {
            long start = offset[0];
            int tag = hprofHeap.readDumpTag(offset);

            if (tag == HprofHeap.CLASS_DUMP) {
                ClassDump classDump = new ClassDump(this, start);
                long classId = classDump.getJavaClassId();
                LongMap.Entry classEntry = hprofHeap.idToOffsetMap.put(classId, start);

                cls.add(classDump);
                classEntry.setIndex(cls.size());
            }
        }

        classes = Collections.unmodifiableList(cls);
        hprofHeap.getLoadClassSegment().setLoadClassOffsets();
        arrayMap = new HashMap(classes.size() / 15);
        extractSpecialClasses();

        return classes;
    }

    void extractSpecialClasses() {
        ClassDump java_lang_Object = null;
        primitiveArrayMap = new HashMap();

        Iterator classIt = classes.iterator();

        while (classIt.hasNext()) {
            ClassDump jcls = (ClassDump) classIt.next();
            String vmName = jcls.getLoadClass().getVMName();
            Integer typeObj = null;

            if (vmName.equals("[Z")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BOOLEAN);
            } else if (vmName.equals("[C")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.CHAR);
            } else if (vmName.equals("[F")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.FLOAT);
            } else if (vmName.equals("[D")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.DOUBLE);
            } else if (vmName.equals("[B")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BYTE);
            } else if (vmName.equals("[S")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.SHORT);
            } else if (vmName.equals("[I")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.INT);
            } else if (vmName.equals("[J")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.LONG);
            } else if (vmName.equals("java/lang/Class")) { // NOI18N
                java_lang_Class = jcls;
            } else if (vmName.equals("java/lang/Object")) { // NOI18N
                java_lang_Object = jcls;
            } else if (vmName.equals("boolean[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BOOLEAN);
            } else if (vmName.equals("char[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.CHAR);
            } else if (vmName.equals("float[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.FLOAT);
            } else if (vmName.equals("double[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.DOUBLE);
            } else if (vmName.equals("byte[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BYTE);
            } else if (vmName.equals("short[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.SHORT);
            } else if (vmName.equals("int[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.INT);
            } else if (vmName.equals("long[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.LONG);
            } else if (vmName.equals("java.lang.Class")) { // NOI18N
                java_lang_Class = jcls;
            } else if (vmName.equals("java.lang.Object")) { // NOI18N
                java_lang_Object = jcls;
            }

            if (typeObj != null) {
                primitiveArrayMap.put(typeObj, jcls);
            }
        }
        if (java_lang_Object != null) {
            newSize = java_lang_Object.getRawInstanceSize() > 0;
        }
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);
        if (classes == null) {
            out.writeInt(0);
        } else {
            out.writeInt(classes.size());
            for (int i=0; i<classes.size(); i++) {
                ClassDump classDump = (ClassDump) classes.get(i);

                classDump.writeToStream(out);
                Long size = arrayMap.get(classDump);
                out.writeBoolean(size != null);
                if (size != null) {
                    out.writeLong(size.longValue());
                }
            }
        }
    }

    ClassDumpSegment(HprofHeap heap, long start, long end, DataInputStream dis) throws IOException {
        this(heap, start, end);
        int classesSize = dis.readInt();
        if (classesSize != 0) {
            List<JavaClass> cls = new ArrayList /*<JavaClass>*/(classesSize);
            arrayMap = new HashMap(classesSize / 15);
            
            for (int i=0; i<classesSize; i++) {
                ClassDump c = new ClassDump(this, dis.readLong(), dis);
                cls.add(c);
                if (dis.readBoolean()) {
                    Long size = Long.valueOf(dis.readLong());
                    arrayMap.put(c, size);
                }
            }
            classes = Collections.unmodifiableList(cls);
        }
    }
    
    private static class FieldsCache extends LinkedHashMap {
        private static final int SIZE = 500;
        
        FieldsCache() {
            super(SIZE,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size() > SIZE) {
                return true;
            }
            return false;
        }
    }
}

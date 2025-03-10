/*
 * Copyright (c) 1997, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 *
 * @author Tomas Hurka
 */
class HprofHeap implements Heap {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // dump tags
    static final int STRING = 1;
    static final int LOAD_CLASS = 2;
    private static final int UNLOAD_CLASS = 3;
    static final int STACK_FRAME = 4;
    static final int STACK_TRACE = 5;
    private static final int ALLOC_SITES = 6;
    static final int HEAP_SUMMARY = 7;
    private static final int START_THREAD = 0xa;
    private static final int END_THREAD = 0xb;
    private static final int HEAP_DUMP = 0xc;
    private static final int HEAP_DUMP_SEGMENT = 0x1c;
    private static final int HEAP_DUMP_END = 0x2c;
    private static final int CPU_SAMPLES = 0xd;
    private static final int CONTROL_SETTINGS = 0xe;

    // heap dump tags
    static final int ROOT_UNKNOWN = 0xff;
    static final int ROOT_JNI_GLOBAL = 1;
    static final int ROOT_JNI_LOCAL = 2;
    static final int ROOT_JAVA_FRAME = 3;
    static final int ROOT_NATIVE_STACK = 4;
    static final int ROOT_STICKY_CLASS = 5;
    static final int ROOT_THREAD_BLOCK = 6;
    static final int ROOT_MONITOR_USED = 7;
    static final int ROOT_THREAD_OBJECT = 8;
    static final int CLASS_DUMP = 0x20;
    static final int INSTANCE_DUMP = 0x21;
    static final int OBJECT_ARRAY_DUMP = 0x22;
    static final int PRIMITIVE_ARRAY_DUMP = 0x23;

    //  HPROF HEAP 1.0.3 tags
    static final int HEAP_DUMP_INFO                = 0xfe;
    static final int ROOT_INTERNED_STRING          = 0x89;
    static final int ROOT_FINALIZING               = 0x8a;
    static final int ROOT_DEBUGGER                 = 0x8b;
    static final int ROOT_REFERENCE_CLEANUP        = 0x8c;
    static final int ROOT_VM_INTERNAL              = 0x8d;
    static final int ROOT_JNI_MONITOR              = 0x8e;
    static final int UNREACHABLE                   = 0x90; /* deprecated */
    static final int PRIMITIVE_ARRAY_NODATA_DUMP   = 0xc3;

    // basic type
    static final int OBJECT = 2;
    static final int BOOLEAN = 4;
    static final int CHAR = 5;
    static final int FLOAT = 6;
    static final int DOUBLE = 7;
    static final int BYTE = 8;
    static final int SHORT = 9;
    static final int INT = 10;
    static final int LONG = 11;
    private static final boolean DEBUG = false;

    private static final String SNAPSHOT_ID = "NBPHD";
    private static final int SNAPSHOT_VERSION  = 5;
    private static final String OS_PROP = "os.name";
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofByteBuffer dumpBuffer;
    LongMap idToOffsetMap;
    private NearestGCRoot nearestGCRoot;
    final HprofGCRoots gcRoots;
    private ComputedSummary computedSummary;
    private final Object computedSummaryLock = new Object();
    private DominatorTree domTree;
    private TagBounds allInstanceDumpBounds;
    private TagBounds heapDumpSegment;
    private TagBounds[] heapTagBounds;
    private TagBounds[] tagBounds = new TagBounds[0xff];
    private boolean instancesCountComputed;
    private final Object instancesCountLock = new Object();
    private boolean referencesComputed;
    private final Object referencesLock = new Object();
    private boolean retainedSizeComputed;
    private final Object retainedSizeLock = new Object();
    private boolean retainedSizeByClassComputed;
    private final Object retainedSizeByClassLock = new Object();
    private int idMapSize;
    private int segment;

    // for serialization
    File heapDumpFile;
    CacheDirectory cacheDirectory;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofHeap(File dumpFile, int seg, CacheDirectory cacheDir) throws FileNotFoundException, IOException {
        cacheDirectory = cacheDir;
        dumpBuffer = cacheDir.createHprofByteBuffer(dumpFile);
        segment = seg;
        fillTagBounds(dumpBuffer.getHeaderSize());
        heapDumpSegment = computeHeapDumpStart();

        if (heapDumpSegment != null) {
            fillHeapTagBounds();
        }

        idToOffsetMap = new LongMap(idMapSize,dumpBuffer.getIDSize(),dumpBuffer.getFoffsetSize(), cacheDirectory);
        nearestGCRoot = new NearestGCRoot(this);
        gcRoots = new HprofGCRoots(this);
        heapDumpFile = dumpFile;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public List<JavaClass> getAllClasses() {
        ClassDumpSegment classDumpBounds;

        if (heapDumpSegment == null) {
            return Collections.emptyList();
        }

        classDumpBounds = getClassDumpSegment();

        if (classDumpBounds == null) {
            return Collections.emptyList();
        }

        return classDumpBounds.createClassCollection();
    }

    public List<Instance> getBiggestObjectsByRetainedSize(int number) {
        long[] ids;
        List<Instance> bigObjects = new ArrayList<>(number);
        
        computeRetainedSize();
        ids = idToOffsetMap.getBiggestObjectsByRetainedSize(number);
        for (int i=0;i<ids.length;i++) {
            bigObjects.add(getInstanceByID(ids[i]));
        }
        return bigObjects;
    }
    
    public Collection<GCRoot> getGCRoots(Instance instance) {
       Long instanceId = Long.valueOf(instance.getInstanceId());
       Object gcroot = gcRoots.getGCRoots(instanceId);
       if (gcroot == null) {
           return Collections.emptyList();
       }
       if (gcroot instanceof GCRoot) {
           return Collections.singletonList((GCRoot)gcroot);
       }
       return Collections.unmodifiableCollection((Collection<GCRoot>)gcroot);
    }

    public Collection<GCRoot> getGCRoots() {
        if (heapDumpSegment == null) {
            return Collections.emptyList();
        }
        return gcRoots.getGCRoots();
    }

    public Instance getInstanceByID(long instanceID) {
        if (instanceID == 0L) {
            return null;
        }

        computeInstances();
        LongMap.Entry entry = idToOffsetMap.get(instanceID);

        if (entry == null) {
            return null;
        }
        return getInstanceByOffset(new long[] {entry.getOffset()});
    }

    public JavaClass getJavaClassByID(long javaclassId) {
        return getClassDumpSegment().getClassDumpByID(javaclassId);
    }

    public JavaClass getJavaClassByName(String fqn) {
        if (heapDumpSegment == null) {
            return null;
        }
        return getClassDumpSegment().getJavaClassByName(fqn);
    }

    public Collection<JavaClass> getJavaClassesByRegExp(String regexp) {
        if (heapDumpSegment == null) {
            return Collections.emptyList();
        }
        return getClassDumpSegment().getJavaClassesByRegExp(regexp);
    }
    
    
    private class InstancesIterator implements Iterator<Instance> {
        private long[] offset;
        private Instance nextInstance;
        
        private InstancesIterator() {
            offset = new long[] { allInstanceDumpBounds.startOffset };
        }

        public boolean hasNext() {
            while (offset[0] < allInstanceDumpBounds.endOffset && nextInstance == null) {
                nextInstance = getInstanceByOffset(offset);
            }
            return nextInstance != null;
        }

        public Instance next() {
            if (hasNext()) {
                Instance ni = nextInstance;

                nextInstance = null;
                return ni;
            }
            throw new NoSuchElementException();
        }
    }
        
    public Iterator<Instance> getAllInstancesIterator() {
        // make sure java classes are initialized
        List<JavaClass> classes = getAllClasses();
        if (classes.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new InstancesIterator();
    }
    
    public HeapSummary getSummary() {
        TagBounds summaryBound = tagBounds[HEAP_SUMMARY];

        if (summaryBound != null) {
            return new Summary(dumpBuffer, summaryBound.startOffset);
        }

        synchronized (computedSummaryLock) {
            if (computedSummary == null) {
                computedSummary = new ComputedSummary(this);
            }
        }

        return computedSummary;
    }

    public Properties getSystemProperties() {
        JavaClass systemClass = getJavaClassByName("java.lang.System"); // NOI18N
        if (systemClass != null) {
            Instance props = (Instance) systemClass.getValueOfStaticField("props"); //NOI18N

            if (props == null) {
                props = (Instance) systemClass.getValueOfStaticField("systemProperties"); //NOI18N
            }
            if (props != null) {
                return HprofProxy.getProperties(props);
            }
        }
        // Substrate VM
        systemClass = getJavaClassByName("com.oracle.svm.core.jdk.SystemPropertiesSupport"); // NOI18N
        if (systemClass != null) {
            for (JavaClass propSupportSubClass : systemClass.getSubClasses()) {
                List<Instance> propSupportInstances = propSupportSubClass.getInstances();

                if (!propSupportInstances.isEmpty()) {
                    Instance propSupportInstance = propSupportInstances.get(0);
                    Object props = propSupportInstance.getValueOfField("properties");   // NOI18N

                    if (props instanceof Instance) {
                        return HprofProxy.getProperties((Instance) props);
                    }
                }
            }
        }
        return null;
    }

    public boolean isRetainedSizeComputed() {
        return retainedSizeComputed;
    }

    public boolean isRetainedSizeByClassComputed() {
        return retainedSizeByClassComputed;
    }

    //---- Serialization support
    void writeToFile() {
        if (!cacheDirectory.isTemporary()) {
            try {
                DataOutputStream out;
                File outFile = cacheDirectory.getHeapDumpAuxFile();
                out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile), 32768));
                writeToStream(out);
                out.close();
                cacheDirectory.setDirty(false);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }
    
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeUTF(SNAPSHOT_ID);
        out.writeInt(SNAPSHOT_VERSION);
        out.writeUTF(heapDumpFile.getAbsolutePath());
        out.writeLong(dumpBuffer.getTime());
        out.writeUTF(System.getProperty(OS_PROP));
        nearestGCRoot.writeToStream(out);
        allInstanceDumpBounds.writeToStream(out);
        heapDumpSegment.writeToStream(out);
        TagBounds.writeToStream(heapTagBounds, out);
        TagBounds.writeToStream(tagBounds, out);
        out.writeBoolean(instancesCountComputed);
        out.writeBoolean(referencesComputed);
        out.writeBoolean(retainedSizeComputed);
        out.writeBoolean(retainedSizeByClassComputed);
        out.writeInt(idMapSize);
        out.writeInt(segment);        
        idToOffsetMap.writeToStream(out);
        out.writeBoolean(domTree != null);
        if (domTree != null) {
            domTree.writeToStream(out);
        }
    }

    HprofHeap(DataInputStream dis, CacheDirectory cacheDir) throws IOException {
        if (cacheDir.isDirty()) {
            throw new IOException("Dirty cache "+cacheDir);
        }
        String id = dis.readUTF();
        if (!SNAPSHOT_ID.equals(id)) {
            throw new IOException("Invalid HPROF dump id "+id);
        }
        int version = dis.readInt();
        if (version != SNAPSHOT_VERSION) {
            throw new IOException("Invalid HPROF version "+SNAPSHOT_VERSION+" loaded "+version);            
        }
        heapDumpFile = cacheDir.getHeapFile(dis.readUTF());
        cacheDirectory = cacheDir;
        dumpBuffer = HprofByteBuffer.createHprofByteBuffer(heapDumpFile);
        long time = dis.readLong();
        if (time != dumpBuffer.getTime()) {
            throw new IOException("HPROF time mismatch. Cached "+time+" from heap dump "+dumpBuffer.getTime());
        }
        String os = dis.readUTF();
        if (!os.equals(System.getProperty(OS_PROP))) {
            System.err.println("Warning: HPROF OS mismatch. Cached "+os+" current OS "+System.getProperty(OS_PROP));
        }
        nearestGCRoot = new NearestGCRoot(this, dis);
        allInstanceDumpBounds = new TagBounds(dis);
        heapDumpSegment = new TagBounds(dis);
        heapTagBounds = new TagBounds[0x100];
        TagBounds.readFromStream(dis, this, heapTagBounds);
        TagBounds.readFromStream(dis, this, tagBounds);        
        instancesCountComputed = dis.readBoolean();
        referencesComputed = dis.readBoolean();
        retainedSizeComputed = dis.readBoolean();
        retainedSizeByClassComputed = dis.readBoolean();
        idMapSize = dis.readInt();
        segment = dis.readInt();
        idToOffsetMap = new LongMap(dis, cacheDirectory);
        if (dis.readBoolean()) {
            domTree = new DominatorTree(this, dis);
        }
        gcRoots = new HprofGCRoots(this);
        getClassDumpSegment().extractSpecialClasses();            
    }
    
    ClassDumpSegment getClassDumpSegment() {
        return (ClassDumpSegment) heapTagBounds[CLASS_DUMP];
    }

    LoadClassSegment getLoadClassSegment() {
        return (LoadClassSegment) tagBounds[LOAD_CLASS];
    }

    StringSegment getStringSegment() {
        return (StringSegment) tagBounds[STRING];
    }
    
    StackTraceSegment getStackTraceSegment() {
        return (StackTraceSegment) tagBounds[STACK_TRACE];
    }
    
    StackFrameSegment getStackFrameSegment() {
        return (StackFrameSegment) tagBounds[STACK_FRAME];
    }
    
    TagBounds getAllInstanceDumpBounds() {
        return allInstanceDumpBounds;
    }
    
    long getRetainedSize(Instance instance) {
        computeRetainedSize();
        return idToOffsetMap.get(instance.getInstanceId()).getRetainedSize();
    }

    int getValueSize(final byte type) {
        switch (type) {
            case HprofHeap.OBJECT:
                return dumpBuffer.getIDSize();
            case HprofHeap.BOOLEAN:
                return 1;
            case HprofHeap.CHAR:
                return 2;
            case HprofHeap.FLOAT:
                return 4;
            case HprofHeap.DOUBLE:
                return 8;
            case HprofHeap.BYTE:
                return 1;
            case HprofHeap.SHORT:
                return 2;
            case HprofHeap.INT:
                return 4;
            case HprofHeap.LONG:
                return 8;
            default:
                throw new IllegalArgumentException("Invalid type " + type); // NOI18N
        }
    }

    Instance getInstanceByOffset(long[] offset) {
        return getInstanceByOffset(offset, null, -1);
    }

    Instance getInstanceByOffset(long[] offset, ClassDump instanceClassDump, long instanceClassId) {
        long start = offset[0];
        assert start != 0L;
        ClassDump classDump;
        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        int idSize = dumpBuffer.getIDSize();
        int classIdOffset = 0;

        int tag = readDumpTag(offset);

        if (tag == INSTANCE_DUMP) {
            classIdOffset = idSize + 4;
        } else if (tag == OBJECT_ARRAY_DUMP) {
            classIdOffset = idSize + 4 + 4;
        } else if (tag == PRIMITIVE_ARRAY_DUMP) {
            classIdOffset = idSize + 4 + 4;
        }

        if (tag == PRIMITIVE_ARRAY_DUMP) {
            classDump = classDumpBounds.getPrimitiveArrayClass(dumpBuffer.get(start + 1 + classIdOffset));
            if (instanceClassId != -1 && classDump.getJavaClassId() != instanceClassId) {
                return null;
            }

            return new PrimitiveArrayDump(classDump, start);
        } else {
            long classId = dumpBuffer.getID(start + 1 + classIdOffset);
            if (instanceClassId != -1 && classId != instanceClassId) {
                return null;
            }
            if (instanceClassDump == null) {
                classDump = classDumpBounds.getClassDumpByID(classId);
            } else {
                classDump = instanceClassDump;        
            }
        }

        if (classDump == null) {
            return null;
        }
        if (tag == INSTANCE_DUMP) {
            return new InstanceDump(classDump, start);
        } else if (tag == OBJECT_ARRAY_DUMP) {
            return new ObjectArrayDump(classDump, start);
        } else if (tag == CLASS_DUMP) {
            return new ClassDumpInstance(classDump);
        }
        // other heap dump tags, ROOT_ etc.
        return null;
    }

    void computeInstances() {
        synchronized (instancesCountLock) {
        if (instancesCountComputed) {
            return;
        }

        HeapProgress.progressStart();
        cacheDirectory.setDirty(true);
        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        int idSize = dumpBuffer.getIDSize();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };
        Map<Long,JavaClass> classIdToClassMap = classDumpBounds.getClassIdToClassMap();

        for (long counter = 0; offset[0] < allInstanceDumpBounds.endOffset; counter++) {
            int classIdOffset = 0;
            int instanceIdOffset = 0;
            ClassDump classDump = null;
            long start = offset[0];
            int tag = readDumpTag(offset);
            LongMap.Entry instanceEntry = null;

            if (tag == INSTANCE_DUMP) {
                instanceIdOffset = 1;
                classIdOffset = idSize + 4;
            } else if (tag == OBJECT_ARRAY_DUMP) {
                instanceIdOffset = 1;
                classIdOffset = idSize + 4 + 4;
            } else if (tag == PRIMITIVE_ARRAY_DUMP) {
                byte type = dumpBuffer.get(start + 1 + idSize + 4 + 4);
                instanceIdOffset = 1;
                classDump = classDumpBounds.getPrimitiveArrayClass(type);
            }

            if (instanceIdOffset != 0) {
                long instanceId = dumpBuffer.getID(start + instanceIdOffset);
                instanceEntry = idToOffsetMap.put(instanceId, start);
            }

            if (classIdOffset != 0) {
                long classId = dumpBuffer.getID(start + 1 + classIdOffset);
                classDump = (ClassDump) classIdToClassMap.get(new Long(classId));
            }

            if (classDump != null) {
                classDump.registerInstance(start);
                instanceEntry.setIndex(classDump.getInstancesCount());
                classDumpBounds.addInstanceSize(classDump, tag, start);
            }
            HeapProgress.progress(counter,allInstanceDumpBounds.startOffset,start,allInstanceDumpBounds.endOffset);
        }
        instancesCountComputed = true;
        writeToFile();
        }
        HeapProgress.progressFinish();
    }

    List<Value> findReferencesFor(long instanceId) {
        assert instanceId != 0L : "InstanceID is null";
        computeReferences();
        
        List<Value> refs = new ArrayList<>();
        LongIterator refIdsIt = idToOffsetMap.get(instanceId).getReferences();
        int idSize = dumpBuffer.getIDSize();
        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        long[] offset = new long[1];
        
        while (refIdsIt.hasNext()) {
            long foundInstanceId = refIdsIt.next();
            offset[0] = idToOffsetMap.get(foundInstanceId).getOffset();
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                int size = dumpBuffer.getInt(start + 1 + idSize + 4 + idSize);
                byte[] fields = new byte[size];
                dumpBuffer.get(start + 1 + idSize + 4 + idSize + 4, fields);
                long classId = dumpBuffer.getID(start + 1 + idSize + 4);
                ClassDump classDump = classDumpBounds.getClassDumpByID(classId);
                InstanceDump instance = new InstanceDump(classDump, start);

                for (Object field : instance.getFieldValues()) {
                    if (field instanceof HprofInstanceObjectValue) {
                        HprofInstanceObjectValue objectValue = (HprofInstanceObjectValue) field;

                        if (objectValue.getInstanceId() == instanceId) {
                            refs.add(objectValue);
                        }
                    }
                }
                if (refs.isEmpty() && classId == instanceId) {
                    SyntheticClassField syntheticClassField = new SyntheticClassField(classDump);
                    long fieldOffset = start + 1 + dumpBuffer.getIDSize() + 4;
                    
                    refs.add(new SyntheticClassObjectValue(instance,syntheticClassField,fieldOffset));
                }
            } else if (tag == OBJECT_ARRAY_DUMP) {
                int elements = dumpBuffer.getInt(start + 1 + idSize + 4);
                long classId = dumpBuffer.getID(start + 1 + idSize + 4 + 4);
                ClassDump classDump = classDumpBounds.getClassDumpByID(classId);
                long position = start + 1 + idSize + 4 + 4 + idSize;

                for (int i = 0; i < elements; i++, position += idSize) {
                    if (dumpBuffer.getID(position) == instanceId) {
                        refs.add(new HprofArrayValue(classDump, start, i));
                    }
                }
            } else if (tag == CLASS_DUMP) {
                ClassDump cls = classDumpBounds.getClassDumpByID(foundInstanceId);
                cls.findStaticReferencesFor(instanceId, refs);
            }
        }

        return refs;
    }

    void computeReferences() {
        synchronized (referencesLock) {
        if (referencesComputed) {
            return;
        }

        HeapProgress.progressStart();
        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        int idSize = dumpBuffer.getIDSize();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };
        Map<Long,JavaClass> classIdToClassMap = classDumpBounds.getClassIdToClassMap();

        computeInstances();
        cacheDirectory.setDirty(true);
        for (long counter=0; offset[0] < allInstanceDumpBounds.endOffset; counter++) {
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                long classId = dumpBuffer.getID(start+1+idSize+4);
                ClassDump classDump = (ClassDump) classIdToClassMap.get(new Long(classId));
                if (classDump != null) {
                    long instanceId = dumpBuffer.getID(start+1);
                    long inOff = start+1+idSize+4+idSize+4;

                    for (Field f : classDump.getAllInstanceFields()) {
                        HprofField field = (HprofField)f;
                        if (field.getValueType() == HprofHeap.OBJECT) {
                            long outId = dumpBuffer.getID(inOff);

                            if (outId != 0) {
                                LongMap.Entry entry = idToOffsetMap.get(outId);
                                if (entry != null) {
                                    entry.addReference(instanceId);
                                } else {
                                    //    System.err.println("instance entry:" + Long.toHexString(outId));
                                }
                            }
                        }
                        inOff += field.getValueSize();
                    }
                }
            } else if (tag == OBJECT_ARRAY_DUMP) {
                long instanceId = dumpBuffer.getID(start+1);
                int elements = dumpBuffer.getInt(start+1+idSize+4);
                long position = start+1+idSize+4+4+idSize;
                
                for(int i=0;i<elements;i++,position+=idSize) {
                    long outId = dumpBuffer.getID(position);
                    
                    if (outId == 0) continue;
                    LongMap.Entry entry = idToOffsetMap.get(outId);
                    if (entry != null) {
                        entry.addReference(instanceId);
                    } else {
                        //    System.err.println("bad array entry:" + Long.toHexString(outId));
                    }
                }
            }
            HeapProgress.progress(counter,allInstanceDumpBounds.startOffset,start,allInstanceDumpBounds.endOffset);
        }
        
        for (JavaClass cls : getClassDumpSegment().createClassCollection()) {
            for (FieldValue field : cls.getStaticFieldValues()) {
                if (field instanceof HprofFieldObjectValue) {
                    long outId = ((HprofFieldObjectValue)field).getInstanceID();

                    if (outId != 0) {
                        LongMap.Entry entry = idToOffsetMap.get(outId);
                        if (entry == null) {
                            //    System.err.println("instance entry:" + Long.toHexString(outId));
                            continue;
                        }
                        entry.addReference(cls.getJavaClassId());
                    }
                }
            }
        }
        idToOffsetMap.flush();
        referencesComputed = true;
        writeToFile();
        }
        HeapProgress.progressFinish();        
    }
    
    void computeRetainedSize() {
        synchronized (retainedSizeLock) {
        if (retainedSizeComputed) {
            return;
        }
        HeapProgress.progressStart();
        LongBuffer leaves = nearestGCRoot.getLeaves();
        cacheDirectory.setDirty(true);
        new TreeObject(this,leaves).computeTrees();
        domTree = new DominatorTree(this,nearestGCRoot.getMultipleParents());
        domTree.computeDominators();

        // deep path first
        try {
            LongBuffer deepPathBuffer = nearestGCRoot.getDeepPathBuffer();
            LongBuffer deepPath = deepPathBuffer.revertBuffer();

            deepPathBuffer.reset();
            deepPathBuffer.delete();
            if (deepPath.hasData()) {
                for (long deepObjId = deepPath.readLong(); deepObjId != 0; deepObjId = deepPath.readLong()) {
                    LongMap.Entry deepObjEntry = idToOffsetMap.get(deepObjId);
                    assert deepObjEntry.isDeepObj();
                    long idomId = domTree.getIdomId(deepObjId, deepObjEntry);
                    LongMap.Entry idomEntry = idToOffsetMap.get(idomId);

                    if (!deepObjEntry.isTreeObj()) {
                        Instance deepInstance = getInstanceByID(deepObjId);
                        long size = deepInstance.getSize();
                        long origSize = deepObjEntry.getRetainedSize();

                        if (origSize < 0) origSize = 0;
                        deepObjEntry.setRetainedSize(origSize + size);
                    }
                    if (idomEntry.isDeepObj() && !idomEntry.isTreeObj()) {
                        long origSize = idomEntry.getRetainedSize();
                        if (origSize < 0) origSize = 0;
                        idomEntry.setRetainedSize(origSize + deepObjEntry.getRetainedSize());
                    }
                }
            }
            deepPath.delete();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage(), ex);
        }

        long[] offset = new long[] { allInstanceDumpBounds.startOffset };

        for (long counter=0; offset[0] < allInstanceDumpBounds.endOffset; counter++) {
            int instanceIdOffset = 0;
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                instanceIdOffset = 1;
            } else if (tag == OBJECT_ARRAY_DUMP) {
                instanceIdOffset = 1;
            } else if (tag == PRIMITIVE_ARRAY_DUMP) {
                instanceIdOffset = 1;
            } else {
                continue;
            }
            long instanceId = dumpBuffer.getID(start + instanceIdOffset);
            LongMap.Entry instanceEntry = idToOffsetMap.get(instanceId);
            long idom = domTree.getIdomId(instanceId,instanceEntry);
            boolean isTreeObj = instanceEntry.isTreeObj();
            boolean deepObj = instanceEntry.isDeepObj();
            long instSize = 0;
            
            if (!deepObj && !isTreeObj && (instanceEntry.getNearestGCRootPointer() != 0 || gcRoots.getGCRoots(new Long(instanceId)) != null)) {
                long origSize = instanceEntry.getRetainedSize();
                if (origSize < 0) origSize = 0;
                Instance instance = getInstanceByOffset(new long[] {start});
                instSize = instance != null ? instance.getSize() : getClassDumpSegment().sizeSettings.getMinimumInstanceSize();
                instanceEntry.setRetainedSize(origSize + instSize);
            }
            if (idom != 0) {
                long size;
                LongMap.Entry entry = idToOffsetMap.get(idom);
                
                if (entry.isDeepObj()) {
                    continue;
                } else if (isTreeObj) {
                    size = instanceEntry.getRetainedSize();
                } else if (deepObj) {
                    size = instanceEntry.getRetainedSize();
                } else {
                    assert instSize != 0;
                    size = instSize;
                }
                for (;idom!=0;idom=domTree.getIdomId(idom,entry)) {
                    entry = idToOffsetMap.get(idom);
                    if (entry.isTreeObj()) {
                        break;
                    }
                    long retainedSize = entry.getRetainedSize();
                    if (retainedSize < 0) retainedSize = 0;
                    entry.setRetainedSize(retainedSize+size);
                }
            }
            HeapProgress.progress(counter,allInstanceDumpBounds.startOffset,start,allInstanceDumpBounds.endOffset);
        }
        retainedSizeComputed = true;
        writeToFile();
        }
        HeapProgress.progressFinish();
    }

    void computeRetainedSizeByClass() {
        synchronized (retainedSizeByClassLock) {
        if (retainedSizeByClassComputed) {
            return;
        }
        computeRetainedSize();
        cacheDirectory.setDirty(true);
        HeapProgress.progressStart();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };

        for (long counter=0; offset[0] < allInstanceDumpBounds.endOffset; counter++) {
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP || tag == OBJECT_ARRAY_DUMP || tag == PRIMITIVE_ARRAY_DUMP) {
                Instance i = getInstanceByOffset(new long[] {start});
                if (i != null) {
                    ClassDump javaClass = (ClassDump) i.getJavaClass();
                    if (javaClass != null && !domTree.hasInstanceInChain(tag, i)) {
                        javaClass.addSizeForInstance(i);
                    }
                }
            }
            HeapProgress.progress(counter,allInstanceDumpBounds.startOffset,start,allInstanceDumpBounds.endOffset);
        }
        // all done, release domTree
        domTree = null;
        retainedSizeByClassComputed = true;
        writeToFile();
        }
        HeapProgress.progressFinish();
    }

    Instance getNearestGCRootPointer(Instance instance) {
        return nearestGCRoot.getNearestGCRootPointer(instance);
    }
    
    boolean isGCRoot(Instance instance) {
       Long instanceId = Long.valueOf(instance.getInstanceId());
       return gcRoots.getGCRoots(instanceId) != null;
    }

    int readDumpTag(long[] offset) {
        long position = offset[0];
        int dumpTag = dumpBuffer.get(position++) & 0xFF;
        long size = 0;
        long tagOffset = position;
        int idSize = dumpBuffer.getIDSize();

        switch (dumpTag) {
            case -1:
            case ROOT_UNKNOWN:

                if (DEBUG) {
                    System.out.println("Tag ROOT_UNKNOWN"); // NOI18N
                }

                size = idSize;
                dumpTag = ROOT_UNKNOWN;

                break;
            case ROOT_JNI_GLOBAL:

                if (DEBUG) {
                    System.out.println("Tag ROOT_JNI_GLOBAL"); // NOI18N
                }

                size = 2 * idSize;

                break;
            case ROOT_JNI_LOCAL: {
                if (DEBUG) {
                    System.out.print("Tag ROOT_JNI_LOCAL"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int threadSerial = dumpBuffer.getInt(position);
                    position += 4;

                    int frameNum = dumpBuffer.getInt(position);
                    position += 4;
                    System.out.println(" Object ID " + objId + " Thread serial " + threadSerial + " Frame num " + frameNum); // NOI18N
                }

                size = idSize + (2 * 4);

                break;
            }
            case ROOT_JAVA_FRAME:

                if (DEBUG) {
                    System.out.println("Tag ROOT_JAVA_FRAME"); // NOI18N
                    int threadSerial = dumpBuffer.getInt(position);
                    position += 4;

                    int frameNum = dumpBuffer.getInt(position);
                    position += 4;
                    System.out.println(" Thread serial " + threadSerial + " Frame num " + frameNum); // NOI18N
                }

                size = idSize + (2 * 4);

                break;
            case ROOT_NATIVE_STACK:

                if (DEBUG) {
                    System.out.println("Tag ROOT_NATIVE_STACK"); // NOI18N
                }

                size = idSize + 4;

                break;
            case ROOT_STICKY_CLASS:

                if (DEBUG) {
                    System.out.println("Tag ROOT_STICKY_CLASS"); // NOI18N
                }

                size = idSize;

                break;
            case ROOT_THREAD_BLOCK:

                if (DEBUG) {
                    System.out.println("Tag ROOT_THREAD_BLOCK"); // NOI18N
                }

                size = idSize + 4;

                break;
            case ROOT_MONITOR_USED:

                if (DEBUG) {
                    System.out.println("Tag ROOT_MONITOR_USED"); // NOI18N
                }

                size = idSize;

                break;
            case ROOT_THREAD_OBJECT:

                if (DEBUG) {
                    System.out.println("Tag ROOT_THREAD_OBJECT"); // NOI18N
                }

                size = idSize + (2 * 4);

                break;
            case CLASS_DUMP: {
                int constantSize = idSize + 4 + (6 * idSize) + 4;
                int cpoolSize;
                int sfSize;
                int ifSize;

                if (DEBUG) {
                    System.out.println("Tag CLASS_DUMP, start offset " + tagOffset); // NOI18N

                    long classId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;

                    long superId = dumpBuffer.getID(position);
                    position += idSize;

                    long classLoaderId = dumpBuffer.getID(position);
                    position += idSize;

                    long signersId = dumpBuffer.getID(position);
                    position += idSize;

                    long protDomainId = dumpBuffer.getID(position);
                    position += idSize;
                    dumpBuffer.getID(position);
                    position += idSize;
                    dumpBuffer.getID(position);
                    position += idSize;

                    int instSize = dumpBuffer.getInt(position);
                    position += 4;
                    offset[0] = position;
                    cpoolSize = readConstantPool(offset);
                    sfSize = readStaticFields(offset);
                    ifSize = readInstanceFields(offset);
                    System.out.println("ClassId " + classId + " stack Serial " + stackSerial + " Super ID " + superId       // NOI18N
                                       + " ClassLoader ID " + classLoaderId + " signers " + signersId + " Protect Dom Id "  // NOI18N
                                       + protDomainId + " Size " + instSize);                                               // NOI18N
                    System.out.println(" Cpool " + cpoolSize + " Static fields " + sfSize + " Instance fileds " + ifSize);  // NOI18N
                } else {
                    offset[0] = position + constantSize;
                    cpoolSize = readConstantPool(offset);
                    sfSize = readStaticFields(offset);
                    ifSize = readInstanceFields(offset);
                }
                size = constantSize + cpoolSize + sfSize + ifSize;

                break;
            }
            case INSTANCE_DUMP: {
                int fieldSize;

                if (DEBUG) {
                    System.out.println("Tag INSTANCE_DUMP"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;

                    long classId = dumpBuffer.getID(position);
                    position += idSize;
                    fieldSize = dumpBuffer.getInt(position);
                    position += 4;
                    System.out.println("Obj ID " + objId + " Stack serial " + stackSerial + " Class ID " + classId
                                       + " Field size " + fieldSize); // NOI18N
                } else {
                    fieldSize = dumpBuffer.getInt(position + idSize + 4 + idSize);
                }

                size = idSize + 4 + idSize + 4 + fieldSize;

                break;
            }
            case OBJECT_ARRAY_DUMP: {
                long elements;

                if (DEBUG) {
                    System.out.println("Tag OBJECT_ARRAY_DUMP"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;
                    elements = dumpBuffer.getInt(position);
                    position += 4;

                    long classId = dumpBuffer.getID(position);
                    position += idSize;

                    int dataSize = 0;

                    System.out.println("Obj ID " + objId + " Stack serial " + stackSerial + " Elements " + elements // NOI18N
                                           + " Type " + classId); // NOI18N

                    for (int i = 0; i < elements; i++) {
                        dataSize += dumpBuffer.getIDSize();
                        System.out.println("Instance ID " + dumpBuffer.getID(position)); // NOI18N
                        position += idSize;
                    }
                } else {
                    elements = dumpBuffer.getInt(position + idSize + 4);
                }

                size = idSize + 4 + 4 + idSize + (elements * idSize);

                break;
            }
            case PRIMITIVE_ARRAY_DUMP: {
                long elements;
                byte type;

                if (DEBUG) {
                    System.out.println("Tag PRIMITINE_ARRAY_DUMP"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;
                    elements = dumpBuffer.getInt(position);
                    position += 4;
                    type = dumpBuffer.get(position++);

                    int dataSize = 0;
                    System.out.println("Obj ID " + objId + " Stack serial " + stackSerial + " Elements " + elements + " Type " + type); // NOI18N

                    for (int i = 0; i < elements; i++) {
                        dataSize += getValueSize(type);
                    }
                } else {
                    elements = dumpBuffer.getInt(position + idSize + 4);
                    type = dumpBuffer.get(position + idSize + 4 + 4);
                }

                size = idSize + 4 + 4 + 1 + (elements * getValueSize(type));

                break;
            }
            case HEAP_DUMP_SEGMENT: { // to handle big dumps
                size = 4 + 4;

                break;
            }

             /* HPROF HEAP 1.0.3 tags */
            case HEAP_DUMP_INFO: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_HEAP_DUMP_INFO"); // NOI18N
                    int heapId = dumpBuffer.getInt(position);
                    position += 4;

                    long stringID = dumpBuffer.getID(position);
                    position += idSize;
                    System.out.println(" Dump info id " + heapId + " String ID " + stringID); // NOI18N
                }

                size = 4 + idSize;

                break;
            }
            case ROOT_INTERNED_STRING: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_ROOT_INTERNED_STRING"); // NOI18N
                }

                size = idSize;

                break;
          }
            case ROOT_FINALIZING: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_ROOT_FINALIZING"); // NOI18N
                }

                size = idSize;

                break;
            }
            case ROOT_DEBUGGER: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_ROOT_DEBUGGER"); // NOI18N
                }

                size = idSize;

                break;
            }
            case ROOT_REFERENCE_CLEANUP: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_ROOT_REFERENCE_CLEANUP"); // NOI18N
                }

                size = idSize;

                break;
            }
            case ROOT_VM_INTERNAL: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_ROOT_VM_INTERNAL"); // NOI18N
                }

                size = idSize;

                break;
            }
            case ROOT_JNI_MONITOR: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_ROOT_JNI_MONITOR"); // NOI18N
                }

                size = idSize;

                break;
            }
            case UNREACHABLE: {

                if (DEBUG) {
                    System.out.println("Tag HPROF_UNREACHABLE"); // NOI18N
                }

                size = idSize;

                break;
            }
            case PRIMITIVE_ARRAY_NODATA_DUMP: {
                    throw new IllegalArgumentException(
                        "Don't know how to load a nodata array");
                //break;
            }

            default:throw new IllegalArgumentException("Invalid dump tag " + dumpTag + " at position " + (position - 1)); // NOI18N              
        }

        offset[0] = tagOffset + size;

        return dumpTag;
    }

    int readTag(long[] offset) {
        long start = offset[0];
        int tag = dumpBuffer.get(start);

        //int time = dumpBuffer.getInt(start+1);
        long len = dumpBuffer.getInt(start + 1 + 4) & 0xFFFFFFFFL;  // len is unsigned int
         // only HEAP_DUMP_END can have zero length
        if (len == 0 && tag != HEAP_DUMP_END && dumpBuffer.version != HprofByteBuffer.JAVA_PROFILE_1_0_3) {
            // broken tag length
            offset[0] = -1;
        } else {
            offset[0] = start + 1 + 4 + 4 + len;
        }

        return tag;
    }

    TagBounds getHeapTagBound(int heapTag) {
        return heapTagBounds[heapTag];
    }

    /**
     *
     * @return number of microseconds since the time stamp in the header
     */
    long getHeapTime() {
        if (heapDumpSegment == null) return 0;
        return getTagTime(heapDumpSegment.startOffset);
    }

    private long getTagTime(long start) {
        int time = dumpBuffer.getInt(start+1);
        return time & 0xFFFFFFFFL; // time is unsigned int
    }

    private abstract class SegmentConsumer {
        int i;
        TagBounds heapDumpTag;

        abstract boolean accept(long start, long end);
    }

    int computeTotalNumberSegments() throws IOException {
        SegmentConsumer sc = new SegmentConsumer() {
            boolean accept(long start, long end) {
                i++;
                return false;
            }
        };
        heapDumpSegIterator(sc);
        return sc.i;
    }

    private TagBounds computeHeapDumpStart() throws IOException {
        SegmentConsumer sc = new SegmentConsumer() {
            boolean accept(long start, long end) {
                if (i++ == segment) {
                    heapDumpTag = new TagBounds(HEAP_DUMP, start, end);
                    return true;
                }
                return false;
            }
        };
        heapDumpSegIterator(sc);
        if (sc.heapDumpTag == null) {
            throw new IOException("Invalid segment " + segment); // NOI18N
        }
        return sc.heapDumpTag;
    }

    private void heapDumpSegIterator(SegmentConsumer sc) throws IOException {
        TagBounds heapDumpBounds = tagBounds[HEAP_DUMP];

        if (heapDumpBounds != null) {
            long start = heapDumpBounds.startOffset;
            long[] offset = new long[] { start };

            for (;start < heapDumpBounds.endOffset; start = offset[0]) {
                int tag = readTag(offset);

                if (tag == HEAP_DUMP) {
                    if (sc.accept(start,offset[0])) return;
                }
            }
        } else {
            TagBounds heapDumpSegmentBounds = tagBounds[HEAP_DUMP_SEGMENT];

            if (heapDumpSegmentBounds != null) {
                TagBounds heapDumpEndBounds = tagBounds[HEAP_DUMP_END];
                if (heapDumpEndBounds == null) {
                    throw new IOException("Heap dump is broken.\nTag 0x"+Integer.toHexString(HEAP_DUMP_END)+" is missing."); // NOI18N
                }
                if (heapDumpSegmentBounds.endOffset == heapDumpEndBounds.startOffset) {
                    // shortcut - just one segment
                    sc.accept(heapDumpSegmentBounds.startOffset,heapDumpSegmentBounds.endOffset);
                    return;
                }
                heapDumpSegmentBounds = heapDumpSegmentBounds.union(heapDumpEndBounds);
                long start = heapDumpSegmentBounds.startOffset;
                long[] offset = new long[] { start };
                long segmentStart = 0;
                long segmentEnd = 0;

                for (;start < heapDumpSegmentBounds.endOffset; start = offset[0]) {
                    int tag = readTag(offset);

                    if (tag == HEAP_DUMP_SEGMENT) {
                        if (segmentStart == 0) segmentStart = start;
                        segmentEnd = offset[0];
                    }
                    if (tag == HEAP_DUMP_END) {
                        if (sc.accept(segmentStart,segmentEnd)) return;
                        segmentStart = 0;
                    }
                }
            }
        }
    }

    private void fillHeapTagBounds() {
        if (heapTagBounds != null) {
            return;
        }

        HeapProgress.progressStart();
        heapTagBounds = new TagBounds[0x100];

        long[] offset = new long[] { heapDumpSegment.startOffset + 1 + 4 + 4 };
        
        for (long counter=0; offset[0] < heapDumpSegment.endOffset; counter++) {
            long start = offset[0];
            int tag = readDumpTag(offset);
            TagBounds bounds = heapTagBounds[tag];
            long end = offset[0];

            if (bounds == null) {
                TagBounds newBounds;

                if (tag == CLASS_DUMP) {
                    newBounds = new ClassDumpSegment(this, start, end);
                } else {
                    newBounds = new TagBounds(tag, start, end);
                }

                heapTagBounds[tag] = newBounds;
            } else {
                bounds.endOffset = end;
            }

            if ((tag == CLASS_DUMP) || (tag == INSTANCE_DUMP) || (tag == OBJECT_ARRAY_DUMP) || (tag == PRIMITIVE_ARRAY_DUMP)) {
                idMapSize++;
            }
            HeapProgress.progress(counter,heapDumpSegment.startOffset,start,heapDumpSegment.endOffset);
        }

        TagBounds instanceDumpBounds = heapTagBounds[INSTANCE_DUMP];
        TagBounds objArrayDumpBounds = heapTagBounds[OBJECT_ARRAY_DUMP];
        TagBounds primArrayDumpBounds = heapTagBounds[PRIMITIVE_ARRAY_DUMP];
        if (instanceDumpBounds == null) {
            instanceDumpBounds = new TagBounds(-1, heapDumpSegment.endOffset, heapDumpSegment.endOffset);
        }
        allInstanceDumpBounds = instanceDumpBounds.union(objArrayDumpBounds);
        allInstanceDumpBounds = allInstanceDumpBounds.union(primArrayDumpBounds);
        HeapProgress.progressFinish();
    }

    private void fillTagBounds(long tagStart) throws IOException {
        long[] offset = new long[] { tagStart };

        while (offset[0] < dumpBuffer.capacity()) {
            long start = offset[0];
            int tag = readTag(offset);
            TagBounds bounds = tagBounds[tag];
            long end = offset[0];

            if (end == -1) {
                // tag with zero-length -> broken heap dump
                throw new IOException("Heap dump is broken.\nTag 0x"+Integer.toHexString(tag)+" at offset "+start+" has zero length.");
            }
            if (bounds == null) {
                TagBounds newBounds;

                if (tag == LOAD_CLASS) {
                    newBounds = new LoadClassSegment(this, start, end);
                } else if (tag == STRING) {
                    newBounds = new StringSegment(this, start, end);
                } else if (tag == STACK_TRACE) {
                    newBounds = new StackTraceSegment(this, start, end);
                } else if (tag == STACK_FRAME) {
                    newBounds = new StackFrameSegment(this, start, end);
                } else {
                    newBounds = new TagBounds(tag, start, end);
                }

                tagBounds[tag] = newBounds;
            } else {
                bounds.endOffset = end;
            }
        }
    }

    private int readConstantPool(long[] offset) {
        long start = offset[0];
        int size = dumpBuffer.getShort(start);
        offset[0] += 2;

        for (int i = 0; i < size; i++) {
            offset[0] += 2;
            readValue(offset);
        }

        return (int) (offset[0] - start);
    }

    private int readInstanceFields(long[] offset) {
        long position = offset[0];
        int fields = dumpBuffer.getShort(offset[0]);
        offset[0] += 2;

        if (DEBUG) {
            for (int i = 0; i < fields; i++) {
                long nameId = dumpBuffer.getID(offset[0]);
                offset[0] += dumpBuffer.getIDSize();

                byte type = dumpBuffer.get(offset[0]++);
                System.out.println("Instance field name ID " + nameId + " Type " + type); // NOI18N
            }
        } else {
            offset[0] += (fields * (dumpBuffer.getIDSize() + 1));
        }

        return (int) (offset[0] - position);
    }

    private int readStaticFields(long[] offset) {
        long start = offset[0];
        int fields = dumpBuffer.getShort(start);
        offset[0] += 2;

        int idSize = dumpBuffer.getIDSize();

        for (int i = 0; i < fields; i++) {
            if (DEBUG) {
                long nameId = dumpBuffer.getID(offset[0]);
                System.out.print("Static field name ID " + nameId + " "); // NOI18N
            }

            offset[0] += idSize;

            byte type = readValue(offset);
        }

        return (int) (offset[0] - start);
    }

    private byte readValue(long[] offset) {
        byte type = dumpBuffer.get(offset[0]++);
        offset[0] += getValueSize(type);

        return type;
    }
}

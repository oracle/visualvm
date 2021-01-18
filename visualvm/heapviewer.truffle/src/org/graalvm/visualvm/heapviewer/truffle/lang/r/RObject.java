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
package org.graalvm.visualvm.heapviewer.truffle.lang.r;

import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.TruffleFrame;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.Type;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;

/**
 *
 * @author Tomas Hurka
 */
class RObject extends TruffleObject.InstanceBased {
    
//    public static final DataType<RObject> DATA_TYPE = new DataType<RObject>(RObject.class, null, null);
    
    static final String R_OBJECT_FQN = "com.oracle.truffle.r.runtime.data.RBaseObject"; // NOI18N
    static final String R_SCALAR_FQN = "com.oracle.truffle.r.runtime.data.RScalarVector";   // NOI18N
    static final String R_WRAPPER_FQN = "com.oracle.truffle.r.runtime.data.RForeignWrapper";  // NOI18N
    private static final String RLOGICAL_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RLogicalVector";   // NOI18N
    private static final String RCOMPLEX_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RComplexVector";   // NOI18N
    private static final String RPAIR_LIST_FQN = "com.oracle.truffle.r.runtime.data.RPairList";   // NOI18N
    private static final String RNULL_FQN = "com.oracle.truffle.r.runtime.data.RNull"; // NOI18N
    private static final String[] typeMapping = new String[] {
        "RRawVector", "raw", // NOI18N
        "RRaw", "raw", // NOI18N
        "RLogicalVector", "logical", // NOI18N
        "RLogical", "logical", // NOI18N
        "RIntVector", "integer", // NOI18N
        "RInteger", "integer", // NOI18N
        "RForeignIntWrapper", "integer", // NOI18N
        "RIntSequence", "integer", // NOI18N
        "RDoubleVector", "double", // NOI18N
        "RDouble", "double", // NOI18N
        "RForeignDoubleWrapper", "double", // NOI18N
        "RComplexVector", "complex", // NOI18N
        "RComplex", "complex", // NOI18N
        "RStringVector", "character", // NOI18N
        "RString", "character", // NOI18N
        "RForeignStringWrapper", "character", // NOI18N
        "RList", "list", // NOI18N
        "RScalarList", "list", // NOI18N
        "RExpression", "expression", // NOI18N
        "RFunction", "closure", // NOI18N
        "RSymbol", "symbol", // NOI18N
        "REnvironment", "environment", // NOI18N
        "RPairList", "pairlist", // NOI18N
        "RArgsValuesAndNames", "pairlist", // NOI18N
        "RLanguage", "language", // NOI18N
        "RPromise", "promise", // NOI18N
        "RExternalPtr", "externalptr", // NOI18N
        "StdConnections", "connection", // NOI18N
        "RS4Object", "S4", // NOI18N
        "CharSXPWrapper", "charsxp_wrapper"}; // NOI18N

    private static Map<String,String> typeMap;

    static {
        typeMap = new HashMap();
        for (int i=0; i<typeMapping.length; i+=2) {
            typeMap.put(typeMapping[i], typeMapping[i+1]);
        }
    }

    private final Instance instance;
    private Instance data;
    private final Boolean complete;
    private final Integer refCount;
    private final Instance attributesInstance;
    private final String className;
    private final Instance frameInstance;
    private final String dataType;
    private DynamicObject attributes;
    private final List<FieldValue> fieldValues;
    private boolean namesComputed;
    private List<String> names;
    private boolean dimComputed;
    private List<Integer> dim;
    private TruffleFrame frame;
    
    private String type;


    public RObject(Instance instance) {
        this(null, instance);
    }

    public RObject(String type, Instance instance) {
        this.instance = instance;
        this.type = type;

        data = findDataField(instance);

        Object[] values = HeapUtils.getValuesOfFields(instance, "complete", "refCount", "attributes", "frameAccess"); // NOI18N

        Object completeO = values[0];
        complete = completeO == null ? null : Boolean.parseBoolean(completeO.toString());
        
        Object refCountO = values[1];
        refCount = refCountO == null ? null : Integer.parseInt(refCountO.toString());
        
        attributesInstance = (Instance) values[2];
        className = instance.getJavaClass().getName();
        dataType = data == null ? null : data.getJavaClass().getName().replace("[]", ""); // NOI18N
        fieldValues = new LazyFieldValues();
        Instance frameAccess = (Instance) values[3];
        if (frameAccess != null) {
            frameInstance = (Instance) frameAccess.getValueOfField("frame");    // NOI18N
        } else {
            frameInstance = null;
        }
        if (data == null && RPAIR_LIST_FQN.equals(className)) {
            data = new RPairList(instance);
        }
        if (data == null && isSubClassOf(instance, R_WRAPPER_FQN)) {
            data = getDataFromWrapper(instance);
        }
    }
    
    
    public static boolean isRObject(Instance rObj) {
        return isSubClassOf(rObj, R_OBJECT_FQN) || isSubClassOf(rObj, R_SCALAR_FQN)
                || isSubClassOf(rObj, R_WRAPPER_FQN);
    }
    
    private static boolean isSubClassOf(Instance i, String superClassName) {
        if (i != null) {
            JavaClass superCls = i.getJavaClass().getSuperClass();

            for (; superCls != null; superCls = superCls.getSuperClass()) {
                if (superCls.getName().equals(superClassName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    @Override
    public Instance getInstance() {
        return instance;
    }
    
    @Override
    public String getType(Heap heap) {
        if (type == null) type = getType(className);
        return type;
    }

    @Override
    public long getTypeId(Heap heap) {
        return instance.getJavaClass().getJavaClassId();
    }
    
    static String getRType(Instance instance) {
        return getType(instance.getJavaClass().getName());
    }
    
    private static String getType(String className) {
        String type = className.substring(className.lastIndexOf('.') + 1); // NOI18N
        int dindex = type.indexOf('$'); // NOI18N
        if (dindex>0) {
            type = type.substring(0, dindex);
        }
        String convertedType = typeMap.get(type);

        if (convertedType != null) {
            return convertedType;
        }
        return type;
    }

    @Override
    public long getSize() {
        long size = instance.getSize();
        if (data != null) {
            size += data.getSize();
        }
        return size;
    }
    
    @Override
    public long getRetainedSize() {
        return instance.getRetainedSize();
    }
    
    public List getValues() {
        if (data != null) {
            if (data instanceof PrimitiveArrayInstance) {
                return ((PrimitiveArrayInstance)data).getValues();
            }
            if (data instanceof ObjectArrayInstance) {
                return ((ObjectArrayInstance)data).getValues();
            }
        }
        return Collections.emptyList();
    }

    public int getLength() {
        if (data != null) {
            if (data instanceof PrimitiveArrayInstance) {
                int len = ((PrimitiveArrayInstance)data).getLength();
                if (RCOMPLEX_VECTOR_FQN.equals(className)) {
                    return len/2;
                }
                return len;
            }
            if (data instanceof ObjectArrayInstance) {
                return ((ObjectArrayInstance)data).getLength();
            }
        }
        return 0;
    }

    public boolean isPrimitiveArray() {
        return data instanceof PrimitiveArrayInstance;
    }
    
    public final boolean isTemporary() {
        return refCount == 0;
    }

    public final boolean isShared() {
        return refCount > 1;
    }

    public final boolean isComplete() {
        return complete;
    }
    
    public List<String> names() {
        if (namesComputed) {
            return names;
        }
        DynamicObject attrs = getAttributes();
        namesComputed = true;
        if (attrs != null) {
            FieldValue nameValue = attrs.getFieldValue("names");   // NOI18N
            if (nameValue instanceof ObjectFieldValue) {
                Instance nameInst = ((ObjectFieldValue)nameValue).getInstance();
                List namesArr = new RObject(nameInst).getValues();
                names = new ArrayList(namesArr.size());

                for (Object name : namesArr) {
                    Instance string = (Instance) name;

                    names.add(DetailsUtils.getInstanceString(string, null));
                }
                return names;
            }
        }
        return null;
    }

    public List<Integer> getDim() {
        if (dimComputed) {
            return dim;
        }
        DynamicObject attrs = getAttributes();
        dimComputed = true;
        if (attrs != null) {
            FieldValue dimsValue = attrs.getFieldValue("dim");   // NOI18N
            if (dimsValue instanceof ObjectFieldValue) {
                Instance dimsInst = ((ObjectFieldValue)dimsValue).getInstance();
                List dimsArr = new RObject(dimsInst).getValues();
                dim = new ArrayList(dimsArr.size());

                for (Object string : dimsArr) {
                    dim.add(Integer.valueOf((String)string));
                }
                return dim;
            }
        }
        return null;
    }

    List<FieldValue> getFieldValues() {
        return fieldValues;
    }

    DynamicObject getAttributes() {
        if (attributes == null && attributesInstance != null) {
            attributes = new DynamicObject(attributesInstance);
        }
        return attributes;
    }

    TruffleFrame getFrame() {
        if (frame == null && frameInstance != null) {
            frame = new TruffleFrame(frameInstance);
        }
        return frame;
    }

    List<FieldValue> getReferences() {
        List<Value> refs = instance.getReferences();
        List<FieldValue> robjRefs = new ArrayList();

        for (Value ref : refs) {
            Instance defInstance = ref.getDefiningInstance();
            if (ref instanceof ArrayItemValue) {
                if (defInstance instanceof ObjectArrayInstance) {
                    List<Value> arrRefs = defInstance.getReferences();

                    for (Value arrRef : arrRefs) {
                        Instance rInstance = arrRef.getDefiningInstance();

                        if (RObject.isRObject(rInstance)) {
                            RObject robject = new RObject(rInstance);
                            int index = ((ArrayItemValue)ref).getIndex();

                            robjRefs.add(robject.getFieldValues().get(index));
                        } else if (TruffleFrame.isTruffleFrame(rInstance)) {
                            if (arrRef instanceof ObjectFieldValue) {
                                ObjectFieldValue arrRefFV = (ObjectFieldValue) arrRef;
                                if (arrRefFV.getField().getName().equals("locals")) {
                                    List<Instance> frefs = getObjectFieldValueRefs(rInstance, "frame"); // NOI18N

                                    for (Instance fref : frefs) {
                                        List<Instance> farefs = getObjectFieldValueRefs(fref, "frameAccess");   // NOI18N

                                        for (Instance rObj : farefs) {
                                            if (RObject.isRObject(rObj)) {
                                                RObject refRObj = new RObject(rObj);
                                                TruffleFrame refFrame = refRObj.getFrame();

                                                if (refFrame != null) {
                                                    for (FieldValue fv : refFrame.getLocalFieldValues()) {
                                                        if (fv instanceof ObjectFieldValue) {
                                                            ObjectFieldValue ofv = (ObjectFieldValue) fv;
                                                            if (getInstance().equals(ofv.getInstance())) {
                                                                robjRefs.add(new FrameFieldValue(rObj, ofv));
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        addAttribute(rInstance, robjRefs);
                    }
                }
            }
            if (defInstance != null && defInstance.getJavaClass().getName().equals(RPAIR_LIST_FQN)) {
                FieldValue rootReference = findRootRPairList(defInstance);

                robjRefs.add(rootReference);
            }
            addAttribute(defInstance, robjRefs);
        }
        return robjRefs;
    }

    private List<Instance> getObjectFieldValueRefs(Instance refInstance, String fieldName) {
        List<Instance> foundRefs = new ArrayList<>();
        List<Value> refs = refInstance.getReferences();

        for (Value ref : refs) {
            if (ref instanceof ObjectFieldValue) {
                ObjectFieldValue refo = (ObjectFieldValue) ref;
                if (fieldName.equals(refo.getField().getName())) {
                    foundRefs.add(refo.getDefiningInstance());
                }
            }
        }
        return foundRefs;
    }

    private void addAttribute(Instance dynObjInstance, List<FieldValue> robjRefs) {
        if (DynamicObject.isDynamicObject(dynObjInstance)) {
            List<Value> refs = dynObjInstance.getReferences();

            for (Value ref : refs) {
                Instance defInstance = ref.getDefiningInstance();

                if (RObject.isRObject(defInstance)) {
                    RObject robject = new RObject(defInstance);
                    DynamicObject attrs = robject.getAttributes();

                    if (attrs != null && attrs.getInstance().equals(dynObjInstance)) {
                        for (FieldValue fv : attrs.getFieldValues()) {
                            if (fv instanceof ObjectFieldValue) {
                                ObjectFieldValue ofv = (ObjectFieldValue) fv;

                                if (ofv.getInstance().equals(instance)) {
                                    robjRefs.add(fv);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private FieldValue findRootRPairList(Instance pairList) {
        int index = 0;

        for (Instance parent = getParentRlist(pairList); parent != null; parent = getParentRlist(parent)) {
            index++;
            pairList = parent;
        }
        return new RObject(pairList).getFieldValues().get(index);
    }

    private Instance getParentRlist(Instance pairList) {
        List<Value> refs = pairList.getReferences();

        if (refs.size() == 1) {
           Value val = refs.get(0);

           if (val instanceof ObjectFieldValue) {
               ObjectFieldValue fval = (ObjectFieldValue) val;
               Instance parent = fval.getDefiningInstance();

               if (parent.getJavaClass().getName().equals(RPAIR_LIST_FQN) && fval.getField().getName().equals("cdr")) { // NOI18N
                   return parent;
               }
           }
        }
        return null;
    }

    static Instance getDataFromWrapper(Instance instance) {
        Instance delegate = (Instance) instance.getValueOfField("delegate"); // NOI18N

        if (delegate != null) {
            Instance proxy = (Instance) delegate.getValueOfField("proxy"); // NOI18N

            if (proxy != null) {
                return (Instance) proxy.getValueOfField("val$values"); // NOI18N
            }
        }
        return null;
    }

    static Instance findDataField(Instance instance) {
        for (Object val : instance.getFieldValues()) {
            FieldValue fv = (FieldValue) val;

            if (fv instanceof ObjectFieldValue && "data".equals(fv.getField().getName())) { // NOI18N
                Instance data = ((ObjectFieldValue)fv).getInstance();

                if (data != null && !instance.equals(data)) {
                    if (data.getJavaClass().isArray()) {
                        return data;
                    }
                    return findDataField(data);
                }
            }
        }
        return null;
    }

    private class RPairList implements ObjectArrayInstance {

        private Instance pairListInstance;
        private int length;
        private int size;
        private List values;

        private RPairList(Instance instance) {
            pairListInstance = instance;
            length = -1;
        }

        private void initData() {
            if (length == -1) {
                JavaClass pairListClass = pairListInstance.getJavaClass();
                boolean hasNames = false;

                length = 0;
                values = new ArrayList();
                List<String> nameList = new ArrayList();
                for (Instance cdr = pairListInstance;
                   cdr != null && cdr.getJavaClass().equals(pairListClass);
                   cdr = (Instance) cdr.getValueOfField("cdr")) { // NOI18N
                    String name = getName((Instance)cdr.getValueOfField("tag")); // NOI18N
                    length++;
                    values.add(cdr.getValueOfField("car")); // NOI18N
                    if (name != null) {
                        hasNames = true;
                    }
                    nameList.add(name);
                    size+=cdr.getSize();
                }
                if (hasNames) {
                    namesComputed = true;
                    names = nameList;
                }
            }
        }

        @Override
        public int getLength() {
            initData();
            return length;
        }

        @Override
        public List getValues() {
            initData();
            return values;
        }

        @Override
        public List getItems() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public List getFieldValues() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public boolean isGCRoot() {
            return pairListInstance.isGCRoot();
        }

        @Override
        public long getInstanceId() {
            return pairListInstance.getInstanceId()+1;
        }

        @Override
        public int getInstanceNumber() {
            return pairListInstance.getInstanceNumber();
        }

        @Override
        public JavaClass getJavaClass() {
            return pairListInstance.getJavaClass();
        }

        @Override
        public Instance getNearestGCRootPointer() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public long getReachableSize() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public List getReferences() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public long getRetainedSize() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public long getSize() {
            initData();
            return size;
        }

        @Override
        public List getStaticFieldValues() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public Object getValueOfField(String string) {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RPairList) {
                RPairList rlist = (RPairList) obj;
                return pairListInstance.equals(rlist.pairListInstance);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return pairListInstance.hashCode();
        }

        private String getName(Instance tag) {
            if (tag != null) {
                if (tag.getJavaClass().getName().equals(RNULL_FQN)) {
                    return null;
                }
                return DetailsSupport.getDetailsString(tag, null);
            }
            return null;
        }
    }
    
    private class LazyFieldValues extends AbstractList<FieldValue> {

        @Override
        public FieldValue get(int index) {
            if (isPrimitiveArray()) {
                if (RLOGICAL_VECTOR_FQN.equals(className)) {
                    return new RLogicalFieldValue(index);
                } else if (RCOMPLEX_VECTOR_FQN.equals(className)) {
                    return new RComplexFieldValue(index);
                }
                return new RFieldValue(index);
            }
            return new RObjectFieldValue(index);
        }

        @Override
        public int size() {
            return getLength();
        }
    }
    
    private class RFieldValue implements FieldValue {

        int index;

        private RFieldValue(int i) {
            index = i;
        }
        
        @Override
        public Field getField() {
            return new RField(index);
        }

        @Override
        public String getValue() {
            return (String) getValues().get(index);
        }

        @Override
        public Instance getDefiningInstance() {
            return instance;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RFieldValue) {
                RFieldValue rfv = (RFieldValue) obj;

                return instance.equals(rfv.getDefiningInstance())
                     && index == rfv.index;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * instance.hashCode() + index;
        }
    }

    private class RLogicalFieldValue extends RFieldValue {

        public RLogicalFieldValue(int i) {
            super(i);
        }

        @Override
        public Field getField() {
            return new RNamedField("logical", index); // NOI18N
        }

        @Override
        public String getValue() {
            String valString = (String) getValues().get(index);
            int val = Integer.parseInt(valString);

            if (val == 0) {
                valString = "FALSE"; // NOI18N
            } else if (val == 1) {
                valString = "TRUE"; // NOI18N
            }
            return valString;
        }
    }

    private class RComplexFieldValue extends RFieldValue {

        public RComplexFieldValue(int i) {
            super(i);
        }

        @Override
        public Field getField() {
            return new RNamedField("complex", index); // NOI18N
        }

        @Override
        public String getValue() {
            List vals = getValues();
            return vals.get(2*index)+"+"+vals.get(2*index+1)+"i"; // NOI18N
        }
    }
    
    private class RObjectFieldValue implements ObjectFieldValue {

        private int index;
        
        private RObjectFieldValue(int i) {
            index = i;
        }

        @Override
        public Instance getInstance() {
            return (Instance) getValues().get(index);
        }

        @Override
        public Field getField() {
            return new RField(index);
        }

        @Override
        public String getValue() {
            return String.valueOf(getInstance().getInstanceId());
        }

        @Override
        public Instance getDefiningInstance() {
            return instance;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RObjectFieldValue) {
                RObjectFieldValue rfv = (RObjectFieldValue) obj;

                return instance.equals(rfv.getDefiningInstance())
                     && index == rfv.index;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * instance.hashCode() + index;
        }
    }
    
    private class FrameFieldValue implements ObjectFieldValue {

        final ObjectFieldValue frameValue;
        final Instance rObject;

        private FrameFieldValue(Instance ro, ObjectFieldValue fv) {
            rObject = ro;
            frameValue = fv;
        }

        @Override
        public Instance getInstance() {
            return frameValue.getInstance();
        }

        @Override
        public Field getField() {
            return frameValue.getField();
        }

        @Override
        public String getValue() {
            return frameValue.getValue();
        }

        @Override
        public Instance getDefiningInstance() {
            return rObject;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof FrameFieldValue) {
                return frameValue.equals(obj);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return frameValue.hashCode();
        }
    }

    private class RField implements Field {

        private int index;

        private RField(int i) {
            index = i;
        }
        
        @Override
        public JavaClass getDeclaringClass() {
            return instance.getJavaClass();
        }

        @Override
        public String getName() {
            List<Integer> dims = getDim();
            if (dims != null) {
                int rindex = index;
                StringBuilder name = new StringBuilder();

                for (Integer dim : dims) {
                    int offset = rindex % dim;
                    rindex /= dim;
                    name.append(offset+1).append(','); // NOI18N
                }
                return '['+name.substring(0, name.length()-1)+']'; // NOI18N
            }
            List<String> names = names();
            String name = "["+(index+1)+"]"; // NOI18N
            if (names != null) {
                return name+" ("+names.get(index)+")"; // NOI18N
            }
            return name;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public Type getType() {
            return new Type() {
                @Override
                public String getName() {
                    return dataType;
                }
            };
        }
    }

    private class RNamedField extends RField {

        private final String name;

        private RNamedField(String n, int i) {
            super(i);
            name = n;
        }

        @Override
        public Type getType() {
            return new Type() {
                @Override
                public String getName() {
                    return name;
                }
            };
        }
    }
}

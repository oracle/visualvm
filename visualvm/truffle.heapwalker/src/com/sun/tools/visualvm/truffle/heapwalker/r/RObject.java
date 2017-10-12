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
package com.sun.tools.visualvm.truffle.heapwalker.r;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleFrame;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;

/**
 *
 * @author Tomas Hurka
 */
public class RObject {
    
    public static final DataType<RObject> DATA_TYPE = new DataType<RObject>(RObject.class, null, null);
    
    static final String R_OBJECT_FQN = "com.oracle.truffle.r.runtime.data.RBaseObject"; // NOI18N
    private static final String RLOGICAL_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RLogicalVector";   // NOI18N
    private static final String RCOMPLEX_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RComplexVector";   // NOI18N
    private static final String RPAIR_LIST_FQN = "com.oracle.truffle.r.runtime.data.RPairList";   // NOI18N
    private static final String RNULL_FQN = "com.oracle.truffle.r.runtime.data.RNull"; // NOI18N
    private static final String[] typeMaping = new String[] {
        "RRawVector", "raw",
        "RLogicalVector", "logical",
        "RIntVector", "integer",
        "RDoubleVector", "double",
        "RComplexVector", "complex",
        "RStringVector", "character",
        "RList", "list",
        "RExpression", "expression",
        "RFunction", "function",
        "RSymbol", "symbol",
        "REnvironment", "environment",
        "RPairList", "pairlist",
        "RLanguage", "language",
        "RPromise", "promise",
        "RExternalPtr", "externalptr",
        "RS4Object", "S4"};

    private static Map<String,String> typeMap;

    static {
        typeMap = new HashMap();
        for (int i=0; i<typeMaping.length; i+=2) {
            typeMap.put(typeMaping[i], typeMaping[i+1]);
        }
    }

    private final Instance instance;
    private Instance data;
    private final Boolean complete;
    private final Integer refCount;
    private final Instance attributesInstance;
    private final String className;
    private final Instance frameInstance;
    private String type;
    private DynamicObject attributes;
    private List<FieldValue> fieldValues;
    private boolean namesComputed;
    private List<String> names;
    private boolean dimComputed;
    private List<Integer> dim;
    private TruffleFrame frame;

    public RObject(Instance instance) {
        this.instance = instance;
        data = (Instance) instance.getValueOfField("data"); // NOI18N
        complete = (Boolean) instance.getValueOfField("complete");  // NOI18N
        refCount = (Integer) instance.getValueOfField("refCount");  // NOI18N
        attributesInstance = (Instance) instance.getValueOfField("attributes"); // NOI18N
        className = instance.getJavaClass().getName();
        type = null;
        if (data != null) type = data.getJavaClass().getName().replace("[]", "");
        fieldValues = new LazyFieldValues();
        Instance frameAccess = (Instance) instance.getValueOfField("frameAccess"); // NOI18N
        if (frameAccess != null) {
            frameInstance = (Instance) frameAccess.getValueOfField("frame");
        } else {
            frameInstance = null;
        }
        if (data == null && RPAIR_LIST_FQN.equals(className)) {
            data = new RPairList(instance);
        }
    }
    
    
    public static boolean isRObject(Instance rObj) {
        return isSubClassOf(rObj, R_OBJECT_FQN);
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
    
    
    public Instance getInstance() {
        return instance;
    }
    
    public String getType() {
        String type = className.substring(className.lastIndexOf('.') + 1);
        int dindex = type.indexOf('$');
        if (dindex>0) {
            type = type.substring(0, dindex);
        }
        String convertedType = typeMap.get(type);

        if (convertedType != null) {
            return convertedType;
        }
        return type;
    }

    public long getSize() {
        long size = instance.getSize();
        if (data != null) {
            size += data.getSize();
        }
        return size;
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
                   cdr = (Instance) cdr.getValueOfField("cdr")) {
                    String name = getName((Instance)cdr.getValueOfField("tag"));
                    length++;
                    values.add(cdr.getValueOfField("car"));
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List getFieldValues() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getReachableSize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List getReferences() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getRetainedSize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getSize() {
            initData();
            return size;
        }

        @Override
        public List getStaticFieldValues() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object getValueOfField(String string) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    }

    private class RLogicalFieldValue extends RFieldValue {

        public RLogicalFieldValue(int i) {
            super(i);
        }

        @Override
        public Field getField() {
            return new RNamedField("logical", index);
        }

        @Override
        public String getValue() {
            String valString = (String) getValues().get(index);
            int val = Integer.valueOf(valString);

            if (val == 0) {
                valString = "FALSE";
            } else if (val == 1) {
                valString = "TRUE";
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
            return new RNamedField("complex", index);
        }

        @Override
        public String getValue() {
            List vals = getValues();
            return vals.get(2*index)+"+"+vals.get(2*index+1)+"i";
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
                    name.append(offset+1).append(',');
                }
                return '['+name.substring(0, name.length()-1)+']';
            }
            List<String> names = names();
            String name = "["+(index+1)+"]";
            if (names != null) {
                return name+" ("+names.get(index)+")";
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
                    return type;
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

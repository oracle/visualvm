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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;

/**
 *
 * @author Tomas Hurka
 */
public class RObject {
    
    public static final DataType<RObject> DATA_TYPE = new DataType<RObject>(RObject.class, null, null);
    
    static final String R_OBJECT_FQN = "com.oracle.truffle.r.runtime.data.RBaseObject"; // NOI18N
    
    
    private final Instance instance;
    private final Instance data;
    private final Boolean complete;
    private final Integer refCount;
    private final Instance attributesInstance;
    private String type;
    private DynamicObject attributes;
    private List<FieldValue> fieldValues;

    public RObject(Instance instance) {
        this.instance = instance;
        data = (Instance) instance.getValueOfField("data"); // NOI18N
        complete = (Boolean) instance.getValueOfField("complete");  // NOI18N
        refCount = (Integer) instance.getValueOfField("refCount");  // NOI18N
        attributesInstance = (Instance) instance.getValueOfField("attributes"); // NOI18N
        type = null;
        if (data != null) type = data.getJavaClass().getName().replace("[]", "");
        fieldValues = new LazyFieldValues();
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
        return instance.getJavaClass().getName();
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
                return ((PrimitiveArrayInstance)data).getLength();
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
        DynamicObject attrs = getAttributes();
        if (attrs != null) {
            FieldValue nameValue = attrs.getFieldValue("names");   // NOI18N
            if (nameValue instanceof ObjectFieldValue) {
                Instance nameInst = ((ObjectFieldValue)nameValue).getInstance();
                List namesArr = new RObject(nameInst).getValues();
                List<String> namesStr = new ArrayList(namesArr.size());

                for (Object name : namesArr) {
                    Instance string = (Instance) name;

                    namesStr.add(DetailsUtils.getInstanceString(string, null));
                }
                return namesStr;
            }
        }
        return null;
    }
    
    List<FieldValue> getFieldValues() {
        return fieldValues;
    }
    
    public Instance getArray() {
        return data;
    }
    
    private DynamicObject getAttributes() {
        if (attributes == null && attributesInstance != null) {
            attributes = new DynamicObject(attributesInstance);
        }
        return attributes;
    }
    
    private class LazyFieldValues extends AbstractList<FieldValue> {

        @Override
        public FieldValue get(int index) {
            if (isPrimitiveArray()) {
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
        
        public RFieldValue(int i) {
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
            List<String> names = names();
            String name = "["+index+"]";
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
}

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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObject;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
public class PythonObject {

    public static final DataType<PythonObject> DATA_TYPE = new DataType<PythonObject>(PythonObject.class, null, null);

    static final String PYTHON_OBJECT_FQN = "com.oracle.graal.python.runtime.object.PythonObject"; // NOI18N
    static final String PYTHON_LIST_FQN = "com.oracle.graal.python.runtime.sequence.PList"; // NOI18N
    static final String TREEMAP_ENTRY_FQN = "java.util.TreeMap$Entry";  // NOI18N
    static final String TREEMAP_FQN = "java.util.TreeMap";  // NOI18N

    private final Instance instance;
    private final Instance storage;
    private final Instance store;
    private final ObjectArrayInstance array;
    private final Instance map;
    private final Instance set;
    private final Instance pythonClass;
    private String listType;
    private boolean isPrimitiveList;

    public PythonObject(Instance instance) {
        this.instance = instance;
        storage = (Instance) instance.getValueOfField("storage"); // NOI18N
        pythonClass = (Instance) instance.getValueOfField("pythonClass"); // NOI18N
        store = (Instance) instance.getValueOfField("store"); // NOI18N
        array = (ObjectArrayInstance) instance.getValueOfField("array"); // NOI18N
        map = (Instance) instance.getValueOfField("map"); // NOI18N
        set = (Instance) instance.getValueOfField("set"); // NOI18N
    }

    public static boolean isPythonObject(Instance rObj) {
        return isSubClassOf(rObj, PYTHON_OBJECT_FQN);
    }

    List<FieldValue> getItems() {
        if (store != null || array != null) {
            return getListFields();
        }
        return Collections.EMPTY_LIST;
    }

    List<FieldValue> getAttributes() {
        if (map != null) {
            return getMapFields();
        }
        if (set != null) {
            return getSetFields();
        }
        return new DynamicObject(storage).getFieldValues();
    }

    public Instance getInstance() {
        return instance;
    }

    public String getType() {
        return DetailsUtils.getInstanceString(pythonClass, null);
    }

    public long getSize() {
        long size = instance.getSize();
        if (storage != null) {
            size += storage.getSize();
        }
        if (store != null) {
            size += store.getSize();
        }
        if (array != null) {
            size += array.getSize();
        }
        return size;
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
                        Instance pInstance = arrRef.getDefiningInstance();

                        if (PythonObject.isPythonObject(pInstance)) {
                            addItem(pInstance, ref, robjRefs);
                        } else {
                            Instance store = getReference(pInstance, PYTHON_OBJECT_FQN, "store");
                            if (PythonObject.isPythonObject(store)) {
                                addItem(store, ref, robjRefs);
                            }
                        }
                        addAttribute(pInstance, robjRefs);
                    }
                }
            }
            if (defInstance != null && defInstance.getJavaClass().getName().equals(TREEMAP_ENTRY_FQN)) {
                FieldValue rootReference = findRootPMap(defInstance);

                robjRefs.add(rootReference);
            }
            addAttribute(defInstance, robjRefs);
        }
        return robjRefs;
    }

    private void addItem(Instance pInstance, Value ref, List<FieldValue> robjRefs) {
        PythonObject pobject = new PythonObject(pInstance);
        int index = ((ArrayItemValue)ref).getIndex();
        List<FieldValue> items = pobject.getItems();

        if (index < items.size()) {
            FieldValue fv = items.get(index);
            if (fv instanceof ObjectFieldValue) {
                ObjectFieldValue ofv = (ObjectFieldValue) fv;
                if (instance.equals(ofv.getInstance())) {
                    robjRefs.add(fv);
                }
            }
        }
    }

    private FieldValue findRootPMap(Instance mapEntry) {
        for (Instance parent = getParentTreeEntry(mapEntry); parent != null; parent = getParentTreeEntry(parent)) {
            mapEntry = parent;
        }
        // top TreeMap$Entry
        Instance treeMap = getReference(mapEntry, TREEMAP_FQN, "root");
        Instance pythonObject = getReference(treeMap, PYTHON_OBJECT_FQN, "map");

        if (isPythonObject(pythonObject)) {
            for (FieldValue fv : new PythonObject(pythonObject).getAttributes()) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;
                    if (instance.equals(ofv.getInstance())) {
                        return fv;
                    }
                }
            }
        }
        return null;
    }

    private Instance getParentTreeEntry(Instance treeEntry) {
        return (Instance) treeEntry.getValueOfField("parent");
    }

    private Instance getReference(Instance instance, String definingClass, String fieldName) {
        if (instance == null) return null;
        List<Value> refs = instance.getReferences();
        for (Value ref : refs) {
            if (ref instanceof ObjectFieldValue) {
               ObjectFieldValue fval = (ObjectFieldValue) ref;
               Instance parent = fval.getDefiningInstance();

               if (fval.getField().getName().equals(fieldName) && isSubClassOf(parent, definingClass)) {
                   return parent;
               }
            }
        }
        return null;
    }

    private void addAttribute(Instance dynObjInstance, List<FieldValue> robjRefs) {
        if (DynamicObject.isDynamicObject(dynObjInstance)) {
            List<Value> refs = dynObjInstance.getReferences();

            for (Value ref : refs) {
                Instance defInstance = ref.getDefiningInstance();

                if (PythonObject.isPythonObject(defInstance)) {
                    PythonObject pobject = new PythonObject(defInstance);

                    if (pobject.storage.equals(dynObjInstance)) {
                        for (FieldValue fv : pobject.getAttributes()) {
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

    private List getValues() {
        Instance vals = null;

        if (store != null) {
            vals = (Instance)store.getValueOfField("values");
        }
        if (array != null) {
            vals = array;
        }
        if (vals != null) {
            listType = vals.getJavaClass().getName().replace("[]", "");
            if (vals instanceof ObjectArrayInstance) {
                return ((ObjectArrayInstance)vals).getValues();
            }
            if (vals instanceof PrimitiveArrayInstance) {
                isPrimitiveList = true;
                return ((PrimitiveArrayInstance)vals).getValues();
            }
        }
        return Collections.emptyList();
    }

    private int getLength() {
        if (store != null) {
            Integer len = (Integer) store.getValueOfField("length");

            if (len != null) {
                return len.intValue();
            }
            return 0;
        }
        if (array != null) {
            return array.getLength();
        }
        return 0;
    }

    private static boolean isSubClassOf(Instance i, String superClassName) {
        if (i != null) {
            JavaClass superCls = i.getJavaClass();

            for (; superCls != null; superCls = superCls.getSuperClass()) {
                if (superCls.getName().equals(superClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<FieldValue> getListFields() {
        return new LazyFieldValues(getValues());
    }

    private List<FieldValue> getMapFields() {
        List fields = new ArrayList();
        Instance rootEntry = (Instance) map.getValueOfField("root");

        getEntries(false, rootEntry, fields);
        return fields;
    }

    private List<FieldValue> getSetFields() {
        List fields = new ArrayList();
        Instance m = (Instance) set.getValueOfField("m");
        Instance rootEntry = (Instance) m.getValueOfField("root");

        getEntries(true, rootEntry, fields);
        return fields;
    }

    private void getEntries(boolean isSet, Instance entry, List fields) {
        if (entry != null) {
            getEntries(isSet, (Instance) entry.getValueOfField("left"), fields);
            fields.add(new PythonMapEntryFieldValue(isSet, entry));
            getEntries(isSet, (Instance) entry.getValueOfField("right"), fields);
        }
    }


    private class PythonMapEntryFieldValue implements ObjectFieldValue {

        Instance entry;
        boolean isSet;

        private PythonMapEntryFieldValue(boolean set, Instance e) {
            entry = e;
            isSet = set;
        }

        @Override
        public Instance getInstance() {
            if (isSet) {
                return (Instance) entry.getValueOfField("key");  // NOI18N
            }
            return (Instance) entry.getValueOfField("value");  // NOI18N
        }

        @Override
        public Field getField() {
            if (isSet) {
                return new PythonMapEntryField("item");
            }
            Instance key = (Instance)entry.getValueOfField("key");
            String name = DetailsUtils.getInstanceString(key, null);
            return new PythonMapEntryField(name);
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

    private class PythonMapEntryField extends PythonField {

        String name;

        private PythonMapEntryField(String n) {
            super(0);
            name = n;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private class PythonFieldValue implements FieldValue {
        private int index;
        Object value;

        private PythonFieldValue(int i, Object val) {
            index = i;
            value = val;
        }

        @Override
        public Field getField() {
            return new PythonField(index);
        }

        @Override
        public String getValue() {
            return (String)value;
        }

        @Override
        public Instance getDefiningInstance() {
            return instance;
        }

    }

    private class PythonObjectFieldValue extends PythonFieldValue implements ObjectFieldValue {

        private PythonObjectFieldValue(int i, Instance val) {
            super(i,val);
        }

        @Override
        public String getValue() {
            return String.valueOf(getInstance().getInstanceId());
        }

        @Override
        public Instance getInstance() {
            return (Instance)value;
        }
    }

    private class PythonField implements Field {

        private int index;

        private PythonField(int i) {
            index = i;
        }

        @Override
        public JavaClass getDeclaringClass() {
            return instance.getJavaClass();
        }

        @Override
        public String getName() {
            return  "["+index+"]";
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
                    return listType;
                }
            };
        }
    }

    private class LazyFieldValues extends AbstractList<FieldValue> {

        List values;

        private LazyFieldValues(List vals) {
            values = vals;
        }

        @Override
        public FieldValue get(int index) {
            if (isPrimitiveList) {
                return new PythonFieldValue(index, values.get(index));
            }
            return new PythonObjectFieldValue(index, (Instance) values.get(index));
        }

        @Override
        public int size() {
            return getLength();
        }
    }
}

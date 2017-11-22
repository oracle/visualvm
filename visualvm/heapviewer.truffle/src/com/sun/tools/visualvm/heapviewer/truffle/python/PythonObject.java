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
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
public class PythonObject {

    public static final DataType<PythonObject> DATA_TYPE = new DataType<PythonObject>(PythonObject.class, null, null);

    static final String PYTHON_OBJECT_FQN = "com.oracle.graal.python.runtime.object.PythonObject"; // NOI18N
    static final String PYTHON_LIST_FQN = "com.oracle.graal.python.runtime.sequence.PList"; // NOI18N
    private final Instance instance;
    private final Instance storage;
    private final Instance store;
    private final ObjectArrayInstance array;
    private final Instance map;
    private final Instance set;
    private final Instance pythonClass;

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

    List<FieldValue> getFieldValues() {
        if (store != null || array != null) {
            return getListFields();
        }
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

    private List getValues() {
        ObjectArrayInstance vals = null;

        if (store != null) {
            vals = (ObjectArrayInstance)store.getValueOfField("values");
        }
        if (array != null) {
            vals = array;
        }
        if (vals != null) {
            return vals.getValues();
        }
        return Collections.emptyList();
    }

    private int getLength() {
        if (store != null) {
            Integer len = (Integer) store.getValueOfField("length");

            return len.intValue();
        }
        if (array != null) {
            return array.getLength();
        }
        return 0;
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

    private List<FieldValue> getListFields() {
        return new LazyFieldValues(getValues());
    }

    private List<FieldValue> getMapFields() {
        List fields = new ArrayList();
        Instance rootEntry = (Instance) map.getValueOfField("root");

        getEntries(rootEntry, fields);
        return fields;
    }

    private List<FieldValue> getSetFields() {
        List fields = new ArrayList();
        Instance m = (Instance) set.getValueOfField("m");
        Instance rootEntry = (Instance) m.getValueOfField("root");

        getEntries(rootEntry, fields);
        return fields;
    }

    private void getEntries(Instance entry, List fields) {
        if (entry != null) {
            getEntries((Instance) entry.getValueOfField("left"), fields);
            fields.add(new PythonMapEntryFieldValue(entry));
            getEntries((Instance) entry.getValueOfField("right"), fields);
        }
    }


    private class PythonMapEntryFieldValue implements ObjectFieldValue {

        Instance entry;

        private PythonMapEntryFieldValue(Instance e) {
            entry = e;
        }

        @Override
        public Instance getInstance() {
            return (Instance) entry.getValueOfField("value");  // NOI18N
        }

        @Override
        public Field getField() {
            return new PythonMapEntryField((Instance)entry.getValueOfField("key"));
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

        Instance key;

        private PythonMapEntryField(Instance k) {
            super(0);
            key = k;

        }

        @Override
        public String getName() {
            return DetailsUtils.getInstanceString(key, null);
        }
    }

    private class PythonObjectFieldValue implements ObjectFieldValue {
        int index;
        Instance value;

        private PythonObjectFieldValue(int i, Instance val) {
            index = i;
            value = val;
        }

        @Override
        public Field getField() {
            return new PythonField(index);
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
        public Instance getInstance() {
            return value;
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
                    return "Object";
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
            return new PythonObjectFieldValue(index, (Instance) values.get(index));
        }

        @Override
        public int size() {
            return getLength();
        }
    }

}

/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.ruby;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.Type;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyObject extends DynamicObject {

    private boolean isPrimitiveList;
    private String listType;
    private Instance array;

    RubyObject(Instance instance) {
        this(null, instance);
    }
    
    RubyObject(String type, Instance instance) {
        super(type, instance);
        Object storage = (Instance) instance.getValueOfField("store");
        if (storage instanceof ObjectArrayInstance || storage instanceof PrimitiveArrayInstance) {
            array =  (Instance) storage;
        }
    }
    
    boolean isRubyObject() {
        return isRubyLangId(getLanguageId());
    }

    @Override
    protected String computeType() {
        Instance metaClass = (Instance) getInstance().getValueOfField("metaClass");
        if (metaClass == null) {
            return super.computeType();
        }
        return DetailsUtils.getInstanceFieldString(metaClass, "nonSingletonClass");
    }

    List<FieldValue> getItems() {
        if (array != null) {
            return getListFields();
        }
        return Collections.EMPTY_LIST;
    }

    private List<FieldValue> getListFields() {
        return new LazyFieldValues(getValues());
    }

    private List getValues() {
        if (array != null) {
            listType = array.getJavaClass().getName().replace("[]", ""); // NOI18N
            if (array instanceof ObjectArrayInstance) {
                return ((ObjectArrayInstance)array).getValues();
            }
            if (array instanceof PrimitiveArrayInstance) {
                isPrimitiveList = true;
                return ((PrimitiveArrayInstance)array).getValues();
            }
        }
        return Collections.emptyList();
    }

    static boolean isRubyObject(Instance instance) {
        return DynamicObject.isDynamicObject(instance) &&
               isRubyLangId(DynamicObject.getLanguageId(instance));
    }
    
    private static boolean isRubyLangId(JavaClass langIdClass) {
        String className = langIdClass.getName();

        return RubyHeapFragment.RUBY_LANG_ID.equals(className)
               || RubyHeapFragment.RUBY_LANG_ID1.equals(className)
               || RubyHeapFragment.RUBY_LANG_ID2.equals(className);
    }

    private class LazyFieldValues extends AbstractList<FieldValue> {

        private List values;

        private LazyFieldValues(List vals) {
            values = vals;
        }

        @Override
        public FieldValue get(int index) {
            if (isPrimitiveList) {
                return new RubyFieldValue(index, values.get(index));
            }
            return new RubyObjectFieldValue(index, (Instance) values.get(index));
        }

        @Override
        public int size() {
            Object size = getInstance().getValueOfField("size");        // NOI18N
            if (size instanceof Integer) {
                return ((Integer)size).intValue();
            }
            return 0;
        }
    }

    private class RubyFieldValue implements FieldValue {
        private int index;
        Object value;

        private RubyFieldValue(int i, Object val) {
            index = i;
            value = val;
        }

        @Override
        public Field getField() {
            return new RubyField(index);
        }

        @Override
        public String getValue() {
            return (String)value;
        }

        @Override
        public Instance getDefiningInstance() {
            return getInstance();
        }

    }

    private class RubyObjectFieldValue extends RubyFieldValue implements ObjectFieldValue {

        private RubyObjectFieldValue(int i, Instance val) {
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

    private class RubyField implements Field {

        private int index;

        private RubyField(int i) {
            index = i;
        }

        @Override
        public JavaClass getDeclaringClass() {
            return getInstance().getJavaClass();
        }

        @Override
        public String getName() {
            return  "["+index+"]"; // NOI18N
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public Type getType() {
            return new RFieldType(listType);
        }
    }

    private static class RFieldType implements Type {

        private final String name;

        private RFieldType(String n) {
            name = n;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RFieldType) {
                return getName().equals(((RFieldType)obj).getName());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }
}

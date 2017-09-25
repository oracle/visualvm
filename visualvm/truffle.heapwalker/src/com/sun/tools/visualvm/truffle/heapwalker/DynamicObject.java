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
package com.sun.tools.visualvm.truffle.heapwalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.PrimitiveType;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class DynamicObject {
    
    public static final DataType<DynamicObject> DATA_TYPE = new DataType<DynamicObject>(DynamicObject.class, null, null);

    static final String DYNAMIC_OBJECT_FQN = "com.oracle.truffle.api.object.DynamicObject";
    private static final String LOCATION_FQN = "com.oracle.truffle.api.object.Location";
    private static final String ENTERPRISE_PACKAGE = "com.oracle.truffle.object.enterprise";
    private static final String PROPERTY_MAP_FQN = "com.oracle.truffle.object.ConsListPropertyMap";
    private static final String TRIE_PROPERTY_MAP_FQN = "com.oracle.truffle.object.TriePropertyMap";
    private static final String PROPERTY_FQN = "com.oracle.truffle.object.PropertyImpl";
    private static final String OBJECT_TYPE_FQN = "com.oracle.truffle.api.object.ObjectType";

    private final Instance instance;
    
    private Instance shape;
    private String type;
    
//    private List<Property> properties;
    private List<FieldValue> values;
    private List<FieldValue> staticValues;

    public DynamicObject(Instance dynObj) {
        this.instance = dynObj;
    }
    
    public Instance getInstance() {
        return instance;
    }
    
    public List<FieldValue> getReferences() {
        List<FieldValue> dynObjRefs = new ArrayList();

        if (getShape() != null) {
            List<Value> refs = instance.getReferences();
            Instance lastInstanceRef = null;

            for (Value ref : refs) {
                if (ref instanceof ObjectFieldValue) {
                    ObjectFieldValue objRef = (ObjectFieldValue) ref;
                    Instance instanceRef = objRef.getDefiningInstance();

                    if (!instanceRef.equals(lastInstanceRef) && DynamicObject.isDynamicObject(instanceRef)) {
                        DynamicObject dynObj = new DynamicObject(instanceRef);

                        List<FieldValue> fieldValues = dynObj.getFieldValues();
                        for (FieldValue fieldVal : fieldValues) {
                            if (fieldVal instanceof ObjectFieldValue) {
                                ObjectFieldValue fieldValObj = (ObjectFieldValue) fieldVal;

                                if (instance.equals(fieldValObj.getInstance())) {
                                    dynObjRefs.add(fieldVal);
                                }
                            }
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableList(dynObjRefs);
    }

    public List<FieldValue> getFieldValues() {
        if (values == null) initFields();
        return values;
    }
    
    public FieldValue getFieldValue(String name) {
        for (FieldValue value : getFieldValues())
            if (name.equals(value.getField().getName()))
                    return value;
        return null;
    }
    
    public FieldValue[] getFieldValues(String... names) {
        FieldValue[] values = new FieldValue[names.length];
        for (FieldValue value : getFieldValues())
            for (int i = 0; i < names.length; i++)
                if (names[i].equals(value.getField().getName()))
                    values[i] = value;
        return values;
    }

    public List<FieldValue> getStaticFieldValues() {
        if (staticValues == null) initFields();
        return staticValues;
    }

    public Instance getShape() {
        if (shape == null) shape = (Instance)instance.getValueOfField("shape"); // NOI18N
        return shape;
    }
    
    public String getType(Heap heap) {
        if (type == null) type = DetailsSupport.getDetailsString(getShape(), heap);
        return type;
    }
    
    public JavaClass getLanguageId() {
        Instance sh = getShape();
        if (sh != null) {
            Instance objectType = (Instance) sh.getValueOfField("objectType");
            if (objectType != null) {
                JavaClass objTypeCls = objectType.getJavaClass();

                while (objTypeCls != null) {
                    JavaClass superObjType = objTypeCls.getSuperClass();

                    if (OBJECT_TYPE_FQN.equals(superObjType.getName())) {
                        return objTypeCls;
                    }
                    objTypeCls = superObjType;
                }
            }
        }
        return null;
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DynamicObject)) return false;
        return instance.equals(((DynamicObject)o).instance);
    }
    
    public int hashCode() {
        return instance.hashCode();
    }
    
    
    private void initFields() {
        Instance propertyMap = getValueofFields(instance, "shape", "fastMapRef", "referent"); // NOI18N
        if (propertyMap == null) propertyMap = getValueofFields(instance, "shape", "propertyMap"); // NOI18N
        if (propertyMap != null) {
//            properties = new ArrayList();
            values = new ArrayList();
            staticValues = new ArrayList();

            for (Instance ip : getMapValues(propertyMap)) {
                Property p = new Property(ip);
//                properties.add(p);
                if (p.isStatic()) staticValues.add(p.getValue(instance));
                else values.add(p.getValue(instance));
            }
        } else {
            values = Collections.EMPTY_LIST;
            staticValues = Collections.EMPTY_LIST;
        }
    }

    private static Instance getValueofFields(Instance instance, String... fields) {
        if (instance != null) {
            for (String field : fields) {
                Object val = instance.getValueOfField(field);
                if (val == null || !(val instanceof Instance)) {
                    return null;
                }
                instance = (Instance) val;
            }
        }
        return instance;
    }

    private static List<Instance> getMapValues(Instance propertyMap) {
        if (propertyMap != null) {
            String mapClass = propertyMap.getJavaClass().getName();
            if (mapClass.equals(HashMap.class.getName())) {
                return getHashMapValues(propertyMap);
            }
            if (mapClass.equals(PROPERTY_MAP_FQN)) {
                return getConsListValues(propertyMap);
            }
            if (mapClass.equals(TRIE_PROPERTY_MAP_FQN)) {
                return getTrieValues(propertyMap);
            }
        }
        return null;
    }

    private static List<Instance> getHashMapValues(Instance propertyMap) {
        List<Instance> mapValues = new ArrayList();
        Object val = propertyMap.getValueOfField("table");  // NOI18N

        if (val != null && val instanceof ObjectArrayInstance) {
            ObjectArrayInstance table = (ObjectArrayInstance) val;

            for (Object el : table.getValues()) {
                for (Instance node = (Instance) el; node != null; node = (Instance) node.getValueOfField("next")) {    // NOI18N
                    Object value = node.getValueOfField("value");   // NOI18N

                    if (value != null && value instanceof Instance) {
                        mapValues.add((Instance) value);
                    }
                }
            }
        }
        return mapValues;
    }

    private static List<Instance> getConsListValues(Instance propertyMap) {
        List<Instance> mapValues = new ArrayList();

        for (Instance node = propertyMap; node != null; node = (Instance) node.getValueOfField("car")) {    // NOI18N
            Object value = node.getValueOfField("cdr"); // NOI18N

            if (value != null && value instanceof Instance) {
                mapValues.add((Instance) value);
            }
        }
        return mapValues;
    }

    private static List<Instance> getTrieValues(Instance propertyMap) {
        List<Instance> mapValues = new ArrayList();
        Object root = propertyMap.getValueOfField("root");  // NOI18N

        getNodeValues(root, mapValues);
        return mapValues;
    }

    private static void getNodeValues(Object nodeObject, List<Instance>nodeValues) {
        if (nodeObject instanceof Instance) {
            Instance node = (Instance) nodeObject;
            JavaClass nodeClass = node.getJavaClass();
            Object entries = node.getValueOfField("entries");  // NOI18N

            if (entries instanceof ObjectArrayInstance) {
                ObjectArrayInstance table = (ObjectArrayInstance) entries;

                for (Object el : table.getValues()) {
                    Instance entry = (Instance) el;

                    if (entry.getJavaClass().equals(nodeClass)) {
                        getNodeValues(entry, nodeValues);
                    } else {
                        Object value = entry.getValueOfField("value");  // NOI18N
                        if (value instanceof Instance) {
                            nodeValues.add((Instance) value);
                        }
                    }
                }
            }
        }
    }

    private static String getShortInstanceId(Instance instance) {
        if (instance == null) {
            return "null";  // NOI18N
        }
        String name = instance.getJavaClass().getName();
        int last = name.lastIndexOf('.');

        if (last != -1) {
            name = name.substring(last + 1);
        }
        return name + "#" + instance.getInstanceNumber();   // NOI18N
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

    public static boolean isDynamicObject(Instance dynObj) {
        return isSubClassOf(dynObj, DYNAMIC_OBJECT_FQN);
    }

    public static boolean hasDynamicObject(Heap heap) {
        JavaClass dynClass = heap.getJavaClassByName(DYNAMIC_OBJECT_FQN);

        return dynClass != null;
    }

    private boolean isLocationObjSubClass(Instance dynObj) {
        return isSubClassOf(dynObj, LOCATION_FQN);
    }


    private class Property implements Field {

        Instance property;
        Instance location;
        String propertyName;

        private Property(Instance p) {
            assert p.getJavaClass().getName().equals(PROPERTY_FQN);
            property = p;
            propertyName = DetailsUtils.getInstanceString(p, null);
            location = (Instance) property.getValueOfField("location"); // NOI18N
        }

        String getPropertyName() {
            return propertyName;
        }

        FieldValue getValue(Instance dynamicObject) {
            return getValueImpl(location, dynamicObject);
        }

        FieldValue getValueImpl(Instance loc, final Instance dynamicObject) {
            JavaClass locClass = loc.getJavaClass();
            final String className = locClass.getName();

            if (className.contains("Constant")) {   // NOI18N
                return getInstanceFieldValue(dynamicObject, loc, "value");  // NOI18N
            }
            if (className.contains("Declared")) {   // NOI18N
                return getInstanceFieldValue(dynamicObject, loc, "value");  // NOI18N
            }
            if (className.startsWith(ENTERPRISE_PACKAGE)) {
                FieldValue val = getEnterpriseValue(loc,className,dynamicObject);

                if (val != null) {
                    return val;
                }
            } else {
                if (className.endsWith("Decorator")) {  // NOI18N
                    Instance actualLoc = (Instance) loc.getValueOfField("longLocation");    // NOI18N
                    FieldValue longValue = getValueImpl(actualLoc, dynamicObject);
                    String valString = longValue.getValue();

                    if (className.contains("DoubleLocation")) { // NOI18N
                        Double d = Double.longBitsToDouble(Long.parseLong(valString));
                        valString = d.toString();
                    }
                    if (className.contains("BooleanLocation")) {    // NOI18N
                        valString = Boolean.toString(Long.parseLong(valString) != 0);
                    }
                    return getFieldValue(dynamicObject, valString);
                }
                if (className.contains("ObjectArrayLocation")) {    // NOI18N
                    Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                    // Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");
                    ObjectArrayInstance arr = (ObjectArrayInstance) dynamicObject.getValueOfField("objext");    // NOI18N

                    return getObjectFieldValue(dynamicObject, (Instance) arr.getValues().get(index));
                }
                if (className.contains("LongArrayLocation")) {  // NOI18N
                    Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                    // Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");
                    PrimitiveArrayInstance arr = (PrimitiveArrayInstance) dynamicObject.getValueOfField("primext");

                    return getFieldValue(dynamicObject, (String) arr.getValues().get(index));
                }
            }
            String superClassName = locClass.getSuperClass().getName();
            if (superClassName.contains("SimpleObjectFieldLocation")) { // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getInstanceFieldValue(dynamicObject, "object" + (index + 1));    // NOI18N
            }
            if (superClassName.contains("SimpleLongFieldLocation")) {   // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getInstanceFieldValue(dynamicObject, "primitive" + (index + 1)); // NOI18N
            }
            return new FieldValue() {
                @Override
                public Field getField() {
                    return Property.this;
                }

                @Override
                public String getValue() {
                    return "Not implemented for " + className; // NOI18N
                }

                @Override
                public Instance getDefiningInstance() {
                    return dynamicObject;
                }
            };
        }

        private FieldValue getInstanceFieldValue(Instance dynObj, String fieldName) {
            return getInstanceFieldValue(dynObj, dynObj, fieldName);
        }

        private FieldValue getInstanceFieldValue(Instance dynObj, Instance i, String fieldName) {
            for (Object f : i.getFieldValues()) {
                FieldValue fieldValue = (FieldValue) f;

                if (fieldValue.getField().getName().equals(fieldName)) {
                    return createFieldValue(dynObj, fieldValue);
                }
            }
            return null;
        }

        @Override
        public boolean isStatic() {
            String locationClassName = location.getJavaClass().getName();
            return locationClassName.contains("Constant") || locationClassName.contains("Declared");  // NOI18N
        }

        @Override
        public JavaClass getDeclaringClass() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public String getName() {
            return propertyName;
        }

        @Override
        public Type getType() {
            String locationClassName = location.getJavaClass().getName();
            if (locationClassName.contains("Object")) {  // NOI18N
                return ObjType.OBJECT;
            }
            if (locationClassName.contains("Boolean")) {  // NOI18N
                return PType.BOOLEAN;
            }
            if (locationClassName.contains("Byte")) {  // NOI18N
                return PType.BYTE;
            }
            if (locationClassName.contains("Char")) {  // NOI18N
                return PType.CHAR;
            }
            if (locationClassName.contains("Double")) {  // NOI18N
                return PType.DOUBLE;
            }
            if (locationClassName.contains("Float")) {  // NOI18N
                return PType.FLOAT;
            }
            if (locationClassName.contains("Int")) {  // NOI18N
                return PType.INT;
            }
            if (locationClassName.contains("Long")) {  // NOI18N
                return PType.LONG;
            }
            if (locationClassName.contains("Short")) {  // NOI18N
                return PType.SHORT;
            }
            return ObjType.OBJECT;
            // throw new IllegalArgumentException(locationClassName);
        }

        private FieldValue createFieldValue(final Instance i, final FieldValue fieldValue) {
            if (fieldValue instanceof ObjectFieldValue) {
                return new ObjectFieldValue() {
                    @Override
                    public Instance getInstance() {
                        return ((ObjectFieldValue) fieldValue).getInstance();
                    }

                    @Override
                    public Field getField() {
                        return Property.this;
                    }

                    @Override
                    public String getValue() {
                        return fieldValue.getValue();
                    }

                    @Override
                    public Instance getDefiningInstance() {
                        return i;
                    }
                };
            }
            return new FieldValue() {
                @Override
                public Field getField() {
                    return Property.this;
                }

                @Override
                public String getValue() {
                    return fieldValue.getValue();
                }

                @Override
                public Instance getDefiningInstance() {
                    return i;
                }
            };
        }

        private FieldValue getFieldValue(final Instance dynamicObject, final String value) {
            return new FieldValue() {
                @Override
                public Field getField() {
                    return Property.this;
                }

                @Override
                public String getValue() {
                    return value;
                }

                @Override
                public Instance getDefiningInstance() {
                    return dynamicObject;
                }
            };
        }

        private ObjectFieldValue getObjectFieldValue(final Instance dynamicObject, final Instance value) {
            return new ObjectFieldValue() {
                @Override
                public Instance getInstance() {
                    return value;
                }

                @Override
                public Field getField() {
                    return Property.this;
                }

                @Override
                public String getValue() {
                    return String.valueOf(value.getInstanceId());
                }

                @Override
                public Instance getDefiningInstance() {
                    return dynamicObject;
                }
            };
        }

        private FieldValue getEnterpriseValue(Instance loc, String className, Instance dynamicObject) {
            if (className.length()-ENTERPRISE_PACKAGE.length() < 5) {   // obfuscated enterprise classes
                return getObfuscatedEnperpriseValue(loc, className, dynamicObject);
            }
            if (className.endsWith("Decorator")) {  // NOI18N
                Instance actualLoc = (Instance) loc.getValueOfField("actualLocation");  // NOI18N
                return getValueImpl(actualLoc, dynamicObject);
            }
            if (className.contains("ObjectFieldLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getInstanceFieldValue(dynamicObject, "object" + (index + 2));
            }
            if (className.contains("IntFieldLocation")) {
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getInstanceFieldValue(dynamicObject, "primitive" + (index + 1)); // NOI18N
            }
            if (className.contains("BooleanFieldLocation")) {   // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Integer i1 = (Integer) dynamicObject.getValueOfField("primitive" + (index + 1));    // NOI18N
                return getFieldValue(dynamicObject, Boolean.toString(i1.intValue() != 0));
            }
            if (className.contains("DoubleFieldLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Integer i1 = (Integer) dynamicObject.getValueOfField("primitive" + (index + 1));    // NOI18N
                Integer i2 = (Integer) dynamicObject.getValueOfField("primitive" + (index + 2));    // NOI18N
                Double d = Double.longBitsToDouble(i2.longValue()<<32 | (i1.longValue() & 0xFFFFFFFFL));
                return getFieldValue(dynamicObject, Double.toString(d));
            }
            if (className.contains("LongFieldLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Integer i1 = (Integer) dynamicObject.getValueOfField("primitive" + (index + 1));    // NOI18N
                Integer i2 = (Integer) dynamicObject.getValueOfField("primitive" + (index + 2));    // NOI18N
                long l = i2.longValue()<<32 | (i1.longValue() & 0xFFFFFFFFL);
                return getFieldValue(dynamicObject, Long.toString(l));
            }
            if (className.contains("ObjectArrayLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                ObjectArrayInstance arr = (ObjectArrayInstance) dynamicObject.getValueOfField("object1");   // NOI18N
                return getObjectFieldValue(dynamicObject, (Instance) arr.getValues().get(index));
            }
            if (className.contains("IntArrayLocation")) {   // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");   // NOI18N
                ObjectFieldValue arrayVal = (ObjectFieldValue) getValueImpl(actualLoc, dynamicObject);
                PrimitiveArrayInstance arr = (PrimitiveArrayInstance) arrayVal.getInstance();
                return getFieldValue(dynamicObject, (String) arr.getValues().get(index));
            }
            if (className.contains("DoubleArrayLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");   // NOI18N
                ObjectFieldValue arrayVal = (ObjectFieldValue) getValueImpl(actualLoc, dynamicObject);
                PrimitiveArrayInstance arr = (PrimitiveArrayInstance) arrayVal.getInstance();
                long i1 = Integer.valueOf((String) arr.getValues().get(index)) & 0xFFFFFFFF;
                long i2 = Integer.valueOf((String) arr.getValues().get(index+1)) & 0xFFFFFFFF;
                Double d = Double.longBitsToDouble(i2<<32 | i1);
                return getFieldValue(dynamicObject, Double.toString(d));
            }
            if (className.contains("LongArrayLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");   // NOI18N
                ObjectFieldValue arrayVal = (ObjectFieldValue) getValueImpl(actualLoc, dynamicObject);
                PrimitiveArrayInstance arr = (PrimitiveArrayInstance) arrayVal.getInstance();
                long i1 = Integer.valueOf((String) arr.getValues().get(index)) & 0xFFFFFFFF;
                long i2 = Integer.valueOf((String) arr.getValues().get(index+1)) & 0xFFFFFFFF;
                long l = i2<<32 | i1;
                return getFieldValue(dynamicObject, Long.toString(l));
            }
            return null;
        }

        private FieldValue getObfuscatedEnperpriseValue(Instance loc, String className, Instance dynamicObject) {
            List fields = loc.getFieldValues();

            if (fields.size() == 2) {
                FieldValue v0 = (FieldValue) fields.get(0);
                FieldValue v1 = (FieldValue) fields.get(1);
                Type t0 = v0.getField().getType();
                Type t1 = v1.getField().getType();

                if (t0.getName().equals("object") && t1.getName().equals("object")) { // Decorator  // NOI18N
                    Instance i0 = ((ObjectFieldValue)v0).getInstance();
                    Instance i1 = ((ObjectFieldValue)v1).getInstance();

                    if (isLocationObjSubClass(i0)) {
                        return getValueImpl(i0, dynamicObject);
                    }
                    if (isLocationObjSubClass(i1)) {
                        return getValueImpl(i1, dynamicObject);
                    }
                }
            }
            if (loc.getValueOfField("arrayLocation") != null && loc.getValueOfField("index") != null) { // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");   // NOI18N
                ObjectFieldValue arrayVal = (ObjectFieldValue) getValueImpl(actualLoc, dynamicObject);
                Instance array = arrayVal.getInstance();
                if (array instanceof PrimitiveArrayInstance) {
                    PrimitiveArrayInstance arr = (PrimitiveArrayInstance) array;
                    if (loc.getValueOfField("allowInt") != null) {  // NOI18N
                        // long, double
                        long i1 = Integer.valueOf((String) arr.getValues().get(index)) & 0xFFFFFFFF;
                        long i2 = Integer.valueOf((String) arr.getValues().get(index+1)) & 0xFFFFFFFF;
                        Double d = Double.longBitsToDouble(i2<<32 | i1);
                        return getFieldValue(dynamicObject, Double.toString(d));
                    }
                    return getFieldValue(dynamicObject, (String) arr.getValues().get(index));
                }
                if (array instanceof ObjectArrayInstance) {
                    ObjectArrayInstance arr = (ObjectArrayInstance) array;
                    return getObjectFieldValue(dynamicObject, (Instance) arr.getValues().get(index));
                }
            }
            if (loc.getValueOfField("index") != null && loc.getValueOfField("offset") != null) {    // NOI18N
                if (loc.getValueOfField("type") != null) {   // TypedObjectFieldLocation
                    Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                    if (index.intValue() == 0) { // test for type Object[]
                        ObjectFieldValue val = (ObjectFieldValue) getDynamicObjectField(dynamicObject, index+1);
                        Instance type = (Instance) loc.getValueOfField("type"); // NOI18N
                        if (val.getInstance() != null && val.getInstance().getJavaClass().getJavaClassId() == type.getInstanceId()) {
                            return val;
                        }
                        return getDynamicObjectField(dynamicObject, index);

                    }
                    return getDynamicObjectField(dynamicObject, index+1);
                }
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                if (loc.getFieldValues().size() > 2) {
                    if (loc.getValueOfField("allowInt") != null) {  // NOI18N
                        // primitive FieldLocation, long double
                        FieldValue fv1 = getDynamicObjectPrimitiveField(dynamicObject, index);
                        FieldValue fv2 = getDynamicObjectPrimitiveField(dynamicObject, index+1);
                        long i1 = Integer.valueOf(fv1.getValue()) & 0xFFFFFFFF;
                        long i2 = Integer.valueOf(fv2.getValue()) & 0xFFFFFFFF;
                        Double d = Double.longBitsToDouble(i2<<32 | i1);
                        return getFieldValue(dynamicObject, Double.toString(d));
                    }
                    // ObjectFieldLocation without type
                    return getDynamicObjectField(dynamicObject, index+1);
                }
                // primitive FieldLocation
                return getDynamicObjectPrimitiveField(dynamicObject, index);
            }
            return null;
        }

        private FieldValue getDynamicObjectPrimitiveField(Instance dynamicObject, int index) {
            return getDynamicObjectField(dynamicObject, index, false);
        }

        private FieldValue getDynamicObjectField(Instance dynamicObject, int index) {
            return getDynamicObjectField(dynamicObject, index, true);
        }

        private FieldValue getDynamicObjectField(Instance dynamicObject, int index, boolean objectType) {
            List fields = dynamicObject.getJavaClass().getFields();

            for (int i = fields.size()-1; i>=0; i--) {
                Field f = (Field) fields.get(i);

                if (f.getType().getName().equals("object") == objectType) { // NOI18N
                    if (index == 0) {
                        return createFieldValue(dynamicObject, getValueOfField(dynamicObject, f));
                    }
                    index--;
                }
            }
            throw new IllegalArgumentException();
        }

        private FieldValue getValueOfField(Instance dynamicObject, Field field) {
             List fieldVals = dynamicObject.getFieldValues();

             for (int i=0; i<fieldVals.size(); i++) {
                 FieldValue fv = (FieldValue) fieldVals.get(i);

                 if (fv.getField().equals(field)) {
                     return fv;
                 }
             }
             throw new IllegalArgumentException(field.getName());
        }
    }

    private static class ObjType implements Type {

        static final Type OBJECT = new ObjType();

        @Override
        public String getName() {
            return "object";    // NOI18N
        }

    }

    private static class PType implements PrimitiveType {

        static final PrimitiveType BOOLEAN = new PType("boolean"); //NOI18N
        static final PrimitiveType CHAR = new PType("char"); //NOI18N
        static final PrimitiveType FLOAT = new PType("float"); //NOI18N
        static final PrimitiveType DOUBLE = new PType("double"); //NOI18N
        static final PrimitiveType BYTE = new PType("byte"); //NOI18N
        static final PrimitiveType SHORT = new PType("short"); //NOI18N
        static final PrimitiveType INT = new PType("int"); //NOI18N
        static final PrimitiveType LONG = new PType("long"); //NOI18N

        private String name;

        PType(String n) {
            name = n;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}


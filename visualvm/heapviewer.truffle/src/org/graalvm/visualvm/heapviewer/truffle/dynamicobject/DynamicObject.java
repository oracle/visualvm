/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.dynamicobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveType;
import org.graalvm.visualvm.lib.jfluid.heap.Type;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class DynamicObject extends TruffleObject.InstanceBased {

    static final String DYNAMIC_OBJECT_FQN = "com.oracle.truffle.api.object.DynamicObject"; // NOI18N
    private static final String LOCATION_FQN = "com.oracle.truffle.api.object.Location"; // NOI18N
    private static final String ENTERPRISE_PACKAGE = "com.oracle.truffle.object.enterprise"; // NOI18N
    private static final String ENTERPRISE_LOCATION_TOP_CLASS = ENTERPRISE_PACKAGE+".EnterpriseLocations"; // NOI18N
    private static final String ENTERPRISE_FIELD_LOCATION_FQN = ENTERPRISE_LOCATION_TOP_CLASS+"$FieldLocation"; // NOI18N
    private static final String PROPERTY_MAP_FQN = "com.oracle.truffle.object.ConsListPropertyMap"; // NOI18N
    private static final String TRIE_PROPERTY_MAP_FQN = "com.oracle.truffle.object.TriePropertyMap"; // NOI18N
    private static final String PROPERTY_FQN = "com.oracle.truffle.object.PropertyImpl"; // NOI18N
    private static final String OBJECT_TYPE_FQN = "com.oracle.truffle.api.object.ObjectType"; // NOI18N

    private final Instance instance;
    
    private Instance shape;
    private String type;
    
    private long size = -1;
    
//    private List<Property> properties;
    private List<FieldValue> values;
    private List<FieldValue> staticValues;

    public DynamicObject(Instance instance) {
        this(null, instance);
    }
    
    public DynamicObject(String type, Instance instance) {
        if (instance == null) throw new IllegalArgumentException("Instance cannot be null"); // NOI18N
        
        this.instance = instance;
        this.type = type;
    }
    
    @Override
    public Instance getInstance() {
        return instance;
    }
    
    public List<FieldValue> getReferences() {
        List<FieldValue> dynObjRefs = new ArrayList<>();

        if (getShape() != null) {
            List<Value> refs = instance.getReferences();
            Set<Instance> foundRefs = new HashSet<>();

            for (Value ref : refs) {
                Instance instanceRef = ref.getDefiningInstance();
                if (ref instanceof ObjectFieldValue) {
                    if (foundRefs.add(instanceRef)) {
                        addReferences(instanceRef, dynObjRefs);
                    }
                }
                if (ref instanceof ArrayItemValue) {
                    List<Value> arrRefs = instanceRef.getReferences();

                    for (Value arrRef : arrRefs) {
                        Instance arrInstanceRef = arrRef.getDefiningInstance();
                        if (foundRefs.add(arrInstanceRef)) {
                            addReferences(instanceRef, arrInstanceRef, dynObjRefs);
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
        if (shape == null) shape = getShape(instance);
        return shape;
    }
    
    public static Instance getShape(Instance instance) {
        return (Instance)instance.getValueOfField("shape"); // NOI18N
    }
    
    @Override
    public String getType() {
        if (type == null) {
            type = computeType();
            if (type == null) type = "<unknown type>"; // NOI18N
        }
        return type;
    }

    @Override
    public long getTypeId() {
        return getShape().getInstanceId();
    }
    
    protected String computeType() {
        return DetailsSupport.getDetailsString(getShape());
    }
    
    public static String getType(Instance instance) {
        Instance shape = getShape(instance);
        return DetailsSupport.getDetailsString(shape);
    }
    
    @Override
    public long getSize() {
        if (size == -1) {
            size = instance.getSize();

            for (Object fv : instance.getFieldValues()) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;
                    Instance value = ofv.getInstance();

                    if (value instanceof ObjectArrayInstance) {
                        size += value.getSize();
                    }
                    if (value instanceof PrimitiveArrayInstance) {
                        size += value.getSize();
                    }
                }
            }
        }
        return size;
    }
    
    @Override
    public long getRetainedSize() {
        return instance.getRetainedSize();
    }
    
    public JavaClass getLanguageId() {
        return getLanguageIdFromShape(getShape());
    }
    
    public static JavaClass getLanguageId(Instance instance) {
        return getLanguageIdFromShape(getShape(instance));
    }
    
    
    
    private void initFields() {
        Instance propertyMap = getValueofFields(instance, "shape", "fastMapRef", "referent"); // NOI18N
        if (propertyMap == null) propertyMap = getValueofFields(instance, "shape", "propertyMap"); // NOI18N
        if (propertyMap != null) {
//            properties = new ArrayList();
            values = new ArrayList<>();
            staticValues = new ArrayList<>();
            boolean hasExtRef = hasField(instance.getJavaClass(), "extRef");    // NOI18N
            boolean hasShortNames = hasField(instance.getJavaClass(), "o0");    // NOI18N

            for (Instance ip : getMapValues(propertyMap)) {
                Property p = new Property(ip, hasExtRef, hasShortNames);
//                properties.add(p);
                if (p.isStatic()) staticValues.add(p.getValue(instance));
                else values.add(p.getValue(instance));
            }
        } else {
            values = Collections.EMPTY_LIST;
            staticValues = Collections.EMPTY_LIST;
        }
    }

    private void addReferences(Instance instanceRef, List dynObjRefs) {
        addReferences(null, instanceRef, dynObjRefs);
    }

    private void addReferences(Instance baseInstance, Instance instanceRef, List dynObjRefs) {
        if (DynamicObject.isDynamicObject(instanceRef)) {
            DynamicObject dynObj = new DynamicObject(instanceRef);

            List<FieldValue> fieldValues = dynObj.getFieldValues();
            for (FieldValue fieldVal : fieldValues) {
                if (fieldVal instanceof ObjectFieldValue) {
                    ObjectFieldValue fieldValObj = (ObjectFieldValue) fieldVal;

                    if (instance.equals(fieldValObj.getInstance())) {
                        dynObjRefs.add(fieldVal);
                    }
                    if (baseInstance != null && baseInstance.equals(fieldValObj.getInstance())) {
                        dynObjRefs.add(fieldVal);
                    }
                }
            }
        }
    }

    private boolean hasField(JavaClass jcls, String name) {
        List<Field> fields = jcls.getFields();

        for (int i = fields.size()-1; i>=0; i--) {
            Field f = fields.get(i);

            if (f.getName().equals(name)) {
                return true;
            }
        }
        jcls = jcls.getSuperClass();
        if (jcls != null) {
            return hasField(jcls, name);
        }
        return false;
    }

    static JavaClass getLanguageIdFromShape(Instance sh) {
        if (sh != null) {
            Instance objectType = (Instance) sh.getValueOfField("objectType"); // NOI18N
            if (objectType != null) {
                JavaClass objTypeCls = objectType.getJavaClass();

                while (objTypeCls != null) {
                    JavaClass superObjType = objTypeCls.getSuperClass();

                    if (superObjType == null
                       || OBJECT_TYPE_FQN.equals(superObjType.getName())
                       || Object.class.getName().equals(superObjType.getName())) {
                        return objTypeCls;
                    }
                    objTypeCls = superObjType;
                }
            }
        }
        return null;
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
        List<Instance> mapValues = new ArrayList<>();
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
        List<Instance> mapValues = new ArrayList<>();

        for (Instance node = propertyMap; node != null; node = (Instance) node.getValueOfField("car")) {    // NOI18N
            Object value = node.getValueOfField("cdr"); // NOI18N

            if (value != null && value instanceof Instance) {
                mapValues.add((Instance) value);
            }
        }
        return mapValues;
    }

    private static List<Instance> getTrieValues(Instance propertyMap) {
        List<Instance> mapValues = new ArrayList<>();
        Object root = propertyMap.getValueOfField("root");  // NOI18N

        getNodeValues(root, mapValues);
        return mapValues;
    }

    private static boolean getNodeValues(Object nodeObject, List<Instance>nodeValues) {
        if (nodeObject instanceof Instance) {
            Instance node = (Instance) nodeObject;
            Object entries = node.getValueOfField("entries");  // NOI18N

            if (entries instanceof ObjectArrayInstance) {
                ObjectArrayInstance table = (ObjectArrayInstance) entries;

                for (Instance entry : table.getValues()) {
                    if (!getNodeValues(entry, nodeValues)) {
                        Object value = entry.getValueOfField("value");  // NOI18N
                        if (value instanceof Instance) {
                            nodeValues.add((Instance) value);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static String getShortInstanceId(Instance instance) {
        if (instance == null) {
            return "null";  // NOI18N
        }
        String name = instance.getJavaClass().getName();
        int last = name.lastIndexOf('.'); // NOI18N

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

    private static boolean isLocationObjSubClass(Instance dynObj) {
        return isSubClassOf(dynObj, LOCATION_FQN);
    }

    private static boolean isEterpriseFieldLocationObjSubClass(Instance dynObj) {
        return isSubClassOf(dynObj, ENTERPRISE_FIELD_LOCATION_FQN);
    }

    private static class Property implements Field {

        Instance property;
        Instance location;
        String propertyName;
        boolean isStatic;
        boolean hasExtRef;
        boolean hasShortNames;

        private Property(Instance p, boolean extRef, boolean shortNames) {
            assert p.getJavaClass().getName().equals(PROPERTY_FQN);
            property = p;
            propertyName = DetailsUtils.getInstanceString(p);
            location = (Instance) property.getValueOfField("location"); // NOI18N
            hasExtRef = extRef;
            hasShortNames = shortNames;
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
                isStatic = true;
                return getInstanceFieldValue(dynamicObject, loc, "value");  // NOI18N
            }
            if (className.contains("Declared")) {   // NOI18N
                isStatic = true;
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
                    ObjectArrayInstance arr = getObjectStore(dynamicObject);

                    return getObjectFieldValue(dynamicObject, arr.getValues().get(index));
                }
                if (className.contains("LongArrayLocation")) {  // NOI18N
                    Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                    // Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");
                    PrimitiveArrayInstance arr;
                    if (hasExtRef) {
                        // extVal is int[]
                        arr = (PrimitiveArrayInstance) dynamicObject.getValueOfField("extVal"); // NOI18N
                        return getFieldValue(dynamicObject, Long.toString(getLong(arr, index)));
                    } else {
                        // primext is long[]
                        arr = (PrimitiveArrayInstance) dynamicObject.getValueOfField("primext"); // NOI18N
                        return getFieldValue(dynamicObject, arr.getValues().get(index));
                    }
                }
            }
            String superClassName = locClass.getSuperClass().getName();
            if (superClassName.contains("SimpleObjectFieldLocation")) { // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getObjectInstanceFieldValue(dynamicObject, index);
            }
            if (superClassName.contains("SimpleLongFieldLocation")) {   // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getPrimitiveInstanceFieldValue(dynamicObject, index);
            }
            if (superClassName.contains("BasicObjectFieldLocation")) { // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getObjectInstanceFieldValue(dynamicObject, index);
            }
            if (superClassName.contains("BasicLongFieldLocation")) {   // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                return getPrimitiveInstanceFieldValue(dynamicObject,index);
            }
            return new DynObjFieldValue(dynamicObject, this) {
                @Override
                public String getValue() {
                    return "Not implemented for " + className; // NOI18N
                }
            };
        }

        private ObjectArrayInstance getObjectStore(final Instance dynamicObject) {
            String fieldName = hasExtRef ? "extRef" : "objext"; // NOI18N

            return (ObjectArrayInstance) dynamicObject.getValueOfField(fieldName);
        }

        private FieldValue getObjectInstanceFieldValue(Instance dynObj, int index) {
            String fieldName = getObjectFieldName(index);
            return getInstanceFieldValue(dynObj, dynObj, fieldName);
        }

        private FieldValue getPrimitiveInstanceFieldValue(Instance dynObj, int index) {
            String fieldName = getPrimitiveFieldName(index);
            return getInstanceFieldValue(dynObj, dynObj, fieldName);
        }

        private FieldValue getInstanceFieldValue(Instance dynObj, String fieldName) {
            return getInstanceFieldValue(dynObj, dynObj, fieldName);
        }

        private FieldValue getInstanceFieldValue(Instance dynObj, Instance i, String fieldName) {
            for (FieldValue fieldValue : i.getFieldValues()) {

                if (fieldValue.getField().getName().equals(fieldName)) {
                    return createFieldValue(dynObj, fieldValue);
                }
            }
            return null;
        }

        @Override
        public boolean isStatic() {
            return isStatic;
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

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Property) {
                Property p = (Property) obj;

                return property.equals(p.property);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return property.hashCode();
        }

        private FieldValue createFieldValue(final Instance i, final FieldValue fieldValue) {
            if (fieldValue instanceof ObjectFieldValue) {
                return new DynObjObjectFieldValue(i, this) {
                    @Override
                    public Instance getInstance() {
                        return ((ObjectFieldValue) fieldValue).getInstance();
                    }
                    @Override
                    public String getValue() {
                        return fieldValue.getValue();
                    }
                };
            }
            return new DynObjFieldValue(i, this) {
                @Override
                public String getValue() {
                    return fieldValue.getValue();
                }
            };
        }

        private FieldValue getFieldValue(final Instance dynamicObject, final String value) {
            return new DynObjFieldValue(dynamicObject, this) {
                @Override
                public String getValue() {
                    return value;
                }
            };
        }

        private ObjectFieldValue getObjectFieldValue(final Instance dynamicObject, final Instance value) {
            return new DynObjObjectFieldValue(dynamicObject, this) {
                @Override
                public Instance getInstance() {
                    return value;
                }

                @Override
                public String getValue() {
                    return String.valueOf(value.getInstanceId());
                }
            };
        }

        private FieldValue getEnterpriseValue(Instance loc, String className, Instance dynamicObject) {
            if (className.length()-ENTERPRISE_PACKAGE.length() < 5) {   // obfuscated enterprise classes
                return getObfuscatedEnperpriseValue(loc, className, dynamicObject);
            }
            if (className.length()-ENTERPRISE_LOCATION_TOP_CLASS.length() < 4) { // obfuscated enterprise inner classes
                return getObfuscatedEnperpriseValue(loc, className, dynamicObject);
            }
            if (className.endsWith("Decorator")) {  // NOI18N
                Instance actualLoc = (Instance) loc.getValueOfField("actualLocation");  // NOI18N
                return getValueImpl(actualLoc, dynamicObject);
            }
            if (className.contains("ObjectFieldLocation")               // NOI18N
                || className.contains("ExtensionArrayLocation")) {      // NOI18N
                String fName = getEnterpriseObjectFieldName(loc);
                FieldValue ret = getInstanceFieldValue(dynamicObject, fName);
                if (ret == null) {
                    // extVal is encoded as non-existing index
                    return getObjectFieldValue(dynamicObject, (Instance) dynamicObject.getValueOfField("extVal"));  // NOI18N
                }
                return ret;
            }
            if (className.contains("IntFieldLocation")) { // NOI18N
                String fName = getEnterprisePrimitiveFieldName(loc);
                return getInstanceFieldValue(dynamicObject, fName);
            }
            if (className.contains("BooleanFieldLocation")) {   // NOI18N
                String fName = getEnterprisePrimitiveFieldName(loc);
                Integer i1 = (Integer) dynamicObject.getValueOfField(fName);
                return getFieldValue(dynamicObject, Boolean.toString(i1.intValue() != 0));
            }
            if (className.contains("DoubleFieldLocation")        // NOI18N
                || className.contains("LongFieldLocation")) {    // NOI18N
                String fName = getEnterprisePrimitiveFieldName(loc);
                Number i1 = (Number) dynamicObject.getValueOfField(fName);
                long val;
                String valString;

                if (i1 instanceof Long) {
                    val = i1.longValue();
                } else {
                    Integer i2 = (Integer) getValueOfNextField(dynamicObject, fName);
                    val = getLong(i1.intValue(), i2);
                }
                if (className.contains("LongFieldLocation")) {      // NOI18N
                    valString = Long.toString(val);
                } else {
                    valString = getDouble(val);
                }
                return getFieldValue(dynamicObject, valString);
            }
            if (className.contains("ObjectArrayLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                ObjectArrayInstance arr = getEnterpriseObjectStore(dynamicObject);
                return getObjectFieldValue(dynamicObject, arr.getValues().get(index));
            }
            if (className.contains("IntArrayLocation")) {   // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                PrimitiveArrayInstance arr = getPrimitiveArray(loc, dynamicObject);
                return getFieldValue(dynamicObject, arr.getValues().get(index));
            }
            if (className.contains("DoubleArrayLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                PrimitiveArrayInstance arr = getPrimitiveArray(loc, dynamicObject);
                return getFieldValue(dynamicObject, getDouble(getLong(arr, index)));
            }
            if (className.contains("LongArrayLocation")) {    // NOI18N
                Integer index = (Integer) loc.getValueOfField("index"); // NOI18N
                PrimitiveArrayInstance arr = getPrimitiveArray(loc, dynamicObject);
                return getFieldValue(dynamicObject, Long.toString(getLong(arr, index)));
            }
            return null;
        }

        private PrimitiveArrayInstance getPrimitiveArray(Instance loc, Instance dynamicObject) {
            Instance actualLoc = (Instance) loc.getValueOfField("arrayLocation");   // NOI18N
            PrimitiveArrayInstance arr;
            if (actualLoc == null) {
                arr =  (PrimitiveArrayInstance) dynamicObject.getValueOfField("extVal");    // NOI18N
            } else {
                ObjectFieldValue arrayVal = (ObjectFieldValue) getValueImpl(actualLoc, dynamicObject);
                arr = (PrimitiveArrayInstance)arrayVal.getInstance();
            }
            return arr;
        }

        private Object getValueOfNextField(Instance dynamicObject, String fieldName) {
            int i = fieldName.length()-1;
            for (; i>=0 && Character.isDigit(fieldName.charAt(i)); i--);
            assert i < fieldName.length()-1 : "Invalid fname "+fieldName;
            int fIndex = Integer.parseInt(fieldName.substring(++i));

            return dynamicObject.getValueOfField(fieldName.substring(0,i)+(fIndex+1));
        }

        private String getEnterpriseObjectFieldName(Instance location) {
            String fName = getEnterpriseFieldNameFromFieldInfo(location);
            if (fName == null) {
                Integer index = (Integer) location.getValueOfField("index"); // NOI18N
                int objectIndex = hasExtRef ? index : index + 2;
                String prefix = hasShortNames ? "o" : "object";         // NOI18N
                fName = prefix + objectIndex;
            }
            return fName;
        }

        private String getEnterprisePrimitiveFieldName(Instance location) {
            String fName = getEnterpriseFieldNameFromFieldInfo(location);
            if (fName == null) {
                Integer index = (Integer) location.getValueOfField("index"); // NOI18N
                int objectIndex = hasExtRef ? index : index + 1;
                String prefix = hasShortNames ? "p" : "primitive";      // NOI18N
                fName = prefix + objectIndex;
            }
            return fName;
        }

        private String getEnterpriseFieldNameFromFieldInfo(Instance location) {
            Object fieldInfo = location.getValueOfField("field");     // NOI18N
            if (fieldInfo instanceof Instance) {
                for (Object fv : ((Instance)fieldInfo).getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        Instance ifv = ((ObjectFieldValue)fv).getInstance();
                        if (String.class.getName().equals(ifv.getJavaClass().getName())) {
                            return DetailsSupport.getDetailsString(ifv);
                        }
                    }
                }
            }
            return null;
        }

        private ObjectArrayInstance getEnterpriseObjectStore(Instance dynamicObject) {
            String fieldName = hasExtRef ? "extRef" : "object1"; // NOI18N

            return (ObjectArrayInstance) dynamicObject.getValueOfField(fieldName);
        }

        private FieldValue getObfuscatedEnperpriseValue(Instance loc, String className, Instance dynamicObject) {
            List<FieldValue> fields = loc.getFieldValues();

            if (fields.size() == 2) {
                FieldValue v0 = fields.get(0);
                FieldValue v1 = fields.get(1);
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
                Boolean allowInt = (Boolean) loc.getValueOfField("allowInt");   // NOI18N

                return getObfuscatedEnterpriseArrayLocation(dynamicObject, index, actualLoc, allowInt);
            }
            if (loc.getValueOfField("index") != null && loc.getValueOfField("offset") != null) {    // NOI18N
                Integer index = (Integer)loc.getValueOfField("index");   // NOI18N
                Instance type = (Instance)loc.getValueOfField("type");   // NOI18N
                Boolean allowInt = (Boolean) loc.getValueOfField("allowInt");   // NOI18N

                return getObfuscatedEnterpriseFieldLocation(dynamicObject, loc, index, type, allowInt);
            }
            if (isEterpriseFieldLocationObjSubClass(loc) && loc.getValueOfField("offset") != null) {   // NOI18N
                Integer locIndex = null;
                Instance locType = null;
                Boolean locAllowInt = null;

                for (Object obj : fields) {
                    FieldValue fv = (FieldValue) obj;
                    Field f = fv.getField();
                    String typeName = f.getType().getName();

                    if ("object".equals(typeName) && !f.getName().equals("tclass")) {   // NOI18N
                        locType = ((ObjectFieldValue)fv).getInstance();
                    }
                    if ("boolean".equals(typeName) && fields.size()==3 && f.getDeclaringClass().getSubClasses().size()==1) {
                        locAllowInt = (Boolean) loc.getValueOfField(f.getName());
                    }
                    if ("int".equals(typeName) && !f.getName().equals("offset")) {   // NOI18N
                        locIndex = (Integer) loc.getValueOfField(f.getName());
                    }
                }
                if (locIndex != null) {
                    return getObfuscatedEnterpriseFieldLocation(dynamicObject, loc, locIndex, locType, locAllowInt);
                }
            }
            if (fields.size() >= 2) {
                // ArrayLocation
                Integer locIndex = null;
                Instance locArrayLocation = null;
                Boolean locAllowInt = null;

                for (Object obj : fields) {
                    FieldValue fv = (FieldValue) obj;
                    Field f = fv.getField();
                    String typeName = f.getType().getName();

                    if ("object".equals(typeName)) {   // NOI18N
                        Instance val = ((ObjectFieldValue)fv).getInstance();
                        if (isLocationObjSubClass(val)) {
                            locArrayLocation = val;
                        }
                    } else if ("boolean".equals(typeName) && fields.size()==3 && f.getDeclaringClass().getSubClasses().size()==2) {
                        locAllowInt = (Boolean) loc.getValueOfField(f.getName());
                    } else if ("int".equals(typeName)) {   // NOI18N
                        locIndex = (Integer) loc.getValueOfField(f.getName());
                    }
                }
                if (locIndex != null && locArrayLocation != null) {
                    return getObfuscatedEnterpriseArrayLocation(dynamicObject, locIndex, locArrayLocation, locAllowInt);
                }
            }
            if (fields.size() == 1) {
                // obfuscated static property location
                isStatic = true;
                FieldValue staticFieldVal = fields.get(0);
                return createFieldValue(dynamicObject, staticFieldVal);
            }
            return null;
        }

        private FieldValue getObfuscatedEnterpriseArrayLocation(Instance dynamicObject, Integer index, Instance actualLoc, Boolean allowInt) {
            ObjectFieldValue arrayVal = (ObjectFieldValue) getValueImpl(actualLoc, dynamicObject);
            Instance array = arrayVal.getInstance();
            if (array instanceof PrimitiveArrayInstance) {
                PrimitiveArrayInstance arr = (PrimitiveArrayInstance) array;
                if (allowInt != null) {
                    // long, double
                    return getFieldValue(dynamicObject, getDouble(getLong(arr, index)));
                }
                return getFieldValue(dynamicObject, arr.getValues().get(index));
            }
            if (array instanceof ObjectArrayInstance) {
                ObjectArrayInstance arr = (ObjectArrayInstance) array;
                return getObjectFieldValue(dynamicObject, arr.getValues().get(index));
            }
            return null;
        }

        private FieldValue getObfuscatedEnterpriseFieldLocation(Instance dynamicObject, Instance loc, Integer index, Instance type, Boolean allowInt) {
            if (type != null) { // TypedObjectFieldLocation
                if (index.intValue() == 0) { // test for type Object[]
                    long typeClassId = type.getInstanceId();  // NOI18N
                    ObjectFieldValue val = (ObjectFieldValue) getDynamicObjectField(dynamicObject, index+1);
                    Instance value = val.getInstance();
                    if (value != null) {
                        // test for the same class as type or subclasses
                        for (JavaClass valueClass = value.getJavaClass(); valueClass != null; valueClass = valueClass.getSuperClass()) {
                            if (valueClass.getJavaClassId() == typeClassId) {
                                // special case for detecting EnterpriseLayout.objectArrayLocation
                                if (isLayoutObjectArrayLocation(loc, valueClass, dynamicObject)) break;
                                return val;
                            }
                        }
                    }
                    // we should have Object[]
                    ObjectFieldValue valarr = (ObjectFieldValue) getDynamicObjectField(dynamicObject, index);
                    Instance valueArr = valarr.getInstance();
                    if (valueArr != null) {
                        // test for Object[]
                        if (valueArr.getJavaClass().getJavaClassId() == typeClassId) {
                            return valarr;
                        }
                    }
                    // fallback in case "type" is interface
                    return val;
                }
                return getDynamicObjectField(dynamicObject, index+1);
            }
            if (loc.getFieldValues().size() > 2) {
                if (allowInt != null) {
                    // primitive FieldLocation, long double
                    FieldValue fv1 = getDynamicObjectPrimitiveField(dynamicObject, index);
                    FieldValue fv2 = getDynamicObjectPrimitiveField(dynamicObject, index+1);
                    Integer i1 = Integer.valueOf(fv1.getValue());
                    Integer i2 = Integer.valueOf(fv2.getValue());
                    return getFieldValue(dynamicObject, getDouble(getLong(i1, i2)));
                }
                if (loc.getFieldValues().size() == 3 && loc.getValueOfField("tclass") != null) {
                    // primitive FieldLocation
                    return getDynamicObjectPrimitiveField(dynamicObject, index);
                }
                // ObjectFieldLocation without type
                return getDynamicObjectField(dynamicObject, index+1);
            }
            // primitive FieldLocation
            return getDynamicObjectPrimitiveField(dynamicObject, index);
        }

        private boolean isLayoutObjectArrayLocation(Instance location, JavaClass valueClass, Instance dynamicObject) {
            if (valueClass.isArray() && "java.lang.Object[]".equals(valueClass.getName())) {
                Instance layout = (Instance) DynamicObject.getShape(dynamicObject).getValueOfField("layout");
                for (Object fv : layout.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (location.equals(((ObjectFieldValue) fv).getInstance())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private FieldValue getDynamicObjectPrimitiveField(Instance dynamicObject, int index) {
            return getDynamicObjectField(dynamicObject, index, false);
        }

        private FieldValue getDynamicObjectField(Instance dynamicObject, int index) {
            return getDynamicObjectField(dynamicObject, index, true);
        }

        private FieldValue getDynamicObjectField(Instance dynamicObject, int index, boolean objectType) {
            List<Field> fields = dynamicObject.getJavaClass().getFields();

            for (int i = fields.size()-1; i>=0; i--) {
                Field f = fields.get(i);

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
             List<FieldValue> fieldVals = dynamicObject.getFieldValues();

             for (FieldValue fv : fieldVals) {

                 if (fv.getField().equals(field)) {
                     return fv;
                 }
             }
             throw new IllegalArgumentException(field.getName());
        }

        private static long getLong(Integer i1, Integer i2) {
            return i2.longValue()<<32 | (i1.longValue() & 0xFFFFFFFFL);
        }

        private static long getLong(PrimitiveArrayInstance arr, int index) {
            List<String> vals = arr.getValues();
            Integer i1 = Integer.valueOf(vals.get(index));
            Integer i2 = Integer.valueOf(vals.get(index+1));

            return getLong(i1, i2);
        }

        private static String getDouble(long l) {
            double d = Double.longBitsToDouble(l);

            return Double.toString(d);
        }

        private String getObjectFieldName(int index) {
            if (hasShortNames) {
                return "o"+(index);     // NOI18N
            }
            return "object"+(index+1);  // NOI18N
        }

        private String getPrimitiveFieldName(int index) {
            if (hasShortNames) {
                return "p"+(index); // NOI18N
            }
            return "primitive"+(index+1);   // NOI18N
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

    private abstract static class DynObjFieldValue implements FieldValue {
        Instance definingInstance;
        Property field;

        private DynObjFieldValue(Instance i, Property p) {
            definingInstance = i;
            field = p;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public Instance getDefiningInstance() {
            return definingInstance;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DynObjFieldValue) {
                DynObjFieldValue dfv = (DynObjFieldValue) obj;

                return definingInstance.equals(dfv.definingInstance)
                    && field.equals(dfv.field);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * definingInstance.hashCode() + field.hashCode();
        }
    }

    private abstract static class DynObjObjectFieldValue extends DynObjFieldValue implements ObjectFieldValue {
        private DynObjObjectFieldValue(Instance i, Property p) {
            super(i,p);
        }
    }
}


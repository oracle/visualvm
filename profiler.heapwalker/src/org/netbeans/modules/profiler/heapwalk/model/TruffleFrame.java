/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 */
package org.netbeans.modules.profiler.heapwalk.model;

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
import org.netbeans.lib.profiler.heap.PrimitiveType;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
public class TruffleFrame {

    private static final String TRUFFLE_FRAME_FQN = "com.oracle.truffle.api.impl.DefaultVirtualFrame";  // NOI18N
    private static final String COMPILER_FRAME_NOBOX_FQN = "org.graalvm.compiler.truffle.FrameWithoutBoxing"; // NOI18N
    private static final String ENT_COMPILER_FRAME_NOBOX_FQN = "com.oracle.graal.truffle.FrameWithoutBoxing"; // NOI18N
    private static final String COMPILER_FRAME_BOX_FQN = "org.graalvm.compiler.truffle.FrameWithBoxing"; // NOI18N
    private static final String ENT_COMPILER_FRAME_BOX_FQN = "com.oracle.graal.truffle.FrameWithBoxing"; // NOI18N
    private static final String ARG_PREFIX = "arg";         // NOI18N
    private static final String LOCAL_UNDEFINED = "undefined"; // NOI18N

    private static final byte OBJECT_TAG = 0;
    private static final byte ILLEGAL_TAG = 1;
    private static final byte LONG_TAG = 2;
    private static final byte INT_TAG = 3;
    private static final byte DOUBLE_TAG = 4;
    private static final byte FLOAT_TAG = 5;
    private static final byte BOOLEAN_TAG = 6;
    private static final byte BYTE_TAG = 7;

    private List<FieldValue> values;
    private boolean isTruffleFrame;

    public TruffleFrame(Instance truffleFrame) {
        values = Collections.EMPTY_LIST;
        if (isTruffleFrameSubClass(truffleFrame)) {
            List<Instance> locals = getObjectArray(truffleFrame, "locals");         // NOI18N
            List<String> primitiveLocals = getPrimitiveArray(truffleFrame, "primitiveLocals");  // NOI18N       // NOI18N
            List<Instance> arguments = getObjectArray(truffleFrame, "arguments");   // NOI18N
            Instance slotArr = getValueofFields(truffleFrame, "descriptor", "slots");   // NOI18N
            List<Instance> slots = getObjectArray(slotArr, "elementData");  // NOI18N
            Instance defaultValue = getValueofFields(truffleFrame, "descriptor", "defaultValue"); // NOI18N

            if (locals != null && arguments != null && slots != null) {
                Instance[] frameSlots = createFrameSlots(slots, locals.size());
                List<FieldValue> vals = new ArrayList(arguments.size() + locals.size());
                createArguments(truffleFrame, arguments, vals);
                createLocals(truffleFrame, locals, primitiveLocals, frameSlots, defaultValue, vals);
                values = Collections.unmodifiableList(vals);
                isTruffleFrame = true;
            }
        }
    }

    public List<FieldValue> getFieldValues() {
        return values;
    }

    public boolean isTruffleFrame() {
        return isTruffleFrame;
    }

    private boolean isTruffleFrameSubClass(Instance truffleFrame) {
        return isSubClassOf(truffleFrame, TRUFFLE_FRAME_FQN)
                || isSubClassOf(truffleFrame, COMPILER_FRAME_NOBOX_FQN)
                || isSubClassOf(truffleFrame, ENT_COMPILER_FRAME_NOBOX_FQN)
                || isSubClassOf(truffleFrame, COMPILER_FRAME_BOX_FQN)
                || isSubClassOf(truffleFrame, ENT_COMPILER_FRAME_BOX_FQN);
    }

    private boolean isSubClassOf(Instance i, String superClassName) {
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

    private List<Instance> getObjectArray(Instance instance, String field) {
        Object localsInst = instance.getValueOfField(field);

        if (localsInst instanceof ObjectArrayInstance) {
            return ((ObjectArrayInstance) localsInst).getValues();
        }
        return null;
    }

    private List<String> getPrimitiveArray(Instance instance, String field) {
        Object localsInst = instance.getValueOfField(field);

        if (localsInst instanceof PrimitiveArrayInstance) {
            return ((PrimitiveArrayInstance) localsInst).getValues();
        }
        return null;
    }

    private void createArguments(Instance truffleFrame, List<Instance> arguments, List<FieldValue> values) {
        for (int i = 0; i < arguments.size(); i++) {
            values.add(new TruffleObjectField(truffleFrame, arguments.get(i), ARG_PREFIX + i));
        }
    }

    private void createLocals(Instance truffleFrame, List<Instance> locals, List<String> primitiveLocals, Instance[] frameSlots,
            Instance defaultValue, List<FieldValue> values) {
        for (int i = 0; i < locals.size(); i++) {
            Instance frameSlot = frameSlots[i];
            Instance nameInst = (Instance) frameSlot.getValueOfField("identifier"); // NOI18N
            String name = getDetails(nameInst);
            Type type = getVauleType(frameSlot);

            if (ObjType.OBJECT.equals(type)) {
                values.add(new TruffleObjectField(truffleFrame, locals.get(i), name));
            } else { // primitive type
                if (primitiveLocals != null) {
                    String value = convertValue(primitiveLocals.get(i), type);
                    values.add(new TruffleField(truffleFrame, value, name, type));
                } else {
                    Instance val = locals.get(i);
                    if (val.equals(defaultValue)) {
                        values.add(new TruffleField(truffleFrame, LOCAL_UNDEFINED, name, type));
                    } else {
                        String value = getDetails(val);
                        values.add(new TruffleField(truffleFrame, value, name, type));
                    }
                }
            }
        }
    }

    private Instance[] createFrameSlots(List<Instance> slots, int size) {
        Instance[] names = new Instance[size];

        for (int i = 0; i < size; i++) {
            Instance frameSlot = slots.get(i);
            Integer index = (Integer) frameSlot.getValueOfField("index"); // NOI18N

            names[index.intValue()] = frameSlot;
        }
        return names;
    }

    private Type getVauleType(Instance frameSlot) {
        Instance kind = (Instance) frameSlot.getValueOfField("kind");
        byte tag = ((Byte) kind.getValueOfField("tag")).byteValue();

        switch (tag) {
            case OBJECT_TAG:
                return ObjType.OBJECT;
            case ILLEGAL_TAG:
                return ObjType.OBJECT;
            case LONG_TAG:
                return PType.LONG;
            case INT_TAG:
                return PType.INT;
            case DOUBLE_TAG:
                return PType.DOUBLE;
            case FLOAT_TAG:
                return PType.FLOAT;
            case BOOLEAN_TAG:
                return PType.BOOLEAN;
            case BYTE_TAG:
                return PType.BYTE;
            default:
                throw new IllegalArgumentException("Unknown type:" + tag);
        }
    }

    private String convertValue(String val, Type type) {
        if (!PType.LONG.equals(type)) {
            long originalLong = Long.parseLong(val);

            if (PType.INT.equals(type)) {
                return String.valueOf((int) originalLong);
            }
            if (PType.DOUBLE.equals(type)) {
                return String.valueOf(Double.longBitsToDouble(originalLong));
            }
            if (PType.FLOAT.equals(type)) {
                return String.valueOf(Float.intBitsToFloat((int) originalLong));
            }
            if (PType.BOOLEAN.equals(type)) {
                return String.valueOf((int) originalLong != 0);
            }
            if (PType.BYTE.equals(type)) {
                return String.valueOf((byte) originalLong);
            }
        }
        return val;
    }

    private String getDetails(Instance i) {
        if (i.getJavaClass().getName().startsWith("java.lang.")) {  // NOI18N
            return DetailsUtils.getInstanceString(i, null);
        }
        return "N/A";   // NOI18N
    }

    private class TruffleField implements FieldValue {

        private final Instance definingInstance;
        private final Field field;
        private final String value;

        private TruffleField(Instance defI, String val, String name, Type type) {
            definingInstance = defI;
            value = val;
            field = new FrameField(defI.getJavaClass(), name, type);
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
        public String getValue() {
            return value;
        }
    }

    private class TruffleObjectField extends TruffleField implements ObjectFieldValue {

        private final Instance instanceValue;

        private TruffleObjectField(Instance defI, Instance val, String name) {
            super(defI, val == null ? null : String.valueOf(val.getInstanceId()), name, ObjType.OBJECT);
            instanceValue = val;
        }

        @Override
        public Instance getInstance() {
            return instanceValue;
        }
    }

    private static class FrameField implements Field {

        private final JavaClass definingClass;
        private final String name;
        private final Type type;

        private FrameField(JavaClass cls, String n, Type t) {
            definingClass = cls;
            name = n;
            type = t;
        }

        @Override
        public JavaClass getDeclaringClass() {
            return definingClass;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    private static class ObjType implements Type {

        static final Type OBJECT = new ObjType();

        @Override
        public String getName() {
            return "Object";    // NOI18N
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

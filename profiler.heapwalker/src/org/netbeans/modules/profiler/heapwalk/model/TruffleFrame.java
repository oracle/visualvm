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
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
public class TruffleFrame {

    private static final String TRUFFLE_FRAME_FQN = "com.oracle.truffle.api.impl.DefaultVirtualFrame";  // NOI18N
    private static final String ARG_PREFIX = "arg";         // NOI18N

    private List<FieldValue> values;
    boolean isTruffleFrame;

    public TruffleFrame(Instance truffleFrame) {
        values = Collections.EMPTY_LIST;
        if (isTruffleFrameSubClass(truffleFrame)) {
            List<Instance> locals = getObjectArray(truffleFrame, "locals");         // NOI18N
            List<Instance> arguments = getObjectArray(truffleFrame, "arguments");   // NOI18N
            Instance slotArr = getValueofFields(truffleFrame, "descriptor", "slots");   // NOI18N
            List<Instance> slots = getObjectArray(slotArr, "elementData");  // NOI18N

            if (locals != null && arguments != null && slots != null) {
                String[] localNames = createLocalNames(slots, locals.size());
                values = new ArrayList(arguments.size() + locals.size());
                createArguments(truffleFrame, arguments, values);
                createLocals(truffleFrame, locals, localNames, values);
            }
            isTruffleFrame = true;
        }

    }

    public List<FieldValue> getFieldValues() {
        return values;
    }

    private boolean isTruffleFrameSubClass(Instance truffleFrame) {
        return isSubClassOf(truffleFrame, TRUFFLE_FRAME_FQN);
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

    private void createArguments(Instance truffleFrame, List<Instance> arguments, List<FieldValue> values) {
        for (int i = 0; i < arguments.size(); i++) {
            values.add(new TruffleField(truffleFrame, arguments.get(i), ARG_PREFIX + i));
        }
    }

    private void createLocals(Instance truffleFrame, List<Instance> locals, String[] names, List<FieldValue> values) {
        for (int i = 0; i < locals.size(); i++) {
            values.add(new TruffleField(truffleFrame, locals.get(i), names[i]));
        }
    }

    private String[] createLocalNames(List<Instance> slots, int size) {
        String[] names = new String[size];

        for (int i = 0; i < size; i++) {
            Instance frameSlot = slots.get(i);
            Integer index = (Integer) frameSlot.getValueOfField("index"); // NOI18n
            Instance nameInst = (Instance) frameSlot.getValueOfField("identifier"); // NOI18N
            String name = DetailsUtils.getInstanceString(nameInst, null);

            names[index.intValue()] = name;
        }
        return names;
    }

    private class TruffleField implements ObjectFieldValue {

        private final Instance definingInstance;
        private final Instance value;
        private final Field field;

        private TruffleField(Instance defI, Instance val, String name) {
            definingInstance = defI;
            value = val;
            field = new FrameField(defI.getJavaClass(), name);
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public String getValue() {
            return String.valueOf(value.getInstanceId());
        }

        @Override
        public Instance getDefiningInstance() {
            return definingInstance;
        }

        @Override
        public Instance getInstance() {
            return value;
        }
    }

    private static class FrameField implements Field {

        private final JavaClass definingClass;
        private final String name;

        private FrameField(JavaClass cls, String n) {
            definingClass = cls;
            name = n;
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
            return ObjType.OBJECT;
        }
    }

    private static class ObjType implements Type {

        static final Type OBJECT = new ObjType();

        @Override
        public String getName() {
            return "object";
        }
    }
}

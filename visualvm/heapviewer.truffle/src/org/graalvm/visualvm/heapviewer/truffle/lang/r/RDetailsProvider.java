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

import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.lookup.ServiceProvider;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class RDetailsProvider extends DetailsProvider.Basic {

    private static final String RVECTOR_MASK = "com.oracle.truffle.r.runtime.data.RVector+";   // NOI18N
    private static final String RABSTRACT_VECTOR_MASK = "com.oracle.truffle.r.runtime.data.model.RAbstractVector+";   // NOI18N
    private static final String RSCALAR_VECTOR_MASK = "com.oracle.truffle.r.runtime.data.RScalarVector+";     // NOI18N
    private static final String RLOGICAL_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RLogicalVector";   // NOI18N
    private static final String RLOGICAL_FQN = "com.oracle.truffle.r.runtime.data.RLogical";   // NOI18N
    private static final String RCOMPLEX_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RComplexVector";   // NOI18N
    private static final String RINT_SEQUENCE_FQN = "com.oracle.truffle.r.runtime.data.RIntSequence";   // NOI18N
    private static final String RINT_SEQUENCE1_FQN = "com.oracle.truffle.r.runtime.data.RIntSeqVectorData";   // NOI18N
    private static final String REXPRESSION_FQN = "com.oracle.truffle.r.runtime.data.RExpression";   // NOI18N
    private static final String RWRAPPER_MASK = "com.oracle.truffle.r.runtime.data.RForeignWrapper+";  // NOI18N
    private static final String RSYMBOL_MASK = "com.oracle.truffle.r.runtime.data.RSymbol"; //NOI18N
    private static final String RFUNCTION_MASK = "com.oracle.truffle.r.runtime.data.RFunction"; //NOI18N
    private static final String RS4OBJECT_MASK = "com.oracle.truffle.r.runtime.data.RS4Object"; // NOI18N
    private static final String RNULL_MASK = "com.oracle.truffle.r.runtime.data.RNull"; // NOI18N
    private static final String RENVIRONMENT_MASK = "com.oracle.truffle.r.runtime.env.REnvironment+";    // NOI18N
    private static final String CHARSXPWRAPPER_FQN = "com.oracle.truffle.r.runtime.data.CharSXPWrapper";    // NOI18N

    private static final byte LOGICAL_TRUE = 1;
    private static final byte LOGICAL_FALSE = 0;
    private static final byte LOGICAL_NA = -1;

    public RDetailsProvider() {
        super(RVECTOR_MASK, RABSTRACT_VECTOR_MASK, RSCALAR_VECTOR_MASK, RINT_SEQUENCE_FQN,
              RINT_SEQUENCE1_FQN, RWRAPPER_MASK, RSYMBOL_MASK, RFUNCTION_MASK,
              RS4OBJECT_MASK, RNULL_MASK, RENVIRONMENT_MASK, CHARSXPWRAPPER_FQN);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (RVECTOR_MASK.equals(className) || RABSTRACT_VECTOR_MASK.equals(className)) {
            Object rawData = RObject.findDataField(instance);

            if (rawData != null) {
                int size;

                if (rawData instanceof ObjectArrayInstance) {
                    ObjectArrayInstance data = (ObjectArrayInstance) rawData;
                    size = data.getLength();
                    if (size == 1) {
                        Object obj = data.getValues().get(0);
                        if (REXPRESSION_FQN.equals(instance.getJavaClass().getName()) && obj instanceof Instance) {
                            String str = DetailsUtils.getInstanceFieldString((Instance)obj, "type", heap); // NOI18N
                            if (str != null) return "[" + str + "]"; // NOI18N
                        }
                        return getValue(obj, false, heap);
                    }
                } else if (rawData instanceof PrimitiveArrayInstance) {
                    PrimitiveArrayInstance data = (PrimitiveArrayInstance) rawData;
                    size = data.getLength();
                    if (size == 1) {
                        boolean isLogical = RLOGICAL_VECTOR_FQN.equals(instance.getJavaClass().getName());
                        return getValue(data.getValues().get(0), isLogical, heap);
                    }
                    if (RCOMPLEX_VECTOR_FQN.equals(instance.getJavaClass().getName())) {
                        size /= 2;
                        if (size == 1) {
                            List vals = data.getValues();
                            return "["+vals.get(0)+"+"+vals.get(1)+"i]"; // NOI18N
                        }
                    }
                } else {
                    return null;
                }
                Boolean complete = (Boolean) instance.getValueOfField("complete"); // NOI18N
                Integer refCount = (Integer) instance.getValueOfField("refCount"); // NOI18N
                String rClassName = getRClassName(instance, heap);
                String refString;

                switch (refCount.intValue()) {
                    case 0:
                       refString = ", temporary"; // NOI18N
                       break;
                    case 1:
                        refString = ""; // NOI18N
                        break;
                    case Integer.MAX_VALUE:
                        refString = ", shared permanent"; // NOI18N
                        break;
                    default:
                        refString = ", shared"; // NOI18N
                }
                if (rClassName == null) {
                    rClassName = ""; // NOI18N
                } else {
                    rClassName = rClassName+" "; // NOI18N
                }
                return "Size: " + size + (complete && size>0 ? ", no NAs" : "") +  refString; // NOI18N
            }
            return getScalar(instance, heap);
        }
        if (RSYMBOL_MASK.equals(className)) {
            Instance name = (Instance) instance.getValueOfField("name");   // NOI18N
            if (name != null) {
                return DetailsSupport.getDetailsString(name, heap);
            } else {
                name = (Instance) instance.getValueOfField("nameWrapper");   // NOI18N
                return name == null ? null : DetailsUtils.getInstanceFieldString(name, "contents", heap); // NOI18N
            }
        }
        if (RFUNCTION_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap);
            String packageName = DetailsUtils.getInstanceFieldString(instance, "packageName", heap);

            if (name != null && !name.isEmpty()) {
                if (packageName != null && !packageName.isEmpty()) {
                    return packageName+":::"+name;
                }
                return name;
            }
            Instance target = (Instance) instance.getValueOfField("target");   // NOI18N
            String value = target == null ? null : DetailsSupport.getDetailsString(target, heap);
            return value == null || value.isEmpty() ? null : value;
        }
        if (RSCALAR_VECTOR_MASK.equals(className)) {
            return getScalar(instance, heap);
        }
        if (RINT_SEQUENCE_FQN.equals(className) || RINT_SEQUENCE1_FQN.equals(className)) {
            Integer stride = (Integer) instance.getValueOfField("stride"); // NOI18N
            Integer start = (Integer) instance.getValueOfField("start"); // NOI18N
            Integer len = (Integer) instance.getValueOfField("length"); // NOI18N

            if (stride != null && start != null & len != null) {
                int length = len.intValue();
                if (length == 0) {  // empty vector
                    return "[]";
                }
                if (stride.intValue() == 1) {
                    int end = start.intValue() + length-1;
                    return "["+start.intValue()+":"+end+"]";
                }
            }
        }
        if (RS4OBJECT_MASK.equals(className)) {
            return getRClassName(instance, heap);
        }
        if (RNULL_MASK.equals(className)) {
            return "NULL"; // NOI18N
        }
        if (RWRAPPER_MASK.equals(className)) {
            Instance delegate = (Instance) instance.getValueOfField("delegate"); // NOI18N

            if (delegate != null) {
                Instance proxy = (Instance) delegate.getValueOfField("proxy"); // NOI18N

                if (proxy != null) {
                    Object rawData = proxy.getValueOfField("val$values"); // NOI18N

                    if (rawData instanceof ObjectArrayInstance) {
                        ObjectArrayInstance data = (ObjectArrayInstance) rawData;
                        int size = data.getLength();
                        if (size == 1) {
                            return getValue(data.getValues().get(0), false, heap)+", foreign"; // NOI18N
                        }
                        return "Size: " + size+", foreign"; // NOI18N
                    }
                }
            }
        }
        if (RENVIRONMENT_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap);  // NOI18N
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        if (CHARSXPWRAPPER_FQN.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "contents", heap);  // NOI18N
        }
         return null;
    }

    private String getRClassName(Instance instance, Heap heap) {
        Instance attributesInst = (Instance) instance.getValueOfField("attributes");   // NOI18N
        if (attributesInst != null) {
            DynamicObject  attributes = new DynamicObject(attributesInst);
            FieldValue classAttr = attributes.getFieldValue("class"); // NOI18N
            if (classAttr instanceof ObjectFieldValue) {
                Instance classAttrName = ((ObjectFieldValue)classAttr).getInstance();
                return "Class: " + DetailsSupport.getDetailsString(classAttrName, heap); // NOI18N
            } else {
                classAttr = attributes.getFieldValue(".S3Class"); // NOI18N
                if (classAttr instanceof ObjectFieldValue) {
                    Instance classAttrName = ((ObjectFieldValue)classAttr).getInstance();
                    if (RObject.isRObject(classAttrName)) {
                        StringBuilder classes = new StringBuilder("S3Class: ["); // NOI18N
                        RObject vector = new RObject(classAttrName);
                        List values = vector.getValues();

                        for (int i=0; i<values.size(); i++) {
                            Instance str = (Instance) values.get(i);
                            classes.append(DetailsSupport.getDetailsString((Instance) str, heap));
                            if (i<values.size()-1) {
                                classes.append(", "); // NOI18N
                            }
                        }
                        return classes.append(']').toString(); // NOI18N
                    }
                    return "S3Class: " + DetailsSupport.getDetailsString(classAttrName, heap); // NOI18N
                }
            }
        }
        return null;
    }

    private static String getValue(Object value, boolean isLogical, Heap heap) {
        String valString;

        if (value instanceof Instance) {
            valString = DetailsSupport.getDetailsString((Instance) value, heap);
        } else {
            valString = value.toString();
        }
        if (isLogical) {
            int val = Integer.parseInt(valString);

            if (val == LOGICAL_FALSE) {
                valString = "FALSE"; // NOI18N
            } else if (val == LOGICAL_TRUE) {
                valString = "TRUE"; // NOI18N
            } else if (val == LOGICAL_NA) {
                valString = "NA"; // NOI18N
            }
        }
        return "["+valString+"]"; // NOI18N
    }

    private String getScalar(Instance instance, Heap heap) {
        Object rawData = instance.getValueOfField("value"); // NOI18N

        if (rawData != null) {
            boolean isLogical = RLOGICAL_FQN.equals(instance.getJavaClass().getName());
            return getValue(rawData, isLogical, heap);
        }
        Double realPart = (Double) instance.getValueOfField("realPart");    // NOI18N
        Double imaginaryPart = (Double) instance.getValueOfField("imaginaryPart"); // NOI18N

        if (realPart != null && imaginaryPart != null) {
            return "["+realPart+"+"+imaginaryPart+"i]"; // NOI18N
        }
        return null;
    }
}

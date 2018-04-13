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
package com.sun.tools.visualvm.heapviewer.truffle.r;

import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.lookup.ServiceProvider;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class RDetailsProvider extends DetailsProvider.Basic {

    private static final String RVECTOR_MASK = "com.oracle.truffle.r.runtime.data.RVector+";   // NOI18N
    private static final String RSCALAR_VECTOR_MASK = "com.oracle.truffle.r.runtime.data.RScalarVector+";     // NOI18N
    private static final String RLOGICAL_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RLogicalVector";   // NOI18N
    private static final String RLOGICAL_FQN = "com.oracle.truffle.r.runtime.data.RLogical";   // NOI18N
    private static final String RCOMPLEX_VECTOR_FQN = "com.oracle.truffle.r.runtime.data.RComplexVector";   // NOI18N
    private static final String RWRAPPER_MASK = "com.oracle.truffle.r.runtime.data.RForeignWrapper+";  // NOI18N
    private static final String RSYMBOL_MASK = "com.oracle.truffle.r.runtime.data.RSymbol"; //NOI18N
    private static final String RFUNCTION_MASK = "com.oracle.truffle.r.runtime.data.RFunction"; //NOI18N
    private static final String RS4OBJECT_MASK = "com.oracle.truffle.r.runtime.data.RS4Object"; // NOI18N
    private static final String RNULL_MASK = "com.oracle.truffle.r.runtime.data.RNull"; // NOI18N

    private static final byte LOGICAL_TRUE = 1;
    private static final byte LOGICAL_FALSE = 0;
    private static final byte LOGICAL_NA = -1;

    public RDetailsProvider() {
        super(RVECTOR_MASK, RSYMBOL_MASK, RFUNCTION_MASK, RSCALAR_VECTOR_MASK, RS4OBJECT_MASK,
             RNULL_MASK, RWRAPPER_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (RVECTOR_MASK.equals(className)) {
            Object rawData = instance.getValueOfField("data");

            if (rawData != null) {
                int size;

                if (rawData instanceof ObjectArrayInstance) {
                    ObjectArrayInstance data = (ObjectArrayInstance) rawData;
                    size = data.getLength();
                    if (size == 1) {
                        return getValue(data.getValues().get(0), false, heap);
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
                            return "["+vals.get(0)+"+"+vals.get(1)+"i]";
                        }
                    }
                } else {
                    return null;
                }
                Boolean complete = (Boolean) instance.getValueOfField("complete");
                Integer refCount = (Integer) instance.getValueOfField("refCount");
                String rClassName = getRClassName(instance, heap);
                String refString;

                switch (refCount.intValue()) {
                    case 0:
                       refString = ", temporary";
                       break;
                    case 1:
                        refString = "";
                        break;
                    case Integer.MAX_VALUE:
                        refString = ", shared permanent";
                        break;
                    default:
                        refString = ", shared";
                }
                if (rClassName == null) {
                    rClassName = "";
                } else {
                    rClassName = rClassName+" ";
                }
                return "Size: " + size + (complete ? "" : ", has NAs") +  refString;
            }
        }
        if (RSYMBOL_MASK.equals(className)) {
            Instance name = (Instance) instance.getValueOfField("name");   // NOI18N
            if (name != null) {
                return DetailsSupport.getDetailsString(name, heap);
            }
        }
        if (RFUNCTION_MASK.equals(className)) {
            Instance target = (Instance) instance.getValueOfField("target");   // NOI18N
            String value = target == null ? null : DetailsSupport.getDetailsString(target, heap);
            return value == null || value.isEmpty() ? null : value;
        }
        if (RSCALAR_VECTOR_MASK.equals(className)) {
            Object rawData = instance.getValueOfField("value");

            if (rawData != null) {
                boolean isLogical = RLOGICAL_FQN.equals(instance.getJavaClass().getName());
                return getValue(rawData, isLogical, heap);
            }
            Double realPart = (Double) instance.getValueOfField("realPart");    // NOI18N
            Double imaginaryPart = (Double) instance.getValueOfField("imaginaryPart"); // NOI18N

            if (realPart != null && imaginaryPart != null) {
                return "["+realPart+"+"+imaginaryPart+"i]";
            }
        }
        if (RS4OBJECT_MASK.equals(className)) {
            return getRClassName(instance, heap);
        }
        if (RNULL_MASK.equals(className)) {
            return "NULL";
        }
        if (RWRAPPER_MASK.equals(className)) {
            Instance delegate = (Instance) instance.getValueOfField("delegate");

            if (delegate != null) {
                Instance proxy = (Instance) delegate.getValueOfField("proxy");

                if (proxy != null) {
                    Object rawData = proxy.getValueOfField("val$values");

                    if (rawData instanceof ObjectArrayInstance) {
                        ObjectArrayInstance data = (ObjectArrayInstance) rawData;
                        int size = data.getLength();
                        if (size == 1) {
                            return getValue(data.getValues().get(0), false, heap)+", foreign";
                        }
                        return "Size: " + size+", foreign";
                    }
                }
            }
        }
        return null;
    }

    private String getRClassName(Instance instance, Heap heap) {
        Instance attributesInst = (Instance) instance.getValueOfField("attributes");   // NOI18N
        if (attributesInst != null) {
            DynamicObject  attributes = new DynamicObject(attributesInst);
            FieldValue classAttr = attributes.getFieldValue("class");
            if (classAttr instanceof ObjectFieldValue) {
                Instance classAttrName = ((ObjectFieldValue)classAttr).getInstance();
                return "Class: " + DetailsSupport.getDetailsString(classAttrName, heap);
            } else {
                classAttr = attributes.getFieldValue(".S3Class");
                if (classAttr instanceof ObjectFieldValue) {
                    Instance classAttrName = ((ObjectFieldValue)classAttr).getInstance();
                    if (RObject.isRObject(classAttrName)) {
                        StringBuilder classes = new StringBuilder("S3Class: [");
                        RObject vector = new RObject(classAttrName);
                        List values = vector.getValues();

                        for (int i=0; i<values.size(); i++) {
                            Instance str = (Instance) values.get(i);
                            classes.append(DetailsSupport.getDetailsString((Instance) str, heap));
                            if (i<values.size()-1) {
                                classes.append(", ");
                            }
                        }
                        return classes.append(']').toString();
                    }
                    return "S3Class: " + DetailsSupport.getDetailsString(classAttrName, heap);
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
            int val = Integer.valueOf(valString);

            if (val == LOGICAL_FALSE) {
                valString = "FALSE";
            } else if (val == LOGICAL_TRUE) {
                valString = "TRUE";
            } else if (val == LOGICAL_NA) {
                valString = "NA";
            }
        }
        return "["+valString+"]";
    }
}

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
package org.graalvm.visualvm.heapviewer.truffle.lang.python;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class PythonDetailsProvider extends DetailsProvider.Basic {

    private static final String PCLASS_MASK = "com.oracle.graal.python.builtins.objects.type.PythonClass+";   // NOI18N
    private static final String PFUNCTION_MASK = "com.oracle.graal.python.builtins.objects.function.PFunction+";   // NOI18N
    private static final String PBUILDIN_FUNCTION_MASK = "com.oracle.graal.python.builtins.objects.function.PBuiltinFunction";   // NOI18N
    private static final String PNONE_MASK = "com.oracle.graal.python.builtins.objects.PNone";   // NOI18N
    private static final String PLIST_MASK = "com.oracle.graal.python.builtins.objects.list.PList";   // NOI18N
    private static final String PSTRING_MASK = "com.oracle.graal.python.builtins.objects.str.PString"; // NOI18N
    private static final String BASIC_STORAGE_MASK = "com.oracle.graal.python.runtime.sequence.storage.BasicSequenceStorage+";   // NOI18N
    private static final String EMPTY_STORAGE_MASK = "com.oracle.graal.python.runtime.sequence.storage.EmptySequenceStorage"; // NOI18N
    private static final String BYTE_STORAGE_MASK = "com.oracle.graal.python.runtime.sequence.storage.ByteSequenceStorage"; // NOI18N
    private static final String PTUPLE_MASK = "com.oracle.graal.python.builtins.objects.tuple.PTuple"; // NOI18N
    private static final String PMODULE_MASK = "com.oracle.graal.python.builtins.objects.module.PythonModule"; // NOI18N
    private static final String PBYTES_MASK = "com.oracle.graal.python.builtins.objects.bytes.PBytes"; // NOI18N
    private static final String PCOMPLEX_MASK = "com.oracle.graal.python.builtins.objects.complex.PComplex"; // NOI18N
    private static final String PINT_MASK = "com.oracle.graal.python.builtins.objects.ints.PInt"; // NOI18N
    private static final String PEXCEPTION_MASK = "com.oracle.graal.python.runtime.exception.PException"; // NOI18N
    private static final String GETSET_DESCRIPTOR_MASK = "com.oracle.graal.python.builtins.objects.getsetdescriptor.GetSetDescriptor"; // NOI18N
    private static final String PBUILDIN_CLASSTYPE_MASK = "com.oracle.graal.python.builtins.PythonBuiltinClassType"; // NOI18N
    private static final String PLAZY_STRING_MASK = "com.oracle.graal.python.builtins.objects.str.LazyString"; // NOI18N

    public PythonDetailsProvider() {
        super(PCLASS_MASK,PFUNCTION_MASK,PNONE_MASK,PLIST_MASK,PSTRING_MASK,BASIC_STORAGE_MASK,
              PTUPLE_MASK,PMODULE_MASK,PBYTES_MASK,EMPTY_STORAGE_MASK,PINT_MASK,
              PCOMPLEX_MASK,PEXCEPTION_MASK,PBUILDIN_FUNCTION_MASK, BYTE_STORAGE_MASK,
              GETSET_DESCRIPTOR_MASK,PBUILDIN_CLASSTYPE_MASK,PLAZY_STRING_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (PCLASS_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "className", heap); // NOI18N
        }
        if (PBUILDIN_FUNCTION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (PFUNCTION_MASK.equals(className)) {
            String enclName = DetailsUtils.getInstanceFieldString(instance, "enclosingClassName", heap);    // NOI18N
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N

            if (enclName != null && !enclName.isEmpty()) {
                if (name != null) {
                    return enclName+"."+name;
                }
            }
            return name;
        }
        if (PSTRING_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "value", heap);    // NOI18N
        }
        if (PBUILDIN_CLASSTYPE_MASK.equals(className)) {
            // get name field of PythonBuiltinClassType - there is a conflict with name field from Enum
            for (Object fv : instance.getFieldValues()) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;
                    if ("name".equals(ofv.getField().getName())) {
                        return DetailsUtils.getInstanceString(ofv.getInstance(), heap);
                    }
                }
            }
        }
        if (PNONE_MASK.equals(className)) {
            return "None"; // NOI18N
        }
        if (PLIST_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "store", heap);    // NOI18N
        }
        if (BASIC_STORAGE_MASK.equals(className)) {
            return DetailsUtils.getIntFieldValue(instance, "length", 0) + " items"; // NOI18N
        }
        if (EMPTY_STORAGE_MASK.equals(className)) {
            return "0 items"; // NOI18N
        }
        if (PTUPLE_MASK.equals(className)) {
            String value = DetailsUtils.getInstanceFieldString(instance, "array", heap);    // NOI18N
            if (value == null) {
                return DetailsUtils.getInstanceFieldString(instance, "store", heap);    // NOI18N
            }
            return value;
        }
        if (PMODULE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (PBYTES_MASK.equals(className)) {
            String bytes = DetailsUtils.getPrimitiveArrayFieldString(instance, "bytes", 0, -1, ",", "..."); // NOI18N

            if (bytes == null) {
                return DetailsUtils.getInstanceFieldString(instance, "store", heap); // NOI18N
            }
        }
        if (PCOMPLEX_MASK.equals(className)) {
            Double realObj = (Double) instance.getValueOfField("real");    // NOI18N
            Double imagObj = (Double) instance.getValueOfField("imag");    // NOI18N

            if (realObj != null && imagObj != null) {
                return complexToString(realObj.doubleValue(), imagObj.doubleValue());
            }
        }
        if (PINT_MASK.equals(className)) {
             return DetailsUtils.getInstanceFieldString(instance, "value", heap); // NOI18N
        }
        if (PEXCEPTION_MASK.equals(className)) {
             return DetailsUtils.getInstanceFieldString(instance, "message", heap); // NOI18N
        }
        if (BYTE_STORAGE_MASK.equals(className)) {
            return DetailsUtils.getPrimitiveArrayFieldString(instance, "values", 0, -1, ",", "..."); // NOI18N
        }
        if (GETSET_DESCRIPTOR_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (PLAZY_STRING_MASK.equals(className)) {
            Object val = instance.getValueOfField("length");   // NOI18N
            Object vall = instance.getValueOfField("left");   // NOI18N
            Object valr = instance.getValueOfField("right");   // NOI18N

            if (val instanceof Integer) {
                String left = DetailsUtils.getInstanceString((Instance)vall, heap);

                if (valr == null || left.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                    return left;
                }
                return left + DetailsUtils.getInstanceString((Instance)valr, heap);
            }
        }
        return null;
    }

    private static String complexToString(double real, double imag) {
        if (real == 0.) {
            return toString(imag) + "j"; // NOI18N
        } else {
            if (imag >= 0) {
                return String.format("(%s+%sj)", toString(real), toString(imag)); // NOI18N
            } else {
                return String.format("(%s-%sj)", toString(real), toString(-imag)); // NOI18N
            }
        }
    }

    private static String toString(double value) {
        if (value == Math.floor(value) && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            return Long.toString((long) value);
        } else {
            return Double.toString(value);
        }
    }
}

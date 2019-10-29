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

import java.util.List;
import java.util.UnknownFormatConversionException;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
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
    private static final String PMANAGEDCLASS_MASK = "com.oracle.graal.python.builtins.objects.type.PythonManagedClass+";   // NOI18N
    private static final String PFUNCTION_MASK = "com.oracle.graal.python.builtins.objects.function.PFunction+";   // NOI18N
    private static final String PBUILTIN_FUNCTION_MASK = "com.oracle.graal.python.builtins.objects.function.PBuiltinFunction";   // NOI18N
    private static final String PBUILTIN_METHOD_MASK = "com.oracle.graal.python.builtins.objects.method.PBuiltinMethod";   // NOI18N
    private static final String PMETHOD_MASK = "com.oracle.graal.python.builtins.objects.method.PMethod";   // NOI18N
    private static final String PDECORATEDMETHOD_MASK = "com.oracle.graal.python.builtins.objects.method.PDecoratedMethod";   // NOI18N
    private static final String PCELL_MASK = "com.oracle.graal.python.builtins.objects.cell.PCell";   // NOI18N
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
    private static final String PBASEEXCEPTION_MASK = "com.oracle.graal.python.builtins.objects.exception.PBaseException"; // NOI18N
    private static final String GETSET_DESCRIPTOR_MASK = "com.oracle.graal.python.builtins.objects.getsetdescriptor.GetSetDescriptor"; // NOI18N
    private static final String PBUILTIN_CLASSTYPE_MASK = "com.oracle.graal.python.builtins.PythonBuiltinClassType"; // NOI18N
    private static final String PLAZY_STRING_MASK = "com.oracle.graal.python.builtins.objects.str.LazyString"; // NOI18N
    private static final String PRANGE_MASK = "com.oracle.graal.python.builtins.objects.range.PRange"; // NOI18N
    private static final String PSOCKET_MASK = "com.oracle.graal.python.builtins.objects.socket.PSocket"; // NOI18N

    public PythonDetailsProvider() {
        super(PCLASS_MASK,PMANAGEDCLASS_MASK,PFUNCTION_MASK,PNONE_MASK,PLIST_MASK,PSTRING_MASK,BASIC_STORAGE_MASK,
              PTUPLE_MASK,PMODULE_MASK,PBYTES_MASK,EMPTY_STORAGE_MASK,PINT_MASK,
              PCOMPLEX_MASK,PEXCEPTION_MASK, PBASEEXCEPTION_MASK,PBUILTIN_FUNCTION_MASK, PBUILTIN_METHOD_MASK, PMETHOD_MASK, PDECORATEDMETHOD_MASK, PCELL_MASK, BYTE_STORAGE_MASK,
              GETSET_DESCRIPTOR_MASK,PBUILTIN_CLASSTYPE_MASK,PLAZY_STRING_MASK, PRANGE_MASK, PSOCKET_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (PCLASS_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "className", heap); // NOI18N
        }
        if (PMANAGEDCLASS_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "className", heap); // NOI18N
        }
        if (PBUILTIN_FUNCTION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (PBUILTIN_METHOD_MASK.equals(className)) {
            Object moduleO = instance.getValueOfField("self"); // NOI18N
            if (!(moduleO instanceof Instance)) moduleO = null;
            else if (!((Instance)moduleO).getJavaClass().getName().equals(PMODULE_MASK)) moduleO = null;
            String module = moduleO == null ? null : DetailsUtils.getInstanceString((Instance)moduleO, heap);
            String function = DetailsUtils.getInstanceFieldString(instance, "function", heap);    // NOI18N
            if (function != null) return module != null ? module + "." + function : function;    // NOI18N
            return null;
        }
        if (PMETHOD_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "function", heap); // NOI18N
        }
        if (PDECORATEDMETHOD_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "callable", heap); // NOI18N
        }
        if (PCELL_MASK.equals(className)) {
            Object refO = instance.getValueOfField("ref");
            if (!(refO instanceof Instance)) refO = null;
            else if (((Instance)refO).getJavaClass().getName().equals(PLIST_MASK)) refO = null;
            else if (((Instance)refO).getJavaClass().getName().equals(PTUPLE_MASK)) refO = null;
            return refO == null ? null : DetailsUtils.getInstanceString((Instance)refO, heap);
        }
        if (PFUNCTION_MASK.equals(className)) {
            String enclName = DetailsUtils.getInstanceFieldString(instance, "enclosingClassName", heap);    // NOI18N
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N

            if (enclName != null && !enclName.isEmpty()) {
                if (name != null) {
                    return enclName+"."+name; // NOI18N
                }
            }
            return name;
        }
        if (PSTRING_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "value", heap);    // NOI18N
        }
        if (PBUILTIN_CLASSTYPE_MASK.equals(className)) {
            // get name field of PythonBuiltinClassType - there is a conflict with name field from Enum
            for (Object fv : instance.getFieldValues()) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;
                    if ("name".equals(ofv.getField().getName())) { // NOI18N
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
            String value = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            if (value == null) {
                Instance storageInst = (Instance) instance.getValueOfField("storage");   // NOI18N
                if (storageInst != null) {
                    DynamicObject attrubutes = new DynamicObject(storageInst);
                    FieldValue nameAttr = attrubutes.getFieldValue("__name__"); // NOI18N
                    if (nameAttr instanceof ObjectFieldValue) {
                        Instance moduleName = ((ObjectFieldValue)nameAttr).getInstance();
                        return DetailsSupport.getDetailsString(moduleName, heap);
                    }
                }
            }
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
             String message = DetailsUtils.getInstanceFieldString(instance, "message", heap); // NOI18N
             return message != null ? message : DetailsUtils.getInstanceFieldString(instance, "pythonException", heap); // NOI18N             
        }
        if (PBASEEXCEPTION_MASK.equals(className)) {
            String message = DetailsUtils.getInstanceFieldString(instance, "messageFormat", heap); // NOI18N
            if (message != null) {
                Object args = instance.getValueOfField("messageArgs"); // NOI18N
                if (args instanceof ObjectArrayInstance) {
                    List vals = ((ObjectArrayInstance)args).getValues();
                    Object[] params = new String[vals.size()];
                    for (int i = 0; i < params.length; i++)
                        params[i] = DetailsUtils.getInstanceString((Instance)vals.get(i), heap);
                    message = safeFormatString(3, message, params);
                }
                return message;
            }
            
            Object args = instance.getValueOfField("args"); // NOI18N
            if (args instanceof Instance) {
                Object store = ((Instance)args).getValueOfField("store"); // NOI18N
                if (store instanceof Instance) {
                    Object values = ((Instance)store).getValueOfField("values"); // NOI18N
                    if (values instanceof ObjectArrayInstance) {
                        ObjectArrayInstance arr = (ObjectArrayInstance)values;
                        if (arr.getLength() > 0) {
                            Instance val = (Instance)arr.getValues().get(0);
                            if (val != null) return DetailsUtils.getInstanceString(val, heap);
                        }
                    }
                }
            }
            
            return null;
        }
        if (BYTE_STORAGE_MASK.equals(className)) {
            return DetailsUtils.getPrimitiveArrayFieldString(instance, "values", 0, -1, ",", "..."); // NOI18N
        }
        if (GETSET_DESCRIPTOR_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (PLAZY_STRING_MASK.equals(className)) {
            Object vall = instance.getValueOfField("left");   // NOI18N
            Object valr = instance.getValueOfField("right");   // NOI18N

            String left = DetailsUtils.getInstanceString((Instance)vall, heap);

            if (valr == null || left.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                return left;
            }
            return left + DetailsUtils.getInstanceString((Instance)valr, heap);
        }
        if (PRANGE_MASK.equals(className)) {
            int start = DetailsUtils.getIntFieldValue(instance, "start", 0); // NOI18N
            int stop = DetailsUtils.getIntFieldValue(instance, "stop", 0); // NOI18N
            int step = DetailsUtils.getIntFieldValue(instance, "step", 1); // NOI18N
            return "[" + start + ", " + stop + ", " + step + "]"; // NOI18N
        }
        if (PSOCKET_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "address", heap); // NOI18N
        }
        return null;
    }

    private static String complexToString(double real, double imag) {
        if (Double.compare(real, 0.0) == 0) {
            return toString(imag) + "j"; // NOI18N
        } else {
            String realString = toString(real);
            if (real == 0.0) {
                // special case where real is actually -0.0
                realString = "-0";  // NOI18N
            }
            if (Double.compare(imag, 0.0) >= 0) {
                return String.format("(%s+%sj)", realString, toString(imag));   // NOI18N
            } else {
                return String.format("(%s-%sj)", realString, toString(-imag));  // NOI18N
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
    
    private static String safeFormatString(int maxIterations, String format, Object... args) {
        while (maxIterations-- > 0) {
            try {
                return String.format(format, args);
            } catch (UnknownFormatConversionException e) {
                format = format.replace("%" + e.getConversion(), "%s"); // NOI18N
            }
        }
        return format;
    }
    
}

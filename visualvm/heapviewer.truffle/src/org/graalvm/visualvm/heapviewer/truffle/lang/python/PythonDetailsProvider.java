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
package org.graalvm.visualvm.heapviewer.truffle.lang.python;

import java.util.List;
import java.util.UnknownFormatConversionException;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
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
    private static final String PFROOT_MASK = "com.oracle.graal.python.nodes.function.FunctionRootNode";    // NOI18N
    private static final String PBFROOT_MASK = "com.oracle.graal.python.nodes.function.BuiltinFunctionRootNode";    // NOI18N
    private static final String PMFROOT_MASK = "com.oracle.graal.python.nodes.ModuleRootNode";                      // NOI18N
    private static final String PGFROOT_MASK = "com.oracle.graal.python.nodes.generator.GeneratorFunctionRootNode"; // NOI18N
    private static final String PTFROOT_MASK = "com.oracle.graal.python.nodes.control.TopLevelExceptionHandler";    // NOI18N
    private static final String DICT_KEY_MASK = "com.oracle.graal.python.builtins.objects.common.EconomicMapStorage$DictKey";   // NOI18N
    private static final String METHOD_NODE_MASK = "com.oracle.graal.python.builtins.objects.cext.capi.ExternalFunctionNodes$MethodDescriptorRoot+"; //NOI18N

    public PythonDetailsProvider() {
        super(PCLASS_MASK,PMANAGEDCLASS_MASK,PFUNCTION_MASK,PNONE_MASK,PLIST_MASK,
              PSTRING_MASK,BASIC_STORAGE_MASK, PTUPLE_MASK,PMODULE_MASK,PBYTES_MASK,
              EMPTY_STORAGE_MASK,PINT_MASK, PCOMPLEX_MASK,PEXCEPTION_MASK,
              PBASEEXCEPTION_MASK, PBUILTIN_FUNCTION_MASK, PBUILTIN_METHOD_MASK,
              PMETHOD_MASK, PDECORATEDMETHOD_MASK, PCELL_MASK, BYTE_STORAGE_MASK,
              GETSET_DESCRIPTOR_MASK,PBUILTIN_CLASSTYPE_MASK, PLAZY_STRING_MASK,
              PRANGE_MASK, PSOCKET_MASK, PFROOT_MASK, PBFROOT_MASK, PMFROOT_MASK,
              PGFROOT_MASK, PTFROOT_MASK, DICT_KEY_MASK, METHOD_NODE_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case PCLASS_MASK:
            case PMANAGEDCLASS_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                if (name != null) {
                    return name;
                }
                return DetailsUtils.getInstanceFieldString(instance, "className"); // NOI18N
            }
            case PBUILTIN_FUNCTION_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case PBUILTIN_METHOD_MASK: {
                Object moduleO = instance.getValueOfField("self"); // NOI18N
                if (!(moduleO instanceof Instance)) moduleO = null;
                else if (!((Instance)moduleO).getJavaClass().getName().equals(PMODULE_MASK)) moduleO = null;
                String module = moduleO == null ? null : DetailsUtils.getInstanceString((Instance)moduleO);
                String function = DetailsUtils.getInstanceFieldString(instance, "function");    // NOI18N
                if (function != null) return module != null ? module + "." + function : function;
                break;
            }
            case PMETHOD_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "function"); // NOI18N
            case PDECORATEDMETHOD_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "callable"); // NOI18N
            case PCELL_MASK: {
                Object refO = instance.getValueOfField("ref");      // NOI18N
                if (!(refO instanceof Instance)) refO = null;
                else if (((Instance)refO).getJavaClass().getName().equals(PLIST_MASK)) refO = null;
                else if (((Instance)refO).getJavaClass().getName().equals(PTUPLE_MASK)) refO = null;
                return refO == null ? null : DetailsUtils.getInstanceString((Instance)refO);
            }
            case PFUNCTION_MASK: {
                String enclName = DetailsUtils.getInstanceFieldString(instance, "enclosingClassName");    // NOI18N
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N

                if (enclName != null && !enclName.isEmpty()) {
                    if (name != null) {
                        return enclName+"."+name; // NOI18N
                    }
                }
                return name;
            }
            case PSTRING_MASK: {
                String val = DetailsUtils.getInstanceFieldString(instance, "materializedValue");    // NOI18N
                if (val != null) return val;
                return DetailsUtils.getInstanceFieldString(instance, "value");    // NOI18N
            }
            case PBUILTIN_CLASSTYPE_MASK: {
                // get name field of PythonBuiltinClassType - there is a conflict with name field from Enum
                for (Object fv : instance.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        ObjectFieldValue ofv = (ObjectFieldValue) fv;
                        if ("name".equals(ofv.getField().getName())) { // NOI18N
                            return DetailsUtils.getInstanceString(ofv.getInstance());
                        }
                    }
                }
                break;
            }
            case PNONE_MASK:
                return "None"; // NOI18N
            case PLIST_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "store");    // NOI18N
            case BASIC_STORAGE_MASK:
                return DetailsUtils.getIntFieldValue(instance, "length", 0) + " items"; // NOI18N
            case EMPTY_STORAGE_MASK:
                return "0 items"; // NOI18N
            case PTUPLE_MASK: {
                String value = DetailsUtils.getInstanceFieldString(instance, "array");    // NOI18N
                if (value == null) {
                    return DetailsUtils.getInstanceFieldString(instance, "store");    // NOI18N
                }
                return value;
            }
            case PMODULE_MASK: {
                String value = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                if (value == null) {
                    Instance storageInst = (Instance) instance.getValueOfField("storage");   // NOI18N
                    if (storageInst == null && DynamicObject.isDynamicObject(instance)) {
                        storageInst = instance;
                    }
                    if (storageInst != null) {
                        DynamicObject attrubutes = new DynamicObject(storageInst);
                        FieldValue nameAttr = attrubutes.getFieldValue("__name__"); // NOI18N
                        if (nameAttr instanceof ObjectFieldValue) {
                            Instance moduleName = ((ObjectFieldValue)nameAttr).getInstance();
                            return DetailsSupport.getDetailsString(moduleName);
                        }
                    }
                }
                break;
            }
            case PBYTES_MASK: {
                String bytes = DetailsUtils.getPrimitiveArrayFieldString(instance, "bytes", 0, -1, ",", "..."); // NOI18N
                if (bytes == null) {
                    return DetailsUtils.getInstanceFieldString(instance, "store"); // NOI18N
                }
                break;
            }
            case PCOMPLEX_MASK: {
                Double realObj = (Double) instance.getValueOfField("real");    // NOI18N
                Double imagObj = (Double) instance.getValueOfField("imag");    // NOI18N
                if (realObj != null && imagObj != null) {
                    return complexToString(realObj.doubleValue(), imagObj.doubleValue());
                }
                break;
            }
            case PINT_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "value"); // NOI18N
            case PEXCEPTION_MASK: {
                String message = DetailsUtils.getInstanceFieldString(instance, "message"); // NOI18N
                return message != null ? message : DetailsUtils.getInstanceFieldString(instance, "pythonException"); // NOI18N
            }
            case PBASEEXCEPTION_MASK: {
                String message = DetailsUtils.getInstanceFieldString(instance, "messageFormat"); // NOI18N
                if (message != null) {
                    Object args = instance.getValueOfField("messageArgs"); // NOI18N
                    if (args instanceof ObjectArrayInstance) {
                        List<Instance> vals = ((ObjectArrayInstance)args).getValues();
                        Object[] params = new String[vals.size()];
                        for (int i = 0; i < params.length; i++)
                            params[i] = DetailsUtils.getInstanceString(vals.get(i));
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
                                Instance val = arr.getValues().get(0);
                                if (val != null) return DetailsUtils.getInstanceString(val);
                            }
                        }
                    }
                }
                break;
            }
            case BYTE_STORAGE_MASK:
                return DetailsUtils.getPrimitiveArrayFieldString(instance, "values", 0, -1, ",", "..."); // NOI18N
            case GETSET_DESCRIPTOR_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case PLAZY_STRING_MASK: {
                Object vall = instance.getValueOfField("left");   // NOI18N
                Object valr = instance.getValueOfField("right");   // NOI18N
                String left = DetailsUtils.getInstanceString((Instance)vall);

                if (valr == null || left.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                    return left;
                }
                return left + DetailsUtils.getInstanceString((Instance)valr);
            }
            case PRANGE_MASK: {
                int start = DetailsUtils.getIntFieldValue(instance, "start", 0); // NOI18N
                int stop = DetailsUtils.getIntFieldValue(instance, "stop", 0); // NOI18N
                int step = DetailsUtils.getIntFieldValue(instance, "step", 1); // NOI18N
                return "[" + start + ", " + stop + ", " + step + "]"; // NOI18N
            }
            case PSOCKET_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "address"); // NOI18N
            case PFROOT_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "functionName"); // NOI18N
            case PBFROOT_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case PMFROOT_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case PGFROOT_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "originalName"); // NOI18N
            case PTFROOT_MASK:
                return "<module __main__>"; // NOI18N
            case DICT_KEY_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "value"); // NOI18N
            case METHOD_NODE_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            default:
                break;
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

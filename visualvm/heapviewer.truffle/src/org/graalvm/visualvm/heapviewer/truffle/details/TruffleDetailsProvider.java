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
package org.graalvm.visualvm.heapviewer.truffle.details;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class TruffleDetailsProvider extends DetailsProvider.Basic {

    private static final String DEFAULT_CALL_TARGET_MASK = "com.oracle.truffle.api.impl.DefaultCallTarget";   // NOI18N
    private static final String OPTIMIZED_CALL_TARGET_MASK = "org.graalvm.compiler.truffle.OptimizedCallTarget"; //NOI18N
    private static final String OPTIMIZED_CALL_TARGET1_MASK = "org.graalvm.compiler.truffle.runtime.OptimizedCallTarget+"; // NOI18N
    private static final String ENT_OPTIMIZED_CALL_TARGET_MASK = "com.oracle.graal.truffle.OptimizedCallTarget"; // NOI18N
    private static final String LANG_INFO_MASK = "com.oracle.truffle.api.nodes.LanguageInfo"; // NOI18N
    private static final String LANG_CACHE_MASK = "com.oracle.truffle.api.vm.LanguageCache"; // NOI18N
    private static final String LANG_CACHE1_MASK = "com.oracle.truffle.polyglot.LanguageCache"; // NOI18N
    private static final String POLYGLOT_MASK = "com.oracle.truffle.api.vm.PolyglotLanguage";   // NOI18N
    private static final String INSTRUMENT_INFO_MASK = "com.oracle.truffle.api.InstrumentInfo"; // NOI18N
    private static final String NATIVE_ROOT_MASK = "com.oracle.truffle.nfi.LibFFIFunctionMessageResolutionForeign$ExecuteLibFFIFunctionSubNode$EXECUTERootNode"; // NOI18N
    private static final String NODE_MASK = "com.oracle.truffle.api.nodes.Node+"; // NOI18N
    private static final String TSTRING_MASK = "com.oracle.truffle.api.strings.AbstractTruffleString+"; // NOI18N
    private static final String TS_LONG_MASK = "com.oracle.truffle.api.strings.AbstractTruffleString$LazyLong"; // NOI18N
    private static final String TS_CONCAT_MASK = "com.oracle.truffle.api.strings.AbstractTruffleString$LazyConcat"; // NOI18N
    private static final String LLVM_NODE_MASK = "com.oracle.truffle.llvm.runtime.nodes.func.LLVMFunctionStartNode"; // NOI18N
    private static final String LLVM_FOREIGN_NODE_MASK = "com.oracle.truffle.llvm.runtime.interop.LLVMForeignFunctionCallNode"; // NOI18N
    private static final String TS_ENCODING_CLASS = "com.oracle.truffle.api.strings.TruffleString$Encoding"; // NOI18N
    private static final Object CACHE_LOCK = new Object();
    private static WeakHashMap<Heap,Map<Byte,Encoding>> CACHE;

    public TruffleDetailsProvider() {
        super(DEFAULT_CALL_TARGET_MASK, OPTIMIZED_CALL_TARGET_MASK, OPTIMIZED_CALL_TARGET1_MASK,
                ENT_OPTIMIZED_CALL_TARGET_MASK, LANG_INFO_MASK, LANG_CACHE_MASK,
                LANG_CACHE1_MASK, POLYGLOT_MASK, INSTRUMENT_INFO_MASK, NATIVE_ROOT_MASK,
                NODE_MASK, TSTRING_MASK, TS_LONG_MASK, TS_CONCAT_MASK, LLVM_NODE_MASK,
                LLVM_FOREIGN_NODE_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case DEFAULT_CALL_TARGET_MASK: {
                String rootNode = DetailsUtils.getInstanceFieldString(instance, "rootNode"); // NOI18N

                if (rootNode != null) {
                    return rootNode;
                }
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            }
            case OPTIMIZED_CALL_TARGET_MASK:
            case OPTIMIZED_CALL_TARGET1_MASK:
            case ENT_OPTIMIZED_CALL_TARGET_MASK: {
                String rootNode = DetailsUtils.getInstanceFieldString(instance, "rootNode"); // NOI18N

                if (rootNode != null) {
                    Object entryPoint = instance.getValueOfField("entryPoint"); // NOI18N

                    if (entryPoint instanceof Long && ((Long) entryPoint).longValue() != 0) {
                        rootNode += " <opt>"; // NOI18N
                    }
                    if (instance.getValueOfField("sourceCallTarget") != null) { // NOI18N
                        rootNode += " <split-" + Long.toHexString(instance.getInstanceId()) + ">"; // NOI18N
                    }
                    return rootNode;
                } else {
                    return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                }
            }
            case LANG_INFO_MASK:
            case LANG_CACHE_MASK:
            case LANG_CACHE1_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                String version = DetailsUtils.getInstanceFieldString(instance, "version"); // NOI18N

                if (name != null && version != null) {
                    return name + " (version " + version + ")"; // NOI18N
                }
                return name;
            }
            case POLYGLOT_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "info"); // NOI18N
            case INSTRUMENT_INFO_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                String version = DetailsUtils.getInstanceFieldString(instance, "version"); // NOI18N

                if (name != null && !name.isEmpty() && version != null && !version.isEmpty()) {
                    return name + " (version " + version + ")"; // NOI18N
                }
                if (name == null || name.isEmpty()) {
                    return DetailsUtils.getInstanceFieldString(instance, "id"); // NOI18N
                }
                return name;
            }
            case NATIVE_ROOT_MASK:
                return "native call"; // NOI18N
            case NODE_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "sourceSection");
            case TSTRING_MASK: {
                Instance next = instance;
                do {
                    String str = getString(next);
                    if (str != null) {
                        return str;
                    }
                    next = (Instance) next.getValueOfField("next"); // NOI18N
                } while (next != null && !instance.equals(next));
                Object data = instance.getValueOfField("data");
                if (data instanceof PrimitiveArrayInstance) {
                    Encoding encoding = getEncoding(instance);
                    Byte stride = (Byte)instance.getValueOfField("stride"); // NOI18N
                    if (stride != null && encoding != null) {
                        byte[] bytes = convertBytes((PrimitiveArrayInstance)data, encoding.naturalStride, stride);
                        try {
                            if ("BYTES".equals(encoding.name)) {
                                return new String(bytes, "ISO-8859-1");
                            }
                            return new String(bytes, encoding.name);
                        } catch (UnsupportedEncodingException ex) {
                            try {
                                return new String(bytes, encoding.name.replace('_', '-'));
                            } catch (UnsupportedEncodingException ex1) {
                                return new String(bytes);
                            }
                        }
                    }
                } else {
                    return DetailsUtils.getInstanceString((Instance) data);
                }
                break;
            }
            case TS_LONG_MASK:
                return String.valueOf(DetailsUtils.getLongFieldValue(instance, "value", 0));    // NOI18N
            case TS_CONCAT_MASK: {
                Object vall = instance.getValueOfField("left");   // NOI18N
                Object valr = instance.getValueOfField("right");   // NOI18N
                String left = DetailsUtils.getInstanceString((Instance)vall);

                if (left == null) {
                    return DetailsUtils.getInstanceString((Instance)valr);
                }
                if (valr == null || left.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                    return left;
                }
                String value = left + DetailsUtils.getInstanceString((Instance)valr);

                if (value.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                    return value.substring(0, DetailsUtils.MAX_ARRAY_LENGTH) + "..."; // NOI18N
                }
                return value;
            }
            case LLVM_NODE_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "originalName"); // NOI18N
                if (name == null) name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                return name;
            }
            case LLVM_FOREIGN_NODE_MASK: {
                Instance classNode = (Instance) instance.getValueOfField("callNode"); // NOI18N
                if (classNode != null) {
                    String value = DetailsUtils.getInstanceFieldString(classNode, "callTarget");    // NOI18N
                    if (value != null) return "LLVM: "+value;        // NOI18N
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

    public View getDetailsView(String className, Instance instance) {
        if (NODE_MASK.equals(className)) {
            Object val = instance.getValueOfField("sourceSection");  // NOI18N
            if (val instanceof Instance) {
                Instance sourceSection = (Instance) val;
                return DetailsSupport.getDetailsView(sourceSection);
            }
        }
        return null;
    }

    private String getString(Instance truffleString) {
        Object data = truffleString.getValueOfField("data");    // NOI18N
        if (data instanceof Instance) {
            Instance idata = (Instance) data;
            if (idata.getJavaClass().getName().equals(String.class.getName())) {
                return DetailsUtils.getInstanceString(idata);
            }
        }
        return null;
    }

    private Encoding getEncoding(Instance truffleString) {
        Byte encodingId = (Byte) truffleString.getValueOfField("encoding"); // NOI18N

        Map<Byte, Encoding> heapCache = getEncodingCache(truffleString);
        Encoding cachedEncoding = heapCache.get(encodingId);
        if (cachedEncoding == null && encodingId != null) {
            Heap heap = truffleString.getJavaClass().getHeap();
            JavaClass encodingClass = heap.getJavaClassByName(TS_ENCODING_CLASS);
            for (Instance encoding : encodingClass.getInstances()) {
                Byte id = (Byte) encoding.getValueOfField("id");    // NOI18N

                if (id.equals(encodingId)) {
                    cachedEncoding = new Encoding(encoding);
                    heapCache.put(encodingId, cachedEncoding);
                }
            }
        }
        return cachedEncoding;
    }

    private Map<Byte, Encoding> getEncodingCache(Instance truffleString) {
        synchronized (CACHE_LOCK) {
            if (CACHE == null) {
                CACHE = new WeakHashMap();
            }
            Heap heap = truffleString.getJavaClass().getHeap();
            Map<Byte, Encoding> heapCache = CACHE.get(heap);
            if (heapCache == null) {
                heapCache = Collections.synchronizedMap(new HashMap<>());
                CACHE.put(heap, heapCache);
            }
            return heapCache;
        }
    }

    private byte[] convertBytes(PrimitiveArrayInstance data, byte naturalStride, byte stride) {
        int inCharSize = 1 << stride;
        int outCharSize = 1 << naturalStride;
        int padding = outCharSize - inCharSize;
        byte[] bytes = new byte[(data.getLength() / inCharSize) * outCharSize];
        List<String> values = data.getValues();
        int op = 0;

        for (int ip = 0; ip < values.size();) {
            for (int j = 0; j < inCharSize; j++) {
                bytes[op++] = Byte.valueOf(values.get(ip++));
            }
            op += padding;
        }
        return bytes;
    }

    private static class Encoding {

        byte encId;
        String name;
        byte naturalStride;

        Encoding(Instance encoding) {
            encId = (Byte) encoding.getValueOfField("id");  // NOI18N
            name = DetailsUtils.getInstanceFieldString(encoding, "name");   // NOI18N
            naturalStride = (Byte) encoding.getValueOfField("naturalStride");   // NOI18N
        }
    }
}

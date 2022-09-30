/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.ruby;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class RubyDetailsProvider extends DetailsProvider.Basic {

    private static final String RUBY_OBJECT_TYPE_MASK = "org.truffleruby.language.RubyObjectType+";   // NOI18N
    private static final String ASCII_ROPE_MASK = "org.truffleruby.core.rope.AsciiOnlyLeafRope";   // NOI18N
    private static final String CONCAT_ROPE_MASK = "org.truffleruby.core.rope.ConcatRope"; // NOI18N
    private static final String SUB_ROPE_MASK = "org.truffleruby.core.rope.SubstringRope";  // NOI18N
    private static final String INVALID_ROPE_MASK = "org.truffleruby.core.rope.InvalidLeafRope";    // NOI18N
    private static final String VALID_ROPE_MASK = "org.truffleruby.core.rope.ValidLeafRope";    // NOI18N
    private static final String INT_ROPE_MASK = "org.truffleruby.core.rope.LazyIntRope"; // NOI18N
    private static final String ROPE_TABLE_KEY_MASK = "org.truffleruby.core.rope.RopeTable$Key"; // NOI18N
    private static final String ENCODING_MASK = "org.jcodings.Encoding+";   // NOI18N
    private static final String MODULE_FIELDS_MASK = "org.truffleruby.core.module.ModuleFields"; // NOI18N
    private static final String BASIC_LAYOUT_MASK= "org.truffleruby.core.basicobject.BasicObjectLayoutImpl$BasicObjectType+"; // NI18N
    private static final String METHOD_INFO_MASK = "org.truffleruby.language.methods.SharedMethodInfo"; // NOi18N
    private static final String RUBY_ROOT_NODE_MASK = "org.truffleruby.language.RubyRootNode"; // NOI18N\
    private static final String RUBY_MODULE_MASK = "org.truffleruby.core.module.RubyModule+";   // NOI18N
    private static final String RUBY_PROC_MASK = "org.truffleruby.core.proc.RubyProc"; // NOI18N
    private static final String RUBY_STRING_MASK = "org.truffleruby.core.string.RubyString"; // NOI18N
    private static final String RUBY_ISTRING_MASK = "org.truffleruby.core.string.ImmutableRubyString"; // NOI18N
    private static final String RUBY_ARRAY_MASK = "org.truffleruby.core.array.RubyArray";   // NOI18N
    private static final String RUBY_SYMBOL_MASK = "org.truffleruby.core.symbol.RubySymbol"; // NOI18N
    private static final String RUBY_HASH_MASK = "org.truffleruby.core.hash.RubyHash"; // NOI18N
    private static final String RUBY_ENCODING_MASK = "org.truffleruby.core.encoding.RubyEncoding"; // NOI18N
    private static final String RUBY_REGEXP_MASK = "org.truffleruby.core.regexp.RubyRegexp"; // NOI18N

    public RubyDetailsProvider() {
        super(RUBY_OBJECT_TYPE_MASK,ASCII_ROPE_MASK,CONCAT_ROPE_MASK,SUB_ROPE_MASK,
                ROPE_TABLE_KEY_MASK,INVALID_ROPE_MASK,VALID_ROPE_MASK,
                INT_ROPE_MASK, ENCODING_MASK, MODULE_FIELDS_MASK,
                BASIC_LAYOUT_MASK, METHOD_INFO_MASK, RUBY_ROOT_NODE_MASK,
                RUBY_MODULE_MASK, RUBY_PROC_MASK, RUBY_STRING_MASK,
                RUBY_ISTRING_MASK, RUBY_ARRAY_MASK,RUBY_SYMBOL_MASK,
                RUBY_HASH_MASK, RUBY_ENCODING_MASK,
                RUBY_REGEXP_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case RUBY_OBJECT_TYPE_MASK: {
                String name = instance.getJavaClass().getName();
                int index = name.lastIndexOf('$'); // NOI18N

                if (index == -1) {
                    index = name.lastIndexOf('.'); // NOI18N
                }
                return name.substring(index+1);
            }
            case ASCII_ROPE_MASK: {
                Integer len = (Integer) instance.getValueOfField("byteLength"); // NOI18N
                return getByteArrayFieldString(instance, "bytes", 0, len.intValue(), "..."); // NOI18N
            }
            case CONCAT_ROPE_MASK: {
                Object vall = instance.getValueOfField("left");   // NOI18N
                Object valr = instance.getValueOfField("right");   // NOI18N

                if (vall == null && valr == null) {
                    // string in 'bytes' similarly to ASCII_ROPE
                    Integer len = (Integer) instance.getValueOfField("byteLength"); // NOI18N
                    return getByteArrayFieldString(instance, "bytes", 0, len.intValue(), "..."); // NOI18N
                }
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
            case SUB_ROPE_MASK: {
                Object offset = instance.getValueOfField("byteOffset");   // NOI18N
                Object child = instance.getValueOfField("child");   // NOI18N
                String childString = DetailsUtils.getInstanceString((Instance) child);
                int byteOffset;
                if (offset == null) {
                    offset = instance.getValueOfField("offset");   // NOI18N
                }       byteOffset = ((Integer) offset).intValue();
                Object length = instance.getValueOfField("byteLength");
                int byteLength = ((Integer) length).intValue();
                if (childString.length() > byteOffset && childString.length() >= byteOffset + byteLength) {
                    return childString.substring(byteOffset, byteOffset + byteLength);
                }
                break;
            }
            case ENCODING_MASK:
                return getByteArrayFieldString(instance, "name", 0, -1, "..."); // NOI18N
            case ROPE_TABLE_KEY_MASK: {
                byte[] bytes = getByteArrayFieldString(instance, "bytes", 0, -1); // NOI18N
                String encodingString = DetailsUtils.getInstanceFieldString(instance, "encoding"); // NOI18N

                return getString(bytes, encodingString, "..."); // NOI18N
            }
            case INVALID_ROPE_MASK: {
                byte[] bytes = getByteArrayFieldString(instance, "bytes", 0, -1); // NOI18N
                String encodingString = DetailsUtils.getInstanceFieldString(instance, "encoding"); // NOI18N

                return getString(bytes, encodingString, "..."); // NOI18N
            }
            case VALID_ROPE_MASK: {
                byte[] bytes = getByteArrayFieldString(instance, "bytes", 0, -1); // NOI18N
                String encodingString = DetailsUtils.getInstanceFieldString(instance, "encoding"); // NOI18N

                return getString(bytes, encodingString, "..."); // NOI18N
            }
            case INT_ROPE_MASK:
                return Integer.toString(DetailsUtils.getIntFieldValue(instance, "value", 0)); // NOI18N
            case MODULE_FIELDS_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case BASIC_LAYOUT_MASK: {
                Instance logicalClassInst = (Instance) instance.getValueOfField("logicalClass"); // NOI18N
                if (DynamicObject.isDynamicObject(logicalClassInst)) {
                    DynamicObject logicalClass = new DynamicObject(logicalClassInst);
                    ObjectFieldValue fields = (ObjectFieldValue) logicalClass.getFieldValue("fields (hidden)"); // NOI18N

                    return DetailsUtils.getInstanceString(fields.getInstance());
                }
                break;
            }
            case METHOD_INFO_MASK: {
                Instance name = (Instance) instance.getValueOfField("name");   // NOI18N

                if (name == null) {
                    name = (Instance) instance.getValueOfField("notes");   // NOI18N
                }
                return DetailsUtils.getInstanceString(name);
            }
            case RUBY_ROOT_NODE_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "sharedMethodInfo"); // NOI18N
            case RUBY_MODULE_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "fields"); // NOI18N
            case RUBY_PROC_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "sharedMethodInfo"); // NOI18N
            case RUBY_ISTRING_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "tstring"); // NOI18N
            case RUBY_STRING_MASK:
                String s = DetailsUtils.getInstanceFieldString(instance, "tstring"); // NOI18N
                if (s == null) s = DetailsUtils.getInstanceFieldString(instance, "rope"); // NOI18N
                return s;
            case RUBY_ARRAY_MASK:
            case RUBY_HASH_MASK: {
                Integer length = (Integer) instance.getValueOfField("size");
                if (length != null) {
                    return Formatters.numberFormat().format(length) + (length == 1 ? " item" : " items"); // NOI18N
                }
                break;
            }
            case RUBY_SYMBOL_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "string"); // NOI18N
            case RUBY_ENCODING_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
            case RUBY_REGEXP_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "source"); // NOI18N
            default:
                break;
        }
        return null;
    }

    private byte[] getByteArrayFieldString(Instance instance, String field, int offset, int count) {
        Object fieldVal = instance.getValueOfField(field);
        if (fieldVal instanceof PrimitiveArrayInstance) {
            PrimitiveArrayInstance array = (PrimitiveArrayInstance)fieldVal;
            List<String> values = array.getValues();
            if (values != null) {
                int valuesCount = count < 0 ? values.size() - offset :
                                  Math.min(count, values.size() - offset);
                int estimatedSize = Math.min(valuesCount, DetailsUtils.MAX_ARRAY_LENGTH + 1);
                byte bytes[] = new byte[estimatedSize];
                int lastValue = offset + valuesCount - 1;
                for (int i = offset; i <= lastValue; i++) {
                    bytes[i-offset] = Byte.parseByte(values.get(i));
                    if (i-offset+1 >= DetailsUtils.MAX_ARRAY_LENGTH) {
                        break;
                    }
                }
                return bytes;
            }
        }
        return null;
    }

    private String getByteArrayFieldString(Instance instance, String field, int offset, int count, String trailer) {
        byte[] bytes = getByteArrayFieldString(instance, field, offset, count);

        return getString(bytes, Charset.defaultCharset().name(), trailer);
    }

    private String getString(byte[] bytes, String encodingString, String trailer) {
        if (bytes != null) {
            String val;
            int len = Math.min(bytes.length, DetailsUtils.MAX_ARRAY_LENGTH);
            try {
                val = new String(bytes, 0, len, encodingString);
            } catch (UnsupportedEncodingException ex) {
                val = new String(bytes, 0, len);
            }
            if (bytes.length > DetailsUtils.MAX_ARRAY_LENGTH) {
                return val + trailer;
            }
            return val;
        }
        return null;
    }
}
